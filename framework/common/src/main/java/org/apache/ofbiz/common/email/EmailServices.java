/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.common.email;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.mail.MimeMessageWrapper;
import org.apache.ofbiz.webapp.view.ApacheFopWorker;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.MacroScreenRenderer;
import org.xml.sax.SAXException;

import com.sun.mail.smtp.SMTPAddressFailedException;

import freemarker.template.TemplateException;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.EmailServicesContext;
/**
 * Email Services
 */
public class EmailServices {

    private static final String MODULE = EmailServices.class.getName();

    private static final String RESOURCE = x.CommonUiLabels;

    /**
     * Basic JavaMail Service
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> sendMail(DispatchContext ctx, EmailServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        String communicationEventId = (String) context.get(x.communicationEventId);
        String orderId = (String) context.get(x.orderId);
        String returnId = (String) context.get(x.returnId);
        Locale locale = (Locale) context.get(x.locale);
        if (communicationEventId != null) {
            Debug.logInfo(x.SendMail_Running_for_communicationEventId + communicationEventId, MODULE);
        }
        Map<String, Object> results = ServiceUtil.returnSuccess();
        String subject = (String) context.get(x.subject);
        subject = FlexibleStringExpander.expandString(subject, context);

        String partyId = (String) context.get(x.partyId);
        String body = (String) context.get(x.body);
        List<Map<String, Object>> bodyParts = UtilGenerics.cast(context.get(x.bodyParts));
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        results.put(x.communicationEventId, communicationEventId);
        results.put(x.partyId, partyId);
        results.put(x.subject, subject);

        if (UtilValidate.isNotEmpty(orderId)) {
            results.put(x.orderId, orderId);
        }
        if (UtilValidate.isNotEmpty(returnId)) {
            results.put(x.returnId, returnId);
        }
        if (UtilValidate.isNotEmpty(body)) {
            body = FlexibleStringExpander.expandString(body, context);
            results.put(x.body, body);
        }
        if (UtilValidate.isNotEmpty(bodyParts)) {
            results.put(x.bodyParts, bodyParts);
        }
        results.put(x.userLogin, userLogin);

        String sendTo = (String) context.get(x.sendTo);
        String sendCc = (String) context.get(x.sendCc);
        String sendBcc = (String) context.get(x.sendBcc);

        // check to see if we should redirect all mail for testing
        String redirectAddress = EntityUtilProperties.getPropertyValue(x.general, x.mail_notifications_redirectTo, delegator);
        if (UtilValidate.isNotEmpty(redirectAddress)) {
            StringBuilder sb = new StringBuilder();
            sb.append(x.To_98448b8d).append(sendTo);
            if (UtilValidate.isNotEmpty(sendCc)) {
                sb.append(x.Cc).append(sendCc);
            }
            if (UtilValidate.isNotEmpty(sendBcc)) {
                sb.append(x.Bcc).append(sendBcc);
            }
            sb.append(x.str_4ff447b8);
            subject += sb.toString();
            sendTo = redirectAddress;
            sendCc = null;
            sendBcc = null;
            if (subject.length() > 255) {
                subject = subject.substring(0, 255);
            }
        }

        String sendFrom = (String) context.get(x.sendFrom);
        if (UtilValidate.isEmpty(sendFrom)) {
            sendFrom = EntityUtilProperties.getPropertyValue(x.general, x.defaultFromEmailAddress, delegator);
        }
        String sendType = (String) context.get(x.sendType);
        String port = (String) context.get(x.port);
        String socketFactoryClass = (String) context.get(x.socketFactoryClass);
        String socketFactoryPort = (String) context.get(x.socketFactoryPort);
        String socketFactoryFallback = (String) context.get(x.socketFactoryFallback);
        String sendVia = (String) context.get(x.sendVia);
        String authUser = (String) context.get(x.authUser);
        String authPass = (String) context.get(x.authPass);
        String messageId = (String) context.get(x.messageId);
        String contentType = (String) context.get(x.contentType);
        Boolean sendPartial = (Boolean) context.get(x.sendPartial);
        Boolean isStartTLSEnabled = (Boolean) context.get(x.startTLSEnabled);

        boolean useSmtpAuth = false;

        // define some default
        if (sendType == null || x.mail_smtp_host.equals(sendType)) {
            sendType = x.mail_smtp_host;
            if (UtilValidate.isEmpty(sendVia)) {
                sendVia = EntityUtilProperties.getPropertyValue(x.general, x.mail_smtp_relay_host, x.localhost, delegator);
            }
            if (UtilValidate.isEmpty(authUser)) {
                authUser = EntityUtilProperties.getPropertyValue(x.general, x.mail_smtp_auth_user, delegator);
            }
            if (UtilValidate.isEmpty(authPass)) {
                authPass = EntityUtilProperties.getPropertyValue(x.general, x.mail_smtp_auth_password, delegator);
            }
            if (UtilValidate.isNotEmpty(authUser)) {
                useSmtpAuth = true;
            }
            if (UtilValidate.isEmpty(port)) {
                port = EntityUtilProperties.getPropertyValue(x.general, x.mail_smtp_port, delegator);
            }
            if (UtilValidate.isEmpty(socketFactoryPort)) {
                socketFactoryPort = EntityUtilProperties.getPropertyValue(x.general, x.mail_smtp_socketFactory_port, delegator);
            }
            if (UtilValidate.isEmpty(socketFactoryClass)) {
                socketFactoryClass = EntityUtilProperties.getPropertyValue(x.general, x.mail_smtp_socketFactory_class, delegator);
            }
            if (UtilValidate.isEmpty(socketFactoryFallback)) {
                socketFactoryFallback = EntityUtilProperties.getPropertyValue(x.general, x.mail_smtp_socketFactory_fallback, x._false, delegator);
            }
            if (sendPartial == null) {
                sendPartial = EntityUtilProperties.propertyValueEqualsIgnoreCase(x.general, x.mail_smtp_sendpartial, x._true, delegator);
            }
            if (isStartTLSEnabled == null) {
                isStartTLSEnabled = EntityUtilProperties.propertyValueEqualsIgnoreCase(x.general, x.mail_smtp_starttls_enable, x._true, delegator);
            }
        } else if (sendVia == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonEmailSendMissingParameterSendVia, locale));
        }

        if (contentType == null) {
            contentType = x.text_html;
        }

        if (UtilValidate.isNotEmpty(bodyParts)) {
            contentType = x.multipart_mixed;
        }
        results.put(x.contentType, contentType);

        Session session;
        MimeMessage mail;
        try {
            Properties props = System.getProperties();
            props.put(sendType, sendVia);
            if (UtilValidate.isNotEmpty(port)) {
                props.put(x.mail_smtp_port, port);
            }
            if (UtilValidate.isNotEmpty(socketFactoryPort)) {
                props.put(x.mail_smtp_socketFactory_port, socketFactoryPort);
            }
            if (UtilValidate.isNotEmpty(socketFactoryClass)) {
                props.put(x.mail_smtp_socketFactory_class, socketFactoryClass);
            }
            if (UtilValidate.isNotEmpty(socketFactoryFallback)) {
                props.put(x.mail_smtp_socketFactory_fallback, socketFactoryFallback);
            }
            if (useSmtpAuth) {
                props.put(x.mail_smtp_auth, x._true);
            }
            if (sendPartial != null) {
                props.put(x.mail_smtp_sendpartial, sendPartial ? x._true : x._false);
            }
            if (isStartTLSEnabled) {
                props.put(x.mail_smtp_starttls_enable, x._true);
            }

            session = Session.getInstance(props);
            boolean debug = EntityUtilProperties.propertyValueEqualsIgnoreCase(x.general, x.mail_debug_on, x.Y, delegator);
            session.setDebug(debug);

            mail = new MimeMessage(session);
            if (messageId != null) {
                mail.setHeader(x.In_Reply_To, messageId);
                mail.setHeader(x.References, messageId);
            }
            mail.setFrom(new InternetAddress(sendFrom));
            mail.setSubject(subject, x.UTF_8);
            mail.setHeader(x.X_Mailer, x.Apache_OFBiz_The_Open_For_Business_Project);
            mail.setSentDate(new Date());
            mail.addRecipients(Message.RecipientType.TO, sendTo);

            if (UtilValidate.isNotEmpty(sendCc)) {
                mail.addRecipients(Message.RecipientType.CC, sendCc);
            }
            if (UtilValidate.isNotEmpty(sendBcc)) {
                mail.addRecipients(Message.RecipientType.BCC, sendBcc);
            }

            if (UtilValidate.isNotEmpty(bodyParts)) {
                // check for multipart message (with attachments)
                // BodyParts contain a list of Maps items containing content(String) and type(String) of the attachement
                MimeMultipart mp = new MimeMultipart();
                Debug.logInfo(bodyParts.size() + x.multiparts_found, MODULE);
                for (Map<String, Object> bodyPart: bodyParts) {
                    Object bodyPartContent = bodyPart.get(x.content);
                    MimeBodyPart mbp = new MimeBodyPart();

                    if (bodyPartContent instanceof String) {
                        Debug.logInfo(x.part_of_type + bodyPart.get(x.type) + x.and_size + bodyPart.get(x.content).toString().length(), MODULE);
                        mbp.setText((String) bodyPartContent, x.UTF_8, ((String) bodyPart.get(x.type)).substring(5));
                    } else if (bodyPartContent instanceof byte[]) {
                        ByteArrayDataSource bads = new ByteArrayDataSource((byte[]) bodyPartContent, (String) bodyPart.get(x.type));
                        Debug.logInfo(x.part_of_type + bodyPart.get(x.type) + x.and_size + ((byte[]) bodyPartContent).length, MODULE);
                        mbp.setDataHandler(new DataHandler(bads));
                    } else if (bodyPartContent instanceof DataHandler) {
                        mbp.setDataHandler((DataHandler) bodyPartContent);
                    } else {
                        mbp.setDataHandler(new DataHandler(bodyPartContent, (String) bodyPart.get(x.type)));
                    }

                    String fileName = (String) bodyPart.get(x.filename);
                    if (fileName != null) {
                        mbp.setFileName(fileName);
                    }
                    mp.addBodyPart(mbp);
                }
                mail.setContent(mp);
                mail.saveChanges();
            } else {
                // create the singelpart message
                if (contentType.startsWith(x.text)) {
                    mail.setText(body, x.UTF_8, contentType.substring(5));
                } else {
                    mail.setContent(body, contentType);
                }
                mail.saveChanges();
            }
        } catch (MessagingException e) {
            Debug.logError(e, x.MessagingException_when_creating_message_to + sendTo + x._from_22fce458 + sendFrom + x.cc + sendCc + x.bcc
                    + sendBcc + x.subject_2c1268d6 + subject + x.str_4ff447b8, MODULE);
            Debug.logError(x.Email_message_that_could_not_be_created_to + sendTo + x.had_context + context, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonEmailSendMessagingException, UtilMisc.toMap(x.sendTo,
                    sendTo, x.sendFrom, sendFrom, x.sendCc, sendCc, x.sendBcc, sendBcc, x.subject, subject), locale));
        }

        // check to see if sending mail is enabled
        String mailEnabled = EntityUtilProperties.getPropertyValue(x.general, x.mail_notifications_enabled, x.N, delegator);
        if (!x.Y.equalsIgnoreCase(mailEnabled)) {
            // no error; just return as if we already processed
            Debug.logImportant(x.Mail_notifications_disabled_in_general_properties_mail_with_subject + subject + x.not_sent_to_addressee
                    + sendTo + x.str_4ff447b8, MODULE);
            if (Debug.verboseOn()) {
                Debug.logVerbose(x.What_would_have_been_sent_the_addressee + sendTo + x.subject_97c234d2 + subject + x.context_9caefb07 + context, MODULE);
            }
            results.put(x.messageWrapper, new MimeMessageWrapper(session, mail));
            return results;
        }

        Transport trans = null;
        try {
            trans = session.getTransport(x.smtp);
            if (!useSmtpAuth) {
                trans.connect();
            } else {
                trans.connect(sendVia, authUser, authPass);
            }
            trans.sendMessage(mail, mail.getAllRecipients());
            results.put(x.messageWrapper, new MimeMessageWrapper(session, mail));
            results.put(x.messageId, mail.getMessageID());
            trans.close();
        } catch (SendFailedException e) {
            // message code prefix may be used by calling services to determine the cause of the failure
            Debug.logError(e, x.ADDRERR_Address_error_when_sending_message_to + sendTo + x._from_22fce458 + sendFrom + x.cc + sendCc
                    + x.bcc + sendBcc + x.subject_2c1268d6 + subject + x.str_4ff447b8, MODULE);
            List<SMTPAddressFailedException> failedAddresses = new LinkedList<>();
            Exception nestedException = null;
            while ((nestedException = e.getNextException()) != null && nestedException instanceof MessagingException) {
                if (nestedException instanceof SMTPAddressFailedException) {
                    SMTPAddressFailedException safe = (SMTPAddressFailedException) nestedException;
                    Debug.logError(x.Failed_to_send_message_to + safe.getAddress() + x.return_code + safe.getReturnCode()
                            + x.return_message + safe.getMessage() + x.str_4ff447b8, MODULE);
                    failedAddresses.add(safe);
                    break;
                }
            }
            Boolean sendFailureNotification = (Boolean) context.get(x.sendFailureNotification);
            if (sendFailureNotification == null || sendFailureNotification) {
                sendFailureNotification(ctx, context, mail, failedAddresses);
                results.put(x.messageWrapper, new MimeMessageWrapper(session, mail));
                try {
                    results.put(x.messageId, mail.getMessageID());
                    trans.close();
                } catch (MessagingException e1) {
                    Debug.logError(e1, MODULE);
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonEmailSendAddressError, UtilMisc.toMap(x.sendTo,
                        sendTo, x.sendFrom, sendFrom, x.sendCc, sendCc, x.sendBcc, sendBcc, x.subject, subject), locale));
            }
        } catch (MessagingException e) {
            // message code prefix may be used by calling services to determine the cause of the failure
            Debug.logError(e, x.CON_Connection_error_when_sending_message_to + sendTo + x._from_22fce458 + sendFrom + x.cc + sendCc
                    + x.bcc + sendBcc + x.subject_2c1268d6 + subject + x.str_4ff447b8, MODULE);
            Debug.logError(x.Email_message_that_could_not_be_sent_to + sendTo + x.had_context + context, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonEmailSendConnectionError, UtilMisc.toMap(x.sendTo,
                    sendTo, x.sendFrom, sendFrom, x.sendCc, sendCc, x.sendBcc, sendBcc, x.subject, subject), locale));
        }
        return results;
    }

    /**
     * JavaMail Service that gets body content from a URL
     *@param ctx The DispatchContext that this service is operating in
     *@param rcontext Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> sendMailFromUrl(DispatchContext ctx, Map<String, ? extends Object> rcontext) {
        // pretty simple, get the content and then call the sendMail method below
        Map<String, Object> sendMailContext = UtilMisc.makeMapWritable(rcontext);
        String bodyUrl = (String) sendMailContext.remove(x.bodyUrl);
        Map<String, Object> bodyUrlParameters = UtilGenerics.cast(sendMailContext.remove(x.bodyUrlParameters));
        Locale locale = (Locale) rcontext.get(x.locale);
        LocalDispatcher dispatcher = ctx.getDispatcher();

        URL url = null;
        URI uri;
        try {
            uri = new URI(bodyUrl);
            url = uri.toURL();
        } catch (IllegalArgumentException | URISyntaxException | MalformedURLException e) {
            Debug.logWarning(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonEmailSendMalformedUrl, UtilMisc.toMap(x.bodyUrl,
                    bodyUrl, x.errorString, e.toString()), locale));
        }

        HttpClient httpClient = new HttpClient(url, bodyUrlParameters);
        String body = null;

        try {
            body = httpClient.post();
        } catch (HttpClientException e) {
            Debug.logWarning(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonEmailSendGettingError, UtilMisc.toMap(x.errorString,
                    e.toString()), locale));
        }

        sendMailContext.put(x.body, body);
        Map<String, Object> sendMailResult;
        try {
            sendMailResult = dispatcher.runSync(x.sendMail, sendMailContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // just return the same result; it contains all necessary information
        return sendMailResult;
    }

    /**
     * JavaMail Service that gets body content from a Screen Widget
     * defined in the product store record and if available as attachment also.
     *@param dctx The DispatchContext that this service is operating in
     *@param rServiceContext Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> sendMailFromScreen(DispatchContext dctx, Map<String, ? extends Object> rServiceContext) {
        Map<String, Object> serviceContext = UtilMisc.makeMapWritable(rServiceContext);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String webSiteId = (String) serviceContext.remove(x.webSiteId);
        String bodyText = (String) serviceContext.remove(x.bodyText);
        String bodyScreenUri = (String) serviceContext.remove(x.bodyScreenUri);
        String xslfoAttachScreenLocationParam = (String) serviceContext.remove(x.xslfoAttachScreenLocation);
        String attachmentNameParam = (String) serviceContext.remove(x.attachmentName);
        List<String> xslfoAttachScreenLocationListParam = UtilGenerics.cast(serviceContext.remove(x.xslfoAttachScreenLocationList));
        List<String> attachmentNameListParam = UtilGenerics.cast(serviceContext.remove(x.attachmentNameList));
        VisualTheme visualTheme = (VisualTheme) rServiceContext.get(x.visualTheme);
        if (visualTheme == null) {
            visualTheme = ThemeFactory.resolveVisualTheme(null);
        }

        List<String> xslfoAttachScreenLocationList = new LinkedList<>();
        List<String> attachmentNameList = new LinkedList<>();
        if (UtilValidate.isNotEmpty(xslfoAttachScreenLocationParam)) {
            xslfoAttachScreenLocationList.add(xslfoAttachScreenLocationParam);
        }
        if (UtilValidate.isNotEmpty(attachmentNameParam)) {
            attachmentNameList.add(attachmentNameParam);
        }
        if (UtilValidate.isNotEmpty(xslfoAttachScreenLocationListParam)) {
            xslfoAttachScreenLocationList.addAll(xslfoAttachScreenLocationListParam);
        }
        if (UtilValidate.isNotEmpty(attachmentNameListParam)) {
            attachmentNameList.addAll(attachmentNameListParam);
        }

        List<String> attachmentTypeList = new LinkedList<>();
        String attachmentTypeParam = (String) serviceContext.remove(x.attachmentType);
        List<String> attachmentTypeListParam = UtilGenerics.cast(serviceContext.remove(x.attachmentTypeList));
        if (UtilValidate.isNotEmpty(attachmentTypeParam)) {
            attachmentTypeList.add(attachmentTypeParam);
        }
        if (UtilValidate.isNotEmpty(attachmentTypeListParam)) {
            attachmentTypeList.addAll(attachmentTypeListParam);
        }

        Locale locale = (Locale) serviceContext.get(x.locale);
        Map<String, Object> bodyParameters = UtilGenerics.cast(serviceContext.remove(x.bodyParameters));
        if (bodyParameters == null) {
            bodyParameters = MapStack.create();
        }
        if (!bodyParameters.containsKey(x.locale)) {
            bodyParameters.put(x.locale, locale);
        } else {
            locale = (Locale) bodyParameters.get(x.locale);
        }
        String partyId = (String) serviceContext.get(x.partyId);
        if (partyId == null) {
            partyId = (String) bodyParameters.get(x.partyId);
        }
        String orderId = (String) bodyParameters.get(x.orderId);
        String returnId = (String) serviceContext.get(x.returnId);
        String custRequestId = (String) bodyParameters.get(x.custRequestId);

        bodyParameters.put(x.communicationEventId, serviceContext.get(x.communicationEventId));
        NotificationServices.setBaseUrl(dctx.getDelegator(), webSiteId, bodyParameters);
        String contentType = (String) serviceContext.remove(x.contentType);

        StringWriter bodyWriter = new StringWriter();

        MapStack<String> screenContext = MapStack.create();
        screenContext.put(x.locale, locale);
        screenContext.put(x.webSiteId, webSiteId);

        ScreenStringRenderer screenStringRenderer = null;
        try {
            screenStringRenderer = new MacroScreenRenderer(visualTheme.getModelTheme(), x.screen);
        } catch (TemplateException | IOException e) {
            Debug.logError(x.Error_rendering_screen_for_email + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonEmailSendRenderingScreenEmailError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
        ScreenRenderer screens = new ScreenRenderer(bodyWriter, screenContext, screenStringRenderer);
        screens.populateContextForService(dctx, bodyParameters);
        screenContext.putAll(bodyParameters);

        if (bodyScreenUri != null) {
            try {
                screens.render(bodyScreenUri);
            } catch (GeneralException | IOException | SAXException | ParserConfigurationException e) {
                Debug.logError(e, x.Error_rendering_screen_for_email + e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonEmailSendRenderingScreenEmailError,
                        UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }

        boolean isMultiPart = false;

        // check if attachment screen location passed in
        if (UtilValidate.isNotEmpty(xslfoAttachScreenLocationList)) {
            List<Map<String, ? extends Object>> bodyParts = new LinkedList<>();
            if (bodyText != null) {
                bodyText = FlexibleStringExpander.expandString(bodyText, screenContext, locale);
                bodyParts.add(UtilMisc.<String, Object>toMap(x.content, bodyText, x.type, UtilValidate.isNotEmpty(contentType) ? contentType
                        : x.text_html));
            } else {
                bodyParts.add(UtilMisc.<String, Object>toMap(x.content, bodyWriter.toString(), x.type, UtilValidate.isNotEmpty(contentType)
                        ? contentType : x.text_html));
            }
            for (int i = 0; i < xslfoAttachScreenLocationList.size(); i++) {
                String xslfoAttachScreenLocation = xslfoAttachScreenLocationList.get(i);
                String attachmentName = x.Details_pdf;
                if (UtilValidate.isNotEmpty(attachmentNameList) && attachmentNameList.size() >= i) {
                    attachmentName = attachmentNameList.get(i);
                }

                String attachmentType = MimeConstants.MIME_PDF;
                if (UtilValidate.isNotEmpty(attachmentTypeList) && attachmentTypeList.size() >= i) {
                    attachmentType = attachmentTypeList.get(i);
                }

                isMultiPart = true;
                // start processing fo pdf attachment
                try (Writer writer = new StringWriter(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    // substitute the freemarker variables...
                    ScreenStringRenderer foScreenStringRenderer = null;
                    if (MimeConstants.MIME_PLAIN_TEXT.equals(attachmentType)) {
                        foScreenStringRenderer = new MacroScreenRenderer(visualTheme.getModelTheme(), x.screentext);
                    } else {
                        foScreenStringRenderer = new MacroScreenRenderer(visualTheme.getModelTheme(), x.screenfop);
                    }
                    ScreenRenderer screensAtt = new ScreenRenderer(writer, screenContext, foScreenStringRenderer);
                    screensAtt.populateContextForService(dctx, bodyParameters);
                    screensAtt.render(xslfoAttachScreenLocation);

                    // create the output stream for the generation

                    if (MimeConstants.MIME_PLAIN_TEXT.equals(attachmentType)) {
                        baos.write(writer.toString().getBytes(x.UTF_8));
                    } else {
                        // create the input stream for the generation
                        StreamSource src = new StreamSource(new StringReader(writer.toString()));
                        Fop fop = ApacheFopWorker.createFopInstance(baos, attachmentType);
                        ApacheFopWorker.transform(src, null, fop);
                    }

                    // store in the list of maps for sendmail....
                    bodyParts.add(UtilMisc.<String, Object>toMap(x.content, baos.toByteArray(), x.type, attachmentType, x.filename, attachmentName));

                } catch (GeneralException | IOException | SAXException | ParserConfigurationException | TemplateException ge) {
                    Debug.logError(x.Error_rendering_PDF_attachment_for_email + ge.toString(), MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonEmailSendRenderingScreenPdfError,
                            UtilMisc.toMap(x.errorString, ge.toString()), locale));
                }

                serviceContext.put(x.bodyParts, bodyParts);
            }
        } else {
            isMultiPart = false;
            // store body and type for single part message in the context.
            if (bodyText != null) {
                bodyText = FlexibleStringExpander.expandString(bodyText, screenContext, locale);
                serviceContext.put(x.body, bodyText);
            } else {
                serviceContext.put(x.body, bodyWriter.toString());
            }

            // Only override the default contentType in case of plaintext, since other contentTypes may be multipart
            //    and would require specific handling.
            if (contentType != null && x.text_plain.equalsIgnoreCase(contentType)) {
                serviceContext.put(x.contentType, x.text_plain);
            } else {
                serviceContext.put(x.contentType, x.text_html);
            }
        }

        // also expand the subject at this point, just in case it has the FlexibleStringExpander syntax in it...
        String subject = (String) serviceContext.remove(x.subject);
        subject = FlexibleStringExpander.expandString(subject, screenContext, locale);
        if (Debug.infoOn()) {
            Debug.logInfo(x.Expanded_email_subject_to + subject, MODULE);
        }
        serviceContext.put(x.subject, subject);
        serviceContext.put(x.partyId, partyId);
        if (UtilValidate.isNotEmpty(orderId)) {
            serviceContext.put(x.orderId, orderId);
        }
        if (UtilValidate.isNotEmpty(returnId)) {
            serviceContext.put(x.returnId, returnId);
        }
        if (UtilValidate.isNotEmpty(custRequestId)) {
            serviceContext.put(x.custRequestId, custRequestId);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose(x.sendMailFromScreen_sendMail_context + serviceContext, MODULE);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, Object> sendMailResult;
        Boolean hideInLog = (Boolean) serviceContext.get(x.hideInLog);
        try {
            if (!Boolean.TRUE.equals(hideInLog)) {
                if (isMultiPart) {
                    sendMailResult = dispatcher.runSync(x.sendMailMultiPart, serviceContext);
                } else {
                    sendMailResult = dispatcher.runSync(x.sendMail, serviceContext);
                }
            } else {
                if (isMultiPart) {
                    sendMailResult = dispatcher.runSync(x.sendMailMultiPartHiddenInLog, serviceContext);
                } else {
                    sendMailResult = dispatcher.runSync(x.sendMailHiddenInLog, serviceContext);
                }
            }
        } catch (Exception e) {
            Debug.logError(e, x.Error_send_email + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonEmailSendError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
        if (ServiceUtil.isError(sendMailResult)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(sendMailResult));
        }

        result.put(x.messageWrapper, sendMailResult.get(x.messageWrapper));
        result.put(x.body, bodyWriter.toString());
        result.put(x.subject, subject);
        result.put(x.communicationEventId, sendMailResult.get(x.communicationEventId));
        if (UtilValidate.isNotEmpty(orderId)) {
            result.put(x.orderId, orderId);
        }
        if (UtilValidate.isNotEmpty(returnId)) {
            result.put(x.returnId, returnId);
        }
        if (UtilValidate.isNotEmpty(custRequestId)) {
            result.put(x.custRequestId, custRequestId);
        }
        return result;
    }

    /**
     * JavaMail Service same than sendMailFromScreen but with hidden result in log.
     * To prevent having not encoded passwords shown in log
     *@param dctx The DispatchContext that this service is operating in
     *@param rServiceContext Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> sendMailHiddenInLogFromScreen(DispatchContext dctx, Map<String, ? extends Object> rServiceContext) {
        Map<String, Object> serviceContext = UtilMisc.makeMapWritable(rServiceContext);
        serviceContext.put(x.hideInLog, true);
        return sendMailFromScreen(dctx, serviceContext);
    }
    public static void sendFailureNotification(DispatchContext dctx, EmailServicesContext context, MimeMessage message,
                                               List<SMTPAddressFailedException> failures) {
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> newContext = new LinkedHashMap<>();
        newContext.put(x.userLogin, context.get(x.userLogin));
        newContext.put(x.sendFailureNotification, false);
        newContext.put(x.sendFrom, context.get(x.sendFrom));
        newContext.put(x.sendTo, context.get(x.sendFrom));
        newContext.put(x.subject, UtilProperties.getMessage(RESOURCE, x.CommonEmailSendUndeliveredMail, locale));
        StringBuilder sb = new StringBuilder();
        sb.append(UtilProperties.getMessage(RESOURCE, x.CommonEmailDeliveryFailed, locale));
        sb.append(x.n_n);
        for (SMTPAddressFailedException failure : failures) {
            sb.append(failure.getAddress());
            sb.append(x.str_ceca32e9);
            sb.append(failure.getMessage());
            sb.append(x.n_n);
        }
        sb.append(UtilProperties.getMessage(RESOURCE, x.CommonEmailDeliveryOriginalMessage, locale));
        sb.append(x.n_n);
        List<Map<String, Object>> bodyParts = new LinkedList<>();
        bodyParts.add(UtilMisc.<String, Object>toMap(x.content, sb.toString(), x.type, x.text_plain));
        try {
            bodyParts.add(UtilMisc.<String, Object>toMap(x.content, message.getDataHandler()));
        } catch (MessagingException e) {
            Debug.logError(e, MODULE);
        }
        newContext.put(x.bodyParts, bodyParts);
        try {
            dctx.getDispatcher().runSync(x.sendMailMultiPart, newContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
    }

    /** class to create a file in memory required for sending as an attachment */
    public static class StringDataSource implements DataSource {
        private String contentType;
        private ByteArrayOutputStream contentArray;

        public StringDataSource(String content, String contentType) throws IOException {
            this.contentType = contentType;
            contentArray = new ByteArrayOutputStream();
            contentArray.write(content.getBytes(x.iso_8859_1));
            contentArray.flush();
            contentArray.close();
        }

        @Override
        public String getContentType() {
            return contentType == null ? x.application_octet_stream : contentType;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(contentArray.toByteArray());
        }

        @Override
        public String getName() {
            return x.stringDatasource;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException(x.Cannot_write_to_this_read_only_resource);
        }
    }

    /** class to create a file in memory required for sending as an attachment */
    public static class ByteArrayDataSource implements DataSource {
        private String contentType;
        private byte[] contentArray;

        public ByteArrayDataSource(byte[] content, String contentType) {
            this.contentType = contentType;
            this.contentArray = content.clone();
        }

        @Override
        public String getContentType() {
            return contentType == null ? x.application_octet_stream : contentType;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(contentArray);
        }

        @Override
        public String getName() {
            return x.ByteArrayDataSource;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException(x.Cannot_write_to_this_read_only_resource);
        }
    }
}
