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
package org.apache.ofbiz.content;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.imaging.ImageReadException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.security.SecuredUpload;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;

import org.apache.ofbiz.persistence.entity.x;
/**
 * ContentManagementServices Class
 */
public class ContentManagementServices {

    private static final String MODULE = ContentManagementServices.class.getName();
    private static final String RESOURCE = "ContentUiLabels";

    /**
     * getSubContent
     * Finds the related subContent given the template Content and the mapKey.
     * This service calls a same-named method in ContentWorker to do the work.
     */
    public static Map<String, Object> getSubContent(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String contentId = (String) context.get(x.contentId);
        String subContentId = (String) context.get(x.subContentId);
        String mapKey = (String) context.get(x.mapKey);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Timestamp fromDate = (Timestamp) context.get(x.fromDate);
        List<String> assocTypes = UtilGenerics.cast(context.get(x.assocTypes));
        String assocTypesString = (String) context.get(x.assocTypesString);
        if (UtilValidate.isNotEmpty(assocTypesString)) {
            List<String> lst = StringUtil.split(assocTypesString, "|");
            if (assocTypes == null) {
                assocTypes = new LinkedList<>();
            }
            assocTypes.addAll(lst);
        }
        GenericValue content = null;
        GenericValue view = null;

        try {
            view = ContentWorker.getSubContentCache(delegator, contentId, mapKey, subContentId, userLogin, assocTypes, fromDate,
                    Boolean.FALSE, null);
            content = ContentWorker.getContentFromView(view);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("view", view);
        results.put("content", content);
        return results;
    }

    /**
     * getContent
     * This service calls a same-named method in ContentWorker to do the work.
     */
    public static Map<String, Object> getContent(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String contentId = (String) context.get(x.contentId);
        GenericValue view = null;

        try {
            view = ContentWorker.getContentCache(delegator, contentId);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("view", view);
        return results;
    }

    /**
     * persistContentAndAssoc
     * A combination method that will create or update all or one of the following:
     * a Content entity, a ContentAssoc related to the Content, and
     * the ElectronicText that may be associated with the Content.
     * The keys for determining if each entity is created is the presence
     * of the contentTypeId, contentAssocTypeId and dataResourceTypeId.
     * This service tries to handle DataResource fields with and
     * without "dr" prefixes.
     * Assumes binary data is always in field, "imageData".
     * <p>
     * This service does not accept straight ContentAssoc parameters. They must be prefaced with "ca" + cap first letter
     */
    public static Map<String, Object> persistContentAndAssoc(DispatchContext dctx, Map<String, ? extends Object> rcontext)
            throws GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Locale locale = (Locale) context.get(x.locale);

        // Check if a webshell is not uploaded
        String textData = (String) context.get(x.textData);
        if (UtilValidate.isNotEmpty(textData)) {
            try {
                if (!SecuredUpload.isValidText(textData, Collections.emptyList())) {
                    Debug.logError("================== Not saved for security reason ==================", MODULE);
                    return ServiceUtil.returnError("================== Not saved for security reason ==================");
                }
            } catch (IOException e) {
                Debug.logError("================== Not saved for security reason ==================", MODULE);
                return ServiceUtil.returnError("================== Not saved for security reason ==================");
            }
        }

        // Knowing why a request fails permission check is one of the more difficult
        // aspects of content management. Setting "displayFailCond" to true will
        // put an html table in result.errorMessage that will show what tests were performed
        Boolean bDisplayFailCond = (Boolean) context.get(x.displayFailCond);
        String mapKey = (String) context.get(x.mapKey);

        // If "deactivateExisting" is set, other Contents that are tied to the same
        // contentIdTo will be deactivated (thruDate set to now)
        String deactivateString = (String) context.get(x.deactivateExisting);
        boolean deactivateExisting = "true".equalsIgnoreCase(deactivateString);

        if (Debug.infoOn()) {
            Debug.logInfo("in persist... mapKey(0):" + mapKey, MODULE);
        }

        // ContentPurposes can get passed in as a delimited string or a list. Combine.
        List<String> contentPurposeList = UtilGenerics.cast(context.get(x.contentPurposeList));
        if (contentPurposeList == null) {
            contentPurposeList = new LinkedList<>();
        }
        String contentPurposeString = (String) context.get(x.contentPurposeString);
        if (UtilValidate.isNotEmpty(contentPurposeString)) {
            List<String> tmpPurposes = StringUtil.split(contentPurposeString, "|");
            contentPurposeList.addAll(tmpPurposes);
        }
        context.put(x.contentPurposeList, contentPurposeList);
        context.put(x.contentPurposeString, null);

        if (Debug.infoOn()) {
            Debug.logInfo("in persist... contentPurposeList(0):" + contentPurposeList, MODULE);
            Debug.logInfo("in persist... textData(0):" + textData, MODULE);
        }

        GenericValue content = delegator.makeValue("Content");

        content.setPKFields(context);
        content.setNonPKFields(context);
        String contentId = (String) content.get(x.contentId);
        String contentTypeId = (String) content.get(x.contentTypeId);
        String origContentId = (String) content.get(x.contentId);
        String origDataResourceId = (String) content.get(x.dataResourceId);

        if (Debug.infoOn()) {
            Debug.logInfo("in persist... contentId(0):" + contentId, MODULE);
        }

        GenericValue dataResource = delegator.makeValue("DataResource");
        dataResource.setPKFields(context);
        dataResource.setNonPKFields(context);
        dataResource.setAllFields(context, false, "dr", null);
        String isPublic = (String) context.get(x.isPublic);
        if (UtilValidate.isEmpty(isPublic)) {
            dataResource.set(x.isPublic, "N");
        }
        context.putAll(dataResource);
        String dataResourceId = (String) dataResource.get(x.dataResourceId);
        String dataResourceTypeId = (String) dataResource.get(x.dataResourceTypeId);
        if (Debug.infoOn()) {
            Debug.logInfo("in persist... dataResourceId(0):" + dataResourceId, MODULE);
        }

        GenericValue contentAssoc = delegator.makeValue("ContentAssoc");
        String contentAssocTypeId = (String) context.get(x.contentAssocTypeId);
        if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
            context.put(x.caContentAssocTypeId, contentAssocTypeId);
        }
        contentAssocTypeId = (String) context.get(x.caContentAssocTypeId);
        contentAssoc.setAllFields(context, false, "ca", null);
        contentAssoc.put("contentId", context.get(x.caContentId));
        context.putAll(contentAssoc);

        GenericValue electronicText = delegator.makeValue("ElectronicText");
        electronicText.setPKFields(context);
        electronicText.setNonPKFields(context);

        // save expected primary keys on result now in case there is no operation that uses them
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("contentId", content.get(x.contentId));
        results.put("dataResourceId", dataResource.get(x.dataResourceId));
        results.put("drDataResourceId", dataResource.get(x.dataResourceId));
        results.put("drDataResourceId", dataResource.get(x.dataResourceId));
        results.put("caContentIdTo", contentAssoc.get(x.contentIdTo));
        results.put("caContentId", contentAssoc.get(x.contentId));
        results.put("caFromDate", contentAssoc.get(x.fromDate));
        results.put("caContentAssocTypeId", contentAssoc.get(x.contentAssocTypeId));

        // get user info for multiple use
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        boolean dataResourceExists = true;
        if (Debug.infoOn()) {
            Debug.logInfo("in persist... dataResourceTypeId(0):" + dataResourceTypeId, MODULE);
        }
        if (UtilValidate.isNotEmpty(dataResourceTypeId)) {
            Map<String, Object> dataResourceResult;
            try {
                dataResourceResult = persistDataResourceAndDataMethod(dctx, context);
            } catch (GenericEntityException | GenericServiceException e) {
                Debug.logError(e, e.toString(), MODULE);
                return ServiceUtil.returnError(e.toString());
            }
            String errorMsg = ServiceUtil.getErrorMessage(dataResourceResult);
            if (UtilValidate.isNotEmpty(errorMsg)) {
                return ServiceUtil.returnError(errorMsg);
            }
            dataResourceId = (String) dataResourceResult.get("dataResourceId");
            results.put("dataResourceId", dataResourceId);
            results.put("drDataResourceId", dataResourceId);
            context.put(x.dataResourceId, dataResourceId);
            content.put("dataResourceId", dataResourceId);
            context.put(x.drDataResourceId, dataResourceId);
        }
        // Do update and create permission checks on Content if warranted.

        context.put(x.skipPermissionCheck, null); // Force check here
        boolean contentExists = true;
        if (Debug.infoOn()) {
            Debug.logInfo("in persist... contentTypeId:" + contentTypeId + " dataResourceTypeId:" + dataResourceTypeId + " contentId:"
                    + contentId + " dataResourceId:" + dataResourceId, MODULE);
        }
        if (UtilValidate.isNotEmpty(contentTypeId)) {
            if (UtilValidate.isEmpty(contentId)) {
                contentExists = false;
            } else {
                try {
                    GenericValue val = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
                    if (val == null) {
                        dataResourceExists = false;
                    }
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.toString());
                }
            }
            context.putAll(content);
            if (contentExists) {
                Map<String, Object> contentContext = new HashMap<>();
                ModelService contentModel = dispatcher.getDispatchContext().getModelService("updateContent");
                contentContext.putAll(contentModel.makeValid(content, ModelService.IN_PARAM));
                contentContext.put("userLogin", userLogin);
                contentContext.put("displayFailCond", bDisplayFailCond);
                contentContext.put("skipPermissionCheck", context.get(x.skipPermissionCheck));
                Debug.logInfo("In persistContentAndAssoc calling updateContent with content: " + contentContext, MODULE);
                Map<String, Object> thisResult = dispatcher.runSync("updateContent", contentContext);
                if (ServiceUtil.isError(thisResult) || ServiceUtil.isFailure(thisResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentContentUpdatingError", UtilMisc.toMap("serviceName",
                            "persistContentAndAssoc"), locale), null, null, thisResult);
                }
            } else {
                Map<String, Object> contentContext = new HashMap<>();
                ModelService contentModel = dispatcher.getDispatchContext().getModelService("createContent");
                contentContext.putAll(contentModel.makeValid(content, ModelService.IN_PARAM));
                contentContext.put("userLogin", userLogin);
                contentContext.put("displayFailCond", bDisplayFailCond);
                contentContext.put("skipPermissionCheck", context.get(x.skipPermissionCheck));
                Debug.logInfo("In persistContentAndAssoc calling createContent with content: " + contentContext, MODULE);
                Map<String, Object> thisResult = dispatcher.runSync("createContent", contentContext);
                if (ServiceUtil.isError(thisResult) || ServiceUtil.isFailure(thisResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentContentCreatingError", UtilMisc.toMap("serviceName",
                            "persistContentAndAssoc"), locale), null, null, thisResult);
                }
                contentId = (String) thisResult.get("contentId");
            }
            results.put("contentId", contentId);
            context.put(x.contentId, contentId);
            context.put(x.caContentIdTo, contentId);

            // Add ContentPurposes if this is a create operation
            if (contentId != null && !contentExists) {
                try {
                    Set<String> contentPurposeSet = new LinkedHashSet<>(contentPurposeList);
                    for (String contentPurposeTypeId : contentPurposeSet) {
                        GenericValue contentPurpose = delegator.makeValue("ContentPurpose", UtilMisc.toMap("contentId", contentId,
                                "contentPurposeTypeId", contentPurposeTypeId));
                        contentPurpose.create();
                    }
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.toString());
                }
            }

        } else if (UtilValidate.isNotEmpty(dataResourceTypeId) && UtilValidate.isNotEmpty(contentId)) {
            // If dataResource was not previously existing, then update the associated content with its id
            if (UtilValidate.isNotEmpty(dataResourceId) && !dataResourceExists) {
                Map<String, Object> map = new HashMap<>();
                map.put("userLogin", userLogin);
                map.put("dataResourceId", dataResourceId);
                map.put("contentId", contentId);
                if (Debug.infoOn()) {
                    Debug.logInfo("in persist... context:" + context, MODULE);
                }
                Map<String, Object> r = dispatcher.runSync("updateContent", map);
                boolean isError = ModelService.RESPOND_ERROR.equals(r.get(ModelService.RESPONSE_MESSAGE));
                if (isError) {
                    return ServiceUtil.returnError((String) r.get(ModelService.ERROR_MESSAGE));
                }
            }
        }

        // Put contentId
        if (UtilValidate.isNotEmpty(contentId)) {
            contentAssoc.put("contentIdTo", contentId);
        }
        // If parentContentIdTo or parentContentIdFrom exists, create association with newly created content
        if (Debug.infoOn()) {
            Debug.logInfo("CREATING contentASSOC contentAssocTypeId:" + contentAssocTypeId, MODULE);
        }
        // create content assoc if the key values are present....
        if (Debug.infoOn()) {
            Debug.logInfo("contentAssoc: " + contentAssoc.toString(), MODULE);
        }
        if (UtilValidate.isNotEmpty(contentAssocTypeId) && contentAssoc.get(x.contentId) != null && contentAssoc.get(x.contentIdTo) != null) {
            if (Debug.infoOn()) {
                Debug.logInfo("in persistContentAndAssoc, deactivateExisting:" + deactivateExisting, MODULE);
            }
            Map<String, Object> contentAssocContext = new HashMap<>();
            contentAssocContext.put("userLogin", userLogin);
            contentAssocContext.put("displayFailCond", bDisplayFailCond);
            contentAssocContext.put("skipPermissionCheck", context.get(x.skipPermissionCheck));
            Map<String, Object> thisResult = null;
            try {
                GenericValue contentAssocExisting = EntityQuery.use(delegator).from("ContentAssoc").where(contentAssoc.getPrimaryKey()).queryOne();
                if (contentAssocExisting == null) {
                    ModelService contentAssocModel = dispatcher.getDispatchContext().getModelService("createContentAssoc");
                    Map<String, Object> ctx = contentAssocModel.makeValid(contentAssoc, ModelService.IN_PARAM);
                    contentAssocContext.putAll(ctx);
                    thisResult = dispatcher.runSync("createContentAssoc", contentAssocContext);
                    if (ServiceUtil.isError(thisResult) || ServiceUtil.isFailure(thisResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                    }

                    results.put("caContentIdTo", thisResult.get("contentIdTo"));
                    results.put("caContentId", thisResult.get("contentIdFrom"));
                    results.put("caContentAssocTypeId", thisResult.get("contentAssocTypeId"));
                    results.put("caFromDate", thisResult.get("fromDate"));
                    results.put("caSequenceNum", thisResult.get("sequenceNum"));
                } else {
                    if (deactivateExisting) {
                        contentAssocExisting.put("thruDate", UtilDateTime.nowTimestamp());
                    } else if (UtilValidate.isNotEmpty(context.get(x.thruDate))) {
                        contentAssocExisting.put("thruDate", context.get(x.thruDate));
                    }
                    ModelService contentAssocModel = dispatcher.getDispatchContext().getModelService("updateContentAssoc");
                    Map<String, Object> ctx = contentAssocModel.makeValid(contentAssocExisting, ModelService.IN_PARAM);
                    contentAssocContext.putAll(ctx);
                    thisResult = dispatcher.runSync("updateContentAssoc", contentAssocContext);
                    if (ServiceUtil.isError(thisResult) || ServiceUtil.isFailure(thisResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                    }
                }
            } catch (GenericEntityException | GenericServiceException e) {
                throw new GenericServiceException(e.toString());
            }
            String errMsg = ServiceUtil.getErrorMessage(thisResult);
            if (UtilValidate.isNotEmpty(errMsg)) {
                return ServiceUtil.returnError(errMsg);
            }
        }
        context.remove("skipPermissionCheck");
        context.put(x.contentId, origContentId);
        context.put(x.dataResourceId, origDataResourceId);
        context.remove("dataResource");
        Debug.logInfo("results:" + results, MODULE);
        return results;
    }

    /**
     * Service for update publish sites with a ContentRole that will tie them to the passed in party.
     */
    public static Map<String, Object> updateSiteRoles(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Map<String, Object> results = new HashMap<>();
        String siteContentId = (String) context.get(x.contentId);
        String partyId = (String) context.get(x.partyId);

        if (UtilValidate.isEmpty(siteContentId) || UtilValidate.isEmpty(partyId)) {
            return results;
        }

        List<GenericValue> siteRoles = null;
        try {
            siteRoles = EntityQuery.use(delegator).from("RoleType").where("parentTypeId", "BLOG").cache().queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }

        for (GenericValue roleType : siteRoles) {
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.put("partyId", partyId);
            serviceContext.put("contentId", siteContentId);
            serviceContext.put("userLogin", userLogin);
            Debug.logInfo("updateSiteRoles, serviceContext(0):" + serviceContext, MODULE);
            String siteRole = (String) roleType.get(x.roleTypeId); // BLOG_EDITOR, BLOG_ADMIN, etc.
            String cappedSiteRole = ModelUtil.dbNameToVarName(siteRole);
            if (Debug.infoOn()) {
                Debug.logInfo("updateSiteRoles, cappediteRole(1):" + cappedSiteRole, MODULE);
            }
            String siteRoleVal = (String) context.get(cappedSiteRole);
            if (Debug.infoOn()) {
                Debug.logInfo("updateSiteRoles, siteRoleVal(1):" + siteRoleVal, MODULE);
                Debug.logInfo("updateSiteRoles, context(1):" + context, MODULE);
            }
            Object fromDate = context.get(cappedSiteRole + "FromDate");
            if (Debug.infoOn()) {
                Debug.logInfo("updateSiteRoles, fromDate(1):" + fromDate, MODULE);
            }
            serviceContext.put("roleTypeId", siteRole);
            if (siteRoleVal != null && "Y".equalsIgnoreCase(siteRoleVal)) {
                // for now, will assume that any error is due to duplicates - ignore
                if (fromDate == null) {
                    try {
                        Map<String, Object> newContext = new HashMap<>();
                        newContext.put("contentId", serviceContext.get("contentId"));
                        newContext.put("partyId", serviceContext.get("partyId"));
                        newContext.put("roleTypeId", serviceContext.get("roleTypeId"));
                        newContext.put("userLogin", userLogin);
                        Map<String, Object> permResults = dispatcher.runSync("deactivateAllContentRoles", newContext);
                        if (ServiceUtil.isError(permResults)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(permResults));
                        }
                        serviceContext.put("fromDate", UtilDateTime.nowTimestamp());
                        if (Debug.infoOn()) {
                            Debug.logInfo("updateSiteRoles, serviceContext(1):" + serviceContext, MODULE);
                        }
                        permResults = dispatcher.runSync("createContentRole", serviceContext);
                        if (ServiceUtil.isError(permResults)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(permResults));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, e.toString(), MODULE);
                        return ServiceUtil.returnError(e.toString());
                    }
                }
            } else {
                if (fromDate != null) {
                    // for now, will assume that any error is due to non-existence - ignore
                    try {
                        Debug.logInfo("updateSiteRoles, serviceContext(2):" + serviceContext, MODULE);
                        Map<String, Object> newContext = new HashMap<>();
                        newContext.put("contentId", serviceContext.get("contentId"));
                        newContext.put("partyId", serviceContext.get("partyId"));
                        newContext.put("roleTypeId", serviceContext.get("roleTypeId"));
                        newContext.put("userLogin", userLogin);
                        Map<String, Object> permResults = dispatcher.runSync("deactivateAllContentRoles", newContext);
                        if (ServiceUtil.isError(permResults)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(permResults));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, e.toString(), MODULE);
                        return ServiceUtil.returnError(e.toString());
                    }
                }
            }
        }
        return results;
    }

    public static Map<String, Object> persistDataResourceAndData(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result;
        try {
            ModelService checkPermModel = dispatcher.getDispatchContext().getModelService("checkContentPermission");
            Map<String, Object> ctx = checkPermModel.makeValid(context, ModelService.IN_PARAM);
            Map<String, Object> thisResult = dispatcher.runSync("checkContentPermission", ctx);
            if (ServiceUtil.isError(thisResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
            }
            String permissionStatus = (String) thisResult.get("permissionStatus");
            if (UtilValidate.isNotEmpty(permissionStatus) && "granted".equalsIgnoreCase(permissionStatus)) {
                result = persistDataResourceAndDataMethod(dctx, context);
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentContentNoAccessToUploadImage", locale));
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, e.toString(), MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        String errorMsg = ServiceUtil.getErrorMessage(result);
        if (UtilValidate.isNotEmpty(errorMsg)) {
            return ServiceUtil.returnError(errorMsg);
        }
        return result;
    }

    public static Map<String, Object> persistDataResourceAndDataMethod(DispatchContext dctx, Map<String, ? extends Object> rcontext)
            throws GenericServiceException, GenericEntityException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);

        String errorMessage = validateUploadedFile(dctx, context);
        if (errorMessage != null) {
            return ServiceUtil.returnError(errorMessage);
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> newDrContext = new HashMap<>();
        GenericValue dataResource = delegator.makeValue("DataResource");
        dataResource.setPKFields(context);
        dataResource.setNonPKFields(context);
        dataResource.setAllFields(context, false, "dr", null);
        context.putAll(dataResource);

        GenericValue electronicText = delegator.makeValue("ElectronicText");
        electronicText.setPKFields(context);
        electronicText.setNonPKFields(context);
        String textData = (String) electronicText.get(x.textData);


        String dataResourceId = (String) dataResource.get(x.dataResourceId);
        String dataResourceTypeId = (String) dataResource.get(x.dataResourceTypeId);
        if (Debug.infoOn()) {
            Debug.logInfo("in persist... dataResourceId(0):" + dataResourceId, MODULE);
        }
        context.put(x.skipPermissionCheck, "granted"); // TODO: a temp hack because I don't want to bother with DataResource permissions at this time.
        boolean dataResourceExists = true;
        if (UtilValidate.isEmpty(dataResourceId)) {
            dataResourceExists = false;
        } else {
            try {
                GenericValue val = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).queryOne();
                if (val == null) {
                    dataResourceExists = false;
                }
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.toString());
            }
        }
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        ModelService dataResourceModel = dispatcher.getDispatchContext().getModelService("updateDataResource");
        Map<String, Object> ctx = dataResourceModel.makeValid(dataResource, ModelService.IN_PARAM);
        newDrContext.putAll(ctx);
        newDrContext.put("userLogin", userLogin);
        newDrContext.put("skipPermissionCheck", context.get(x.skipPermissionCheck));
        ByteBuffer imageDataBytes = (ByteBuffer) context.get(x.imageData);
        String mimeTypeId = (String) newDrContext.get("mimeTypeId");
        if (imageDataBytes != null && (mimeTypeId == null || (mimeTypeId.indexOf("image") >= 0) || (mimeTypeId.indexOf("application") >= 0))) {
            mimeTypeId = (String) context.get(x._imageData_contentType);
            if ("IMAGE_OBJECT".equals(dataResourceTypeId)) {
                String fileName = (String) context.get(x._imageData_fileName);
                newDrContext.put("objectInfo", fileName);
            }
            newDrContext.put("mimeTypeId", mimeTypeId);
        }

        if (!dataResourceExists) { // Create
            Map<String, Object> thisResult = dispatcher.runSync("createDataResource", newDrContext);
            if (ServiceUtil.isError(thisResult)) {
                throw (new GenericServiceException(ServiceUtil.getErrorMessage(thisResult)));
            }
            dataResourceId = (String) thisResult.get("dataResourceId");
            if (Debug.infoOn()) {
                Debug.logInfo("in persist... dataResourceId(0):" + dataResourceId, MODULE);
            }
            dataResource = (GenericValue) thisResult.get("dataResource");
            Map<String, Object> fileContext = new HashMap<>();
            fileContext.put("userLogin", userLogin);
            if ("IMAGE_OBJECT".equals(dataResourceTypeId)) {
                if (imageDataBytes != null) {
                    fileContext.put("dataResourceId", dataResourceId);
                    fileContext.put("imageData", imageDataBytes);
                    thisResult = dispatcher.runSync("createImage", fileContext);
                    if (ServiceUtil.isError(thisResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                    }
                }
                // We don't want SHORT_TEXT and SURVEY to be caught by the last else if, hence the 2 empty else if
            } else if ("SHORT_TEXT".equals(dataResourceTypeId)) {
                // To avoid checkstyle issue, placing log statement.
                Debug.logInfo("dataResourceTypeId: " + dataResourceId + " found.", MODULE);
            } else if (dataResourceTypeId.startsWith("SURVEY")) {
                // To avoid checkstyle issue, placing log statement.
                Debug.logInfo("dataResourceTypeId: " + dataResourceId + " found.", MODULE);
            } else if (dataResourceTypeId.indexOf("_FILE") >= 0) {
                Map<String, Object> uploadImage = new HashMap<>();
                uploadImage.put("userLogin", userLogin);
                uploadImage.put("dataResourceId", dataResourceId);
                uploadImage.put("dataResourceTypeId", dataResourceTypeId);
                uploadImage.put("rootDir", context.get(x.objectInfo));
                uploadImage.put("uploadedFile", imageDataBytes);
                uploadImage.put("_uploadedFile_fileName", context.get(x._imageData_fileName));
                uploadImage.put("_uploadedFile_contentType", context.get(x._imageData_contentType));
                thisResult = dispatcher.runSync("attachUploadToDataResource", uploadImage);
                if (ServiceUtil.isError(thisResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                }
            } else {
                // assume ELECTRONIC_TEXT
                if (UtilValidate.isNotEmpty(textData)) {
                    fileContext.put("dataResourceId", dataResourceId);
                    fileContext.put("textData", textData);
                    thisResult = dispatcher.runSync("createElectronicText", fileContext);
                    if (ServiceUtil.isError(thisResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                    }
                }
            }
        } else { // Update
            Map<String, Object> thisResult = dispatcher.runSync("updateDataResource", newDrContext);
            if (ServiceUtil.isError(thisResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
            }
            Map<String, Object> fileContext = new HashMap<>();
            fileContext.put("userLogin", userLogin);
            String forceElectronicText = (String) context.get(x.forceElectronicText);
            if ("IMAGE_OBJECT".equals(dataResourceTypeId)) {
                if (imageDataBytes != null || "true".equalsIgnoreCase(forceElectronicText)) {
                    fileContext.put("dataResourceId", dataResourceId);
                    fileContext.put("imageData", imageDataBytes);
                    thisResult = dispatcher.runSync("updateImage", fileContext);
                    if (ServiceUtil.isError(thisResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                    }
                }
                // We don't want SHORT_TEXT and SURVEY to be caught by the last else if, hence the 2 empty else if
            } else if ("SHORT_TEXT".equals(dataResourceTypeId)) {
                // To avoid checkstyle issue, placing log statement.
                Debug.logInfo("dataResourceTypeId: " + dataResourceId + " found.", MODULE);
            } else if (dataResourceTypeId.startsWith("SURVEY")) {
                // To avoid checkstyle issue, placing log statement.
                Debug.logInfo("dataResourceTypeId: " + dataResourceId + " found.", MODULE);
            } else if (dataResourceTypeId.indexOf("_FILE") >= 0) {
                Map<String, Object> uploadImage = new HashMap<>();
                uploadImage.put("userLogin", userLogin);
                uploadImage.put("dataResourceId", dataResourceId);
                uploadImage.put("dataResourceTypeId", dataResourceTypeId);
                uploadImage.put("rootDir", context.get(x.objectInfo));
                uploadImage.put("uploadedFile", imageDataBytes);
                uploadImage.put("_uploadedFile_fileName", context.get(x._imageData_fileName));
                uploadImage.put("_uploadedFile_contentType", context.get(x._imageData_contentType));
                thisResult = dispatcher.runSync("attachUploadToDataResource", uploadImage);
                if (ServiceUtil.isError(thisResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                }
            } else {
                if (UtilValidate.isNotEmpty(textData) || "true".equalsIgnoreCase(forceElectronicText)) {
                    fileContext.put("dataResourceId", dataResourceId);
                    fileContext.put("textData", textData);
                    thisResult = dispatcher.runSync("updateElectronicText", fileContext);
                    if (ServiceUtil.isError(thisResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                    }
                }
            }
        }
        result.put("dataResourceId", dataResourceId);
        result.put("drDataResourceId", dataResourceId);
        context.put(x.dataResourceId, dataResourceId);
        return result;
    }

    public static void addRoleToUser(Delegator delegator, LocalDispatcher dispatcher, Map<String, Object> serviceContext)
            throws GenericServiceException, GenericEntityException {
        Map<String, Object> result = new HashMap<>();
        List<GenericValue> userLoginList = EntityQuery.use(delegator).from("UserLogin").where("partyId", serviceContext.get("partyId")).queryList();
        for (GenericValue partyUserLogin : userLoginList) {
            String partyUserLoginId = partyUserLogin.getString(x.userLoginId);
            serviceContext.put("contentId", partyUserLoginId); // author contentId
            result = dispatcher.runSync("createContentRole", serviceContext);
            if (ServiceUtil.isError(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), MODULE);
            }
        }
    }

    public static Map<String, Object> updateSiteRolesDyn(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> results = new HashMap<>();
        Map<String, Object> thisResult = new HashMap<>();
        Map<String, Object> serviceContext = new HashMap<>();
        // siteContentId will equal "ADMIN_MASTER", "AGINC_MASTER", etc.
        // Remember that this service is called in the "multi" mode,
        // with a new siteContentId each time.
        // siteContentId could also have been name deptContentId, since this same
        // service is used for updating department roles, too.
        String siteContentId = (String) context.get(x.contentId);
        String partyId = (String) context.get(x.partyId);
        serviceContext.put("partyId", partyId);
        serviceContext.put("contentId", siteContentId);

        List<GenericValue> siteRoles = null;
        try {
            siteRoles = EntityQuery.use(delegator).from("RoleType").where("parentTypeId", "BLOG").cache().queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }
        for (GenericValue roleType : siteRoles) {
            String siteRole = (String) roleType.get(x.roleTypeId); // BLOG_EDITOR, BLOG_ADMIN, etc.
            String cappedSiteRole = ModelUtil.dbNameToVarName(siteRole);

            String siteRoleVal = (String) context.get(cappedSiteRole);
            Object fromDate = context.get(cappedSiteRole + "FromDate");
            serviceContext.put("roleTypeId", siteRole);
            if (siteRoleVal != null && "Y".equalsIgnoreCase(siteRoleVal)) {
                // for now, will assume that any error is due to duplicates - ignore
                if (fromDate == null) {
                    try {
                        serviceContext.put("fromDate", UtilDateTime.nowTimestamp());
                        if (Debug.infoOn()) {
                            Debug.logInfo("updateSiteRoles, serviceContext(1):" + serviceContext, MODULE);
                        }
                        addRoleToUser(delegator, dispatcher, serviceContext);
                        thisResult = dispatcher.runSync("createContentRole", serviceContext);
                        if (ServiceUtil.isError(thisResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                        }
                    } catch (GenericEntityException | GenericServiceException e) {
                        Debug.logError(e, e.toString(), MODULE);
                    }
                }
            } else {
                if (fromDate != null) {
                    // for now, will assume that any error is due to non-existence - ignore
                    // return ServiceUtil.returnError(e.toString());
                    try {
                        Debug.logInfo("updateSiteRoles, serviceContext(2):" + serviceContext, MODULE);
                        Map<String, Object> newContext = new HashMap<>();
                        newContext.put("contentId", serviceContext.get("contentId"));
                        newContext.put("partyId", serviceContext.get("partyId"));
                        newContext.put("roleTypeId", serviceContext.get("roleTypeId"));
                        thisResult = dispatcher.runSync("deactivateAllContentRoles", newContext);
                        if (ServiceUtil.isError(thisResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, e.toString(), MODULE);
                    }
                }
            }
        }
        return results;
    }

    public static Map<String, Object> updateOrRemove(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> results = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String entityName = (String) context.get(x.entityName);
        String action = (String) context.get(x.action);
        String pkFieldCount = (String) context.get(x.pkFieldCount);
        Map<String, String> pkFields = new HashMap<>();
        int fieldCount = Integer.parseInt(pkFieldCount);
        for (int i = 0; i < fieldCount; i++) {
            String fieldName = (String) context.get(x.fieldName + i);
            String fieldValue = (String) context.get(x.fieldValue + i);
            if (UtilValidate.isEmpty(fieldValue)) {
                // It may be the case that the last row in a form is "empty" waiting for
                // someone to enter a value, in which case we do not want to throw an
                // error, we just want to ignore it.
                return results;
            }
            pkFields.put(fieldName, fieldValue);
        }
        boolean doLink = "Y".equalsIgnoreCase(action);
        if (Debug.infoOn()) {
            Debug.logInfo("in updateOrRemove, context:" + context, MODULE);
        }
        try {
            GenericValue entityValuePK = delegator.makeValue(entityName, pkFields);
            if (Debug.infoOn()) {
                Debug.logInfo("in updateOrRemove, entityValuePK:" + entityValuePK, MODULE);
            }
            GenericValue entityValueExisting = EntityQuery.use(delegator).from(entityName).where(entityValuePK).cache().queryOne();
            if (Debug.infoOn()) {
                Debug.logInfo("in updateOrRemove, entityValueExisting:" + entityValueExisting, MODULE);
            }
            if (entityValueExisting == null) {
                if (doLink) {
                    entityValuePK.create();
                    if (Debug.infoOn()) {
                        Debug.logInfo("in updateOrRemove, entityValuePK: CREATED", MODULE);
                    }
                }
            } else {
                if (!doLink) {
                    entityValueExisting.remove();
                    if (Debug.infoOn()) {
                        Debug.logInfo("in updateOrRemove, entityValueExisting: REMOVED", MODULE);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }

    /**
     * Reorder sequence numbers in ContentAssoc entities for a given parent id
     * if variable dir contains the string up or down, realize for the given contentIdTo switch
     * with the previous or the next
     * @param dctx
     * @param context
     * @return
     * @throws GenericServiceException
     */
    public static Map<String, Object> resequence(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String contentId = (String) context.get(x.contentId);

        int seqStep = (Integer) context.get(x.seqInc);

        // Resolve the association type to use for resolve content's child
        List<String> typeList = UtilGenerics.cast(context.get(x.typeList));
        if (typeList == null) {
            typeList = new LinkedList<>();
        }
        String contentAssocTypeId = (String) context.get(x.contentAssocTypeId);
        if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
            typeList.add(contentAssocTypeId);
        }
        if (UtilValidate.isEmpty(typeList)) {
            typeList = UtilMisc.toList("PUBLISH_LINK", "SUB_CONTENT");
        }

        // Resolve all content assoc to resequence from the content
        try {
            List<GenericValue> contentAssocs = EntityQuery.use(delegator).from("ContentAssoc")
                    .where(List.of(
                            EntityCondition.makeCondition("contentId", contentId),
                            EntityCondition.makeCondition("contentAssocTypeId", EntityOperator.IN, typeList)))
                    .orderBy("sequenceNum", "fromDate", "createdDate")
                    .filterByDate()
                    .queryList();
            String contentIdTo = (String) context.get(x.contentIdTo);
            String dir = (String) context.get(x.dir);
            int seqNum = seqStep;
            boolean switchSequence = UtilValidate.isNotEmpty(contentIdTo) && UtilValidate.isNotEmpty(dir);
            boolean switchModeUp = switchSequence && dir.startsWith("up");
            int changePosition = -1;

            // update the sequence for all element and check if we need switch two element
            for (int i = 0; i < contentAssocs.size(); i++) {
                boolean stopLimit = ((i <= 0 && switchModeUp) || (i + 1 >= contentAssocs.size() && !switchModeUp));
                GenericValue contentAssoc = contentAssocs.get(i);
                contentAssoc.put("sequenceNum", (long) seqNum);
                seqNum += seqStep;
                if (switchSequence
                        && contentIdTo.equals(contentAssoc.getString(x.contentIdTo))
                        && !stopLimit) {
                    changePosition = i;
                }
            }

            // We need to switch, do it
            if (changePosition >= 0) {
                GenericValue currentContent = contentAssocs.get(changePosition);
                GenericValue destinationContent = contentAssocs.get(changePosition + (switchModeUp ? -1 : +1));
                long switchSeqNum = currentContent.getLong(x.sequenceNum);
                currentContent.put("sequenceNum", destinationContent.getLong(x.sequenceNum));
                destinationContent.put("sequenceNum", switchSeqNum);
            }
            delegator.storeAll(contentAssocs);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> changeLeafToNode(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> thisResult = new HashMap<>();
        String contentId = (String) context.get(x.contentId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String userLoginId = userLogin.getString(x.userLoginId);
        Locale locale = (Locale) context.get(x.locale);
        try {
            GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            if (content == null) {
                Debug.logError("content was null", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentNoContentFound", UtilMisc.toMap("contentId", ""), locale));
            }
            String dataResourceId = content.getString(x.dataResourceId);

            content.set(x.dataResourceId, null);
            content.set(x.lastModifiedDate, UtilDateTime.nowTimestamp());
            content.set(x.lastModifiedByUserLogin, userLoginId);
            content.store();

            if (UtilValidate.isNotEmpty(dataResourceId)) {
                // add previous DataResource as part of new subcontent
                GenericValue contentClone = (GenericValue) content.clone();
                contentClone.set(x.dataResourceId, dataResourceId);
                content.set(x.lastModifiedDate, UtilDateTime.nowTimestamp());
                content.set(x.lastModifiedByUserLogin, userLoginId);
                content.set(x.createdDate, UtilDateTime.nowTimestamp());
                content.set(x.createdByUserLogin, userLoginId);

                contentClone.set(x.contentId, null);
                ModelService modelService = dctx.getModelService("persistContentAndAssoc");
                Map<String, Object> serviceIn = modelService.makeValid(contentClone, ModelService.IN_PARAM);
                serviceIn.put("userLogin", userLogin);
                serviceIn.put("contentIdTo", contentId);
                serviceIn.put("contentAssocTypeId", "SUB_CONTENT");
                serviceIn.put("sequenceNum", 50L);
                try {
                    thisResult = dispatcher.runSync("persistContentAndAssoc", serviceIn);
                    if (ServiceUtil.isError(thisResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                    }
                } catch (ServiceAuthException e) {
                    return ServiceUtil.returnError(e.toString());
                }

                List<String> typeList = UtilMisc.toList("SUB_CONTENT");
                ContentManagementWorker.updateStatsTopDown(delegator, contentId, typeList);
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> updateLeafCount(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        List<String> typeList = UtilGenerics.cast(context.get(x.typeList));
        if (typeList == null) {
            typeList = UtilMisc.toList("PUBLISH_LINK", "SUB_CONTENT");
        }
        String startContentId = (String) context.get(x.contentId);
        try {
            int leafCount = ContentManagementWorker.updateStatsTopDown(delegator, startContentId, typeList);
            result.put("leafCount", leafCount);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    /**
     * This service changes the contentTypeId of the current content and its children depending on the pageMode.
     * if pageMode == "outline" then if the contentTypeId of children is not "OUTLINE_NODE" or "PAGE_NODE"
     * (it could be DOCUMENT or SUBPAGE_NODE) then it will get changed to PAGE_NODE.`
     * if pageMode == "page" then if the contentTypeId of children is not "PAGE_NODE" or "SUBPAGE_NODE"
     * (it could be DOCUMENT or OUTLINE_NODE) then it will get changed to SUBPAGE_NODE.`
     */
    public static Map<String, Object> updatePageType(DispatchContext dctx, Map<String, ? extends Object> rcontext) throws GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Map<String, Object> results = new HashMap<>();
        Locale locale = (Locale) context.get(x.locale);
        Set<String> visitedSet = UtilGenerics.cast(context.get(x.visitedSet));
        if (visitedSet == null) {
            visitedSet = new HashSet<>();
            context.put(x.visitedSet, visitedSet);
        }
        String pageMode = (String) context.get(x.pageMode);
        String contentId = (String) context.get(x.contentId);
        visitedSet.add(contentId);
        String contentTypeId = "PAGE_NODE";
        if (pageMode != null && pageMode.toLowerCase(Locale.getDefault()).indexOf("outline") >= 0) {
            contentTypeId = "OUTLINE_NODE";
        }
        GenericValue thisContent = null;
        try {
            thisContent = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            if (thisContent == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentNoContentFound", UtilMisc.toMap("contentId", contentId),
                        locale));
            }
            thisContent.set(x.contentTypeId, contentTypeId);
            thisContent.store();
            List<GenericValue> kids = ContentWorker.getAssociatedContent(thisContent, "from", UtilMisc.toList("SUB_CONTENT"), null, null, null);
            for (GenericValue kidContent : kids) {
                if ("OUTLINE_NODE".equals(contentTypeId)) {
                    updateOutlineNodeChildren(kidContent, false, context);
                } else {
                    updatePageNodeChildren(kidContent, context);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.toString());
        }

        return results;
    }

    public static Map<String, Object> resetToOutlineMode(DispatchContext dctx, Map<String, ? extends Object> rcontext)
            throws GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Map<String, Object> results = new HashMap<>();
        Locale locale = (Locale) context.get(x.locale);
        Set<String> visitedSet = UtilGenerics.cast(context.get(x.visitedSet));
        if (visitedSet == null) {
            visitedSet = new HashSet<>();
            context.put(x.visitedSet, visitedSet);
        }
        String contentId = (String) context.get(x.contentId);
        String pageMode = (String) context.get(x.pageMode);
        String contentTypeId = "OUTLINE_NODE";
        if (pageMode != null && pageMode.toLowerCase(Locale.getDefault()).indexOf("page") >= 0) {
            contentTypeId = "PAGE_NODE";
        }
        GenericValue thisContent = null;
        try {
            thisContent = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            if (thisContent == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ContentNoContentFound", UtilMisc.toMap("contentId", contentId), locale));
            }
            thisContent.set(x.contentTypeId, "OUTLINE_NODE");
            thisContent.store();
            List<GenericValue> kids = ContentWorker.getAssociatedContent(thisContent, "from", UtilMisc.toList("SUB_CONTENT"), null, null, null);
            for (GenericValue kidContent : kids) {
                if ("OUTLINE_NODE".equals(contentTypeId)) {
                    updateOutlineNodeChildren(kidContent, true, context);
                } else {
                    kidContent.put("contentTypeId", "PAGE_NODE");
                    kidContent.store();
                    List<GenericValue> kids2 = ContentWorker.getAssociatedContent(kidContent, "from", UtilMisc.toList("SUB_CONTENT"), null, null,
                            null);
                    for (GenericValue kidContent2 : kids2) {
                        updatePageNodeChildren(kidContent2, context);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }

    public static Map<String, Object> clearContentAssocViewCache(DispatchContext dctx, Map<String, ? extends Object> context)
            throws GenericServiceException {
        Map<String, Object> results = new HashMap<>();
        UtilCache<?, ?> utilCache = UtilCache.findCache("entitycache.entity-list.default.ContentAssocViewFrom");

        if (utilCache != null) {
            utilCache.clear();
        }

        utilCache = UtilCache.findCache("entitycache.entity-list.default.ContentAssocViewTo");
        if (utilCache != null) {
            utilCache.clear();
        }

        return results;
    }

    public static Map<String, Object> clearContentAssocDataResourceViewCache(DispatchContext dctx, Map<String, ? extends Object> context)
            throws GenericServiceException {
        Map<String, Object> results = new HashMap<>();

        UtilCache<?, ?> utilCache = UtilCache.findCache("entitycache.entity-list.default.ContentAssocViewDataResourceFrom");
        if (utilCache != null) {
            utilCache.clear();
        }

        utilCache = UtilCache.findCache("entitycache.entity-list.default.ContentAssocViewDataResourceTo");
        if (utilCache != null) {
            utilCache.clear();
        }

        return results;
    }

    public static void updatePageNodeChildren(GenericValue content, Map<String, Object> context) throws GenericEntityException {
        String contentId = content.getString(x.contentId);
        Set<String> visitedSet = UtilGenerics.cast(context.get(x.visitedSet));
        if (visitedSet == null) {
            visitedSet = new HashSet<>();
            context.put(x.visitedSet, visitedSet);
        } else {
            if (visitedSet.contains(contentId)) {
                Debug.logWarning("visitedSet already contains:" + contentId, MODULE);
                return;
            } else {
                visitedSet.add(contentId);
            }
        }
        String newContentTypeId = "SUBPAGE_NODE";
        content.put("contentTypeId", newContentTypeId);
        content.store();
        List<GenericValue> kids = ContentWorker.getAssociatedContent(content, "from", UtilMisc.toList("SUB_CONTENT"), null, null, null);
        for (GenericValue kidContent : kids) {
            updatePageNodeChildren(kidContent, context);
        }
    }

    public static void updateOutlineNodeChildren(GenericValue content, boolean forceOutline, Map<String, Object> context)
            throws GenericEntityException {
        String contentId = content.getString(x.contentId);
        Set<String> visitedSet = UtilGenerics.cast(context.get(x.visitedSet));
        if (visitedSet == null) {
            visitedSet = new HashSet<>();
            context.put(x.visitedSet, visitedSet);
        } else {
            if (visitedSet.contains(contentId)) {
                Debug.logWarning("visitedSet already contains:" + contentId, MODULE);
                return;
            } else {
                visitedSet.add(contentId);
            }
        }
        String contentTypeId = content.getString(x.contentTypeId);
        String newContentTypeId = contentTypeId;
        String dataResourceId = content.getString(x.dataResourceId);
        Long branchCount = (Long) content.get(x.childBranchCount);
        if (forceOutline) {
            newContentTypeId = "OUTLINE_NODE";
        } else if (contentTypeId == null || "DOCUMENT".equals(contentTypeId)) {
            if (UtilValidate.isEmpty(dataResourceId) || (branchCount != null && branchCount.intValue() > 0)) {
                newContentTypeId = "OUTLINE_NODE";
            } else {
                newContentTypeId = "PAGE_NODE";
            }
        } else if ("SUBPAGE_NODE".equals(contentTypeId)) {
            newContentTypeId = "PAGE_NODE";
        }

        content.put("contentTypeId", newContentTypeId);
        content.store();

        if (contentTypeId == null || "DOCUMENT".equals(contentTypeId) || "OUTLINE_NODE".equals(contentTypeId)) {
            List<GenericValue> kids = ContentWorker.getAssociatedContent(content, "from", UtilMisc.toList("SUB_CONTENT"), null, null, null);
            for (GenericValue kidContent : kids) {
                updateOutlineNodeChildren(kidContent, forceOutline, context);
            }
        }
    }

    public static Map<String, Object> findSubNodes(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException {
        Map<String, Object> results = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String contentIdTo = (String) context.get(x.contentId);
        try {
            List<GenericValue> lst = EntityQuery.use(delegator).from("ContentAssocDataResourceViewFrom")
                    .where("caContentIdTo", contentIdTo,
                            "caContentAssocTypeId", "SUB_CONTENT",
                            "caThruDate", null)
                    .orderBy("caSequenceNum", "caFromDate", "createdDate")
                    .queryList();
            results.put("_LIST_", lst);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }

    public static String updateTypeAndFile(GenericValue dataResource, Map<String, Object> context) {
        String retVal = null;
        String mimeTypeId = (String) context.get(x._imageData_contentType);
        String fileName = (String) context.get(x._imageData_fileName);
        try {
            if (UtilValidate.isNotEmpty(fileName)) {
                dataResource.set(x.objectInfo, fileName);
            }
            if (UtilValidate.isNotEmpty(mimeTypeId)) {
                dataResource.set(x.mimeTypeId, mimeTypeId);
            }
            dataResource.store();
        } catch (GenericEntityException e) {
            retVal = "Unable to update the DataResource record";
        }
        return retVal;
    }

    public static Map<String, Object> initContentChildCounts(DispatchContext dctx, Map<String, ? extends Object> context)
            throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue content = (GenericValue) context.get(x.content);
        if (content == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentNoContentFound", UtilMisc.toMap("contentId", ""), locale));
        }
        Long leafCount = (Long) content.get(x.childLeafCount);
        if (leafCount == null) {
            content.set(x.childLeafCount, 0L);
        }
        Long branchCount = (Long) content.get(x.childBranchCount);
        if (branchCount == null) {
            content.set(x.childBranchCount, 0L);
        }

        return result;
    }

    public static Map<String, Object> incrementContentChildStats(DispatchContext dctx, Map<String, ? extends Object> context)
            throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        String contentId = (String) context.get(x.contentId);
        String contentAssocTypeId = (String) context.get(x.contentAssocTypeId);

        try {
            GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).cache().queryOne();
            if (content == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentNoContentFound", UtilMisc.toMap("contentId", contentId),
                        locale));
            }
            Long leafCount = (Long) content.get(x.childLeafCount);
            if (leafCount == null) {
                leafCount = 0L;
            }
            int changeLeafCount = leafCount.intValue() + 1;
            int changeBranchCount = 1;

            ContentManagementWorker.updateStatsBottomUp(delegator, contentId, UtilMisc.toList(contentAssocTypeId), changeBranchCount,
                    changeLeafCount);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> decrementContentChildStats(DispatchContext dctx, Map<String, ? extends Object> context)
            throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        String contentId = (String) context.get(x.contentId);
        String contentAssocTypeId = (String) context.get(x.contentAssocTypeId);

        try {
            GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).cache().queryOne();
            if (content == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentNoContentFound", UtilMisc.toMap("contentId", contentId),
                        locale));
            }
            Long leafCount = (Long) content.get(x.childLeafCount);
            if (leafCount == null) {
                leafCount = 0L;
            }
            int changeLeafCount = -1 * leafCount.intValue() - 1;
            int changeBranchCount = -1;

            ContentManagementWorker.updateStatsBottomUp(delegator, contentId, UtilMisc.toList(contentAssocTypeId), changeBranchCount,
                    changeLeafCount);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> updateContentChildStats(DispatchContext dctx, Map<String, ? extends Object> context)
            throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();

        String contentId = (String) context.get(x.contentId);
        String contentAssocTypeId = (String) context.get(x.contentAssocTypeId);
        List<String> typeList = new LinkedList<>();
        if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
            typeList.add(contentAssocTypeId);
        } else {
            typeList = UtilMisc.toList("PUBLISH_LINK", "SUB_CONTENT");
        }

        try {
            ContentManagementWorker.updateStatsTopDown(delegator, contentId, typeList);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> updateContentSubscription(DispatchContext dctx, Map<String, ? extends Object> context)
            throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Map<String, Object> thisResult = new HashMap<>();
        String partyId = (String) context.get(x.partyId);
        String webPubPt = (String) context.get(x.contentId);
        String roleTypeId = (String) context.get(x.useRoleTypeId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Integer useTime = (Integer) context.get(x.useTime);
        String useTimeUomId = (String) context.get(x.useTimeUomId);
        boolean hasExistingContentRole = false;
        GenericValue contentRole = null;
        try {
            contentRole = EntityQuery.use(delegator).from("ContentRole")
                    .where("partyId", partyId, "contentId", webPubPt, "roleTypeId", roleTypeId)
                    .orderBy("fromDate DESC")
                    .cache().filterByDate()
                    .queryFirst();
            if (contentRole != null) {
                hasExistingContentRole = true;
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }

        if (contentRole == null) {
            contentRole = delegator.makeValue("ContentRole");
            contentRole.set(x.contentId, webPubPt);
            contentRole.set(x.partyId, partyId);
            contentRole.set(x.roleTypeId, roleTypeId);
            contentRole.set(x.fromDate, nowTimestamp);
        }

        Timestamp thruDate = (Timestamp) contentRole.get(x.thruDate);
        if (thruDate == null) {
            // no thruDate? start with NOW
            thruDate = nowTimestamp;
        } else {
            // there is a thru date... if it is in the past, bring it up to NOW before adding on the time period
            //don't want to penalize for skipping time, in other words if they had a subscription last year for a month and buy another month, we
            // want that second month to start now and not last year
            if (thruDate.before(nowTimestamp)) {
                thruDate = nowTimestamp;
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(thruDate);
        int field = Calendar.MONTH;
        if ("TF_day".equals(useTimeUomId)) {
            field = Calendar.DAY_OF_YEAR;
        } else if ("TF_wk".equals(useTimeUomId)) {
            field = Calendar.WEEK_OF_YEAR;
        } else if ("TF_mon".equals(useTimeUomId)) {
            field = Calendar.MONTH;
        } else if ("TF_yr".equals(useTimeUomId)) {
            field = Calendar.YEAR;
        } else {
            Debug.logWarning("Don't know anything about useTimeUomId [" + useTimeUomId + "], defaulting to month", MODULE);
        }
        calendar.add(field, useTime);
        thruDate = new Timestamp(calendar.getTimeInMillis());
        contentRole.set(x.thruDate, thruDate);
        try {
            if (hasExistingContentRole) {
                contentRole.store();
            } else {
                Map<String, Object> map = new HashMap<>();
                map.put("partyId", partyId);
                map.put("roleTypeId", roleTypeId);
                map.put("userLogin", userLogin);
                thisResult = dispatcher.runSync("ensurePartyRole", map);
                if (ServiceUtil.isError(thisResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResult));
                }
                contentRole.create();
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> updateContentSubscriptionByProduct(DispatchContext dctx, Map<String, ? extends Object> rcontext)
            throws GenericServiceException {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Map<String, Object> result;
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productId = (String) context.get(x.productId);
        Integer qty = (Integer) context.get(x.quantity);
        if (qty == null) {
            qty = 1;
        }

        GenericValue productContent = null;
        try {
            List<GenericValue> lst = EntityQuery.use(delegator).from("ProductContent")
                    .where("productId", productId, "productContentTypeId", "ONLINE_ACCESS")
                    .orderBy("purchaseFromDate", "purchaseThruDate")
                    .filterByDate("purchaseFromDate", "purchaseThruDate")
                    .cache().queryList();
            List<GenericValue> listThrusOnly = EntityUtil.filterOutByCondition(lst, EntityCondition.makeCondition("purchaseThruDate",
                    EntityOperator.EQUALS, null));
            if (!listThrusOnly.isEmpty()) {
                productContent = listThrusOnly.get(0);
            } else if (!lst.isEmpty()) {
                productContent = lst.get(0);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.toString(), MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        if (productContent == null) {
            String msg = UtilProperties.getMessage(RESOURCE, "ContentNoProductContentFound", UtilMisc.toMap("productId", productId), locale);
            Debug.logError(msg, MODULE);
            return ServiceUtil.returnError(msg);
        }
        Long useTime = (Long) productContent.get(x.useTime);
        Integer newUseTime = null;
        if (UtilValidate.isNotEmpty(useTime)) {
            newUseTime = useTime.intValue() * qty;
        }
        context.put(x.useTime, newUseTime);
        context.put(x.useTimeUomId, productContent.get(x.useTimeUomId));
        context.put(x.useRoleTypeId, productContent.get(x.useRoleTypeId));
        context.put(x.contentId, productContent.get(x.contentId));
        ModelService subscriptionModel = dispatcher.getDispatchContext().getModelService("updateContentSubscription");
        Map<String, Object> ctx = subscriptionModel.makeValid(context, ModelService.IN_PARAM);
        result = dispatcher.runSync("updateContentSubscription", ctx);
        if (ServiceUtil.isError(result)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
        }
        return result;
    }

    public static Map<String, Object> updateContentSubscriptionByOrder(DispatchContext dctx, Map<String, ? extends Object> rcontext)
            throws GenericServiceException {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderId = (String) context.get(x.orderId);

        Debug.logInfo("In updateContentSubscriptionByOrder service with orderId: " + orderId, MODULE);

        GenericValue orderHeader = null;
        try {
            GenericValue orderRole = EntityQuery.use(delegator).from("OrderRole")
                    .where("orderId", orderId, "roleTypeId", "END_USER_CUSTOMER")
                    .queryFirst();
            if (orderRole != null) {
                String partyId = (String) orderRole.get(x.partyId);
                context.put(x.partyId, partyId);
            } else {
                String msg = "No OrderRole found for orderId:" + orderId;
                return ServiceUtil.returnFailure(msg);

            }
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            if (orderHeader == null) {
                String msg = UtilProperties.getMessage(RESOURCE, "ContentNoOrderHeaderFound", UtilMisc.toMap("orderId", orderId), locale);
                return ServiceUtil.returnError(msg);
            }
            Timestamp orderCreatedDate = (Timestamp) orderHeader.get(x.orderDate);
            context.put(x.orderCreatedDate, orderCreatedDate);
            List<GenericValue> orderItemList = orderHeader.getRelated(x.OrderItem, null, null, false);
            ModelService subscriptionModel = dispatcher.getDispatchContext().getModelService("updateContentSubscriptionByProduct");
            for (GenericValue orderItem : orderItemList) {
                BigDecimal qty = orderItem.getBigDecimal(x.quantity);
                String productId = (String) orderItem.get(x.productId);
                long productContentCount = EntityQuery.use(delegator).from("ProductContent")
                        .where("productId", productId, "productContentTypeId", "ONLINE_ACCESS")
                        .filterByDate().queryCount();
                if (productContentCount > 0) {
                    context.put(x.productId, productId);
                    context.put(x.quantity, qty.intValue());
                    Map<String, Object> ctx = subscriptionModel.makeValid(context, ModelService.IN_PARAM);
                    result = dispatcher.runSync("updateContentSubscriptionByProduct", ctx);
                    if (ServiceUtil.isError(result)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.toString(), MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> followNodeChildren(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException {
        Map<String, Object> result = null;
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        if (!security.hasEntityPermission("CONTENTMGR", "_ADMIN", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentPermissionNotGranted", locale));
        }
        String contentId = (String) context.get(x.contentId);
        String serviceName = (String) context.get(x.serviceName);
        String contentAssocTypeId = (String) context.get(x.contentAssocTypeId);
        List<String> contentAssocTypeIdList = new LinkedList<>();
        if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
            contentAssocTypeIdList = StringUtil.split(contentAssocTypeId, "|");
        }
        if (contentAssocTypeIdList.isEmpty()) {
            contentAssocTypeIdList.add("SUB_CONTENT");
        }
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("userLogin", userLogin);
        ctx.put("contentAssocTypeIdList", contentAssocTypeIdList);
        try {

            GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            result = followNodeChildrenMethod(content, dispatcher, serviceName, ctx);
        } catch (GenericEntityException e) {
            Debug.logError(e.toString(), MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> followNodeChildrenMethod(GenericValue content, LocalDispatcher dispatcher, String serviceName,
            Map<String, Object> context) throws GenericEntityException, GenericServiceException {
        Map<String, Object> result = null;
        String contentId = content.getString(x.contentId);
        List<String> contentAssocTypeIdList = UtilGenerics.cast(context.get(x.contentAssocTypeIdList));
        Locale locale = (Locale) context.get(x.locale);
        Set<String> visitedSet = UtilGenerics.cast(context.get(x.visitedSet));
        if (visitedSet == null) {
            visitedSet = new HashSet<>();
            context.put(x.visitedSet, visitedSet);
        } else {
            if (visitedSet.contains(contentId)) {
                Debug.logWarning("visitedSet already contains:" + contentId, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentVisitedSet", locale) + contentId);
            } else {
                visitedSet.add(contentId);
            }
        }

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        result = dispatcher.runSync(serviceName, UtilMisc.toMap("content", content, "userLogin", userLogin));
        if (ServiceUtil.isError(result)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
        }

        List<GenericValue> kids = ContentWorker.getAssociatedContent(content, "from", contentAssocTypeIdList, null, null, null);
        for (GenericValue kidContent : kids) {
            followNodeChildrenMethod(kidContent, dispatcher, serviceName, context);
        }
        return result;
    }

    private static String validateUploadedFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        String objectInfo = (String) context.get(x.objectInfo);
        String errorMessage = null;
        if (!UtilValidate.isEmpty(objectInfo)) {
            File file = new File(objectInfo);
            if (file.isFile()) {
                try {
                    // Check if a webshell is not uploaded
                    // This should now be useless after the add of SecuredUpload::isValidFile in GroovyBaseScript for createAnonFile service.
                    // But I prefer to keep it anyway, it's hard to test all cases, better safe than sorry
                    if (!org.apache.ofbiz.security.SecuredUpload.isValidFile(objectInfo, "All", delegator)) {
                        errorMessage = UtilProperties.getMessage("SecurityUiLabels", "SupportedFileFormatsIncludingSvg", locale);
                    }
                } catch (ImageReadException | IOException e) {
                    errorMessage = UtilProperties.getMessage(RESOURCE, "ContentUnableToOpenFileForWriting", UtilMisc.toMap("fileName",
                            objectInfo), locale);
                }
            }
        }
        return errorMessage;
    }
}
