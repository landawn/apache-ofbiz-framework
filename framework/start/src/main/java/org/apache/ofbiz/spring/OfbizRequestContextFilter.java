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
package org.apache.ofbiz.spring;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.security.SecurityConfigurationException;
import org.apache.ofbiz.security.SecurityFactory;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.apache.ofbiz.base.util.Debug;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring-managed filter that sets up the OFBiz request context (delegator,
 * dispatcher, security) as request attributes for the REST API layer.
 * <p>
 * This filter only applies to Spring-handled {@code /api/*} paths. Legacy
 * webapps deployed as Tomcat child contexts retain their own ContextFilter
 * configured via web.xml.
 * <p>
 * Constructor parameters are injected lazily because the OFBiz runtime
 * (which provides Delegator/Dispatcher/Security) starts via SmartLifecycle,
 * which runs after singleton bean creation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class OfbizRequestContextFilter extends OncePerRequestFilter {

    private static final String MODULE = OfbizRequestContextFilter.class.getName();

    private final Delegator delegator;
    private final LocalDispatcher dispatcher;
    private final Security security;

    public OfbizRequestContextFilter(@Lazy Delegator delegator,
            @Lazy LocalDispatcher dispatcher,
            @Lazy Security security) {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.security = security;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Set core OFBiz objects as request attributes for downstream code
        request.setAttribute("delegator", delegator);
        request.setAttribute("dispatcher", dispatcher);
        request.setAttribute("security", security);

        // Set character encoding if not already set
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        // Set server root URL
        request.setAttribute("_SERVER_ROOT_URL_", UtilHttp.getServerRootUrl(request));

        // Handle multi-tenancy for API requests
        if (EntityUtil.isMultiTenantEnabled()) {
            handleMultiTenant(request);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter API requests handled by Spring controllers.
        // Legacy webapps have their own ContextFilter via web.xml.
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    private void handleMultiTenant(HttpServletRequest request) {
        String serverName = request.getServerName();
        try {
            Delegator baseDelegator = DelegatorFactory.getDelegator(delegator.getDelegatorBaseName());
            GenericValue tenantDomainName = EntityQuery.use(baseDelegator)
                    .from("TenantDomainName")
                    .where("domainName", serverName)
                    .queryOne();
            if (tenantDomainName != null) {
                String tenantId = tenantDomainName.getString("tenantId");
                if (UtilValidate.isNotEmpty(tenantId)) {
                    String tenantDelegatorName = delegator.getDelegatorBaseName() + "#" + tenantId;
                    Delegator tenantDelegator = DelegatorFactory.getDelegator(tenantDelegatorName);
                    LocalDispatcher tenantDispatcher = ServiceContainer.getLocalDispatcher(
                            "spring-dispatcher#" + tenantId, tenantDelegator);
                    Security tenantSecurity = SecurityFactory.getInstance(tenantDelegator);

                    request.setAttribute("delegator", tenantDelegator);
                    request.setAttribute("dispatcher", tenantDispatcher);
                    request.setAttribute("security", tenantSecurity);
                    request.setAttribute("userTenantId", tenantId);
                }
            }
        } catch (GenericEntityException | SecurityConfigurationException e) {
            Debug.logWarning(e, "Unable to resolve tenant for API request", MODULE);
        }
    }
}
