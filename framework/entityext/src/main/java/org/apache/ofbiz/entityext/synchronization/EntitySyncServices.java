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
package org.apache.ofbiz.entityext.synchronization;

import static org.apache.ofbiz.base.util.UtilGenerics.checkCollection;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.serialize.SerializeException;
import org.apache.ofbiz.entity.serialize.XmlSerializer;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.EntitySyncDao;
import org.apache.ofbiz.persistence.entity.EntitySyncEntity;
import org.apache.ofbiz.entityext.synchronization.EntitySyncContext.SyncAbortException;
import org.apache.ofbiz.entityext.synchronization.EntitySyncContext.SyncErrorException;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.landawn.abacus.jdbc.dao.Dao;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.condition.Condition;
import com.ibm.icu.util.Calendar;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.EntitySyncServicesContext;
/**
 * Entity Engine Sync Services
 */
public class EntitySyncServices {

    private static final String DAO_CLASS_PREFIX = x.org_apache_ofbiz_persistence_dao;
    private static final String DAO_CLASS_SUFFIX = x.Dao;
    private static final String MODULE = EntitySyncServices.class.getName();
    private static final String RESOURCE = x.EntityExtUiLabels;

    /**
     * Run an Entity Sync (checks to see if other already running, etc)
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> runEntitySync(DispatchContext dctx, EntitySyncServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        EntitySyncContext esc = null;
        try {
            esc = new EntitySyncContext(dctx, context);
            if (x.Y.equals(esc.getEntitySync().get(x.forPullOnly))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtCannotDoEntitySyncPush, locale));
            }

            esc.runPushStartRunning();

            // increment starting time to run until now
            esc.setSplitStartTime(); // just run this the first time, will be updated between each loop automatically
            while (esc.hasMoreTimeToSync()) {

                // this will result in lots of log messages, so leaving commented out unless needed/wanted later
                // Debug.logInfo("Doing runEntitySync split, currentRunStartTime=" + esc.currentRunStartTime + ", currentRunEndTime="
                // + esc.currentRunEndTime, MODULE);

                esc.setTotalSplits(esc.getTotalSplits() + 1);

                // tx times are indexed
                // keep track of how long these sync runs take and store that info on the history table
                // saves info about removed, all entities that don't have no-auto-stamp set, this will be done in the GenericDAO like the stamp sets

                // ===== INSERTS =====
                ArrayList<GenericValue> valuesToCreate = esc.assembleValuesToCreate();
                // ===== UPDATES =====
                ArrayList<GenericValue> valuesToStore = esc.assembleValuesToStore();
                // ===== DELETES =====
                List<GenericEntity> keysToRemove = esc.assembleKeysToRemove();

                esc.runPushSendData(valuesToCreate, valuesToStore, keysToRemove);

                esc.saveResultsReportedFromDataStore();
                esc.advanceRunTimes();
            }

            esc.saveFinalSyncResults();

        } catch (SyncAbortException e) {
            return e.returnError(MODULE);
        } catch (SyncErrorException e) {
            e.saveSyncErrorInfo(esc);
            return e.returnError(MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Store Entity Sync Data
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> storeEntitySyncData(DispatchContext dctx, EntitySyncServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String overrideDelegatorName = (String) context.get(x.delegatorName);
        Locale locale = (Locale) context.get(x.locale);
        if (UtilValidate.isNotEmpty(overrideDelegatorName)) {
            delegator = DelegatorFactory.getDelegator(overrideDelegatorName);
            if (delegator == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtCannotFindDelegator,
                        UtilMisc.toMap(x.overrideDelegatorName, overrideDelegatorName), locale));
            }
        }
        //LocalDispatcher dispatcher = dctx.getDispatcher();

        String entitySyncId = (String) context.get(x.entitySyncId);
        // incoming lists will already be sorted by lastUpdatedStamp (or lastCreatedStamp)
        List<GenericValue> valuesToCreate = UtilGenerics.cast(context.get(x.valuesToCreate));
        List<GenericValue> valuesToStore = UtilGenerics.cast(context.get(x.valuesToStore));
        List<GenericEntity> keysToRemove = UtilGenerics.cast(context.get(x.keysToRemove));

        if (Debug.infoOn()) {
            Debug.logInfo(x.Running_storeEntitySyncData + entitySyncId + x.str_1518af21 + valuesToCreate.size() + x.to_create + valuesToStore.size()
                    + x.to_store + keysToRemove.size() + x.to_remove, MODULE);
        }
        try {
            long toCreateInserted = 0;
            long toCreateUpdated = 0;
            long toCreateNotUpdated = 0;
            long toStoreInserted = 0;
            long toStoreUpdated = 0;
            long toStoreNotUpdated = 0;
            long toRemoveDeleted = 0;
            long toRemoveAlreadyDeleted = 0;

            // create all values in the valuesToCreate List; if the value already exists update it, or if exists and was updated more recently
            // than this one dont update it
            for (GenericValue valueToCreate : valuesToCreate) {
                // to Create check if exists (find by pk), if not insert; if exists check lastUpdatedStamp: if null or before the candidate value
                // insert, otherwise don't insert
                // NOTE: use the delegator from this DispatchContext rather than the one named in the GenericValue

                // maintain the original timestamps when doing storage of synced data, by default with will update the timestamps to now
                valueToCreate.setIsFromEntitySync(true);

                // check to make sure all foreign keys are created; if not create dummy values as place holders
                valueToCreate.checkFks(true);

                GenericValue existingValue = queryOneByPrimaryKey(delegator, valueToCreate.getEntityName(), valueToCreate.getPrimaryKey());
                if (existingValue == null) {
                    delegator.create(valueToCreate);
                    toCreateInserted++;
                } else {
                    // if the existing value has a stamp field that is AFTER the stamp on the valueToCreate, don't update it
                    if (existingValue.get(ModelEntity.STAMP_FIELD) != null && existingValue.getTimestamp(ModelEntity.STAMP_FIELD)
                            .after(valueToCreate.getTimestamp(ModelEntity.STAMP_FIELD))) {
                        toCreateNotUpdated++;
                    } else {
                        delegator.store(valueToCreate);
                        toCreateUpdated++;
                    }
                }
            }

            // iterate through to store list and store each
            for (GenericValue valueToStore : valuesToStore) {
                // to store check if exists (find by pk), if not insert; if exists check lastUpdatedStamp: if null or before the candidate value
                // insert, otherwise don't insert

                // maintain the original timestamps when doing storage of synced data, by default with will update the timestamps to now
                valueToStore.setIsFromEntitySync(true);

                // check to make sure all foreign keys are created; if not create dummy values as place holders
                valueToStore.checkFks(true);

                GenericValue existingValue = queryOneByPrimaryKey(delegator, valueToStore.getEntityName(), valueToStore.getPrimaryKey());
                if (existingValue == null) {
                    delegator.create(valueToStore);
                    toStoreInserted++;
                } else {
                    // if the existing value has a stamp field that is AFTER the stamp on the valueToStore, don't update it
                    if (existingValue.get(ModelEntity.STAMP_FIELD) != null && existingValue.getTimestamp(ModelEntity.STAMP_FIELD)
                            .after(valueToStore.getTimestamp(ModelEntity.STAMP_FIELD))) {
                        toStoreNotUpdated++;
                    } else {
                        delegator.store(valueToStore);
                        toStoreUpdated++;
                    }
                }
            }

            // iterate through to remove list and remove each
            for (GenericEntity pkToRemove : keysToRemove) {
                // check to see if it exists, if so remove and count, if not just count already removed
                // always do a removeByAnd, if it was a removeByAnd great, if it was a removeByPrimaryKey, this will also work and save us a query
                pkToRemove.setIsFromEntitySync(true);

                // remove the stamp fields inserted by EntitySyncContext.java at or near line 646
                pkToRemove.remove(ModelEntity.STAMP_TX_FIELD);
                pkToRemove.remove(ModelEntity.STAMP_FIELD);
                pkToRemove.remove(ModelEntity.CREATE_STAMP_TX_FIELD);
                pkToRemove.remove(ModelEntity.CREATE_STAMP_FIELD);

                int numRemByAnd = delegator.removeByAnd(pkToRemove.getEntityName(), pkToRemove);
                if (numRemByAnd == 0) {
                    toRemoveAlreadyDeleted++;
                } else {
                    toRemoveDeleted++;
                }
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.toCreateInserted, toCreateInserted);
            result.put(x.toCreateUpdated, toCreateUpdated);
            result.put(x.toCreateNotUpdated, toCreateNotUpdated);
            result.put(x.toStoreInserted, toStoreInserted);
            result.put(x.toStoreUpdated, toStoreUpdated);
            result.put(x.toStoreNotUpdated, toStoreNotUpdated);
            result.put(x.toRemoveDeleted, toRemoveDeleted);
            result.put(x.toRemoveAlreadyDeleted, toRemoveAlreadyDeleted);
            if (Debug.infoOn()) {
                Debug.logInfo(x.Finisching_storeEntitySyncData + entitySyncId + x.str_1518af21 + keysToRemove.size() + x.to_remove_Actually_removed
                        + toRemoveDeleted + x.already_removed + toRemoveAlreadyDeleted, MODULE);
            }
            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Exception_saving_Entity_Sync_Data_for_entitySyncId + entitySyncId + x.str_89222ecc + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtExceptionSavingEntitySyncData,
                    UtilMisc.toMap(x.entitySyncId, entitySyncId, x.errorString, e.toString()), locale));
        } catch (Throwable t) {
            Debug.logError(t, x.Error_saving_Entity_Sync_Data_for_entitySyncId + entitySyncId + x.str_89222ecc + t.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtErrorSavingEntitySyncData,
                    UtilMisc.toMap(x.entitySyncId, entitySyncId, x.errorString, t.toString()), locale));
        }
    }

    /**
     * Run Pull Entity Sync - Pull From Remote
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> runPullEntitySync(DispatchContext dctx, EntitySyncServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        String entitySyncId = (String) context.get(x.entitySyncId);
        String remotePullAndReportEntitySyncDataName = (String) context.get(x.remotePullAndReportEntitySyncDataName);

        Debug.logInfo(x.Running_runPullEntitySync_for_entitySyncId + context.get(x.entitySyncId), MODULE);

        // loop until no data is returned to store
        boolean gotMoreData = true;

        Timestamp startDate = null;
        Long toCreateInserted = null;
        Long toCreateUpdated = null;
        Long toCreateNotUpdated = null;
        Long toStoreInserted = null;
        Long toStoreUpdated = null;
        Long toStoreNotUpdated = null;
        Long toRemoveDeleted = null;
        Long toRemoveAlreadyDeleted = null;

        while (gotMoreData) {
            gotMoreData = false;

            // call pullAndReportEntitySyncData, initially with no results, then with results from last loop
            Map<String, Object> remoteCallContext = new HashMap<>();
            remoteCallContext.put(x.entitySyncId, entitySyncId);
            remoteCallContext.put(x.delegatorName, context.get(x.remoteDelegatorName));
            remoteCallContext.put(x.userLogin, context.get(x.userLogin));

            remoteCallContext.put(x.startDate, startDate);
            remoteCallContext.put(x.toCreateInserted, toCreateInserted);
            remoteCallContext.put(x.toCreateUpdated, toCreateUpdated);
            remoteCallContext.put(x.toCreateNotUpdated, toCreateNotUpdated);
            remoteCallContext.put(x.toStoreInserted, toStoreInserted);
            remoteCallContext.put(x.toStoreUpdated, toStoreUpdated);
            remoteCallContext.put(x.toStoreNotUpdated, toStoreNotUpdated);
            remoteCallContext.put(x.toRemoveDeleted, toRemoveDeleted);
            remoteCallContext.put(x.toRemoveAlreadyDeleted, toRemoveAlreadyDeleted);

            try {
                Map<String, Object> result = dispatcher.runSync(remotePullAndReportEntitySyncDataName, remoteCallContext);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtErrorCallingRemotePull,
                            UtilMisc.toMap(x.remotePullAndReportEntitySyncDataName, remotePullAndReportEntitySyncDataName), locale), null, null,
                            result);
                }

                startDate = (Timestamp) result.get(x.startDate);

                try {
                    // store data returned, get results (just call storeEntitySyncData locally, get the numbers back and boom shakalaka)

                    // anything to store locally?
                    if (startDate != null && (UtilValidate.isNotEmpty(result.get(x.valuesToCreate))
                            || UtilValidate.isNotEmpty(result.get(x.valuesToStore))
                            || UtilValidate.isNotEmpty(result.get(x.keysToRemove)))) {

                        // yep, we got more data
                        gotMoreData = true;

                        // at least one of the is not empty, make sure none of them are null now too...
                        List<GenericValue> valuesToCreate = checkCollection(result.get(x.valuesToCreate), GenericValue.class);
                        if (valuesToCreate == null) valuesToCreate = Collections.emptyList();
                        List<GenericValue> valuesToStore = checkCollection(result.get(x.valuesToStore), GenericValue.class);
                        if (valuesToStore == null) valuesToStore = Collections.emptyList();
                        List<GenericEntity> keysToRemove = checkCollection(result.get(x.keysToRemove), GenericEntity.class);
                        if (keysToRemove == null) keysToRemove = Collections.emptyList();

                        Map<String, Object> callLocalStoreContext = UtilMisc.toMap(x.entitySyncId, entitySyncId, x.delegatorName,
                                context.get(x.localDelegatorName),
                                x.valuesToCreate, valuesToCreate, x.valuesToStore, valuesToStore,
                                x.keysToRemove, keysToRemove);

                        callLocalStoreContext.put(x.userLogin, context.get(x.userLogin));
                        Map<String, Object> storeResult = dispatcher.runSync(x.storeEntitySyncData, callLocalStoreContext);
                        if (ServiceUtil.isError(storeResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtErrorCallingService, locale),
                                    null, null, storeResult);
                        }

                        // get results for next pass
                        toCreateInserted = (Long) storeResult.get(x.toCreateInserted);
                        toCreateUpdated = (Long) storeResult.get(x.toCreateUpdated);
                        toCreateNotUpdated = (Long) storeResult.get(x.toCreateNotUpdated);
                        toStoreInserted = (Long) storeResult.get(x.toStoreInserted);
                        toStoreUpdated = (Long) storeResult.get(x.toStoreUpdated);
                        toStoreNotUpdated = (Long) storeResult.get(x.toStoreNotUpdated);
                        toRemoveDeleted = (Long) storeResult.get(x.toRemoveDeleted);
                        toRemoveAlreadyDeleted = (Long) storeResult.get(x.toRemoveAlreadyDeleted);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Error_calling_service_to_store_data_locally + e.toString(), MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtErrorCallingService, locale) + e.toString());
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Exception_calling_remote_pull_and_report_EntitySync_service_with_name + remotePullAndReportEntitySyncDataName
                        + x.str_d2d58684 + e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtErrorCallingRemotePull,
                        UtilMisc.toMap(x.remotePullAndReportEntitySyncDataName, remotePullAndReportEntitySyncDataName), locale) + e.toString());
            } catch (Throwable t) {
                Debug.logError(t, x.Error_calling_remote_pull_and_report_EntitySync_service_with_name + remotePullAndReportEntitySyncDataName
                        + x.str_d2d58684 + t.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtErrorCallingRemotePull,
                        UtilMisc.toMap(x.remotePullAndReportEntitySyncDataName, remotePullAndReportEntitySyncDataName), locale) + t.toString());
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Pull and Report Entity Sync Data - Called Remotely to Push Results from last pull, the Pull next set of results.
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> pullAndReportEntitySyncData(DispatchContext dctx, EntitySyncServicesContext context) {
        EntitySyncContext esc = null;
        Locale locale = (Locale) context.get(x.locale);
        try {
            esc = new EntitySyncContext(dctx, context);

            Debug.logInfo(x.Doing_pullAndReportEntitySyncData_for_entitySyncId + esc.getEntitySyncId() + x.currentRunStartTime
                    + esc.getCurrentRunStartTime() + x.currentRunEndTime + esc.getCurrentRunEndTime(), MODULE);

            if (x.Y.equals(esc.getEntitySync().get(x.forPushOnly))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtCannotDoEntitySyncPush, locale));
            }

            // Part 1: if any results are passed, store the results for the given startDate, update EntitySync, etc
            // restore info from last pull, or if no results start new run
            esc.runPullStartOrRestoreSavedResults();


            // increment starting time to run until now
            while (esc.hasMoreTimeToSync()) {
                // make sure the following message is commented out before commit:
                // Debug.logInfo("(loop)Doing pullAndReportEntitySyncData split, currentRunStartTime=" + esc.currentRunStartTime + ",
                // currentRunEndTime=" + esc.currentRunEndTime, MODULE);

                esc.setTotalSplits(esc.getTotalSplits() + 1);

                // tx times are indexed
                // keep track of how long these sync runs take and store that info on the history table
                // saves info about removed, all entities that don't have no-auto-stamp set, this will be done in the GenericDAO like the stamp sets

                // Part 2: get the next set of data for the given entitySyncId
                // Part 2a: return it back for storage but leave the EntitySyncHistory without results, and don't update the EntitySync last time

                // ===== INSERTS =====
                ArrayList<GenericValue> valuesToCreate = esc.assembleValuesToCreate();
                // ===== UPDATES =====
                ArrayList<GenericValue> valuesToStore = esc.assembleValuesToStore();
                // ===== DELETES =====
                List<GenericEntity> keysToRemove = esc.assembleKeysToRemove();

                esc.setTotalRowCounts(valuesToCreate, valuesToStore, keysToRemove);

                if (Debug.infoOn()) {
                    Debug.logInfo(x.Service_pullAndReportEntitySyncData_returning + valuesToCreate.size() + x.to_create
                            + valuesToStore.size() + x.to_store + keysToRemove.size() + x.to_remove_269fed4b + esc.getTotalRowsPerSplit()
                            + x.total_rows_per_split, MODULE);
                }
                if (esc.getTotalRowsPerSplit() > 0) {
                    // stop if we found some data, otherwise look and try again
                    Map<String, Object> result = ServiceUtil.returnSuccess();
                    result.put(x.startDate, esc.getStartDate());
                    result.put(x.valuesToCreate, valuesToCreate);
                    result.put(x.valuesToStore, valuesToStore);
                    result.put(x.keysToRemove, keysToRemove);
                    return result;
                } else {
                    // save the progress to EntitySync and EntitySyncHistory, and move on...
                    esc.saveResultsReportedFromDataStore();
                    esc.advanceRunTimes();
                }
            }

            // if no more results from database to return, save final settings
            if (!esc.hasMoreTimeToSync()) {
                esc.saveFinalSyncResults();
            }
        } catch (SyncAbortException e) {
            return e.returnError(MODULE);
        } catch (SyncErrorException e) {
            e.saveSyncErrorInfo(esc);
            return e.returnError(MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> runOfflineEntitySync(DispatchContext dctx, EntitySyncServicesContext context) {
        String fileName = (String) context.get(x.fileName);
        EntitySyncContext esc = null;
        long totalRowsExported = 0;
        try {
            esc = new EntitySyncContext(dctx, context);

            Debug.logInfo(x.Doing_runManualEntitySync_for_entitySyncId + esc.getEntitySyncId() + x.currentRunStartTime
                    + esc.getCurrentRunStartTime() + x.currentRunEndTime + esc.getCurrentRunEndTime(), MODULE);
            Document mainDoc = UtilXml.makeEmptyXmlDocument(x.xml_entity_synchronization);
            Element docElement = mainDoc.getDocumentElement();
            docElement.setAttribute(x.xml_lang, x.en_US);
            esc.runOfflineStartRunning();

            // increment starting time to run until now
            esc.setSplitStartTime(); // just run this the first time, will be updated between each loop automatically

            while (esc.hasMoreTimeToSync()) {
                esc.setTotalSplits(esc.getTotalSplits() + 1);

                ArrayList<GenericValue> valuesToCreate = esc.assembleValuesToCreate();
                ArrayList<GenericValue> valuesToStore = esc.assembleValuesToStore();
                List<GenericEntity> keysToRemove = esc.assembleKeysToRemove();

                long currentRows = esc.setTotalRowCounts(valuesToCreate, valuesToStore, keysToRemove);
                totalRowsExported += currentRows;

                if (currentRows > 0) {
                    // create the XML document
                    Element syncElement = UtilXml.addChildElement(docElement, x.entity_sync, mainDoc);
                    syncElement.setAttribute(x.entitySyncId, esc.getEntitySyncId());
                    syncElement.setAttribute(x.lastSuccessfulSynchTime, esc.getCurrentRunEndTime().toString());

                    // serialize the list data for XML storage
                    try {
                        UtilXml.addChildElementValue(syncElement, x.values_to_create, XmlSerializer.serialize(valuesToCreate), mainDoc);
                        UtilXml.addChildElementValue(syncElement, x.values_to_store, XmlSerializer.serialize(valuesToStore), mainDoc);
                        UtilXml.addChildElementValue(syncElement, x.keys_to_remove, XmlSerializer.serialize(keysToRemove), mainDoc);
                    } catch (SerializeException e) {
                        throw new EntitySyncContext.SyncOtherErrorException(x.List_serialization_problem, e);
                    } catch (IOException e) {
                        throw new EntitySyncContext.SyncOtherErrorException(x.XML_writing_problem, e);
                    }
                }

                // update the result info
                esc.runSaveOfflineSyncInfo(currentRows);
                esc.advanceRunTimes();
            }

            if (totalRowsExported > 0) {
                // check the file name; use a default if none is passed in
                if (UtilValidate.isEmpty(fileName)) {
                    SimpleDateFormat sdf = new SimpleDateFormat(x.yyyyMMddHHmmss);
                    fileName = x.offline_entitySync + esc.getEntitySyncId() + x.str_3bc15c8a + sdf.format(new Date()) + x.xml_657e4752;
                }

                // write the XML file
                try {
                    UtilXml.writeXmlDocument(fileName, mainDoc);
                } catch (java.io.IOException e) {
                    throw new EntitySyncContext.SyncOtherErrorException(e);
                }
            } else {
                Debug.logInfo(x.No_rows_to_write_no_data_exported, MODULE);
            }

            // save the final results
            esc.saveFinalSyncResults();
        } catch (SyncAbortException e) {
            return e.returnError(MODULE);
        } catch (SyncErrorException e) {
            e.saveSyncErrorInfo(esc);
            return e.returnError(MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> loadOfflineSyncData(DispatchContext dctx, EntitySyncServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String fileName = (String) context.get(x.xmlFileName);
        Locale locale = (Locale) context.get(x.locale);
        URL xmlFile = UtilURL.fromResource(fileName);
        if (xmlFile != null) {
            Document xmlSyncDoc = null;
            try {
                xmlSyncDoc = UtilXml.readXmlDocument(xmlFile, false);
            } catch (SAXException | IOException | ParserConfigurationException e) {
                Debug.logError(e, MODULE);
            }
            if (xmlSyncDoc == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtEntitySyncXMLDocumentIsNotValid,
                        UtilMisc.toMap(x.fileName, fileName), locale));
            }

            List<? extends Element> syncElements = UtilXml.childElementList(xmlSyncDoc.getDocumentElement());
            if (syncElements != null) {
                for (Element entitySync: syncElements) {
                    String entitySyncId = entitySync.getAttribute(x.entitySyncId);
                    String startTime = entitySync.getAttribute(x.lastSuccessfulSynchTime);

                    String createString = UtilXml.childElementValue(entitySync, x.values_to_create);
                    String storeString = UtilXml.childElementValue(entitySync, x.values_to_store);
                    String removeString = UtilXml.childElementValue(entitySync, x.keys_to_remove);

                    // de-serialize the value lists
                    try {
                        List<GenericValue> valuesToCreate = checkCollection(XmlSerializer.deserialize(createString, delegator), GenericValue.class);
                        List<GenericValue> valuesToStore = checkCollection(XmlSerializer.deserialize(storeString, delegator), GenericValue.class);
                        List<GenericEntity> keysToRemove = checkCollection(XmlSerializer.deserialize(removeString, delegator), GenericEntity.class);

                        Map<String, Object> storeContext = UtilMisc.toMap(x.entitySyncId, entitySyncId, x.valuesToCreate, valuesToCreate,
                                x.valuesToStore, valuesToStore, x.keysToRemove, keysToRemove, x.userLogin, userLogin);

                        // store the value(s)
                        Map<String, Object> storeResult = dispatcher.runSync(x.storeEntitySyncData, storeContext);
                        if (ServiceUtil.isError(storeResult)) {
                            throw new GenericServiceException(ServiceUtil.getErrorMessage(storeResult));
                        }

                        // TODO create a response document to send back to the initial sync machine
                    } catch (GenericServiceException | IOException | ParserConfigurationException | SAXException | SerializeException gse) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtUnableToLoadXMLDocument,
                                UtilMisc.toMap(x.entitySyncId, entitySyncId, x.startTime, startTime, x.errorString, gse.getMessage()), locale));
                    }
                }
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtOfflineXMLFileNotFound,
                    UtilMisc.toMap(x.fileName, fileName), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateOfflineEntitySync(DispatchContext dctx, EntitySyncServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtThisServiceIsNotYetImplemented, locale));
    }

    /**
     * Clean EntitySyncRemove Info
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> cleanSyncRemoveInfo(DispatchContext dctx, EntitySyncServicesContext context) {
        Debug.logInfo(x.Running_cleanSyncRemoveInfo, MODULE);
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        try {
            // find the largest keepRemoveInfoHours value on an EntitySyncRemove and kill everything before that,
            // if none found default to 10 days (240 hours)
            double keepRemoveInfoHours = 24;

            EntitySyncDao entitySyncDao = DaoRegistry.getDao(delegator, x.EntitySync, EntitySyncDao.class);
            List<EntitySyncEntity> entitySyncList = entitySyncDao.list(Filters.alwaysTrue());
            for (EntitySyncEntity entitySync : entitySyncList) {
                Double curKrih = entitySync.getKeepRemoveInfoHours();
                if (curKrih != null) {
                    double curKrihVal = curKrih;
                    if (curKrihVal > keepRemoveInfoHours) {
                        keepRemoveInfoHours = curKrihVal;
                    }
                }
            }


            int keepSeconds = (int) Math.floor(keepRemoveInfoHours * 3600);

            Calendar nowCal = Calendar.getInstance();
            nowCal.setTimeInMillis(System.currentTimeMillis());
            nowCal.add(Calendar.SECOND, -keepSeconds);
            Timestamp keepAfterStamp = new Timestamp(nowCal.getTimeInMillis());

            int numRemoved = delegator.removeByCondition(x.EntitySyncRemove, EntityCondition.makeCondition(ModelEntity.STAMP_TX_FIELD,
                    EntityOperator.LESS_THAN, keepAfterStamp));
            Debug.logInfo(x.In_cleanSyncRemoveInfo_removed + numRemoved + x.values_with_TX_timestamp_before + keepAfterStamp + x.str_4ff447b8, MODULE);

            return ServiceUtil.returnSuccess();
        } catch (Exception e) {
            Debug.logError(e, x.Error_cleaning_out_EntitySyncRemove_info + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtErrorCleaningEntitySyncRemove,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
    }

    private static GenericValue queryOneByPrimaryKey(Delegator delegator, String entityName, Map<String, ? extends Object> primaryKey)
            throws GenericEntityException {
        try {
            Dao<?, ?, ?> dao = resolveDao(delegator, entityName);
            List<GenericValue> rows = dao.query(buildPkCondition(primaryKey), (rs, labels) -> toGenericValues(rs, labels, delegator, entityName));
            return rows.isEmpty() ? null : rows.get(0);
        } catch (SQLException e) {
            throw new GenericEntityException(x.Failed_to_query_entity_via_DAO + entityName, e);
        }
    }

    private static Condition buildPkCondition(Map<String, ? extends Object> primaryKey) {
        List<Condition> conditions = new ArrayList<>(primaryKey.size());
        for (Map.Entry<String, ? extends Object> entry : primaryKey.entrySet()) {
            if (entry.getValue() == null) {
                conditions.add(Filters.isNull(entry.getKey()));
            } else {
                conditions.add(Filters.eq(entry.getKey(), entry.getValue()));
            }
        }
        return conditions.isEmpty() ? Filters.alwaysTrue() : Filters.and(conditions);
    }

    private static List<GenericValue> toGenericValues(ResultSet rs, List<String> labels, Delegator delegator, String entityName)
            throws SQLException {
        List<GenericValue> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> fields = new HashMap<>();
            for (int i = 0; i < labels.size(); i++) {
                fields.put(labels.get(i), rs.getObject(i + 1));
            }
            rows.add(delegator.makeValue(entityName, fields));
        }
        return rows;
    }

    @SuppressWarnings({ x.rawtypes, x.unchecked })
    private static Dao<?, ?, ?> resolveDao(Delegator delegator, String entityName) throws GenericEntityException {
        try {
            Class<Dao<?, ?, ?>> daoClass = (Class) Class.forName(DAO_CLASS_PREFIX + entityName + DAO_CLASS_SUFFIX);
            return (Dao<?, ?, ?>) DaoRegistry.getDao(delegator, entityName, (Class) daoClass);
        } catch (ClassNotFoundException e) {
            throw new GenericEntityException(x.No_DAO_implementation_found_for_entity + entityName, e);
        }
    }
}
