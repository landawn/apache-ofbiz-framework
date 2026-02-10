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
package org.apache.ofbiz.content.survey;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.ContentDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.SurveyDao;
import org.apache.ofbiz.persistence.dao.SurveyQuestionApplDao;
import org.apache.ofbiz.persistence.dao.SurveyQuestionDao;
import org.apache.ofbiz.persistence.dao.SurveyResponseAnswerDao;
import org.apache.ofbiz.persistence.dao.SurveyResponseDao;
import org.apache.ofbiz.persistence.entity.ContentEntity;
import org.apache.ofbiz.persistence.entity.SurveyEntity;
import org.apache.ofbiz.persistence.entity.SurveyQuestionApplEntity;
import org.apache.ofbiz.persistence.entity.SurveyQuestionEntity;
import org.apache.ofbiz.persistence.entity.SurveyResponseAnswerEntity;
import org.apache.ofbiz.persistence.entity.SurveyResponseEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;



import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PdfSurveyServicesContext;
/**
 * PdfSurveyServices Class
 */

public class PdfSurveyServices {

    private static final String MODULE = PdfSurveyServices.class.getName();
    private static final String RESOURCE = x.ContentUiLabels;

    /**
     */
    public static Map<String, Object> buildSurveyFromPdf(DispatchContext dctx, PdfSurveyServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        String surveyId = null;
        try {
            String surveyName = (String) context.get(x.surveyName);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ByteBuffer byteBuffer = getInputByteBuffer(context, delegator);
            PdfReader pdfReader = new PdfReader(byteBuffer.array());
            PdfStamper pdfStamper = new PdfStamper(pdfReader, os);
            AcroFields acroFields = pdfStamper.getAcroFields();
            Map<String, Object> acroFieldMap = UtilGenerics.cast(acroFields.getAllFields());

            String contentId = (String) context.get(x.contentId);
            GenericValue survey = null;
            surveyId = (String) context.get(x.surveyId);
            if (UtilValidate.isEmpty(surveyId)) {
                survey = delegator.makeValue(x.Survey, UtilMisc.toMap(x.surveyName, surveyName));
                survey.set(x.surveyId, surveyId);
                survey.set(x.allowMultiple, x.Y);
                survey.set(x.allowUpdate, x.Y);
                survey = delegator.createSetNextSeqId(survey);
                surveyId = survey.getString(x.surveyId);
            }

            // create a SurveyQuestionCategory to put the questions in
            Map<String, Object> createCategoryResultMap = dispatcher.runSync(x.createSurveyQuestionCategory,
                    UtilMisc.<String, Object>toMap(x.description, x.From_AcroForm_in_Content + contentId + x.for_Survey + surveyId
                            + x.str_4ff447b8, x.userLogin, userLogin));
            if (ServiceUtil.isError(createCategoryResultMap)) {
                pdfStamper.close();
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createCategoryResultMap));
            }
            String surveyQuestionCategoryId = (String) createCategoryResultMap.get(x.surveyQuestionCategoryId);

            pdfStamper.setFormFlattening(true);
            for (String fieldName : acroFieldMap.keySet()) {
                AcroFields.Item item = acroFields.getFieldItem(fieldName);
                int type = acroFields.getFieldType(fieldName);
                String value = acroFields.getField(fieldName);
                Debug.logInfo(x.fieldName_c17d781f + fieldName + x.item_3f775015 + item + x.value_9d5e20d1 + value, MODULE);

                GenericValue surveyQuestion = delegator.makeValue(x.SurveyQuestion, UtilMisc.toMap(x.question, fieldName));
                String surveyQuestionId = delegator.getNextSeqId(x.SurveyQuestion);
                surveyQuestion.set(x.surveyQuestionId, surveyQuestionId);
                surveyQuestion.set(x.surveyQuestionCategoryId, surveyQuestionCategoryId);

                if (type == AcroFields.FIELD_TYPE_TEXT) {
                    surveyQuestion.set(x.surveyQuestionTypeId, x.TEXT_SHORT);
                } else if (type == AcroFields.FIELD_TYPE_RADIOBUTTON) {
                    surveyQuestion.set(x.surveyQuestionTypeId, x.OPTION);
                } else if (type == AcroFields.FIELD_TYPE_LIST || type == AcroFields.FIELD_TYPE_COMBO) {
                    surveyQuestion.set(x.surveyQuestionTypeId, x.OPTION);
                    // TODO: handle these specially with the acroFields.getListOptionDisplay (and getListOptionExport?)
                } else {
                    surveyQuestion.set(x.surveyQuestionTypeId, x.TEXT_SHORT);
                    Debug.logWarning(x.Building_Survey_from_PDF_fieldName + fieldName + x.don_t_know_how_to_handle_field_type
                            + type + x.defaulting_to_short_text, MODULE);
                }

                // ==== create a good sequenceNum based on tab order or if no tab order then the page location

                Integer tabPage = item.getPage(0);
                Integer tabOrder = item.getTabOrder(0);
                Debug.logInfo(x.tabPage + tabPage + x.tabOrder + tabOrder, MODULE);

                //array of float  multiple of 5. For each of this groups the values are: [page, llx, lly, urx, ury]
                float[] fieldPositions = acroFields.getFieldPositions(fieldName);
                float fieldPage = fieldPositions[0];
                float fieldLlx = fieldPositions[1];
                float fieldLly = fieldPositions[2];
                float fieldUrx = fieldPositions[3];
                float fieldUry = fieldPositions[4];
                Debug.logInfo(x.fieldPage + fieldPage + x.fieldLlx + fieldLlx + x.fieldLly + fieldLly + x.fieldUrx
                        + fieldUrx + x.fieldUry + fieldUry, MODULE);

                Long sequenceNum = null;
                if (tabPage != null && tabOrder != null) {
                    sequenceNum = (long) (tabPage * 1000 + tabOrder);
                    Debug.logInfo(x.tabPage + tabPage + x.tabOrder + tabOrder + x.sequenceNum_441f482e + sequenceNum, MODULE);
                } else if (fieldPositions.length > 0) {
                    sequenceNum = (long) fieldPage * 10000 + (long) fieldLly * 1000 + (long) fieldLlx;
                    Debug.logInfo(x.fieldPage + fieldPage + x.fieldLlx + fieldLlx + x.fieldLly + fieldLly + x.fieldUrx
                            + fieldUrx + x.fieldUry + fieldUry + x.sequenceNum_441f482e + sequenceNum, MODULE);
                }

                // TODO: need to find something better to put into these fields...
                String annotation = null;
                for (int k = 0; k < item.size(); ++k) {
                    PdfDictionary dict = item.getWidget(k);

                    // if the "/Type" value is "/Annot", then get the value of "/TU" for the annotation

                    PdfObject typeValue = null;
                    PdfObject tuValue = null;

                    Set<PdfName> dictKeys = UtilGenerics.cast(dict.getKeys());
                    for (PdfName dictKeyName : dictKeys) {
                        PdfObject dictObject = dict.get(dictKeyName);

                        if (x.Type_33f3450a.equals(dictKeyName.toString())) {
                            typeValue = dictObject;
                        } else if (x.TU.equals(dictKeyName.toString())) {
                            tuValue = dictObject;
                        }
                    }
                    if (tuValue != null && typeValue != null && x.Annot.equals(typeValue.toString())) {
                        annotation = tuValue.toString();
                    }
                }

                surveyQuestion.set(x.description, fieldName);
                if (UtilValidate.isNotEmpty(annotation)) {
                    surveyQuestion.set(x.question, annotation);
                } else {
                    surveyQuestion.set(x.question, fieldName);
                }

                GenericValue surveyQuestionAppl = delegator.makeValue(x.SurveyQuestionAppl,
                        UtilMisc.toMap(x.surveyId, surveyId, x.surveyQuestionId, surveyQuestionId));
                surveyQuestionAppl.set(x.fromDate, nowTimestamp);
                surveyQuestionAppl.set(x.externalFieldRef, fieldName);

                if (sequenceNum != null) {
                    surveyQuestionAppl.set(x.sequenceNum, sequenceNum);
                }

                surveyQuestion.create();
                surveyQuestionAppl.create();
            }
            pdfStamper.close();
            if (UtilValidate.isNotEmpty(contentId)) {
                survey = getSurveyValue(delegator, surveyId);
                survey.set(x.acroFormContentId, contentId);
                survey.store();
            }
        } catch (GeneralException | DocumentException | IOException e) {
            Debug.logError(e, x.Error_generating_PDF + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentPDFGeneratingError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put(x.surveyId, surveyId);
        return results;
    }

    /**
     */
    public static Map<String, Object> buildSurveyResponseFromPdf(DispatchContext dctx, PdfSurveyServicesContext context) {
        String surveyResponseId = null;
        Locale locale = (Locale) context.get(x.locale);
        try {
            Delegator delegator = dctx.getDelegator();
            String partyId = (String) context.get(x.partyId);
            String surveyId = (String) context.get(x.surveyId);
            surveyResponseId = (String) context.get(x.surveyResponseId);
            if (UtilValidate.isNotEmpty(surveyResponseId)) {
                GenericValue surveyResponse = getSurveyResponseValue(delegator, surveyResponseId);
                if (surveyResponse != null) {
                    surveyId = surveyResponse.getString(x.surveyId);
                }
            } else {
                surveyResponseId = delegator.getNextSeqId(x.SurveyResponse);
                GenericValue surveyResponse = delegator.makeValue(x.SurveyResponse,
                        UtilMisc.toMap(x.surveyResponseId, surveyResponseId, x.surveyId, surveyId, x.partyId, partyId));
                surveyResponse.set(x.responseDate, UtilDateTime.nowTimestamp());
                surveyResponse.set(x.lastModifiedDate, UtilDateTime.nowTimestamp());
                surveyResponse.create();
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ByteBuffer byteBuffer = getInputByteBuffer(context, delegator);
            PdfReader r = new PdfReader(byteBuffer.array());
            PdfStamper s = new PdfStamper(r, os);
            AcroFields fs = s.getAcroFields();
            Map<String, Object> hm = UtilGenerics.cast(fs.getAllFields());
            s.setFormFlattening(true);
            for (String fieldName : hm.keySet()) {
                // AcroFields.Item item = fs.getFieldItem(fieldName);
                String value = fs.getField(fieldName);
                GenericValue surveyQuestionAndAppl = getSurveyQuestionAndApplValue(delegator, surveyId, fieldName);
                if (surveyQuestionAndAppl == null) {
                    Debug.logInfo(x.No_question_found_for_surveyId + surveyId + x.and_externalFieldRef + fieldName,
                            MODULE);
                    continue;
                }

                String surveyQuestionId = (String) surveyQuestionAndAppl.get(x.surveyQuestionId);
                String surveyQuestionTypeId = (String) surveyQuestionAndAppl.get(x.surveyQuestionTypeId);
                GenericValue surveyResponseAnswer = delegator.makeValue(x.SurveyResponseAnswer,
                        UtilMisc.toMap(x.surveyResponseId, surveyResponseId, x.surveyQuestionId, surveyQuestionId));
                if (surveyQuestionTypeId == null || x.TEXT_SHORT.equals(surveyQuestionTypeId)) {
                    surveyResponseAnswer.set(x.textResponse, value);
                }

                delegator.create(surveyResponseAnswer);
            }
            s.close();
        } catch (GeneralException | DocumentException | IOException e) {
            Debug.logError(e, x.Error_generating_PDF + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentPDFGeneratingError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put(x.surveyResponseId, surveyResponseId);
        return results;
    }

    /**
     * @throws GeneralException if getInputByteBuffer fails
     */
    public static Map<String, Object> getAcroFieldsFromPdf(DispatchContext dctx, PdfSurveyServicesContext context) throws GeneralException {
        Map<String, Object> acroFieldMap = new HashMap<>();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Delegator delegator = dctx.getDelegator();
        ByteBuffer byteBuffer = getInputByteBuffer(context, delegator);
        try (PdfReader r = new PdfReader(byteBuffer.array());
                PdfStamper s = new PdfStamper(r, os)) {
            AcroFields fs = s.getAcroFields();
            Map<String, Object> map = UtilGenerics.cast(fs.getAllFields());
            s.setFormFlattening(true);

            for (String fieldName : map.keySet()) {
                String parmValue = fs.getField(fieldName);
                acroFieldMap.put(fieldName, parmValue);
            }

        } catch (DocumentException | IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put(x.acroFieldMap, acroFieldMap);
        return results;
    }

    /**
     */
    public static Map<String, Object> setAcroFields(DispatchContext dctx, PdfSurveyServicesContext context) {
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        try {
            Map<String, Object> acroFieldMap = UtilGenerics.cast(context.get(x.acroFieldMap));
            ByteBuffer byteBuffer = getInputByteBuffer(context, delegator);
            PdfReader r = new PdfReader(byteBuffer.array());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfStamper s = new PdfStamper(r, baos);
            AcroFields fs = s.getAcroFields();
            Map<String, Object> map = UtilGenerics.cast(fs.getAllFields());
            s.setFormFlattening(true);

            for (String fieldName : map.keySet()) {
                String fieldValue = fs.getField(fieldName);
                Object obj = acroFieldMap.get(fieldName);
                if (obj instanceof Date) {
                    Date d = (Date) obj;
                    fieldValue = UtilDateTime.toDateString(d);
                } else if (obj instanceof Long) {
                    Long lg = (Long) obj;
                    fieldValue = lg.toString();
                } else if (obj instanceof Integer) {
                    Integer ii = (Integer) obj;
                    fieldValue = ii.toString();
                } else {
                    fieldValue = (String) obj;
                }

                if (UtilValidate.isNotEmpty(fieldValue)) {
                    fs.setField(fieldName, fieldValue);
                }
            }

            s.close();
            baos.close();
            ByteBuffer outByteBuffer = ByteBuffer.wrap(baos.toByteArray());
            results.put(x.outByteBuffer, outByteBuffer);
        } catch (DocumentException | IOException | GeneralException e) {
            Debug.logError(e, MODULE);
            results = ServiceUtil.returnError(e.getMessage());
        }
        return results;
    }


    /**
     */
    public static Map<String, Object> buildPdfFromSurveyResponse(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        ServiceContext context = new ServiceContext(UtilMisc.makeMapWritable(rcontext));
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        String surveyResponseId = (String) context.get(x.surveyResponseId);
        String contentId = (String) context.get(x.contentId);
        String surveyId = null;

        Document document = new Document();
        try {
            if (UtilValidate.isNotEmpty(surveyResponseId)) {
                GenericValue surveyResponse = getSurveyResponseValue(delegator, surveyResponseId);
                if (surveyResponse != null) {
                    surveyId = surveyResponse.getString(x.surveyId);
                }
            }
            if (UtilValidate.isNotEmpty(surveyId) && UtilValidate.isEmpty(contentId)) {
                GenericValue survey = getSurveyValue(delegator, surveyId);
                if (survey != null) {
                    String acroFormContentId = survey.getString(x.acroFormContentId);
                    if (UtilValidate.isNotEmpty(acroFormContentId)) {
                        context.put(x.contentId, acroFormContentId);
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);

            List<GenericValue> responses = getSurveyResponseAnswerValues(delegator, surveyResponseId);
            for (GenericValue surveyResponseAnswer : responses) {
                String value = null;
                String surveyQuestionId = (String) surveyResponseAnswer.get(x.surveyQuestionId);
                GenericValue surveyQuestion = getSurveyQuestionValue(delegator, surveyQuestionId);
                String questionType = surveyQuestion.getString(x.surveyQuestionTypeId);
                // DEJ20060227 this isn't used, if needed in the future should get from SurveyQuestionAppl.externalFieldRef
                // String fieldName = surveyQuestion.getString("description");
                if (x.OPTION.equals(questionType)) {
                    value = surveyResponseAnswer.getString(x.surveyOptionSeqId);
                } else if (x.BOOLEAN.equals(questionType)) {
                    value = surveyResponseAnswer.getString(x.booleanResponse);
                } else if (x.NUMBER_LONG.equals(questionType) || x.NUMBER_CURRENCY.equals(questionType) || x.NUMBER_FLOAT.equals(questionType)) {
                    Double num = surveyResponseAnswer.getDouble(x.numericResponse);
                    if (num != null) {
                        value = num.toString();
                    }
                } else if (x.SEPERATOR_LINE.equals(questionType) || x.SEPERATOR_TEXT.equals(questionType)) {
                    // not really a question; ignore completely, adding log statement to avoid checkstyle
                    Debug.logInfo(x.Not_really_a_question_ignore_completely_Question_type + questionType, MODULE);
                } else {
                    value = surveyResponseAnswer.getString(x.textResponse);
                }
                Chunk chunk = new Chunk(surveyQuestion.getString(x.question) + x.str_ceca32e9 + value);
                Paragraph p = new Paragraph(chunk);
                document.add(p);
            }
            ByteBuffer outByteBuffer = ByteBuffer.wrap(baos.toByteArray());
            results.put(x.outByteBuffer, outByteBuffer);
        } catch (GenericEntityException | DocumentException e) {
            Debug.logError(e, MODULE);
            results = ServiceUtil.returnError(e.getMessage());
        }

        return results;
    }

    /**
     * Returns list of maps with "question" -&gt; SurveyQuestion and "response" -&gt; SurveyResponseAnswer
     */
    public static Map<String, Object> buildSurveyQuestionsAndAnswers(DispatchContext dctx, PdfSurveyServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        String surveyResponseId = (String) context.get(x.surveyResponseId);
        List<Object> qAndA = new LinkedList<>();

        try {
            List<GenericValue> responses = getSurveyResponseAnswerValues(delegator, surveyResponseId);
            for (GenericValue surveyResponseAnswer : responses) {
                String surveyQuestionId = (String) surveyResponseAnswer.get(x.surveyQuestionId);
                GenericValue surveyQuestion = getSurveyQuestionValue(delegator, surveyQuestionId);
                qAndA.add(UtilMisc.toMap(x.question, surveyQuestion, x.response, surveyResponseAnswer));
            }
            results.put(x.questionsAndAnswers, qAndA);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            results = ServiceUtil.returnError(e.getMessage());
        }

        return results;
    }

    /**
     */
    public static Map<String, Object> setAcroFieldsFromSurveyResponse(DispatchContext dctx, PdfSurveyServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> acroFieldMap = new HashMap<>();
        String surveyResponseId = (String) context.get(x.surveyResponseId);
        String acroFormContentId = null;

        try {
            String surveyId = null;
            if (UtilValidate.isNotEmpty(surveyResponseId)) {
                GenericValue surveyResponse = getSurveyResponseValue(delegator, surveyResponseId);
                if (surveyResponse != null) {
                    surveyId = surveyResponse.getString(x.surveyId);
                }
            }

            if (UtilValidate.isNotEmpty(surveyId)) {
                GenericValue survey = getSurveyValue(delegator, surveyId);
                if (survey != null) {
                    acroFormContentId = survey.getString(x.acroFormContentId);
                }
            }

            List<GenericValue> responses = getSurveyResponseAnswerValues(delegator, surveyResponseId);
            for (GenericValue surveyResponseAnswer : responses) {
                String value = null;
                String surveyQuestionId = (String) surveyResponseAnswer.get(x.surveyQuestionId);

                GenericValue surveyQuestion = getSurveyQuestionValue(delegator, surveyQuestionId);

                GenericValue surveyQuestionAppl = getCurrentSurveyQuestionApplValue(delegator, surveyId, surveyQuestionId);

                String questionType = surveyQuestion.getString(x.surveyQuestionTypeId);
                String fieldName = surveyQuestionAppl.getString(x.externalFieldRef);
                if (x.OPTION.equals(questionType)) {
                    value = surveyResponseAnswer.getString(x.surveyOptionSeqId);
                } else if (x.BOOLEAN.equals(questionType)) {
                    value = surveyResponseAnswer.getString(x.booleanResponse);
                } else if (x.NUMBER_LONG.equals(questionType) || x.NUMBER_CURRENCY.equals(questionType) || x.NUMBER_FLOAT.equals(questionType)) {
                    Double num = surveyResponseAnswer.getDouble(x.numericResponse);
                    if (num != null) {
                        value = num.toString();
                    }
                } else if (x.SEPERATOR_LINE.equals(questionType) || x.SEPERATOR_TEXT.equals(questionType)) {
                    // not really a question; ignore completely, adding log to ignore checkstyle issue
                    Debug.logInfo(x.Not_really_a_question_ignore_completely_Question_type + questionType, MODULE);
                } else {
                    value = surveyResponseAnswer.getString(x.textResponse);
                }
                acroFieldMap.put(fieldName, value);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        try {
            ModelService modelService = dispatcher.getDispatchContext().getModelService(x.setAcroFields);
            Map<String, Object> ctx = modelService.makeValid(context, ModelService.IN_PARAM);
            ctx.put(x.acroFieldMap, acroFieldMap);
            ctx.put(x.contentId, acroFormContentId);
            Map<String, Object> map = dispatcher.runSync(x.setAcroFields, ctx);
            if (ServiceUtil.isError(map)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(map));
            }
            String pdfFileNameOut = (String) context.get(x.pdfFileNameOut);
            ByteBuffer outByteBuffer = (ByteBuffer) map.get(x.outByteBuffer);
            results.put(x.outByteBuffer, outByteBuffer);
            if (UtilValidate.isNotEmpty(pdfFileNameOut)) {
                FileOutputStream fos = new FileOutputStream(pdfFileNameOut);
                fos.write(outByteBuffer.array());
                fos.close();
            }
        } catch (IOException | GenericServiceException e) {
            Debug.logError(e, x.Error_generating_PDF + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentPDFGeneratingError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        return results;
    }

    private static GenericValue getSurveyValue(Delegator delegator, String surveyId) throws GenericEntityException {
        SurveyDao surveyDao = DaoRegistry.getDao(delegator, x.Survey, SurveyDao.class);
        try {
            SurveyEntity surveyEntity = surveyDao.get(surveyId).orElse(null);
            return surveyEntity == null ? null : delegator.makeValue(x.Survey, Beans.beanToMap(surveyEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getSurveyResponseValue(Delegator delegator, String surveyResponseId) throws GenericEntityException {
        SurveyResponseDao surveyResponseDao = DaoRegistry.getDao(delegator, x.SurveyResponse, SurveyResponseDao.class);
        try {
            SurveyResponseEntity surveyResponseEntity = surveyResponseDao.get(surveyResponseId).orElse(null);
            return surveyResponseEntity == null ? null : delegator.makeValue(x.SurveyResponse, Beans.beanToMap(surveyResponseEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static List<GenericValue> getSurveyResponseAnswerValues(Delegator delegator, String surveyResponseId)
            throws GenericEntityException {
        SurveyResponseAnswerDao surveyResponseAnswerDao = DaoRegistry.getDao(delegator, x.SurveyResponseAnswer, SurveyResponseAnswerDao.class);
        List<SurveyResponseAnswerEntity> surveyResponseAnswerEntities;
        try {
            surveyResponseAnswerEntities = surveyResponseAnswerDao.list(Filters.eq(x.surveyResponseId, surveyResponseId));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
        List<GenericValue> values = new LinkedList<>();
        for (SurveyResponseAnswerEntity surveyResponseAnswerEntity : surveyResponseAnswerEntities) {
            values.add(delegator.makeValue(x.SurveyResponseAnswer, Beans.beanToMap(surveyResponseAnswerEntity)));
        }
        return values;
    }

    private static GenericValue getSurveyQuestionValue(Delegator delegator, String surveyQuestionId) throws GenericEntityException {
        SurveyQuestionDao surveyQuestionDao = DaoRegistry.getDao(delegator, x.SurveyQuestion, SurveyQuestionDao.class);
        try {
            SurveyQuestionEntity surveyQuestionEntity = surveyQuestionDao.get(surveyQuestionId).orElse(null);
            return surveyQuestionEntity == null ? null : delegator.makeValue(x.SurveyQuestion, Beans.beanToMap(surveyQuestionEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    private static GenericValue getCurrentSurveyQuestionApplValue(Delegator delegator, String surveyId, String surveyQuestionId)
            throws GenericEntityException {
        SurveyQuestionApplDao surveyQuestionApplDao = DaoRegistry.getDao(delegator, x.SurveyQuestionAppl, SurveyQuestionApplDao.class);
        List<SurveyQuestionApplEntity> surveyQuestionApplEntities;
        try {
            surveyQuestionApplEntities = surveyQuestionApplDao.list(Filters.and(
                    Filters.eq(x.surveyId, surveyId),
                    Filters.eq(x.surveyQuestionId, surveyQuestionId)));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
        List<GenericValue> values = new LinkedList<>();
        for (SurveyQuestionApplEntity surveyQuestionApplEntity : surveyQuestionApplEntities) {
            values.add(delegator.makeValue(x.SurveyQuestionAppl, Beans.beanToMap(surveyQuestionApplEntity)));
        }
        values = EntityUtil.filterByDate(values);
        values = EntityUtil.orderBy(values, UtilMisc.toList(x.fromDate_f5440273));
        return EntityUtil.getFirst(values);
    }

    private static GenericValue getSurveyQuestionAndApplValue(Delegator delegator, String surveyId, String externalFieldRef)
            throws GenericEntityException {
        SurveyQuestionApplDao surveyQuestionApplDao = DaoRegistry.getDao(delegator, x.SurveyQuestionAppl, SurveyQuestionApplDao.class);
        SurveyQuestionDao surveyQuestionDao = DaoRegistry.getDao(delegator, x.SurveyQuestion, SurveyQuestionDao.class);
        SurveyQuestionApplEntity surveyQuestionApplEntity;
        try {
            surveyQuestionApplEntity = surveyQuestionApplDao.list(Filters.and(
                    Filters.eq(x.surveyId, surveyId),
                    Filters.eq(x.externalFieldRef, externalFieldRef))).stream().findFirst().orElse(null);
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
        if (surveyQuestionApplEntity == null) {
            return null;
        }

        Map<String, Object> merged = new HashMap<>(Beans.beanToMap(surveyQuestionApplEntity));
        SurveyQuestionEntity surveyQuestionEntity;
        try {
            surveyQuestionEntity = surveyQuestionDao.get(surveyQuestionApplEntity.getSurveyQuestionId()).orElse(null);
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
        if (surveyQuestionEntity != null) {
            merged.putAll(Beans.beanToMap(surveyQuestionEntity));
        }
        return delegator.makeValue(x.SurveyQuestionAndAppl, merged);
    }

    private static GenericValue getContentValue(Delegator delegator, String contentId) throws GenericEntityException {
        ContentDao contentDao = DaoRegistry.getDao(delegator, x.Content, ContentDao.class);
        try {
            ContentEntity contentEntity = contentDao.get(contentId).orElse(null);
            return contentEntity == null ? null : delegator.makeValue(x.Content, Beans.beanToMap(contentEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }

    public static ByteBuffer getInputByteBuffer(PdfSurveyServicesContext context, Delegator delegator) throws GeneralException {
        ByteBuffer inputByteBuffer = (ByteBuffer) context.get(x.inputByteBuffer);

        if (inputByteBuffer == null) {
            String pdfFileNameIn = (String) context.get(x.pdfFileNameIn);
            String contentId = (String) context.get(x.contentId);
            if (UtilValidate.isNotEmpty(pdfFileNameIn)) {
                try (FileInputStream fis = new FileInputStream(pdfFileNameIn)) {
                    int c;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((c = fis.read()) != -1) {
                        baos.write(c);
                    }
                    inputByteBuffer = ByteBuffer.wrap(baos.toByteArray());
                } catch (IOException e) {
                    throw(new GeneralException(e.getMessage()));
                }
            } else if (UtilValidate.isNotEmpty(contentId)) {
                try {
                    Locale locale = (Locale) context.get(x.locale);
                    String https = (String) context.get(x.https);
                    String webSiteId = (String) context.get(x.webSiteId);
                    String rootDir = (String) context.get(x.rootDir);
                    GenericValue content = getContentValue(delegator, contentId);
                    String dataResourceId = content.getString(x.dataResourceId);
                    inputByteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, dataResourceId, https, webSiteId, locale, rootDir);
                } catch (GenericEntityException | IOException e) {
                    throw(new GeneralException(e.getMessage()));
                }
            }
        }
        return inputByteBuffer;
    }
}

