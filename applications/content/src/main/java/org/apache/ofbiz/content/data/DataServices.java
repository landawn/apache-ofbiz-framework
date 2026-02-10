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
package org.apache.ofbiz.content.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.imaging.ImageReadException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.DataResourceDao;
import org.apache.ofbiz.persistence.dao.ElectronicTextDao;
import org.apache.ofbiz.persistence.dao.ImageDataResourceDao;
import org.apache.ofbiz.persistence.dao.StatusItemDao;
import org.apache.ofbiz.persistence.entity.DataResourceEntity;
import org.apache.ofbiz.persistence.entity.ElectronicTextEntity;
import org.apache.ofbiz.persistence.entity.ImageDataResourceEntity;
import org.apache.ofbiz.persistence.entity.StatusItemEntity;
import org.apache.ofbiz.security.SecuredUpload;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;


import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.DataServicesContext;
/**
 * DataServices Class
 */
public class DataServices {

    private static final String MODULE = DataServices.class.getName();
    private static final String RESOURCE = x.ContentUiLabels;

    public static Map<String, Object> clearAssociatedRenderCache(DispatchContext dctx, DataServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String dataResourceId = (String) context.get(x.dataResourceId);
        Locale locale = (Locale) context.get(x.locale);
        try {
            DataResourceWorker.clearAssociatedRenderCache(delegator, dataResourceId);
        } catch (GeneralException e) {
            Debug.logError(e, x.Unable_to_clear_associated_render_cache_with_dataResourceId + dataResourceId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentClearAssociatedRenderCacheError,
                    UtilMisc.toMap(x.dataResourceId, dataResourceId), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * A top-level service for creating a DataResource and ElectronicText together.
     */
    public static Map<String, Object> createDataResourceAndText(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        DataServicesContext context = new DataServicesContext(UtilMisc.makeMapWritable(rcontext));
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> thisResult = createDataResourceMethod(dctx, context);
        if (thisResult.get(ModelService.RESPONSE_MESSAGE) != null) {
            return ServiceUtil.returnError((String) thisResult.get(ModelService.ERROR_MESSAGE));
        }

        result.put(x.dataResourceId, thisResult.get(x.dataResourceId));
        context.put(x.dataResourceId, thisResult.get(x.dataResourceId));

        String dataResourceTypeId = (String) context.get(x.dataResourceTypeId);
        if (dataResourceTypeId != null && x.ELECTRONIC_TEXT.equals(dataResourceTypeId)) {
            thisResult = createElectronicText(dctx, context);
            if (thisResult.get(ModelService.RESPONSE_MESSAGE) != null) {
                return ServiceUtil.returnError((String) thisResult.get(ModelService.ERROR_MESSAGE));
            }
        }

        return result;
    }

    /**
     * A service wrapper for the createDataResourceMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> createDataResource(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = createDataResourceMethod(dctx, context);
        return result;
    }

    public static Map<String, Object> createDataResourceMethod(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        DataServicesContext context = new DataServicesContext(UtilMisc.makeMapWritable(rcontext));
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String userLoginId = (String) userLogin.get(x.userLoginId);
        String createdByUserLogin = userLoginId;
        String lastModifiedByUserLogin = userLoginId;
        Timestamp createdDate = UtilDateTime.nowTimestamp();
        Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();
        String dataTemplateTypeId = (String) context.get(x.dataTemplateTypeId);
        if (UtilValidate.isEmpty(dataTemplateTypeId)) {
            dataTemplateTypeId = x.NONE;
            context.put(x.dataTemplateTypeId, dataTemplateTypeId);
        }

        // If textData exists, then create DataResource and return dataResourceId
        String dataResourceId = (String) context.get(x.dataResourceId);
        if (UtilValidate.isEmpty(dataResourceId)) {
            dataResourceId = delegator.getNextSeqId(x.DataResource);
        }
        if (Debug.infoOn()) {
            Debug.logInfo(x.in_createDataResourceMethod_dataResourceId + dataResourceId, MODULE);
        }
        GenericValue dataResource = delegator.makeValue(x.DataResource, UtilMisc.toMap(x.dataResourceId, dataResourceId));
        dataResource.setNonPKFields(context);
        dataResource.put(x.createdByUserLogin, createdByUserLogin);
        dataResource.put(x.lastModifiedByUserLogin, lastModifiedByUserLogin);
        dataResource.put(x.createdDate, createdDate);
        dataResource.put(x.lastModifiedDate, lastModifiedDate);
        // get first statusId  for content out of the statusItem table if not provided
        if (UtilValidate.isEmpty(dataResource.get(x.statusId))) {
            try {
                StatusItemDao statusItemDao = DaoRegistry.getDao(delegator, x.StatusItem, StatusItemDao.class);
                List<GenericValue> statusItems = new LinkedList<>();
                for (StatusItemEntity statusItemEntity : statusItemDao.list(Filters.eq(x.statusTypeId, x.CONTENT_STATUS))) {
                    statusItems.add(delegator.makeValue(x.StatusItem, Beans.beanToMap(statusItemEntity)));
                }
                statusItems = EntityUtil.orderBy(statusItems, UtilMisc.toList(x.sequenceId));
                GenericValue statusItem = EntityUtil.getFirst(statusItems);
                if (statusItem != null) {
                    dataResource.put(x.statusId, statusItem.get(x.statusId));
                }
            } catch (Exception e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        try {
            dataResource.create();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        result.put(x.dataResourceId, dataResourceId);
        result.put(x.dataResource, dataResource);
        return result;
    }

    /**
     * A service wrapper for the createElectronicTextMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> createElectronicText(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = createElectronicTextMethod(dctx, context);
        return result;
    }

    public static Map<String, Object> createElectronicTextMethod(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String dataResourceId = (String) context.get(x.dataResourceId);
        String textData = (String) context.get(x.textData);
        if (UtilValidate.isNotEmpty(textData)) {
            GenericValue electronicText = delegator.makeValue(x.ElectronicText,
                    UtilMisc.toMap(x.dataResourceId, dataResourceId, x.textData, textData));
            try {
                electronicText.create();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        return result;
    }

    /**
     * A service wrapper for the createFileMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> createFile(DispatchContext dctx, DataServicesContext context) {
        return createFileMethod(dctx, context);
    }

    public static Map<String, Object> createFileNoPerm(DispatchContext dctx, Map<String, ? extends Object> rcontext) throws IOException,
            ImageReadException {
        String originalFileName = (String) rcontext.get(x.dataResourceName);
        String fileNameAndPath = (String) rcontext.get(x.objectInfo);
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) rcontext.get(x.locale);
        File file = new File(fileNameAndPath);
        if (!originalFileName.isEmpty()) {
            // Check the file name
            if (!SecuredUpload.isValidFileName(originalFileName, delegator)) {
                String errorMessage = UtilProperties.getMessage(x.SecurityUiLabels, x.SupportedFileFormatsIncludingSvg, locale);
                return ServiceUtil.returnError(errorMessage);
            }
            // TODO we could verify the file type (here "All") with dataResourceTypeId. Anyway it's done with isValidFile()
            // We would just have a better error message
            if (file.exists()) {
                // Check if a webshell is not uploaded
                if (!SecuredUpload.isValidFile(fileNameAndPath, x.All, delegator)) {
                    String errorMessage = UtilProperties.getMessage(x.SecurityUiLabels, x.SupportedFileFormatsIncludingSvg, locale);
                    return ServiceUtil.returnError(errorMessage);
                }
            }
        }

        DataServicesContext context = new DataServicesContext(UtilMisc.makeMapWritable(rcontext));
        context.put(x.skipPermissionCheck, x._true);
        return createFileMethod(dctx, context);
    }

    public static Map<String, Object> createFileMethod(DispatchContext dctx, DataServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String dataResourceTypeId = (String) context.get(x.dataResourceTypeId);
        String objectInfo = (String) context.get(x.objectInfo);
        ByteBuffer binData = (ByteBuffer) context.get(x.binData);
        String textData = (String) context.get(x.textData);
        Locale locale = (Locale) context.get(x.locale);

        // a few place holders
        String prefix = x.emptyString;
        String sep = x.emptyString;

        // extended validation for binary/character data
        if (UtilValidate.isNotEmpty(textData) && binData != null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentCannotProcessBothCharacterAndBinaryFile, locale));
        }

        // obtain a reference to the file
        File file = null;
        if (UtilValidate.isEmpty(objectInfo)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentUnableObtainReferenceToFile,
                    UtilMisc.toMap(x.objectInfo, x.emptyString), locale));
        }
        if (UtilValidate.isEmpty(dataResourceTypeId) || x.LOCAL_FILE.equals(dataResourceTypeId) || x.LOCAL_FILE_BIN.equals(dataResourceTypeId)) {
            file = new File(objectInfo);
            if (!file.isAbsolute()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentLocalFileDoesNotPointToAbsoluteLocation, locale));
            }
        } else if (x.OFBIZ_FILE.equals(dataResourceTypeId) || x.OFBIZ_FILE_BIN.equals(dataResourceTypeId)) {
            prefix = System.getProperty(x.ofbiz_home);
            if (objectInfo.indexOf('/') != 0 && prefix.lastIndexOf('/') != (prefix.length() - 1)) {
                sep = x.str_42099b4a;
            }
            file = new File(prefix + sep + objectInfo);
        } else if (x.CONTEXT_FILE.equals(dataResourceTypeId) || x.CONTEXT_FILE_BIN.equals(dataResourceTypeId)) {
            prefix = (String) context.get(x.rootDir);
            if (UtilValidate.isEmpty(prefix)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentCannotFindContextFileWithEmptyContextRoot, locale));
            }
            if (objectInfo.indexOf('/') != 0 && prefix.lastIndexOf('/') != (prefix.length() - 1)) {
                sep = x.str_42099b4a;
            }
            file = new File(prefix + sep + objectInfo);
        }
        if (file == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentUnableObtainReferenceToFile,
                    UtilMisc.toMap(x.objectInfo, objectInfo), locale));
        }

        // write the data to the file
        if (UtilValidate.isNotEmpty(textData)) {
            try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);) {
                out.write(textData);
                // Check if a webshell is not uploaded
                // TODO I believe the call below to SecuredUpload::isValidFile is now useless because of the same in createFileNoPerm
                if (!SecuredUpload.isValidFile(file.getAbsolutePath(), x.Text, delegator)) {
                    String errorMessage = UtilProperties.getMessage(x.SecurityUiLabels, x.SupportedTextFileFormats, locale);
                    return ServiceUtil.returnError(errorMessage);
                }
            } catch (IOException | ImageReadException e) {
                Debug.logWarning(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentUnableWriteCharacterDataToFile,
                        UtilMisc.toMap(x.fileName, file.getAbsolutePath()), locale));
            }
        } else if (binData != null) {
            try {
                Path tempFile = Files.createTempFile(null, null);
                Files.write(tempFile, binData.array(), StandardOpenOption.APPEND);
                // Check if a webshell is not uploaded
                // TODO I believe the call below to SecuredUpload::isValidFile is now useless because of the same in createFileNoPerm
                if (!SecuredUpload.isValidFile(tempFile.toString(), x.All, delegator)) {
                    String errorMessage = UtilProperties.getMessage(x.SecurityUiLabels, x.SupportedFileFormatsIncludingSvg, locale);
                    return ServiceUtil.returnError(errorMessage);
                }
                File tempFileToDelete = new File(tempFile.toString());
                tempFileToDelete.deleteOnExit();
                RandomAccessFile out = new RandomAccessFile(file, x.rw);
                out.write(binData.array());
                out.close();

            } catch (FileNotFoundException | ImageReadException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentUnableToOpenFileForWriting,
                        UtilMisc.toMap(x.fileName, file.getAbsolutePath()), locale));
            } catch (IOException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentUnableWriteBinaryDataToFile,
                        UtilMisc.toMap(x.fileName, file.getAbsolutePath()), locale));
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentNoContentFilePassed,
                    UtilMisc.toMap(x.fileName, file.getAbsolutePath()), locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        return result;
    }

    /**
     * A top-level service for updating a DataResource and ElectronicText together.
     */
    public static Map<String, Object> updateDataResourceAndText(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> thisResult = updateDataResourceMethod(dctx, context);
        if (thisResult.get(ModelService.RESPONSE_MESSAGE) != null) {
            return ServiceUtil.returnError((String) thisResult.get(ModelService.ERROR_MESSAGE));
        }
        String dataResourceTypeId = (String) context.get(x.dataResourceTypeId);
        if (dataResourceTypeId != null && x.ELECTRONIC_TEXT.equals(dataResourceTypeId)) {
            thisResult = updateElectronicText(dctx, context);
            if (thisResult.get(ModelService.RESPONSE_MESSAGE) != null) {
                return ServiceUtil.returnError((String) thisResult.get(ModelService.ERROR_MESSAGE));
            }
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * A service wrapper for the updateDataResourceMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> updateDataResource(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = updateDataResourceMethod(dctx, context);
        return result;
    }

    public static Map<String, Object> updateDataResourceMethod(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        GenericValue dataResource = null;
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String userLoginId = (String) userLogin.get(x.userLoginId);
        String lastModifiedByUserLogin = userLoginId;
        Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();

        // If textData exists, then create DataResource and return dataResourceId
        String dataResourceId = (String) context.get(x.dataResourceId);
        DataResourceDao dataResourceDao = DaoRegistry.getDao(delegator, x.DataResource, DataResourceDao.class);
        DataResourceEntity dataResourceEntity;
        try {
            dataResourceEntity = dataResourceDao.get(dataResourceId).orElse(null);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (dataResourceEntity == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentDataResourceNotFound,
                    UtilMisc.toMap(x.parameters_dataResourceId, dataResourceId), locale));
        }
        dataResource = delegator.makeValue(x.DataResource, Beans.beanToMap(dataResourceEntity));

        dataResource.setNonPKFields(context);
        dataResource.put(x.lastModifiedByUserLogin, lastModifiedByUserLogin);
        dataResource.put(x.lastModifiedDate, lastModifiedDate);

        try {
            dataResource.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        result.put(x.dataResource, dataResource);
        return result;
    }

    /**
     * A service wrapper for the updateElectronicTextMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> updateElectronicText(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = updateElectronicTextMethod(dctx, context);
        return result;
    }

    /**
     * Because sometimes a DataResource will exist, but no ElectronicText has been created, this method will create an
     * ElectronicText if it does not exist.
     * @param dctx the dispatch context
     * @param context the context
     * @return update the ElectronicText
     */
    public static Map<String, Object> updateElectronicTextMethod(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        GenericValue electronicText = null;
        Locale locale = (Locale) context.get(x.locale);
        String dataResourceId = (String) context.get(x.dataResourceId);
        result.put(x.dataResourceId, dataResourceId);
        String contentId = (String) context.get(x.contentId);
        result.put(x.contentId, contentId);
        if (UtilValidate.isEmpty(dataResourceId)) {
            Debug.logError(x.dataResourceId_is_null, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentDataResourceIsNull, locale));
        }
        String textData = (String) context.get(x.textData);
        if (Debug.verboseOn()) {
            Debug.logVerbose(x.in_updateElectronicText_textData + textData, MODULE);
        }
        try {
            ElectronicTextDao electronicTextDao = DaoRegistry.getDao(delegator, x.ElectronicText, ElectronicTextDao.class);
            ElectronicTextEntity electronicTextEntity = electronicTextDao.get(dataResourceId).orElse(null);
            electronicText = electronicTextEntity == null ? null : delegator.makeValue(x.ElectronicText, Beans.beanToMap(electronicTextEntity));
            if (electronicText != null) {
                electronicText.put(x.textData, textData);
                electronicText.store();
            } else {
                electronicText = delegator.makeValue(x.ElectronicText);
                electronicText.put(x.dataResourceId, dataResourceId);
                electronicText.put(x.textData, textData);
                electronicText.create();
            }
        } catch (Exception e) {
            Debug.logWarning(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentElectronicTextNotFound, locale) + x.str_b858cb28 + e.getMessage());
        }

        return result;
    }

    /**
     * A service wrapper for the updateFileMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> updateFile(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = null;
        try {
            result = updateFileMethod(dctx, context);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> updateFileMethod(DispatchContext dctx, DataServicesContext context) throws GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<>();
        Locale locale = (Locale) context.get(x.locale);
        String dataResourceTypeId = (String) context.get(x.dataResourceTypeId);
        String objectInfo = (String) context.get(x.objectInfo);
        String textData = (String) context.get(x.textData);
        ByteBuffer binData = (ByteBuffer) context.get(x.binData);
        String prefix = x.emptyString;
        File file = null;
        String fileName = x.emptyString;
        String sep = x.emptyString;
        try {
            if (UtilValidate.isEmpty(dataResourceTypeId) || dataResourceTypeId.startsWith(x.LOCAL_FILE)) {
                fileName = prefix + sep + objectInfo;
                file = new File(fileName);
                if (!file.isAbsolute()) {
                    throw new GenericServiceException(x.File + fileName + x.is_not_absolute);
                }
            } else if (dataResourceTypeId.startsWith(x.OFBIZ_FILE)) {
                prefix = System.getProperty(x.ofbiz_home);
                if (objectInfo.indexOf('/') != 0 && prefix.lastIndexOf('/') != (prefix.length() - 1)) {
                    sep = x.str_42099b4a;
                }
                file = new File(prefix + sep + objectInfo);
            } else if (dataResourceTypeId.startsWith(x.CONTEXT_FILE)) {
                prefix = (String) context.get(x.rootDir);
                if (objectInfo.indexOf('/') != 0 && prefix.lastIndexOf('/') != (prefix.length() - 1)) {
                    sep = x.str_42099b4a;
                }
                file = new File(prefix + sep + objectInfo);
            }
            if (file == null) {
                throw new IOException(x.File_is_null);
            }

            // write the data to the file
            if (UtilValidate.isNotEmpty(textData)) {
                try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);) {
                    out.write(textData);
                    // Check if a webshell is not uploaded
                    // TODO I believe the call below to SecuredUpload::isValidFile is now useless because of the same in createFileNoPerm
                    if (!SecuredUpload.isValidFile(file.getAbsolutePath(), x.Text, delegator)) {
                        String errorMessage = UtilProperties.getMessage(x.SecurityUiLabels, x.SupportedTextFileFormats, locale);
                        return ServiceUtil.returnError(errorMessage);
                    }
                } catch (IOException | ImageReadException e) {
                    Debug.logWarning(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentUnableWriteCharacterDataToFile,
                            UtilMisc.toMap(x.fileName, file.getAbsolutePath()), locale));
                }
            } else if (binData != null) {
                try {
                    // Check if a webshell is not uploaded
                    // TODO I believe the call below to SecuredUpload::isValidFile is now useless because of the same in createFileNoPerm
                    Path tempFile = Files.createTempFile(null, null);
                    Files.write(tempFile, binData.array(), StandardOpenOption.APPEND);
                    if (!SecuredUpload.isValidFile(tempFile.toString(), x.Image, delegator)) {
                        String errorMessage = UtilProperties.getMessage(x.SecurityUiLabels, x.SupportedFileFormatsIncludingSvg, locale);
                        return ServiceUtil.returnError(errorMessage);
                    }
                    File tempFileToDelete = new File(tempFile.toString());
                    tempFileToDelete.deleteOnExit();
                    RandomAccessFile out = new RandomAccessFile(file, x.rw);
                    out.setLength(binData.array().length);
                    out.write(binData.array());
                    out.close();
                } catch (FileNotFoundException | ImageReadException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentUnableToOpenFileForWriting,
                            UtilMisc.toMap(x.fileName, file.getAbsolutePath()), locale));
                } catch (IOException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentUnableWriteBinaryDataToFile,
                            UtilMisc.toMap(x.fileName, file.getAbsolutePath()), locale));
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentNoContentFilePassed,
                        UtilMisc.toMap(x.fileName, file.getAbsolutePath()), locale));
            }

        } catch (IOException e) {
            Debug.logWarning(e, MODULE);
            throw new GenericServiceException(e.getMessage());
        }

        return result;
    }

    public static Map<String, Object> renderDataResourceAsText(DispatchContext dctx, DataServicesContext context)
            throws GeneralException, IOException {
        Map<String, Object> results = new HashMap<>();
        //LocalDispatcher dispatcher = dctx.getDispatcher();
        Writer out = (Writer) context.get(x.outWriter);
        Map<String, Object> templateContext = UtilGenerics.cast(context.get(x.templateContext));
        //GenericValue userLogin = (GenericValue) context.get("userLogin");
        String dataResourceId = (String) context.get(x.dataResourceId);
        if (templateContext != null && UtilValidate.isEmpty(dataResourceId)) {
            dataResourceId = (String) templateContext.get(x.dataResourceId);
        }
        String mimeTypeId = (String) context.get(x.mimeTypeId);
        if (templateContext != null && UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = (String) templateContext.get(x.mimeTypeId);
        }

        Locale locale = (Locale) context.get(x.locale);

        if (templateContext == null) {
            templateContext = new HashMap<>();
        }

        Writer outWriter = new StringWriter();
        DataResourceWorker.renderDataResourceAsText(dctx.getDispatcher(), dataResourceId, outWriter, templateContext, locale, mimeTypeId, true);
        try {
            out.write(outWriter.toString());
            results.put(x.textData, outWriter.toString());
        } catch (IOException e) {
            Debug.logError(e, x.Error_rendering_sub_content_text, MODULE);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }

    /**
     * A service wrapper for the updateImageMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> updateImage(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = updateImageMethod(dctx, context);
        return result;
    }

    public static Map<String, Object> updateImageMethod(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        //Locale locale = (Locale) context.get("locale");
        String dataResourceId = (String) context.get(x.dataResourceId);
        ByteBuffer byteBuffer = (ByteBuffer) context.get(x.imageData);
        if (byteBuffer != null) {
            byte[] imageBytes = byteBuffer.array();
            try {
                ImageDataResourceDao imageDataResourceDao = DaoRegistry.getDao(delegator, x.ImageDataResource, ImageDataResourceDao.class);
                ImageDataResourceEntity imageDataResourceEntity = imageDataResourceDao.get(dataResourceId).orElse(null);
                GenericValue imageDataResource = imageDataResourceEntity == null ? null
                        : delegator.makeValue(x.ImageDataResource, Beans.beanToMap(imageDataResourceEntity));
                if (Debug.infoOn()) {
                    Debug.logInfo(x.imageDataResource_U + imageDataResource, MODULE);
                    Debug.logInfo(x.imageBytes_U + Arrays.toString(imageBytes), MODULE);
                }
                if (imageDataResource == null) {
                    return createImageMethod(dctx, context);
                }
                imageDataResource.setBytes(x.imageData, imageBytes);
                imageDataResource.store();
            } catch (Exception e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        return result;
    }

    /**
     * A service wrapper for the createImageMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> createImage(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = createImageMethod(dctx, context);
        return result;
    }

    public static Map<String, Object> createImageMethod(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String dataResourceId = (String) context.get(x.dataResourceId);
        ByteBuffer byteBuffer = (ByteBuffer) context.get(x.imageData);
        if (byteBuffer != null) {
            byte[] imageBytes = byteBuffer.array();
            try {
                GenericValue imageDataResource = delegator.makeValue(x.ImageDataResource, UtilMisc.toMap(x.dataResourceId, dataResourceId));
                imageDataResource.setBytes(x.imageData, imageBytes);
                if (Debug.infoOn()) {
                    Debug.logInfo(x.imageDataResource_C + imageDataResource, MODULE);
                }
                imageDataResource.create();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        return result;
    }

    /**
     * A service wrapper for the createBinaryFileMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> createBinaryFile(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = null;
        try {
            result = createBinaryFileMethod(dctx, context);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> createBinaryFileMethod(DispatchContext dctx, DataServicesContext context)
            throws GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<>();
        GenericValue dataResource = (GenericValue) context.get(x.dataResource);
        String dataResourceTypeId = (String) dataResource.get(x.dataResourceTypeId);
        String objectInfo = (String) dataResource.get(x.objectInfo);
        byte[] imageData = (byte[]) context.get(x.imageData);
        String rootDir = (String) context.get(x.rootDir);
        Locale locale = (Locale) context.get(x.locale);
        File file = null;
        if (Debug.infoOn()) {
            Debug.logInfo(x.in_createBinaryFileMethod_dataResourceTypeId + dataResourceTypeId, MODULE);
            Debug.logInfo(x.in_createBinaryFileMethod_objectInfo + objectInfo, MODULE);
            Debug.logInfo(x.in_createBinaryFileMethod_rootDir + rootDir, MODULE);
        }
        try {
            file = DataResourceWorker.getContentFile(dataResourceTypeId, objectInfo, rootDir);
        } catch (FileNotFoundException | GeneralException e) {
            Debug.logWarning(e, MODULE);
            throw new GenericServiceException(e.getMessage());
        }
        if (Debug.infoOn()) {
            Debug.logInfo(x.in_createBinaryFileMethod_file + file, MODULE);
            Debug.logInfo(x.in_createBinaryFileMethod_imageData + imageData.length, MODULE);
        }
        if (imageData != null && imageData.length > 0) {
            try (FileOutputStream out = new FileOutputStream(file);) {
                out.write(imageData);
                // Check if a webshell is not uploaded
                // TODO I believe the call below to SecuredUpload::isValidFile is now useless because of the same in createFileNoPerm
                if (!SecuredUpload.isValidFile(file.getAbsolutePath(), x.All, delegator)) {
                    String errorMessage = UtilProperties.getMessage(x.SecurityUiLabels, x.SupportedFileFormatsIncludingSvg, locale);
                    return ServiceUtil.returnError(errorMessage);
                }
                if (Debug.infoOn()) {
                    Debug.logInfo(x.in_createBinaryFileMethod_length + file.length(), MODULE);
                }
            } catch (IOException | ImageReadException e) {
                Debug.logWarning(e, MODULE);
                throw new GenericServiceException(e.getMessage());
            }
        }
        return result;
    }


    /**
     * A service wrapper for the createBinaryFileMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> updateBinaryFile(DispatchContext dctx, DataServicesContext context) {
        Map<String, Object> result = null;
        try {
            result = updateBinaryFileMethod(dctx, context);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> updateBinaryFileMethod(DispatchContext dctx, DataServicesContext context)
            throws GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<>();
        GenericValue dataResource = (GenericValue) context.get(x.dataResource);
        String dataResourceTypeId = (String) dataResource.get(x.dataResourceTypeId);
        String objectInfo = (String) dataResource.get(x.objectInfo);
        byte[] imageData = (byte[]) context.get(x.imageData);
        String rootDir = (String) context.get(x.rootDir);
        Locale locale = (Locale) context.get(x.locale);
        File file = null;
        if (Debug.infoOn()) {
            Debug.logInfo(x.in_updateBinaryFileMethod_dataResourceTypeId + dataResourceTypeId, MODULE);
            Debug.logInfo(x.in_updateBinaryFileMethod_objectInfo + objectInfo, MODULE);
            Debug.logInfo(x.in_updateBinaryFileMethod_rootDir + rootDir, MODULE);
        }
        try {
            file = DataResourceWorker.getContentFile(dataResourceTypeId, objectInfo, rootDir);
        } catch (FileNotFoundException | GeneralException e) {
            Debug.logWarning(e, MODULE);
            throw new GenericServiceException(e.getMessage());
        }
        if (Debug.infoOn()) {
            Debug.logInfo(x.in_updateBinaryFileMethod_file + file, MODULE);
            Debug.logInfo(x.in_updateBinaryFileMethod_imageData + Arrays.toString(imageData), MODULE);
        }
        if (imageData != null && imageData.length > 0) {
            try (FileOutputStream out = new FileOutputStream(file);) {
                out.write(imageData);
                // Check if a webshell is not uploaded
                // TODO I believe the call below to SecuredUpload::isValidFile is now useless because of the same in createFileNoPerm
                if (!SecuredUpload.isValidFile(file.getAbsolutePath(), x.All, delegator)) {
                    String errorMessage = UtilProperties.getMessage(x.SecurityUiLabels, x.SupportedFileFormatsIncludingSvg, locale);
                    return ServiceUtil.returnError(errorMessage);
                }

                if (Debug.infoOn()) {
                    Debug.logInfo(x.in_updateBinaryFileMethod_length + file.length(), MODULE);
                }
            } catch (IOException | ImageReadException e) {
                Debug.logWarning(e, MODULE);
                throw new GenericServiceException(e.getMessage());
            }
        }
        return result;
    }
}
