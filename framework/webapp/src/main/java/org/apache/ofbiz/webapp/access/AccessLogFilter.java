/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ofbiz.webapp.access;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 * Single cross-cutting servlet filter that emits, per HTTP request:
 * <ol>
 *   <li>A JSON line describing the request (method, path, headers, body).</li>
 *   <li>A JSON line describing the response (status, path, headers, body).</li>
 *   <li>A copy-pastable {@code curl} command that reproduces the request.</li>
 * </ol>
 *
 * <p>Wired into all OFBiz webapps via the Tomcat default {@code web.xml} at
 * {@code framework/catalina/config/web.xml}. The three lines for a given request share
 * the same {@code reqId} MDC value for grouping. Output is routed to the
 * {@code org.apache.ofbiz.access} logger which writes to
 * {@code logs/apache-ofbiz-framework/api_<date>.log} and stdout.
 *
 * <p>Headers are <b>not</b> redacted &mdash; including {@code Authorization}, {@code Cookie}
 * and friends &mdash; because these logs exist for local API debug/replay and the generated
 * {@code curl} line must be runnable as-is. Do not enable this logger in production.
 */
public final class AccessLogFilter implements Filter {

    // Use the Log4j 2 API directly. SLF4J 2.x on the OFBiz classpath has no ServiceLoader
    // provider (only the legacy log4j-slf4j-impl 1.7.x binding), so LoggerFactory.getLogger(...)
    // would return a NOP logger and silently swallow everything we send.
    private static final Logger LOG = LogManager.getLogger("org.apache.ofbiz.access");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicLong REQ_SEQ = new AtomicLong(System.nanoTime() & 0xFFFFFFFFL);

    /** Hard cap so a stray multi-MB upload doesn't bloat the log file. */
    private static final int MAX_BODY_BYTES = 64 * 1024;

    /** Skip these URL suffixes (static assets). */
    private static final Set<String> SKIP_SUFFIXES = Set.of(
            ".ico", ".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".svg",
            ".woff", ".woff2", ".ttf", ".eot", ".map");
    /** Skip these URL prefixes. */
    private static final Set<String> SKIP_PREFIXES = Set.of("/images/");

    /**
     * Tomcat/servlet headers that the client would recompute or that don't belong in the
     * generated {@code curl} command. We omit them from the curl line but still log them in
     * the request JSON for completeness.
     */
    private static final Set<String> CURL_STRIP_HEADERS = Set.of(
            "host", "content-length", "connection", "transfer-encoding");

    @Override
    public void init(FilterConfig filterConfig) {
        LOG.info("{\"_event\":\"AccessLogFilter loaded\",\"webapp\":\""
                + filterConfig.getServletContext().getContextPath() + "\"}");
    }

    @Override
    public void destroy() {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        if (shouldSkip(httpReq.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String reqId = Long.toString(REQ_SEQ.incrementAndGet(), 36);
        ThreadContext.put("reqId", reqId);

        CachingHttpServletRequest cachingReq = new CachingHttpServletRequest(httpReq);
        CachingHttpServletResponse cachingResp = new CachingHttpServletResponse(httpResp);

        try {
            logRequest(cachingReq);
            chain.doFilter(cachingReq, cachingResp);
        } finally {
            try {
                logResponse(cachingReq, cachingResp);
                logCurl(cachingReq);
            } catch (RuntimeException loggingFailure) {
                LOG.warn("AccessLogFilter post-processing failed: {}", loggingFailure.toString());
            } finally {
                ThreadContext.remove("reqId");
                // Ensure any response bytes we buffered are flushed to the real response.
                cachingResp.flushBufferIfNeeded();
            }
        }
    }

    // ------------------------------------------------------------------------
    // log emitters
    // ------------------------------------------------------------------------

    private void logRequest(CachingHttpServletRequest req) {
        ObjectNode node = JSON.createObjectNode();
        node.put("httpMethod", req.getMethod());
        node.put("requestPath", buildRequestPath(req));
        node.set("requestHttpHeaders", requestHeaders(req));
        node.put("requestBody", requestBodyForLog(req));
        emit(node);
    }

    /**
     * Build the {@code requestBody} value to log. For form-urlencoded and multipart we cannot
     * tee the input stream without breaking Tomcat's lazy parameter parsing (login fields etc.
     * would arrive empty downstream), so we reconstruct from {@link HttpServletRequest#getParameterMap()}.
     */
    private static String requestBodyForLog(CachingHttpServletRequest req) {
        String ct = req.getContentType();
        if (isMultipart(ct)) {
            int n = req.getContentLength();
            return "<multipart: " + (ct == null ? "unknown" : ct)
                    + (n >= 0 ? ", " + n + " bytes>" : ">");
        }
        if (isFormUrlEncoded(ct)) {
            return formBodyFromParameterMap(req);
        }
        return bodyAsLogValue(req.getCachedBody(), ct, req.getCharacterEncoding());
    }

    /**
     * Render the form parameters as the wire body. {@link HttpServletRequest#getParameterMap()}
     * is a merge of query-string and form-body params, so for POST forms with a query string
     * the body printed here may include query-string params too &mdash; acceptable for a debug
     * log (the full URL with query string is also printed in the curl line).
     */
    private static String formBodyFromParameterMap(HttpServletRequest req) {
        Map<String, String[]> params = req.getParameterMap();
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(128);
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            String key = URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8);
            String[] values = e.getValue() == null ? new String[]{""} : e.getValue();
            for (String v : values) {
                if (sb.length() > 0) sb.append('&');
                sb.append(key).append('=')
                        .append(URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    private void logResponse(CachingHttpServletRequest req, CachingHttpServletResponse resp) {
        ObjectNode node = JSON.createObjectNode();
        node.put("httpStatus", resp.getStatus());
        node.put("requestPath", buildRequestPath(req));
        node.set("responseHttpHeaders", responseHeaders(resp));
        node.put("responseBody", bodyAsLogValue(resp.getCachedBody(), resp.getContentType(),
                resp.getCharacterEncoding()));
        emit(node);
    }

    private void logCurl(CachingHttpServletRequest req) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("curl -X ").append(req.getMethod()).append(" '").append(req.getRequestURL());
        String qs = req.getQueryString();
        if (qs != null && !qs.isEmpty()) {
            sb.append('?').append(qs);
        }
        sb.append("'");

        Enumeration<String> names = req.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                if (name == null || CURL_STRIP_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                Enumeration<String> values = req.getHeaders(name);
                while (values != null && values.hasMoreElements()) {
                    String value = values.nextElement();
                    sb.append(" \\\n     -H '").append(name).append(": ")
                            .append(escapeForSingleQuotes(value)).append("'");
                }
            }
        }

        if (!"GET".equalsIgnoreCase(req.getMethod()) && !"HEAD".equalsIgnoreCase(req.getMethod())) {
            String ct = req.getContentType();
            if (isMultipart(ct)) {
                sb.append(" \\\n     # multipart body omitted from curl line");
            } else if (isFormUrlEncoded(ct)) {
                String formBody = formBodyFromParameterMap(req);
                if (!formBody.isEmpty()) {
                    sb.append(" \\\n     --data '").append(escapeForSingleQuotes(formBody)).append("'");
                }
            } else {
                byte[] body = req.getCachedBody();
                if (body != null && body.length > 0
                        && isTextual(ct) && body.length <= MAX_BODY_BYTES) {
                    String bodyStr = decode(body, req.getCharacterEncoding());
                    sb.append(" \\\n     --data-raw '").append(escapeForSingleQuotes(bodyStr)).append("'");
                }
            }
        }
        LOG.info(sb.toString());
    }

    private void emit(ObjectNode node) {
        try {
            LOG.info(JSON.writeValueAsString(node));
        } catch (Exception e) {
            LOG.warn("AccessLogFilter could not serialize log node: {}", e.toString());
        }
    }

    // ------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------

    private static boolean shouldSkip(String uri) {
        if (uri == null) {
            return true;
        }
        for (String prefix : SKIP_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        int lastSlash = uri.lastIndexOf('/');
        int lastDot = uri.lastIndexOf('.');
        if (lastDot > lastSlash) {
            String suffix = uri.substring(lastDot).toLowerCase(Locale.ROOT);
            if (SKIP_SUFFIXES.contains(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static String buildRequestPath(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        if (req.getContextPath() != null) sb.append(req.getContextPath());
        if (req.getServletPath() != null) sb.append(req.getServletPath());
        if (req.getPathInfo() != null) sb.append(req.getPathInfo());
        return sb.toString();
    }

    private static ObjectNode requestHeaders(HttpServletRequest req) {
        ObjectNode out = JSON.createObjectNode();
        Enumeration<String> names = req.getHeaderNames();
        if (names == null) return out;
        // Preserve insertion order, lowercase keys; collapse multi-value headers with ", "
        Map<String, String> collected = new LinkedHashMap<>();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name == null) continue;
            String key = name.toLowerCase(Locale.ROOT);
            StringBuilder joined = new StringBuilder();
            Enumeration<String> values = req.getHeaders(name);
            while (values != null && values.hasMoreElements()) {
                if (joined.length() > 0) joined.append(", ");
                joined.append(values.nextElement());
            }
            collected.merge(key, joined.toString(), (a, b) ->
                    a.isEmpty() ? b : (b.isEmpty() ? a : a + ", " + b));
        }
        collected.forEach(out::put);
        return out;
    }

    private static ObjectNode responseHeaders(HttpServletResponse resp) {
        ObjectNode out = JSON.createObjectNode();
        for (String name : resp.getHeaderNames()) {
            if (name == null) continue;
            String key = name.toLowerCase(Locale.ROOT);
            StringBuilder joined = new StringBuilder();
            for (String value : resp.getHeaders(name)) {
                if (joined.length() > 0) joined.append(", ");
                joined.append(value);
            }
            out.put(key, joined.toString());
        }
        return out;
    }

    /** Render a body to its log-friendly string form (text body / elided / binary tag). */
    private static String bodyAsLogValue(byte[] bytes, String contentType, String charset) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        if (!isTextual(contentType)) {
            return "<binary: " + (contentType == null ? "unknown" : contentType)
                    + ", " + bytes.length + " bytes>";
        }
        if (bytes.length > MAX_BODY_BYTES) {
            return "<elided: " + bytes.length + " bytes>";
        }
        return decode(bytes, charset);
    }

    private static boolean isFormUrlEncoded(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT)
                .startsWith("application/x-www-form-urlencoded");
    }

    private static boolean isMultipart(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT)
                .startsWith("multipart/");
    }

    /** Heuristic: treat JSON, XML, form, plain text and HTML as textual. */
    private static boolean isTextual(String contentType) {
        if (contentType == null) {
            // No content-type usually means small text payload (or empty); allow.
            return true;
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        return ct.startsWith("text/")
                || ct.startsWith("application/json")
                || ct.startsWith("application/xml")
                || ct.startsWith("application/x-www-form-urlencoded")
                || ct.startsWith("application/javascript")
                || ct.startsWith("application/xhtml")
                || ct.contains("+json")
                || ct.contains("+xml");
    }

    private static String decode(byte[] bytes, String charsetName) {
        Charset cs = StandardCharsets.UTF_8;
        if (charsetName != null) {
            try {
                cs = Charset.forName(charsetName);
            } catch (RuntimeException ignored) {
                // fallback to UTF-8
            }
        }
        return new String(bytes, cs);
    }

    /** Escape a string so it can be embedded in a bash single-quoted literal. */
    private static String escapeForSingleQuotes(String s) {
        if (s == null) return "";
        return s.replace("'", "'\\''");
    }

    // ------------------------------------------------------------------------
    // request body caching wrapper
    // ------------------------------------------------------------------------

    private static final class CachingHttpServletRequest extends HttpServletRequestWrapper {

        private byte[] cachedBody;

        CachingHttpServletRequest(HttpServletRequest delegate) {
            super(delegate);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ensureCached();
            return new CachedServletInputStream(cachedBody);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            ensureCached();
            String enc = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(cachedBody), enc != null ? enc : "UTF-8"));
        }

        byte[] getCachedBody() {
            if (cachedBody == null) {
                try {
                    ensureCached();
                } catch (IOException e) {
                    cachedBody = new byte[0];
                }
            }
            return cachedBody;
        }

        private void ensureCached() throws IOException {
            if (cachedBody != null) return;
            // Do NOT consume the body for form-urlencoded or multipart: Tomcat parses these
            // lazily via getParameterMap() / getPart() on the wrapped (delegating) request.
            // Reading the stream here would leave Tomcat with no bytes to parse, causing
            // form fields (USERNAME, PASSWORD, ...) to arrive empty downstream. We log these
            // body shapes from getParameterMap() instead — see requestBodyForLog().
            String ct = ((HttpServletRequest) getRequest()).getContentType();
            if (isFormUrlEncoded(ct) || isMultipart(ct)) {
                cachedBody = new byte[0];
                return;
            }
            try (ServletInputStream original = ((HttpServletRequest) getRequest()).getInputStream();
                    ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[4096];
                int n;
                while ((n = original.read(chunk)) != -1) {
                    buf.write(chunk, 0, n);
                }
                cachedBody = buf.toByteArray();
            }
        }
    }

    private static final class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream backing;
        CachedServletInputStream(byte[] bytes) {
            this.backing = new ByteArrayInputStream(bytes);
        }
        @Override public boolean isFinished() {
            return backing.available() == 0;
        }
        @Override public boolean isReady() {
            return true;
        }
        @Override public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Async reads are not supported");
        }
        @Override public int read() {
            return backing.read();
        }
        @Override public int read(byte[] b, int off, int len) {
            return backing.read(b, off, len);
        }
    }

    // ------------------------------------------------------------------------
    // response body capturing wrapper (tees writes to a buffer)
    // ------------------------------------------------------------------------

    private static final class CachingHttpServletResponse extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private ServletOutputStream wrappedStream;
        private PrintWriter wrappedWriter;

        CachingHttpServletResponse(HttpServletResponse delegate) {
            super(delegate);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (wrappedWriter != null) {
                throw new IllegalStateException("getWriter() has already been called");
            }
            if (wrappedStream == null) {
                wrappedStream = new TeeServletOutputStream(super.getOutputStream(), buffer);
            }
            return wrappedStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (wrappedStream != null) {
                throw new IllegalStateException("getOutputStream() has already been called");
            }
            if (wrappedWriter == null) {
                String enc = getCharacterEncoding() != null ? getCharacterEncoding() : "UTF-8";
                wrappedStream = new TeeServletOutputStream(super.getOutputStream(), buffer);
                wrappedWriter = new PrintWriter(new OutputStreamWriter(wrappedStream, enc), false);
            }
            return wrappedWriter;
        }

        byte[] getCachedBody() {
            return buffer.toByteArray();
        }

        void flushBufferIfNeeded() {
            try {
                if (wrappedWriter != null) {
                    wrappedWriter.flush();
                } else if (wrappedStream != null) {
                    wrappedStream.flush();
                }
            } catch (IOException ignored) {
                // Underlying response may already be closed if the client disconnected.
            }
        }
    }

    private static final class TeeServletOutputStream extends ServletOutputStream {
        private final ServletOutputStream delegate;
        private final ByteArrayOutputStream sink;

        TeeServletOutputStream(ServletOutputStream delegate, ByteArrayOutputStream sink) {
            this.delegate = delegate;
            this.sink = sink;
        }
        @Override public boolean isReady() {
            return delegate.isReady();
        }
        @Override public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }
        @Override public void write(int b) throws IOException {
            delegate.write(b);
            sink.write(b);
        }
        @Override public void write(byte[] b) throws IOException {
            delegate.write(b);
            sink.write(b);
        }
        @Override public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            sink.write(b, off, len);
        }
        @Override public void flush() throws IOException {
            delegate.flush();
        }
        @Override public void close() throws IOException {
            delegate.close();
        }
    }
}
