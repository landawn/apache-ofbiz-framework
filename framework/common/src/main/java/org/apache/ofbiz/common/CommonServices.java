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
package org.apache.ofbiz.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.internet.MimeMessage;

import org.apache.ofbiz.base.metrics.Metrics;
import org.apache.ofbiz.base.metrics.MetricsFactory;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.SequenceValueItemDao;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceSynchronization;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.mail.MimeMessageWrapper;
import com.landawn.abacus.query.Filters;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.CommonServicesContext;
/**
 * Common Services
 */
public class CommonServices {

    private static final String MODULE = CommonServices.class.getName();
    private static final String RESOURCE = x.CommonUiLabels;

    /**
     * Generic Test Service
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> testService(DispatchContext dctx, CommonServicesContext context) {
        Map<String, Object> response = ServiceUtil.returnSuccess();

        if (!context.isEmpty()) {
            for (Map.Entry<String, ?> entry: context.entrySet()) {
                Object cKey = entry.getKey();
                Object value = entry.getValue();

                Debug.logInfo(x.SVC_CONTEXT + cKey + x.str_d8705abf + value, MODULE);
            }
        }
        if (!context.containsKey(x.message)) {
            response.put(x.resp, x.no_message_found);
        } else {
            Debug.logInfo(x.SERVICE_TEST + (String) context.get(x.message), MODULE);
            response.put(x.resp, x.service_done);
        }

        Debug.logInfo(x.SVC + dctx.getName() + x.str_e0851ade, MODULE);
        return response;
    }

    /**
     * Generic Test SOAP Service
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> testSOAPService(DispatchContext dctx, CommonServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> response = ServiceUtil.returnSuccess();

        List<GenericValue> testingNodes = new LinkedList<>();
        for (int i = 0; i < 3; i++) {
            GenericValue testingNode = delegator.makeValue(x.TestingNode);
            testingNode.put(x.testingNodeId, x.TESTING_NODE + i);
            testingNode.put(x.description, x.Testing_Node + i);
            testingNode.put(x.createdStamp, UtilDateTime.nowTimestamp());
            testingNodes.add(testingNode);
        }
        response.put(x.testingNodes, testingNodes);
        return response;
    }

    public static Map<String, Object> blockingTestService(DispatchContext dctx, CommonServicesContext context) {
        Long duration = (Long) context.get(x.duration);
        if (duration == null) {
            duration = 30000L;
        }
        Debug.logInfo(x.SERVICE_BLOCKING + duration / 1000d + x.seconds, MODULE);
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
        }
        return CommonServices.testService(dctx, context);
    }

    public static Map<String, Object> testRollbackListener(DispatchContext dctx, CommonServicesContext context) {
        try {
            ServiceSynchronization.registerRollbackService(dctx, x.testScv, null, context, false, false);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
        Locale locale = (Locale) context.get(x.locale);
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonTestRollingBack, locale));
    }

    public static Map<String, Object> testCommitListener(DispatchContext dctx, CommonServicesContext context) {
        try {
            ServiceSynchronization.registerCommitService(dctx, x.testScv, null, context, false, false);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Create Note Record
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createNote(DispatchContext ctx, CommonServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Timestamp noteDate = (Timestamp) context.get(x.noteDate);
        String partyId = (String) context.get(x.partyId);
        String noteName = (String) context.get(x.noteName);
        String note = (String) context.get(x.note);
        String noteId = delegator.getNextSeqId(x.NoteData);
        Locale locale = (Locale) context.get(x.locale);
        if (noteDate == null) {
            noteDate = UtilDateTime.nowTimestamp();
        }


        // check for a party id
        if (partyId == null) {
            if (userLogin != null && userLogin.get(x.partyId) != null) {
                partyId = userLogin.getString(x.partyId);
            }
        }

        Map<String, Object> fields = UtilMisc.toMap(x.noteId, noteId, x.noteName, noteName, x.noteInfo, note,
                x.noteParty, partyId, x.noteDateTime, noteDate);

        try {
            GenericValue newValue = delegator.makeValue(x.NoteData, fields);

            delegator.create(newValue);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonNoteCannotBeUpdated,
                    UtilMisc.toMap(x.errorString, e.getMessage()), locale));
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put(x.noteId, noteId);
        result.put(x.partyId, partyId);
        return result;
    }

    /**
     * Service for setting debugging levels.
     *@param dctc The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> adjustDebugLevels(DispatchContext dctc, CommonServicesContext context) {
        Debug.set(Debug.FATAL, x.Y.equalsIgnoreCase((String) context.get(x.fatal)));
        Debug.set(Debug.ERROR, x.Y.equalsIgnoreCase((String) context.get(x.error)));
        Debug.set(Debug.WARNING, x.Y.equalsIgnoreCase((String) context.get(x.warning)));
        Debug.set(Debug.IMPORTANT, x.Y.equalsIgnoreCase((String) context.get(x.important)));
        Debug.set(Debug.INFO, x.Y.equalsIgnoreCase((String) context.get(x.info)));
        Debug.set(Debug.TIMING, x.Y.equalsIgnoreCase((String) context.get(x.timing)));
        Debug.set(Debug.VERBOSE, x.Y.equalsIgnoreCase((String) context.get(x.verbose)));

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> forceGc(DispatchContext dctx, CommonServicesContext context) {
        System.gc();
        return ServiceUtil.returnSuccess();
    }

    /**
     * Echo service; returns exactly what was sent.
     * This service does not have required parameters and does not validate
     */
    public static Map<String, Object> echoService(DispatchContext dctx, CommonServicesContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.putAll(context);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Return Error Service; Used for testing error handling
     */
    public static Map<String, Object> returnErrorService(DispatchContext dctx, CommonServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonServiceReturnError, locale));
    }

    /**
     * Return TRUE Service; ECA Condition Service
     */
    public static Map<String, Object> conditionTrueService(DispatchContext dctx, CommonServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.conditionReply, Boolean.TRUE);
        return result;
    }

    /**
     * Return FALSE Service; ECA Condition Service
     */
    public static Map<String, Object> conditionFalseService(DispatchContext dctx, CommonServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.conditionReply, Boolean.FALSE);
        return result;
    }

    /** Cause a Referential Integrity Error */
    public static Map<String, Object> entityFailTest(DispatchContext dctx, CommonServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        // attempt to create a DataSource entity w/ an invalid dataSourceTypeId
        GenericValue newEntity = delegator.makeValue(x.DataSource);
        newEntity.set(x.dataSourceId, x.ENTITY_FAIL_TEST);
        newEntity.set(x.dataSourceTypeId, x.ENTITY_FAIL_TEST);
        newEntity.set(x.description, x.Entity_Fail_Test_Delete_me_if_I_am_here);
        try {
            delegator.create(newEntity);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonEntityTestFailure, locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /** Test entity sorting */
    public static Map<String, Object> entitySortTest(DispatchContext dctx, CommonServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Set<ModelEntity> set = new TreeSet<>();

        set.add(delegator.getModelEntity(x.Person));
        set.add(delegator.getModelEntity(x.PartyRole));
        set.add(delegator.getModelEntity(x.Party));
        set.add(delegator.getModelEntity(x.ContactMech));
        set.add(delegator.getModelEntity(x.PartyContactMech));
        set.add(delegator.getModelEntity(x.OrderHeader));
        set.add(delegator.getModelEntity(x.OrderItem));
        set.add(delegator.getModelEntity(x.OrderContactMech));
        set.add(delegator.getModelEntity(x.OrderRole));
        set.add(delegator.getModelEntity(x.Product));
        set.add(delegator.getModelEntity(x.RoleType));

        for (ModelEntity modelEntity: set) {
            Debug.logInfo(modelEntity.getEntityName(), MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> makeALotOfVisits(DispatchContext dctx, CommonServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        int count = (Integer) context.get(x.count);

        for (int i = 0; i < count; i++) {
            GenericValue v = delegator.makeValue(x.Visit);
            String seqId = delegator.getNextSeqId(x.Visit);

            v.set(x.visitId, seqId);
            v.set(x.userCreated, x.N);
            v.set(x.sessionId, x.NA_97a00357 + seqId);
            v.set(x.serverIpAddress, x._127_0_0_1);
            v.set(x.serverHostName, x.localhost);
            v.set(x.webappName, x.webtools);
            v.set(x.initialLocale, x.en_US_fa73905e);
            v.set(x.initialRequest, x.https_localhost_8443_webtools_control_main);
            v.set(x.initialReferrer, x.https_localhost_8443_webtools_control_main);
            v.set(x.initialUserAgent, x.Mozilla_5_0_Macintosh_U_PPC_Mac_OS_X_en_us_AppleWebKit_124_KHTML_like_Gecko_Safari_125_1);
            v.set(x.clientIpAddress, x._127_0_0_1);
            v.set(x.clientHostName, x.localhost);
            v.set(x.fromDate, UtilDateTime.nowTimestamp());

            try {
                delegator.create(v);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> displayXaDebugInfo(DispatchContext dctx, CommonServicesContext context) {
        if (TransactionUtil.debugResources()) {
            if (UtilValidate.isNotEmpty(TransactionUtil.DEBUG_RES_MAP)) {
                TransactionUtil.logRunningTx();
            } else {
                Debug.logInfo(x.No_running_transaction_to_display, MODULE);
            }
        } else {
            Debug.logInfo(x.Debug_resources_is_disabled, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> byteBufferTest(DispatchContext dctx, CommonServicesContext context) {
        ByteBuffer buffer1 = (ByteBuffer) context.get(x.byteBuffer1);
        ByteBuffer buffer2 = (ByteBuffer) context.get(x.byteBuffer2);
        String fileName1 = (String) context.get(x.saveAsFileName1);
        String fileName2 = (String) context.get(x.saveAsFileName2);
        String ofbizHome = System.getProperty(x.ofbiz_home);
        String outputPath1 = ofbizHome + (fileName1.startsWith(x.str_42099b4a) ? fileName1 : x.str_42099b4a + fileName1);
        String outputPath2 = ofbizHome + (fileName2.startsWith(x.str_42099b4a) ? fileName2 : x.str_42099b4a + fileName2);
        RandomAccessFile file1 = null;
        RandomAccessFile file2 = null;

        try {
            file1 = new RandomAccessFile(outputPath1, x.rw);
            file2 = new RandomAccessFile(outputPath2, x.rw);
            file1.write(buffer1.array());
            file2.write(buffer2.array());
        } catch (IOException e) {
            Debug.logError(e, MODULE);
        } finally {
            try {
                file1.close();
                file2.close();
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> uploadTest(DispatchContext dctx, CommonServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        byte[] array = (byte[]) context.get(x.uploadFile);
        String fileName = (String) context.get(x._uploadFile_fileName);
        String contentType = (String) context.get(x._uploadFile_contentType);

        Map<String, Object> createCtx = new LinkedHashMap<>();
        createCtx.put(x.binData, array);
        createCtx.put(x.dataResourceTypeId, x.OFBIZ_FILE);
        createCtx.put(x.dataResourceName, fileName);
        createCtx.put(x.dataCategoryId, x.PERSONAL);
        createCtx.put(x.statusId, x.CTNT_PUBLISHED);
        createCtx.put(x.mimeTypeId, contentType);
        createCtx.put(x.userLogin, userLogin);

        Map<String, Object> createResp = null;
        try {
            createResp = dispatcher.runSync(x.createFile, createCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(createResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createResp));
        }

        GenericValue dataResource = (GenericValue) createResp.get(x.dataResource);
        if (dataResource != null) {
            Map<String, Object> contentCtx = new LinkedHashMap<>();
            contentCtx.put(x.dataResourceId, dataResource.getString(x.dataResourceId));
            contentCtx.put(x.localeString, ((Locale) context.get(x.locale)).toString());
            contentCtx.put(x.contentTypeId, x.DOCUMENT);
            contentCtx.put(x.mimeTypeId, contentType);
            contentCtx.put(x.contentName, fileName);
            contentCtx.put(x.statusId, x.CTNT_PUBLISHED);
            contentCtx.put(x.userLogin, userLogin);

            Map<String, Object> contentResp = null;
            try {
                contentResp = dispatcher.runSync(x.createContent, contentCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(contentResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(contentResp));
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> mcaTest(DispatchContext dctx, CommonServicesContext context) {
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get(x.messageWrapper);
        MimeMessage message = wrapper.getMessage();
        try {
            if (message.getAllRecipients() != null) {
                Debug.logInfo(x.To_52cea31d + UtilMisc.toListArray(message.getAllRecipients()), MODULE);
            }
            if (message.getFrom() != null) {
                Debug.logInfo(x.From_b4f579b4 + UtilMisc.toListArray(message.getFrom()), MODULE);
            }
            Debug.logInfo(x.Subject_27b6d84a + message.getSubject(), MODULE);
            if (message.getSentDate() != null) {
                Debug.logInfo(x.Sent + message.getSentDate().toString(), MODULE);
            }
            if (message.getReceivedDate() != null) {
                Debug.logInfo(x.Received + message.getReceivedDate().toString(), MODULE);
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> streamTest(DispatchContext dctx, CommonServicesContext context) {
        InputStream in = (InputStream) context.get(x.inputStream);
        OutputStream out = (OutputStream) context.get(x.outputStream);

        String line;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            while ((line = reader.readLine()) != null) {
                Debug.logInfo(x.Read_line + line, MODULE);
                writer.write(line);
            }
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.contentType, x.text_plain);
        return result;
    }

    public static Map<String, Object> ping(DispatchContext dctx, CommonServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String message = (String) context.get(x.message);
        Locale locale = (Locale) context.get(x.locale);
        if (message == null) {
            message = x.PONG;
        }

        long count;
        try {
            SequenceValueItemDao sequenceValueItemDao = DaoRegistry.getDao(delegator, x.SequenceValueItem, SequenceValueItemDao.class);
            count = sequenceValueItemDao.count(Filters.alwaysTrue());
        } catch (Exception e) {
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonPingDatasourceCannotConnect, locale));
        }

        if (count != 0L) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.message, message);
            return result;
        }
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonPingDatasourceInvalidCount, locale));
    }

    public static Map<String, Object> getAllMetrics(DispatchContext dctx, CommonServicesContext context) {
        List<Map<String, Object>> metricsMapList = new LinkedList<>();
        Collection<Metrics> metricsList = MetricsFactory.getMetrics();
        for (Metrics metrics : metricsList) {
            Map<String, Object> metricsMap = new LinkedHashMap<>();
            metricsMap.put(x.name, metrics.getName());
            metricsMap.put(x.serviceRate, metrics.getServiceRate());
            metricsMap.put(x.threshold, metrics.getThreshold());
            metricsMap.put(x.totalEvents, metrics.getTotalEvents());
            metricsMapList.add(metricsMap);
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.metricsList, metricsMapList);
        return result;
    }

    public static Map<String, Object> resetMetric(DispatchContext dctx, CommonServicesContext context) {
        String originalName = (String) context.get(x.name);
        Locale locale = (Locale) context.get(x.locale);
        String name = UtilCodec.getDecoder(x.url).decode(originalName);
        if (name == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonExceptionThrownWhileDecodingMetric,
                    UtilMisc.toMap(x.originalName, originalName), locale));
        }
        Metrics metric = MetricsFactory.getMetric(name);
        if (metric != null) {
            metric.reset();
            return ServiceUtil.returnSuccess();
        }
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.CommonMetricNotFound, UtilMisc.toMap(x.name, name), locale));
    }
}

