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

package org.apache.ofbiz.party.party;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVFormat.Builder;
import org.apache.commons.csv.CSVRecord;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.ContactMechPurposeTypeDao;
import org.apache.ofbiz.persistence.dao.ContactMechTypeDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.GeoDao;
import org.apache.ofbiz.persistence.dao.PartyAttributeDao;
import org.apache.ofbiz.persistence.dao.PartyDao;
import org.apache.ofbiz.persistence.dao.PartyGroupDao;
import org.apache.ofbiz.persistence.dao.PartyIdentificationDao;
import org.apache.ofbiz.persistence.dao.PartyRoleDao;
import org.apache.ofbiz.persistence.dao.PartyTypeDao;
import org.apache.ofbiz.persistence.dao.PersonDao;
import org.apache.ofbiz.persistence.dao.RoleTypeDao;
import org.apache.ofbiz.persistence.dao.UomDao;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PartyServicesContext;
/**
 * Services for Party/Person/Group maintenance
 */
public class PartyServices {

    private static final String MODULE = PartyServices.class.getName();
    private static final String RESOURCE = x.PartyUiLabels;
    private static final String RES_ERROR = x.PartyErrorUiLabels;

    /**
     * Creates a Person.
     * If no partyId is specified a numeric partyId is retrieved from the Party sequence.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> createPerson(DispatchContext ctx, PartyServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();
        Locale locale = (Locale) context.get(x.locale);
        // in most cases userLogin will be null, but get anyway so we can keep track of that info if it is available
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String partyId = (String) context.get(x.partyId);
        String description = (String) context.get(x.description);

        // if specified partyId starts with a number, return an error
        if (UtilValidate.isNotEmpty(partyId) && partyId.matches(x.d)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.party_id_is_digit, locale));
        }

        // partyId might be empty, so check it and get next seq party id if empty
        if (UtilValidate.isEmpty(partyId)) {
            try {
                partyId = delegator.getNextSeqId(x.Party);
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.party_id_generation_failure, locale));
            }
        }

        // check to see if party object exists, if so make sure it is PERSON type party
        GenericValue party = null;

        try {
            party = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Party, UtilMisc.toMap(x.partyId, partyId),
                    false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
        }

        if (party != null) {
            if (!x.PERSON.equals(party.getString(x.partyTypeId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.person_create_party_exists_not_person_type, locale));
            }
        } else {
            // create a party if one doesn't already exist with an initial status from the input
            String statusId = (String) context.get(x.statusId);
            if (statusId == null) {
                statusId = x.PARTY_ENABLED;
            }
            Map<String, Object> newPartyMap = UtilMisc.toMap(x.partyId, partyId, x.partyTypeId, x.PERSON, x.description, description,
                    x.createdDate, now, x.lastModifiedDate, now, x.statusId, statusId);
            String preferredCurrencyUomId = (String) context.get(x.preferredCurrencyUomId);
            if (UtilValidate.isNotEmpty(preferredCurrencyUomId)) {
                newPartyMap.put(x.preferredCurrencyUomId, preferredCurrencyUomId);
            }
            String externalId = (String) context.get(x.externalId);
            if (UtilValidate.isNotEmpty(externalId)) {
                newPartyMap.put(x.externalId, externalId);
            }
            if (userLogin != null) {
                newPartyMap.put(x.createdByUserLogin, userLogin.get(x.userLoginId));
                newPartyMap.put(x.lastModifiedByUserLogin, userLogin.get(x.userLoginId));
            }
            party = delegator.makeValue(x.Party, newPartyMap);
            toBeStored.add(party);

            // create the status history
            GenericValue statusRec = delegator.makeValue(x.PartyStatus,
                    UtilMisc.toMap(x.partyId, partyId, x.statusId, statusId, x.statusDate, now));
            if (userLogin != null) {
                statusRec.put(x.changeByUserLoginId, userLogin.get(x.userLoginId));
            }
            toBeStored.add(statusRec);
        }

        GenericValue person = null;

        try {
            person = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Person, UtilMisc.toMap(x.partyId, partyId),
                    false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
        }

        if (person != null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.person_create_person_exists, locale));
        }

        person = delegator.makeValue(x.Person, UtilMisc.toMap(x.partyId, partyId));
        person.setNonPKFields(context);
        toBeStored.add(person);

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.person_create_db_error, new Object[] {e.getMessage() }, locale));
        }

        result.put(x.partyId, partyId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Sets a party status.
     * <b>security check</b>: the status change must be defined in StatusValidChange.
     */
    public static Map<String, Object> setPartyStatus(DispatchContext ctx, PartyServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue loggedInUserLogin = (GenericValue) context.get(x.userLogin);

        String partyId = (String) context.get(x.partyId);
        String statusId = (String) context.get(x.statusId);
        Timestamp statusDate = (Timestamp) context.get(x.statusDate);
        if (statusDate == null) {
            statusDate = UtilDateTime.nowTimestamp();
        }

        try {
            GenericValue party = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Party,
                    UtilMisc.toMap(x.partyId, partyId), false);

            String oldStatusId = party.getString(x.statusId);
            if (!statusId.equals(oldStatusId)) {

                if (oldStatusId == null) { // old records
                    party.set(x.statusId, statusId);
                    oldStatusId = party.getString(x.statusId);
                } else {
                    // check that status is defined as a valid change
                    GenericValue statusValidChange = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator,
                            x.StatusValidChange, UtilMisc.toMap(x.statusId, party.getString(x.statusId), x.statusIdTo, statusId), false);
                    if (statusValidChange == null) {
                        String errorMsg = x.Cannot_change_party_status_from + party.getString(x.statusId) + x.to + statusId;
                        Debug.logWarning(errorMsg, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.PartyStatusCannotBeChanged,
                                UtilMisc.toMap(x.partyFromStatusId, party.getString(x.statusId), x.partyToStatusId, statusId), locale));
                    }
                    party.set(x.statusId, statusId);
                }
                party.store();

                // record this status change in PartyStatus table
                GenericValue partyStatus = delegator.makeValue(x.PartyStatus, UtilMisc.toMap(x.partyId, partyId, x.statusId, statusId,
                        x.statusDate, statusDate));
                if (loggedInUserLogin != null) {
                    partyStatus.put(x.changeByUserLoginId, loggedInUserLogin.get(x.userLoginId));
                }
                partyStatus.create();

                // disable all userlogins for this user when the new status is disabled
                if ((x.PARTY_DISABLED).equals(statusId)) {
                    EntityCondition cond = EntityCondition.makeCondition(
                            EntityCondition.makeCondition(x.partyId, partyId),
                            EntityCondition.makeCondition(x.enabled, EntityOperator.NOT_EQUAL, x.N));
                    List<GenericValue> userLogins = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findByCondition(delegator,
                            x.UserLogin, cond, null, null, null, false);
                    for (GenericValue userLogin : userLogins) {
                        userLogin.set(x.enabled, x.N);
                        userLogin.set(x.disabledDateTime, UtilDateTime.nowTimestamp());
                    }
                    delegator.storeAll(userLogins);
                }
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put(x.oldStatusId, oldStatusId);
            return results;
        } catch (GenericEntityException e) {
            Debug.logError(e, e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.person_update_write_failure, new Object[] {e.getMessage() }, locale));
        }
    }

    /**
     * Updates a Person.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> updatePerson(DispatchContext ctx, PartyServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);

        String partyId = getPartyId(context);
        if (UtilValidate.isEmpty(partyId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(ServiceUtil.getResource(),
                    x.serviceUtil_party_id_missing, locale));
        }

        GenericValue person = null;
        GenericValue party = null;

        try {
            person = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Person, UtilMisc.toMap(x.partyId, partyId),
                    false);
            party = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Party, UtilMisc.toMap(x.partyId, partyId),
                    false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.person_update_read_failure, new Object[] {e.getMessage() }, locale));
        }

        if (person == null || party == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.person_update_not_found, locale));
        }

        // update status by separate service
        String oldStatusId = party.getString(x.statusId);
        if (party.get(x.statusId) == null) { // old records
            party.set(x.statusId, x.PARTY_ENABLED);
        }

        person.setNonPKFields(context);
        party.setNonPKFields(context);

        party.set(x.statusId, oldStatusId);

        try {
            person.store();
            party.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.person_update_write_failure, new Object[] {e.getMessage() }, locale));
        }

        if (UtilValidate.isNotEmpty(context.get(x.statusId)) && !context.get(x.statusId).equals(oldStatusId)) {
            try {
                Map<String, Object> serviceResult = dispatcher.runSync(x.setPartyStatus, UtilMisc.toMap(x.partyId, partyId, x.statusId,
                        context.get(x.statusId), x.userLogin, context.get(x.userLogin)));
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.person_update_write_failure, new Object[] {e.getMessage() }, locale));
            }
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        result.put(ModelService.SUCCESS_MESSAGE,
                UtilProperties.getMessage(RES_ERROR, x.person_update_success, locale));
        return result;
    }

    /**
     * Creates a PartyGroup.
     * If no partyId is specified a numeric partyId is retrieved from the Party sequence.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> createPartyGroup(DispatchContext ctx, PartyServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = (String) context.get(x.partyId);
        Locale locale = (Locale) context.get(x.locale);

        // partyId might be empty, so check it and get next seq party id if empty
        if (UtilValidate.isEmpty(partyId)) {
            try {
                partyId = delegator.getNextSeqId(x.Party);
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.partyservices_could_not_create_party_group_generation_failure, locale));
            }
        } else {
            // if specified partyId starts with a number, return an error
            if (partyId.matches(x.d)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.partyservices_could_not_create_party_ID_digit, locale));
            }
        }

        try {
            // check to see if party object exists, if so make sure it is PARTY_GROUP type party
            GenericValue party = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Party,
                    UtilMisc.toMap(x.partyId, partyId), false);
            GenericValue partyGroupPartyType = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.PartyType,
                    UtilMisc.toMap(x.partyTypeId, x.PARTY_GROUP), true);

            if (partyGroupPartyType == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.partyservices_partyservices_party_type_not_found_in_database_cannot_create_party_group, locale));
            }

            if (party != null) {
                GenericValue partyType = party.getRelatedOne(x.PartyType, true);

                if (!EntityTypeUtil.isType(partyType, partyGroupPartyType)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.partyservices_partyservices_cannot_create_party_group_already_exists_not_PARTY_GROUP_type, locale));
                }
            } else {
                // create a party if one doesn't already exist
                String partyTypeId = x.PARTY_GROUP;

                if (UtilValidate.isNotEmpty(context.get(x.partyTypeId))) {
                    GenericValue desiredPartyType = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.PartyType,
                            UtilMisc.toMap(x.partyTypeId, context.get(x.partyTypeId)), true);
                    if (desiredPartyType != null && EntityTypeUtil.isType(desiredPartyType, partyGroupPartyType)) {
                        partyTypeId = desiredPartyType.getString(x.partyTypeId);
                    } else {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                x.PartyPartyTypeIdNotFound, UtilMisc.toMap(x.partyTypeId, context.get(x.partyTypeId)), locale));
                    }
                }

                Map<String, Object> newPartyMap = UtilMisc.toMap(x.partyId, partyId, x.partyTypeId, partyTypeId, x.createdDate, now,
                        x.lastModifiedDate, now);
                if (userLogin != null) {
                    newPartyMap.put(x.createdByUserLogin, userLogin.get(x.userLoginId));
                    newPartyMap.put(x.lastModifiedByUserLogin, userLogin.get(x.userLoginId));
                }

                String statusId = (String) context.get(x.statusId);
                party = delegator.makeValue(x.Party, newPartyMap);
                party.setNonPKFields(context);

                if (statusId == null) {
                    statusId = x.PARTY_ENABLED;
                }
                party.set(x.statusId, statusId);
                party.create();

                // create the status history
                GenericValue partyStat = delegator.makeValue(x.PartyStatus,
                        UtilMisc.toMap(x.partyId, partyId, x.statusId, statusId, x.statusDate, now));
                if (userLogin != null) {
                    partyStat.put(x.changeByUserLoginId, userLogin.get(x.userLoginId));
                }
                partyStat.create();
            }

            GenericValue partyGroup = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.PartyGroup,
                    UtilMisc.toMap(x.partyId, partyId), false);
            if (partyGroup != null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.partyservices_cannot_create_party_group_already_exists, locale));
            }

            partyGroup = delegator.makeValue(x.PartyGroup, UtilMisc.toMap(x.partyId, partyId));
            partyGroup.setNonPKFields(context);
            partyGroup.create();

        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_data_source_error_adding_party_group,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }

        result.put(x.partyId, partyId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates a PartyGroup.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> updatePartyGroup(DispatchContext ctx, PartyServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);

        String partyId = getPartyId(context);
        if (UtilValidate.isEmpty(partyId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(ServiceUtil.getResource(),
                    x.serviceUtil_party_id_missing, locale));
        }

        GenericValue partyGroup = null;
        GenericValue party = null;

        try {
            partyGroup = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.PartyGroup,
                    UtilMisc.toMap(x.partyId, partyId), false);
            party = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Party,
                    UtilMisc.toMap(x.partyId, partyId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_could_not_update_party_information_read,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }

        if (partyGroup == null || party == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_could_not_update_party_information_not_found, locale));
        }


        // update status by separate service
        String oldStatusId = party.getString(x.statusId);
        partyGroup.setNonPKFields(context);
        party.setNonPKFields(context);
        party.set(x.statusId, oldStatusId);

        try {
            partyGroup.store();
            party.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_could_not_update_party_information_write,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }

        if (UtilValidate.isNotEmpty(context.get(x.statusId)) && !context.get(x.statusId).equals(oldStatusId)) {
            try {
                Map<String, Object> serviceResult = dispatcher.runSync(x.setPartyStatus, UtilMisc.toMap(x.partyId, partyId,
                        x.statusId, context.get(x.statusId), x.userLogin, context.get(x.userLogin)));
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.person_update_write_failure, new Object[] {e.getMessage() }, locale));
            }
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Create an Affiliate entity.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> createAffiliate(DispatchContext ctx, PartyServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = getPartyId(context);

        // if specified partyId starts with a number, return an error
        if (UtilValidate.isNotEmpty(partyId) && partyId.matches(x.d)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_cannot_create_affiliate_digit, locale));
        }

        // partyId might be empty, so check it and get next seq party id if empty
        if (UtilValidate.isEmpty(partyId)) {
            try {
                partyId = delegator.getNextSeqId(x.Party);
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.partyservices_cannot_create_affiliate_generation_failure, locale));
            }
        }

        // check to see if party object exists, if so make sure it is AFFILIATE type party
        GenericValue party = null;

        try {
            party = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Party, UtilMisc.toMap(x.partyId, partyId),
                    false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
        }

        if (party == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_cannot_create_affiliate_no_party_entity, locale));
        }

        GenericValue affiliate = null;

        try {
            affiliate = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Affiliate,
                    UtilMisc.toMap(x.partyId, partyId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
        }

        if (affiliate != null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_cannot_create_affiliate_ID_already_exists, locale));
        }

        affiliate = delegator.makeValue(x.Affiliate, UtilMisc.toMap(x.partyId, partyId));
        affiliate.setNonPKFields(context);
        affiliate.set(x.dateTimeCreated, now, false);

        try {
            delegator.create(affiliate);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_could_not_add_affiliate_info_write,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }

        result.put(x.partyId, partyId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates an Affiliate.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> updateAffiliate(DispatchContext ctx, PartyServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        String partyId = getPartyId(context);
        if (UtilValidate.isEmpty(partyId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(ServiceUtil.getResource(),
                    x.serviceUtil_party_id_missing, locale));
        }

        GenericValue affiliate = null;

        try {
            affiliate = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Affiliate,
                    UtilMisc.toMap(x.partyId, partyId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_could_not_update_affiliate_information_read,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }

        if (affiliate == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_could_not_update_affiliate_information_not_found, locale));
        }

        affiliate.setNonPKFields(context);

        try {
            affiliate.store();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_could_not_update_affiliate_information_write,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Add a PartyNote.
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> createPartyNote(DispatchContext dctx, PartyServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String noteString = (String) context.get(x.note);
        String partyId = (String) context.get(x.partyId);
        String noteId = (String) context.get(x.noteId);
        String noteName = (String) context.get(x.noteName);
        Locale locale = (Locale) context.get(x.locale);

        //Make sure the note Id actually exists if one is passed to avoid a foreign key error below
        if (noteId != null) {
            try {
                GenericValue value = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.NoteData,
                        UtilMisc.toMap(x.noteId, noteId), false);
                if (value == null) {
                    Debug.logError(x.ERROR_Note_id_does_not_exist_for + noteId + x.autogenerating, MODULE);
                    noteId = null;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, x.ERROR_Note_id_does_not_exist_for + noteId + x.autogenerating, MODULE);
                noteId = null;
            }
        }

        // if no noteId is specified, then create and associate the note with the userLogin
        if (noteId == null) {
            Map<String, Object> noteRes = null;
            try {
                noteRes = dispatcher.runSync(x.createNote, UtilMisc.toMap(x.partyId, userLogin.getString(x.partyId),
                         x.note, noteString, x.userLogin, userLogin, x.locale, locale, x.noteName, noteName));
                if (ServiceUtil.isError(noteRes)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(noteRes));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, e.getMessage(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.PartyNoteCreationError, UtilMisc.toMap(x.errorString, e.getMessage()), locale));
            }

            if (noteRes.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
                return noteRes;
            }

            noteId = (String) noteRes.get(x.noteId);

            if (UtilValidate.isEmpty(noteId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.partyservices_problem_creating_note_no_noteId_returned, locale));
            }
        }
        result.put(x.noteId, noteId);

        // Set the party info
        try {
            Map<String, String> fields = UtilMisc.toMap(x.partyId, partyId, x.noteId, noteId);
            GenericValue v = delegator.makeValue(x.PartyNote, fields);

            delegator.create(v);
        } catch (GenericEntityException ee) {
            Debug.logError(ee, MODULE);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_problem_associating_note_with_party,
                    UtilMisc.toMap(x.errMessage, ee.getMessage()), locale));
        }
        result.put(ModelService.SUCCESS_MESSAGE,
                UtilProperties.getMessage(RESOURCE, x.PartyNoteCreatedSuccessfully, locale));
        return result;
    }

    /**
     * Get the party object(s) from an e-mail address
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getPartiesFromExactEmail(DispatchContext dctx, PartyServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = new LinkedList<>();
        String email = (String) context.get(x.email);
        Locale locale = (Locale) context.get(x.locale);

        if (email.isEmpty()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_required_parameter_email_cannot_be_empty, locale));
        }

        try {
            List<GenericValue> c = DaoRegistry.getDao(delegator, x.PartyAndContactMech, PartyDao.class).findByCondition(delegator,
                    x.PartyAndContactMech,
                    EntityCondition.makeCondition(EntityFunction.upperField(x.infoString), EntityOperator.EQUALS,
                            EntityFunction.upper(email.toUpperCase(Locale.getDefault()))),
                    null, UtilMisc.toList(x.infoString), null, false);
            c = EntityUtil.filterByDate(c);

            if (Debug.verboseOn()) {
                Debug.logVerbose(x.List + c, MODULE);
            }
            if (Debug.infoOn()) {
                Debug.logInfo(x.PartyFromEmail_number_found + c.size(), MODULE);
            }
            if (c != null) {
                for (GenericValue pacm: c) {
                    GenericValue party = delegator.makeValue(x.Party, UtilMisc.toMap(x.partyId, pacm.get(x.partyId),
                            x.partyTypeId, pacm.get(x.partyTypeId)));

                    parties.add(UtilMisc.<String, GenericValue>toMap(x.party, party));
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_cannot_get_party_entities_read,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }
        if (!parties.isEmpty()) {
            result.put(x.parties, parties);
        }
        return result;
    }

    public static Map<String, Object> getPartiesFromPartOfEmail(DispatchContext dctx, PartyServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = new LinkedList<>();
        String email = (String) context.get(x.email);
        Locale locale = (Locale) context.get(x.locale);

        if (email.isEmpty()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_required_parameter_email_cannot_be_empty, locale));
        }

        try {
            List<GenericValue> c = DaoRegistry.getDao(delegator, x.PartyAndContactMech, PartyDao.class).findByCondition(delegator,
                    x.PartyAndContactMech,
                    EntityCondition.makeCondition(EntityFunction.upperField(x.infoString), EntityOperator.LIKE,
                            EntityFunction.upper((x.str_4345cb1f + email.toUpperCase(Locale.getDefault())) + x.str_4345cb1f)),
                    null, UtilMisc.toList(x.infoString), null, false);
            c = EntityUtil.filterByDate(c);

            if (Debug.verboseOn()) {
                Debug.logVerbose(x.List + c, MODULE);
            }
            if (Debug.infoOn()) {
                Debug.logInfo(x.PartyFromEmail_number_found + c.size(), MODULE);
            }
            if (c != null) {
                for (GenericValue pacm: c) {
                    GenericValue party = delegator.makeValue(x.Party, UtilMisc.toMap(x.partyId, pacm.get(x.partyId),
                            x.partyTypeId, pacm.get(x.partyTypeId)));

                    parties.add(UtilMisc.<String, GenericValue>toMap(x.party, party));
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_cannot_get_party_entities_read,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }
        if (!parties.isEmpty()) {
            result.put(x.parties, parties);
        }
        return result;
    }

    /**
     * Get the party object(s) from a user login ID
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getPartiesFromPartOfUserloginId(DispatchContext dctx, PartyServicesContext context) {
        Debug.logWarning(x.Running_the_getPartiesFromPartOfUserloginId_Service, MODULE);
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = new LinkedList<>();
        String userLoginId = (String) context.get(x.userLoginId);
        Locale locale = (Locale) context.get(x.locale);

        if (userLoginId.isEmpty()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyCannotGetUserLoginFromParty, locale));
        }

        try {
            Collection<GenericValue> ulc = DaoRegistry.getDao(delegator, x.PartyAndUserLogin, PartyDao.class).findByCondition(delegator,
                    x.PartyAndUserLogin,
                    EntityCondition.makeCondition(EntityFunction.upperField(x.userLoginId), EntityOperator.LIKE,
                            EntityFunction.upper(x.str_4345cb1f + userLoginId.toUpperCase(Locale.getDefault()) + x.str_4345cb1f)),
                    null, UtilMisc.toList(x.userLoginId), null, false);

            if (Debug.verboseOn()) {
                Debug.logVerbose(x.Collection + ulc, MODULE);
            }
            if (Debug.infoOn()) {
                Debug.logInfo(x.PartyFromUserLogin_number_found + ulc.size(), MODULE);
            }
            if (ulc != null) {
                for (GenericValue ul: ulc) {
                    GenericValue party = delegator.makeValue(x.Party, UtilMisc.toMap(x.partyId, ul.get(x.partyId),
                            x.partyTypeId, ul.get(x.partyTypeId)));
                    parties.add(UtilMisc.<String, GenericValue>toMap(x.party, party));
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_cannot_get_party_entities_read,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }
        if (UtilValidate.isNotEmpty(parties)) {
            result.put(x.parties, parties);
        }
        return result;
    }

    /**
     * Get the party object(s) from person information
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getPartiesFromPerson(DispatchContext dctx, PartyServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = new LinkedList<>();
        String firstName = (String) context.get(x.firstName);
        String lastName = (String) context.get(x.lastName);
        Locale locale = (Locale) context.get(x.locale);

        if (firstName == null) {
            firstName = x.emptyString;
        }
        if (lastName == null) {
            lastName = x.emptyString;
        }
        if (firstName.isEmpty() && lastName.isEmpty()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_both_names_cannot_be_empty, locale));
        }

        try {
            EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition(EntityFunction.upperField(x.firstName), EntityOperator.LIKE,
                            EntityFunction.upper(x.str_4345cb1f + firstName.toUpperCase(Locale.getDefault()) + x.str_4345cb1f)),
                    EntityCondition.makeCondition(EntityFunction.upperField(x.lastName), EntityOperator.LIKE,
                            EntityFunction.upper(x.str_4345cb1f + lastName.toUpperCase(Locale.getDefault()) + x.str_4345cb1f)));
            Collection<GenericValue> pc = DaoRegistry.getDao(delegator, x.Person, PersonDao.class).findByCondition(delegator, x.Person, ecl, null,
                    UtilMisc.toList(x.lastName, x.firstName, x.partyId), null, false);

            if (Debug.infoOn()) {
                Debug.logInfo(x.PartyFromPerson_number_found + pc.size(), MODULE);
            }
            if (pc != null) {
                for (GenericValue person: pc) {
                    GenericValue party = delegator.makeValue(x.Party, UtilMisc.toMap(x.partyId,
                            person.get(x.partyId), x.partyTypeId, x.PERSON));

                    parties.add(UtilMisc.<String, GenericValue>toMap(x.person, person, x.party, party));
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_cannot_get_party_entities_read,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }
        if (!parties.isEmpty()) {
            result.put(x.parties, parties);
        }
        return result;
    }

    /**
     * Get the party object(s) from party group name.
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getPartiesFromPartyGroup(DispatchContext dctx, PartyServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = new LinkedList<>();
        String groupName = (String) context.get(x.groupName);
        Locale locale = (Locale) context.get(x.locale);

        if (groupName.isEmpty()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyCannotgetPartiesFromPartyGroup, locale));
        }

        try {
            Collection<GenericValue> pc = DaoRegistry.getDao(delegator, x.PartyGroup, PartyGroupDao.class).findByCondition(delegator, x.PartyGroup,
                    EntityCondition.makeCondition(EntityFunction.upperField(x.groupName), EntityOperator.LIKE,
                            EntityFunction.upper(x.str_4345cb1f + groupName.toUpperCase(Locale.getDefault()) + x.str_4345cb1f)),
                    null, UtilMisc.toList(x.groupName, x.partyId), null, false);

            if (Debug.infoOn()) {
                Debug.logInfo(x.PartyFromGroup_number_found + pc.size(), MODULE);
            }
            if (pc != null) {
                for (GenericValue group: pc) {
                    GenericValue party = delegator.makeValue(x.Party, UtilMisc.toMap(x.partyId,
                            group.get(x.partyId), x.partyTypeId, x.PARTY_GROUP));

                    parties.add(UtilMisc.<String, GenericValue>toMap(x.partyGroup, group, x.party, party));
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_cannot_get_party_entities_read,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }
        if (!parties.isEmpty()) {
            result.put(x.parties, parties);
        }
        return result;
    }

    /**
     * Get the party object(s) from party externalId.
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getPartiesFromExternalId(DispatchContext dctx, PartyServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        List<GenericValue> parties;
        String externalId = (String) context.get(x.externalId);
        Locale locale = (Locale) context.get(x.locale);

        try {
            parties = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findByCondition(delegator, x.Party,
                    EntityCondition.makeCondition(EntityFunction.upperField(x.externalId), EntityOperator.EQUALS, EntityFunction.upper(externalId)),
                    null, UtilMisc.toList(x.externalId, x.partyId), null, false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_cannot_get_party_entities_read,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }
        result.put(x.parties, parties);
        return result;
    }

    public static Map<String, Object> getPerson(DispatchContext dctx, PartyServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get(x.partyId);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue person = null;

        try {
            person = DaoRegistry.getDao(delegator, x.Person, PersonDao.class).findOne(delegator, x.Person, UtilMisc.toMap(x.partyId, partyId), true);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.partyservices_cannot_get_party_entities_read,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }
        if (person != null) {
            result.put(x.lookupPerson, person);
        }
        return result;
    }

    @Deprecated // migration from ftl to widget in process.
    public static Map<String, Object> findParty(DispatchContext dctx, PartyServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        String extInfo = (String) context.get(x.extInfo);

        // get the role types
        try {
            List<GenericValue> roleTypes = DaoRegistry.getDao(delegator, x.RoleType, RoleTypeDao.class).findByAnd(delegator, x.RoleType,
                    UtilMisc.toMap(), UtilMisc.toList(x.description), false);
            result.put(x.roleTypes, roleTypes);
        } catch (GenericEntityException e) {
            String errMsg = x.Error_looking_up_RoleTypes + e.toString();
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyLookupRoleTypeError,
                    UtilMisc.toMap(x.errMessage, e.toString()), locale));
        }

        // current role type
        String roleTypeId;
        try {
            roleTypeId = (String) context.get(x.roleTypeId);
            if (UtilValidate.isNotEmpty(roleTypeId)) {
                GenericValue currentRole = DaoRegistry.getDao(delegator, x.RoleType, RoleTypeDao.class).findOne(delegator, x.RoleType,
                        UtilMisc.toMap(x.roleTypeId, roleTypeId), true);
                result.put(x.currentRole, currentRole);
            }
        } catch (GenericEntityException e) {
            String errMsg = x.Error_looking_up_current_RoleType + e.toString();
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyLookupRoleTypeError,
                    UtilMisc.toMap(x.errMessage, e.toString()), locale));
        }

        //get party types
        try {
            List<GenericValue> partyTypes = DaoRegistry.getDao(delegator, x.PartyType, PartyTypeDao.class).findByAnd(delegator, x.PartyType,
                    UtilMisc.toMap(), UtilMisc.toList(x.description), false);
            result.put(x.partyTypes, partyTypes);
        } catch (GenericEntityException e) {
            String errMsg = x.Error_looking_up_PartyTypes + e.toString();
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyLookupPartyTypeError,
                    UtilMisc.toMap(x.errMessage, e.toString()), locale));
        }

        // current party type
        String partyTypeId;
        try {
            partyTypeId = (String) context.get(x.partyTypeId);
            if (UtilValidate.isNotEmpty(partyTypeId)) {
                GenericValue currentPartyType = DaoRegistry.getDao(delegator, x.PartyType, PartyTypeDao.class).findOne(delegator, x.PartyType,
                        UtilMisc.toMap(x.partyTypeId, partyTypeId), true);
                result.put(x.currentPartyType, currentPartyType);
            }
        } catch (GenericEntityException e) {
            String errMsg = x.Error_looking_up_current_PartyType + e.toString();
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyLookupPartyTypeError,
                    UtilMisc.toMap(x.errMessage, e.toString()), locale));
        }

        // current state
        String stateProvinceGeoId;
        try {
            stateProvinceGeoId = (String) context.get(x.stateProvinceGeoId);
            if (UtilValidate.isNotEmpty(stateProvinceGeoId)) {
                GenericValue currentStateGeo = DaoRegistry.getDao(delegator, x.Geo, GeoDao.class).findOne(delegator, x.Geo,
                        UtilMisc.toMap(x.geoId, stateProvinceGeoId), true);
                result.put(x.currentStateGeo, currentStateGeo);
            }
        } catch (GenericEntityException e) {
            String errMsg = x.Error_looking_up_current_stateProvinceGeo + e.toString();
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyLookupStateProvinceGeoError,
                    UtilMisc.toMap(x.errMessage, e.toString()), locale));
        }

        // set the page parameters
        int viewIndex = 0;
        try {
            viewIndex = Integer.parseInt((String) context.get(x.VIEW_INDEX));
        } catch (Exception e) {
            viewIndex = 0;
        }
        result.put(x.viewIndex, viewIndex);

        int viewSize = 20;
        try {
            viewSize = Integer.parseInt((String) context.get(x.VIEW_SIZE));
        } catch (Exception e) {
            viewSize = 20;
        }
        result.put(x.viewSize, viewSize);

        // get the lookup flag
        String lookupFlag = (String) context.get(x.lookupFlag);

        // blank param list
        String paramList = x.emptyString;

        List<GenericValue> partyList = null;
        int partyListSize = 0;
        int lowIndex = 0;
        int highIndex = 0;

        if (x.Y.equals(lookupFlag)) {
            String showAll = (context.get(x.showAll) != null ? (String) context.get(x.showAll) : x.N);
            paramList = paramList + x.lookupFlag_0b46d785 + lookupFlag + x.showAll_32d02f04 + showAll + x.extInfo_d050ee6e + extInfo;

            // create the dynamic view entity
            DynamicViewEntity dynamicView = new DynamicViewEntity();

            // default view settings
            dynamicView.addMemberEntity(x.PT, x.Party);
            dynamicView.addAlias(x.PT, x.partyId);
            dynamicView.addAlias(x.PT, x.statusId);
            dynamicView.addAlias(x.PT, x.partyTypeId);
            dynamicView.addAlias(x.PT, x.createdDate);
            dynamicView.addAlias(x.PT, x.lastModifiedDate);
            dynamicView.addRelation(x.one_nofk, x.emptyString, x.PartyType, ModelKeyMap.makeKeyMapList(x.partyTypeId));
            dynamicView.addRelation(x.many, x.emptyString, x.UserLogin, ModelKeyMap.makeKeyMapList(x.partyId));

            // define the main condition & expression list
            List<EntityCondition> andExprs = new LinkedList<>();
            EntityCondition mainCond = null;

            List<String> orderBy = new LinkedList<>();
            List<String> fieldsToSelect = new LinkedList<>();
            // fields we need to select; will be used to set distinct
            fieldsToSelect.add(x.partyId);
            fieldsToSelect.add(x.statusId);
            fieldsToSelect.add(x.partyTypeId);
            fieldsToSelect.add(x.createdDate);
            fieldsToSelect.add(x.lastModifiedDate);

            // filter on parties that have relationship with logged in user
            String partyRelationshipTypeId = (String) context.get(x.partyRelationshipTypeId);
            if (UtilValidate.isNotEmpty(partyRelationshipTypeId)) {
                // add relation to view
                dynamicView.addMemberEntity(x.PRSHP, x.PartyRelationship);
                dynamicView.addAlias(x.PRSHP, x.partyIdTo);
                dynamicView.addAlias(x.PRSHP, x.partyRelationshipTypeId);
                dynamicView.addViewLink(x.PT, x.PRSHP, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId, x.partyIdTo));
                List<String> ownerPartyIds = UtilGenerics.cast(context.get(x.ownerPartyIds));
                EntityCondition relationshipCond = null;
                if (UtilValidate.isEmpty(ownerPartyIds)) {
                    String partyIdFrom = userLogin.getString(x.partyId);
                    paramList = paramList + x.partyIdFrom_194b6806 + partyIdFrom;
                    relationshipCond = EntityCondition.makeCondition(EntityFunction.upperField(x.partyIdFrom),
                            EntityOperator.EQUALS, EntityFunction.upper(partyIdFrom));
                } else {
                    relationshipCond = EntityCondition.makeCondition(x.partyIdFrom, EntityOperator.IN, ownerPartyIds);
                }
                dynamicView.addAlias(x.PRSHP, x.partyIdFrom);
                // add the expr
                andExprs.add(EntityCondition.makeCondition(
                        relationshipCond, EntityOperator.AND,
                        EntityCondition.makeCondition(EntityFunction.upperField(x.partyRelationshipTypeId),
                                EntityOperator.EQUALS, EntityFunction.upper(partyRelationshipTypeId))));
                fieldsToSelect.add(x.partyIdTo);
            }

            // get the params
            String partyId = (String) context.get(x.partyId);
            String statusId = (String) context.get(x.statusId);
            String userLoginId = (String) context.get(x.userLoginId);
            String firstName = (String) context.get(x.firstName);
            String lastName = (String) context.get(x.lastName);
            String groupName = (String) context.get(x.groupName);

            if (!x.Y.equals(showAll)) {
                // check for a partyId
                if (UtilValidate.isNotEmpty(partyId)) {
                    paramList = paramList + x.partyId_af53ecb9 + partyId;
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.partyId),
                            EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + partyId + x.str_4345cb1f)));
                }

                // now the statusId - send ANY for all statuses; leave null for just enabled; or pass a specific status
                if (statusId != null) {
                    paramList = paramList + x.statusId_0013b398 + statusId;
                    if (!x.ANY.equalsIgnoreCase(statusId)) {
                        andExprs.add(EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, statusId));
                    }
                } else {
                    // NOTE: _must_ explicitly allow null as it is not included in a not equal in many databases... odd but true
                    andExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, null),
                            EntityOperator.OR, EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.PARTY_DISABLED)));
                }
                // check for partyTypeId
                if (partyTypeId != null && !x.ANY.equals(partyTypeId)) {
                    paramList = paramList + x.partyTypeId_20033ffd + partyTypeId;
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.partyTypeId),
                            EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + partyTypeId + x.str_4345cb1f)));
                }

                // ----
                // UserLogin Fields
                // ----

                // filter on user login
                if (UtilValidate.isNotEmpty(userLoginId)) {
                    paramList = paramList + x.userLoginId_c79dbe25 + userLoginId;

                    // modify the dynamic view
                    dynamicView.addMemberEntity(x.UL, x.UserLogin);
                    dynamicView.addAlias(x.UL, x.userLoginId);
                    dynamicView.addViewLink(x.PT, x.UL, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));

                    // add the expr
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.userLoginId),
                            EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + userLoginId + x.str_4345cb1f)));

                    fieldsToSelect.add(x.userLoginId);
                }

                // ----
                // PartyGroup Fields
                // ----

                // filter on groupName
                if (UtilValidate.isNotEmpty(groupName)) {
                    paramList = paramList + x.groupName_c47b59ad + groupName;

                    // modify the dynamic view
                    dynamicView.addMemberEntity(x.PG, x.PartyGroup);
                    dynamicView.addAlias(x.PG, x.groupName);
                    dynamicView.addViewLink(x.PT, x.PG, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));

                    // add the expr
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.groupName),
                            EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + groupName + x.str_4345cb1f)));

                    fieldsToSelect.add(x.groupName);
                }

                // ----
                // Person Fields
                // ----

                // modify the dynamic view
                if (UtilValidate.isNotEmpty(firstName) || UtilValidate.isNotEmpty(lastName)) {
                    dynamicView.addMemberEntity(x.PE, x.Person);
                    dynamicView.addAlias(x.PE, x.firstName);
                    dynamicView.addAlias(x.PE, x.lastName);
                    dynamicView.addViewLink(x.PT, x.PE, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));

                    fieldsToSelect.add(x.firstName);
                    fieldsToSelect.add(x.lastName);
                    orderBy.add(x.lastName);
                    orderBy.add(x.firstName);
                }

                // filter on firstName
                if (UtilValidate.isNotEmpty(firstName)) {
                    paramList = paramList + x.firstName_c8075a0d + firstName;
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.firstName),
                            EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + firstName + x.str_4345cb1f)));
                }

                // filter on lastName
                if (UtilValidate.isNotEmpty(lastName)) {
                    paramList = paramList + x.lastName_72f9f70a + lastName;
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.lastName),
                            EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + lastName + x.str_4345cb1f)));
                }

                // ----
                // RoleType Fields
                // ----

                // filter on role member
                if (roleTypeId != null && !x.ANY.equals(roleTypeId)) {
                    paramList = paramList + x.roleTypeId_42730128 + roleTypeId;

                    // add role to view
                    dynamicView.addMemberEntity(x.PR, x.PartyRole);
                    dynamicView.addAlias(x.PR, x.roleTypeId);
                    dynamicView.addViewLink(x.PT, x.PR, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));

                    // add the expr
                    andExprs.add(EntityCondition.makeCondition(x.roleTypeId, EntityOperator.EQUALS, roleTypeId));

                    fieldsToSelect.add(x.roleTypeId);
                }

                // ----
                // InventoryItem Fields
                // ----

                // filter on inventory item's fields
                String inventoryItemId = (String) context.get(x.inventoryItemId);
                String serialNumber = (String) context.get(x.serialNumber);
                String softIdentifier = (String) context.get(x.softIdentifier);
                if (UtilValidate.isNotEmpty(inventoryItemId) || UtilValidate.isNotEmpty(serialNumber) || UtilValidate.isNotEmpty(softIdentifier)) {
                    // add role to view
                    dynamicView.addMemberEntity(x.II, x.InventoryItem);
                    dynamicView.addAlias(x.II, x.ownerPartyId);
                    dynamicView.addViewLink(x.PT, x.II, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId, x.ownerPartyId));
                }
                if (UtilValidate.isNotEmpty(inventoryItemId)) {
                    paramList = paramList + x.inventoryItemId_a8875ca1 + inventoryItemId;
                    dynamicView.addAlias(x.II, x.inventoryItemId);
                    // add the expr
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.inventoryItemId),
                            EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + inventoryItemId + x.str_4345cb1f)));
                    fieldsToSelect.add(x.inventoryItemId);
                }
                if (UtilValidate.isNotEmpty(serialNumber)) {
                    paramList = paramList + x.serialNumber_c235132c + serialNumber;
                    dynamicView.addAlias(x.II, x.serialNumber);
                    // add the expr
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.serialNumber),
                            EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + serialNumber + x.str_4345cb1f)));
                    fieldsToSelect.add(x.serialNumber);
                }
                if (UtilValidate.isNotEmpty(softIdentifier)) {
                    paramList = paramList + x.softIdentifier_c5e13db7 + softIdentifier;
                    dynamicView.addAlias(x.II, x.softIdentifier);
                    // add the expr
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.softIdentifier),
                            EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + softIdentifier + x.str_4345cb1f)));
                    fieldsToSelect.add(x.softIdentifier);
                }

                // ----
                // PostalAddress fields
                // ----
                if (x.P.equals(extInfo)) {
                    // add address to dynamic view
                    dynamicView.addMemberEntity(x.PC, x.PartyContactMech);
                    dynamicView.addMemberEntity(x.PA, x.PostalAddress);
                    dynamicView.addAlias(x.PC, x.contactMechId);
                    dynamicView.addAlias(x.PA, x.address1);
                    dynamicView.addAlias(x.PA, x.address2);
                    dynamicView.addAlias(x.PA, x.city);
                    dynamicView.addAlias(x.PA, x.stateProvinceGeoId);
                    dynamicView.addAlias(x.PA, x.countryGeoId);
                    dynamicView.addAlias(x.PA, x.postalCode);
                    dynamicView.addViewLink(x.PT, x.PC, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));
                    dynamicView.addViewLink(x.PC, x.PA, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.contactMechId));

                    // filter on address1
                    String address1 = (String) context.get(x.address1);
                    if (UtilValidate.isNotEmpty(address1)) {
                        paramList = paramList + x.address1_01bc8765 + address1;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.address1),
                                EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + address1 + x.str_4345cb1f)));
                    }

                    // filter on address2
                    String address2 = (String) context.get(x.address2);
                    if (UtilValidate.isNotEmpty(address2)) {
                        paramList = paramList + x.address2_1beab712 + address2;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.address2),
                                EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + address2 + x.str_4345cb1f)));
                    }

                    // filter on city
                    String city = (String) context.get(x.city);
                    if (UtilValidate.isNotEmpty(city)) {
                        paramList = paramList + x.city_b30bcabd + city;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.city),
                                EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + city + x.str_4345cb1f)));
                    }

                    // filter on state geo
                    if (stateProvinceGeoId != null && !x.ANY.equals(stateProvinceGeoId)) {
                        paramList = paramList + x.stateProvinceGeoId_c9c59260 + stateProvinceGeoId;
                        andExprs.add(EntityCondition.makeCondition(x.stateProvinceGeoId, EntityOperator.EQUALS, stateProvinceGeoId));
                    }

                    // filter on postal code
                    String postalCode = (String) context.get(x.postalCode);
                    if (UtilValidate.isNotEmpty(postalCode)) {
                        paramList = paramList + x.postalCode_ca414fe2 + postalCode;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.postalCode),
                                EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + postalCode + x.str_4345cb1f)));
                    }

                    fieldsToSelect.add(x.postalCode);
                    fieldsToSelect.add(x.city);
                    fieldsToSelect.add(x.stateProvinceGeoId);
                }

                // ----
                // Generic CM Fields
                // ----
                if (x.O.equals(extInfo)) {
                    // add info to dynamic view
                    dynamicView.addMemberEntity(x.PC, x.PartyContactMech);
                    dynamicView.addMemberEntity(x.CM, x.ContactMech);
                    dynamicView.addAlias(x.PC, x.contactMechId);
                    dynamicView.addAlias(x.CM, x.infoString);
                    dynamicView.addViewLink(x.PT, x.PC, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));
                    dynamicView.addViewLink(x.PC, x.CM, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.contactMechId));

                    // filter on infoString
                    String infoString = (String) context.get(x.infoString);
                    if (UtilValidate.isNotEmpty(infoString)) {
                        paramList = paramList + x.infoString_90b6c8ba + infoString;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.infoString),
                                EntityOperator.LIKE, EntityFunction.upper(x.str_4345cb1f + infoString + x.str_4345cb1f)));
                        fieldsToSelect.add(x.infoString);
                    }

                }

                // ----
                // TelecomNumber Fields
                // ----
                if (x.T.equals(extInfo)) {
                    // add telecom to dynamic view
                    dynamicView.addMemberEntity(x.PC, x.PartyContactMech);
                    dynamicView.addMemberEntity(x.TM, x.TelecomNumber);
                    dynamicView.addAlias(x.PC, x.contactMechId);
                    dynamicView.addAlias(x.TM, x.countryCode);
                    dynamicView.addAlias(x.TM, x.areaCode);
                    dynamicView.addAlias(x.TM, x.contactNumber);
                    dynamicView.addViewLink(x.PT, x.PC, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));
                    dynamicView.addViewLink(x.PC, x.TM, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.contactMechId));

                    // filter on countryCode
                    String countryCode = (String) context.get(x.countryCode);
                    if (UtilValidate.isNotEmpty(countryCode)) {
                        paramList = paramList + x.countryCode_eaac9671 + countryCode;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.countryCode),
                                EntityOperator.EQUALS, EntityFunction.upper(countryCode)));
                    }

                    // filter on areaCode
                    String areaCode = (String) context.get(x.areaCode);
                    if (UtilValidate.isNotEmpty(areaCode)) {
                        paramList = paramList + x.areaCode_7247310a + areaCode;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.areaCode),
                                EntityOperator.EQUALS, EntityFunction.upper(areaCode)));
                    }

                    // filter on contact number
                    String contactNumber = (String) context.get(x.contactNumber);
                    if (UtilValidate.isNotEmpty(contactNumber)) {
                        paramList = paramList + x.contactNumber_0cc8dd09 + contactNumber;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.contactNumber),
                                EntityOperator.EQUALS, EntityFunction.upper(contactNumber)));
                    }

                    fieldsToSelect.add(x.contactNumber);
                    fieldsToSelect.add(x.areaCode);
                }

                // ---- End of Dynamic View Creation

                // build the main condition
                if (!andExprs.isEmpty()) {
                    mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
                }
            }

            Debug.logInfo(x.In_findParty_mainCond + mainCond, MODULE);

            String sortField = (String) context.get(x.sortField);
            if (UtilValidate.isNotEmpty(sortField)) {
                orderBy.add(sortField);
            }

            // do the lookup
            if (mainCond != null || x.Y.equals(showAll)) {
                lowIndex = viewIndex * viewSize + 1;
                highIndex = (viewIndex + 1) * viewSize;

                // set distinct on so we only get one row per order
                // using list iterator
                EntityFindOptions findOptions = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE,
                        EntityFindOptions.CONCUR_READ_ONLY, true);
                findOptions.setFetchSize(highIndex);
                try (EntityListIterator pli = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findIteratorByCondition(delegator,
                        dynamicView, mainCond, UtilMisc.toSet(fieldsToSelect), orderBy, findOptions)) {

                    // get the partial list for this page
                    partyList = pli.getPartialList(lowIndex, viewSize);

                    // attempt to get the full size
                    partyListSize = pli.getResultsSizeAfterPartialList();
                    if (highIndex > partyListSize) {
                        highIndex = partyListSize;
                    }

                } catch (GenericEntityException e) {
                    String errMsg = x.Failure_in_party_find_operation_rolling_back_transaction + e.toString();
                    Debug.logError(e, errMsg, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.PartyLookupPartyError,
                            UtilMisc.toMap(x.errMessage, e.toString()), locale));
                }
            } else {
                partyListSize = 0;
            }
        }

        if (partyList == null) {
            partyList = new LinkedList<>();
        }
        result.put(x.partyList, partyList);
        result.put(x.partyListSize, partyListSize);
        result.put(x.paramList, paramList);
        result.put(x.highIndex, highIndex);
        result.put(x.lowIndex, lowIndex);

        return result;
    }

    public static Map<String, Object> performFindParty(DispatchContext dctx, PartyServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        String extInfo = (String) context.get(x.extInfo);
        EntityCondition extCond = (EntityCondition) context.get(x.extCond);
        EntityListIterator listIt = null;

        // get the lookup flag
        String noConditionFind = (String) context.get(x.noConditionFind);

        // create the dynamic view entity
        DynamicViewEntity dynamicView = new DynamicViewEntity();

        // default view settings
        dynamicView.addMemberEntity(x.PT, x.Party);
        dynamicView.addAlias(x.PT, x.partyId);
        dynamicView.addAlias(x.PT, x.statusId);
        dynamicView.addAlias(x.PT, x.partyTypeId);
        dynamicView.addAlias(x.PT, x.externalId);
        dynamicView.addAlias(x.PT, x.createdDate);
        dynamicView.addAlias(x.PT, x.lastModifiedDate);
        dynamicView.addRelation(x.one_nofk, x.emptyString, x.PartyType, ModelKeyMap.makeKeyMapList(x.partyTypeId));
        dynamicView.addRelation(x.many, x.emptyString, x.UserLogin, ModelKeyMap.makeKeyMapList(x.partyId));

        // define the main condition & expression list
        List<EntityCondition> andExprs = new ArrayList<>();
        EntityCondition mainCond = null;

        List<String> orderBy = new ArrayList<>();
        String sortField = (String) context.get(x.sortField);
        if (UtilValidate.isNotEmpty(sortField)) {
            orderBy.add(sortField);
        }
        List<String> fieldsToSelect = new ArrayList<>();
        // fields we need to select; will be used to set distinct
        fieldsToSelect.add(x.partyId);
        fieldsToSelect.add(x.statusId);
        fieldsToSelect.add(x.partyTypeId);
        fieldsToSelect.add(x.externalId);
        fieldsToSelect.add(x.createdDate);
        fieldsToSelect.add(x.lastModifiedDate);

        // filter on parties that have relationship with logged in user
        String partyRelationshipTypeId = (String) context.get(x.partyRelationshipTypeId);
        if (UtilValidate.isNotEmpty(partyRelationshipTypeId)) {
            // add relation to view
            dynamicView.addMemberEntity(x.PRSHP, x.PartyRelationship);
            dynamicView.addAlias(x.PRSHP, x.partyIdTo);
            dynamicView.addAlias(x.PRSHP, x.partyRelationshipTypeId);
            dynamicView.addViewLink(x.PT, x.PRSHP, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId, x.partyIdTo));
            List<String> ownerPartyIds = UtilGenerics.cast(context.get(x.ownerPartyIds));
            EntityCondition relationshipCond = null;
            if (UtilValidate.isEmpty(ownerPartyIds)) {
                String partyIdFrom = userLogin.getString(x.partyId);
                relationshipCond = EntityCondition.makeCondition(EntityFunction.upperField(x.partyIdFrom),
                        EntityOperator.EQUALS, EntityFunction.upper(partyIdFrom));
            } else {
                relationshipCond = EntityCondition.makeCondition(x.partyIdFrom, EntityOperator.IN, ownerPartyIds);
            }
            dynamicView.addAlias(x.PRSHP, x.partyIdFrom);
            // add the expr
            andExprs.add(EntityCondition.makeCondition(
                    relationshipCond, EntityOperator.AND,
                    EntityCondition.makeCondition(EntityFunction.upperField(x.partyRelationshipTypeId), EntityOperator.EQUALS,
                            EntityFunction.upper(partyRelationshipTypeId))));
            fieldsToSelect.add(x.partyIdTo);
        }

        // get the params
        String partyId = (String) context.get(x.partyId);
        String partyTypeId = (String) context.get(x.partyTypeId);
        String roleTypeId = (String) context.get(x.roleTypeId);
        String statusId = (String) context.get(x.statusId);
        String userLoginId = (String) context.get(x.userLoginId);
        String externalId = (String) context.get(x.externalId);
        String firstName = (String) context.get(x.firstName);
        String lastName = (String) context.get(x.lastName);
        String groupName = (String) context.get(x.groupName);

        // check for a partyId
        if (UtilValidate.isNotEmpty(partyId)) {
            andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.partyId), EntityOperator.LIKE,
                    EntityFunction.upper(x.str_4345cb1f + partyId + x.str_4345cb1f)));
        }

        // now the statusId - send ANY for all statuses; leave null for just enabled; or pass a specific status
        if (UtilValidate.isNotEmpty(statusId)) {
            andExprs.add(EntityCondition.makeCondition(x.statusId, statusId));
        } else {
            // NOTE: _must_ explicitly allow null as it is not included in a not equal in many databases... odd but true
            andExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition(x.statusId, GenericEntity.NULL_FIELD),
                    EntityOperator.OR, EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.PARTY_DISABLED)));
        }
        // check for partyTypeId
        if (UtilValidate.isNotEmpty(partyTypeId)) {
            andExprs.add(EntityCondition.makeCondition(x.partyTypeId, partyTypeId));
        }

        if (UtilValidate.isNotEmpty(externalId)) {
            andExprs.add(EntityCondition.makeCondition(x.externalId, externalId));
        }
        // ----
        // UserLogin Fields
        // ----

        // filter on user login
        if (UtilValidate.isNotEmpty(userLoginId)) {

            // modify the dynamic view
            dynamicView.addMemberEntity(x.UL, x.UserLogin);
            dynamicView.addAlias(x.UL, x.userLoginId);
            dynamicView.addViewLink(x.PT, x.UL, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));

            // add the expr
            andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.userLoginId), EntityOperator.LIKE,
                    EntityFunction.upper(x.str_4345cb1f + userLoginId + x.str_4345cb1f)));
            fieldsToSelect.add(x.userLoginId);
        }

        // ----
        // PartyGroup Fields
        // ----

        // filter on groupName
        if (UtilValidate.isNotEmpty(groupName)) {

            // modify the dynamic view
            dynamicView.addMemberEntity(x.PG, x.PartyGroup);
            dynamicView.addAlias(x.PG, x.groupName);
            dynamicView.addViewLink(x.PT, x.PG, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));

            // add the expr
            andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.groupName), EntityOperator.LIKE,
                    EntityFunction.upper(x.str_4345cb1f + groupName + x.str_4345cb1f)));
            fieldsToSelect.add(x.groupName);
        }

        // ----
        // Person Fields
        // ----

        // modify the dynamic view
        if (UtilValidate.isNotEmpty(firstName) || UtilValidate.isNotEmpty(lastName)) {
            dynamicView.addMemberEntity(x.PE, x.Person);
            dynamicView.addAlias(x.PE, x.firstName);
            dynamicView.addAlias(x.PE, x.lastName);
            dynamicView.addViewLink(x.PT, x.PE, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));

            fieldsToSelect.add(x.firstName);
            fieldsToSelect.add(x.lastName);
            orderBy.add(x.lastName);
            orderBy.add(x.firstName);
        }

        // filter on firstName
        if (UtilValidate.isNotEmpty(firstName)) {
            andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.firstName), EntityOperator.LIKE,
                    EntityFunction.upper(x.str_4345cb1f + firstName + x.str_4345cb1f)));
        }

        // filter on lastName
        if (UtilValidate.isNotEmpty(lastName)) {
            andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.lastName), EntityOperator.LIKE,
                    EntityFunction.upper(x.str_4345cb1f + lastName + x.str_4345cb1f)));
        }

        // ----
        // RoleType Fields
        // ----

        // filter on role member
        if (UtilValidate.isNotEmpty(roleTypeId)) {

            // add role to view
            dynamicView.addMemberEntity(x.PR, x.PartyRole);
            dynamicView.addAlias(x.PR, x.roleTypeId);
            dynamicView.addViewLink(x.PT, x.PR, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));

            // add the expr
            andExprs.add(EntityCondition.makeCondition(x.roleTypeId, roleTypeId));
            fieldsToSelect.add(x.roleTypeId);
        }

        // ----
        // PartyClassificationGroup Fields
        // ----

        List<String> partyClassificationGroupIds = UtilGenerics.cast(context.get(x.partyClassificationGroupId));
        if (UtilValidate.isNotEmpty(partyClassificationGroupIds)) {
            // add PartyClassification to view
            dynamicView.addMemberEntity(x.PC, x.PartyClassification);
            dynamicView.addAlias(x.PC, x.partyClassificationGroupId);
            dynamicView.addViewLink(x.PT, x.PC, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));

            // add the expr
            andExprs.add(EntityCondition.makeCondition(x.partyClassificationGroupId, EntityOperator.IN, partyClassificationGroupIds));
            fieldsToSelect.add(x.partyClassificationGroupId);
        }

        // ----
        // PartyIdentification Fields
        // ----

        String idValue = (String) context.get(x.idValue);
        String partyIdentificationTypeId = (String) context.get(x.partyIdentificationTypeId);
        if (x.I.equals(extInfo) || UtilValidate.isNotEmpty(idValue) || UtilValidate.isNotEmpty(partyIdentificationTypeId)) {
            // add role to view
            dynamicView.addMemberEntity(x.PAI, x.PartyIdentification);
            dynamicView.addAlias(x.PAI, x.idValue);
            dynamicView.addAlias(x.PAI, x.partyIdentificationTypeId);
            dynamicView.addViewLink(x.PT, x.PAI, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));
            fieldsToSelect.add(x.idValue);
            fieldsToSelect.add(x.partyIdentificationTypeId);
            if (UtilValidate.isNotEmpty(idValue)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.idValue), EntityOperator.LIKE,
                        EntityFunction.upper(x.str_4345cb1f.concat(idValue).concat(x.str_4345cb1f))));
            }
            if (UtilValidate.isNotEmpty(partyIdentificationTypeId)) {
                andExprs.add(EntityCondition.makeCondition(x.partyIdentificationTypeId, partyIdentificationTypeId));
            }
        }

        // ----
        // InventoryItem Fields
        // ----

        // filter on inventory item's fields
        String inventoryItemId = (String) context.get(x.inventoryItemId);
        String serialNumber = (String) context.get(x.serialNumber);
        String softIdentifier = (String) context.get(x.softIdentifier);
        if (UtilValidate.isNotEmpty(inventoryItemId)
                || UtilValidate.isNotEmpty(serialNumber)
                || UtilValidate.isNotEmpty(softIdentifier)) {

            // add role to view
            dynamicView.addMemberEntity(x.II, x.InventoryItem);
            dynamicView.addAlias(x.II, x.ownerPartyId);
            dynamicView.addViewLink(x.PT, x.II, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId, x.ownerPartyId));
        }
        if (UtilValidate.isNotEmpty(inventoryItemId)) {
            dynamicView.addAlias(x.II, x.inventoryItemId);
            // add the expr
            andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.inventoryItemId), EntityOperator.LIKE,
                    EntityFunction.upper(x.str_4345cb1f + inventoryItemId + x.str_4345cb1f)));
            fieldsToSelect.add(x.inventoryItemId);
        }
        if (UtilValidate.isNotEmpty(serialNumber)) {
            dynamicView.addAlias(x.II, x.serialNumber);
            // add the expr
            andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.serialNumber), EntityOperator.LIKE,
                    EntityFunction.upper(x.str_4345cb1f + serialNumber + x.str_4345cb1f)));
            fieldsToSelect.add(x.serialNumber);
        }
        if (UtilValidate.isNotEmpty(softIdentifier)) {
            dynamicView.addAlias(x.II, x.softIdentifier);
            // add the expr
            andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.softIdentifier), EntityOperator.LIKE,
                    EntityFunction.upper(x.str_4345cb1f + softIdentifier + x.str_4345cb1f)));
            fieldsToSelect.add(x.softIdentifier);
        }

        // ----
        // PostalAddress fields
        // ----
        String stateProvinceGeoId = (String) context.get(x.stateProvinceGeoId);
        if (x.P.equals(extInfo)
                || UtilValidate.isNotEmpty(context.get(x.address1)) || UtilValidate.isNotEmpty(context.get(x.address2))
                || UtilValidate.isNotEmpty(context.get(x.city)) || UtilValidate.isNotEmpty(context.get(x.postalCode))
                || UtilValidate.isNotEmpty(context.get(x.countryGeoId)) || (UtilValidate.isNotEmpty(stateProvinceGeoId))) {
            // add address to dynamic view
            dynamicView.addMemberEntity(x.PC, x.PartyContactMech);
            dynamicView.addMemberEntity(x.PA, x.PostalAddress);
            dynamicView.addAlias(x.PC, x.contactMechId);
            dynamicView.addAlias(x.PA, x.address1);
            dynamicView.addAlias(x.PA, x.address2);
            dynamicView.addAlias(x.PA, x.city);
            dynamicView.addAlias(x.PA, x.stateProvinceGeoId);
            dynamicView.addAlias(x.PA, x.countryGeoId);
            dynamicView.addAlias(x.PA, x.postalCode);
            dynamicView.addViewLink(x.PT, x.PC, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));
            dynamicView.addViewLink(x.PC, x.PA, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.contactMechId));

            // filter on address1
            String address1 = (String) context.get(x.address1);
            if (UtilValidate.isNotEmpty(address1)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.address1), EntityOperator.LIKE,
                        EntityFunction.upper(x.str_4345cb1f + address1 + x.str_4345cb1f)));
            }

            // filter on address2
            String address2 = (String) context.get(x.address2);
            if (UtilValidate.isNotEmpty(address2)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.address2), EntityOperator.LIKE,
                        EntityFunction.upper(x.str_4345cb1f + address2 + x.str_4345cb1f)));
            }

            // filter on city
            String city = (String) context.get(x.city);
            if (UtilValidate.isNotEmpty(city)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.city), EntityOperator.LIKE,
                        EntityFunction.upper(x.str_4345cb1f + city + x.str_4345cb1f)));
            }

            // filter on state geo
            if (UtilValidate.isNotEmpty(stateProvinceGeoId)) {
                andExprs.add(EntityCondition.makeCondition(x.stateProvinceGeoId, stateProvinceGeoId));
            }

            // filter on postal code
            String postalCode = (String) context.get(x.postalCode);
            if (UtilValidate.isNotEmpty(postalCode)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.postalCode), EntityOperator.LIKE,
                        EntityFunction.upper(x.str_4345cb1f + postalCode + x.str_4345cb1f)));
            }

            fieldsToSelect.add(x.postalCode);
            fieldsToSelect.add(x.city);
            fieldsToSelect.add(x.stateProvinceGeoId);
        }

        // ----
        // Generic CM Fields
        // ----
        if (x.O.equals(extInfo) || UtilValidate.isNotEmpty(context.get(x.infoString))) {
            // add info to dynamic view
            dynamicView.addMemberEntity(x.PC, x.PartyContactMech);
            dynamicView.addMemberEntity(x.CM, x.ContactMech);
            dynamicView.addAlias(x.PC, x.contactMechId);
            dynamicView.addAlias(x.CM, x.infoString);
            dynamicView.addViewLink(x.PT, x.PC, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));
            dynamicView.addViewLink(x.PC, x.CM, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.contactMechId));

            // filter on infoString
            String infoString = (String) context.get(x.infoString);
            if (UtilValidate.isNotEmpty(infoString)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.infoString), EntityOperator.LIKE,
                        EntityFunction.upper(x.str_4345cb1f + infoString + x.str_4345cb1f)));
                fieldsToSelect.add(x.infoString);
            }
        }

        // ----
        // TelecomNumber Fields
        // ----
        if (x.T.equals(extInfo)
                || UtilValidate.isNotEmpty(context.get(x.countryCode))
                || UtilValidate.isNotEmpty(context.get(x.areaCode))
                || UtilValidate.isNotEmpty(context.get(x.contactNumber))) {
            // add telecom to dynamic view
            dynamicView.addMemberEntity(x.PC, x.PartyContactMech);
            dynamicView.addMemberEntity(x.TM, x.TelecomNumber);
            dynamicView.addAlias(x.PC, x.contactMechId);
            dynamicView.addAlias(x.TM, x.countryCode);
            dynamicView.addAlias(x.TM, x.areaCode);
            dynamicView.addAlias(x.TM, x.contactNumber);
            dynamicView.addViewLink(x.PT, x.PC, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.partyId));
            dynamicView.addViewLink(x.PC, x.TM, Boolean.FALSE, ModelKeyMap.makeKeyMapList(x.contactMechId));

            // filter on countryCode
            String countryCode = (String) context.get(x.countryCode);
            if (UtilValidate.isNotEmpty(countryCode)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.countryCode),
                        EntityOperator.EQUALS, EntityFunction.upper(countryCode)));
            }

            // filter on areaCode
            String areaCode = (String) context.get(x.areaCode);
            if (UtilValidate.isNotEmpty(areaCode)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.areaCode),
                        EntityOperator.EQUALS, EntityFunction.upper(areaCode)));
            }

            // filter on contact number
            String contactNumber = (String) context.get(x.contactNumber);
            if (UtilValidate.isNotEmpty(contactNumber)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.upperField(x.contactNumber),
                        EntityOperator.EQUALS, EntityFunction.upper(contactNumber)));
            }
            fieldsToSelect.add(x.contactNumber);
            fieldsToSelect.add(x.areaCode);
        }
        // ---- End of Dynamic View Creation

        // build the main condition, add the extend condition is it present
        if (UtilValidate.isNotEmpty(extCond)) {
            andExprs.add(extCond);
        }
        if (UtilValidate.isNotEmpty(andExprs)) {
            mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
        }
        if (Debug.infoOn()) {
            Debug.logInfo(x.In_findParty_mainCond + mainCond, MODULE);
        }

        // do the lookup
        if (UtilValidate.isNotEmpty(noConditionFind) && (x.Y.equals(noConditionFind) || andExprs.size() > 1)) {
            //exclude on condition the status expr
            try {
                // set distinct on so we only get one row per party
                // using list iterator
                EntityFindOptions findOptions = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE,
                        EntityFindOptions.CONCUR_READ_ONLY, true);
                listIt = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findIteratorByCondition(delegator, dynamicView, mainCond,
                        UtilMisc.toSet(fieldsToSelect), orderBy, findOptions);
            } catch (GenericEntityException e) {
                String errMsg = x.Failure_in_party_find_operation_rolling_back_transaction + e.toString();
                Debug.logError(e, errMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.PartyLookupPartyError,
                        UtilMisc.toMap(x.errMessage, e.toString()), locale));
            }
        }
        result.put(x.listIt, listIt);
        return result;
    }

    /**
     * Changes the association of contact mechs, purposes, notes, orders and attributes from
     * one party to another for the purpose of merging records together. Flags the from party
     * as disabled so it no longer appears in a search.
     * @param dctx the dispatch context
     * @param context the context
     * @return the result of the service execution
     */
    public static Map<String, Object> linkParty(DispatchContext dctx, PartyServicesContext context) {
        Delegator delegator = DelegatorFactory.getDelegator(x.default_no_eca);
        Locale locale = (Locale) context.get(x.locale);

        String partyIdTo = (String) context.get(x.partyIdTo);
        String partyId = (String) context.get(x.partyId);
        Timestamp now = UtilDateTime.nowTimestamp();

        if (partyIdTo.equals(partyId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyCannotLinkPartyToItSelf, locale));
        }

        // get the from/to party records
        GenericValue partyTo;
        try {
            partyTo = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Party, UtilMisc.toMap(x.partyId, partyIdTo),
                    false);
        } catch (GenericEntityException e) {
            Debug.logInfo(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (partyTo == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyPartyToDoesNotExists, locale));
        }
        if (x.PARTY_DISABLED.equals(partyTo.get(x.statusId))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyCannotMergeDisabledParty, locale));
        }

        GenericValue party;
        try {
            party = DaoRegistry.getDao(delegator, x.Party, PartyDao.class).findOne(delegator, x.Party, UtilMisc.toMap(x.partyId, partyId), false);
        } catch (GenericEntityException e) {
            Debug.logInfo(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (party == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyPartyFromDoesNotExists, locale));
        }

        // update the contact mech records
        try {
            delegator.storeByCondition(x.PartyContactMech, UtilMisc.<String, Object>toMap(x.partyId, partyIdTo, x.thruDate, now),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the contact mech purpose records
        try {
            delegator.storeByCondition(x.PartyContactMechPurpose, UtilMisc.<String, Object>toMap(x.partyId, partyIdTo, x.thruDate, now),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the party notes
        try {
            delegator.storeByCondition(x.PartyNote, UtilMisc.toMap(x.partyId, partyIdTo),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the inventory item(s)
        try {
            delegator.storeByCondition(x.InventoryItem, UtilMisc.toMap(x.ownerPartyId, partyIdTo),
                    EntityCondition.makeCondition(x.ownerPartyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the subscription
        try {
            delegator.storeByCondition(x.Subscription, UtilMisc.toMap(x.partyId, partyIdTo),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the userLogin records
        try {
            delegator.storeByCondition(x.UserLogin, UtilMisc.toMap(x.partyId, partyIdTo),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the non-existing party roles
        List<GenericValue> rolesToMove;
        try {
            rolesToMove = DaoRegistry.getDao(delegator, x.PartyRole, PartyRoleDao.class).findByAnd(delegator, x.PartyRole,
                    UtilMisc.toMap(x.partyId, partyId), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        for (GenericValue attr: rolesToMove) {
            attr.set(x.partyId, partyIdTo);
            try {
                if (DaoRegistry.getDao(delegator, x.PartyRole, PartyRoleDao.class).findOne(delegator, x.PartyRole, attr.getPrimaryKey(),
                        false) == null) {
                    attr.create();
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // update the order role records
        try {
            delegator.storeByCondition(x.OrderRole, UtilMisc.toMap(x.partyId, partyIdTo),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // invoice role
        try {
            delegator.storeByCondition(x.InvoiceRole, UtilMisc.toMap(x.partyId, partyIdTo),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // data RESOURCE role
        try {
            delegator.storeByCondition(x.DataResourceRole, UtilMisc.toMap(x.partyId, partyIdTo),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // content role
        try {
            delegator.storeByCondition(x.ContentRole, UtilMisc.toMap(x.partyId, partyIdTo),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the fin account
        try {
            delegator.storeByCondition(x.FinAccountRole, UtilMisc.toMap(x.partyId, partyIdTo),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the Product Store Role records
        try {
            delegator.storeByCondition(x.ProductStoreRole, UtilMisc.<String, Object>toMap(x.partyId, partyIdTo, x.thruDate, now),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        //  update the Communication Event Role records
        try {
            delegator.storeByCondition(x.CommunicationEventRole, UtilMisc.toMap(x.partyId, partyIdTo),
                    EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the non-existing attributes
        List<GenericValue> attrsToMove;
        try {
            attrsToMove = DaoRegistry.getDao(delegator, x.PartyAttribute, PartyAttributeDao.class).findByAnd(delegator, x.PartyAttribute,
                    UtilMisc.toMap(x.partyId, partyId), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        for (GenericValue attr: attrsToMove) {
            attr.set(x.partyId, partyIdTo);
            try {
                if (DaoRegistry.getDao(delegator, x.PartyAttribute, PartyAttributeDao.class).findOne(delegator, x.PartyAttribute,
                        attr.getPrimaryKey(), false) == null) {
                    attr.create();
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        try {
            delegator.removeByAnd(x.PartyAttribute, UtilMisc.toMap(x.partyId, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // create a party link attribute
        GenericValue linkAttr = delegator.makeValue(x.PartyAttribute);
        linkAttr.set(x.partyId, partyId);
        linkAttr.set(x.attrName, x.LINKED_TO);
        linkAttr.set(x.attrValue, partyIdTo);
        try {
            delegator.create(linkAttr);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // disable the party
        String currentStatus = party.getString(x.statusId);
        if (currentStatus == null || !x.PARTY_DISABLED.equals(currentStatus)) {
            party.set(x.statusId, x.PARTY_DISABLED);

            try {
                party.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, x.Error_setting_disable_mode_on_partyId + partyId, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        Map<String, Object> resp = ServiceUtil.returnSuccess();
        resp.put(x.partyId, partyIdTo);
        return resp;
    }

    public static Map<String, Object> importAddressMatchMapCsv(DispatchContext dctx, PartyServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        ByteBuffer fileBytes = (ByteBuffer) context.get(x.uploadedFile);
        String encoding = System.getProperty(x.file_encoding);
        String csvFile = Charset.forName(encoding).decode(fileBytes).toString();
        csvFile = csvFile.replaceAll(x.r, x.emptyString);
        String[] records = csvFile.split(x.n);

        for (int i = 0; i < records.length; i++) {
            if (records[i] != null) {
                String str = records[i].trim();
                String[] map = str.split(x.str_5c10b5b2);
                if (map.length != 2 && map.length != 3) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            x.PartyImportInvalidCsvFile, locale));
                }
                GenericValue addrMap = delegator.makeValue(x.AddressMatchMap);
                addrMap.put(x.mapKey, map[0].trim().toUpperCase(Locale.getDefault()));
                addrMap.put(x.mapValue, map[1].trim().toUpperCase(Locale.getDefault()));
                int seq = i + 1;
                if (map.length == 3) {
                    char[] chars = map[2].toCharArray();
                    boolean isNumber = true;
                    for (char c : chars) {
                        if (!Character.isDigit(c)) {
                            isNumber = false;
                        }
                    }
                    if (isNumber) {
                        try {
                            seq = Integer.parseInt(map[2]);
                        } catch (Throwable t) {
                            Debug.logWarning(t, x.Unable_to_parse_number, MODULE);
                        }
                    }
                }

                addrMap.put(x.sequenceNum, (long) seq);
                Debug.logInfo(x.Creating_map_entry + addrMap, MODULE);
                try {
                    delegator.create(addrMap);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.PartyImportNoRecordsFoundInFile, locale));
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static String getPartyId(PartyServicesContext context) {
        String partyId = (String) context.get(x.partyId);
        if (UtilValidate.isEmpty(partyId)) {
            GenericValue userLogin = (GenericValue) context.get(x.userLogin);
            if (userLogin != null) {
                partyId = userLogin.getString(x.partyId);
            }
        }
        return partyId;
    }
    /**
     * Finds partyId(s) corresponding to a party reference, partyId or a GoodIdentification idValue
     * @param ctx the dispatch context
     * @param context use to search with partyId or goodIdentification.idValue
     * @return a GenericValue with a partyId and a List of complementary partyId found
     */
    public static Map<String, Object> findPartyById(DispatchContext ctx, PartyServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        String idToFind = (String) context.get(x.idToFind);
        String partyIdentificationTypeId = (String) context.get(x.partyIdentificationTypeId);
        String searchPartyFirstContext = (String) context.get(x.searchPartyFirst);
        String searchAllIdContext = (String) context.get(x.searchAllId);

        boolean searchPartyFirst = !UtilValidate.isNotEmpty(searchPartyFirstContext) || !x.N.equals(searchPartyFirstContext);
        boolean searchAllId = UtilValidate.isNotEmpty(searchAllIdContext) && x.Y.equals(searchAllIdContext);

        GenericValue party = null;
        List<GenericValue> partiesFound = null;
        try {
            partiesFound = PartyWorker.findPartiesById(delegator, idToFind, partyIdentificationTypeId, searchPartyFirst, searchAllId);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (UtilValidate.isNotEmpty(partiesFound)) {
            // gets the first partyId of the List
            party = EntityUtil.getFirst(partiesFound);
            // remove this partyId
            partiesFound.remove(0);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.party, party);
        result.put(x.partiesFound, partiesFound);

        return result;
    }

    public static Map<String, Object> importParty(DispatchContext dctx, PartyServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        ByteBuffer fileBytes = (ByteBuffer) context.get(x.uploadedFile);
        String encoding = System.getProperty(x.file_encoding);
        String csvString = Charset.forName(encoding).decode(fileBytes).toString();
        Builder csvFormatBuilder = Builder.create().setHeader();
        CSVFormat fmt = csvFormatBuilder.build();
        List<String> errMsgs = new LinkedList<>();
        List<String> newErrMsgs = new LinkedList<>();
        String lastPartyId = null;        // last partyId read from the csv file
        String currentPartyId = null;     // current partyId from the csv file
        String newPartyId = null;        // new to create/update partyId in the system
        String newCompanyPartyId = null;
        int partiesCreated = 0;
        Map<String, Object> result = null;
        String newContactMechId = null;
        String currentContactMechTypeId = null;

        String lastAddress1 = null;
        String lastAddress2 = null;
        String lastCity = null;
        String lastCountryGeoId = null;

        String lastEmailAddress = null;

        String lastCountryCode = null;
        String lastAreaCode = null;
        String lastContactNumber = null;

        String lastContactMechPurposeTypeId = null;
        String currentContactMechPurposeTypeId = null;

        boolean addParty = false; // when modify party, contact mech not added again


        try (BufferedReader csvReader = new BufferedReader(new StringReader(csvString))) {
            for (CSVRecord rec : fmt.parse(csvReader)) {
                if (UtilValidate.isNotEmpty(rec.get(x.partyId))) {
                    currentPartyId = rec.get(x.partyId);
                }
                if (lastPartyId == null || !currentPartyId.equals(lastPartyId)) {
                    newPartyId = null;
                    currentContactMechPurposeTypeId = null;
                    lastAddress1 = null;
                    lastAddress2 = null;
                    lastCity = null;
                    lastCountryGeoId = null;

                    lastEmailAddress = null;

                    lastCountryCode = null;
                    lastAreaCode = null;
                    lastContactNumber = null;

                    // party validation
                    List<GenericValue> currencyCheck = DaoRegistry.getDao(delegator, x.Uom, UomDao.class).findByAnd(delegator, x.Uom,
                            UtilMisc.toMap(x.abbreviation, rec.get(x.preferredCurrencyUomId), x.uomTypeId, x.CURRENCY_MEASURE), null, false);
                    if (UtilValidate.isNotEmpty(rec.get(x.preferredCurrencyUomId)) && currencyCheck.isEmpty()) {
                        newErrMsgs.add(x.Line_number + rec.getRecordNumber() + x.partyId_57082245 + currentPartyId + x.Currency_code_not_found_for
                                + rec.get(x.preferredCurrencyUomId));
                    }

                    if (UtilValidate.isEmpty(rec.get(x.roleTypeId))) {
                        newErrMsgs.add(x.Line_number + rec.getRecordNumber()
                                + x.Mandatory_roletype_is_missing_possible_values_CUSTOMER_SUPPLIER_EMPLOYEE_and_more);
                    } else if (DaoRegistry.getDao(delegator, x.RoleType, RoleTypeDao.class).findOne(delegator, x.RoleType,
                            UtilMisc.toMap(x.roleTypeId, rec.get(x.roleTypeId)), false) == null) {
                        newErrMsgs.add(x.Line_number + rec.getRecordNumber() + x.RoletypeId_is_not_valid + rec.get(x.roleTypeId));
                    }

                    if (UtilValidate.isNotEmpty(rec.get(x.contactMechTypeId))
                            && DaoRegistry.getDao(delegator, x.ContactMechType, ContactMechTypeDao.class).findOne(delegator, x.ContactMechType,
                                    UtilMisc.toMap(x.contactMechTypeId, rec.get(x.contactMechTypeId)), true) == null) {
                        newErrMsgs.add(x.Line_number + rec.getRecordNumber() + x.partyId_57082245 + currentPartyId
                                + x.contactMechTypeId_code_not_found_for
                                + rec.get(x.contactMechTypeId));
                    }

                    if (UtilValidate.isNotEmpty(rec.get(x.contactMechPurposeTypeId))
                            && DaoRegistry.getDao(delegator, x.ContactMechPurposeType, ContactMechPurposeTypeDao.class).findOne(delegator,
                                    x.ContactMechPurposeType,
                                    UtilMisc.toMap(x.contactMechPurposeTypeId, rec.get(x.contactMechPurposeTypeId)), true) == null) {
                        newErrMsgs.add(x.Line_number + rec.getRecordNumber() + x.partyId_57082245 + currentPartyId
                                + x.contactMechPurposeTypeId_code_not_found_for + rec.get(x.contactMechPurposeTypeId));
                    }

                    if (UtilValidate.isNotEmpty(rec.get(x.contactMechTypeId)) && x.POSTAL_ADDRESS.equals(rec.get(x.contactMechTypeId))) {
                        if (UtilValidate.isEmpty(rec.get(x.countryGeoId))) {
                            newErrMsgs.add(x.Line_number + rec.getRecordNumber() + x.partyId_57082245 + currentPartyId + x.Country_code_missing);
                        } else {
                            List<GenericValue> countryCheck = DaoRegistry.getDao(delegator, x.Geo, GeoDao.class).findByAnd(delegator, x.Geo,
                                    UtilMisc.toMap(x.geoTypeId, x.COUNTRY, x.abbreviation, rec.get(x.countryGeoId)), null, false);
                            if (countryCheck.isEmpty()) {
                                newErrMsgs.add(x.Line_number + rec.getRecordNumber() + x.partyId_cb0b719b + currentPartyId + x.Invalid_Country_code
                                        + rec.get(x.countryGeoId));
                            }
                        }

                        if (UtilValidate.isEmpty(rec.get(x.city))) {
                            newErrMsgs.add(x.Line_number + rec.getRecordNumber() + x.partyId_cb0b719b + currentPartyId + x.City_name_is_missing);
                        }

                        if (UtilValidate.isNotEmpty(rec.get(x.stateProvinceGeoId))) {
                            List<GenericValue> stateCheck = DaoRegistry.getDao(delegator, x.Geo, GeoDao.class).findByAnd(delegator, x.Geo,
                                    UtilMisc.toMap(x.geoTypeId, x.STATE, x.abbreviation, rec.get(x.stateProvinceGeoId)), null, false);
                            if (stateCheck.isEmpty()) {
                                newErrMsgs.add(x.Line_number + rec.getRecordNumber() + x.partyId_cb0b719b + currentPartyId
                                        + x.Invalid_stateProvinceGeoId_code + rec.get(x.countryGeoId));
                            }
                        }
                    }

                    if (UtilValidate.isNotEmpty(rec.get(x.contactMechTypeId)) && x.TELECOM_NUMBER.equals(rec.get(x.contactMechTypeId))) {
                        if (UtilValidate.isEmpty(rec.get(x.telAreaCode)) && UtilValidate.isEmpty(rec.get(x.telAreaCode))) {
                            newErrMsgs.add(x.Line_number + rec.getRecordNumber() + x.partyId_cb0b719b + currentPartyId + x.telephone_number_missing);
                        }
                    }

                    if (UtilValidate.isNotEmpty(rec.get(x.contactMechTypeId)) && x.EMAIL_ADDRESS.equals(rec.get(x.contactMechTypeId))) {
                        if (UtilValidate.isEmpty(rec.get(x.emailAddress))) {
                            newErrMsgs.add(x.Line_number + rec.getRecordNumber() + x.partyId_cb0b719b + currentPartyId + x.email_address_missing);
                        }
                    }

                    if (errMsgs.isEmpty()) {
                        List<GenericValue> partyCheck = DaoRegistry.getDao(delegator, x.PartyIdentification, PartyIdentificationDao.class).findByAnd(
                                delegator, x.PartyIdentification,
                                UtilMisc.toMap(x.partyIdentificationTypeId, x.PARTY_IMPORT, x.idValue, rec.get(x.partyId)), null, false);
                        addParty = partyCheck.isEmpty();
                        if (!addParty) { // update party
                            newPartyId = EntityUtil.getFirst(partyCheck).getString(x.partyId);

                            if (UtilValidate.isNotEmpty(rec.get(x.groupName))) {
                                Map<String, Object> partyGroup = UtilMisc.toMap(
                                        x.partyId, newPartyId,
                                        x.preferredCurrencyUomId, rec.get(x.preferredCurrencyUomId),
                                        x.groupName, rec.get(x.groupName),
                                        x.userLogin, userLogin);
                                result = dispatcher.runSync(x.updatePartyGroup, partyGroup);
                                if (ServiceUtil.isError(result)) {
                                    // Eclipse reports here: Resource leak: '<unassigned Closeable value>' is not closed at this location
                                    // but it's OK. As csvReader is in a try-with-ressource it will be closed anyway
                                    // I prefer to not put @SuppressWarnings("resource") to the whole method
                                    // BTW to be consistent Eclipse should also reports the same issue below
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                                }
                            } else { // person
                                Map<String, Object> person = UtilMisc.toMap(
                                        x.partyId, newPartyId,
                                        x.firstName, rec.get(x.firstName),
                                        x.middleName, rec.get(x.middleName),
                                        x.lastName, rec.get(x.lastName),
                                        x.preferredCurrencyUomId, rec.get(x.preferredCurrencyUomId),
                                        x.userLogin, userLogin);
                                result = dispatcher.runSync(x.updatePerson, person);
                                if (ServiceUtil.isError(result)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                                }
                            }

                        } else { // create new party
                            if (UtilValidate.isNotEmpty(rec.get(x.groupName))) {
                                Map<String, Object> partyGroup = UtilMisc.toMap(
                                        x.preferredCurrencyUomId, rec.get(x.preferredCurrencyUomId),
                                        x.groupName, rec.get(x.groupName),
                                        x.userLogin, userLogin,
                                        x.statusId, x.PARTY_ENABLED);
                                result = dispatcher.runSync(x.createPartyGroup, partyGroup);
                                if (ServiceUtil.isError(result)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                                }
                            } else { // person
                                Map<String, Object> person = UtilMisc.toMap(
                                        x.firstName, rec.get(x.firstName),
                                        x.middleName, rec.get(x.middleName),
                                        x.lastName, rec.get(x.lastName),
                                        x.preferredCurrencyUomId, rec.get(x.preferredCurrencyUomId),
                                        x.statusId, x.PARTY_ENABLED,
                                        x.userLogin, userLogin);
                                result = dispatcher.runSync(x.createPerson, person);
                                if (ServiceUtil.isError(result)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                                }
                            }
                            newPartyId = (String) result.get(x.partyId);

                            Map<String, Object> partyIdentification = UtilMisc.toMap(x.partyId, newPartyId,
                                    x.partyIdentificationTypeId, x.PARTY_IMPORT, x.idValue, rec.get(x.partyId), x.userLogin, userLogin);

                            result = dispatcher.runSync(x.createPartyIdentification, partyIdentification);
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }

                            Map<String, Object> partyRole = UtilMisc.toMap(x.partyId, newPartyId, x.roleTypeId, rec.get(x.roleTypeId),
                                    x.userLogin, userLogin);
                            dispatcher.runSync(x.createPartyRole, partyRole);
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }

                            if (UtilValidate.isNotEmpty(rec.get(x.companyPartyId))) {
                                List<GenericValue> companyCheck = DaoRegistry.getDao(delegator, x.PartyIdentification,
                                        PartyIdentificationDao.class).findByAnd(delegator, x.PartyIdentification,
                                                UtilMisc.toMap(x.partyIdentificationTypeId, x.PARTY_IMPORT, x.idValue, rec.get(x.partyId)), null,
                                                false);
                                if (companyCheck.isEmpty()) { // update party group
                                    // company does not exist so create
                                    Map<String, Object> companyPartyGroup = UtilMisc.toMap(
                                            x.partyId, newCompanyPartyId, x.statusId, x.PARTY_ENABLED, x.userLogin, userLogin);
                                    result = dispatcher.runSync(x.createPartyGroup, companyPartyGroup);
                                    if (ServiceUtil.isError(result)) {
                                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                                    }
                                    newCompanyPartyId = (String) result.get(x.partyId);
                                } else {
                                    newCompanyPartyId = EntityUtil.getFirst(companyCheck).getString(x.partyId);
                                }

                                Map<String, Object> companyRole = UtilMisc.toMap(
                                        x.partyId, newCompanyPartyId, x.roleTypeId, x.ACCOUNT, x.userLogin, userLogin);
                                Map<String, Object> serviceResult = dispatcher.runSync(x.createPartyRole, companyRole);
                                if (ServiceUtil.isError(serviceResult)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                                }

                                // company exist, so create link
                                Map<String, Object> partyRelationship = UtilMisc.toMap(x.partyIdTo, newPartyId, x.partyIdFrom, newCompanyPartyId,
                                        x.roleTypeIdFrom, x.ACCOUNT, x.partyRelationshipTypeId, x.EMPLOYMENT, x.userLogin, userLogin);
                                result = dispatcher.runSync(x.createPartyRelationship, partyRelationship);
                                if (ServiceUtil.isError(result)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                                }
                            }
                        }
                        Debug.logInfo(x.New_party_created_with_id + newPartyId, MODULE);
                        partiesCreated++;
                    } else {
                        errMsgs.addAll(newErrMsgs);
                        newErrMsgs = new LinkedList<>();
                    }
                }

                currentContactMechTypeId = rec.get(x.contactMechTypeId);
                currentContactMechPurposeTypeId = rec.get(x.contactMechPurposeTypeId);
                // party correctly created (not updated) and contactMechtype provided?
                if (newPartyId != null && addParty && UtilValidate.isNotEmpty(currentContactMechTypeId)) {

                    // fill maps and check changes
                    Map<String, Object> emailAddress = UtilMisc.toMap(x.contactMechTypeId, x.EMAIL_ADDRESS, x.userLogin, userLogin);
                    boolean emailAddressChanged = false;
                    if (x.EMAIL_ADDRESS.equals(currentContactMechTypeId)) {
                        emailAddress.put(x.infoString, rec.get(x.emailAddress));
                        emailAddressChanged = lastEmailAddress == null || !lastEmailAddress.equals(rec.get(x.emailAddress));
                        lastEmailAddress = rec.get(x.emailAddress);
                    }

                    Map<String, Object> postalAddress = UtilMisc.toMap(x.userLogin, (Object) userLogin);
                    // casting is here necessary for some compiler versions

                    boolean postalAddressChanged = false;
                    if (x.POSTAL_ADDRESS.equals(currentContactMechTypeId)) {
                        postalAddress.put(x.address1, rec.get(x.address1));
                        postalAddress.put(x.address2, rec.get(x.address2));
                        postalAddress.put(x.city, rec.get(x.city));
                        postalAddress.put(x.stateProvinceGeoId, rec.get(x.stateProvinceGeoId));
                        postalAddress.put(x.countryGeoId, rec.get(x.countryGeoId));
                        postalAddress.put(x.postalCode, rec.get(x.postalCode));
                        postalAddressChanged =
                                lastAddress1 == null || !lastAddress1.equals(postalAddress.get(x.address1))
                                || lastAddress2 == null || !lastAddress2.equals(postalAddress.get(x.address2))
                                || lastCity == null || !lastCity.equals(postalAddress.get(x.city))
                                || lastCountryGeoId == null || !lastCountryGeoId.equals(postalAddress.get(x.countryGeoId));
                        lastAddress1 = (String) postalAddress.get(x.address1);
                        lastAddress2 = (String) postalAddress.get(x.address2);
                        lastCity = (String) postalAddress.get(x.city);
                        lastCountryGeoId = (String) postalAddress.get(x.countryGeoId);
                    }

                    Map<String, Object> telecomNumber = UtilMisc.toMap(x.userLogin, (Object) userLogin);
                    // casting is here necessary for some compiler versions

                    boolean telecomNumberChanged = false;
                    if (x.TELECOM_NUMBER.equals(currentContactMechTypeId)) {
                        telecomNumber.put(x.countryCode, rec.get(x.telCountryCode));
                        telecomNumber.put(x.areaCode, rec.get(x.telAreaCode));
                        telecomNumber.put(x.contactNumber, rec.get(x.telContactNumber));
                        telecomNumberChanged =
                                lastCountryCode == null || !lastCountryCode.equals(telecomNumber.get(x.countryCode))
                                || lastAreaCode == null || !lastAreaCode.equals(telecomNumber.get(x.areaCode))
                                || lastContactNumber == null || !lastContactNumber.equals(telecomNumber.get(x.contactNumber));
                        lastCountryCode = (String) telecomNumber.get(x.countryCode);
                        lastAreaCode = (String) telecomNumber.get(x.areaCode);
                        lastContactNumber = (String) telecomNumber.get(x.contactNumber);
                    }

                    Map<String, Object> partyContactMechPurpose = UtilMisc.toMap(x.partyId, newPartyId, x.userLogin, userLogin);
                    boolean partyContactMechPurposeChanged = false;
                    currentContactMechPurposeTypeId = rec.get(x.contactMechPurposeTypeId);
                    if (currentContactMechPurposeTypeId != null && (x.TELECOM_NUMBER.equals(currentContactMechTypeId)
                            || x.POSTAL_ADDRESS.equals(currentContactMechTypeId) || x.EMAIL_ADDRESS.equals(currentContactMechTypeId))) {
                        partyContactMechPurpose.put(x.contactMechPurposeTypeId, currentContactMechPurposeTypeId);
                        partyContactMechPurpose.put(x.contactMechTypeId, currentContactMechTypeId);
                        partyContactMechPurposeChanged = (lastContactMechPurposeTypeId == null
                                || !lastContactMechPurposeTypeId.equals(currentContactMechPurposeTypeId)) && !telecomNumberChanged
                                && !postalAddressChanged && !emailAddressChanged;
                        Debug.logInfo(x.Last + lastContactMechPurposeTypeId + x.current + currentContactMechPurposeTypeId + x.t
                                + telecomNumberChanged + x.p + postalAddressChanged + x.e + emailAddressChanged + x.result_6c4ed479
                                + partyContactMechPurposeChanged, MODULE);
                    }
                    lastContactMechPurposeTypeId = currentContactMechPurposeTypeId;

                    // update
                    if (errMsgs.isEmpty()) {

                        if (postalAddressChanged) {
                            result = dispatcher.runSync(x.createPostalAddress, postalAddress);
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                            newContactMechId = (String) result.get(x.contactMechId);
                            if (currentContactMechPurposeTypeId == null) {
                                currentContactMechPurposeTypeId = x.GENERAL_LOCATION;
                            }
                            Map<String, Object> serviceResult = dispatcher.runSync(x.createPartyContactMech, UtilMisc.toMap(x.partyId, newPartyId,
                                    x.contactMechId, newContactMechId, x.contactMechTypeId, currentContactMechTypeId,
                                    x.contactMechPurposeTypeId, currentContactMechPurposeTypeId, x.userLogin, userLogin));
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        }

                        if (telecomNumberChanged) {
                            result = dispatcher.runSync(x.createTelecomNumber, telecomNumber);
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                            newContactMechId = (String) result.get(x.contactMechId);
                            if (currentContactMechPurposeTypeId == null) {
                                currentContactMechPurposeTypeId = x.PHONE_WORK;
                            }
                            Map<String, Object> resultMap = dispatcher.runSync(x.createPartyContactMech, UtilMisc.toMap(x.partyId, newPartyId,
                                    x.contactMechId, newContactMechId, x.contactMechTypeId, currentContactMechTypeId,
                                    x.contactMechPurposeTypeId, currentContactMechPurposeTypeId, x.userLogin, userLogin));
                            if (ServiceUtil.isError(resultMap)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                        }

                        if (emailAddressChanged) {
                            result = dispatcher.runSync(x.createContactMech, emailAddress);
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                            newContactMechId = (String) result.get(x.contactMechId);
                            if (currentContactMechPurposeTypeId == null) {
                                currentContactMechPurposeTypeId = x.PRIMARY_EMAIL;
                            }
                            Map<String, Object> resultMap = dispatcher.runSync(x.createPartyContactMech, UtilMisc.toMap(x.partyId, newPartyId,
                                    x.contactMechId, newContactMechId, x.contactMechTypeId, currentContactMechTypeId,
                                    x.contactMechPurposeTypeId, currentContactMechPurposeTypeId, x.userLogin, userLogin));
                            if (ServiceUtil.isError(resultMap)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                        }

                        if (partyContactMechPurposeChanged) {
                            partyContactMechPurpose.put(x.contactMechId, newContactMechId);
                            result = dispatcher.runSync(x.createPartyContactMechPurpose, partyContactMechPurpose);
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                        }
                        lastPartyId = currentPartyId;
                        errMsgs.addAll(newErrMsgs);
                        newErrMsgs = new LinkedList<>();
                    }
                }
            }
        } catch (GenericServiceException | GenericEntityException | IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (!errMsgs.isEmpty()) {
            return ServiceUtil.returnError(errMsgs);
        }

        result = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE, x.PartyNewPartiesCreated,
                UtilMisc.toMap(x.partiesCreated, partiesCreated), locale));
        return result;
    }
}
