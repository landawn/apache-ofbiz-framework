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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.ContentAssocDao;
import org.apache.ofbiz.persistence.dao.ContentDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.entity.ContentAssocEntity;
import org.apache.ofbiz.persistence.entity.ContentEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ContentServicesContext;
/**
 * ContentServices Class
 */
public class ContentServices {

    private static final String MODULE = ContentServices.class.getName();
    private static final String RESOURCE = x.ContentUiLabels;

    /**
     * findRelatedContent Finds the related
     */
    public static Map<String, Object> findRelatedContent(DispatchContext dctx, ContentServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> results = new HashMap<>();

        GenericValue currentContent = (GenericValue) context.get(x.currentContent);
        String fromDate = (String) context.get(x.fromDate);
        String thruDate = (String) context.get(x.thruDate);
        String toFrom = (String) context.get(x.toFrom);
        Locale locale = (Locale) context.get(x.locale);
        if (toFrom == null) {
            toFrom = x.TO;
        } else {
            toFrom = toFrom.toUpperCase(Locale.getDefault());
        }

        List<String> assocTypes = UtilGenerics.cast(context.get(x.contentAssocTypeList));
        List<String> targetOperations = UtilGenerics.cast(context.get(x.targetOperationList));
        List<String> contentTypes = UtilGenerics.cast(context.get(x.contentTypeList));
        List<GenericValue> contentList = null;

        try {
            contentList = ContentWorker.getAssociatedContent(currentContent, toFrom, assocTypes, contentTypes, fromDate, thruDate);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentAssocRetrievingError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        if (UtilValidate.isEmpty(targetOperations)) {
            results.put(x.contentList, contentList);
            return results;
        }

        Map<String, Object> serviceInMap = new HashMap<>();
        serviceInMap.put(x.userLogin, context.get(x.userLogin));
        serviceInMap.put(x.targetOperationList, targetOperations);
        serviceInMap.put(x.entityOperation, context.get(x.entityOperation));

        List<GenericValue> permittedList = new LinkedList<>();
        Map<String, Object> permResults = null;
        for (GenericValue content : contentList) {
            serviceInMap.put(x.currentContent, content);
            try {
                permResults = dispatcher.runSync(x.checkContentPermission, serviceInMap);
                if (ServiceUtil.isError(permResults)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(permResults));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_checking_permissions, x.ContentServices);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentPermissionNotGranted, locale));
            }

            String permissionStatus = (String) permResults.get(x.permissionStatus);
            if (permissionStatus != null && x.granted.equalsIgnoreCase(permissionStatus)) {
                permittedList.add(content);
            }

        }

        results.put(x.contentList, permittedList);
        return results;
    }
    /**
     * This is a generic service for traversing a Content tree, typical of a blog response tree. It calls the ContentWorker.traverse method.
     */
    public static Map<String, Object> findContentParents(DispatchContext dctx, ContentServicesContext context) {
        Map<String, Object> results = new HashMap<>();
        List<Object> parentList = new LinkedList<>();
        results.put(x.parentList, parentList);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String contentId = (String) context.get(x.contentId);
        String contentAssocTypeId = (String) context.get(x.contentAssocTypeId);
        String direction = (String) context.get(x.direction);
        if (UtilValidate.isEmpty(direction)) {
            direction = x.To;
        }
        Map<String, Object> traversMap = new HashMap<>();
        traversMap.put(x.contentId, contentId);
        traversMap.put(x.direction, direction);
        traversMap.put(x.contentAssocTypeId, contentAssocTypeId);
        try {
            Map<String, Object> thisResults = dispatcher.runSync(x.traverseContent, traversMap);
            if (ServiceUtil.isError(thisResults)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResults));
            }
            Map<String, Object> nodeMap = UtilGenerics.cast(thisResults.get(x.nodeMap));
            walkParentTree(nodeMap, parentList);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnFailure(e.getMessage());
        }
        return results;
    }

    private static void walkParentTree(Map<String, Object> nodeMap, List<Object> parentList) {
        List<Map<String, Object>> kids = UtilGenerics.cast(nodeMap.get(x.kids));
        if (UtilValidate.isEmpty(kids)) {
            parentList.add(nodeMap.get(x.contentId));
        } else {
            for (Map<String, Object> node : kids) {
                walkParentTree(node, parentList);
            }
        }
    }
    /**
     * This is a generic service for traversing a Content tree, typical of a blog response tree. It calls the ContentWorker.traverse method.
     */
    public static Map<String, Object> traverseContent(DispatchContext dctx, ContentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> results = new HashMap<>();
        Locale locale = (Locale) context.get(x.locale);

        String contentId = (String) context.get(x.contentId);
        String direction = (String) context.get(x.direction);
        if (direction != null && x.From.equalsIgnoreCase(direction)) {
            direction = x.From;
        } else {
            direction = x.To;
        }

        if (contentId == null) {
            contentId = x.PUBLISH_ROOT;
        }

        GenericValue content = null;
        try {
            ContentDao contentDao = DaoRegistry.getDao(delegator, x.Content, ContentDao.class);
            ContentEntity contentEntity = contentDao.get(contentId).orElse(null);
            content = contentEntity == null ? null : delegator.makeValue(x.Content, Beans.beanToMap(contentEntity));
        } catch (Exception e) {
            Debug.logError(e, x.Entity_Error + e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentNoContentFound,
                    UtilMisc.toMap(x.contentId, contentId), locale));
        }

        String fromDateStr = (String) context.get(x.fromDateStr);
        String thruDateStr = (String) context.get(x.thruDateStr);
        Timestamp fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }

        Timestamp thruDate = null;
        if (UtilValidate.isNotEmpty(thruDateStr)) {
            thruDate = UtilDateTime.toTimestamp(thruDateStr);
        }

        Map<String, Object> whenMap = new HashMap<>();
        whenMap.put(x.followWhen, context.get(x.followWhen));
        whenMap.put(x.pickWhen, context.get(x.pickWhen));
        whenMap.put(x.returnBeforePickWhen, context.get(x.returnBeforePickWhen));
        whenMap.put(x.returnAfterPickWhen, context.get(x.returnAfterPickWhen));

        String startContentAssocTypeId = (String) context.get(x.contentAssocTypeId);
        if (startContentAssocTypeId != null) {
            startContentAssocTypeId = x.PUBLISH;
        }

        Map<String, Object> nodeMap = new HashMap<>();
        List<GenericValue> pickList = new LinkedList<>();
        ContentWorker.traverse(delegator, content, fromDate, thruDate, whenMap, 0, nodeMap, startContentAssocTypeId, pickList, direction);

        results.put(x.nodeMap, nodeMap);
        results.put(x.pickList, pickList);
        return results;
    }

    /**
     * Update a ContentAssoc service. The work is done in a separate method so that complex services that need this
     * functionality do not need to incur the reflection performance penalty.
     */
    public static Map<String, Object> deactivateContentAssoc(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        ServiceContext context = new ServiceContext(UtilMisc.makeMapWritable(rcontext));
        context.put(x.entityOperation, x.UPDATE_f97c688e);
        List<String> targetOperationList = ContentWorker.prepTargetOperationList(context, x.UPDATE_f97c688e);

        List<String> contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put(x.targetOperationList, targetOperationList);
        context.put(x.contentPurposeList, contentPurposeList);
        context.put(x.skipPermissionCheck, null);

        Map<String, Object> result = deactivateContentAssocMethod(dctx, context);
        return result;
    }

    /**
     * Update a ContentAssoc method. The work is done in this separate method so that complex services that need this
     * functionality do not need to incur the reflection performance penalty.
     */
    public static Map<String, Object> deactivateContentAssocMethod(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        ServiceContext context = new ServiceContext(UtilMisc.makeMapWritable(rcontext));
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<>();
        Locale locale = (Locale) context.get(x.locale);
        context.put(x.entityOperation, x.UPDATE_f97c688e);
        List<String> targetOperationList = ContentWorker.prepTargetOperationList(context, x.UPDATE_f97c688e);

        List<String> contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put(x.targetOperationList, targetOperationList);
        context.put(x.contentPurposeList, contentPurposeList);

        GenericValue pk = delegator.makeValue(x.ContentAssoc);
        pk.setAllFields(context, false, null, Boolean.TRUE);
        pk.setAllFields(context, false, x.ca, Boolean.TRUE);

        GenericValue contentAssoc = null;
        try {
            ContentAssocDao contentAssocDao = DaoRegistry.getDao(delegator, x.ContentAssoc, ContentAssocDao.class);
            List<com.landawn.abacus.query.condition.Condition> conditions = new LinkedList<>();
            if (pk.get(x.contentId) != null) {
                conditions.add(Filters.eq(x.contentId, pk.get(x.contentId)));
            }
            if (pk.get(x.contentIdTo) != null) {
                conditions.add(Filters.eq(x.contentIdTo, pk.get(x.contentIdTo)));
            }
            if (pk.get(x.contentAssocTypeId) != null) {
                conditions.add(Filters.eq(x.contentAssocTypeId, pk.get(x.contentAssocTypeId)));
            }
            if (pk.get(x.fromDate) != null) {
                conditions.add(Filters.eq(x.fromDate, pk.get(x.fromDate)));
            }
            List<ContentAssocEntity> contentAssocEntities = conditions.isEmpty() ? new LinkedList<>()
                    : contentAssocDao.list(Filters.and(conditions));
            if (!contentAssocEntities.isEmpty()) {
                contentAssoc = delegator.makeValue(x.ContentAssoc, Beans.beanToMap(contentAssocEntities.get(0)));
            }
        } catch (Exception e) {
            Debug.logError(e, x.Entity_Error + e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentAssocRetrievingError,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }

        if (contentAssoc == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentAssocDeactivatingError, locale));
        }

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String userLoginId = (String) userLogin.get(x.userLoginId);
        String lastModifiedByUserLogin = userLoginId;
        Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();
        contentAssoc.put(x.lastModifiedByUserLogin, lastModifiedByUserLogin);
        contentAssoc.put(x.lastModifiedDate, lastModifiedDate);
        contentAssoc.put(x.thruDate, UtilDateTime.nowTimestamp());

        String permissionStatus = null;
        Map<String, Object> serviceInMap = new HashMap<>();
        serviceInMap.put(x.userLogin, context.get(x.userLogin));
        serviceInMap.put(x.targetOperationList, targetOperationList);
        serviceInMap.put(x.contentPurposeList, contentPurposeList);
        serviceInMap.put(x.entityOperation, context.get(x.entityOperation));
        serviceInMap.put(x.contentIdTo, contentAssoc.get(x.contentIdTo));
        serviceInMap.put(x.contentIdFrom, contentAssoc.get(x.contentId));

        Map<String, Object> permResults = null;
        try {
            permResults = dispatcher.runSync(x.checkAssocPermission, serviceInMap);
            if (ServiceUtil.isError(permResults)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(permResults));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_checking_permissions, x.ContentServices);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentPermissionNotGranted, locale));
        }
        permissionStatus = (String) permResults.get(x.permissionStatus);

        if (permissionStatus != null && x.granted.equals(permissionStatus)) {
            try {
                contentAssoc.store();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            String errorMsg = ContentWorker.prepPermissionErrorMsg(permResults);
            return ServiceUtil.returnError(errorMsg);
        }

        return result;
    }

    /**
     * Deactivates any active ContentAssoc (except the current one) that is associated with the passed in template/layout contentId and mapKey.
     */
    public static Map<String, Object> deactivateAssocs(DispatchContext dctx, ContentServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String contentIdTo = (String) context.get(x.contentIdTo);
        String mapKey = (String) context.get(x.mapKey);
        String contentAssocTypeId = (String) context.get(x.contentAssocTypeId);
        String activeContentId = (String) context.get(x.activeContentId);
        String contentId = (String) context.get(x.contentId);
        Timestamp fromDate = (Timestamp) context.get(x.fromDate);
        Locale locale = (Locale) context.get(x.locale);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        String sequenceNum = null;
        Map<String, Object> results = new HashMap<>();

        try {
            ContentAssocDao contentAssocDao = DaoRegistry.getDao(delegator, x.ContentAssoc, ContentAssocDao.class);
            GenericValue activeAssoc = null;
            if (fromDate != null) {
                List<ContentAssocEntity> activeAssocEntities = contentAssocDao.list(Filters.and(
                        Filters.eq(x.contentId, activeContentId),
                        Filters.eq(x.contentIdTo, contentIdTo),
                        Filters.eq(x.fromDate, fromDate),
                        Filters.eq(x.contentAssocTypeId, contentAssocTypeId)));
                if (!activeAssocEntities.isEmpty()) {
                    activeAssoc = delegator.makeValue(x.ContentAssoc, Beans.beanToMap(activeAssocEntities.get(0)));
                }
                if (activeAssoc == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentAssocNotFound,
                            UtilMisc.toMap(x.activeContentId, activeContentId, x.contentIdTo, contentIdTo, x.contentAssocTypeId, contentAssocTypeId,
                                    x.fromDate, fromDate), locale));
                }
                sequenceNum = (String) activeAssoc.get(x.sequenceNum);
            }

            List<EntityCondition> exprList = new LinkedList<>();
            exprList.add(EntityCondition.makeCondition(x.mapKey, EntityOperator.EQUALS, mapKey));
            if (sequenceNum != null) {
                exprList.add(EntityCondition.makeCondition(x.sequenceNum, EntityOperator.EQUALS, sequenceNum));
            }
            exprList.add(EntityCondition.makeCondition(x.mapKey, EntityOperator.EQUALS, mapKey));
            exprList.add(EntityCondition.makeCondition(x.thruDate, EntityOperator.EQUALS, null));
            exprList.add(EntityCondition.makeCondition(x.contentIdTo, EntityOperator.EQUALS, contentIdTo));
            exprList.add(EntityCondition.makeCondition(x.contentAssocTypeId, EntityOperator.EQUALS, contentAssocTypeId));

            if (UtilValidate.isNotEmpty(activeContentId)) {
                exprList.add(EntityCondition.makeCondition(x.contentId, EntityOperator.NOT_EQUAL, activeContentId));
            }
            if (UtilValidate.isNotEmpty(contentId)) {
                exprList.add(EntityCondition.makeCondition(x.contentId, EntityOperator.EQUALS, contentId));
            }

            EntityConditionList<EntityCondition> assocExprList = EntityCondition.makeCondition(exprList, EntityOperator.AND);
            List<com.landawn.abacus.query.condition.Condition> assocFilters = new LinkedList<>();
            assocFilters.add(Filters.eq(x.mapKey, mapKey));
            if (sequenceNum != null) {
                assocFilters.add(Filters.eq(x.sequenceNum, sequenceNum));
            }
            assocFilters.add(Filters.isNull(x.thruDate));
            assocFilters.add(Filters.eq(x.contentIdTo, contentIdTo));
            assocFilters.add(Filters.eq(x.contentAssocTypeId, contentAssocTypeId));
            if (UtilValidate.isNotEmpty(activeContentId)) {
                assocFilters.add(Filters.ne(x.contentId, activeContentId));
            }
            if (UtilValidate.isNotEmpty(contentId)) {
                assocFilters.add(Filters.eq(x.contentId, contentId));
            }
            List<ContentAssocEntity> relatedAssocEntities = contentAssocDao.list(Filters.and(assocFilters));
            List<GenericValue> relatedAssocs = new LinkedList<>();
            for (ContentAssocEntity contentAssocEntity : relatedAssocEntities) {
                relatedAssocs.add(delegator.makeValue(x.ContentAssoc, Beans.beanToMap(contentAssocEntity)));
            }
            relatedAssocs = EntityUtil.filterByDate(relatedAssocs);
            relatedAssocs = EntityUtil.orderBy(relatedAssocs, UtilMisc.toList(x.fromDate));

            for (GenericValue val : relatedAssocs) {
                val.set(x.thruDate, nowTimestamp);
                val.store();
            }
            results.put(x.deactivatedList, relatedAssocs);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        return results;
    }

    /**
     * Get and render subcontent associated with template id and mapkey. If subContentId is supplied, that content will be rendered
     * without searching for other matching content.
     */
    public static Map<String, Object> renderSubContentAsText(DispatchContext dctx, ContentServicesContext context) {
        Map<String, Object> results = new HashMap<>();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        Map<String, Object> templateContext = UtilGenerics.cast(context.get(x.templateContext));
        String contentId = (String) context.get(x.contentId);

        if (templateContext != null && UtilValidate.isEmpty(contentId)) {
            contentId = (String) templateContext.get(x.contentId);
        }
        String mapKey = (String) context.get(x.mapKey);
        if (templateContext != null && UtilValidate.isEmpty(mapKey)) {
            mapKey = (String) templateContext.get(x.mapKey);
        }
        String mimeTypeId = (String) context.get(x.mimeTypeId);
        if (templateContext != null && UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = (String) templateContext.get(x.mimeTypeId);
        }
        Locale locale = (Locale) context.get(x.locale);
        if (templateContext != null && locale == null) {
            locale = (Locale) templateContext.get(x.locale);
        }

        Writer out = (Writer) context.get(x.outWriter);
        Writer outWriter = new StringWriter();

        if (templateContext == null) {
            templateContext = new HashMap<>();
        }

        try {
            ContentWorker.renderSubContentAsText(dispatcher, contentId, outWriter, mapKey, templateContext, locale, mimeTypeId, true);
            out.write(outWriter.toString());
            results.put(x.textData, outWriter.toString());
        } catch (GeneralException | IOException e) {
            Debug.logError(e, x.Error_rendering_sub_content_text, MODULE);
            return ServiceUtil.returnError(e.toString());
        }

        return results;

    }

    /**
     * Get and render subcontent associated with template id and mapkey. If subContentId is supplied, that content will be rendered
     * without searching for other matching content.
     */
    public static Map<String, Object> renderContentAsText(DispatchContext dctx, ContentServicesContext context) {
        Map<String, Object> results = new HashMap<>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Writer out = (Writer) context.get(x.outWriter);

        Map<String, Object> templateContext = UtilGenerics.cast(context.get(x.templateContext));
        String contentId = (String) context.get(x.contentId);
        if (templateContext != null && UtilValidate.isEmpty(contentId)) {
            contentId = (String) templateContext.get(x.contentId);
        }
        String mimeTypeId = (String) context.get(x.mimeTypeId);
        if (templateContext != null && UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = (String) templateContext.get(x.mimeTypeId);
        }
        Locale locale = (Locale) context.get(x.locale);
        if (templateContext != null && locale == null) {
            locale = (Locale) templateContext.get(x.locale);
        }

        if (templateContext == null) {
            templateContext = new HashMap<>();
        }

        Writer outWriter = new StringWriter();
        GenericValue view = (GenericValue) context.get(x.subContentDataResourceView);
        if (view != null && view.containsKey(x.contentId)) {
            contentId = view.getString(x.contentId);
        }

        try {
            ContentWorker.renderContentAsText(dispatcher, contentId, outWriter, templateContext, locale, mimeTypeId, null, null, true);
            if (out != null) out.write(outWriter.toString());
            results.put(x.textData, outWriter.toString());
        } catch (GeneralException | IOException e) {
            Debug.logError(e, x.Error_rendering_sub_content_text, MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }

    public static Map<String, Object> linkContentToPubPt(DispatchContext dctx, ContentServicesContext context) {
        Map<String, Object> results = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);

        String contentId = (String) context.get(x.contentId);
        String contentIdTo = (String) context.get(x.contentIdTo);
        String contentAssocTypeId = (String) context.get(x.contentAssocTypeId);
        String statusId = (String) context.get(x.statusId);
        String privilegeEnumId = (String) context.get(x.privilegeEnumId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        if (Debug.infoOn()) {
            Debug.logInfo(x.in_publishContent_statusId + statusId, MODULE);
        }
        if (Debug.infoOn()) {
            Debug.logInfo(x.in_publishContent_userLogin + userLogin, MODULE);
        }

        Map<String, Object> mapIn = new HashMap<>();
        mapIn.put(x.contentId, contentId);
        mapIn.put(x.contentIdTo, contentIdTo);
        mapIn.put(x.contentAssocTypeId, contentAssocTypeId);
        String publish = (String) context.get(x.publish);

        try {
            boolean isPublished = false;
            GenericValue contentAssocViewFrom = ContentWorker.getContentAssocViewFrom(delegator, contentIdTo, contentId, contentAssocTypeId,
                    statusId, privilegeEnumId);
            if (contentAssocViewFrom != null) {
                isPublished = true;
            }
            if (Debug.infoOn()) {
                Debug.logInfo(x.in_publishContent_contentId + contentId + x.contentIdTo_91e92707 + contentIdTo + x.contentAssocTypeId_5c6ca894
                        + contentAssocTypeId + x.publish_17d2a345 + publish + x.isPublished + isPublished, MODULE);
            }
            if (UtilValidate.isNotEmpty(publish) && x.Y.equalsIgnoreCase(publish)) {
                ContentDao contentDao = DaoRegistry.getDao(delegator, x.Content, ContentDao.class);
                ContentEntity contentEntity = contentDao.get(contentId).orElse(null);
                GenericValue content = contentEntity == null ? null : delegator.makeValue(x.Content, Beans.beanToMap(contentEntity));
                if (content == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentNoContentFound,
                            UtilMisc.toMap(x.contentId, contentId), locale));
                }
                String contentStatusId = (String) content.get(x.statusId);
                String contentPrivilegeEnumId = (String) content.get(x.privilegeEnumId);

                if (Debug.infoOn()) {
                    Debug.logInfo(x.in_publishContent_statusId + statusId + x.contentStatusId + contentStatusId + x.privilegeEnumId_c744adb9
                            + privilegeEnumId + x.contentPrivilegeEnumId + contentPrivilegeEnumId, MODULE);
                }
                // Don't do anything if link was already there
                if (!isPublished) {
                    content.put(x.privilegeEnumId, privilegeEnumId);
                    content.put(x.statusId, statusId);
                    content.store();

                    mapIn = new HashMap<>();
                    mapIn.put(x.contentId, contentId);
                    mapIn.put(x.contentIdTo, contentIdTo);
                    mapIn.put(x.contentAssocTypeId, contentAssocTypeId);
                    mapIn.put(x.mapKey, context.get(x.mapKey));
                    mapIn.put(x.fromDate, UtilDateTime.nowTimestamp());
                    mapIn.put(x.createdDate, UtilDateTime.nowTimestamp());
                    mapIn.put(x.lastModifiedDate, UtilDateTime.nowTimestamp());
                    mapIn.put(x.createdByUserLogin, userLogin.get(x.userLoginId));
                    mapIn.put(x.lastModifiedByUserLogin, userLogin.get(x.userLoginId));
                    delegator.create(x.ContentAssoc, mapIn);
                }
            } else {
                // Only deactive if currently published
                if (isPublished) {
                    Map<String, Object> thisResults = dispatcher.runSync(x.deactivateAssocs, mapIn);
                    if (ServiceUtil.isError(thisResults)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResults));
                    }
                }
            }
        } catch (Exception e) {
            Debug.logError(e, x.Problem_getting_existing_content, x.ContentServices);
            return ServiceUtil.returnError(e.getMessage());
        }

        return results;
    }

    public static Map<String, Object> publishContent(DispatchContext dctx, ContentServicesContext context) throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        GenericValue content = (GenericValue) context.get(x.content);

        try {
            content.put(x.statusId, x.CTNT_PUBLISHED);
            content.store();
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> getPrefixedMembers(DispatchContext dctx, ContentServicesContext context) throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> mapIn = UtilGenerics.cast(context.get(x.mapIn));
        String prefix = (String) context.get(x.prefix);
        Map<String, Object> mapOut = new HashMap<>();
        result.put(x.mapOut, mapOut);
        if (mapIn != null) {
            Set<Map.Entry<String, Object>> entrySet = mapIn.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                if (key.startsWith(prefix)) {
                    Object value = entry.getValue();
                    mapOut.put(key, value);
                }
            }
        }
        return result;
    }

    public static Map<String, Object> splitString(DispatchContext dctx, ContentServicesContext context) throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        List<String> outputList = new LinkedList<>();
        String delimiter = UtilFormatOut.checkEmpty((String) context.get(x.delimiter), x.str_3eb41622);
        String inputString = (String) context.get(x.inputString);
        if (UtilValidate.isNotEmpty(inputString)) {
            outputList = StringUtil.split(inputString, delimiter);
        }
        result.put(x.outputList, outputList);
        return result;
    }

    public static Map<String, Object> joinString(DispatchContext dctx, ContentServicesContext context) throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        String outputString = null;
        String delimiter = UtilFormatOut.checkEmpty((String) context.get(x.delimiter), x.str_3eb41622);
        List<String> inputList = UtilGenerics.cast(context.get(x.inputList));
        if (inputList != null) {
            outputString = StringUtil.join(inputList, delimiter);
        }
        result.put(x.outputString, outputString);
        return result;
    }

    public static Map<String, Object> urlEncodeArgs(DispatchContext dctx, ContentServicesContext context) throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> mapFiltered = new HashMap<>();
        Map<String, Object> mapIn = UtilGenerics.cast(context.get(x.mapIn));
        if (mapIn != null) {
            Set<Map.Entry<String, Object>> entrySet = mapIn.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof String) {
                    if (UtilValidate.isNotEmpty(value)) {
                        mapFiltered.put(key, value);
                    }
                } else if (value != null) {
                    mapFiltered.put(key, value);
                }
            }
            String outputString = UtilHttp.urlEncodeArgs(mapFiltered);
            result.put(x.outputString, outputString);
        }
        return result;
    }

}
