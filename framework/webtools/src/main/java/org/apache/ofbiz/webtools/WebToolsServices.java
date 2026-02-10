/*
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
 */
package org.apache.ofbiz.webtools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilPlist;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilProperties.UtilResourceBundle;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelFieldType;
import org.apache.ofbiz.entity.model.ModelIndex;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.model.ModelReader;
import org.apache.ofbiz.entity.model.ModelRelation;
import org.apache.ofbiz.entity.model.ModelUtil;
import org.apache.ofbiz.entity.model.ModelViewEntity;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityDataAssert;
import org.apache.ofbiz.entity.util.EntityDataLoader;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntitySaxReader;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.entityext.EntityGroupUtil;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webtools.artifactinfo.ArtifactInfoFactory;
import org.apache.ofbiz.webtools.artifactinfo.ServiceArtifactInfo;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.WebToolsServicesContext;
/**
 * WebTools Services
 */

public class WebToolsServices {

    private static final String MODULE = WebToolsServices.class.getName();
    private static final String RESOURCE = x.WebtoolsUiLabels;

    public static Map<String, Object> entityImport(DispatchContext dctx, WebToolsServicesContext context) {
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        List<String> messages = new LinkedList<>();

        String filename = (String) context.get(x.filename);
        String fmfilename = (String) context.get(x.fmfilename);
        String fulltext = (String) context.get(x.fulltext);
        boolean isUrl = (String) context.get(x.isUrl) != null;
        String onlyInserts = (String) context.get(x.onlyInserts);
        String maintainTimeStamps = (String) context.get(x.maintainTimeStamps);
        String createDummyFks = (String) context.get(x.createDummyFks);
        String checkDataOnly = (String) context.get(x.checkDataOnly);
        Map<String, Object> placeholderValues = UtilGenerics.cast(context.get(x.placeholderValues));

        Integer txTimeout = (Integer) context.get(x.txTimeout);
        if (txTimeout == null) {
            txTimeout = 7200;
        }
        URL url = null;

        // #############################
        // The filename to parse is prepared
        // #############################
        if (UtilValidate.isNotEmpty(filename)) {
            try {
                url = isUrl ? FlexibleLocation.resolveLocation(filename) : UtilURL.fromFilename(filename);
            } catch (MalformedURLException mue) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsInvalidFileName,
                        UtilMisc.toMap(x.filename, filename, x.errorString, mue.getMessage()), locale));
            } catch (Exception exc) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsErrorReadingFileName,
                        UtilMisc.toMap(x.filename, filename, x.errorString, exc.getMessage()), locale));
            }
        }

        // #############################
        // FM Template
        // #############################
        if (UtilValidate.isUrlInStringAndDoesNotStartByComponentProtocol(fulltext)
                && !x._true.equals(EntityUtilProperties.getPropertyValue(x.security, x.security_datafile_loadurls_enable, x._false, delegator))) {
            Debug.logError(x.For_security_reason_HTTP_URLs_are_not_accepted_see_OFBIZ_12304, MODULE);
            Debug.logInfo(x.Rather_load_your_data_from_a_file_or_set_SystemProperty_security_datafile_loadurls_enable_true, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsErrorDatafileLoadUrlNotEnabled, locale));
        }
        if (UtilValidate.isNotEmpty(fmfilename) && (UtilValidate.isNotEmpty(fulltext) || url != null)) {
            File fmFile = new File(fmfilename);
            if (!fmFile.exists()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsErrorReadingTemplateFile,
                        UtilMisc.toMap(x.filename, fmfilename, x.errorString, x.Template_file_not_found), locale));
            }
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setValidating(true);
                factory.setNamespaceAware(true);

                factory.setAttribute(x.http_xml_org_sax_features_validation, true);
                factory.setAttribute(x.http_apache_org_xml_features_validation_schema, true);

                factory.setFeature(x.http_xml_org_sax_features_external_general_entities, false);
                factory.setFeature(x.http_xml_org_sax_features_external_parameter_entities, false);
                factory.setFeature(x.http_apache_org_xml_features_nonvalidating_load_external_dtd, false);
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);

                DocumentBuilder builder = factory.newDocumentBuilder();
                InputSource ins = url != null ? new InputSource(url.openStream()) : new InputSource(new StringReader(fulltext));
                Document doc;
                try {
                    doc = builder.parse(ins);
                } finally {
                    if (ins.getByteStream() != null) {
                        ins.getByteStream().close();
                    }
                    if (ins.getCharacterStream() != null) {
                        ins.getCharacterStream().close();
                    }
                }
                StringWriter outWriter = new StringWriter();
                Map<String, Object> fmcontext = new HashMap<>();
                fmcontext.put(x.doc, doc);
                FreeMarkerWorker.renderTemplate(fmFile.toURI().toURL().toString(), fmcontext, outWriter);
                fulltext = outWriter.toString();
            } catch (Exception ex) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsErrorProcessingTemplateFile,
                        UtilMisc.toMap(x.filename, fmfilename, x.errorString, ex.getMessage()), locale));
            }
        }

        // #############################
        // The parsing takes place
        // #############################
        if (fulltext != null || url != null) {
            try {
                Map<String, Object> inputMap = UtilMisc.toMap(x.onlyInserts, onlyInserts,
                        x.createDummyFks, createDummyFks,
                        x.checkDataOnly, checkDataOnly,
                        x.maintainTimeStamps, maintainTimeStamps,
                        x.txTimeout, txTimeout,
                        x.placeholderValues, placeholderValues,
                        x.userLogin, userLogin);
                if (fulltext != null) {
                    inputMap.put(x.xmltext, fulltext);
                } else {
                    inputMap.put(x.url, url);
                }
                Map<String, Object> outputMap = dispatcher.runSync(x.parseEntityXmlFile, inputMap);
                if (ServiceUtil.isError(outputMap)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsErrorParsingFile,
                            UtilMisc.toMap(x.errorString, ServiceUtil.getErrorMessage(outputMap)), locale));
                } else {
                    Long numberRead = (Long) outputMap.get(x.rowProcessed);
                    messages.add(UtilProperties.getMessage(RESOURCE, x.EntityImportRowProcessed,
                            UtilMisc.toMap(x.numberRead, numberRead.toString()), locale));
                }
            } catch (GenericServiceException gsex) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityImportParsingError,
                        UtilMisc.toMap(x.errorString, gsex.getMessage()), locale));
            }
        } else {
            messages.add(UtilProperties.getMessage(RESOURCE, x.EntityImportNoXmlFileSpecified, locale));
        }

        // send the notification
        Map<String, Object> resp = UtilMisc.toMap(x.messages, (Object) messages);
        return resp;
    }

    public static Map<String, Object> entityImportDir(DispatchContext dctx, WebToolsServicesContext context) {
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        List<String> messages = new LinkedList<>();

        String path = (String) context.get(x.path);
        String onlyInserts = (String) context.get(x.onlyInserts);
        String maintainTimeStamps = (String) context.get(x.maintainTimeStamps);
        String createDummyFks = (String) context.get(x.createDummyFks);
        boolean deleteFiles = (String) context.get(x.deleteFiles) != null;
        String checkDataOnly = (String) context.get(x.checkDataOnly);
        Map<String, Object> placeholderValues = UtilGenerics.cast(context.get(x.placeholderValues));

        Integer txTimeout = (Integer) context.get(x.txTimeout);
        Long filePause = (Long) context.get(x.filePause);

        if (txTimeout == null) {
            txTimeout = 7200;
        }
        if (filePause == null) {
            filePause = 0L;
        }

        if (UtilValidate.isNotEmpty(path)) {
            long pauseLong = filePause;
            File baseDir = new File(path);

            if (baseDir.isDirectory() && baseDir.canRead()) {
                File[] fileArray = baseDir.listFiles();
                List<File> files = new LinkedList<>();
                if (fileArray == null) {
                    Debug.logError(x.There_are_no_files_to_import_from_this_directory, MODULE);
                    return null;
                }
                for (File file : fileArray) {
                    if (file.getName().toUpperCase().endsWith(x.XML)) {
                        files.add(file);
                    }
                }

                int passes = 0;
                int initialListSize = files.size();
                int lastUnprocessedFilesCount = 0;
                List<File> unprocessedFiles = new LinkedList<>();
                while (!files.isEmpty()
                        && files.size() != lastUnprocessedFilesCount) {
                    lastUnprocessedFilesCount = files.size();
                    unprocessedFiles = new LinkedList<>();
                    for (File f : files) {
                        Map<String, Object> parseEntityXmlFileArgs = UtilMisc.toMap(x.onlyInserts, onlyInserts,
                                x.createDummyFks, createDummyFks,
                                x.checkDataOnly, checkDataOnly,
                                x.maintainTimeStamps, maintainTimeStamps,
                                x.txTimeout, txTimeout,
                                x.placeholderValues, placeholderValues,
                                x.userLogin, userLogin);

                        try {
                            URL furl = f.toURI().toURL();
                            parseEntityXmlFileArgs.put(x.url, furl);
                            Map<String, Object> outputMap = dispatcher.runSync(x.parseEntityXmlFile, parseEntityXmlFileArgs);
                            Long numberRead = (Long) outputMap.get(x.rowProcessed);
                            messages.add(UtilProperties.getMessage(RESOURCE, x.EntityImportNumberOfEntityToBeProcessed,
                                    UtilMisc.toMap(x.numberRead, numberRead.toString(), x.fileName, f.getName()), locale));
                            if (deleteFiles) {
                                messages.add(UtilProperties.getMessage(RESOURCE, x.EntityImportDeletFile, UtilMisc.toMap(x.fileName, f.getName()),
                                        locale));
                                f.delete();
                            }
                        } catch (Exception e) {
                            unprocessedFiles.add(f);
                            messages.add(UtilProperties.getMessage(RESOURCE, x.EntityImportFailedFile, UtilMisc.toMap(x.fileName, f.getName()),
                                    locale));
                        }
                        // pause in between files
                        if (pauseLong > 0) {
                            Debug.logInfo(x.Pausing_for + pauseLong + x.seconds_612f151b + UtilDateTime.nowTimestamp(), MODULE);
                            try {
                                Thread.sleep((pauseLong * 1000));
                            } catch (InterruptedException ie) {
                                Debug.logInfo(x.Pause_finished + UtilDateTime.nowTimestamp(), MODULE);
                            }
                        }
                    }
                    files = unprocessedFiles;
                    passes++;
                    messages.add(UtilProperties.getMessage(RESOURCE, x.EntityImportPassedFile, UtilMisc.toMap(x.passes, passes), locale));
                    Debug.logInfo(x.Pass + passes + x.complete, MODULE);
                }
                lastUnprocessedFilesCount = unprocessedFiles.size();
                messages.add(x.str_d64b2d36);
                messages.add(UtilProperties.getMessage(RESOURCE, x.EntityImportSucceededNumberFile, UtilMisc.toMap(x.succeeded,
                        initialListSize - lastUnprocessedFilesCount, x.total_5a537e20, initialListSize), locale));
                messages.add(UtilProperties.getMessage(RESOURCE, x.EntityImportFailedNumberFile, UtilMisc.toMap(x.failed,
                        lastUnprocessedFilesCount, x.total_5a537e20, initialListSize), locale));
                messages.add(x.str_d64b2d36);
                messages.add(UtilProperties.getMessage(RESOURCE, x.EntityImportFailedFileList, locale));
                for (File file : unprocessedFiles) {
                    messages.add(file.toString());
                }
            } else {
                messages.add(UtilProperties.getMessage(RESOURCE, x.EntityImportPathNotFound, locale));
            }
        } else {
            messages.add(UtilProperties.getMessage(RESOURCE, x.EntityImportPathNotSpecified, locale));
        }
        // send the notification
        Map<String, Object> resp = UtilMisc.toMap(x.messages, (Object) messages);
        return resp;
    }

    public static Map<String, Object> entityImportReaders(DispatchContext dctx, WebToolsServicesContext context) {
        String readers = (String) context.get(x.readers);
        String overrideDelegator = (String) context.get(x.overrideDelegator);
        String overrideGroup = (String) context.get(x.overrideGroup);
        boolean useDummyFks = x._true.equals(context.get(x.createDummyFks));
        boolean maintainTxs = x._true.equals(context.get(x.maintainTimeStamps));
        boolean tryInserts = x._true.equals(context.get(x.onlyInserts));
        boolean checkDataOnly = x._true.equals(context.get(x.checkDataOnly));
        Locale locale = (Locale) context.get(x.locale);
        Integer txTimeoutInt = (Integer) context.get(x.txTimeout);
        int txTimeout = txTimeoutInt != null ? txTimeoutInt : -1;

        List<Object> messages = new LinkedList<>();

        // parse the pass in list of readers to use
        List<String> readerNames = null;
        if (UtilValidate.isNotEmpty(readers) && !x.none.equalsIgnoreCase(readers)) {
            if (readers.indexOf(x.str_5c10b5b2) == -1) {
                readerNames = new LinkedList<>();
                readerNames.add(readers);
            } else {
                readerNames = StringUtil.split(readers, x.str_5c10b5b2);
            }
        }

        String groupNameToUse = overrideGroup != null ? overrideGroup : x.org_apache_ofbiz;
        Delegator delegator = null;
        if (UtilValidate.isNotEmpty(overrideDelegator)) {
            delegator = DelegatorFactory.getDelegator(overrideDelegator);
        } else {
            delegator = dctx.getDelegator();
        }

        String helperName = delegator.getGroupHelperName(groupNameToUse);
        if (helperName == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityImportNoDataSourceSpecified,
                    UtilMisc.toMap(x.groupNameToUse, groupNameToUse), locale));
        }

        // get the reader name URLs first
        List<URL> urlList = null;
        if (readerNames != null) {
            urlList = EntityDataLoader.getUrlList(helperName, readerNames);
        } else if (!x.none.equalsIgnoreCase(readers)) {
            urlList = EntityDataLoader.getUrlList(helperName);
        }

        // need a list if it is empty
        if (urlList == null) {
            urlList = new LinkedList<>();
        }

        // process the list of files
        NumberFormat changedFormat = NumberFormat.getIntegerInstance();
        changedFormat.setMinimumIntegerDigits(5);
        changedFormat.setGroupingUsed(false);

        List<Object> errorMessages = new LinkedList<>();
        List<String> infoMessages = new LinkedList<>();
        int totalRowsChanged = 0;
        if (UtilValidate.isNotEmpty(urlList)) {
            messages.add(x.Doing_a_data + (checkDataOnly ? x.check : x.load) + x.with_the_following_files);
            for (URL dataUrl : urlList) {
                messages.add(dataUrl.toExternalForm());
            }

            messages.add(x.Starting_the_data + (checkDataOnly ? x.check : x.load) + x.str_6eae3a5b);

            for (URL dataUrl : urlList) {
                try {
                    int rowsChanged = 0;
                    if (checkDataOnly) {
                        try {
                            errorMessages.add(x.Checking_data_in + dataUrl.toExternalForm() + x.str_4ff447b8);
                            rowsChanged = EntityDataAssert.assertData(dataUrl, delegator, errorMessages);
                        } catch (SAXException | IOException | ParserConfigurationException e) {
                            errorMessages.add(x.Error_checking_data_in + dataUrl.toExternalForm() + x.str_89222ecc + e.toString());
                        }
                    } else {
                        rowsChanged = EntityDataLoader.loadData(dataUrl, helperName, delegator, errorMessages, txTimeout, useDummyFks, maintainTxs,
                                tryInserts);
                    }
                    totalRowsChanged += rowsChanged;
                    infoMessages.add(changedFormat.format(rowsChanged) + x.of + changedFormat.format(totalRowsChanged) + x._from_0b70336f
                            + dataUrl.toExternalForm());
                } catch (GenericEntityException e) {
                    Debug.logError(e, x.Error_loading_data_file + dataUrl.toExternalForm(), MODULE);
                }
            }
        } else {
            messages.add(x.No_data + (checkDataOnly ? x.check : x.load) + x.files_found);
        }

        if (!infoMessages.isEmpty()) {
            messages.add(x.Here_is_a_summary_of_the_data + (checkDataOnly ? x.check : x.load) + x.str_05a79f06);
            messages.addAll(infoMessages);
        }

        if (!errorMessages.isEmpty()) {
            messages.add(x.The_following_errors_occurred_in_the_data + (checkDataOnly ? x.check : x.load) + x.str_05a79f06);
            messages.addAll(errorMessages);
        }

        messages.add(x.Finished_the_data + (checkDataOnly ? x.check : x.load) + x._with + totalRowsChanged + x.rows
                + (checkDataOnly ? x.checked : x.changed) + x.str_3a52ce78);

        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        resultMap.put(x.messages, messages);
        return resultMap;
    }

    public static Map<String, Object> parseEntityXmlFile(DispatchContext dctx, WebToolsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        URL url = (URL) context.get(x.url);
        String xmltext = (String) context.get(x.xmltext);

        if (url == null && xmltext == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityImportNoXmlFileOrTextSpecified, locale));
        }
        boolean onlyInserts = (String) context.get(x.onlyInserts) != null;
        boolean maintainTimeStamps = (String) context.get(x.maintainTimeStamps) != null;
        boolean createDummyFks = (String) context.get(x.createDummyFks) != null;
        boolean checkDataOnly = (String) context.get(x.checkDataOnly) != null;
        Integer txTimeout = (Integer) context.get(x.txTimeout);
        Map<String, Object> placeholderValues = UtilGenerics.cast(context.get(x.placeholderValues));

        if (txTimeout == null) {
            txTimeout = 7200;
        }

        long rowProcessed = 0;
        try {
            EntitySaxReader reader = new EntitySaxReader(delegator);
            reader.setUseTryInsertMethod(onlyInserts);
            reader.setMaintainTxStamps(maintainTimeStamps);
            reader.setTransactionTimeout(txTimeout);
            reader.setCreateDummyFks(createDummyFks);
            reader.setCheckDataOnly(checkDataOnly);
            reader.setPlaceholderValues(placeholderValues);

            long numberRead = (url != null ? reader.parse(url) : reader.parse(xmltext));
            rowProcessed = numberRead;
        } catch (Exception ex) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityImportParsingError, UtilMisc.toMap(x.errorString,
                    ex.toString()), locale));
        }
        // send the notification
        Map<String, Object> resp = UtilMisc.<String, Object>toMap(x.rowProcessed, rowProcessed);
        return resp;
    }

    public static Map<String, Object> entityExportAll(DispatchContext dctx, WebToolsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        String outpath = (String) context.get(x.outpath); // mandatory
        Timestamp fromDate = (Timestamp) context.get(x.fromDate);
        Integer txTimeout = (Integer) context.get(x.txTimeout);
        if (txTimeout == null) {
            txTimeout = 7200;
        }

        List<String> results = new LinkedList<>();

        if (UtilValidate.isNotEmpty(outpath)) {
            File outdir = new File(outpath);
            if (!outdir.exists()) {
                outdir.mkdir();
            }
            if (outdir.isDirectory() && outdir.canWrite()) {
                Set<String> passedEntityNames;
                try {
                    ModelReader reader = delegator.getModelReader();
                    Collection<String> ec = reader.getEntityNames();
                    passedEntityNames = new TreeSet<>(ec);
                } catch (Exception exc) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityImportErrorRetrievingEntityNames, locale));
                }
                int fileNumber = 1;

                for (String curEntityName : passedEntityNames) {
                    long numberWritten = 0;
                    ModelEntity me = delegator.getModelEntity(curEntityName);
                    if (me instanceof ModelViewEntity) {
                        results.add(x.str_1e5c2f36 + fileNumber + x.vvv + curEntityName + x.skipping_view_entity);
                        continue;
                    }
                    List<EntityCondition> conds = new LinkedList<>();
                    if (UtilValidate.isNotEmpty(fromDate)) {
                        conds.add(EntityCondition.makeCondition(x.createdStamp, EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
                    }
                    EntityCondition whereCond = conds.isEmpty() ? null : EntityCondition.makeCondition(conds, EntityOperator.AND);

                    try {
                        boolean beganTx = TransactionUtil.begin();
                        // some databases don't support cursors, or other problems may happen, so if there is an error here log it and
                        // move on to get as much as possible. Don't bother writing the file if there's nothing to put into it
                        EntityFindOptions findOptions = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE,
                                EntityFindOptions.CONCUR_READ_ONLY, false);
                        try (EntityListIterator values = delegator.find(curEntityName, whereCond, null, null, me.getPkFieldNames(),
                                findOptions)) {
                            GenericValue value = values.next();
                            if (value != null) {
                                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                                        new File(outdir, curEntityName + x.xml_657e4752)), x.UTF_8)))) {
                                    writer.println(x.xml_version_1_0_encoding_UTF_8);
                                    writer.println(x.entity_engine_xml);
                                    do {
                                        value.writeXmlText(writer, x.emptyString);
                                        numberWritten++;
                                        if (numberWritten % 500 == 0) {
                                            TransactionUtil.commit(beganTx);
                                            beganTx = TransactionUtil.begin();
                                        }
                                        value = values.next();
                                    } while (value != null);
                                    writer.println(x.entity_engine_xml_bedbf593);
                                } catch (UnsupportedEncodingException | FileNotFoundException e) {
                                    results.add(x.str_1e5c2f36 + fileNumber + x.xxx_Error_when_writing + curEntityName + x.str_ceca32e9 + e);
                                }
                                results.add(x.str_1e5c2f36 + fileNumber + x.str_7ba1b0d7 + numberWritten + x.str_01af9139 + curEntityName + x.wrote + numberWritten + x.records_c7b997d2);
                            } else {
                                results.add(x.str_1e5c2f36 + fileNumber + x.str_d882b322 + curEntityName + x.has_no_records_not_writing_file);
                            }
                            TransactionUtil.commit(beganTx);
                        } catch (GenericEntityException entityEx) {
                            results.add(x.str_1e5c2f36 + fileNumber + x.xxx_Error_when_writing + curEntityName + x.str_ceca32e9 + entityEx);
                            continue;
                        }
                        fileNumber++;
                    } catch (GenericTransactionException e) {
                        Debug.logError(e, MODULE);
                        results.add(e.getLocalizedMessage());
                    }
                }
            } else {
                results.add(x.Path_not_found_or_no_write_access);
            }
        } else {
            results.add(x.No_path_specified_doing_nothing);
        }
        // send the notification
        Map<String, Object> resp = UtilMisc.<String, Object>toMap(x.results_cdf7e925, results);
        return resp;
    }

    /**
     * Get entity reference data. Returns the number of entities in
     * <code>numberOfEntities</code> and a List of Maps -
     * <code>packagesList</code>.
     * Each Map contains:<br>
     * <ul><li><code>packageName</code> - the entity package name</li>
     * <li><code>entitiesList</code> - a list of Maps:
     * <ul>
     * <li><code>entityName</code></li>
     * <li><code>helperName</code></li>
     * <li><code>groupName</code></li>
     * <li><code>plainTableName</code></li>
     * <li><code>title</code></li>
     * <li><code>description</code></li>
     * <!-- <li><code>location</code></li> -->
     * <li><code>javaNameList</code> - list of Maps:
     * <ul>
     * <li><code>isPk</code></li>
     * <li><code>name</code></li>
     * <li><code>colName</code></li>
     * <li><code>description</code></li>
     * <li><code>type</code></li>
     * <li><code>javaType</code></li>
     * <li><code>sqlType</code></li>
     * </ul>
     * </li>
     * <li><code>relationsList</code> - list of Maps:
     * <ul>
     * <li><code>title</code></li>
     * <!-- <li><code>description</code></li> -->
     * <li><code>relEntity</code></li>
     * <li><code>fkName</code></li>
     * <li><code>type</code></li>
     * <li><code>length</code></li>
     * <li><code>keysList</code> - list of Maps:
     * <ul>
     * <li><code>row</code></li>
     * <li><code>fieldName</code></li>
     * <li><code>relFieldName</code></li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * <li><code>indexList</code> - list of Maps:
     * <ul>
     * <li><code>name</code></li>
     * <!-- <li><code>description</code></li> -->
     * <li><code>fieldNameList</code> - list of Strings</li>
     * </ul>
     * </li>
     * </ul>
     * </li></ul>
     */
    public static Map<String, Object> getEntityRefData(DispatchContext dctx, WebToolsServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();

        ModelReader reader = delegator.getModelReader();
        Map<String, TreeSet<String>> entitiesByPackage = new HashMap<>();
        Set<String> packageNames = new TreeSet<>();
        Set<String> tableNames = new TreeSet<>();

        //put the entityNames TreeSets in a HashMap by packageName
        try {
            Collection<String> ec = reader.getEntityNames();
            resultMap.put(x.numberOfEntities, ec.size());
            for (String eName : ec) {
                ModelEntity ent = reader.getModelEntity(eName);
                //make sure the table name is in the list of all table names, if not null
                if (UtilValidate.isNotEmpty(ent.getPlainTableName())) {
                    tableNames.add(ent.getPlainTableName());
                }
                TreeSet<String> entities = entitiesByPackage.get(ent.getPackageName());
                if (entities == null) {
                    entities = new TreeSet<>();
                    entitiesByPackage.put(ent.getPackageName(), entities);
                    packageNames.add(ent.getPackageName());
                }
                entities.add(eName);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityImportErrorRetrievingEntityNames, locale)
                    + e.getMessage());
        }

        String search = (String) context.get(x.search);
        List<Map<String, Object>> packagesList = new LinkedList<>();
        try {
            for (String pName : packageNames) {
                Map<String, Object> packageMap = new HashMap<>();
                TreeSet<String> entities = entitiesByPackage.get(pName);
                List<Map<String, Object>> entitiesList = new LinkedList<>();
                for (String entityName : entities) {
                    Map<String, Object> entityMap = new HashMap<>();
                    String helperName = delegator.getEntityHelperName(entityName);
                    String groupName = delegator.getEntityGroupName(entityName);
                    if (search == null || entityName.toLowerCase().indexOf(search.toLowerCase()) != -1) {
                        ModelEntity entity = reader.getModelEntity(entityName);
                        ResourceBundle bundle = null;
                        if (UtilValidate.isNotEmpty(entity.getDefaultResourceName())) {
                            try {
                                bundle = UtilResourceBundle.getBundle(entity.getDefaultResourceName(), locale, loader);
                            } catch (Exception exception) {
                                Debug.logInfo(exception.getMessage(), MODULE);
                            }
                        }
                        String entityDescription = null;
                        if (bundle != null) {
                            try {
                                entityDescription = bundle.getString(x.EntityDescription + entity.getEntityName());
                            } catch (Exception exception) {
                                Debug.logWarning(x.EntityDescription_for_entity + entity.getEntityName() + x.is_missing, MODULE);
                            }
                        }
                        if (UtilValidate.isEmpty(entityDescription)) {
                            entityDescription = entity.getDescription();
                        }

                        // fields list
                        List<Map<String, Object>> javaNameList = new LinkedList<>();
                        for (Iterator<ModelField> f = entity.getFieldsIterator(); f.hasNext();) {
                            Map<String, Object> javaNameMap = new HashMap<>();
                            ModelField field = f.next();
                            ModelFieldType type = delegator.getEntityFieldType(entity, field.getType());
                            javaNameMap.put(x.isPk, field.getIsPk());
                            javaNameMap.put(x.name, field.getName());
                            javaNameMap.put(x.colName, field.getColName());
                            String fieldDescription = null;
                            if (bundle != null) {
                                try {
                                    fieldDescription = bundle.getString(x.FieldDescription + entity.getEntityName() + x.str_3a52ce78 + field.getName());
                                } catch (Exception exception) {
                                    Debug.logWarning(x.FieldDescription_for_entity_field + entity.getEntityName() + x.str_3a52ce78
                                            + field.getName() + x.is_missing, MODULE);
                                }
                            }
                            if (UtilValidate.isEmpty(fieldDescription)) {
                                fieldDescription = field.getDescription();
                            }
                            if (UtilValidate.isEmpty(fieldDescription) && bundle != null) {
                                try {
                                    fieldDescription = bundle.getString(x.FieldDescription + field.getName());
                                } catch (Exception exception) {
                                    Debug.logWarning(x.FieldDescription_for_field + field.getName() + x.is_missing, MODULE);
                                }
                            }
                            if (UtilValidate.isEmpty(fieldDescription)) {
                                fieldDescription = ModelUtil.javaNameToDbName(field.getName()).toLowerCase();
                                fieldDescription = ModelUtil.upperFirstChar(fieldDescription.replace('_', ' '));
                            }
                            javaNameMap.put(x.description, fieldDescription);
                            javaNameMap.put(x.type, (field.getType()) != null ? field.getType() : null);
                            javaNameMap.put(x.javaType, (field.getType() != null && type != null) ? type.getJavaType() : x.Undefined);
                            javaNameMap.put(x.sqlType, (type != null && type.getSqlType() != null) ? type.getSqlType() : x.Undefined);
                            javaNameMap.put(x.encrypted, field.getEncryptMethod().isEncrypted());
                            javaNameMap.put(x.encryptMethod, field.getEncryptMethod());
                            javaNameList.add(javaNameMap);
                        }

                        // relations list
                        List<Map<String, Object>> relationsList = new LinkedList<>();
                        for (int r = 0; r < entity.getRelationsSize(); r++) {
                            Map<String, Object> relationMap = new HashMap<>();
                            ModelRelation relation = entity.getRelation(r);
                            List<Map<String, Object>> keysList = new LinkedList<>();
                            int row = 1;
                            for (ModelKeyMap keyMap : relation.getKeyMaps()) {
                                Map<String, Object> keysMap = new HashMap<>();
                                String fieldName = null;
                                String relFieldName = null;
                                if (keyMap.getFieldName().equals(keyMap.getRelFieldName())) {
                                    fieldName = keyMap.getFieldName();
                                    relFieldName = x.aa;
                                } else {
                                    fieldName = keyMap.getFieldName();
                                    relFieldName = keyMap.getRelFieldName();
                                }
                                keysMap.put(x.row, row++);
                                keysMap.put(x.fieldName, fieldName);
                                keysMap.put(x.relFieldName, relFieldName);
                                keysList.add(keysMap);
                            }
                            relationMap.put(x.title, relation.getTitle());
                            relationMap.put(x.description, relation.getDescription());
                            relationMap.put(x.relEntity, relation.getRelEntityName());
                            relationMap.put(x.fkName, relation.getFkName());
                            relationMap.put(x.type, relation.getType());
                            relationMap.put(x.length, relation.getType().length());
                            relationMap.put(x.keysList, keysList);
                            relationsList.add(relationMap);
                        }

                        // index list
                        List<Map<String, Object>> indexList = new LinkedList<>();
                        for (int r = 0; r < entity.getIndexesSize(); r++) {
                            List<String> fieldNameList = new LinkedList<>();

                            ModelIndex index = entity.getIndex(r);
                            for (Iterator<ModelIndex.Field> fieldIterator = index.getFields().iterator(); fieldIterator.hasNext();) {
                                fieldNameList.add(fieldIterator.next().getFieldName());
                            }

                            Map<String, Object> indexMap = new HashMap<>();
                            indexMap.put(x.name, index.getName());
                            indexMap.put(x.description, index.getDescription());
                            indexMap.put(x.fieldNameList, fieldNameList);
                            indexList.add(indexMap);
                        }

                        entityMap.put(x.entityName, entityName);
                        entityMap.put(x.helperName, helperName);
                        entityMap.put(x.groupName, groupName);
                        entityMap.put(x.plainTableName, entity.getPlainTableName());
                        entityMap.put(x.title, entity.getTitle());
                        entityMap.put(x.description, entityDescription);
                        String entityLocation = entity.getLocation();
                        entityLocation = StringUtils.replaceOnce(entityLocation, System.getProperty(x.ofbiz_home) + x.str_42099b4a, x.emptyString);
                        entityMap.put(x.location, entityLocation);
                        entityMap.put(x.javaNameList, javaNameList);
                        entityMap.put(x.relationsList, relationsList);
                        entityMap.put(x.indexList, indexList);
                        entitiesList.add(entityMap);
                    }
                }
                packageMap.put(x.packageName, pName);
                packageMap.put(x.entitiesList, entitiesList);
                packagesList.add(packageMap);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityImportErrorRetrievingEntityNames, locale)
                    + e.getMessage());
        }

        resultMap.put(x.packagesList, packagesList);
        return resultMap;
    }

    public static Map<String, Object> exportEntityEoModelBundle(DispatchContext dctx, WebToolsServicesContext context) {
        String eomodeldFullPath = (String) context.get(x.eomodeldFullPath);
        String entityPackageNameOrig = (String) context.get(x.entityPackageName);
        String entityGroupId = (String) context.get(x.entityGroupId);
        String datasourceName = (String) context.get(x.datasourceName);
        String entityNamePrefix = (String) context.get(x.entityNamePrefix);
        Locale locale = (Locale) context.get(x.locale);
        if (datasourceName == null) datasourceName = x.localderby;

        ModelReader reader = dctx.getDelegator().getModelReader();

        try {
            if (!eomodeldFullPath.endsWith(x.eomodeld)) {
                eomodeldFullPath = eomodeldFullPath + x.eomodeld;
            }

            File outdir = new File(eomodeldFullPath);
            if (!outdir.exists()) {
                outdir.mkdir();
            }
            if (!outdir.isDirectory()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsEomodelFullPathIsNotADirectory,
                        UtilMisc.toMap(x.eomodeldFullPath, eomodeldFullPath), locale));
            }
            if (!outdir.canWrite()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsEomodelFullPathIsNotWriteable,
                        UtilMisc.toMap(x.eomodeldFullPath, eomodeldFullPath), locale));
            }

            Set<String> entityNames = new TreeSet<>();
            if (UtilValidate.isNotEmpty(entityPackageNameOrig)) {
                Set<String> entityPackageNameSet = new HashSet<>();
                entityPackageNameSet.addAll(StringUtil.split(entityPackageNameOrig, x.str_5c10b5b2));

                Debug.logInfo(x.Exporting_with_entityPackageNameSet + entityPackageNameSet, MODULE);

                Map<String, TreeSet<String>> entitiesByPackage = reader.getEntitiesByPackage(entityPackageNameSet, null);
                for (Map.Entry<String, TreeSet<String>> entitiesByPackageMapEntry : entitiesByPackage.entrySet()) {
                    entityNames.addAll(entitiesByPackageMapEntry.getValue());
                }
            } else if (UtilValidate.isNotEmpty(entityGroupId)) {
                Debug.logInfo(x.Exporting_entites_from_the_Group + entityGroupId, MODULE);
                entityNames.addAll(EntityGroupUtil.getEntityNamesByGroup(entityGroupId, dctx.getDelegator(), false));
            } else {
                entityNames.addAll(reader.getEntityNames());
            }
            Debug.logInfo(x.Exporting_the_following_entities + entityNames, MODULE);

            // remove all view-entity
            Iterator<String> filterEntityNameIter = entityNames.iterator();
            while (filterEntityNameIter.hasNext()) {
                String entityName = filterEntityNameIter.next();
                ModelEntity modelEntity = reader.getModelEntity(entityName);
                if (modelEntity instanceof ModelViewEntity) {
                    filterEntityNameIter.remove();
                }
            }

            // write the index.eomodeld file
            Map<String, Object> topLevelMap = new HashMap<>();
            topLevelMap.put(x.EOModelVersion, x._2_1);
            List<Map<String, Object>> entitiesMapList = new LinkedList<>();
            topLevelMap.put(x.entities_9d88f3cc, entitiesMapList);
            for (String entityName : entityNames) {
                Map<String, Object> entitiesMap = new HashMap<>();
                entitiesMapList.add(entitiesMap);
                entitiesMap.put(x.className, x.EOGenericRecord);
                entitiesMap.put(x.name, entityName);
            }
            UtilPlist.writePlistFile(topLevelMap, eomodeldFullPath, x.index_eomodeld, true);

            // write each <EntityName>.plist file
            for (String curEntityName : entityNames) {
                ModelEntity modelEntity = reader.getModelEntity(curEntityName);
                UtilPlist.writePlistFile(modelEntity.createEoModelMap(entityNamePrefix, datasourceName, entityNames, reader), eomodeldFullPath,
                        curEntityName + x.plist, true);
            }
            Integer entityNamesSize = entityNames.size();
            return ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE, x.WebtoolsEomodelExported,
                    UtilMisc.toMap(x.entityNamesSize, entityNamesSize.toString(), x.eomodeldFullPath, eomodeldFullPath), locale));
        } catch (UnsupportedEncodingException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsEomodelSavingFileError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        } catch (FileNotFoundException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsEomodelFileOrDirectoryNotFound,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsEomodelErrorGettingEntityNames,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
    }

    /**
     * Performs an entity maintenance security check. Returns hasPermission=true
     * if the user has the ENTITY_MAINT permission.
     * @param dctx    the dispatch context
     * @param context the context
     * @return return the result of the service execution
     */
    public static Map<String, Object> entityMaintPermCheck(DispatchContext dctx, WebToolsServicesContext context) {
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Security security = dctx.getSecurity();
        Map<String, Object> resultMap = null;
        if (security.hasPermission(x.ENTITY_MAINT, userLogin)) {
            resultMap = ServiceUtil.returnSuccess();
            resultMap.put(x.hasPermission, true);
        } else {
            resultMap = ServiceUtil.returnFailure(UtilProperties.getMessage(RESOURCE, x.WebtoolsPermissionError, locale));
            resultMap.put(x.hasPermission, false);
        }
        return resultMap;
    }


    public static Map<String, Object> exportServiceEoModelBundle(DispatchContext dctx, WebToolsServicesContext context) {
        String eomodeldFullPath = (String) context.get(x.eomodeldFullPath);
        String serviceName = (String) context.get(x.serviceName);
        Locale locale = (Locale) context.get(x.locale);

        if (eomodeldFullPath.endsWith(x.str_42099b4a)) {
            eomodeldFullPath = eomodeldFullPath + serviceName + x.eomodeld;
        }

        if (!eomodeldFullPath.endsWith(x.eomodeld)) {
            eomodeldFullPath = eomodeldFullPath + x.eomodeld;
        }

        File outdir = new File(eomodeldFullPath);
        if (!outdir.exists()) {
            outdir.mkdir();
        }
        if (!outdir.isDirectory()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsEomodelFullPathIsNotADirectory,
                    UtilMisc.toMap(x.eomodeldFullPath, eomodeldFullPath), locale));
        }
        if (!outdir.canWrite()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsEomodelFullPathIsNotWriteable,
                    UtilMisc.toMap(x.eomodeldFullPath, eomodeldFullPath), locale));
        }

        try {
            ArtifactInfoFactory aif = ArtifactInfoFactory.getArtifactInfoFactory(x._default);
            ServiceArtifactInfo serviceInfo = aif.getServiceArtifactInfo(serviceName);
            serviceInfo.writeServiceCallGraphEoModel(eomodeldFullPath);
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsEomodelErrorGettingEntityNames,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        } catch (UnsupportedEncodingException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsEomodelSavingFileError,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        } catch (FileNotFoundException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.WebtoolsEomodelFileOrDirectoryNotFound,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }
}
