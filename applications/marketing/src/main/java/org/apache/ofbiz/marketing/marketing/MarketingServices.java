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
package org.apache.ofbiz.marketing.marketing;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.ContactListDao;
import org.apache.ofbiz.persistence.dao.ContactListPartyDao;
import org.apache.ofbiz.persistence.dao.ContactMechDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.PartyContactMechDao;
import org.apache.ofbiz.persistence.dao.PartyContactMechPurposeDao;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.persistence.entity.ContactListEntity;
import org.apache.ofbiz.persistence.entity.ContactListPartyEntity;
import org.apache.ofbiz.persistence.entity.ContactMechEntity;
import org.apache.ofbiz.persistence.entity.PartyContactMechEntity;
import org.apache.ofbiz.persistence.entity.PartyContactMechPurposeEntity;
import org.apache.ofbiz.persistence.entity.UserLoginEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.MarketingServicesContext;
/**
 * MarketingServices contains static service methods for Marketing Campaigns and Contact Lists.
 * See the documentation in marketing/servicedef/services.xml and use the service reference in
 * webtools.  Comments in this file are implemntation notes and technical details.
 */
public class MarketingServices {

    private static final String MODULE = MarketingServices.class.getName();
    public static final String RESOURCE = x.MarketingUiLabels;
    private static final String RES_ORDER = x.OrderUiLabels;

    public static Map<String, Object> signUpForContactList(DispatchContext dctx, MarketingServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        Timestamp fromDate = UtilDateTime.nowTimestamp();
        String contactListId = (String) context.get(x.contactListId);
        String email = (String) context.get(x.email);
        String partyId = (String) context.get(x.partyId);
        String successMessage = UtilProperties.getMessage(RESOURCE, x.MarketingNewsletterSubscriptionRequestSuccessMessage, locale);

        if (!UtilValidate.isEmail(email)) {
            String error = UtilProperties.getMessage(RESOURCE, x.MarketingCampaignInvalidEmailInput, locale);
            return ServiceUtil.returnError(error);
        }

        try {
            ContactListDao contactListDao = DaoRegistry.getDao(delegator, x.ContactList, ContactListDao.class);
            UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
            ContactMechDao contactMechDao = DaoRegistry.getDao(delegator, x.ContactMech, ContactMechDao.class);
            PartyContactMechDao partyContactMechDao = DaoRegistry.getDao(delegator, x.PartyContactMech, PartyContactMechDao.class);
            PartyContactMechPurposeDao partyContactMechPurposeDao = DaoRegistry.getDao(delegator, x.PartyContactMechPurpose,
                    PartyContactMechPurposeDao.class);
            ContactListPartyDao contactListPartyDao = DaoRegistry.getDao(delegator, x.ContactListParty, ContactListPartyDao.class);

            // locate the contact list
            ContactListEntity contactListEntity = contactListDao.get(contactListId).orElse(null);
            GenericValue contactList = contactListEntity == null ? null : delegator.makeValue(x.ContactList, Beans.beanToMap(contactListEntity));
            if (contactList == null) {
                String error = UtilProperties.getMessage(RESOURCE, x.MarketingContactListNotFound, UtilMisc.<String, Object>toMap(x.contactListId,
                        contactListId), locale);
                return ServiceUtil.returnError(error);
            }

            // perform actions as the system user
            UserLoginEntity userLoginEntity = userLoginDao.get(x.system).orElse(null);
            GenericValue userLogin = userLoginEntity == null ? null : delegator.makeValue(x.UserLogin, Beans.beanToMap(userLoginEntity));

            // associate the email with anonymous user TODO: do we need a custom contact mech purpose type, say MARKETING_EMAIL?
            if (partyId == null) {
                // Check existing email
                GenericValue contact = null;
                List<ContactMechEntity> contactMechEntities = contactMechDao.list(Filters.and(
                        Filters.eq(x.infoString, email),
                        Filters.eq(x.contactMechTypeId, x.EMAIL_ADDRESS)));
                for (ContactMechEntity contactMechEntity : contactMechEntities) {
                    List<PartyContactMechEntity> partyContactMechEntities = partyContactMechDao.list(
                            Filters.eq(x.contactMechId, contactMechEntity.getContactMechId()));
                    List<GenericValue> partyContactMechs = new LinkedList<>();
                    for (PartyContactMechEntity partyContactMechEntity : partyContactMechEntities) {
                        partyContactMechs.add(delegator.makeValue(x.PartyContactMech, Beans.beanToMap(partyContactMechEntity)));
                    }
                    partyContactMechs = EntityUtil.filterByDate(partyContactMechs);
                    partyContactMechs = EntityUtil.orderBy(partyContactMechs, UtilMisc.toList(x.fromDate_f5440273));
                    for (GenericValue partyContactMech : partyContactMechs) {
                        List<PartyContactMechPurposeEntity> partyContactMechPurposeEntities = partyContactMechPurposeDao.list(Filters.and(
                                Filters.eq(x.partyId, partyContactMech.getString(x.partyId)),
                                Filters.eq(x.contactMechId, contactMechEntity.getContactMechId()),
                                Filters.eq(x.contactMechPurposeTypeId, x.PRIMARY_EMAIL)));
                        List<GenericValue> partyContactMechPurposes = new LinkedList<>();
                        for (PartyContactMechPurposeEntity partyContactMechPurposeEntity : partyContactMechPurposeEntities) {
                            partyContactMechPurposes.add(
                                    delegator.makeValue(x.PartyContactMechPurpose, Beans.beanToMap(partyContactMechPurposeEntity)));
                        }
                        partyContactMechPurposes = EntityUtil.filterByDate(partyContactMechPurposes);
                        if (UtilValidate.isNotEmpty(partyContactMechPurposes)) {
                            contact = delegator.makeValue(x.PartyContactDetailByPurpose,
                                    UtilMisc.toMap(x.partyId, partyContactMech.getString(x.partyId),
                                            x.fromDate, partyContactMech.getTimestamp(x.fromDate)));
                            break;
                        }
                    }
                    if (contact != null) {
                        break;
                    }
                }
                if (contact != null) {
                    partyId = contact.getString(x.partyId);
                } else {
                    partyId = x.NA;
                }
            }
            Map<String, Object> input = UtilMisc.toMap(x.userLogin, userLogin, x.emailAddress, email, x.partyId, partyId,
                    x.fromDate, fromDate, x.contactMechPurposeTypeId, x.OTHER_EMAIL);
            Map<String, Object> serviceResults = dispatcher.runSync(x.createPartyEmailAddress, input);
            if (ServiceUtil.isError(serviceResults)) {
                throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
            }
            String contactMechId = (String) serviceResults.get(x.contactMechId);

            //checks if user is already subscribed to newsletter
            input = UtilMisc.toMap(x.contactListId, contactList.get(x.contactListId), x.partyId, partyId, x.preferredContactMechId, contactMechId);
            List<ContactListPartyEntity> contactListPartyEntities = contactListPartyDao.list(Filters.and(
                    Filters.eq(x.contactListId, input.get(x.contactListId)),
                    Filters.eq(x.partyId, input.get(x.partyId)),
                    Filters.eq(x.preferredContactMechId, input.get(x.preferredContactMechId))));
            List<GenericValue> contactListPartyList = new LinkedList<>();
            for (ContactListPartyEntity contactListPartyEntity : contactListPartyEntities) {
                contactListPartyList.add(delegator.makeValue(x.ContactListParty, Beans.beanToMap(contactListPartyEntity)));
            }
            contactListPartyList = EntityUtil.filterByDate(contactListPartyList);

            List<GenericValue> acceptedContactListPartyList = EntityUtil.filterByAnd(contactListPartyList,
                    UtilMisc.toMap(x.statusId, x.CLPT_ACCEPTED));
            if (UtilValidate.isNotEmpty(acceptedContactListPartyList)) {
                String error = UtilProperties.getMessage(RESOURCE, x.MarketingNewsletterSubscriptionAlreadyExistsMsg, locale);
                Debug.logError(error, MODULE);
                return ServiceUtil.returnError(error);
            }
            /* checks if user has already requested to sign up: if yes, delete all the existing
             * pending records and then add a new one.
             */
            List<GenericValue> pendingContactListPartyList = EntityUtil.filterByAnd(contactListPartyList,
                    UtilMisc.toMap(x.statusId, x.CLPT_PENDING));
            if (UtilValidate.isNotEmpty(pendingContactListPartyList)) {
                successMessage = UtilProperties.getMessage(RESOURCE, x.MarketingNewsletterSubscriptionReqstAlreadyExistsMsg, locale);
                int count = 0;
                for (GenericValue pendingCLP : pendingContactListPartyList) {
                    Map<String, Object> deletePendingCLPInput = UtilMisc.toMap(x.userLogin, userLogin,
                            x.contactListId, pendingCLP.get(x.contactListId), x.fromDate, pendingCLP.get(x.fromDate),
                            x.partyId, pendingCLP.get(x.partyId));

                    Map<String, Object> deletePendingCLPResults = dispatcher.runSync(x.deleteContactListParty, deletePendingCLPInput);
                    if (ServiceUtil.isSuccess(deletePendingCLPResults)) {
                        count++;
                    } else {
                        Debug.logError(ServiceUtil.getErrorMessage(deletePendingCLPResults), MODULE);
                    }
                }
                Debug.logInfo(x.Successfully_deleted + count + x.old_Contact_List_PENDING_requests, MODULE);
            }

            // create a new association at this fromDate to the anonymous party with status pending
            input = UtilMisc.toMap(x.userLogin, userLogin, x.contactListId, contactList.get(x.contactListId),
                x.partyId, partyId, x.fromDate, fromDate, x.statusId, x.CLPT_PENDING, x.preferredContactMechId, contactMechId, x.baseLocation,
                context.get(x.baseLocation));
            serviceResults = dispatcher.runSync(x.createContactListParty, input);
            if (ServiceUtil.isError(serviceResults)) {
                throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
            }
        } catch (GenericServiceException e) {
            String error = UtilProperties.getMessage(RESOURCE, x.MarketingServiceError, locale);
            Debug.logInfo(e, error + e.getMessage(), MODULE);
            return ServiceUtil.returnError(error);
        } catch (Exception e) {
            String error = UtilProperties.getMessage(RES_ORDER, x.checkhelper_problems_reading_database, locale);
            Debug.logInfo(e, error + e.getMessage(), MODULE);
            return ServiceUtil.returnError(error);
        }
        return ServiceUtil.returnSuccess(successMessage);
    }

    public static Map<String, Object> deleteContactListParty(DispatchContext dctx, MarketingServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        String contactListId = (String) context.get(x.contactListId);
        String partyId = (String) context.get(x.partyId);
        Timestamp fromDate = (Timestamp) context.get(x.fromDate);
        String successMessage = UtilProperties.getMessage(RESOURCE, x.MarketingNewsletterSubscriptionPendingRequestDeletedMessage, locale);

        Map<String, Object> input = UtilMisc.toMap(x.contactListId, contactListId, x.partyId, partyId,
                x.fromDate, fromDate);
        int cntListPartyRemoved = 0;
        try {
            ContactListPartyDao contactListPartyDao = DaoRegistry.getDao(delegator, x.ContactListParty, ContactListPartyDao.class);
            List<ContactListPartyEntity> contactListPartyEntities = contactListPartyDao.list(Filters.and(
                    Filters.eq(x.contactListId, input.get(x.contactListId)),
                    Filters.eq(x.partyId, input.get(x.partyId)),
                    Filters.eq(x.fromDate, input.get(x.fromDate))));
            List<GenericValue> contactListParties = new LinkedList<>();
            for (ContactListPartyEntity contactListPartyEntity : contactListPartyEntities) {
                contactListParties.add(delegator.makeValue(x.ContactListParty, Beans.beanToMap(contactListPartyEntity)));
            }
            contactListParties = EntityUtil.filterByDate(contactListParties);
            GenericValue contactListParty = EntityUtil.getFirst(contactListParties);
            if (contactListParty != null) {
                List<GenericValue> relContactListPartyStatusList = contactListParty.getRelated(x.ContactListPartyStatus, null, null, true);
                int cntLstPrtStatusRemoved = 0;
                if (relContactListPartyStatusList != null && relContactListPartyStatusList.size() > 0) {
                    cntLstPrtStatusRemoved = delegator.removeAll(relContactListPartyStatusList);
                }
                if (cntLstPrtStatusRemoved > 0) {
                    cntListPartyRemoved = delegator.removeValue(contactListParty);
                }
            }
            if (cntListPartyRemoved > 0) {
                successMessage = successMessage + x.contactListId_21451956 + contactListId
                        + x.partyId_d1075f19 + partyId + x.fromDate_1dae066c
                        + fromDate + x.Status_51b35858 + contactListParty.getString(x.statusId) + x.str_4ff447b8;
                Debug.logInfo(successMessage, MODULE);
            }
        } catch (Exception e) {
            String error = UtilProperties.getMessage(RES_ORDER, x.checkhelper_problems_reading_database, locale);
            Debug.logError(e, error + e.getMessage(), MODULE);
            return ServiceUtil.returnError(error);
        }
        return ServiceUtil.returnSuccess(successMessage);
    }
}
