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

package org.apache.ofbiz.party.communication;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.email.NotificationServices;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.CommEventContentAssocDao;
import org.apache.ofbiz.persistence.dao.CommunicationEventDao;
import org.apache.ofbiz.persistence.dao.CommunicationEventRoleDao;
import org.apache.ofbiz.persistence.dao.ContactListCommStatusDao;
import org.apache.ofbiz.persistence.dao.ContactListDao;
import org.apache.ofbiz.persistence.dao.ContactListPartyStatusDao;
import org.apache.ofbiz.persistence.dao.ContactMechDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.FtpAddressDao;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.persistence.dao.WebSiteDao;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.mail.MimeMessageWrapper;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.CommunicationEventServicesContext;
public class CommunicationEventServices {

    private static final String MODULE = CommunicationEventServices.class.getName();
    private static final String RESOURCE = x.PartyErrorUiLabels;

    public static Map<String, Object> sendCommEventAsEmail(DispatchContext ctx, CommunicationEventServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        String communicationEventId = (String) context.get(x.communicationEventId);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<Object> errorMessages = new LinkedList<>(); // used to keep a list of all error messages returned from sending emails to contact list

        try {
            // find the communication event and make sure that it is actually an email
            GenericValue communicationEvent = DaoRegistry.getDao(delegator, x.CommunicationEvent, CommunicationEventDao.class).findOneByWhere(
                    delegator, x.CommunicationEvent, UtilMisc.toMap(x.communicationEventId, communicationEventId), null, null, false);
            if (communicationEvent == null) {
                String errMsg = UtilProperties.getMessage(RESOURCE, x.commeventservices_communication_event_not_found_failure, locale);
                return ServiceUtil.returnError(errMsg + x.str_b858cb28 + communicationEventId);
            }
            String communicationEventType = communicationEvent.getString(x.communicationEventTypeId);
            if (communicationEventType == null || !(x.EMAIL_COMMUNICATION.equals(communicationEventType)
                    || x.AUTO_EMAIL_COMM.equals(communicationEventType))) {
                String errMsg = UtilProperties.getMessage(RESOURCE, x.commeventservices_communication_event_must_be_email_for_email, locale);
                return ServiceUtil.returnError(errMsg + x.str_b858cb28 + communicationEventId);
            }

            // make sure the from contact mech is an email if it is specified
            if ((communicationEvent.getRelatedOne(x.FromContactMech, false) == null)
                    || (!(x.EMAIL_ADDRESS.equals(communicationEvent.getRelatedOne(x.FromContactMech, false).getString(x.contactMechTypeId)))
                    || (communicationEvent.getRelatedOne(x.FromContactMech, false).getString(x.infoString) == null))) {
                String errMsg = UtilProperties.getMessage(RESOURCE, x.commeventservices_communication_event_from_contact_mech_must_be_email, locale);
                return ServiceUtil.returnError(errMsg + x.str_b858cb28 + communicationEventId);
            }

            // assign some default values because required by sendmail and better not make them defaults over there
            if (UtilValidate.isEmpty(communicationEvent.getString(x.subject))) {
                communicationEvent.put(x.subject, x.str_b858cb28);
            }
            if (UtilValidate.isEmpty(communicationEvent.getString(x.content))) {
                communicationEvent.put(x.content, x.str_b858cb28);
            }

            // prepare the email
            Map<String, Object> sendMailParams = new HashMap<>();
            sendMailParams.put(x.sendFrom, communicationEvent.getRelatedOne(x.FromContactMech, false).getString(x.infoString));
            sendMailParams.put(x.subject, communicationEvent.getString(x.subject));
            sendMailParams.put(x.contentType, communicationEvent.getString(x.contentMimeTypeId));
            sendMailParams.put(x.userLogin, userLogin);

            Debug.logInfo(x.Sending_communicationEvent + communicationEventId, MODULE);

            // check for attachments
            boolean isMultiPart = false;
            List<GenericValue> comEventContents = DaoRegistry.getDao(delegator, x.CommEventContentAssoc, CommEventContentAssocDao.class)
                    .findListByWhere(delegator, x.CommEventContentAssoc, UtilMisc.toMap(x.communicationEventId, communicationEventId),
                            null, null, false, true);
            if (UtilValidate.isNotEmpty(comEventContents)) {
                isMultiPart = true;
                List<Map<String, ? extends Object>> bodyParts = new LinkedList<>();
                if (UtilValidate.isNotEmpty(communicationEvent.getString(x.content))) {
                    bodyParts.add(UtilMisc.<String, Object>toMap(x.content, communicationEvent.getString(x.content), x.type,
                            communicationEvent.getString(x.contentMimeTypeId)));
                }
                for (GenericValue comEventContent : comEventContents) {
                    GenericValue content = comEventContent.getRelatedOne(x.FromContent, false);
                    GenericValue dataResource = content.getRelatedOne(x.DataResource, false);
                    ByteBuffer dataContent = DataResourceWorker.getContentAsByteBuffer(delegator, dataResource.getString(x.dataResourceId),
                            null, null, locale, null);
                    bodyParts.add(UtilMisc.<String, Object>toMap(x.content, dataContent.array(), x.type, dataResource.getString(x.mimeTypeId),
                            x.filename, dataResource.getString(x.dataResourceName)));
                }
                sendMailParams.put(x.bodyParts, bodyParts);
            } else {
                sendMailParams.put(x.body, communicationEvent.getString(x.content));
            }

            // if there is no contact list, then send look for a contactMechIdTo and partyId
            if ((UtilValidate.isEmpty(communicationEvent.getString(x.contactListId)))) {
                // send to address
                String sendTo = communicationEvent.getString(x.toString);

                if (UtilValidate.isEmpty(sendTo)) {
                    GenericValue toContactMech = communicationEvent.getRelatedOne(x.ToContactMech, false);
                    if (toContactMech != null && x.EMAIL_ADDRESS.equals(toContactMech.getString(x.contactMechTypeId))) {
                        sendTo = toContactMech.getString(x.infoString);
                    }
                }
                if (UtilValidate.isEmpty(sendTo)) {
                    String errMsg = UtilProperties.getMessage(RESOURCE, x.commeventservices_communication_event_to_contact_mech_must_be_email,
                            locale);
                    return ServiceUtil.returnError(errMsg + x.str_b858cb28 + communicationEventId);
                }

                // add other parties from roles, collect all email on map to parse it after
                List<String> alreadyLoaded = UtilMisc.toList(sendTo);
                List<String> availableRoleTypeIds = UtilMisc.toList(x.ADDRESSEE, x.CC, x.BCC);
                Map<String, Object> emailsCollector = UtilMisc.toMap(x.ADDRESSEE, UtilMisc.toList(sendTo));
                List<GenericValue> commRoles = communicationEvent.getRelated(x.CommunicationEventRole, null, null, false);
                if (UtilValidate.isNotEmpty(commRoles)) {
                    for (GenericValue commRole : commRoles) { // 'from' and 'to' already defined on communication event
                        GenericValue contactMech = commRole.getRelatedOne(x.ContactMech, false);
                        if (contactMech != null && UtilValidate.isNotEmpty(contactMech.getString(x.infoString))) {
                            String infoString = contactMech.getString(x.infoString);
                            String roleTypeId = commRole.getString(x.roleTypeId);
                            if (alreadyLoaded.contains(infoString)
                                    && !availableRoleTypeIds.contains(roleTypeId)) {
                                continue;
                            }
                            alreadyLoaded.add(infoString);
                            UtilMisc.addToListInMap(infoString, emailsCollector, roleTypeId);
                        }
                    }
                }
                sendMailParams.put(x.sendTo, String.join(x.str_5c10b5b2, UtilMisc.getListFromMap(emailsCollector, x.ADDRESSEE)));
                sendMailParams.put(x.sendCc, emailsCollector.containsKey(x.CC)
                        ? String.join(x.str_5c10b5b2, UtilMisc.getListFromMap(emailsCollector, x.CC))
                        : null);
                sendMailParams.put(x.sendBcc, emailsCollector.containsKey(x.BCC)
                        ? String.join(x.str_5c10b5b2, UtilMisc.getListFromMap(emailsCollector, x.BCC))
                        : null);

                sendMailParams.put(x.communicationEventId, communicationEventId);
                sendMailParams.put(x.partyId, communicationEvent.getString(x.partyIdTo));  // who it's going to

                // send it - using a new transaction
                Map<String, Object> tmpResult = null;
                if (isMultiPart) {
                    tmpResult = dispatcher.runSync(x.sendMailMultiPart, sendMailParams, 360, true);
                    if (ServiceUtil.isError(tmpResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
                    }
                } else {
                    tmpResult = dispatcher.runSync(x.sendMail, sendMailParams, 360, true);
                    if (ServiceUtil.isError(tmpResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
                    }
                }

                if (ServiceUtil.isError(tmpResult)) {
                    if (ServiceUtil.getErrorMessage(tmpResult).startsWith(x.ADDRERR)) {
                        // address error; mark the communication event as BOUNCED
                        communicationEvent.set(x.statusId, x.COM_BOUNCED);
                        try {
                            communicationEvent.store();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    } else {
                        // setup or communication error
                        errorMessages.add(ServiceUtil.getErrorMessage(tmpResult));
                    }
                } else {
                    // set the message ID on this communication event
                    String messageId = (String) tmpResult.get(x.messageId);
                    communicationEvent.set(x.messageId, messageId);
                    try {
                        communicationEvent.store();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    Map<String, Object> completeResult = dispatcher.runSync(x.setCommEventComplete,
                            UtilMisc.<String, Object>toMap(x.communicationEventId, communicationEventId, x.partyIdFrom, communicationEvent
                                    .getString(x.partyIdFrom), x.userLogin, userLogin));
                    if (ServiceUtil.isError(completeResult)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(completeResult));
                    }
                }

            } else {
                // Call the sendEmailToContactList service if there's a contactListId present
                Map<String, Object> sendEmailToContactListContext = new HashMap<>();
                sendEmailToContactListContext.put(x.contactListId, communicationEvent.getString(x.contactListId));
                sendEmailToContactListContext.put(x.communicationEventId, communicationEventId);
                sendEmailToContactListContext.put(x.userLogin, userLogin);
                try {
                    dispatcher.runAsync(x.sendEmailToContactList, sendEmailToContactListContext);
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(RESOURCE, x.commeventservices_errorCallingSendEmailToContactListService, locale);
                    Debug.logError(e, errMsg, MODULE);
                    errorMessages.add(errMsg);
                    errorMessages.addAll(e.getMessageList());
                }
            }
        } catch (IOException | GeneralException eey) {
            return ServiceUtil.returnError(eey.getMessage());
        }

        // If there were errors, then the result of this service should be error with the full list of messages
        if (!errorMessages.isEmpty()) {
            result = ServiceUtil.returnError(errorMessages);
        }
        return result;
    }

    /**
     * Service to send all content associated to a FILE_TRANSFER_COMM CommunicationEvent,
     * with contactMechIdTo as a FtpAdress contactMech
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> sendCommEventAsFtp(DispatchContext ctx, CommunicationEventServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String communicationEventId = (String) context.get(x.communicationEventId);
        List<String> errorMessages = new ArrayList<>();
        try {
            GenericValue communicationEvent = DaoRegistry.getDao(delegator, x.CommunicationEvent, CommunicationEventDao.class).findOneByWhere(
                    delegator, x.CommunicationEvent, UtilMisc.toMap(x.communicationEventId, communicationEventId), null, null, false);
            if (communicationEvent == null) {
                String errMsg = UtilProperties.getMessage(RESOURCE, x.commeventservices_communication_event_not_found_failure, locale);
                return ServiceUtil.returnError(errMsg + x.str_b858cb28 + communicationEventId);
            }

            if (x.COM_COMPLETE.equals(communicationEvent.getString(x.statusId))) return ServiceUtil.returnSuccess();

            String communicationEventType = communicationEvent.getString(x.communicationEventTypeId);
            if (communicationEventType == null || !x.FILE_TRANSFER_COMM.equals(communicationEventType)) {
                String errMsg = UtilProperties.getMessage(RESOURCE, x.commeventservices_communication_event_must_be_ftp_for_ftp, locale);
                return ServiceUtil.returnError(errMsg + x.str_b858cb28 + communicationEventId);
            }

            String contactMechId = communicationEvent.getString(x.contactMechIdTo);

            // Check contactMech type to FTP_ADDRESS
            GenericValue contactMech = DaoRegistry.getDao(delegator, x.ContactMech, ContactMechDao.class).findOneByWhere(delegator,
                    x.ContactMech, UtilMisc.toMap(x.contactMechId, contactMechId), null, null, true);
            GenericValue ftpAddress = DaoRegistry.getDao(delegator, x.FtpAddress, FtpAddressDao.class).findOneByWhere(delegator,
                    x.FtpAddress, UtilMisc.toMap(x.contactMechId, contactMechId), null, null, true);
            if (null == contactMech || null == ftpAddress || !x.FTP_ADDRESS.equals(contactMech.getString(x.contactMechTypeId))) {
                String errMsg = UtilProperties.getMessage(RESOURCE, x.commeventservices_communication_event_to_contact_mech_must_be_ftp, locale);
                return ServiceUtil.returnError(errMsg + x.str_b858cb28 + communicationEventId);
            }

            // Get list of children communication events, to avoid same content multi-send
            List<GenericValue> childrenCommunicationEvent = DaoRegistry.getDao(delegator, x.CommunicationEvent, CommunicationEventDao.class)
                    .findByCondition(delegator, x.CommunicationEvent, EntityCondition.makeCondition(x.parentCommEventId, communicationEventId),
                            UtilMisc.toList(x.communicationEventId, x.statusId), null, null, true);
            List<String> childrenCommunicationEventIds = EntityUtil.getFieldListFromEntityList(childrenCommunicationEvent,
                    x.communicationEventId, true);
            // Retrieve all contents to send
            List<GenericValue> contents = DaoRegistry.getDao(delegator, x.CommEventContentDataResource, UserLoginDao.class).findListByWhere(
                    delegator, x.CommEventContentDataResource, UtilMisc.toMap(x.communicationEventId, communicationEventId), null, null, true);

            if (UtilValidate.isNotEmpty(contents)) {
                if (UtilValidate.isEmpty(communicationEvent.getTimestamp(x.datetimeStarted))) {
                    //store the startDate into the communication
                    Map<String, Object> updateCommEventResult = dispatcher.runSync(x.updateCommunicationEvent,
                            UtilMisc.toMap(x.communicationEventId, communicationEventId, x.datetimeStarted, UtilDateTime.nowTimestamp(),
                                    x.userLogin, userLogin), 600, true);
                    if (ServiceUtil.isError(updateCommEventResult)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(updateCommEventResult));
                    }
                }

                for (GenericValue content : contents) {
                    Map<String, Object> ftpServiceMap = new HashMap<>();
                    //store the child Communication Event, to keep track of errorMessages in note field
                    String childCommunicationEventId = x.emptyString;
                    ftpServiceMap.put(x.userLogin, userLogin);
                    ftpServiceMap.put(x.contentId, content.getString(x.contentId));
                    ftpServiceMap.put(x.partyId, communicationEvent.getString(x.partyIdTo));
                    ftpServiceMap.put(x.contactMechId, contactMechId);
                    // no need to create a child CommEvent if it is a single content transfer
                    if (contents.size() == 1) {
                        ftpServiceMap.put(x.communicationEventId, communicationEvent.get(x.communicationEventId));
                    } else {
                        // check if currentContent is already sent by an existing children communicationEvent
                        EntityCondition sentCond = EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition(x.communicationEventId, EntityOperator.IN, childrenCommunicationEventIds),
                                EntityCondition.makeCondition(x.contentId, content.getString(x.contentId))));
                        GenericValue alreadySent = DaoRegistry.getDao(delegator, x.CommEventContentAssoc, CommEventContentAssocDao.class)
                                .findFirstByWhere(delegator, x.CommEventContentAssoc, sentCond, null, null, true);

                        if (null != alreadySent) {
                            GenericValue childCommEvent = EntityUtil.getFirst(EntityUtil.filterByCondition(childrenCommunicationEvent,
                                    EntityCondition.makeCondition(x.communicationEventId, alreadySent.getString(x.communicationEventId))));
                            // if completely sent, continue to next content
                            if (x.COM_COMPLETE.equals(childCommEvent.getString(x.statusId))) continue;
                            ftpServiceMap.put(x.communicationEventId, childCommEvent.getString(x.communicationEventId));
                        }
                    }

                    Map<String, Object> resultTmp = dispatcher.runSync(x.sendContentToFtp, ftpServiceMap, 600, true);
                    if (ServiceUtil.isError(resultTmp)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(resultTmp));
                    }

                    // attach the parent communication event to the new event created when sending the content, and store error if needed
                    if (UtilValidate.isNotEmpty(resultTmp.get(x.communicationEventId))) {
                        childCommunicationEventId = (String) resultTmp.get(x.communicationEventId);
                    }
                    if (UtilValidate.isNotEmpty(childCommunicationEventId) && !childCommunicationEventId.equals(communicationEventId)) {
                        GenericValue childCommunicationEvent = DaoRegistry.getDao(delegator, x.CommunicationEvent, CommunicationEventDao.class)
                                .findOneByWhere(delegator, x.CommunicationEvent,
                                        UtilMisc.toMap(x.communicationEventId, childCommunicationEventId), null, null, false);
                        childCommunicationEvent.set(x.parentCommEventId, communicationEventId);
                        if (ServiceUtil.isError(resultTmp)) {
                            childCommunicationEvent.set(x.statusId, x.COM_BOUNCED);
                            childCommunicationEvent.set(x.note, ServiceUtil.getErrorMessage(resultTmp));
                        }
                        childCommunicationEvent.store();
                    }
                }
            } else {
                errorMessages.add(UtilProperties.getMessage(RESOURCE, x.commeventservices_communication_event_not_without_content, locale));
            }

            if (!errorMessages.isEmpty()) {
                communicationEvent.set(x.statusId, x.COM_BOUNCED);
                communicationEvent.set(x.note, errorMessages.toString());
                communicationEvent.store();
            } else {
                //Update content status
                for (GenericValue content : contents) {
                    Map<String, Object> updateContentResult = dispatcher.runSync(x.setContentStatus, UtilMisc.<String, Object>toMap(x.contentId,
                            content.getString(x.contentId), x.statusId, x.CTNT_PUBLISHED, x.userLogin, userLogin));
                    if (ServiceUtil.isError(updateContentResult)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(updateContentResult));
                    }
                }

                Map<String, Object> completeResult = dispatcher.runSync(x.setCommEventComplete,
                        UtilMisc.<String, Object>toMap(x.communicationEventId, communicationEventId, x.userLogin, userLogin));
                if (ServiceUtil.isError(completeResult)) {
                    errorMessages.add(ServiceUtil.getErrorMessage(completeResult));
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        if (!errorMessages.isEmpty()) {
            return ServiceUtil.returnFailure(errorMessages);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> sendEmailToContactList(DispatchContext ctx, CommunicationEventServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        List<Object> errorMessages = new LinkedList<>();
        String errorCallingUpdateContactListPartyService = UtilProperties.getMessage(RESOURCE,
                x.commeventservices_errorCallingUpdateContactListPartyService, locale);
        String errorCallingSendMailService = UtilProperties.getMessage(RESOURCE, x.commeventservices_errorCallingSendMailService, locale);
        String errorInSendEmailToContactListService = UtilProperties.getMessage(RESOURCE,
                x.commeventservices_errorInSendEmailToContactListService, locale);
        String skippingInvalidEmailAddress = UtilProperties.getMessage(RESOURCE, x.commeventservices_skippingInvalidEmailAddress, locale);

        String contactListId = (String) context.get(x.contactListId);
        String communicationEventId = (String) context.get(x.communicationEventId);

        // Any exceptions thrown in this block will cause the service to return error
        try {
            GenericValue communicationEvent = DaoRegistry.getDao(delegator, x.CommunicationEvent, CommunicationEventDao.class).findOneByWhere(
                    delegator, x.CommunicationEvent, UtilMisc.toMap(x.communicationEventId, communicationEventId), null, null, false);
            GenericValue contactList = DaoRegistry.getDao(delegator, x.ContactList, ContactListDao.class).findOneByWhere(delegator,
                    x.ContactList, UtilMisc.toMap(x.contactListId, contactListId), null, null, false);

            Map<String, Object> sendMailParams = new HashMap<>();
            sendMailParams.put(x.sendFrom, communicationEvent.getRelatedOne(x.FromContactMech, false).getString(x.infoString));
            sendMailParams.put(x.subject, communicationEvent.getString(x.subject));
            sendMailParams.put(x.contentType, communicationEvent.getString(x.contentMimeTypeId));
            sendMailParams.put(x.userLogin, userLogin);

            // Find a list of distinct email addresses from active, ACCEPTED parties in the contact list
            //      using a list iterator (because there can be a large number)
            List<EntityCondition> conditionList = UtilMisc.toList(
                        EntityCondition.makeCondition(x.contactListId, EntityOperator.EQUALS, contactList.get(x.contactListId)),
                        EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.CLPT_ACCEPTED),
                        EntityCondition.makeCondition(x.preferredContactMechId, EntityOperator.NOT_EQUAL, null),
                        EntityUtil.getFilterByDateExpr(), EntityUtil.getFilterByDateExpr(x.contactFromDate, x.contactThruDate));

            EntityCondition contactListPartyAndContactMechCondition = EntityCondition.makeCondition(conditionList, EntityOperator.AND);
            EntityFindOptions contactListPartyAndContactMechFindOptions = new EntityFindOptions();
            contactListPartyAndContactMechFindOptions.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);
            contactListPartyAndContactMechFindOptions.setDistinct(true);
            try (EntityListIterator eli = DaoRegistry.getDao(delegator, x.ContactListPartyAndContactMech, UserLoginDao.class)
                    .findIteratorByCondition(delegator, x.ContactListPartyAndContactMech, contactListPartyAndContactMechCondition,
                            UtilMisc.toList(x.partyId, x.preferredContactMechId, x.fromDate, x.infoString), null,
                            contactListPartyAndContactMechFindOptions)) {
                // Send an email to each contact list member
                // loop through the list iterator
                // CHECKSTYLE_OFF: ALMOST_ALL
                for (GenericValue contactListPartyAndContactMech; (contactListPartyAndContactMech = eli.next()) != null;) {
                    // CHECKSTYLE_N: ALMOST_ALL
                    Debug.logInfo(x.Contact_info + contactListPartyAndContactMech, MODULE);
                    // Any exceptions thrown in this inner block will only relate to a single email of the list, so should
                    //  only be logged and not cause the service to return an error
                    try {

                        String emailAddress = contactListPartyAndContactMech.getString(x.infoString);
                        if (UtilValidate.isEmpty(emailAddress)) {
                            continue;
                        }
                        emailAddress = emailAddress.trim();

                        if (!UtilValidate.isEmail(emailAddress)) {

                            // If validation fails, just log and skip the email address
                            Debug.logError(skippingInvalidEmailAddress + x.str_ceca32e9 + emailAddress, MODULE);
                            errorMessages.add(skippingInvalidEmailAddress + x.str_ceca32e9 + emailAddress);
                            continue;
                        }

                        // Because we're retrieving infoString only above (so as not to pollute the distinctness), we
                        //      need to retrieve the partyId it's related to. Since this could be multiple parties, get
                        //      only the most recent valid one via ContactListPartyAndContactMech.
                        List<EntityCondition> clpConditionList = UtilMisc.makeListWritable(conditionList);
                        clpConditionList.add(EntityCondition.makeCondition(x.infoString, EntityOperator.EQUALS, emailAddress));

                        List<GenericValue> lastContactListPartyACMList = DaoRegistry.getDao(delegator, x.ContactListPartyAndContactMech,
                                UserLoginDao.class).findListByWhere(delegator, x.ContactListPartyAndContactMech,
                                        EntityCondition.makeCondition(clpConditionList, EntityOperator.AND), null,
                                        UtilMisc.toList(x.fromDate_f5440273), true);
                        GenericValue lastContactListPartyACM = EntityUtil.getFirst(lastContactListPartyACMList);
                        if (lastContactListPartyACM == null) {
                            continue;
                        }

                        String partyId = lastContactListPartyACM.getString(x.partyId);

                        sendMailParams.put(x.sendTo, emailAddress);
                        sendMailParams.put(x.partyId, partyId);

                        // Retrieve a record for this contactMechId from ContactListCommStatus
                        Map<String, String> contactListCommStatusRecordMap = UtilMisc.toMap(x.contactListId, contactListId, x.communicationEventId,
                                communicationEventId, x.contactMechId, lastContactListPartyACM.getString(x.preferredContactMechId));
                        GenericValue contactListCommStatusRecord = DaoRegistry.getDao(delegator, x.ContactListCommStatus,
                                ContactListCommStatusDao.class).findOneByWhere(delegator, x.ContactListCommStatus,
                                        contactListCommStatusRecordMap, null, null, false);
                        if (contactListCommStatusRecord == null) {

                            // No attempt has been made previously to send to this address, so create a record to reflect
                            //  the beginning of the current attempt
                            Map<String, String> newContactListCommStatusRecordMap = UtilMisc.makeMapWritable(contactListCommStatusRecordMap);
                            newContactListCommStatusRecordMap.put(x.statusId, x.COM_IN_PROGRESS);
                            newContactListCommStatusRecordMap.put(x.partyId, partyId);
                            contactListCommStatusRecord = delegator.create(x.ContactListCommStatus, newContactListCommStatusRecordMap);
                        } else if (contactListCommStatusRecord.get(x.statusId) != null && x.COM_COMPLETE
                                .equals(contactListCommStatusRecord.getString(x.statusId))) {

                            // There was a successful earlier attempt, so skip this address
                            continue;
                        }

                        // Send e-mail
                        Debug.logInfo(x.Sending_email_to_contact_list + contactListId + x.party_e47152f3 + partyId + x.str_d7231b41 + emailAddress, MODULE);
                        // Make the attempt to send the email to the address

                        Map<String, Object> tmpResult = null;

                        // Retrieve a contact list party status
                        GenericValue contactListPartyStatus = DaoRegistry.getDao(delegator, x.ContactListPartyStatus,
                                ContactListPartyStatusDao.class).findFirstByWhere(delegator, x.ContactListPartyStatus,
                                        UtilMisc.toMap(x.contactListId, contactListId, x.partyId,
                                                contactListPartyAndContactMech.getString(x.partyId), x.fromDate,
                                                contactListPartyAndContactMech.getTimestamp(x.fromDate), x.statusId, x.CLPT_ACCEPTED),
                                        null, null, false);
                        if (contactListPartyStatus != null) {
                            // prepare body parameters
                            Map<String, Object> bodyParameters = new HashMap<>();
                            bodyParameters.put(x.contactListId, contactListId);
                            bodyParameters.put(x.partyId, contactListPartyAndContactMech.getString(x.partyId));
                            bodyParameters.put(x.preferredContactMechId, contactListPartyAndContactMech.getString(x.preferredContactMechId));
                            bodyParameters.put(x.emailAddress, emailAddress);
                            bodyParameters.put(x.fromDate, contactListPartyAndContactMech.getTimestamp(x.fromDate));
                            bodyParameters.put(x.optInVerifyCode, contactListPartyStatus.getString(x.optInVerifyCode));
                            bodyParameters.put(x.content, communicationEvent.getString(x.content));
                            NotificationServices.setBaseUrl(delegator, contactList.getString(x.verifyEmailWebSiteId), bodyParameters);

                            GenericValue webSite = DaoRegistry.getDao(delegator, x.WebSite, WebSiteDao.class).findOneByWhere(delegator,
                                    x.WebSite, UtilMisc.toMap(x.webSiteId, contactList.getString(x.verifyEmailWebSiteId)), null, null,
                                    false);
                            if (webSite != null) {
                                GenericValue productStore = webSite.getRelatedOne(x.ProductStore, false);
                                if (productStore != null) {
                                    List<GenericValue> productStoreEmailSettings = productStore.getRelated(x.ProductStoreEmailSetting,
                                            UtilMisc.toMap(x.emailType, x.CONT_EMAIL_TEMPLATE), null, false);
                                    GenericValue productStoreEmailSetting = EntityUtil.getFirst(productStoreEmailSettings);
                                    if (productStoreEmailSetting != null) {
                                        // send e-mail using screen template
                                        sendMailParams.put(x.bodyScreenUri, productStoreEmailSetting.getString(x.bodyScreenLocation));
                                        sendMailParams.put(x.bodyParameters, bodyParameters);
                                        sendMailParams.remove(x.body);
                                        tmpResult = dispatcher.runSync(x.sendMailFromScreen, sendMailParams, 360, true);
                                        if (ServiceUtil.isError(tmpResult)) {
                                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
                                        }
                                    }
                                }
                            }
                        }

                        // If the e-mail does not be sent then send normal e-mail
                        if (UtilValidate.isEmpty(tmpResult)) {
                            sendMailParams.put(x.body, communicationEvent.getString(x.content));
                            tmpResult = dispatcher.runSync(x.sendMail, sendMailParams, 360, true);
                            if (ServiceUtil.isError(tmpResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
                            }
                        }

                        if (tmpResult == null || ServiceUtil.isError(tmpResult)) {
                            if (tmpResult != null && ServiceUtil.getErrorMessage(tmpResult).startsWith(x.ADDRERR)) {
                                // address error; mark the communication event as BOUNCED
                                contactListCommStatusRecord.set(x.statusId, x.COM_BOUNCED);
                                try {
                                    contactListCommStatusRecord.store();
                                } catch (GenericEntityException e) {
                                    Debug.logError(e, MODULE);
                                    errorMessages.add(e.getMessage());
                                }
                                // deactivate from the contact list
                                try {
                                    GenericValue contactListParty = contactListPartyAndContactMech.getRelatedOne(x.ContactListParty, false);
                                    if (contactListParty != null) {
                                        contactListParty.set(x.statusId, x.CLPT_INVALID);
                                        contactListParty.store();
                                    }
                                } catch (GenericEntityException e) {
                                    Debug.logError(e, MODULE);
                                    errorMessages.add(e.getMessage());
                                }
                                continue;
                            }
                            // If the send attempt fails, just log and skip the email address
                            if (tmpResult != null) {
                                Debug.logError(errorCallingSendMailService + x.str_ceca32e9 + ServiceUtil.getErrorMessage(tmpResult), MODULE);
                                errorMessages.add(errorCallingSendMailService + x.str_ceca32e9 + ServiceUtil.getErrorMessage(tmpResult));
                                continue;
                            }
                        }
                        // attach the parent communication event to the new event created when sending
                        // the mail
                        if (tmpResult != null) {
                            String thisCommEventId = (String) tmpResult.get(x.communicationEventId);
                            GenericValue thisCommEvent = DaoRegistry.getDao(delegator, x.CommunicationEvent, CommunicationEventDao.class)
                                    .findOneByWhere(delegator, x.CommunicationEvent,
                                            UtilMisc.toMap(x.communicationEventId, thisCommEventId), null, null, false);
                            if (thisCommEvent != null) {
                                thisCommEvent.set(x.contactListId, contactListId);
                                thisCommEvent.set(x.parentCommEventId, communicationEventId);
                                thisCommEvent.store();
                            }
                            String messageId = (String) tmpResult.get(x.messageId);
                            contactListCommStatusRecord.set(x.messageId, messageId);

                            if (x.Y.equals(contactList.get(x.singleUse))) {
                                // Expire the ContactListParty if the list is single use and sendEmail finishes successfully
                                tmpResult = dispatcher.runSync(x.updateContactListParty, UtilMisc.toMap(x.contactListId,
                                        lastContactListPartyACM.get(x.contactListId),
                                        x.partyId, partyId, x.fromDate, lastContactListPartyACM.get(x.fromDate),
                                        x.thruDate, UtilDateTime.nowTimestamp(), x.userLogin, userLogin));
                                if (ServiceUtil.isError(tmpResult)) {

                                    // If the expiry fails, just log and skip the email address
                                    Debug.logError(errorCallingUpdateContactListPartyService + x.str_ceca32e9 + ServiceUtil.getErrorMessage(tmpResult), MODULE);
                                    errorMessages.add(errorCallingUpdateContactListPartyService + x.str_ceca32e9 + ServiceUtil.getErrorMessage(tmpResult));
                                    continue;
                                }
                            }
                        }

                        // All is successful, so update the ContactListCommStatus record
                        contactListCommStatusRecord.set(x.statusId, x.COM_COMPLETE);
                        delegator.store(contactListCommStatusRecord);

                        // Don't return a service error just because of failure for one address - just log the error and continue
                    } catch (GenericEntityException | GenericServiceException nonFatalGEE) {
                        Debug.logError(nonFatalGEE, errorInSendEmailToContactListService, MODULE);
                        errorMessages.add(errorInSendEmailToContactListService + x.str_ceca32e9 + nonFatalGEE.getMessage());
                    }
                }
            } catch (GenericEntityException fatalGEE) {
                return ServiceUtil.returnError(fatalGEE.getMessage());
            }

        } catch (GenericEntityException fatalGEE) {
            return ServiceUtil.returnError(fatalGEE.getMessage());
        }

        return errorMessages.isEmpty() ? ServiceUtil.returnSuccess() : ServiceUtil.returnError(errorMessages);
    }

    public static Map<String, Object> setCommEventComplete(DispatchContext dctx, CommunicationEventServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String communicationEventId = (String) context.get(x.communicationEventId);
        String partyIdFrom = (String) context.get(x.partyIdFrom);

        try {
            GenericValue communicationEvent = DaoRegistry.getDao(delegator, x.CommunicationEvent, CommunicationEventDao.class).findOneByWhere(
                    delegator, x.CommunicationEvent, UtilMisc.toMap(x.communicationEventId, communicationEventId), null, null, true);
            if (communicationEvent == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(x.PartyUiLabels, x.PartyCommunicationEventNotFound,
                        UtilMisc.toMap(x.communicationEventId, communicationEventId), (Locale) context.get(x.locale)));
            }
            Timestamp endDate = communicationEvent.getTimestamp(x.datetimeEnded);
            if (endDate == null) {
                endDate = UtilDateTime.nowTimestamp();
            }
            Map<String, Object> result = dispatcher.runSync(x.updateCommunicationEvent, UtilMisc.<String, Object>toMap(x.communicationEventId,
                    communicationEventId, x.partyIdFrom, partyIdFrom, x.statusId, x.COM_COMPLETE, x.datetimeEnded, endDate, x.userLogin, userLogin));
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GeneralException esx) {
            return ServiceUtil.returnError(esx.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /*
     * Store an outgoing file transfer as a communication event;
     * runs as a pre-invoke ECA on sendContentToFtp service
     * - service should run as the 'system' user
     */
    public static Map<String, Object> createCommEventFromFtpTransfer(DispatchContext dctx, CommunicationEventServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String contentId = (String) context.get(x.contentId);
        String contactMechId = (String) context.get(x.contactMechId);
        String partyId = (String) context.get(x.partyId);
        String communicationEventId;

        Timestamp now = UtilDateTime.nowTimestamp();

        Map<String, Object> commEventMap = new HashMap<>();
        commEventMap.put(x.communicationEventTypeId, x.FILE_TRANSFER_COMM);
        commEventMap.put(x.contactMechTypeId, x.FTP_ADDRESS);
        commEventMap.put(x.contactMechIdTo, contactMechId);
        commEventMap.put(x.statusId, x.COM_PENDING);
        commEventMap.put(x.datetimeStarted, now);
        commEventMap.put(x.entryDate, now);
        commEventMap.put(x.userLogin, userLogin);
        if (UtilValidate.isNotEmpty(partyId)) {
            commEventMap.put(x.partyIdTo, partyId);
        }

        Map<String, Object> createResult;
        try {
            createResult = dispatcher.runSync(x.createCommunicationEvent, commEventMap);
            if (ServiceUtil.isError(createResult)) {
                return createResult;
            }
            communicationEventId = (String) createResult.get(x.communicationEventId);

            //add content to newly created commEvent
            Map<String, Object> createCommEventContentMap = new HashMap<>();
            createCommEventContentMap.put(x.userLogin, userLogin);
            createCommEventContentMap.put(x.contentId, contentId);
            createCommEventContentMap.put(x.communicationEventId, communicationEventId);
            createResult = dispatcher.runSync(x.createCommEventContentAssoc, createCommEventContentMap);
            if (ServiceUtil.isError(createResult)) {
                return createResult;
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.communicationEventId, communicationEventId);
        return result;
    }

    /*
     * Store an outgoing email as a communication event;
     * runs as a pre-invoke ECA on sendMail and sendMultipartMail services
     * - service should run as the 'system' user
     */
    public static Map<String, Object> createCommEventFromEmail(DispatchContext dctx, CommunicationEventServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String subject = (String) context.get(x.subject);
        String sendFrom = (String) context.get(x.sendFrom);
        String sendTo = (String) context.get(x.sendTo);
        String partyId = (String) context.get(x.partyId);
        String contentType = (String) context.get(x.contentType);
        String statusId = (String) context.get(x.statusId);
        String orderId = (String) context.get(x.orderId);
        String returnId = (String) context.get(x.returnId);
        if (statusId == null) {
            statusId = x.COM_PENDING;
        }

        // get the from contact mech info
        String contactMechIdFrom = null;
        String partyIdFrom = null;
        GenericValue fromCm;
        try {
            List<GenericValue> fromCmList = DaoRegistry.getDao(delegator, x.PartyAndContactMech, UserLoginDao.class).findListByWhere(
                    delegator, x.PartyAndContactMech, UtilMisc.toMap(x.infoString, sendFrom), null, UtilMisc.toList(x.fromDate_f5440273), false,
                    true);
            fromCm = EntityUtil.getFirst(fromCmList);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (fromCm != null) {
            contactMechIdFrom = fromCm.getString(x.contactMechId);
            partyIdFrom = fromCm.getString(x.partyId);
        }

        // get the to contact mech info
        String contactMechIdTo = null;
        GenericValue toCm;
        try {
            List<GenericValue> toCmList = DaoRegistry.getDao(delegator, x.PartyAndContactMech, UserLoginDao.class).findListByWhere(
                    delegator, x.PartyAndContactMech, UtilMisc.toMap(x.infoString, sendTo, x.partyId, partyId), null,
                    UtilMisc.toList(x.fromDate_f5440273), false, true);
            toCm = EntityUtil.getFirst(toCmList);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (toCm != null) {
            contactMechIdTo = toCm.getString(x.contactMechId);
        }

        Timestamp now = UtilDateTime.nowTimestamp();

        Map<String, Object> commEventMap = new HashMap<>();
        commEventMap.put(x.communicationEventTypeId, x.EMAIL_COMMUNICATION);
        commEventMap.put(x.contactMechTypeId, x.EMAIL_ADDRESS);
        commEventMap.put(x.contactMechIdFrom, contactMechIdFrom);
        commEventMap.put(x.contactMechIdTo, contactMechIdTo);
        commEventMap.put(x.statusId, statusId);

        commEventMap.put(x.partyIdFrom, partyIdFrom);
        commEventMap.put(x.partyIdTo, partyId);
        commEventMap.put(x.datetimeStarted, now);
        commEventMap.put(x.entryDate, now);

        commEventMap.put(x.subject, subject);
        commEventMap.put(x.userLogin, userLogin);
        commEventMap.put(x.contentMimeTypeId, contentType);
        if (UtilValidate.isNotEmpty(orderId)) {
            commEventMap.put(x.orderId, orderId);
        }
        if (UtilValidate.isNotEmpty(returnId)) {
            commEventMap.put(x.returnId, returnId);
        }

        Map<String, Object> createResult;
        try {
            createResult = dispatcher.runSync(x.createCommunicationEvent, commEventMap);
            if (ServiceUtil.isError(createResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(createResult)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createResult));
        }
        String communicationEventId = (String) createResult.get(x.communicationEventId);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.communicationEventId, communicationEventId);
        return result;
    }

    /*
     * Update the communication event with information from the email;
     * runs as a post-commit ECA on sendMail and sendMultiPartMail services
     * - service should run as the 'system' user
     */
    public static Map<String, Object> updateCommEventAfterEmail(DispatchContext dctx, CommunicationEventServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String communicationEventId = (String) context.get(x.communicationEventId);
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get(x.messageWrapper);

        Map<String, Object> commEventMap = new HashMap<>();
        commEventMap.put(x.communicationEventId, communicationEventId);
        commEventMap.put(x.subject, wrapper.getSubject());
        commEventMap.put(x.statusId, x.COM_COMPLETE);
        commEventMap.put(x.datetimeEnded, UtilDateTime.nowTimestamp());
        commEventMap.put(x.entryDate, wrapper.getSentDate());
        commEventMap.put(x.messageId, wrapper.getMessageId());
        commEventMap.put(x.userLogin, userLogin);
        commEventMap.put(x.content, wrapper.getMessageBody());

        // populate the address (to/from/cc/bcc) data
        populateAddressesFromMessage(wrapper, commEventMap);

        // save the communication event
        try {
            Map<String, Object> result = dispatcher.runSync(x.updateCommunicationEvent, commEventMap);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // attachments
        try {
            createAttachmentContent(dispatcher, dctx.getDelegator(), wrapper, communicationEventId, userLogin);
        } catch (GenericServiceException | GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * This service is the main one for processing incoming emails.
     * Its only argument is a wrapper for the JavaMail MimeMessage object.
     * From this object, all the fields, headers and content of the message can be accessed.
     * The first thing this service does is try to discover the partyId of the message sender
     * by doing a reverse find on the email address. It uses the findPartyFromEmailAddress service to do this.
     * It then creates a CommunicationEvent entity by calling the createCommunicationEvent service using the appropriate fields from the email and the
     * discovered partyId, if it exists, as the partyIdFrom. Note that it sets the communicationEventTypeId
     * field to AUTO_EMAIL_COMM. This is useful for tracking email generated communications.
     * The service tries to find appropriate content for inclusion in the CommunicationEvent.content field.
     * If the contentType of the content starts with "text", the getContent() call returns a string and it is used.
     * If the contentType starts with "multipart", then the "parts" of the content are iterated thru and the first
     * one of mime type, "text/..." is used.
     * If the contentType has a value of "multipart" then the parts of the content (except the one used in the main
     * CommunicationEvent.content field) are cycled thru and attached to the CommunicationEvent entity using the
     * createCommContentDataResource service. This happens in the EmailWorker.addAttachmentsToCommEvent method.
     * However multiparts can contain multiparts. A recursive function has been added.
     * -Al Byers - Hans Bakker
     * @param dctx the dispatch context
     * @param context the context
     * @return returns the result of the service execution
     */
    public static Map<String, Object> storeIncomingEmail(DispatchContext dctx, CommunicationEventServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get(x.messageWrapper);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        String partyIdTo = null;
        String partyIdFrom = null;
        String communicationEventId = null;
        String contactMechIdFrom = null;
        String contactMechIdTo = null;

        Map<String, Object> result = null;
        try {
            Address[] addressesFrom = wrapper.getFrom();
            Address[] addressesTo = wrapper.getTo();
            Address[] addressesCC = wrapper.getCc();
            Address[] addressesBCC = wrapper.getBcc();
            String messageId = wrapper.getMessageId().replaceAll(x.str_f94f378e, x.emptyString);

            String aboutThisEmail = x.message_4cffa9ef + messageId + x._from_22fce458
                    + ((addressesFrom == null || addressesFrom[0] == null) ? x.not_found_094b763b : addressesFrom[0].toString()) + x.to_23757279
                    + ((addressesTo == null || addressesTo[0] == null) ? x.not_found_094b763b : addressesTo[0].toString()) + x.str_4ff447b8;

            if (Debug.verboseOn()) {
                Debug.logVerbose(x.Processing_Incoming_Email + aboutThisEmail, MODULE);
            }

            // ignore the message when the spam status = yes
            String spamHeaderName = EntityUtilProperties.getPropertyValue(x.general, x.mail_spam_name, x.N, delegator);
            String configHeaderValue = EntityUtilProperties.getPropertyValue(x.general, x.mail_spam_value, delegator);
            //          only execute when config file has been set && header variable found
            if (!x.N.equals(spamHeaderName) && wrapper.getHeader(spamHeaderName) != null && wrapper.getHeader(spamHeaderName).length > 0) {
                String msgHeaderValue = wrapper.getHeader(spamHeaderName)[0];
                if (msgHeaderValue != null && msgHeaderValue.startsWith(configHeaderValue)) {
                    Debug.logInfo(x.Incoming_Email_message_ignored_was_detected_by_external_spam_checker, MODULE);
                    return ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                            x.PartyCommEventMessageIgnoredDetectedByExternalSpamChecker, locale));
                }
            }

            // if no 'from' addresses specified ignore the message
            if (addressesFrom == null) {
                Debug.logInfo(x.Incoming_Email_message_ignored_had_not_from_email_address, MODULE);
                return ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                        x.PartyCommEventMessageIgnoredNoFromAddressSpecified, locale));
            }

            // make sure this isn't a duplicate
            List<GenericValue> commEvents;
            try {
                commEvents = DaoRegistry.getDao(delegator, x.CommunicationEvent, CommunicationEventDao.class).findListByWhere(delegator,
                        x.CommunicationEvent, UtilMisc.toMap(x.messageId, messageId), null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (!commEvents.isEmpty()) {
                Debug.logInfo(x.Ignoring_Duplicate_Email + aboutThisEmail, MODULE);
                return ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                        x.PartyCommEventMessageIgnoredDuplicateMessageId, locale));
            }

            // get the related partId's
            List<Map<String, Object>> toParties = buildListOfPartyInfoFromEmailAddresses(addressesTo, userLogin, dispatcher);
            List<Map<String, Object>> ccParties = buildListOfPartyInfoFromEmailAddresses(addressesCC, userLogin, dispatcher);
            List<Map<String, Object>> bccParties = buildListOfPartyInfoFromEmailAddresses(addressesBCC, userLogin, dispatcher);

            //Get the first address from the list - this is the partyIdTo field of the CommunicationEvent
            if (!toParties.isEmpty()) {
                Map<String, Object> firstAddressTo = toParties.get(0);
                partyIdTo = (String) firstAddressTo.get(x.partyId);
                contactMechIdTo = (String) firstAddressTo.get(x.contactMechId);
            }

            String deliveredTo = wrapper.getFirstHeader(x.Delivered_To);
            if (deliveredTo != null) {
                // check if started with the domain name if yes remove including the dash.
                String dn = deliveredTo.substring(deliveredTo.indexOf('@') + 1, deliveredTo.length());
                if (deliveredTo.startsWith(dn)) {
                    deliveredTo = deliveredTo.substring(dn.length() + 1, deliveredTo.length());
                }
            }

            // if partyIdTo not found try to find the "to" address using the delivered-to header
            if ((partyIdTo == null) && (deliveredTo != null)) {
                result = dispatcher.runSync(x.findPartyFromEmailAddress, UtilMisc.<String, Object>toMap(x.address,
                        deliveredTo, x.userLogin, userLogin));
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
                partyIdTo = (String) result.get(x.partyId);
                contactMechIdTo = (String) result.get(x.contactMechId);
            }
            if (userLogin.get(x.partyId) == null && partyIdTo != null) {
                int ch = 0;
                for (ch = partyIdTo.length(); ch > 0 && Character.isDigit(partyIdTo.charAt(ch - 1)); ch--) {
                    Debug.log(x.Increase_partyIdTo_string_to_create_a_prefix, MODULE);
                }
                userLogin.put(x.partyId, partyIdTo.substring(0, ch)); //allow services to be called to have prefix
            }

            // get the 'from' partyId
            result = getParyInfoFromEmailAddress(addressesFrom, userLogin, dispatcher);
            partyIdFrom = (String) result.get(x.partyId);
            contactMechIdFrom = (String) result.get(x.contactMechId);

            Map<String, Object> commEventMap = new HashMap<>();
            commEventMap.put(x.communicationEventTypeId, x.AUTO_EMAIL_COMM);
            commEventMap.put(x.contactMechTypeId, x.EMAIL_ADDRESS);
            commEventMap.put(x.messageId, messageId);

            String subject = wrapper.getSubject();
            commEventMap.put(x.subject, subject);

            // Set sent and received dates
            commEventMap.put(x.entryDate, nowTimestamp);
            commEventMap.put(x.datetimeStarted, UtilDateTime.toTimestamp(wrapper.getSentDate()));
            commEventMap.put(x.datetimeEnded, UtilDateTime.toTimestamp(wrapper.getReceivedDate()));

            // default role types (_NA_)
            commEventMap.put(x.roleTypeIdFrom, x.NA);
            commEventMap.put(x.roleTypeIdTo, x.NA);

            // get the content(type) part
            String messageBodyContentType = wrapper.getMessageBodyContentType();
            if (messageBodyContentType.indexOf(';') > -1) {
                messageBodyContentType = messageBodyContentType.substring(0, messageBodyContentType.indexOf(';'));
            }

            // select the plain text bodypart
            String messageBody = null;
            if (wrapper.getMainPartCount() > 1) {
                for (int ind = 0; ind < wrapper.getMainPartCount(); ind++) {
                    BodyPart p = wrapper.getPart(ind + x.emptyString);
                    if (p.getContentType().toLowerCase(Locale.getDefault()).indexOf(x.text_plain) > -1) {
                        messageBody = (String) p.getContent();
                        break;
                    }
                }
            }

            if (messageBody == null) {
                messageBody = wrapper.getMessageBody();
            }

            commEventMap.put(x.content, messageBody);
            commEventMap.put(x.contentMimeTypeId, messageBodyContentType.toLowerCase(Locale.getDefault()));

            // check for for a reply to communication event (using in-reply-to the parent messageID)
            String[] inReplyTo = wrapper.getHeader(x.In_Reply_To);
            if (inReplyTo != null && inReplyTo[0] != null) {
                GenericValue parentCommEvent = null;
                try {
                    parentCommEvent = DaoRegistry.getDao(delegator, x.CommunicationEvent, CommunicationEventDao.class).findFirstByWhere(
                            delegator, x.CommunicationEvent, UtilMisc.toMap(x.messageId, inReplyTo[0].replaceAll(x.str_f94f378e, x.emptyString)), null, null,
                            false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
                if (parentCommEvent != null) {
                    String parentCommEventId = parentCommEvent.getString(x.communicationEventId);
                    String orgCommEventId = parentCommEvent.getString(x.origCommEventId);
                    if (orgCommEventId == null) {
                        orgCommEventId = parentCommEventId;
                    }
                    commEventMap.put(x.parentCommEventId, parentCommEventId);
                    commEventMap.put(x.origCommEventId, orgCommEventId);
                }
            }

            // populate the address (to/from/cc/bcc) data
            populateAddressesFromMessage(wrapper, commEventMap);

            // store from/to parties, but when not found make a note of the email to/from address in the workEffort Note Section.
            String commNote = x.emptyString;
            if (partyIdFrom != null) {
                commEventMap.put(x.partyIdFrom, partyIdFrom);
                commEventMap.put(x.contactMechIdFrom, contactMechIdFrom);
            } else {
                commNote += x.Sent_from + ((InternetAddress) addressesFrom[0]).getAddress() + x.str_d2d58684;
                commNote += x.Sent_Name_from + ((InternetAddress) addressesFrom[0]).getPersonal() + x.str_d2d58684;
            }

            if (partyIdTo != null) {
                commEventMap.put(x.partyIdTo, partyIdTo);
                commEventMap.put(x.contactMechIdTo, contactMechIdTo);
            } else {
                commNote += x.Sent_to + ((InternetAddress) addressesTo[0]).getAddress() + x.str_d2d58684;
                if (deliveredTo != null) {
                    commNote += x.Delivered_To_e8e69091 + deliveredTo + x.str_d2d58684;
                }
            }

            commNote += x.Sent_to + ((InternetAddress) addressesTo[0]).getAddress() + x.str_d2d58684;
            commNote += x.Delivered_To_e8e69091 + deliveredTo + x.str_d2d58684;

            if (partyIdTo != null && partyIdFrom != null) {
                commEventMap.put(x.statusId, x.COM_ENTERED);
            } else {
                commEventMap.put(x.statusId, x.COM_UNKNOWN_PARTY);
            }
            if (commNote.length() > 255) {
                commNote = commNote.substring(0, 255);
            }

            if (!(x.emptyString.equals(commNote))) {
                commEventMap.put(x.note, commNote);
            }

            commEventMap.put(x.userLogin, userLogin);

            // Populate the CommunicationEvent.headerString field with the email headers
            StringBuilder headerString = new StringBuilder();
            Enumeration<?> headerLines = wrapper.getMessage().getAllHeaderLines();
            while (headerLines.hasMoreElements()) {
                headerString.append(System.getProperty(x.line_separator));
                headerString.append(headerLines.nextElement());
            }
            String header = headerString.toString();
            commEventMap.put(x.headerString, header.replaceAll(x.str_f94f378e, x.emptyString));

            result = dispatcher.runSync(x.createCommunicationEvent, commEventMap);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
            communicationEventId = (String) result.get(x.communicationEventId);
            Debug.logInfo(x.Persisting_New_Email + aboutThisEmail + x.into_CommunicationEventId + communicationEventId, MODULE);

            // handle the attachments
            createAttachmentContent(dispatcher, delegator, wrapper, communicationEventId, userLogin);

            // For all addresses create a CommunicationEventRoles
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, toParties, x.ADDRESSEE);
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, ccParties, x.CC);
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, bccParties, x.BCC);

            // get the related work effort info
            List<Map<String, Object>> toWorkEffortInfos = buildListOfWorkEffortInfoFromEmailAddresses(addressesTo, userLogin, dispatcher);
            List<Map<String, Object>> ccWorkEffortInfos = buildListOfWorkEffortInfoFromEmailAddresses(addressesCC, userLogin, dispatcher);
            List<Map<String, Object>> bccWorkEffortInfos = buildListOfWorkEffortInfoFromEmailAddresses(addressesBCC, userLogin, dispatcher);

            // For all WorkEffort addresses create a CommunicationEventWorkEffs
            createCommunicationEventWorkEffs(userLogin, dispatcher, toWorkEffortInfos, communicationEventId);
            createCommunicationEventWorkEffs(userLogin, dispatcher, ccWorkEffortInfos, communicationEventId);
            createCommunicationEventWorkEffs(userLogin, dispatcher, bccWorkEffortInfos, communicationEventId);

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put(x.communicationEventId, communicationEventId);
            results.put(x.statusId, commEventMap.get(x.statusId));
            return results;
        } catch (MessagingException | GenericServiceException | GenericEntityException | IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    private static void populateAddressesFromMessage(MimeMessageWrapper wrapper, Map<String, Object> commEventMap) {
        // Retrieve all the addresses from the email
        Address[] addressesFrom = wrapper.getFrom();
        Address[] addressesTo = wrapper.getTo();
        Address[] addressesCC = wrapper.getCc();
        Address[] addressesBCC = wrapper.getBcc();

        Set<String> emailAddressesFrom = new TreeSet<>();
        Set<String> emailAddressesTo = new TreeSet<>();
        Set<String> emailAddressesCC = new TreeSet<>();
        Set<String> emailAddressesBCC = new TreeSet<>();
        for (Address element : addressesFrom) {
            emailAddressesFrom.add(((InternetAddress) element).getAddress());
        }
        for (Address element : addressesTo) {
            emailAddressesTo.add(((InternetAddress) element).getAddress());
        }
        if (addressesCC != null) {
            for (Address element : addressesCC) {
                emailAddressesCC.add(((InternetAddress) element).getAddress());
            }
        }
        if (addressesBCC != null) {
            for (Address element : addressesBCC) {
                emailAddressesBCC.add(((InternetAddress) element).getAddress());
            }
        }
        String fromString = StringUtil.join(emailAddressesFrom, x.str_5c10b5b2);
        String toString = StringUtil.join(emailAddressesTo, x.str_5c10b5b2);
        String ccString = StringUtil.join(emailAddressesCC, x.str_5c10b5b2);
        String bccString = StringUtil.join(emailAddressesBCC, x.str_5c10b5b2);

        if (UtilValidate.isNotEmpty(fromString)) {
            commEventMap.put(x.fromString, fromString);
        }
        if (UtilValidate.isNotEmpty(toString)) {
            commEventMap.put(x.toString, toString);
        }
        if (UtilValidate.isNotEmpty(ccString)) {
            commEventMap.put(x.ccString, ccString);
        }
        if (UtilValidate.isNotEmpty(bccString)) {
            commEventMap.put(x.bccString, bccString);
        }
    }

    private static List<String> getCommEventAttachmentNames(final Delegator delegator, final String communicationEventId)
            throws GenericEntityException {
        List<GenericValue> commEventContentAssocList = DaoRegistry.getDao(delegator, x.CommEventContentDataResource, UserLoginDao.class)
                .findListByWhere(delegator, x.CommEventContentDataResource,
                        EntityCondition.makeCondition(x.communicationEventId, communicationEventId), null, null, false, true);

        List<String> attachmentNames = new ArrayList<>();
        for (GenericValue commEventContentAssoc : commEventContentAssocList) {
            String dataResourceName = commEventContentAssoc.getString(x.drDataResourceName);
            attachmentNames.add(dataResourceName);
        }

        return attachmentNames;
    }

    private static void createAttachmentContent(LocalDispatcher dispatcher, Delegator delegator, MimeMessageWrapper wrapper,
            String communicationEventId, GenericValue userLogin) throws GenericServiceException, GenericEntityException {
        // handle the attachments
        String subject = wrapper.getSubject();
        List<String> attachmentIndexes = wrapper.getAttachmentIndexes();
        List<String> currentAttachmentNames = getCommEventAttachmentNames(delegator, communicationEventId);

        if (!attachmentIndexes.isEmpty()) {
            Debug.logInfo(x.message_has_attachments + attachmentIndexes.size() + x.str_b5b031f4, MODULE);
            for (String attachmentIdx : attachmentIndexes) {
                String attFileName = wrapper.getPartFilename(attachmentIdx);
                if (currentAttachmentNames.contains(attFileName)) {
                    Debug.logWarning(String.format(x.CommunicationEvent_s_already_has_attachment_named_s, communicationEventId,
                            attFileName), MODULE);
                    continue;
                }

                Map<String, Object> attachmentMap = new HashMap<>();
                attachmentMap.put(x.communicationEventId, communicationEventId);
                attachmentMap.put(x.contentTypeId, x.DOCUMENT);
                attachmentMap.put(x.mimeTypeId, x.text_html);
                attachmentMap.put(x.userLogin, userLogin);
                if (subject != null && subject.length() > 80) {
                    subject = subject.substring(0, 80); // make sure not too big for database field. (20 characters for filename)
                }

                String attContentType = wrapper.getPartContentType(attachmentIdx);
                if (attContentType != null && attContentType.indexOf(';') > -1) {
                    attContentType = attContentType.toLowerCase(Locale.getDefault()).substring(0, attContentType.indexOf(';'));
                }

                if (UtilValidate.isNotEmpty(attFileName)) {
                    attachmentMap.put(x.contentName, attFileName);
                    attachmentMap.put(x.description, subject + x.str_3bc15c8a + attachmentIdx);
                } else {
                    attachmentMap.put(x.contentName, subject + x.str_3bc15c8a + attachmentIdx);
                }

                attachmentMap.put(x.drMimeTypeId, attContentType);
                if (attContentType != null && attContentType.startsWith(x.text)) {
                    String text = wrapper.getPartText(attachmentIdx);
                    attachmentMap.put(x.drDataResourceTypeId, x.ELECTRONIC_TEXT);
                    attachmentMap.put(x.textData, text);
                } else {
                    ByteBuffer data = wrapper.getPartByteBuffer(attachmentIdx);
                    if (Debug.infoOn()) {
                        Debug.logInfo(x.Binary_attachment_size + data.limit(), MODULE);
                    }
                    attachmentMap.put(x.drDataResourceName, attFileName);
                    attachmentMap.put(x.imageData, data);
                    attachmentMap.put(x.drDataResourceTypeId, x.IMAGE_OBJECT); // TODO: why always use IMAGE
                    attachmentMap.put(x._imageData_contentType, attContentType);
                }

                // save the content
                Map<String, Object> result = dispatcher.runSync(x.createCommContentDataResource, attachmentMap);
                if (ServiceUtil.isError(result)) {
                    String errorMessage = ServiceUtil.getErrorMessage(result);
                    Debug.logError(errorMessage, MODULE);
                    throw new GenericServiceException(errorMessage);
                }
            }
        }
    }

    private static void createCommEventRoles(GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher, String
            communicationEventId, List<Map<String, Object>> parties, String roleTypeId) {
        // It's not clear what the "role" of this communication event should be, so we'll just put _NA_
        // check and see if this role was already created and ignore if true
        try {
            for (Map<String, Object> result : parties) {
                String partyId = (String) result.get(x.partyId);
                GenericValue commEventRole = DaoRegistry.getDao(delegator, x.CommunicationEventRole, CommunicationEventRoleDao.class)
                        .findOneByWhere(delegator, x.CommunicationEventRole,
                                UtilMisc.toMap(x.communicationEventId, communicationEventId, x.partyId, partyId, x.roleTypeId, roleTypeId),
                                null, null, false);
                if (commEventRole == null) {
                    Map<String, Object> input = UtilMisc.toMap(x.communicationEventId, communicationEventId,
                            x.partyId, partyId, x.roleTypeId, roleTypeId, x.userLogin, userLogin,
                            x.contactMechId, (String) result.get(x.contactMechId),
                            x.statusId, x.COM_ROLE_CREATED);
                    Map<String, Object> resultMap = dispatcher.runSync(x.createCommunicationEventRole, input);
                    if (ServiceUtil.isError(resultMap)) {
                        String errorMessage = ServiceUtil.getErrorMessage(resultMap);
                        Debug.logError(errorMessage, MODULE);
                    }
                }
            }
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
    }

    private static void createCommunicationEventWorkEffs(GenericValue userLogin, LocalDispatcher dispatcher, List<Map<String,
            Object>> workEffortInfos, String communicationEventId) {
        // create relationship between communication event and work efforts
        try {
            for (Map<String, Object> result : workEffortInfos) {
                String workEffortId = (String) result.get(x.workEffortId);
                Map<String, Object> resultMap = dispatcher.runSync(x.createCommunicationEventWorkEff,
                        UtilMisc.toMap(x.workEffortId, workEffortId, x.communicationEventId, communicationEventId, x.userLogin, userLogin));
                if (ServiceUtil.isError(resultMap)) {
                    String errorMessage = ServiceUtil.getErrorMessage(resultMap);
                    Debug.logError(errorMessage, MODULE);
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
    }

    /*
     * Helper method to retrieve the party information from the first email address of the Address[] specified.
     */
    private static Map<String, Object> getParyInfoFromEmailAddress(Address[] addresses, GenericValue userLogin, LocalDispatcher dispatcher)
            throws GenericServiceException {
        InternetAddress emailAddress = null;
        Map<String, Object> map = null;
        Map<String, Object> result = null;

        if (addresses == null) {
            return null;
        }

        if (addresses.length > 0) {
            Address addr = addresses[0];
            if (addr instanceof InternetAddress) {
                emailAddress = (InternetAddress) addr;
            }
        }

        if (emailAddress != null) {
            map = new HashMap<>();
            map.put(x.address, emailAddress.getAddress());
            map.put(x.userLogin, userLogin);
            result = dispatcher.runSync(x.findPartyFromEmailAddress, map);
            if (ServiceUtil.isError(result)) {
                String errorMessage = ServiceUtil.getErrorMessage(result);
                Debug.logError(errorMessage, MODULE);
                throw new GenericServiceException(errorMessage);
            }
        }

        return result;
    }

    /*
     * Calls findPartyFromEmailAddress service and returns a List of the results for the array of addresses
     */
    private static List<Map<String, Object>> buildListOfPartyInfoFromEmailAddresses(Address[] addresses, GenericValue userLogin,
            LocalDispatcher dispatcher) throws GenericServiceException {
        InternetAddress emailAddress = null;
        Map<String, Object> result = null;
        List<Map<String, Object>> tempResults = new LinkedList<>();

        if (addresses != null) {
            for (Address addr: addresses) {
                if (addr instanceof InternetAddress) {
                    emailAddress = (InternetAddress) addr;

                    result = dispatcher.runSync(x.findPartyFromEmailAddress,
                            UtilMisc.toMap(x.address, emailAddress.getAddress(), x.userLogin, userLogin));
                    if (ServiceUtil.isError(result)) {
                        String errorMessage = ServiceUtil.getErrorMessage(result);
                        Debug.logError(errorMessage, MODULE);
                        throw new GenericServiceException(errorMessage);
                    }
                    if (result.get(x.partyId) != null) {
                        tempResults.add(result);
                    }
                }
            }
        }
        return tempResults;
    }

    /*
     * Gets WorkEffort info from e-mail address and returns a List of the results for the array of addresses
     */
    private static List<Map<String, Object>> buildListOfWorkEffortInfoFromEmailAddresses(Address[] addresses, GenericValue
            userLogin, LocalDispatcher dispatcher) throws GenericServiceException {
        InternetAddress emailAddress = null;
        Map<String, Object> result = null;
        Delegator delegator = dispatcher.getDelegator();
        List<Map<String, Object>> tempResults = new LinkedList<>();
        String caseInsensitiveEmail = EntityUtilProperties.getPropertyValue(x.general, x.mail_address_caseInsensitive, x.N, delegator);

        if (addresses != null) {
            for (Address addr: addresses) {
                if (addr instanceof InternetAddress) {
                    emailAddress = (InternetAddress) addr;
                    Map<String, String> inputFields = new HashMap<>();
                    inputFields.put(x.infoString, emailAddress.getAddress());
                    inputFields.put(x.infoString_ic, caseInsensitiveEmail);
                    result = dispatcher.runSync(x.performFind, UtilMisc.<String, Object>toMap(x.entityName,
                            x.WorkEffortContactMechView, x.inputFields, inputFields, x.userLogin, userLogin));
                    if (ServiceUtil.isError(result)) {
                        String errorMessage = ServiceUtil.getErrorMessage(result);
                        Debug.logError(errorMessage, MODULE);
                        throw new GenericServiceException(errorMessage);
                    }
                    try (EntityListIterator listIt = (EntityListIterator) result.get(x.listIt)) {
                        List<GenericValue> list = listIt.getCompleteList();
                        List<GenericValue> filteredList = EntityUtil.filterByDate(list);
                        tempResults.addAll(filteredList);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }
                }
            }
        }
        return tempResults;
    }

    /*
     * Service to process incoming email and look for a bounce message. If the email is indeed a bounce message
     * the CommunicationEvent will be updated with the proper COM_BOUNCED status.
     */
    public static Map<String, Object> processBouncedMessage(DispatchContext dctx, CommunicationEventServicesContext context) {
        Debug.logInfo(x.Running_process_bounced_message_check, MODULE);
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get(x.messageWrapper);

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        int parts = wrapper.getMainPartCount();

        if (parts >= 3) { // it must have all three parts in order to process correctly
            // get the second part (delivery report)
            String contentType = wrapper.getPartContentType(x._1); // index 1 should be the second part
            if (contentType != null && x.message_delivery_status.equalsIgnoreCase(contentType)) {
                Debug.logInfo(x.Delivery_status_report_part_found_processing, MODULE);

                // get the content of the part
                String part2Text = wrapper.getPartRawText(x._1);
                if (part2Text == null) {
                    part2Text = x.emptyString;
                }
                if (Debug.verboseOn()) {
                    Debug.logVerbose(x.Part_2_Text + part2Text, MODULE);
                }

                // find the "Action" element and obtain its value (looking for "failed")
                Pattern p2 = Pattern.compile(x.Action, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                Matcher m2 = p2.matcher(part2Text);
                String action = null;
                if (m2.find()) {
                    action = m2.group(1);
                }

                if (action != null && x.failed.equalsIgnoreCase(action)) {
                    // message bounced -- get the original message
                    String part3Text = wrapper.getPartRawText(x._2); // index 2 should be the third part
                    if (part3Text == null) {
                        part3Text = x.emptyString;
                    }
                    if (Debug.verboseOn()) {
                        Debug.logVerbose(x.Part_3_Text + part3Text, MODULE);
                    }

                    // find the "Message-Id" element and obtain its value (looking for "failed")
                    Pattern p3 = Pattern.compile(x.Message_Id, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                    Matcher m3 = p3.matcher(part3Text);
                    String messageId = null;
                    if (m3.find()) {
                        Debug.logInfo(x.Found_message_id + m3.group(), MODULE);
                        messageId = m3.group(1);
                    }

                    // find the matching communication event
                    if (messageId != null) {
                        List<GenericValue> values;
                        try {
                            values = DaoRegistry.getDao(delegator, x.CommunicationEvent, CommunicationEventDao.class).findListByWhere(
                                    delegator, x.CommunicationEvent, UtilMisc.toMap(x.messageId, messageId), null, null, false);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        if (UtilValidate.isNotEmpty(values)) {
                            // there should be only one; unique key
                            GenericValue value = values.get(0);

                            // update the communication event status
                            Map<String, Object> updateCtx = new HashMap<>();
                            updateCtx.put(x.communicationEventId, value.getString(x.communicationEventId));
                            updateCtx.put(x.statusId, x.COM_BOUNCED);
                            updateCtx.put(x.userLogin, context.get(x.userLogin));
                            Map<String, Object> result;
                            try {
                                result = dispatcher.runSync(x.updateCommunicationEvent, updateCtx);
                                if (ServiceUtil.isError(result)) {
                                    String errorMessage = ServiceUtil.getErrorMessage(result);
                                    Debug.logError(errorMessage, MODULE);
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, MODULE);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                        } else {
                            if (Debug.infoOn()) {
                                Debug.logInfo(x.Unable_to_find_CommunicationEvent_with_the_matching_messageId + messageId, MODULE);
                            }

                            // no communication events found for that message ID; possible this is a NEWSLETTER
                            try {
                                values = DaoRegistry.getDao(delegator, x.ContactListCommStatus, ContactListCommStatusDao.class)
                                        .findListByWhere(delegator, x.ContactListCommStatus, UtilMisc.toMap(x.messageId, messageId), null,
                                                null, false);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, MODULE);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                            if (UtilValidate.isNotEmpty(values)) {
                                // there should be only one; unique key
                                GenericValue value = values.get(0);

                                Map<String, Object> updateCtx = new HashMap<>();
                                updateCtx.put(x.communicationEventId, value.getString(x.communicationEventId));
                                updateCtx.put(x.contactListId, value.getString(x.contactListId));
                                updateCtx.put(x.contactMechId, value.getString(x.contactMechId));
                                updateCtx.put(x.partyId, value.getString(x.partyId));
                                updateCtx.put(x.statusId, x.COM_BOUNCED);
                                updateCtx.put(x.userLogin, context.get(x.userLogin));
                                Map<String, Object> result;
                                try {
                                    result = dispatcher.runSync(x.updateContactListCommStatus, updateCtx);
                                } catch (GenericServiceException e) {
                                    Debug.logError(e, MODULE);
                                    return ServiceUtil.returnError(e.getMessage());
                                }
                                if (ServiceUtil.isError(result)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                                }
                            } else {
                                if (Debug.infoOn()) {
                                    Debug.logInfo(x.Unable_to_find_ContactListCommStatus_with_the_matching_messageId + messageId, MODULE);
                                }
                            }
                        }
                    } else {
                        Debug.logWarning(x.No_message_ID_attached_to_part, MODULE);
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> logIncomingMessage(DispatchContext dctx, CommunicationEventServicesContext context) {
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get(x.messageWrapper);
        Debug.logInfo(x.Message_recevied + wrapper.getSubject(), MODULE);
        Debug.logInfo(x.Content_Type + wrapper.getContentType(), MODULE);
        Debug.logInfo(x.Number_of_parts + wrapper.getMainPartCount(), MODULE);
        Debug.logInfo(x.Number_of_attachments + wrapper.getAttachmentIndexes().size(), MODULE);
        Debug.logInfo(x.Message_ID + wrapper.getMessageId(), MODULE);

        Debug.logInfo(x.MESSAGE + wrapper.getMessageBody(), MODULE);

        List<String> attachmentIndexes = wrapper.getAttachmentIndexes();
        if (!attachmentIndexes.isEmpty()) {
            Debug.logInfo(x.ATTACHMENTS, MODULE);
            for (String idx : attachmentIndexes) {
                Debug.logInfo(x.Filename + wrapper.getPartFilename(idx), MODULE);
                Debug.logInfo(x.Content_Type_e7dc5067 + wrapper.getPartContentType(idx), MODULE);
            }
        }

        return ServiceUtil.returnSuccess();

    }

    /*
     * Event which marks a communication event as read, and returns a 1px image to the browser/mail client
     * Is updated because the read status is now stored in the communicationEventRole
     * This services is updated but could not be tested. assumed is "read" for partyIdTo on the commevent
     */
    public static String markCommunicationAsRead(HttpServletRequest request, HttpServletResponse response) {
        String communicationEventId = null;

        // pull the communication event from path info, so we can hide the process from the user
        String pathInfo = request.getPathInfo();
        String[] pathParsed = pathInfo.split(x.str_42099b4a, 3);
        if (pathParsed.length > 2) {
            pathInfo = pathParsed[2];
        } else {
            pathInfo = null;
        }
        if (pathInfo != null && pathInfo.indexOf('/') > -1) {
            pathParsed = pathInfo.split(x.str_42099b4a);
            communicationEventId = pathParsed[0];
        }

        // update the communication event
        if (communicationEventId != null) {
            Debug.logInfo(x.Marking_communicationEventId + communicationEventId + x.from_path_info + request.getPathInfo()
                    + x.as_read, MODULE);
            Delegator delegator = (Delegator) request.getAttribute(x.delegator);
            GenericValue communicationEvent = null;
            try {
                communicationEvent = DaoRegistry.getDao(delegator, x.CommunicationEvent, CommunicationEventDao.class).findOneByWhere(
                        delegator, x.CommunicationEvent, UtilMisc.toMap(x.communicationEventId, communicationEventId), null, null, true);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute(x.dispatcher);
            try {
                dispatcher.runAsync(x.setCommEventRoleToRead, UtilMisc.toMap(x.communicationEventId, communicationEventId,
                        x.partyId, communicationEvent.getString(x.partyIdTo)));
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
            }
        }

        // return the 1px image (spacer.gif)
        URL imageUrl;
        try {
            imageUrl = FlexibleLocation.resolveLocation(x.component_common_theme_webapp_images_spacer_gif);
            try (InputStream imageStream = imageUrl.openStream()) {
                UtilHttp.streamContentToBrowser(response, imageStream, 43, x.image_gif, null);
            }
        } catch (IOException e) {
            Debug.logError(e, MODULE);
        }

        // return null to not return any view
        return null;
    }
}
