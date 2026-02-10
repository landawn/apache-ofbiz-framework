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

package org.apache.ofbiz.party.contact;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;



import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ContactMechServicesContext;
/**
 * Services for Contact Mechanism maintenance
 */
public class ContactMechServices {

    private static final String MODULE = ContactMechServices.class.getName();
    private static final String RESOURCE = x.PartyUiLabels;
    private static final String RES_ERROR = x.PartyErrorUiLabels;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Creates a ContactMech
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createContactMech(DispatchContext ctx, ContactMechServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PARTYMGR, x.PCM_CREATE);

        if (!result.isEmpty()) {
            return result;
        }

        String contactMechTypeId = (String) context.get(x.contactMechTypeId);

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId(x.ContactMech);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_create_contact_info_id_generation_failure, locale));
        }

        GenericValue tempContactMech = delegator.makeValue(x.ContactMech, UtilMisc.toMap(x.contactMechId, newCmId,
                x.contactMechTypeId, contactMechTypeId));
        toBeStored.add(tempContactMech);

        if (!x.NA.equals(partyId)) {
            toBeStored.add(delegator.makeValue(x.PartyContactMech, UtilMisc.toMap(x.partyId, partyId, x.contactMechId, newCmId,
                    x.fromDate, now, x.roleTypeId, context.get(x.roleTypeId), x.allowSolicitation, context.get(x.allowSolicitation),
                    x.extension, context.get(x.extension))));
        }

        if (x.POSTAL_ADDRESS.equals(contactMechTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_service_createContactMech_not_be_used_for_POSTAL_ADDRESS, locale));
        } else if (x.TELECOM_NUMBER.equals(contactMechTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_service_createContactMech_not_be_used_for_TELECOM_NUMBER, locale));
        } else {
            tempContactMech.set(x.infoString, context.get(x.infoString));
        }

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_create_contact_info_write,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }

        result.put(x.contactMechId, newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates a ContactMech
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_UPDATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateContactMech(DispatchContext ctx, ContactMechServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();
        boolean isModified = false;

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PARTYMGR, x.PCM_UPDATE);

        if (!result.isEmpty()) {
            return result;
        }

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId(x.ContactMech);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_change_contact_info_id_generation_failure, locale));
        }

        String contactMechId = (String) context.get(x.contactMechId);
        GenericValue contactMech;
        GenericValue partyContactMech = null;

        try {
            UserLoginDao contactMechDao = DaoRegistry.getDao(delegator, x.ContactMech, UserLoginDao.class);
            contactMech = contactMechDao.findOne(delegator, x.ContactMech, UtilMisc.toMap(x.contactMechId, contactMechId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            contactMech = null;
        }

        if (!x.NA.equals(partyId)) {
            // try to find a PartyContactMech with a valid date range
            try {
                UserLoginDao partyContactMechDao = DaoRegistry.getDao(delegator, x.PartyContactMech, UserLoginDao.class);
                List<GenericValue> partyContactMechs = partyContactMechDao.findByAnd(delegator, x.PartyContactMech,
                        UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId), UtilMisc.toList(x.fromDate), false);
                partyContactMech = EntityUtil.getFirst(EntityUtil.filterByDate(partyContactMechs, true));
                if (partyContactMech == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.contactmechservices_cannot_update_specified_contact_info_not_corresponds, locale));
                }
                toBeStored.add(partyContactMech);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                contactMech = null;
            }
        }
        if (contactMech == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_find_specified_contact_info_read, locale));
        }

        String contactMechTypeId = contactMech.getString(x.contactMechTypeId);

        // never change a contact mech, just create a new one with the changes
        GenericValue newContactMech = GenericValue.create(contactMech);
        GenericValue newPartyContactMech = GenericValue.create(partyContactMech);

        if (x.POSTAL_ADDRESS.equals(contactMechTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_service_updateContactMech_not_be_used_for_POSTAL_ADDRESS, locale));
        } else if (x.TELECOM_NUMBER.equals(contactMechTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_service_updateContactMech_not_be_used_for_TELECOM_NUMBER, locale));
        } else {
            newContactMech.set(x.infoString, context.get(x.infoString));
        }

        newPartyContactMech.set(x.roleTypeId, context.get(x.roleTypeId));
        newPartyContactMech.set(x.allowSolicitation, context.get(x.allowSolicitation));

        if (!newContactMech.equals(contactMech)) {
            isModified = true;
        }
        if (!newPartyContactMech.equals(partyContactMech)) {
            isModified = true;
        }

        toBeStored.add(newContactMech);
        toBeStored.add(newPartyContactMech);

        if (isModified) {
            newContactMech.set(x.contactMechId, newCmId);
            newPartyContactMech.set(x.contactMechId, newCmId);
            newPartyContactMech.set(x.fromDate, now);
            newPartyContactMech.set(x.thruDate, null);

            try {
                Iterator<GenericValue> partyContactMechPurposes = UtilMisc.toIterator(partyContactMech.getRelated(x.PartyContactMechPurpose,
                        null, null, false));

                while (partyContactMechPurposes != null && partyContactMechPurposes.hasNext()) {
                    GenericValue tempVal = GenericValue.create(partyContactMechPurposes.next());

                    tempVal.set(x.contactMechId, newCmId);
                    toBeStored.add(tempVal);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.contactmechservices_could_not_change_contact_info_read,
                        UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
            }

            partyContactMech.set(x.thruDate, now);
            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.contactmechservices_could_not_change_contact_info_write,
                        UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
            }
        } else {
            result.put(x.newContactMechId, contactMechId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RES_ERROR,
                       x.contactmechservices_no_changes_made_not_updating, locale));
            return result;
        }

        result.put(x.newContactMechId, newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Deletes a ContactMech
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_DELETE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> deleteContactMech(DispatchContext ctx, ContactMechServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PARTYMGR, x.PCM_DELETE);

        if (!result.isEmpty()) {
            return result;
        }

        // never delete a contact mechanism, just put a to date on the link to the party
        String contactMechId = (String) context.get(x.contactMechId);
        GenericValue partyContactMech = null;

        try {
            // try to find a PartyContactMech with a valid date range
            UserLoginDao partyContactMechDao = DaoRegistry.getDao(delegator, x.PartyContactMech, UserLoginDao.class);
            List<GenericValue> partyContactMechs = partyContactMechDao.findByAnd(delegator, x.PartyContactMech,
                    UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId), UtilMisc.toList(x.fromDate), false);
            partyContactMech = EntityUtil.getFirst(EntityUtil.filterByDate(partyContactMechs, true));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_delete_contact_info_read,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }

        if (partyContactMech == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_delete_contact_info_no_contact_found, locale));
        }

        partyContactMech.set(x.thruDate, UtilDateTime.nowTimestamp());
        try {
            partyContactMech.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_delete_contact_info_write, locale));
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    // ============================================================================
    // ============================================================================

    /**
     * Creates a PostalAddress
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createPostalAddress(DispatchContext ctx, ContactMechServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PARTYMGR, x.PCM_CREATE);

        if (!result.isEmpty()) {
            return result;
        }

        String contactMechTypeId = x.POSTAL_ADDRESS;

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId(x.ContactMech);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_create_contact_info_id_generation_failure, locale));
        }

        GenericValue tempContactMech = delegator.makeValue(x.ContactMech, UtilMisc.toMap(x.contactMechId, newCmId,
                x.contactMechTypeId, contactMechTypeId));
        toBeStored.add(tempContactMech);

        // don't create a PartyContactMech if there is no party; we define no party as sending _NA_ as partyId
        if (!x.NA.equals(partyId)) {
            toBeStored.add(delegator.makeValue(x.PartyContactMech,
                    UtilMisc.toMap(x.partyId, partyId, x.contactMechId, newCmId,
                        x.fromDate, now, x.roleTypeId, context.get(x.roleTypeId), x.allowSolicitation,
                        context.get(x.allowSolicitation), x.extension, context.get(x.extension))));
        }

        GenericValue newAddr = delegator.makeValue(x.PostalAddress);

        newAddr.set(x.contactMechId, newCmId);
        newAddr.set(x.toName, context.get(x.toName));
        newAddr.set(x.attnName, context.get(x.attnName));
        newAddr.set(x.address1, context.get(x.address1));
        newAddr.set(x.address2, context.get(x.address2));
        newAddr.set(x.directions, context.get(x.directions));
        newAddr.set(x.city, context.get(x.city));
        newAddr.set(x.postalCode, context.get(x.postalCode));
        newAddr.set(x.postalCodeExt, context.get(x.postalCodeExt));
        newAddr.set(x.stateProvinceGeoId, context.get(x.stateProvinceGeoId));
        newAddr.set(x.countryGeoId, context.get(x.countryGeoId));
        newAddr.set(x.postalCodeGeoId, context.get(x.postalCodeGeoId));
        toBeStored.add(newAddr);

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_create_contact_info_write,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }

        result.put(x.contactMechId, newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates a PostalAddress
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_UPDATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updatePostalAddress(DispatchContext ctx, ContactMechServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();
        boolean isModified = false;

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PARTYMGR, x.PCM_UPDATE);

        if (!result.isEmpty()) {
            return result;
        }

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId(x.ContactMech);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_change_contact_info_id_generation_failure, locale));
        }

        String contactMechId = (String) context.get(x.contactMechId);
        GenericValue contactMech;
        GenericValue partyContactMech = null;

        try {
            UserLoginDao contactMechDao = DaoRegistry.getDao(delegator, x.ContactMech, UserLoginDao.class);
            contactMech = contactMechDao.findOne(delegator, x.ContactMech, UtilMisc.toMap(x.contactMechId, contactMechId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            contactMech = null;
        }

        if (!x.NA.equals(partyId)) {
            // try to find a PartyContactMech with a valid date range
            try {
                UserLoginDao partyContactMechDao = DaoRegistry.getDao(delegator, x.PartyContactMech, UserLoginDao.class);
                List<GenericValue> partyContactMechs = partyContactMechDao.findByAnd(delegator, x.PartyContactMech,
                        UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId), UtilMisc.toList(x.fromDate), false);
                partyContactMech = EntityUtil.getFirst(EntityUtil.filterByDate(partyContactMechs, true));
                if (partyContactMech == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.contactmechservices_cannot_update_specified_contact_info_not_corresponds, locale));
                }
                toBeStored.add(partyContactMech);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
                contactMech = null;
            }
        }
        if (contactMech == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_find_specified_contact_info_read, locale));
        }

        // never change a contact mech, just create a new one with the changes
        GenericValue newContactMech = GenericValue.create(contactMech);
        GenericValue newPartyContactMech = null;
        if (partyContactMech != null) {
            newPartyContactMech = GenericValue.create(partyContactMech);
        }
        GenericValue relatedEntityToSet = null;

        if (x.POSTAL_ADDRESS.equals(contactMech.getString(x.contactMechTypeId))) {
            GenericValue addr;
            try {
                UserLoginDao postalAddressDao = DaoRegistry.getDao(delegator, x.PostalAddress, UserLoginDao.class);
                addr = postalAddressDao.findOne(delegator, x.PostalAddress, UtilMisc.toMap(x.contactMechId, contactMechId), false);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), MODULE);
                addr = null;
            }
            relatedEntityToSet = GenericValue.create(addr);
            relatedEntityToSet.set(x.toName, context.get(x.toName));
            relatedEntityToSet.set(x.attnName, context.get(x.attnName));
            relatedEntityToSet.set(x.address1, context.get(x.address1));
            relatedEntityToSet.set(x.address2, context.get(x.address2));
            relatedEntityToSet.set(x.directions, context.get(x.directions));
            relatedEntityToSet.set(x.city, context.get(x.city));
            relatedEntityToSet.set(x.postalCode, context.get(x.postalCode));
            relatedEntityToSet.set(x.postalCodeExt, context.get(x.postalCodeExt));
            relatedEntityToSet.set(x.stateProvinceGeoId, context.get(x.stateProvinceGeoId));
            relatedEntityToSet.set(x.countryGeoId, context.get(x.countryGeoId));
            relatedEntityToSet.set(x.postalCodeGeoId, context.get(x.postalCodeGeoId));
            if (addr == null || !relatedEntityToSet.equals(addr)) {
                isModified = true;
            }
            relatedEntityToSet.set(x.contactMechId, newCmId);
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_update_contact_as_POSTAL_ADDRESS_specified,
                    UtilMisc.toMap(x.contactMechTypeId, contactMech.getString(x.contactMechTypeId)), locale));
        }

        if (newPartyContactMech != null) {
            newPartyContactMech.set(x.roleTypeId, context.get(x.roleTypeId));
            newPartyContactMech.set(x.allowSolicitation, context.get(x.allowSolicitation));
        }

        if (!newContactMech.equals(contactMech)) {
            isModified = true;
        }
        if (newPartyContactMech != null && !newPartyContactMech.equals(partyContactMech)) {
            isModified = true;
        }

        toBeStored.add(newContactMech);
        if (newPartyContactMech != null) {
            toBeStored.add(newPartyContactMech);
        }

        if (isModified) {
            toBeStored.add(relatedEntityToSet);

            newContactMech.set(x.contactMechId, newCmId);
            if (newPartyContactMech != null) {
                newPartyContactMech.set(x.contactMechId, newCmId);
                newPartyContactMech.set(x.fromDate, now);
                newPartyContactMech.set(x.thruDate, null);

                try {
                    Iterator<GenericValue> partyContactMechPurposes = UtilMisc.toIterator(partyContactMech.getRelated(x.PartyContactMechPurpose,
                            null, null, false));

                    while (partyContactMechPurposes != null && partyContactMechPurposes.hasNext()) {
                        GenericValue tempVal = GenericValue.create(partyContactMechPurposes.next());

                        tempVal.set(x.contactMechId, newCmId);
                        toBeStored.add(tempVal);
                    }
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.toString(), MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.contactmechservices_could_not_change_contact_info_read,
                            UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
                }

                partyContactMech.set(x.thruDate, now);
            }

            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.contactmechservices_could_not_change_contact_info_write,
                        UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
            }
        } else {
            result.put(x.newContactMechId, contactMechId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_no_changes_made_not_updating, locale));
            return result;
        }

        result.put(x.newContactMechId, newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    // ============================================================================
    // ============================================================================

    /**
     * Creates a TelecomNumber
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createTelecomNumber(DispatchContext ctx, ContactMechServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PARTYMGR, x.PCM_CREATE);

        if (!result.isEmpty()) {
            return result;
        }

        String contactMechTypeId = x.TELECOM_NUMBER;

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId(x.ContactMech);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_create_contact_info_id_generation_failure, locale));
        }

        GenericValue tempContactMech = delegator.makeValue(x.ContactMech, UtilMisc.toMap(x.contactMechId, newCmId,
                x.contactMechTypeId, contactMechTypeId));
        toBeStored.add(tempContactMech);

        toBeStored.add(delegator.makeValue(x.PartyContactMech, UtilMisc.toMap(x.partyId, partyId, x.contactMechId, newCmId,
                    x.fromDate, now, x.roleTypeId, context.get(x.roleTypeId), x.allowSolicitation, context.get(x.allowSolicitation),
                x.extension, context.get(x.extension))));

        toBeStored.add(delegator.makeValue(x.TelecomNumber, UtilMisc.toMap(x.contactMechId, newCmId,
                    x.countryCode, context.get(x.countryCode), x.areaCode, context.get(x.areaCode), x.contactNumber, context.get(x.contactNumber))));

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_create_contact_info_write,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }

        result.put(x.contactMechId, newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates a TelecomNumber
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_UPDATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateTelecomNumber(DispatchContext ctx, ContactMechServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();
        boolean isModified = false;

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PARTYMGR, x.PCM_UPDATE);

        if (!result.isEmpty()) {
            return result;
        }

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId(x.ContactMech);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_change_contact_info_id_generation_failure, locale));
        }

        String contactMechId = (String) context.get(x.contactMechId);
        GenericValue contactMech = null;
        GenericValue partyContactMech = null;

        try {
            UserLoginDao contactMechDao = DaoRegistry.getDao(delegator, x.ContactMech, UserLoginDao.class);
            contactMech = contactMechDao.findOne(delegator, x.ContactMech, UtilMisc.toMap(x.contactMechId, contactMechId), false);
            // try to find a PartyContactMech with a valid date range
            UserLoginDao partyContactMechDao = DaoRegistry.getDao(delegator, x.PartyContactMech, UserLoginDao.class);
            List<GenericValue> partyContactMechs = partyContactMechDao.findByAnd(delegator, x.PartyContactMech,
                    UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId), UtilMisc.toList(x.fromDate), false);
            partyContactMech = EntityUtil.getFirst(EntityUtil.filterByDate(partyContactMechs, true));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            contactMech = null;
        }
        if (contactMech == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_find_specified_contact_info_read, locale));
        }
        if (partyContactMech == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_cannot_update_specified_contact_info_not_corresponds, locale));
        }
        toBeStored.add(partyContactMech);

        // never change a contact mech, just create a new one with the changes
        GenericValue newContactMech = GenericValue.create(contactMech);
        GenericValue newPartyContactMech = GenericValue.create(partyContactMech);
        GenericValue relatedEntityToSet = null;

        if (x.TELECOM_NUMBER.equals(contactMech.getString(x.contactMechTypeId))) {
            GenericValue telNum;
            try {
                UserLoginDao telecomNumberDao = DaoRegistry.getDao(delegator, x.TelecomNumber, UserLoginDao.class);
                telNum = telecomNumberDao.findOne(delegator, x.TelecomNumber, UtilMisc.toMap(x.contactMechId, contactMechId), false);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), MODULE);
                telNum = null;
            }
            relatedEntityToSet = GenericValue.create(telNum);
            relatedEntityToSet.set(x.countryCode, context.get(x.countryCode));
            relatedEntityToSet.set(x.areaCode, context.get(x.areaCode));
            relatedEntityToSet.set(x.contactNumber, context.get(x.contactNumber));

            if (telNum == null || !relatedEntityToSet.equals(telNum)) {
                isModified = true;
            }
            relatedEntityToSet.set(x.contactMechId, newCmId);
            newPartyContactMech.set(x.extension, context.get(x.extension));
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_update_contact_as_TELECOM_NUMBER_specified,
                    UtilMisc.toMap(x.contactMechTypeId, contactMech.getString(x.contactMechTypeId)), locale));
        }

        newPartyContactMech.set(x.roleTypeId, context.get(x.roleTypeId));
        newPartyContactMech.set(x.allowSolicitation, context.get(x.allowSolicitation));

        if (!newContactMech.equals(contactMech)) {
            isModified = true;
        }
        if (!newPartyContactMech.equals(partyContactMech)) {
            isModified = true;
        }

        toBeStored.add(newContactMech);
        toBeStored.add(newPartyContactMech);

        if (isModified) {
            toBeStored.add(relatedEntityToSet);

            newContactMech.set(x.contactMechId, newCmId);
            newPartyContactMech.set(x.contactMechId, newCmId);
            newPartyContactMech.set(x.fromDate, now);
            newPartyContactMech.set(x.thruDate, null);

            try {
                Iterator<GenericValue> partyContactMechPurposes = UtilMisc.toIterator(partyContactMech.getRelated(x.PartyContactMechPurpose,
                        null, null, false));

                while (partyContactMechPurposes != null && partyContactMechPurposes.hasNext()) {
                    GenericValue tempVal = GenericValue.create(partyContactMechPurposes.next());

                    tempVal.set(x.contactMechId, newCmId);
                    toBeStored.add(tempVal);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.contactmechservices_could_not_change_contact_info_read,
                        UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
            }

            partyContactMech.set(x.thruDate, now);
            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.contactmechservices_could_not_change_contact_info_write,
                        UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
            }
        } else {
            result.put(x.newContactMechId, contactMechId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_no_changes_made_not_updating, locale));
            return result;
        }

        result.put(x.newContactMechId, newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    // ============================================================================
    // ============================================================================

    /**
     * Creates a EmailAddress
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createEmailAddress(DispatchContext ctx, ContactMechServicesContext context) {
        ContactMechServicesContext newContext = new ContactMechServicesContext(UtilMisc.makeMapWritable(context));

        newContext.put(x.infoString, newContext.get(x.emailAddress));
        newContext.remove(x.emailAddress);
        newContext.put(x.contactMechTypeId, x.EMAIL_ADDRESS);

        return createContactMech(ctx, newContext);
    }

    /**
     * Updates a EmailAddress
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_UPDATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateEmailAddress(DispatchContext ctx, ContactMechServicesContext context) {
        ContactMechServicesContext newContext = new ContactMechServicesContext(UtilMisc.makeMapWritable(context));

        newContext.put(x.infoString, newContext.get(x.emailAddress));
        newContext.remove(x.emailAddress);
        return updateContactMech(ctx, newContext);
    }

    // ============================================================================
    // ============================================================================

    /**
     * Creates a PartyContactMechPurpose
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createPartyContactMechPurpose(DispatchContext ctx, ContactMechServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, x.PARTYMGR, x.PCM_CREATE);
        String errMsg = null;
        Locale locale = (Locale) context.get(x.locale);

        if (!result.isEmpty()) {
            return result;
        }

        // required parameters
        String contactMechId = (String) context.get(x.contactMechId);
        String contactMechPurposeTypeId = (String) context.get(x.contactMechPurposeTypeId);
        Timestamp fromDate = (Timestamp) context.get(x.fromDate);

        GenericValue tempVal;
        try {
            UserLoginDao partyContactWithPurposeDao = DaoRegistry.getDao(delegator, x.PartyContactWithPurpose, UserLoginDao.class);
            List<GenericValue> partyContactWithPurposes = partyContactWithPurposeDao.findByAnd(delegator, x.PartyContactWithPurpose,
                    UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId, x.contactMechPurposeTypeId, contactMechPurposeTypeId),
                    null, false);
            Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
            partyContactWithPurposes = EntityUtil.filterByDate(partyContactWithPurposes, nowTimestamp, x.contactFromDate, x.contactThruDate,
                    true);
            partyContactWithPurposes = EntityUtil.filterByDate(partyContactWithPurposes, nowTimestamp, x.purposeFromDate, x.purposeThruDate,
                    true);
            tempVal = EntityUtil.getFirst(partyContactWithPurposes);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            tempVal = null;
        }

        if (UtilValidate.isEmpty(fromDate)) {
            fromDate = UtilDateTime.nowTimestamp();
        }

        if (tempVal != null) {
            // exists already with valid date, show warning
            errMsg = UtilProperties.getMessage(RES_ERROR,
                       x.contactmechservices_could_not_create_new_purpose_already_exists, locale);
            errMsg += x.str_ceca32e9 + tempVal.getPrimaryKey().toString();
            return ServiceUtil.returnError(errMsg);
        }
        // no entry with a valid date range exists, create new with open thruDate
        GenericValue newPartyContactMechPurpose = delegator.makeValue(x.PartyContactMechPurpose,
                UtilMisc.toMap(x.partyId, partyId, x.contactMechId, contactMechId, x.contactMechPurposeTypeId, contactMechPurposeTypeId,
                    x.fromDate, fromDate));

        try {
            delegator.create(newPartyContactMechPurpose);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.contactmechservices_could_not_add_purpose_write,
                    UtilMisc.toMap(x.errMessage, e.getMessage()), locale));
        }

        result.put(x.fromDate, fromDate);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Just wraps the ContactMechWorker method of the same name.
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> getPartyContactMechValueMaps(DispatchContext ctx, ContactMechServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String partyId = (String) context.get(x.partyId);
        Locale locale = (Locale) context.get(x.locale);
        if (UtilValidate.isEmpty(partyId)) {
            if (userLogin != null) {
                partyId = userLogin.getString(x.partyId);
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.PartyCannotGetPartyContactMech, locale));
            }
        }
        Boolean bShowOld = (Boolean) context.get(x.showOld);
        boolean showOld = Boolean.TRUE.equals(bShowOld);
        String contactMechTypeId = (String) context.get(x.contactMechTypeId);
        List<Map<String, Object>> valueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, partyId, showOld, contactMechTypeId);
        result.put(x.valueMaps, valueMaps);
        return result;
    }

    /**
     * Copies all contact mechs from one party to another. Does not delete or overwrite any contact mechs.
     */
    public static Map<String, Object> copyPartyContactMechs(DispatchContext dctx, ContactMechServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String partyIdFrom = (String) context.get(x.partyIdFrom);
        String partyIdTo = (String) context.get(x.partyIdTo);
        Locale locale = (Locale) context.get(x.locale);

        try {
            // grab all of the non-expired contact mechs using this party worker method
            List<Map<String, Object>> valueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, partyIdFrom, false);

            // loop through results
            for (Map<String, Object> thisMap: valueMaps) {
                GenericValue contactMech = (GenericValue) thisMap.get(x.contactMech);
                GenericValue partyContactMech = (GenericValue) thisMap.get(x.partyContactMech);
                List<GenericValue> partyContactMechPurposes = UtilGenerics.cast(thisMap.get(x.partyContactMechPurposes));

                // get the contactMechId
                String contactMechId = contactMech.getString(x.contactMechId);

                // create a new party contact mech for the partyIdTo
                Map<String, Object> serviceResults = dispatcher.runSync(x.createPartyContactMech, UtilMisc.<String, Object>toMap(x.partyId,
                        partyIdTo, x.userLogin, userLogin, x.contactMechId, contactMechId, x.contactMechTypeId,
                        contactMech.getString(x.contactMechTypeId), x.fromDate, UtilDateTime.nowTimestamp(), x.allowSolicitation,
                        partyContactMech.getString(x.allowSolicitation), x.extension, partyContactMech.getString(x.extension)));
                if (ServiceUtil.isError(serviceResults)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                }

                // loop through purposes and copy each as a new purpose for the partyIdTo
                for (GenericValue purpose: partyContactMechPurposes) {
                    Map<String, Object> input = UtilMisc.toMap(x.partyId, partyIdTo, x.contactMechId, contactMechId, x.userLogin, userLogin);
                    input.put(x.contactMechPurposeTypeId, purpose.getString(x.contactMechPurposeTypeId));
                    serviceResults = dispatcher.runSync(x.createPartyContactMechPurpose, input);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                    }
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.PartyCannotCopyPartyContactMech,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates an EmailAddressVerification
     */
    public static Map<String, Object> createEmailAddressVerification(DispatchContext dctx, ContactMechServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String emailAddress = (String) context.get(x.emailAddress);
        String verifyHash = null;

        String expireTime = EntityUtilProperties.getPropertyValue(x.security, x.email_verification_expire_hours, delegator);
        Integer expTime = Integer.valueOf(expireTime);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expTime);
        Date date = calendar.getTime();
        Timestamp expireDate = UtilDateTime.toTimestamp(date);

        synchronized (ContactMechServices.class) {
            while (true) {
                Long random = SECURE_RANDOM.nextLong();
                verifyHash = HashCrypt.digestHash(x.MD5, Long.toString(random).getBytes(StandardCharsets.UTF_8));
                List<GenericValue> emailAddVerifications = null;
                try {
                    UserLoginDao emailAddressVerificationDao = DaoRegistry.getDao(delegator, x.EmailAddressVerification, UserLoginDao.class);
                    emailAddVerifications = emailAddressVerificationDao.findByAnd(delegator, x.EmailAddressVerification,
                            UtilMisc.toMap(x.verifyHash, verifyHash), null, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e.getMessage(), MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
                if (UtilValidate.isEmpty(emailAddVerifications)) {
                    GenericValue emailAddressVerification = delegator.makeValue(x.EmailAddressVerification);
                    emailAddressVerification.set(x.emailAddress, emailAddress);
                    emailAddressVerification.set(x.verifyHash, verifyHash);
                    emailAddressVerification.set(x.expireDate, expireDate);
                    try {
                        delegator.create(emailAddressVerification);
                    } catch (GenericEntityException e) {
                        Debug.logError(e.getMessage(), MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                    break;
                }
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.verifyHash, verifyHash);
        return result;
    }

}
