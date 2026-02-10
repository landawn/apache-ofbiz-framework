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
package org.apache.ofbiz.common.preferences;

import static org.apache.ofbiz.base.util.UtilGenerics.checkMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserPreferenceDao;
import org.apache.ofbiz.persistence.entity.UserPreferenceEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;


import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PreferenceServicesContext;
/**
 * User preference services.<p>User preferences are stored as key-value pairs.
 * <p>User preferences can be grouped - so that multiple preference pairs can be
 * handled at once. Preference groups also allow a single userPrefTypeId to be
 * used more than once - with each occurence having a unique userPrefGroupTypeId.</p>
 * <p>User preference values are stored as Strings, so the easiest and most
 * efficient way to handle user preference values is to keep them as strings.
 * This class handles any data conversion needed.</p>
 */
public class PreferenceServices {
    private static final String MODULE = PreferenceServices.class.getName();

    private static final String RESOURCE = x.PrefErrorUiLabels;

    /**
     * Retrieves a single user preference from persistent storage. Call with
     * userPrefTypeId and optional userPrefLoginId. If userPrefLoginId isn't
     * specified, then the currently logged-in user's userLoginId will be
     * used. The retrieved preference is contained in the <b>userPrefMap</b> element.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getUserPreference(DispatchContext ctx, PreferenceServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        if (!PreferenceWorker.isValidGetId(ctx, context)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.getPreference_permissionError, locale));
        }
        Delegator delegator = ctx.getDelegator();

        String userPrefTypeId = (String) context.get(x.userPrefTypeId);
        if (UtilValidate.isEmpty(userPrefTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.getPreference_invalidArgument, locale));
        }
        String userLoginId = PreferenceWorker.getUserLoginId(context, true);
        Map<String, String> fieldMap = UtilMisc.toMap(x.userLoginId, userLoginId, x.userPrefTypeId, userPrefTypeId);
        String userPrefGroupTypeId = (String) context.get(x.userPrefGroupTypeId);
        if (UtilValidate.isNotEmpty(userPrefGroupTypeId)) {
            fieldMap.put(x.userPrefGroupTypeId, userPrefGroupTypeId);
        }

        Map<String, Object> userPrefMap = null;
        try {
            UserPreferenceDao userPreferenceDao = DaoRegistry.getDao(delegator, x.UserPreference, UserPreferenceDao.class);
            List<UserPreferenceEntity> preferenceEntities = userPreferenceDao.list(Filters.and(
                    Filters.eq(x.userLoginId, fieldMap.get(x.userLoginId)),
                    Filters.eq(x.userPrefTypeId, fieldMap.get(x.userPrefTypeId)),
                    UtilValidate.isNotEmpty(fieldMap.get(x.userPrefGroupTypeId))
                            ? Filters.eq(x.userPrefGroupTypeId, fieldMap.get(x.userPrefGroupTypeId))
                            : Filters.alwaysTrue()));
            GenericValue preference = preferenceEntities.isEmpty() ? null
                    : delegator.makeValue(x.UserPreference, Beans.beanToMap(preferenceEntities.get(0)));
            if (preference != null) {
                userPrefMap = PreferenceWorker.createUserPrefMap(preference);
            }
        } catch (Exception e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.getPreference_readFailure, new Object[] {e.getMessage() }, locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.userPrefMap, userPrefMap);
        if (userPrefMap != null) {
            // Put the value in the result Map too, makes access easier for calling methods.
            Object userPrefValue = userPrefMap.get(userPrefTypeId);
            if (userPrefValue != null) {
                result.put(x.userPrefValue, userPrefValue);
            }
        }
        return result;
    }

    /**
     * Retrieves a group of user preferences from persistent storage. Call with
     * userPrefGroupTypeId and optional userPrefLoginId. If userPrefLoginId isn't
     * specified, then the currently logged-in user's userLoginId will be
     * used. The retrieved preferences group is contained in the <b>userPrefMap</b> element.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getUserPreferenceGroup(DispatchContext ctx, PreferenceServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        if (!PreferenceWorker.isValidGetId(ctx, context)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.getPreference_permissionError, locale));
        }
        Delegator delegator = ctx.getDelegator();

        String userPrefGroupTypeId = (String) context.get(x.userPrefGroupTypeId);
        if (UtilValidate.isEmpty(userPrefGroupTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.getPreference_invalidArgument, locale));
        }
        String userLoginId = PreferenceWorker.getUserLoginId(context, false);

        Map<String, Object> userPrefMap = null;
        try {
            UserPreferenceDao userPreferenceDao = DaoRegistry.getDao(delegator, x.UserPreference, UserPreferenceDao.class);
            Map<String, String> fieldMap = UtilMisc.toMap(x.userLoginId, x.NA, x.userPrefGroupTypeId, userPrefGroupTypeId);
            List<UserPreferenceEntity> preferenceEntities = userPreferenceDao.list(Filters.and(
                    Filters.eq(x.userLoginId, fieldMap.get(x.userLoginId)),
                    Filters.eq(x.userPrefGroupTypeId, fieldMap.get(x.userPrefGroupTypeId))));
            List<GenericValue> preferences = new ArrayList<>();
            for (UserPreferenceEntity preferenceEntity : preferenceEntities) {
                preferences.add(delegator.makeValue(x.UserPreference, Beans.beanToMap(preferenceEntity)));
            }
            userPrefMap = PreferenceWorker.createUserPrefMap(preferences);
            if (userLoginId != null) {
                fieldMap.put(x.userLoginId, userLoginId);
                preferenceEntities = userPreferenceDao.list(Filters.and(
                        Filters.eq(x.userLoginId, fieldMap.get(x.userLoginId)),
                        Filters.eq(x.userPrefGroupTypeId, fieldMap.get(x.userPrefGroupTypeId))));
                preferences = new ArrayList<>();
                for (UserPreferenceEntity preferenceEntity : preferenceEntities) {
                    preferences.add(delegator.makeValue(x.UserPreference, Beans.beanToMap(preferenceEntity)));
                }
                userPrefMap.putAll(PreferenceWorker.createUserPrefMap(preferences));
            }
        } catch (Exception e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.getPreference_readFailure, new Object[] {e.getMessage() }, locale));
        }
        // for the 'DEFAULT' values find the related values in general properties and if found use those.
        Properties generalProperties = UtilProperties.getProperties(x.general);
        for (Map.Entry<String, Object> pairs: userPrefMap.entrySet()) {
            if (x.DEFAULT.equals(pairs.getValue())) {
                if (UtilValidate.isNotEmpty(generalProperties.get(pairs.getKey()))) {
                    userPrefMap.put(pairs.getKey(), generalProperties.get(pairs.getKey()));
                }
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.userPrefMap, userPrefMap);
        return result;
    }

    /**
     * Stores a single user preference in persistent storage. Call with
     * userPrefTypeId, userPrefGroupTypeId, userPrefValue and optional userPrefLoginId.
     * If userPrefLoginId isn't specified, then the currently logged-in user's
     * userLoginId will be used.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> setUserPreference(DispatchContext ctx, PreferenceServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        String userLoginId = PreferenceWorker.getUserLoginId(context, false);
        String userPrefTypeId = (String) context.get(x.userPrefTypeId);
        Object userPrefValue = context.get(x.userPrefValue);
        if (UtilValidate.isEmpty(userLoginId) || UtilValidate.isEmpty(userPrefTypeId) || userPrefValue == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.setPreference_invalidArgument, locale));
        }
        String userPrefGroupTypeId = (String) context.get(x.userPrefGroupTypeId);
        String userPrefDataType = (String) context.get(x.userPrefDataType);

        try {
            if (UtilValidate.isNotEmpty(userPrefDataType)) {
                userPrefValue = ObjectType.simpleTypeOrObjectConvert(userPrefValue, userPrefDataType, null, null, false);
            }
            GenericValue rec = delegator.makeValidValue(x.UserPreference, PreferenceWorker.toFieldMap(userLoginId, userPrefTypeId,
                    userPrefGroupTypeId, userPrefValue));
            delegator.createOrStore(rec);
        } catch (GeneralException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.setPreference_writeFailure, new Object[] {e.getMessage() }, locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> removeUserPreference(DispatchContext ctx, PreferenceServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        String userLoginId = PreferenceWorker.getUserLoginId(context, false);
        String userPrefTypeId = (String) context.get(x.userPrefTypeId);
        if (UtilValidate.isEmpty(userLoginId) || UtilValidate.isEmpty(userPrefTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.setPreference_invalidArgument, locale));
        }

        try {
            UserPreferenceDao userPreferenceDao = DaoRegistry.getDao(delegator, x.UserPreference, UserPreferenceDao.class);
            UserPreferenceEntity userPreferenceEntity = UserPreferenceEntity.builder()
                    .userLoginId(userLoginId)
                    .userPrefTypeId(userPrefTypeId)
                    .build();
            GenericValue rec = userPreferenceDao.get(userPreferenceEntity)
                    .map(preferenceEntity -> delegator.makeValue(x.UserPreference, Beans.beanToMap(preferenceEntity)))
                    .orElse(null);
            if (rec != null) {
                rec.remove();
            }
        } catch (Exception e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.setPreference_writeFailure, new Object[] {e.getMessage() }, locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Stores a user preference group in persistent storage. Call with
     * userPrefMap, userPrefGroupTypeId and optional userPrefLoginId. If userPrefLoginId
     * isn't specified, then the currently logged-in user's userLoginId will be
     * used.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> setUserPreferenceGroup(DispatchContext ctx, PreferenceServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        String userLoginId = PreferenceWorker.getUserLoginId(context, false);
        Map<String, Object> userPrefMap = checkMap(context.get(x.userPrefMap), String.class, Object.class);
        String userPrefGroupTypeId = (String) context.get(x.userPrefGroupTypeId);
        if (UtilValidate.isEmpty(userLoginId) || UtilValidate.isEmpty(userPrefGroupTypeId) || userPrefMap == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.setPreference_invalidArgument, locale));
        }

        try {
            for (Map.Entry<String, Object> mapEntry: userPrefMap.entrySet()) {
                GenericValue rec = delegator.makeValidValue(x.UserPreference, PreferenceWorker.toFieldMap(userLoginId, mapEntry.getKey(),
                        userPrefGroupTypeId, mapEntry.getValue()));
                delegator.createOrStore(rec);
            }
        } catch (GeneralException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.setPreference_writeFailure, new Object[] {e.getMessage() }, locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Copies a user preference group. Call with
     * fromUserLoginId, userPrefGroupTypeId and optional userPrefLoginId. If userPrefLoginId
     * isn't specified, then the currently logged-in user's userLoginId will be
     * used.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> copyUserPreferenceGroup(DispatchContext ctx, PreferenceServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        String userLoginId = PreferenceWorker.getUserLoginId(context, false);
        String fromUserLoginId = (String) context.get(x.fromUserLoginId);
        String userPrefGroupTypeId = (String) context.get(x.userPrefGroupTypeId);
        if (UtilValidate.isEmpty(userLoginId) || UtilValidate.isEmpty(userPrefGroupTypeId) || UtilValidate.isEmpty(fromUserLoginId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.copyPreference_invalidArgument, locale));
        }

        try {
            UserPreferenceDao userPreferenceDao = DaoRegistry.getDao(delegator, x.UserPreference, UserPreferenceDao.class);
            List<UserPreferenceEntity> preferenceEntities = userPreferenceDao.list(Filters.and(
                    Filters.eq(x.userLoginId, fromUserLoginId),
                    Filters.eq(x.userPrefGroupTypeId, userPrefGroupTypeId)));
            List<GenericValue> resultList = new ArrayList<>();
            for (UserPreferenceEntity preferenceEntity : preferenceEntities) {
                resultList.add(delegator.makeValue(x.UserPreference, Beans.beanToMap(preferenceEntity)));
            }
            if (resultList != null) {
                for (GenericValue preference: resultList) {
                    preference.set(x.userLoginId, userLoginId);
                }
                delegator.storeAll(resultList);
            }
        } catch (Exception e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.copyPreference_writeFailure, new Object[] {e.getMessage() },
                    locale));
        }

        return ServiceUtil.returnSuccess();
    }
}
