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
package org.apache.ofbiz.content.layout;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.ContentManagementWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMapProcessor;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * LayoutEvents Class
 */
public class LayoutEvents {

    private static final String MODULE = LayoutEvents.class.getName();
    private static final String ERR_RESOURCE = "ContentErrorUiLabels";

    public static String createLayoutImage(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);

        try {
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
            HttpSession session = request.getSession();
            Map<String, Object> uploadResults = LayoutWorker.uploadImageAndParameters(request, "imageData");
            Map<String, Object> formInput = UtilGenerics.cast(uploadResults.get("formInput"));
            Map<String, Object> context = new HashMap<>();
            ByteBuffer byteWrap = (ByteBuffer) uploadResults.get("imageData");
            if (byteWrap == null) {
                String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "layoutEvents.image_data_null", locale);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            String imageFileName = (String) uploadResults.get("imageFileName");
            String imageFileNameExt = null;
            if (UtilValidate.isNotEmpty(imageFileName)) {
                int pos = imageFileName.lastIndexOf('.');
                if (pos >= 0) {
                    imageFileNameExt = imageFileName.substring(pos + 1);
                }
            }
            String mimeTypeId = "image/" + imageFileNameExt;
            List<Object> errorMessages = new LinkedList<>();
            if (locale == null) {
                locale = Locale.getDefault();
            }
            context.put(org.apache.ofbiz.persistence.entity.x.locale, locale);

            try {
                SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                        "contentIn", formInput, context, errorMessages, locale);
                SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                        "dataResourceIn", formInput, context, errorMessages, locale);
                SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                        "contentAssocIn", formInput, context, errorMessages, locale);
            } catch (MiniLangException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }

            context.put(org.apache.ofbiz.persistence.entity.x.dataResourceName, context.get(org.apache.ofbiz.persistence.entity.x.contentName));
            context.put(org.apache.ofbiz.persistence.entity.x.userLogin, session.getAttribute("userLogin"));
            context.put(org.apache.ofbiz.persistence.entity.x.dataResourceTypeId, "IMAGE_OBJECT");
            context.put(org.apache.ofbiz.persistence.entity.x.contentAssocTypeId, "SUB_CONTENT");
            context.put(org.apache.ofbiz.persistence.entity.x.contentTypeId, "DOCUMENT");
            context.put(org.apache.ofbiz.persistence.entity.x.contentIdTo, formInput.get("contentIdTo"));
            context.put(org.apache.ofbiz.persistence.entity.x.textData, formInput.get("textData"));
            String contentPurposeTypeId = (String) formInput.get("contentPurposeTypeId");
            if (UtilValidate.isNotEmpty(contentPurposeTypeId)) {
                context.put(org.apache.ofbiz.persistence.entity.x.contentPurposeList, UtilMisc.toList(contentPurposeTypeId));
            }

            Map<String, Object> result = dispatcher.runSync("persistContentAndAssoc", context);
            if (ServiceUtil.isError(result)) {
                String errorMessage = ServiceUtil.getErrorMessage(result);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                return "error";
            }

            String dataResourceId = (String) result.get("dataResourceId");
            String activeContentId = (String) result.get("contentId");
            if (UtilValidate.isNotEmpty(activeContentId)) {
                Map<String, Object> context2 = new HashMap<>();
                context2.put("activeContentId", activeContentId);
                context2.put("contentAssocTypeId", result.get("contentAssocTypeId"));
                context2.put("fromDate", result.get("fromDate"));

                request.setAttribute("contentId", result.get("contentId"));
                request.setAttribute("drDataResourceId", dataResourceId);
                request.setAttribute("currentEntityName", "SubContentDataResourceId");

                context2.put("contentIdTo", formInput.get("contentIdTo"));
                context2.put("mapKey", formInput.get("mapKey"));
                Map<String, Object> serviceResult = new HashMap<>();
                serviceResult = dispatcher.runSync("deactivateAssocs", context2);
                if (ServiceUtil.isError(serviceResult)) {
                    String errorMessage = ServiceUtil.getErrorMessage(serviceResult);
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                    Debug.logError(errorMessage, MODULE);
                    return "error";
                }
            }

            GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).queryOne();
            // Use objectInfo field to store the name of the file, since there is no place in ImageDataResource for it.
            if (dataResource != null) {
                dataResource.set(org.apache.ofbiz.persistence.entity.x.objectInfo, imageFileName);
                dataResource.set(org.apache.ofbiz.persistence.entity.x.mimeTypeId, mimeTypeId);
                dataResource.store();
            }

            // See if this needs to be a create or an update procedure
            GenericValue imageDataResource = EntityQuery.use(delegator).from("ImageDataResource").where("dataResourceId", dataResourceId).queryOne();
            if (imageDataResource == null) {
                imageDataResource = delegator.makeValue("ImageDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                imageDataResource.set(org.apache.ofbiz.persistence.entity.x.imageData, byteWrap.array());
                imageDataResource.create();
            } else {
                imageDataResource.set(org.apache.ofbiz.persistence.entity.x.imageData, byteWrap.array());
                imageDataResource.store();
            }
        } catch (GenericEntityException | GenericServiceException e3) {
            request.setAttribute("_ERROR_MESSAGE_", e3.getMessage());
            return "error";
        }
        return "success";
    }

    public static String updateLayoutImage(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        try {
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            HttpSession session = request.getSession();
            Map<String, Object> uploadResults = LayoutWorker.uploadImageAndParameters(request, "imageData");
            Map<String, Object> context = UtilGenerics.cast(uploadResults.get("formInput"));
            ByteBuffer byteWrap = (ByteBuffer) uploadResults.get("imageData");
            if (byteWrap == null) {
                String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "layoutEvents.image_data_null", locale);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            String imageFileName = (String) uploadResults.get("imageFileName");
            if (Debug.verboseOn()) {
                Debug.logVerbose("in createLayoutImage(java), context:" + context, "");
            }
            context.put(org.apache.ofbiz.persistence.entity.x.userLogin, session.getAttribute("userLogin"));
            context.put(org.apache.ofbiz.persistence.entity.x.dataResourceTypeId, "IMAGE_OBJECT");
            context.put(org.apache.ofbiz.persistence.entity.x.contentAssocTypeId, "SUB_CONTENT");
            context.put(org.apache.ofbiz.persistence.entity.x.contentTypeId, "DOCUMENT");
            context.put(org.apache.ofbiz.persistence.entity.x.mimeType, context.get(org.apache.ofbiz.persistence.entity.x.drMimeType));
            context.put(org.apache.ofbiz.persistence.entity.x.drMimeType, null);
            context.put(org.apache.ofbiz.persistence.entity.x.objectInfo, context.get(org.apache.ofbiz.persistence.entity.x.drobjectInfo));
            context.put(org.apache.ofbiz.persistence.entity.x.drObjectInfo, null);
            context.put(org.apache.ofbiz.persistence.entity.x.drDataResourceTypeId, null);

            String dataResourceId = (String) context.get(org.apache.ofbiz.persistence.entity.x.drDataResourceId);
            if (Debug.verboseOn()) {
                Debug.logVerbose("in createLayoutImage(java), dataResourceId:" + dataResourceId, "");
            }

            GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).queryOne();
            if (Debug.verboseOn()) {
                Debug.logVerbose("in createLayoutImage(java), dataResource:" + dataResource, "");
            }
            // Use objectInfo field to store the name of the file, since there is no
            // place in ImageDataResource for it.
            if (Debug.verboseOn()) {
                Debug.logVerbose("in createLayoutImage(java), imageFileName:" + imageFileName, "");
            }
            if (dataResource != null) {
                dataResource.setNonPKFields(context);
                dataResource.store();
            }

            // See if this needs to be a create or an update procedure
            GenericValue imageDataResource = EntityQuery.use(delegator).from("ImageDataResource").where("dataResourceId", dataResourceId).queryOne();
            if (imageDataResource == null) {
                imageDataResource = delegator.makeValue("ImageDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                imageDataResource.set(org.apache.ofbiz.persistence.entity.x.imageData, byteWrap.array());
                imageDataResource.create();
            } else {
                imageDataResource.set(org.apache.ofbiz.persistence.entity.x.imageData, byteWrap.array());
                imageDataResource.store();
            }
        } catch (GenericEntityException e3) {
            request.setAttribute("_ERROR_MESSAGE_", e3.getMessage());
            return "error";
        }
        return "success";
    }

    public static String replaceSubContent(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        Locale locale = UtilHttp.getLocale(request);
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        if (Debug.verboseOn()) {
            Debug.logVerbose("in replaceSubContent, paramMap:" + paramMap, MODULE);
        }
        String dataResourceId = (String) paramMap.get("dataResourceId");
        if (UtilValidate.isEmpty(dataResourceId)) {
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "layoutEvents.data_ressource_id_null", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        String contentIdTo = (String) paramMap.get("contentIdTo");
        if (UtilValidate.isEmpty(contentIdTo)) {
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "layoutEvents.content_id_to_null", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        String mapKey = (String) paramMap.get("mapKey");

        context.put(org.apache.ofbiz.persistence.entity.x.dataResourceId, dataResourceId);
        String contentId = (String) paramMap.get("contentId");
        context.put(org.apache.ofbiz.persistence.entity.x.userLogin, session.getAttribute("userLogin"));

        if (UtilValidate.isNotEmpty(contentId)) {
            context.put(org.apache.ofbiz.persistence.entity.x.contentId, contentId);
            context.put(org.apache.ofbiz.persistence.entity.x.contentIdTo, contentIdTo);
            context.put(org.apache.ofbiz.persistence.entity.x.mapKey, mapKey);
            context.put(org.apache.ofbiz.persistence.entity.x.contentAssocTypeId, "SUB_CONTENT");

            try {
                Map<String, Object> result = dispatcher.runSync("persistContentAndAssoc", context);
                if (ServiceUtil.isError(result)) {
                    String errorMessage = ServiceUtil.getErrorMessage(result);
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                    Debug.logError(errorMessage, MODULE);
                    return "error";
                }
                request.setAttribute("contentId", contentIdTo);
                Map<String, Object> context2 = new HashMap<>();
                context2.put("activeContentId", contentId);
                context2.put("contentAssocTypeId", "SUB_CONTENT");
                context2.put("fromDate", result.get("fromDate"));

                request.setAttribute("drDataResourceId", null);
                request.setAttribute("currentEntityName", "ContentDataResourceView");

                context2.put("contentIdTo", contentIdTo);
                context2.put("mapKey", mapKey);
                Map<String, Object> serviceResult = new HashMap<>();
                serviceResult = dispatcher.runSync("deactivateAssocs", context2);
                if (ServiceUtil.isError(serviceResult)) {
                    String errorMessage = ServiceUtil.getErrorMessage(serviceResult);
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                    Debug.logError(errorMessage, MODULE);
                    return "error";
                }
            } catch (GenericServiceException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }
        }

        return "success";
    }

    public static String cloneLayout(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        Locale locale = UtilHttp.getLocale(request);
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        String contentId = (String) paramMap.get("contentId");
        if (Debug.verboseOn()) {
            Debug.logVerbose("in cloneLayout, contentId:" + contentId, "");
        }
        if (UtilValidate.isEmpty(contentId)) {
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "layoutEvents.content_id_empty", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        String contentIdTo = (String) paramMap.get("contentIdTo");
        if (Debug.verboseOn()) {
            Debug.logVerbose("in cloneLayout, contentIdTo:" + contentIdTo, "");
        }
        GenericValue content = null;
        GenericValue newContent = null;
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String userLoginId = (String) userLogin.get(org.apache.ofbiz.persistence.entity.x.userLoginId);
        List<GenericValue> entityList = null;
        String newId = null;
        String newDataResourceId = null;
        try {
            content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            if (Debug.verboseOn()) {
                Debug.logVerbose("in cloneLayout, content:" + content, "");
            }
            if (content == null) {
                String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "layoutEvents.content_empty", locale);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            newContent = delegator.makeValue("Content", content);
            if (Debug.verboseOn()) {
                Debug.logVerbose("in cloneLayout, newContent:" + newContent, "");
            }
            String oldName = (String) content.get(org.apache.ofbiz.persistence.entity.x.contentName);
            newId = delegator.getNextSeqId("Content");
            newContent.set(org.apache.ofbiz.persistence.entity.x.contentId, newId);
            String dataResourceId = (String) content.get(org.apache.ofbiz.persistence.entity.x.dataResourceId);
            GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).queryOne();
            if (dataResource != null) {
                GenericValue newDataResource = delegator.makeValue("DataResource", dataResource);
                if (Debug.verboseOn()) {
                    Debug.logVerbose("in cloneLayout, newDataResource:" + newDataResource, "");
                }
                String dataResourceName = "Copy:" + (String) dataResource.get(org.apache.ofbiz.persistence.entity.x.dataResourceName);
                newDataResource.set(org.apache.ofbiz.persistence.entity.x.dataResourceName, dataResourceName);
                newDataResourceId = delegator.getNextSeqId("DataResource");
                newDataResource.set(org.apache.ofbiz.persistence.entity.x.dataResourceId, newDataResourceId);
                newDataResource.set(org.apache.ofbiz.persistence.entity.x.createdDate, UtilDateTime.nowTimestamp());
                newDataResource.set(org.apache.ofbiz.persistence.entity.x.lastModifiedDate, UtilDateTime.nowTimestamp());
                newDataResource.set(org.apache.ofbiz.persistence.entity.x.createdByUserLogin, userLoginId);
                newDataResource.set(org.apache.ofbiz.persistence.entity.x.lastModifiedByUserLogin, userLoginId);
                newDataResource.create();
            }
            newContent.set(org.apache.ofbiz.persistence.entity.x.contentName, "Copy - " + oldName);
            newContent.set(org.apache.ofbiz.persistence.entity.x.createdDate, UtilDateTime.nowTimestamp());
            newContent.set(org.apache.ofbiz.persistence.entity.x.lastModifiedDate, UtilDateTime.nowTimestamp());
            newContent.set(org.apache.ofbiz.persistence.entity.x.createdByUserLogin, userLoginId);
            newContent.set(org.apache.ofbiz.persistence.entity.x.lastModifiedByUserLogin, userLoginId);
            newContent.create();
            if (Debug.verboseOn()) {
                Debug.logVerbose("in cloneLayout, newContent:" + newContent, "");
            }

            GenericValue newContentAssoc = delegator.makeValue("ContentAssoc");
            newContentAssoc.set(org.apache.ofbiz.persistence.entity.x.contentId, newId);
            newContentAssoc.set(org.apache.ofbiz.persistence.entity.x.contentIdTo, "TEMPLATE_MASTER");
            newContentAssoc.set(org.apache.ofbiz.persistence.entity.x.contentAssocTypeId, "SUB_CONTENT");
            newContentAssoc.set(org.apache.ofbiz.persistence.entity.x.fromDate, UtilDateTime.nowTimestamp());
            newContentAssoc.create();
            if (Debug.verboseOn()) {
                Debug.logVerbose("in cloneLayout, newContentAssoc:" + newContentAssoc, "");
            }
        } catch (GenericEntityException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        Map<String, Object> serviceIn = new HashMap<>();
        Map<String, Object> results = null;
        serviceIn.put("fromDate", UtilDateTime.nowTimestamp());
        serviceIn.put("contentId", contentId);
        serviceIn.put("userLogin", session.getAttribute("userLogin"));
        serviceIn.put("direction", "From");
        serviceIn.put("thruDate", null);
        serviceIn.put("assocTypes", UtilMisc.toList("SUB_CONTENT"));
        try {
            results = dispatcher.runSync("getAssocAndContentAndDataResource", serviceIn);
            if (ServiceUtil.isError(results)) {
                String errorMessage = ServiceUtil.getErrorMessage(results);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                return "error";
            }
            entityList = UtilGenerics.cast(results.get("entityList"));
            if (UtilValidate.isEmpty(entityList)) {
                String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "layoutEvents.no_subcontent", locale);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
            }
        } catch (GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }

        serviceIn = new HashMap<>();
        serviceIn.put("userLogin", session.getAttribute("userLogin"));

        // Can't count on records being unique
        Map<String, GenericValue> beenThere = new HashMap<>();
        for (GenericValue view : entityList) {
            List<Object> errorMessages = new LinkedList<>();
            if (locale == null) {
                locale = Locale.getDefault();
            }
            try {
                SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml", "contentAssocIn",
                        view, serviceIn, errorMessages, locale);
            } catch (IllegalArgumentException | MiniLangException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }
            String contentIdFrom = (String) view.get(org.apache.ofbiz.persistence.entity.x.contentId);
            String mapKey = (String) view.get(org.apache.ofbiz.persistence.entity.x.caMapKey);
            Timestamp fromDate = (Timestamp) view.get(org.apache.ofbiz.persistence.entity.x.caFromDate);
            Timestamp thruDate = (Timestamp) view.get(org.apache.ofbiz.persistence.entity.x.caThruDate);
            if (Debug.verboseOn()) {
                Debug.logVerbose("in cloneLayout, contentIdFrom:" + contentIdFrom + " fromDate:" + fromDate + " thruDate:" + thruDate
                        + " mapKey:" + mapKey, "");
            }
            if (beenThere.get(contentIdFrom) == null) {
                serviceIn.put("contentIdFrom", contentIdFrom);
                serviceIn.put("contentIdTo", newId);
                serviceIn.put("fromDate", UtilDateTime.nowTimestamp());
                serviceIn.put("thruDate", null);
                try {
                    results = dispatcher.runSync("persistContentAndAssoc", serviceIn);
                    if (ServiceUtil.isError(results)) {
                        String errorMessage = ServiceUtil.getErrorMessage(results);
                        request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                        Debug.logError(errorMessage, MODULE);
                        return "error";
                    }
                } catch (GenericServiceException e) {
                    request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                    return "error";
                }
                beenThere.put(contentIdFrom, view);
            }
        }

        GenericValue view = delegator.makeValue("ContentDataResourceView");
        view.set(org.apache.ofbiz.persistence.entity.x.contentId, newId);
        view.set(org.apache.ofbiz.persistence.entity.x.drDataResourceId, newDataResourceId);
        if (Debug.verboseOn()) {
            Debug.logVerbose("in cloneLayout, view:" + view, "");
        }
        ContentManagementWorker.setCurrentEntityMap(request, view);
        request.setAttribute("contentId", view.get(org.apache.ofbiz.persistence.entity.x.contentId));
        request.setAttribute("drDataResourceId", view.get(org.apache.ofbiz.persistence.entity.x.drDataResourceId));
        return "success";
    }

    public static String createLayoutSubContent(HttpServletRequest request, HttpServletResponse response) {
        try {
            LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
            HttpSession session = request.getSession();
            Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
            String contentIdTo = (String) paramMap.get("contentIdTo");
            String mapKey = (String) paramMap.get("mapKey");
            if (Debug.verboseOn()) {
                Debug.logVerbose("in createSubContent, contentIdTo:" + contentIdTo, MODULE);
                Debug.logVerbose("in createSubContent, mapKey:" + mapKey, MODULE);
            }
            Map<String, Object> context = new HashMap<>();
            List<Object> errorMessages = new LinkedList<>();
            Locale loc = (Locale) request.getSession().getServletContext().getAttribute("locale");
            if (loc == null) {
                loc = Locale.getDefault();
            }
            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
            context.put(org.apache.ofbiz.persistence.entity.x.userLogin, userLogin);

            String rootDir = request.getSession().getServletContext().getRealPath("/");
            context.put(org.apache.ofbiz.persistence.entity.x.rootDir, rootDir);
            try {
                SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                        "contentIn", paramMap, context, errorMessages, loc);
                SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                        "dataResourceIn", paramMap, context, errorMessages, loc);
                SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                        "contentAssocIn", paramMap, context, errorMessages, loc);
            } catch (MiniLangException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }

            context.put(org.apache.ofbiz.persistence.entity.x.dataResourceName, context.get(org.apache.ofbiz.persistence.entity.x.contentName));
            String contentPurposeTypeId = (String) paramMap.get("contentPurposeTypeId");
            if (UtilValidate.isNotEmpty(contentPurposeTypeId)) {
                context.put(org.apache.ofbiz.persistence.entity.x.contentPurposeList, UtilMisc.toList(contentPurposeTypeId));
            }
            context.put(org.apache.ofbiz.persistence.entity.x.contentIdTo, paramMap.get("contentIdTo"));
            context.put(org.apache.ofbiz.persistence.entity.x.mapKey, paramMap.get("mapKey"));
            context.put(org.apache.ofbiz.persistence.entity.x.textData, paramMap.get("textData"));
            context.put(org.apache.ofbiz.persistence.entity.x.contentAssocTypeId, "SUB_CONTENT");
            if (Debug.verboseOn()) {
                Debug.logVerbose("in createSubContent, context:" + context, MODULE);
            }
            Map<String, Object> result = dispatcher.runSync("persistContentAndAssoc", context);
            if (ServiceUtil.isError(result)) {
                String errorMessage = ServiceUtil.getErrorMessage(result);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                return "error";
            }

            if (Debug.verboseOn()) {
                Debug.logVerbose("in createLayoutFile, result:" + result, MODULE);
            }
            String contentId = (String) result.get("contentId");
            String dataResourceId = (String) result.get("dataResourceId");
            request.setAttribute("contentId", contentId);
            request.setAttribute("drDataResourceId", dataResourceId);
            request.setAttribute("currentEntityName", "SubContentDataResourceId");
            Map<String, Object> context2 = new HashMap<>();
            context2.put("activeContentId", contentId);
            context2.put("contentAssocTypeId", "SUB_CONTENT");
            context2.put("fromDate", result.get("fromDate"));
            context2.put("contentIdTo", contentIdTo);
            context2.put("mapKey", mapKey);
            context2.put("userLogin", userLogin);
            Map<String, Object> serviceResult = new HashMap<>();
            serviceResult = dispatcher.runSync("deactivateAssocs", context2);
            if (ServiceUtil.isError(serviceResult)) {
                String errorMessage = ServiceUtil.getErrorMessage(serviceResult);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                return "error";
            }
        } catch (GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        return "success";
    }

    public static String updateLayoutSubContent(HttpServletRequest request, HttpServletResponse response) {
        try {
            LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
            HttpSession session = request.getSession();
            Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
            Map<String, Object> context = new HashMap<>();
            List<Object> errorMessages = new LinkedList<>();
            Locale loc = (Locale) request.getSession().getServletContext().getAttribute("locale");
            if (loc == null) {
                loc = Locale.getDefault();
            }
            context.put(org.apache.ofbiz.persistence.entity.x.locale, loc);
            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
            context.put(org.apache.ofbiz.persistence.entity.x.userLogin, userLogin);

            String rootDir = request.getSession().getServletContext().getRealPath("/");
            context.put(org.apache.ofbiz.persistence.entity.x.rootDir, rootDir);
            try {
                SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                        "contentIn", paramMap, context, errorMessages, loc);
                SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                        "dataResourceIn", paramMap, context, errorMessages, loc);
                SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                        "contentAssocIn", paramMap, context, errorMessages, loc);
            } catch (MiniLangException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }

            context.put(org.apache.ofbiz.persistence.entity.x.dataResourceName, context.get(org.apache.ofbiz.persistence.entity.x.contentName));
            String contentPurposeTypeId = (String) paramMap.get("contentPurposeTypeId");
            if (UtilValidate.isNotEmpty(contentPurposeTypeId)) {
                context.put(org.apache.ofbiz.persistence.entity.x.contentPurposeList, UtilMisc.toList(contentPurposeTypeId));
            }
            context.put(org.apache.ofbiz.persistence.entity.x.contentIdTo, paramMap.get("contentIdTo"));
            context.put(org.apache.ofbiz.persistence.entity.x.textData, paramMap.get("textData"));
            context.put(org.apache.ofbiz.persistence.entity.x.contentAssocTypeId, null);
            Map<String, Object> result = dispatcher.runSync("persistContentAndAssoc", context);
            if (ServiceUtil.isError(result)) {
                String errorMessage = ServiceUtil.getErrorMessage(result);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                return "error";
            }
            String contentId = (String) result.get("contentId");
            String dataResourceId = (String) result.get("dataResourceId");
            request.setAttribute("contentId", contentId);
            request.setAttribute("drDataResourceId", dataResourceId);
            request.setAttribute("currentEntityName", "SubContentDataResourceId");
        } catch (GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        return "success";
    }

    public static String copyToClip(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        String entityName = (String) paramMap.get("entityName");
        Locale locale = UtilHttp.getLocale(request);

        if (UtilValidate.isEmpty(entityName)) {
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "layoutEvents.entityname_empty", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        GenericValue v = delegator.makeValue(entityName);
        GenericPK passedPK = v.getPrimaryKey();
        Collection<String> keyColl = passedPK.getAllKeys();
        for (String attrName : keyColl) {
            String attrVal = (String) request.getAttribute(attrName);
            if (attrVal == null) {
                attrVal = (String) paramMap.get(attrName);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("in copyToClip, attrName:" + attrName, "");
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("in copyToClip, attrVal:" + attrVal, "");
            }
            if (UtilValidate.isNotEmpty(attrVal)) {
                passedPK.put(attrName, attrVal);
            } else {
                String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "layoutEvents.empty", locale);
                request.setAttribute("_ERROR_MESSAGE_", attrName + " " + errMsg);
                return "error";
            }
        }
        ContentManagementWorker.mruAdd(request, passedPK);

        return "success";
    }
}
