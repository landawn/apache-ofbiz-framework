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
package org.apache.ofbiz.product.imagemanagement;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.common.image.ImageTransform;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.ContentAssocDao;
import org.apache.ofbiz.persistence.dao.ContentDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.FileExtensionDao;
import org.apache.ofbiz.persistence.dao.ProductContentDao;
import org.apache.ofbiz.persistence.entity.ContentAssocEntity;
import org.apache.ofbiz.persistence.entity.ContentEntity;
import org.apache.ofbiz.persistence.entity.FileExtensionEntity;
import org.apache.ofbiz.persistence.entity.ProductContentEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.jdom2.JDOMException;


import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ImageManagementServicesContext;
/**
 * Product Services
 */
public class ImageManagementServices {

    private static final String MODULE = ImageManagementServices.class.getName();
    private static final String RES_ERROR = x.ProductErrorUiLabels;
    private static final String RESOURCE = x.ProductUiLabels;
    private static int imageCount = 0;
    private static String imagePath;

    public static Map<String, Object> addMultipleuploadForProduct(DispatchContext dctx,
            ImageManagementServicesContext context) throws ImageReadException {

        Map<String, Object> result = new HashMap<>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productId = (String) context.get(x.productId);
        productId = productId.trim();
        String productContentTypeId = (String) context.get(x.productContentTypeId);
        ByteBuffer imageData = (ByteBuffer) context.get(x.uploadedFile);
        String uploadFileName = (String) context.get(x._uploadedFile_fileName);
        String imageResize = (String) context.get(x.imageResize);
        Locale locale = (Locale) context.get(x.locale);

        if (UtilValidate.isNotEmpty(uploadFileName)) {
            Debug.logInfo(x.This_is_about_file + uploadFileName + x.str_67b192f2, MODULE);
            String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                    x.image_management_path, delegator), context);
            String imageServerUrl = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                    x.image_management_url, delegator), context);
            String rootTargetDirectory = imageServerPath;
            File rootTargetDir = new File(rootTargetDirectory);
            if (!rootTargetDir.exists()) {
                boolean created = rootTargetDir.mkdirs();
                if (!created) {
                    String errMsg = UtilProperties.getMessage(RES_ERROR, x.ProductCannotCreateTheTargetDirectory, locale);
                    Debug.logFatal(errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                }
            }

            String sizeType = null;
            if (UtilValidate.isNotEmpty(imageResize)) {
                sizeType = imageResize;
            }

            Map<String, Object> contentCtx = new HashMap<>();
            contentCtx.put(x.contentTypeId, x.DOCUMENT);
            contentCtx.put(x.userLogin, userLogin);
            Map<String, Object> contentResult;
            try {
                contentResult = dispatcher.runSync(x.createContent, contentCtx);
                if (ServiceUtil.isError(contentResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(contentResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            String contentId = (String) contentResult.get(x.contentId);
            result.put(x.contentFrameId, contentId);
            result.put(x.contentId, contentId);

            String fileContentType = (String) context.get(x._uploadedFile_contentType);
            if (x.image_pjpeg.equals(fileContentType)) {
                fileContentType = x.image_jpeg;
            } else if (x.image_x_png.equals(fileContentType)) {
                fileContentType = x.image_png;
            }

            // Create folder product id.
            String targetDirectory = imageServerPath + x.str_42099b4a + productId;
            File targetDir = new File(targetDirectory);
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    String errMsg = x.Cannot_create_the_target_directory;
                    Debug.logFatal(errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                }
            }

            String fileToCheck = imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + uploadFileName;
            File file = new File(fileToCheck);
            String imageName = null;
            imagePath = imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + uploadFileName;
            file = checkExistsImage(file);
            if (UtilValidate.isNotEmpty(file)) {
                imageName = file.getPath();
                imageName = imageName.substring(imageName.lastIndexOf(File.separator) + 1);
            } else {
                imageName = x.emptyString;
            }

            if (UtilValidate.isEmpty(imageResize)) {
                try {
                    Path tempFile = Files.createTempFile(null, null);
                    Files.write(tempFile, imageData.array(), StandardOpenOption.APPEND);
                    // Check if a webshell is not uploaded
                    if (!org.apache.ofbiz.security.SecuredUpload.isValidFile(tempFile.toString(), x.Image, delegator)) {
                        String errorMessage = UtilProperties.getMessage(x.SecurityUiLabels, x.SupportedImageFormats, locale);
                        return ServiceUtil.returnError(errorMessage);
                    }
                    File tempFileToDelete = new File(tempFile.toString());
                    tempFileToDelete.deleteOnExit();
                    // Create image file original to folder product id.
                    RandomAccessFile out = new RandomAccessFile(file, x.rw);
                    out.write(imageData.array());
                    out.close();
                } catch (FileNotFoundException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.ProductImageViewUnableWriteFile, UtilMisc.toMap(x.fileName, file.getAbsolutePath()), locale));
                } catch (IOException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.ProductImageViewUnableWriteBinaryData, UtilMisc.toMap(x.fileName, file.getAbsolutePath()), locale));
                }
            } else { // Scale Image in different sizes
                fileToCheck = imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + imageName;
                File fileOriginal = new File(fileToCheck);
                fileOriginal = checkExistsImage(fileOriginal);

                try {
                    Path tempFile = Files.createTempFile(null, null);
                    Files.write(tempFile, imageData.array(), StandardOpenOption.APPEND);
                    // Check if a webshell is not uploaded
                    if (!org.apache.ofbiz.security.SecuredUpload.isValidFile(tempFile.toString(), x.Image, delegator)) {
                        String errorMessage = UtilProperties.getMessage(x.SecurityUiLabels, x.SupportedImageFormats, locale);
                        return ServiceUtil.returnError(errorMessage);
                    }
                    File tempFileToDelete = new File(tempFile.toString());
                    tempFileToDelete.deleteOnExit();
                    RandomAccessFile outFile = new RandomAccessFile(fileOriginal, x.rw);
                    outFile.write(imageData.array());
                    outFile.close();
                } catch (FileNotFoundException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.ProductImageViewUnableWriteFile, UtilMisc.toMap(x.fileName, fileOriginal.getAbsolutePath()), locale));
                } catch (IOException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.ProductImageViewUnableWriteBinaryData, UtilMisc.toMap(x.fileName, fileOriginal.getAbsolutePath()), locale));
                }

                Map<String, Object> resultResize = new HashMap<>();
                try {
                    resultResize.putAll(scaleImageMangementInAllSize(dctx, context, imageName, sizeType, productId));
                } catch (IOException e) {
                    String errMsg = UtilProperties.getMessage(RES_ERROR,
                            x.ProductScaleAdditionalImageInAllDifferentSizesIsImpossible, UtilMisc.toMap(x.errorString, e.toString()), locale);
                    Debug.logError(e, errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                } catch (JDOMException e) {
                    String errMsg = UtilProperties.getMessage(RES_ERROR,
                            x.ProductErrorsOccurInParsingImageProperties_xml, UtilMisc.toMap(x.errorString, e .toString()), locale);
                    Debug.logError(e, errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                }
            }

            Map<String, Object> contentThumbnail = createContentThumbnail(dctx, context, userLogin, imageData, productId, imageName);
            String filenameToUseThumb = (String) contentThumbnail.get(x.filenameToUseThumb);
            String contentIdThumb = (String) contentThumbnail.get(x.contentIdThumb);

            String imageUrl = imageServerUrl + x.str_42099b4a + productId + x.str_42099b4a + imageName;
            String imageUrlThumb = imageServerUrl + x.str_42099b4a + productId + x.str_42099b4a + filenameToUseThumb;

            createContentAndDataResource(dctx, userLogin, imageName, imageUrl, contentId, fileContentType);
            createContentAndDataResource(dctx, userLogin, filenameToUseThumb, imageUrlThumb, contentIdThumb, fileContentType);

            Map<String, Object> createContentAssocMap = new HashMap<>();
            createContentAssocMap.put(x.contentAssocTypeId, x.IMAGE_THUMBNAIL);
            createContentAssocMap.put(x.contentId, contentId);
            createContentAssocMap.put(x.contentIdTo, contentIdThumb);
            createContentAssocMap.put(x.userLogin, userLogin);
            createContentAssocMap.put(x.mapKey, x._100);
            try {
                Map<String, Object> serviceResult = dispatcher.runSync(x.createContentAssoc, createContentAssocMap);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            Map<String, Object> productContentCtx = new HashMap<>();
            productContentCtx.put(x.productId, productId);
            productContentCtx.put(x.productContentTypeId, productContentTypeId);
            productContentCtx.put(x.fromDate, UtilDateTime.nowTimestamp());
            productContentCtx.put(x.userLogin, userLogin);
            productContentCtx.put(x.contentId, contentId);
            productContentCtx.put(x.statusId, x.IM_PENDING);
            try {
                Map<String, Object> serviceResult = dispatcher.runSync(x.createProductContent, productContentCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            Map<String, Object> contentApprovalCtx = new HashMap<>();
            contentApprovalCtx.put(x.contentId, contentId);
            contentApprovalCtx.put(x.userLogin, userLogin);
            try {
                Map<String, Object> serviceResult = dispatcher.runSync(x.createImageContentApproval, contentApprovalCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            String autoApproveImage = EntityUtilProperties.getPropertyValue(x.catalog, x.image_management_autoApproveImage, delegator);
            if (x.Y.equals(autoApproveImage)) {
                Map<String, Object> autoApproveCtx = new HashMap<>();
                autoApproveCtx.put(x.contentId, contentId);
                autoApproveCtx.put(x.userLogin, userLogin);
                autoApproveCtx.put(x.checkStatusId, x.IM_APPROVED);
                try {
                    Map<String, Object> serviceResult = dispatcher.runSync(x.updateStatusImageManagement, autoApproveCtx);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }
        return result;
    }

    public static Map<String, Object> removeImageFileForImageManagement(DispatchContext dctx, ImageManagementServicesContext context) {
        String productId = (String) context.get(x.productId);
        String contentId = (String) context.get(x.contentId);
        String dataResourceName = (String) context.get(x.dataResourceName);
        Delegator delegator = dctx.getDelegator();

        try {
            if (UtilValidate.isNotEmpty(contentId)) {
                String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                        x.image_management_path, delegator), context);
                File file = new File(imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + dataResourceName);
                if (!file.delete()) {
                    Debug.logError(x.File_8c120fff + file.getName() + x.couldn_t_be_deleted, MODULE);
                }
            }
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    private static Map<String, Object> scaleImageMangementInAllSize(DispatchContext dctx, ImageManagementServicesContext context,
                                                                    String filenameToUse, String resizeType, String productId)
        throws IllegalArgumentException, ImagingOpException, IOException, JDOMException {

        /* VARIABLES */
        Locale locale = (Locale) context.get(x.locale);
        List<String> sizeTypeList = null;
        if (UtilValidate.isNotEmpty(resizeType)) {
            sizeTypeList = UtilMisc.toList(resizeType);
        } else {
            sizeTypeList = UtilMisc.toList(x.small, x._100x75, x._150x112, x._320x240, x._640x480, x._800x600, x._1024x768, x._1280x1024, x._1600x1200);
        }

        int index;
        Map<String, Map<String, String>> imgPropertyMap = new HashMap<>();
        BufferedImage bufImg;
        BufferedImage bufNewImg;
        double imgHeight;
        double imgWidth;
        Map<String, String> imgUrlMap = new HashMap<>();
        Map<String, Object> resultXMLMap = new HashMap<>();
        Map<String, Object> resultBufImgMap = new HashMap<>();
        Map<String, Object> resultScaleImgMap = new HashMap<>();
        Map<String, Object> result = new HashMap<>();

        /* ImageProperties.xml */
        String fileName = x.component_product_config_ImageProperties_xml;
        String imgPropertyFullPath = FlexibleLocation.resolveLocation(fileName).getFile();
        resultXMLMap.putAll(ImageTransform.getXMLValue(imgPropertyFullPath, locale));
        if (resultXMLMap.containsKey(x.responseMessage) && x.success.equals(resultXMLMap.get(x.responseMessage))) {
            imgPropertyMap.putAll(UtilGenerics.<Map<String, Map<String, String>>>cast(resultXMLMap.get(x.xml)));
        } else {
            String errMsg = UtilProperties.getMessage(RES_ERROR, x.ScaleImage_unable_to_parse, locale) + x.ImageProperties_xml;
            Debug.logError(errMsg, MODULE);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        /* IMAGE */
        // get Name and Extension
        index = filenameToUse.lastIndexOf('.');
        String imgExtension = filenameToUse.substring(index + 1);
        // paths
        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                x.image_management_path, dctx.getDelegator()), context);
        String imageServerUrl = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                x.image_management_url, dctx.getDelegator()), context);


        /* get original BUFFERED IMAGE */
        resultBufImgMap.putAll(ImageTransform.getBufferedImage(imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + filenameToUse, locale));

        if (resultBufImgMap.containsKey(x.responseMessage) && x.success.equals(resultBufImgMap.get(x.responseMessage))) {
            bufImg = (BufferedImage) resultBufImgMap.get(x.bufferedImage);

            // get Dimensions
            imgHeight = bufImg.getHeight();
            imgWidth = bufImg.getWidth();
            if (imgHeight == 0.0 || imgWidth == 0.0) {
                String errMsg = UtilProperties.getMessage(RES_ERROR, x.ScaleImage_one_current_image_dimension_is_null, locale) + x.imgHeight
                        + imgHeight + x.imgWidth + imgWidth;
                Debug.logError(errMsg, MODULE);
                result.put(ModelService.ERROR_MESSAGE, errMsg);
                return result;
            }

            /* scale Image for each Size Type */
            for (String sizeType : sizeTypeList) {
                resultScaleImgMap.putAll(ImageTransform.scaleImage(bufImg, imgHeight, imgWidth, imgPropertyMap, sizeType, locale));

                if (resultScaleImgMap.containsKey(x.responseMessage) && x.success.equals(resultScaleImgMap.get(x.responseMessage))) {
                    bufNewImg = (BufferedImage) resultScaleImgMap.get(x.bufferedImage);

                    // write the New Scaled Image

                    String targetDirectory = imageServerPath + x.str_42099b4a + productId;
                    File targetDir = new File(targetDirectory);
                    if (!targetDir.exists()) {
                        boolean created = targetDir.mkdirs();
                        if (!created) {
                            String errMsg = UtilProperties.getMessage(RES_ERROR, x.ScaleImage_unable_to_create_target_directory, locale)
                                    + x.str_fc02e199 + targetDirectory;
                            Debug.logFatal(errMsg, MODULE);
                            return ServiceUtil.returnError(errMsg);
                        }
                    }

                    // write new image
                    try {
                        ImageIO.write(bufNewImg, imgExtension, new File(imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + filenameToUse));
                        File deleteFile = new File(imageServerPath + x.str_42099b4a + filenameToUse);
                        if (!deleteFile.delete()) {
                            Debug.logError(x.File_8c120fff + deleteFile.getName() + x.couldn_t_be_deleted, MODULE);
                        }
                    } catch (IllegalArgumentException e) {
                        String errMsg = UtilProperties.getMessage(RES_ERROR, x.ScaleImage_one_parameter_is_null, locale) + e.toString();
                        Debug.logError(errMsg, MODULE);
                        result.put(ModelService.ERROR_MESSAGE, errMsg);
                        return result;
                    } catch (IOException e) {
                        String errMsg = UtilProperties.getMessage(RES_ERROR, x.ScaleImage_error_occurs_during_writing, locale) + e.toString();
                        Debug.logError(errMsg, MODULE);
                        result.put(ModelService.ERROR_MESSAGE, errMsg);
                        return result;
                    }

                    /* write Return Result */
                    String imageUrl = imageServerUrl + x.str_42099b4a + productId + x.str_42099b4a + filenameToUse;
                    imgUrlMap.put(sizeType, imageUrl);

                } // scaleImgMap
            } // sizeIter

            result.put(x.responseMessage, x.success);
            result.put(x.imageUrlMap, imgUrlMap);
            result.put(x.original, resultBufImgMap);
            return result;

        }
        String errMsg = UtilProperties.getMessage(RES_ERROR, x.ScaleImage_unable_to_scale_original_image, locale)
                + x.str_d98411eb + filenameToUse;
        Debug.logError(errMsg, MODULE);
        result.put(ModelService.ERROR_MESSAGE, errMsg);
        return ServiceUtil.returnError(errMsg);
    }

    public static Map<String, Object> createContentAndDataResource(DispatchContext dctx, GenericValue userLogin, String filenameToUse,
                                                                   String imageUrl, String contentId, String fileContentType) {
        Map<String, Object> result = new HashMap<>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        Map<String, Object> dataResourceCtx = new HashMap<>();

        dataResourceCtx.put(x.objectInfo, imageUrl);
        dataResourceCtx.put(x.dataResourceName, filenameToUse);
        dataResourceCtx.put(x.userLogin, userLogin);
        dataResourceCtx.put(x.dataResourceTypeId, x.IMAGE_OBJECT);
        dataResourceCtx.put(x.mimeTypeId, fileContentType);
        dataResourceCtx.put(x.isPublic, x.Y);

        Map<String, Object> dataResourceResult;
        try {
            dataResourceResult = dispatcher.runSync(x.createDataResource, dataResourceCtx);
            if (ServiceUtil.isError(dataResourceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(dataResourceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        String dataResourceId = (String) dataResourceResult.get(x.dataResourceId);
        result.put(x.dataResourceFrameId, dataResourceId);
        result.put(x.dataResourceId, dataResourceId);

        Map<String, Object> contentUp = new HashMap<>();
        contentUp.put(x.contentId, contentId);
        contentUp.put(x.dataResourceId, dataResourceResult.get(x.dataResourceId));
        contentUp.put(x.contentName, filenameToUse);
        contentUp.put(x.userLogin, userLogin);
        try {
            Map<String, Object> serviceResult = dispatcher.runSync(x.updateContent, contentUp);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        GenericValue content = null;
        try {
            ContentDao contentDao = DaoRegistry.getDao(delegator, x.Content, ContentDao.class);
            ContentEntity contentEntity = contentDao.get(contentId).orElse(null);
            content = contentEntity == null ? null : delegator.makeValue(x.Content, Beans.beanToMap(contentEntity));
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (content != null) {
            GenericValue dataResource = null;
            try {
                dataResource = content.getRelatedOne(x.DataResource, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            if (dataResource != null) {
                dataResourceCtx.put(x.dataResourceId, dataResource.getString(x.dataResourceId));
                try {
                    Map<String, Object> serviceResult = dispatcher.runSync(x.updateDataResource, dataResourceCtx);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }
        return result;
    }

    public static Map<String, Object> createContentThumbnail(DispatchContext dctx, ImageManagementServicesContext context,
            GenericValue userLogin, ByteBuffer imageData, String productId, String imageName) throws ImageReadException {
        Map<String, Object> result = new HashMap<>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                x.image_management_path, delegator), context);
        String nameOfThumb = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                x.image_management_nameofthumbnail, delegator), context);

        // Create content for thumbnail
        Map<String, Object> contentThumb = new HashMap<>();
        contentThumb.put(x.contentTypeId, x.DOCUMENT);
        contentThumb.put(x.userLogin, userLogin);
        Map<String, Object> contentThumbResult;
        try {
            contentThumbResult = dispatcher.runSync(x.createContent, contentThumb);
            if (ServiceUtil.isError(contentThumbResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(contentThumbResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        String contentIdThumb = (String) contentThumbResult.get(x.contentId);
        result.put(x.contentIdThumb, contentIdThumb);
        String filenameToUseThumb = imageName.substring(0, imageName.indexOf('.')) + nameOfThumb;
        String fileContentType = (String) context.get(x._uploadedFile_contentType);
        if (x.image_pjpeg.equals(fileContentType)) {
            fileContentType = x.image_jpeg;
        } else if (x.image_x_png.equals(fileContentType)) {
            fileContentType = x.image_png;
        }

        List<GenericValue> fileExtensionThumb;
        try {
            FileExtensionDao fileExtensionDao = DaoRegistry.getDao(delegator, x.FileExtension, FileExtensionDao.class);
            List<FileExtensionEntity> fileExtensionEntities = fileExtensionDao.list(Filters.eq(x.mimeTypeId, fileContentType));
            fileExtensionThumb = new LinkedList<>();
            for (FileExtensionEntity fileExtensionEntity : fileExtensionEntities) {
                fileExtensionThumb.add(delegator.makeValue(x.FileExtension, Beans.beanToMap(fileExtensionEntity)));
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        GenericValue extensionThumb = EntityUtil.getFirst(fileExtensionThumb);
        if (extensionThumb != null) {
            filenameToUseThumb += x.str_3a52ce78 + extensionThumb.getString(x.fileExtensionId);
        }
        result.put(x.filenameToUseThumb, filenameToUseThumb);
        // Create image file thumbnail to folder product id.
        String fileToCheck = imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + filenameToUseThumb;
        File fileOriginalThumb = new File(fileToCheck);
        try {
            Path tempFile = Files.createTempFile(null, null);
            Files.write(tempFile, imageData.array(), StandardOpenOption.APPEND);
            // Check if a webshell is not uploaded
            if (!org.apache.ofbiz.security.SecuredUpload.isValidFile(tempFile.toString(), x.Image, delegator)) {
                String errorMessage = UtilProperties.getMessage(x.SecurityUiLabels, x.SupportedImageFormats, locale);
                return ServiceUtil.returnError(errorMessage);
            }
            File tempFileToDelete = new File(tempFile.toString());
            tempFileToDelete.deleteOnExit();
            RandomAccessFile outFileThumb = new RandomAccessFile(fileOriginalThumb, x.rw);
            outFileThumb.write(imageData.array());
            outFileThumb.close();
        } catch (FileNotFoundException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.ProductImageViewUnableWriteFile,
                    UtilMisc.toMap(x.fileName, fileOriginalThumb.getAbsolutePath()), locale));
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.ProductImageViewUnableWriteBinaryData,
                    UtilMisc.toMap(x.fileName, fileOriginalThumb.getAbsolutePath()), locale));
        }

        return result;
    }

    public static Map<String, Object> resizeImageThumbnail(BufferedImage bufImg, double imgHeight, double imgWidth) {

        /* VARIABLES */
        BufferedImage bufNewImg;
        double defaultHeight;
        double defaultWidth;
        double scaleFactor;
        Map<String, Object> result = new HashMap<>();

        /* DIMENSIONS from ImageProperties */
        defaultHeight = 100;
        defaultWidth = 100;

        /* SCALE FACTOR */
        // find the right Scale Factor related to the Image Dimensions
        if (imgHeight > imgWidth) {
            scaleFactor = defaultHeight / imgHeight;

            // get scaleFactor from the smallest width
            if (defaultWidth < (imgWidth * scaleFactor)) {
                scaleFactor = defaultWidth / imgWidth;
            }
        } else {
            scaleFactor = defaultWidth / imgWidth;
            // get scaleFactor from the smallest height
            if (defaultHeight < (imgHeight * scaleFactor)) {
                scaleFactor = defaultHeight / imgHeight;
            }
        }

        int bufImgType;
        if (BufferedImage.TYPE_CUSTOM == bufImg.getType()) {
            // apply a type for image majority
            bufImgType = BufferedImage.TYPE_INT_ARGB_PRE;
        } else {
            bufImgType = bufImg.getType();
        }

        // scale original image with new size
        Image newImg = bufImg.getScaledInstance((int) (imgWidth * scaleFactor), (int) (imgHeight * scaleFactor), Image.SCALE_SMOOTH);

        bufNewImg = ImageTransform.toBufferedImage(newImg, bufImgType);

        result.put(x.bufferedImage, bufNewImg);
        result.put(x.scaleFactor, scaleFactor);
        return result;
    }

    public static File checkExistsImage(File file) {
        if (!file.exists()) {
            imageCount = 0;
            imagePath = null;
            return file;
        }
        imageCount++;
        String filePath = imagePath.substring(0, imagePath.lastIndexOf('.'));
        String type = imagePath.substring(imagePath.lastIndexOf('.') + 1);
        file = new File(filePath + x.str_28ed3a79 + imageCount + x.str_d4191940 + type);
        return checkExistsImage(file);
    }

    public static Map<String, Object> resizeImage(BufferedImage bufImg, double imgHeight, double imgWidth, double resizeHeight, double resizeWidth) {

        /* VARIABLES */
        BufferedImage bufNewImg;
        double defaultHeight;
        double defaultWidth;
        double scaleFactor;
        Map<String, Object> result = new HashMap<>();

        /* DIMENSIONS from ImageProperties */
        defaultHeight = resizeHeight;
        defaultWidth = resizeWidth;

        /* SCALE FACTOR */
        // find the right Scale Factor related to the Image Dimensions
        if (imgHeight > imgWidth) {
            scaleFactor = defaultHeight / imgHeight;

            // get scaleFactor from the smallest width
            if (defaultWidth < (imgWidth * scaleFactor)) {
                scaleFactor = defaultWidth / imgWidth;
            }
        } else {
            scaleFactor = defaultWidth / imgWidth;
            // get scaleFactor from the smallest height
            if (defaultHeight < (imgHeight * scaleFactor)) {
                scaleFactor = defaultHeight / imgHeight;
            }
        }

        int bufImgType;
        if (BufferedImage.TYPE_CUSTOM == bufImg.getType()) {
            // apply a type for image majority
            bufImgType = BufferedImage.TYPE_INT_ARGB_PRE;
        } else {
            bufImgType = bufImg.getType();
        }

        // scale original image with new size
        Image newImg = bufImg.getScaledInstance((int) (imgWidth * scaleFactor), (int) (imgHeight * scaleFactor), Image.SCALE_SMOOTH);

        bufNewImg = ImageTransform.toBufferedImage(newImg, bufImgType);

        result.put(x.bufferedImage, bufNewImg);
        result.put(x.scaleFactor, scaleFactor);
        return result;
    }

    public static Map<String, Object> createNewImageThumbnail(DispatchContext dctx, ImageManagementServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dispatcher.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                x.image_management_path, delegator), context);
        String imageServerUrl = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                x.image_management_url, delegator), context);
        String productId = (String) context.get(x.productId);
        String contentId = (String) context.get(x.contentId);
        String dataResourceName = (String) context.get(x.dataResourceName);
        String width = (String) context.get(x.sizeWidth);
        String imageType = x.jpg;
        int resizeWidth = Integer.parseInt(width);
        int resizeHeight = resizeWidth;

        try {
            BufferedImage bufImg = ImageIO.read(new File(imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + dataResourceName));
            double imgHeight = bufImg.getHeight();
            double imgWidth = bufImg.getWidth();
            if (dataResourceName.lastIndexOf('.') > 0 && dataResourceName.lastIndexOf('.') < dataResourceName.length()) {
                imageType = dataResourceName.substring(dataResourceName.lastIndexOf('.'));
            }

            String filenameToUse = dataResourceName.substring(0, dataResourceName.length() - 4) + x.str_3bc15c8a + resizeWidth + imageType;

            if (dataResourceName.length() > 3) {
                String mimeType = dataResourceName.substring(dataResourceName.length() - 3, dataResourceName.length());
                Map<String, Object> resultResize = resizeImage(bufImg, imgHeight, imgWidth, resizeHeight, resizeWidth);
                ImageIO.write((RenderedImage) resultResize.get(x.bufferedImage), mimeType, new File(imageServerPath
                        + x.str_42099b4a + productId + x.str_42099b4a + filenameToUse));

                Map<String, Object> contentThumb = new HashMap<>();
                contentThumb.put(x.contentTypeId, x.DOCUMENT);
                contentThumb.put(x.userLogin, userLogin);
                Map<String, Object> contentThumbResult;
                try {
                    contentThumbResult = dispatcher.runSync(x.createContent, contentThumb);
                    if (ServiceUtil.isError(contentThumbResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(contentThumbResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                String contentIdThumb = (String) contentThumbResult.get(x.contentId);
                String imageUrlThumb = imageServerUrl + x.str_42099b4a + productId + x.str_42099b4a + filenameToUse;
                createContentAndDataResource(dctx, userLogin, filenameToUse, imageUrlThumb, contentIdThumb, x.image_jpeg);

                Map<String, Object> createContentAssocMap = new HashMap<>();
                createContentAssocMap.put(x.contentAssocTypeId, x.IMAGE_THUMBNAIL);
                createContentAssocMap.put(x.contentId, contentId);
                createContentAssocMap.put(x.contentIdTo, contentIdThumb);
                createContentAssocMap.put(x.userLogin, userLogin);
                createContentAssocMap.put(x.mapKey, width);
                try {
                    Map<String, Object> serviceResult = dispatcher.runSync(x.createContentAssoc, createContentAssocMap);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        String successMsg = UtilProperties.getMessage(RESOURCE, x.ProductCreateNewThumbnailSizeSuccessful, locale);
        return ServiceUtil.returnSuccess(successMsg);
    }

    public static Map<String, Object> resizeImageOfProduct(DispatchContext dctx, ImageManagementServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                x.image_management_path, delegator), context);
        String productId = (String) context.get(x.productId);
        String dataResourceName = (String) context.get(x.dataResourceName);
        String width = (String) context.get(x.resizeWidth);
        int resizeWidth = Integer.parseInt(width);
        int resizeHeight = resizeWidth;

        try {
            BufferedImage bufImg = ImageIO.read(new File(imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + dataResourceName));
            double imgHeight = bufImg.getHeight();
            double imgWidth = bufImg.getWidth();
            String filenameToUse = dataResourceName;
            String mimeType = dataResourceName.substring(dataResourceName.length() - 3, dataResourceName.length());
            Map<String, Object> resultResize = resizeImage(bufImg, imgHeight, imgWidth, resizeHeight, resizeWidth);
            ImageIO.write((RenderedImage) resultResize.get(x.bufferedImage), mimeType, new File(imageServerPath + x.str_42099b4a
                    + productId + x.str_42099b4a + filenameToUse));
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        String successMsg = UtilProperties.getMessage(RESOURCE, x.ProductResizeImagesSuccessful, locale);
        return ServiceUtil.returnSuccess(successMsg);
    }

    public static Map<String, Object> renameImage(DispatchContext dctx, ImageManagementServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                x.image_management_path, delegator), context);
        String imageServerUrl = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue(x.catalog,
                x.image_management_url, delegator), context);
        String productId = (String) context.get(x.productId);
        String contentId = (String) context.get(x.contentId);
        String filenameToUse = (String) context.get(x.drDataResourceName);
        String imageType = filenameToUse.substring(filenameToUse.lastIndexOf('.'));
        String imgExtension = filenameToUse.substring(filenameToUse.length() - 3, filenameToUse.length());
        String imageUrl = imageServerUrl + x.str_42099b4a + productId + x.str_42099b4a + filenameToUse;

        try {
            ProductContentDao productContentDao = DaoRegistry.getDao(delegator, x.ProductContent, ProductContentDao.class);
            ContentDao contentDao = DaoRegistry.getDao(delegator, x.Content, ContentDao.class);
            ContentAssocDao contentAssocDao = DaoRegistry.getDao(delegator, x.ContentAssoc, ContentAssocDao.class);

            List<ProductContentEntity> productContentEntities = productContentDao.list(Filters.and(
                    Filters.eq(x.productId, productId),
                    Filters.eq(x.contentId, contentId),
                    Filters.eq(x.productContentTypeId, x.IMAGE)));
            GenericValue productContent = productContentEntities.isEmpty() ? null
                    : delegator.makeValue(x.ProductContent, Beans.beanToMap(productContentEntities.get(0)));
            if (productContent != null) {
                ContentEntity productContentEntity = contentDao.get(contentId).orElse(null);
                GenericValue productContentValue = productContentEntity == null ? null
                        : delegator.makeValue(x.Content, Beans.beanToMap(productContentEntity));
                if (productContentValue != null) {
                    GenericValue productContentDataResource = productContentValue.getRelatedOne(x.DataResource, false);
                    if (productContentDataResource != null) {
                        productContent.put(x.drDataResourceName, productContentDataResource.get(x.dataResourceName));
                    }
                }
            }
            String dataResourceName = (String) productContent.get(x.drDataResourceName);
            String mimeType = filenameToUse.substring(filenameToUse.lastIndexOf('.'));

            if (imageType.equals(mimeType)) {
                BufferedImage bufImg = ImageIO.read(new File(imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + dataResourceName));
                ImageIO.write(bufImg, imgExtension, new File(imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + filenameToUse));

                File file = new File(imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + dataResourceName);
                if (!file.delete()) {
                    Debug.logError(x.File_8c120fff + file.getName() + x.couldn_t_be_deleted, MODULE);
                }

                Map<String, Object> contentUp = new HashMap<>();
                contentUp.put(x.contentId, contentId);
                contentUp.put(x.contentName, filenameToUse);
                contentUp.put(x.userLogin, userLogin);
                try {
                    Map<String, Object> serviceResult = dispatcher.runSync(x.updateContent, contentUp);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
                ContentEntity contentEntity = contentDao.get(contentId).orElse(null);
                GenericValue content = contentEntity == null ? null : delegator.makeValue(x.Content, Beans.beanToMap(contentEntity));
                if (content != null) {
                    GenericValue dataResource = null;
                    try {
                        dataResource = content.getRelatedOne(x.DataResource, false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    if (dataResource != null) {
                        Map<String, Object> dataResourceCtx = new HashMap<>();
                        dataResourceCtx.put(x.dataResourceId, dataResource.getString(x.dataResourceId));
                        dataResourceCtx.put(x.objectInfo, imageUrl);
                        dataResourceCtx.put(x.dataResourceName, filenameToUse);
                        dataResourceCtx.put(x.userLogin, userLogin);
                        try {
                            Map<String, Object> serviceResult = dispatcher.runSync(x.updateDataResource, dataResourceCtx);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, MODULE);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    }
                }

                List<ContentAssocEntity> contentAssocEntities = contentAssocDao.list(Filters.and(
                        Filters.eq(x.contentId, contentId),
                        Filters.eq(x.contentAssocTypeId, x.IMAGE_THUMBNAIL)));
                List<GenericValue> contentAssocList = new LinkedList<>();
                for (ContentAssocEntity contentAssocEntity : contentAssocEntities) {
                    contentAssocList.add(delegator.makeValue(x.ContentAssoc, Beans.beanToMap(contentAssocEntity)));
                }
                if (!contentAssocList.isEmpty()) {
                    for (int i = 0; i < contentAssocList.size(); i++) {
                        GenericValue contentAssoc = contentAssocList.get(i);

                        List<GenericValue> dataResourceAssocList = new LinkedList<>();
                        ContentEntity contentAssocContentEntity = contentDao.get((String) contentAssoc.get(x.contentIdTo)).orElse(null);
                        GenericValue contentAssocContent = contentAssocContentEntity == null ? null
                                : delegator.makeValue(x.Content, Beans.beanToMap(contentAssocContentEntity));
                        if (contentAssocContent != null) {
                            GenericValue contentAssocDataResource = contentAssocContent.getRelatedOne(x.DataResource, false);
                            if (contentAssocDataResource != null) {
                                GenericValue contentDataResourceView = delegator.makeValue(x.ContentDataResourceView);
                                contentDataResourceView.put(x.contentId, contentAssoc.get(x.contentIdTo));
                                contentDataResourceView.put(x.drDataResourceName, contentAssocDataResource.get(x.dataResourceName));
                                dataResourceAssocList.add(contentDataResourceView);
                            }
                        }
                        GenericValue dataResourceAssoc = EntityUtil.getFirst(dataResourceAssocList);

                        String drDataResourceNameAssoc = (String) dataResourceAssoc.get(x.drDataResourceName);
                        String filenameToUseAssoc = filenameToUse.substring(0, filenameToUse.length() - 4) + x.str_3bc15c8a + contentAssoc.get(x.mapKey)
                                + imageType;
                        String imageUrlAssoc = imageServerUrl + x.str_42099b4a + productId + x.str_42099b4a + filenameToUseAssoc;

                        BufferedImage bufImgAssoc = ImageIO.read(new File(imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + drDataResourceNameAssoc));
                        ImageIO.write(bufImgAssoc, imgExtension, new File(imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + filenameToUseAssoc));

                        File fileAssoc = new File(imageServerPath + x.str_42099b4a + productId + x.str_42099b4a + drDataResourceNameAssoc);
                        if (!fileAssoc.delete()) {
                            Debug.logError(x.File_8c120fff + fileAssoc.getName() + x.couldn_t_be_deleted, MODULE);
                        }

                        Map<String, Object> contentAssocMap = new HashMap<>();
                        contentAssocMap.put(x.contentId, contentAssoc.get(x.contentIdTo));
                        contentAssocMap.put(x.contentName, filenameToUseAssoc);
                        contentAssocMap.put(x.userLogin, userLogin);
                        try {
                            Map<String, Object> serviceResult = dispatcher.runSync(x.updateContent, contentAssocMap);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, MODULE);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        ContentEntity contentAssocUpEntity = contentDao.get((String) contentAssoc.get(x.contentIdTo)).orElse(null);
                        GenericValue contentAssocUp = contentAssocUpEntity == null ? null
                                : delegator.makeValue(x.Content, Beans.beanToMap(contentAssocUpEntity));
                        if (contentAssocUp != null) {
                            GenericValue dataResourceAssocUp = null;
                            try {
                                dataResourceAssocUp = contentAssocUp.getRelatedOne(x.DataResource, false);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, MODULE);
                                return ServiceUtil.returnError(e.getMessage());
                            }

                            if (dataResourceAssocUp != null) {
                                Map<String, Object> dataResourceAssocMap = new HashMap<>();
                                dataResourceAssocMap.put(x.dataResourceId, dataResourceAssocUp.getString(x.dataResourceId));
                                dataResourceAssocMap.put(x.objectInfo, imageUrlAssoc);
                                dataResourceAssocMap.put(x.dataResourceName, filenameToUseAssoc);
                                dataResourceAssocMap.put(x.userLogin, userLogin);
                                try {
                                    Map<String, Object> serviceResult = dispatcher.runSync(x.updateDataResource, dataResourceAssocMap);
                                    if (ServiceUtil.isError(serviceResult)) {
                                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                                    }
                                } catch (GenericServiceException e) {
                                    Debug.logError(e, MODULE);
                                    return ServiceUtil.returnError(e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        String successMsg = UtilProperties.getMessage(RESOURCE, x.ProductRenameImageSuccessfully, locale);
        return ServiceUtil.returnSuccess(successMsg);
    }
}
