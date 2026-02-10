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
package org.apache.ofbiz.content.content;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entityext.permission.EntityPermissionChecker;
import org.apache.ofbiz.persistence.dao.ContentDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.persistence.entity.ContentEntity;
import org.apache.ofbiz.persistence.entity.UserLoginEntity;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.util.Beans;



import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ContentPermissionServicesContext;
/**
 * ContentPermissionServices Class
 *
 * Services for granting operation permissions on Content entities in a data-driven manner.
 */
public class ContentPermissionServices {

    private static final String MODULE = ContentPermissionServices.class.getName();
    private static final String RESOURCE = x.ContentUiLabels;

    public ContentPermissionServices() { }

    /**
     * checkContentPermission
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     * This service goes thru a series of test to determine if the user has
     * authority to performed anyone of the passed in target operations.
     * It expects a Content entity in "currentContent"
     * It expects a list of contentOperationIds in "targetOperationList" rather
     * than a scalar because it is thought that sometimes more than one operation
     * would fit the situation.
     * Similarly, it expects a list of contentPurposeTypeIds in "contentPurposeList".
     * Again, normally there will just be one, but it is possible that a Content
     * entity could have multiple purposes associated with it.
     * The userLogin GenericValue is also required.
     * A list of roleTypeIds is also possible.
     * The basic sequence of testing events is:
     * First the ContentPurposeOperation table is checked to see if there are any
     * entries with matching purposes (and operations) with no roleTypeId (ie. _NA_).
     * This is done because it would be the most common scenario and is quick to check.
     * Secondly, the CONTENTMGR permission is checked.
     * Thirdly, the ContentPurposeOperation table is rechecked to see if there are
     * any conditions with roleTypeIds that match associated ContentRoles tied to the
     * user.
     * If a Party of "PARTY_GROUP" type is found, the PartyRelationship table is checked
     * to see if the current user is linked to that group.
     * If no match is found to this point and the current Content entity has a value for
     * ownerContentId, then the last step is recusively applied, using the ContentRoles
     * associated with the ownerContent entity.
     */
    public static Map<String, Object> checkContentPermission(DispatchContext dctx, ContentPermissionServicesContext context) {
        Debug.logWarning(new Exception(), x.This_service_has_been_depricated_in_favor_of_genericContentPermission, MODULE);

        Security security = dctx.getSecurity();
        Delegator delegator = dctx.getDelegator();
        //TODO this parameters is still not used but this service need to be replaced by genericContentPermission
        // String statusId = (String) context.get("statusId");
        //TODO this parameters is still not used but this service need to be replaced by genericContentPermission
        // String privilegeEnumId = (String) context.get("privilegeEnumId");
        GenericValue content = (GenericValue) context.get(x.currentContent);
        Boolean bDisplayFailCond = (Boolean) context.get(x.displayFailCond);
        boolean displayFailCond = false;
        if (bDisplayFailCond != null && bDisplayFailCond) {
            displayFailCond = true;
        }
        Debug.logInfo(x.displayFailCond_0 + displayFailCond, x.emptyString);
        Boolean bDisplayPassCond = (Boolean) context.get(x.displayPassCond);
        boolean displayPassCond = false;
        if (bDisplayPassCond != null && bDisplayPassCond) {
            displayPassCond = true;
        }
        Debug.logInfo(x.displayPassCond_0 + displayPassCond, x.emptyString);
        Map<String, Object> results = new HashMap<>();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String partyId = (String) context.get(x.partyId);
        if (UtilValidate.isEmpty(partyId)) {
            String passedUserLoginId = (String) context.get(x.userLoginId);
            if (UtilValidate.isNotEmpty(passedUserLoginId)) {
                try {
                    UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
                    UserLoginEntity userLoginEntity = userLoginDao.get(passedUserLoginId).orElse(null);
                    if (userLoginEntity != null) {
                        userLogin = delegator.makeValue(x.UserLogin, Beans.beanToMap(userLoginEntity));
                    }
                    if (userLogin != null) {
                        partyId = userLogin.getString(x.partyId);
                    }
                } catch (Exception e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }
        if (UtilValidate.isEmpty(partyId) && userLogin != null) {
            partyId = userLogin.getString(x.partyId);
        }


        // Do entity permission check. This will pass users with administrative permissions.
        boolean passed = false;
        // I realized, belatedly, that I wanted to be able to pass parameters in as
        // strings so this service could be used in an action event directly,
        // so I had to write this code to handle both list and strings
        List<String> passedPurposes = UtilGenerics.cast(context.get(x.contentPurposeList));
        String contentPurposeString = (String) context.get(x.contentPurposeString);
        if (UtilValidate.isNotEmpty(contentPurposeString)) {
            List<String> purposesFromString = StringUtil.split(contentPurposeString, x.str_3eb41622);
            if (passedPurposes == null) {
                passedPurposes = new LinkedList<>();
            }
            passedPurposes.addAll(purposesFromString);
        }

        EntityPermissionChecker.StdAuxiliaryValueGetter auxGetter = new EntityPermissionChecker
                .StdAuxiliaryValueGetter(x.ContentPurpose, x.contentPurposeTypeId, x.contentId);
        // Sometimes permissions need to be checked before an entity is created, so
        // there needs to be a method for setting a purpose list
        auxGetter.setList(passedPurposes);
        List<String> targetOperations = UtilGenerics.cast(context.get(x.targetOperationList));
        String targetOperationString = (String) context.get(x.targetOperationString);
        if (UtilValidate.isNotEmpty(targetOperationString)) {
            List<String> operationsFromString = StringUtil.split(targetOperationString, x.str_3eb41622);
            if (targetOperations == null) {
                targetOperations = new LinkedList<>();
            }
            targetOperations.addAll(operationsFromString);
        }
        EntityPermissionChecker.StdPermissionConditionGetter permCondGetter = new EntityPermissionChecker
                .StdPermissionConditionGetter(x.ContentPurposeOperation, x.contentOperationId, x.roleTypeId,
                x.statusId, x.contentPurposeTypeId, x.privilegeEnumId);
        permCondGetter.setOperationList(targetOperations);

        EntityPermissionChecker.StdRelatedRoleGetter roleGetter = new EntityPermissionChecker.StdRelatedRoleGetter(x.Content,
                x.roleTypeId, x.contentId, x.partyId, x.ownerContentId, x.ContentRole);
        List<String> passedRoles = UtilGenerics.cast(context.get(x.roleTypeList));
        if (passedRoles == null) passedRoles = new LinkedList<>();
        String roleTypeString = (String) context.get(x.roleTypeString);
        if (UtilValidate.isNotEmpty(roleTypeString)) {
            List<String> rolesFromString = StringUtil.split(roleTypeString, x.str_3eb41622);
            passedRoles.addAll(rolesFromString);
        }
        roleGetter.setList(passedRoles);

        String entityAction = (String) context.get(x.entityOperation);
        if (entityAction == null) entityAction = x.ADMIN;
        if (userLogin != null) {
            passed = security.hasEntityPermission(x.CONTENTMGR, entityAction, userLogin);
        }

        StringBuilder errBuf = new StringBuilder();
        String permissionStatus = null;
        List<Object> entityIds = new LinkedList<>();
        if (passed) {
            results.put(x.permissionStatus, x.granted);
            permissionStatus = x.granted;
            if (displayPassCond) {
                errBuf.append(x.hasEntityPermission + entityAction + x.PASSED);
            }

        } else {
            if (displayFailCond) {
                errBuf.append(x.hasEntityPermission + entityAction + x.FAILED_4e2a5857);
            }

            if (content != null) {
                entityIds.add(content);
            }
            String quickCheckContentId = (String) context.get(x.quickCheckContentId);
            if (UtilValidate.isNotEmpty(quickCheckContentId)) {
                List<String> quickList = StringUtil.split(quickCheckContentId, x.str_3eb41622);
                if (UtilValidate.isNotEmpty(quickList)) {
                    entityIds.addAll(quickList);
                }
            }
            try {
                boolean check = EntityPermissionChecker.checkPermissionMethod(delegator, partyId, x.Content,
                        entityIds, auxGetter, roleGetter, permCondGetter);
                if (check) {
                    results.put(x.permissionStatus, x.granted);
                } else {
                    results.put(x.permissionStatus, x.rejected);
                }
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
            permissionStatus = (String) results.get(x.permissionStatus);
            errBuf.append(x.permissionStatus_5c8db8e8);
            errBuf.append(permissionStatus);
        }

        if ((x.granted.equals(permissionStatus) && displayPassCond)
                || (x.rejected.equals(permissionStatus) && displayFailCond)) {
            // Don't show this if passed on 'hasEntityPermission'
            if (displayFailCond || displayPassCond) {
                if (!passed) {
                    errBuf.append(x.targetOperations);
                    errBuf.append(targetOperations);

                    String errMsg = permCondGetter.dumpAsText();
                    errBuf.append(x.str_adc83b19);
                    errBuf.append(errMsg);
                    errBuf.append(x.partyId_ccf7b667);
                    errBuf.append(partyId);
                    errBuf.append(x.entityIds);
                    errBuf.append(entityIds);

                    errBuf.append(x.auxList);
                    errBuf.append(auxGetter.getList());

                    errBuf.append(x.roleList);
                    errBuf.append(roleGetter.getList());
                }

            }
        }
        Debug.logInfo(x.displayPass_FailCond_0_errBuf + errBuf.toString(), x.emptyString);
        results.put(ModelService.ERROR_MESSAGE, errBuf.toString());
        return results;
    }

    public static Map<String, Object> checkAssocPermission(DispatchContext dctx, ContentPermissionServicesContext context) {
        Map<String, Object> results = new HashMap<>();
        // Security security = dctx.getSecurity();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Boolean bDisplayFailCond = (Boolean) context.get(x.displayFailCond);
        String contentIdFrom = (String) context.get(x.contentIdFrom);
        String contentIdTo = (String) context.get(x.contentIdTo);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String entityAction = (String) context.get(x.entityOperation);
        Locale locale = (Locale) context.get(x.locale);
        if (entityAction == null) entityAction = x.ADMIN;
        String permissionStatus = null;

        GenericValue contentTo = null;
        GenericValue contentFrom = null;
        try {
            ContentDao contentDao = DaoRegistry.getDao(delegator, x.Content, ContentDao.class);
            ContentEntity contentToEntity = contentDao.get(contentIdTo).orElse(null);
            ContentEntity contentFromEntity = contentDao.get(contentIdFrom).orElse(null);
            if (contentToEntity != null) {
                contentTo = delegator.makeValue(x.Content, Beans.beanToMap(contentToEntity));
            }
            if (contentFromEntity != null) {
                contentFrom = delegator.makeValue(x.Content, Beans.beanToMap(contentFromEntity));
            }
        } catch (Exception e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ContentContentToOrFromErrorRetriving, locale));
        }
        if (contentTo == null || contentFrom == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ContentContentToOrFromIsNull,
                    UtilMisc.toMap(x.contentTo, contentTo, x.contentFrom, contentFrom), locale));
        }
        Map<String, Object> permResults = new HashMap<>();

        // Use the purposes from the from entity for both cases.
        List<String> relatedPurposes = EntityPermissionChecker.getRelatedPurposes(contentFrom, null);
        List<String> relatedPurposesTo = EntityPermissionChecker.getRelatedPurposes(contentTo, relatedPurposes);
        Map<String, Object> serviceInMap = new HashMap<>();
        serviceInMap.put(x.userLogin, userLogin);
        serviceInMap.put(x.targetOperationList, UtilMisc.toList(x.CONTENT_LINK_TO));
        serviceInMap.put(x.contentPurposeList, relatedPurposesTo);
        serviceInMap.put(x.currentContent, contentTo);
        serviceInMap.put(x.displayFailCond, bDisplayFailCond);

        try {
            permResults = dispatcher.runSync(x.checkContentPermission, serviceInMap);
            if (ServiceUtil.isError(permResults)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(permResults));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_checking_permissions, x.ContentServices);
        }
        permissionStatus = (String) permResults.get(x.permissionStatus);
        if (permissionStatus == null || !x.granted.equals(permissionStatus)) {
            if (bDisplayFailCond != null && bDisplayFailCond) {
                String errMsg = (String) permResults.get(ModelService.ERROR_MESSAGE);
                results.put(ModelService.ERROR_MESSAGE, errMsg);
            }
            return results;
        }
        serviceInMap.put(x.currentContent, contentFrom);
        serviceInMap.put(x.targetOperationList, UtilMisc.toList(x.CONTENT_LINK_FROM));
        serviceInMap.put(x.contentPurposeList, relatedPurposes);
        try {
            permResults = dispatcher.runSync(x.checkContentPermission, serviceInMap);
            if (ServiceUtil.isError(permResults)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(permResults));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_checking_permissions, x.ContentServices);
        }
        permissionStatus = (String) permResults.get(x.permissionStatus);
        if (permissionStatus != null && x.granted.equals(permissionStatus)) {
            results.put(x.permissionStatus, x.granted);
        } else {
            if (bDisplayFailCond != null && bDisplayFailCond) {
                String errMsg = (String) permResults.get(ModelService.ERROR_MESSAGE);
                results.put(ModelService.ERROR_MESSAGE, errMsg);
            }
        }
        return results;
    }

}
