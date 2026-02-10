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
package org.apache.ofbiz.product.promo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.ContactMechDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.ProductPromoCodeDao;
import org.apache.ofbiz.persistence.dao.ProductPromoDao;
import org.apache.ofbiz.persistence.dao.ProductStorePromoApplDao;
import org.apache.ofbiz.persistence.entity.ContactMechEntity;
import org.apache.ofbiz.persistence.entity.ProductPromoEntity;
import org.apache.ofbiz.persistence.entity.ProductStorePromoApplEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.condition.Condition;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PromoServicesContext;
/**
 * Promotions Services
 */
public class PromoServices {

    private static final String MODULE = PromoServices.class.getName();
    private static final String RESOURCE = x.ProductUiLabels;
    private static final char[] SMART_CHARS = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
            'X', 'Y', 'Z', '2', '3', '4', '5', '6', '7', '8', '9' };

    public static Map<String, Object> createProductPromoCodeSet(DispatchContext dctx, PromoServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Long quantity = (Long) context.get(x.quantity);
        int codeLength = (Integer) context.get(x.codeLength);
        String promoCodeLayout = (String) context.get(x.promoCodeLayout);

        // For PromoCodes we give the option not to use chars that are easy to mix up like 0<>O, 1<>I, ...
        boolean useSmartLayout = false;
        boolean useNormalLayout = false;
        if (x.smart.equals(promoCodeLayout)) {
            useSmartLayout = true;
        } else if (x.normal.equals(promoCodeLayout)) {
            useNormalLayout = true;
        }

        String newPromoCodeId = x.emptyString;
        StringBuilder bankOfNumbers = new StringBuilder();
        bankOfNumbers.append(UtilProperties.getMessage(RESOURCE, x.ProductPromoCodesCreated, locale));
        ProductPromoCodeDao productPromoCodeDao = DaoRegistry.getDao(delegator, x.ProductPromoCode, ProductPromoCodeDao.class);
        for (long i = 0; i < quantity; i++) {
            Map<String, Object> createProductPromoCodeMap = null;
            boolean foundUniqueNewCode = false;
            long count = 0;

            while (!foundUniqueNewCode) {
                if (useSmartLayout) {
                    newPromoCodeId = RandomStringUtils.random(codeLength, SMART_CHARS);
                } else if (useNormalLayout) {
                    newPromoCodeId = RandomStringUtils.randomAlphanumeric(codeLength);
                }
                boolean promoCodeExists = false;
                try {
                    promoCodeExists = productPromoCodeDao.get(newPromoCodeId).isPresent();
                } catch (Exception e) {
                    Debug.logWarning(x.Could_not_find_ProductPromoCode_for_just_generated_ID + newPromoCodeId, MODULE);
                }
                if (!promoCodeExists) {
                    foundUniqueNewCode = true;
                }

                count++;
                if (count > 999999) {
                    return ServiceUtil.returnError(x.Unable_to_locate_unique_PromoCode_Length + codeLength + x.str_4ff447b8);
                }
            }
            try {
                Map<String, Object> newContext = dctx.makeValidContext(x.createProductPromoCode, ModelService.IN_PARAM, context);
                newContext.put(x.productPromoCodeId, newPromoCodeId);
                createProductPromoCodeMap = dispatcher.runSync(x.createProductPromoCode, newContext);
            } catch (GenericServiceException err) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ProductPromoCodeCannotBeCreated, locale), null, null, null);
            }
            if (ServiceUtil.isError(createProductPromoCodeMap)) {
                // what to do here? try again?
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ProductPromoCodeCannotBeCreated, locale), null,
                        null, createProductPromoCodeMap);
            }
            bankOfNumbers.append((String) createProductPromoCodeMap.get(x.productPromoCodeId));
            bankOfNumbers.append(x.str_5c10b5b2);
        }

        return ServiceUtil.returnSuccess(bankOfNumbers.toString());
    }

    public static Map<String, Object> purgeOldStoreAutoPromos(DispatchContext dctx, PromoServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get(x.productStoreId);
        Locale locale = (Locale) context.get(x.locale);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        List<Condition> condList = new LinkedList<>();
        if (UtilValidate.isEmpty(productStoreId)) {
            condList.add(Filters.eq(x.productStoreId, productStoreId));
        }

        try {
            ProductStorePromoApplDao productStorePromoApplDao = DaoRegistry.getDao(delegator, x.ProductStorePromoAppl, ProductStorePromoApplDao.class);
            ProductPromoDao productPromoDao = DaoRegistry.getDao(delegator, x.ProductPromo, ProductPromoDao.class);

            Condition applCondition = condList.isEmpty() ? Filters.alwaysTrue() : Filters.and(condList);
            List<ProductStorePromoApplEntity> productStorePromoApplEntities = productStorePromoApplDao.list(applCondition);
            Map<String, ProductPromoEntity> productPromoById = new HashMap<>();

            for (ProductStorePromoApplEntity productStorePromoApplEntity : productStorePromoApplEntities) {
                if (productStorePromoApplEntity.getThruDate() == null || !productStorePromoApplEntity.getThruDate().before(nowTimestamp)) {
                    continue;
                }
                String productPromoId = productStorePromoApplEntity.getProductPromoId();
                ProductPromoEntity productPromoEntity = productPromoById.get(productPromoId);
                if (productPromoEntity == null && !productPromoById.containsKey(productPromoId)) {
                    productPromoEntity = productPromoDao.get(productPromoId).orElse(null);
                    productPromoById.put(productPromoId, productPromoEntity);
                }
                if (productPromoEntity == null || !x.Y.equals(productPromoEntity.getUserEntered())) {
                    continue;
                }

                GenericValue productStorePromo = delegator.makeValue(x.ProductStorePromoAppl, UtilMisc.toMap(x.productStoreId,
                        productStorePromoApplEntity.getProductStoreId(), x.productPromoId, productStorePromoApplEntity.getProductPromoId(), x.fromDate,
                        productStorePromoApplEntity.getFromDate(), x.thruDate, productStorePromoApplEntity.getThruDate(), x.sequenceNum,
                        productStorePromoApplEntity.getSequenceNum(), x.manualOnly, productStorePromoApplEntity.getManualOnly()));
                productStorePromo.remove();
            }
        } catch (Exception e) {
            Debug.logError(e, x.Error_removing_expired_ProductStorePromo_records + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductPromoCodeCannotBeRemoved, UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> importPromoCodesFromFile(DispatchContext dctx, PromoServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);

        // check the uploaded file
        ByteBuffer fileBytes = (ByteBuffer) context.get(x.uploadedFile);
        if (fileBytes == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductPromoCodeImportUploadedFileNotValid, locale));
        }

        String encoding = System.getProperty(x.file_encoding);
        String file = Charset.forName(encoding).decode(fileBytes).toString();
        // get the createProductPromoCode Model
        ModelService promoModel;
        try {
            promoModel = dispatcher.getDispatchContext().getModelService(x.createProductPromoCode);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // make a temp context for invocations
        Map<String, Object> invokeCtx = promoModel.makeValid(context, ModelService.IN_PARAM);

        // read the bytes into a reader
        BufferedReader reader = new BufferedReader(new StringReader(file));
        List<Object> errors = new LinkedList<>();
        int lines = 0;
        String line;

        // read the uploaded file and process each line
        try {
            while ((line = reader.readLine()) != null) {
                // check to see if we should ignore this line
                if (!line.isEmpty() && !line.startsWith(x.str_d08f88df)) {
                    if (line.length() <= 20) {
                        // valid promo code
                        Map<String, Object> inContext = new HashMap<>();
                        inContext.putAll(invokeCtx);
                        inContext.put(x.productPromoCodeId, line);
                        Map<String, Object> result = dispatcher.runSync(x.createProductPromoCode, inContext);
                        if (result != null && ServiceUtil.isError(result)) {
                            errors.add(line + x.str_ceca32e9 + ServiceUtil.getErrorMessage(result));
                        }
                    } else {
                        // not valid ignore and notify
                        errors.add(line + UtilProperties.getMessage(RESOURCE, x.ProductPromoCodeInvalidCode, locale));
                    }
                    ++lines;
                }
            }
        } catch (IOException | GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Debug.logError(e, MODULE);
            }
        }

        // return errors or success
        if (!errors.isEmpty()) {
            return ServiceUtil.returnError(errors);
        } else if (lines == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductPromoCodeImportEmptyFile, locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> importPromoCodeEmailsFromFile(DispatchContext dctx, PromoServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productPromoCodeId = (String) context.get(x.productPromoCodeId);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        ByteBuffer bytebufferwrapper = (ByteBuffer) context.get(x.uploadedFile);

        if (bytebufferwrapper == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ProductPromoCodeImportUploadedFileNotValid, locale));
        }

        byte[] wrapper = bytebufferwrapper.array();

      // read the bytes into a reader
        BufferedReader reader = new BufferedReader(new StringReader(new String(wrapper, StandardCharsets.UTF_8)));
        List<Object> errors = new LinkedList<>();
        int lines = 0;
        String line;
        ContactMechDao contactMechDao = DaoRegistry.getDao(dctx.getDelegator(), x.ContactMech, ContactMechDao.class);

        // read the uploaded file and process each line
        try {
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith(x.str_d08f88df)) {
                    if (UtilValidate.isEmail(line)) {
                        // valid email address
                        ContactMechEntity contactMechEntity;
                        String contactMechId;
                        try {
                            //check for existing contactMechId
                            List<ContactMechEntity> contactMechEntities = contactMechDao.list(Filters.eq(x.infoString, line));
                            if (contactMechEntities.size() > 1) {
                                errors.add(line + x.Too_many_contactMechIds_found);
                                continue;
                            }
                            contactMechEntity = contactMechEntities.stream().findFirst().orElse(null);
                        } catch (Exception e) {
                            Debug.logError(e, MODULE);
                            errors.add(line + x.Too_many_contactMechIds_found);
                            continue;
                        }
                        Map<String, Object> result = new HashMap<>();
                        if (contactMechEntity == null) {
                            //If no contactMech found create new
                            result = dispatcher.runSync(x.createContactMech,
                                    UtilMisc.toMap(x.contactMechTypeId, x.EMAIL_ADDRESS, x.infoString, line,
                                            x.userLogin, userLogin));
                            if (ServiceUtil.isError(result)) {
                                errors.add(line + x.str_ceca32e9 + ServiceUtil.getErrorMessage(result));
                                continue;
                            } else {
                                contactMechId = (String) result.get(x.contactMechId);
                            }
                        } else {
                            contactMechId = contactMechEntity.getContactMechId();
                        }
                        result.clear();
                        result = dispatcher.runSync(x.createProductPromoCodeContactMech,
                                UtilMisc.<String, Object>toMap(x.productPromoCodeId,
                                productPromoCodeId, x.contactMechId, contactMechId, x.userLogin, userLogin));
                        if (ServiceUtil.isError(result)) {
                            errors.add(line + x.str_ceca32e9 + ServiceUtil.getErrorMessage(result));
                        }
                    } else {
                        // not valid ignore and notify
                        errors.add(line + x.is_not_a_valid_email_address);
                    }
                    ++lines;
                }
            }
        } catch (IOException | GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Debug.logError(e, MODULE);
            }
        }

        // return errors or success
        if (!errors.isEmpty()) {
            return ServiceUtil.returnError(errors);
        } else if (lines == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ProductPromoCodeImportEmptyFile, locale));
        }

        return ServiceUtil.returnSuccess();
    }
}
