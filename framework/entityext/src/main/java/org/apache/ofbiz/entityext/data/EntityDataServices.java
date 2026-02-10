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
package org.apache.ofbiz.entityext.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.ofbiz.base.crypto.DesCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;
import org.apache.ofbiz.entity.jdbc.DatabaseUtil;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.EntityKeyStoreDao;
import org.apache.ofbiz.persistence.entity.EntityKeyStoreEntity;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.shiro.crypto.cipher.AesCipherService;

import com.landawn.abacus.jdbc.dao.Dao;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.EntityDataServicesContext;
/**
 * Entity Data Import/Export Services
 *
 */
public class EntityDataServices {

    private static final String DAO_CLASS_PREFIX = x.org_apache_ofbiz_persistence_dao;
    private static final String DAO_CLASS_SUFFIX = x.Dao;
    private static final String MODULE = EntityDataServices.class.getName();
    private static final String RESOURCE = x.EntityExtUiLabels;

    public static Map<String, Object> exportDelimitedToDirectory(DispatchContext dctx, EntityDataServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtThisServiceIsNotYetImplemented, locale));
    }

    public static Map<String, Object> importDelimitedFromDirectory(DispatchContext dctx, EntityDataServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get(x.locale);

        // check permission
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        if (!security.hasPermission(x.ENTITY_MAINT, userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtServicePermissionNotGranted, locale));
        }

        // get the directory & delimiter
        String rootDirectory = (String) context.get(x.rootDirectory);
        URL rootDirectoryUrl = UtilURL.fromResource(rootDirectory);
        if (rootDirectoryUrl == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtUnableToLocateRootDirectory,
                    UtilMisc.toMap(x.rootDirectory, rootDirectory), locale));
        }

        String delimiter = (String) context.get(x.delimiter);
        if (delimiter == null) {
            // default delimiter is tab
            delimiter = x.str_ac9231da;
        }

        File root = null;
        try {
            root = new File(new URI(rootDirectoryUrl.toExternalForm()));
        } catch (URISyntaxException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtUnableToLocateRootDirectoryURI, locale));
        }

        if (!root.exists() || !root.isDirectory() || !root.canRead()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtRootDirectoryDoesNotExists, locale));
        }

        // get the file list
        List<File> files = getFileList(root);
        if (UtilValidate.isNotEmpty(files)) {
            for (File file: files) {
                try {
                    Map<String, Object> serviceCtx = UtilMisc.toMap(x.file, file, x.delimiter, delimiter, x.userLogin, userLogin);
                    dispatcher.runSyncIgnore(x.importDelimitedEntityFile, serviceCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                }
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtNoFileAvailableInTheRootDirectory,
                    UtilMisc.toMap(x.rootDirectory, rootDirectory), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> importDelimitedFile(DispatchContext dctx, EntityDataServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get(x.locale);

        // check permission
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        if (!security.hasPermission(x.ENTITY_MAINT, userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtServicePermissionNotGranted, locale));
        }

        String delimiter = (String) context.get(x.delimiter);
        if (delimiter == null) {
            // default delimiter is tab
            delimiter = x.str_ac9231da;
        }

        long startTime = System.currentTimeMillis();

        File file = (File) context.get(x.file);
        int records = 0;
        try {
            records = readEntityFile(file, delimiter, delegator);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (FileNotFoundException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtFileNotFound, UtilMisc.toMap(x.fileName,
                    file.getName()), locale));
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtProblemReadingFile,
                    UtilMisc.toMap(x.fileName, file.getName()), locale));
        }

        long endTime = System.currentTimeMillis();
        long runTime = endTime - startTime;

        Debug.logInfo(x.Imported_Updated + records + x._from_4414a702 + file.getAbsolutePath() + x.str_42cbdb3c + runTime + x.ms, MODULE);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.records, records);
        return result;
    }

    private static List<File> getFileList(File root) {
        List<File> fileList = new LinkedList<>();

        // check for a file list file
        File listFile = new File(root, x.FILELIST_txt);
        Debug.logInfo(x.Checking_file_list + listFile.getPath(), MODULE);
        if (listFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(listFile), StandardCharsets.UTF_8));
            } catch (FileNotFoundException e) {
                Debug.logError(e, MODULE);
            }
            if (reader != null) {
                // read each line as a file name to load
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        File thisFile = new File(root, line);
                        if (thisFile.exists()) {
                            fileList.add(thisFile);
                        }
                    }
                } catch (IOException e) {
                    Debug.logError(e, MODULE);
                }

                // close the reader
                try {
                    reader.close();
                } catch (IOException e) {
                    Debug.logError(e, MODULE);
                }
                Debug.logInfo(x.Read_file_list + fileList.size() + x.entities, MODULE);
            }
        } else {
            File[] files = root.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (!fileName.startsWith(x.str_53a0acfa) && fileName.endsWith(x.txt)) {
                        fileList.add(file);
                    }
                }
            }
            Debug.logInfo(x.No_file_list_found_using_directory_order + fileList.size() + x.entities, MODULE);
        }

        return fileList;
    }

    private static String[] readEntityHeader(File file, String delimiter, BufferedReader dataReader) throws IOException {
        String filePath = file.getPath().replace('\\', '/');

        String[] header = null;
        File headerFile = new File(FileUtil.getFile(filePath.substring(0, filePath.lastIndexOf('/'))), x.str_53a0acfa + file.getName());

        if (headerFile.exists()) {
            try (
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(headerFile),
                            StandardCharsets.UTF_8));) {

                String firstLine = reader.readLine();
                if (firstLine != null) {
                    header = firstLine.split(delimiter);
                }
            } catch (IOException | SecurityException e) {
                Debug.logError(e, MODULE);
            }
        } else {
            BufferedReader reader = dataReader;
            String firstLine = reader.readLine();
            if (firstLine != null) {
                header = firstLine.split(delimiter);
            }
        }
        // read one line from either the header file or the data file if no header file exists
        return header;
    }

    private static int readEntityFile(File file, String delimiter, Delegator delegator) throws IOException, GeneralException {
        String entityName = file.getName().substring(0, file.getName().lastIndexOf('.'));

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        String[] header = readEntityHeader(file, delimiter, reader);

        //Debug.logInfo("Opened data file [" + file.getName() + "] now running...", MODULE);
        GeneralException exception = null;
        String line = null;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            // process the record
            String fields[] = line.split(delimiter);
            //Debug.logInfo("Split record", MODULE);
            if (fields.length < 1) {
                exception = new GeneralException(x.Illegal_number_of_fields + file.getName() + x.str_0d0c4ddd + lineNumber);
                break;
            }

            GenericValue newValue = makeGenericValue(delegator, entityName, header, fields);
            //Debug.logInfo("Made value object", MODULE);
            newValue = delegator.createOrStore(newValue);
            //Debug.logInfo("Stored record", MODULE);

            if (lineNumber % 500 == 0 || lineNumber == 1) {
                Debug.logInfo(x.Records_Stored + file.getName() + x.str_89222ecc + lineNumber, MODULE);
                //Debug.logInfo("Last record : " + newValue, MODULE);
            }

            lineNumber++;
        }
        reader.close();

        // now that we closed the reader; throw the exception
        if (exception != null) {
            throw exception;
        }

        return lineNumber;
    }

    private static GenericValue makeGenericValue(Delegator delegator, String entityName, String[] header, String[] line) {
        GenericValue newValue = delegator.makeValue(entityName);
        for (int i = 0; i < header.length; i++) {
            String name = header[i].trim();

            String value = null;
            if (i < line.length) {
                value = line[i];
            }

            // check for null values
            if (UtilValidate.isNotEmpty(value)) {
                char first = value.charAt(0);
                if (first == 0x00) {
                    value = null;
                }

                // trim non-null values
                if (value != null) {
                    value = value.trim();
                }

                if (value != null && value.isEmpty()) {
                    value = null;
                }
            } else {
                value = null;
            }

            // convert and set the fields
            newValue.setString(name, value);
        }
        return newValue;
    }

    public static Map<String, Object> rebuildAllIndexesAndKeys(DispatchContext dctx, EntityDataServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get(x.locale);

        // check permission
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        if (!security.hasPermission(x.ENTITY_MAINT, userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtServicePermissionNotGranted, locale));
        }

        String groupName = (String) context.get(x.groupName);
        Boolean fixSizes = (Boolean) context.get(x.fixColSizes);
        if (fixSizes == null) fixSizes = Boolean.FALSE;
        List<String> messages = new LinkedList<>();

        GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(groupName);
        DatabaseUtil dbUtil = new DatabaseUtil(helperInfo);
        Map<String, ModelEntity> modelEntities;
        try {
            modelEntities = delegator.getModelEntityMapByGroup(groupName);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Error_getting_list_of_entities_in_group + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtErrorGettingListOfEntityInGroup,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        // step 1 - remove FK indices
        Debug.logImportant(x.Removing_all_foreign_key_indices, MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deleteForeignKeyIndices(modelEntity, messages);
        }

        // step 2 - remove FKs
        Debug.logImportant(x.Removing_all_foreign_keys, MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deleteForeignKeys(modelEntity, modelEntities, messages);
        }

        // step 3 - remove PKs
        Debug.logImportant(x.Removing_all_primary_keys, MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deletePrimaryKey(modelEntity, messages);
        }

        // step 4 - remove declared indices
        Debug.logImportant(x.Removing_all_declared_indices, MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deleteDeclaredIndices(modelEntity, messages);
        }

        // step 5 - repair field sizes
        if (fixSizes) {
            Debug.logImportant(x.Updating_column_field_size_changes, MODULE);
            List<String> fieldsWrongSize = new LinkedList<>();
            dbUtil.checkDb(modelEntities, fieldsWrongSize, messages, true, true, true, true);
            if (!fieldsWrongSize.isEmpty()) {
                dbUtil.repairColumnSizeChanges(modelEntities, fieldsWrongSize, messages);
            } else {
                String thisMsg = x.No_field_sizes_to_update;
                messages.add(thisMsg);
                Debug.logImportant(thisMsg, MODULE);
            }
        }

        // step 6 - create PKs
        Debug.logImportant(x.Creating_all_primary_keys, MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createPrimaryKey(modelEntity, messages);
        }

        // step 7 - create FK indices
        Debug.logImportant(x.Creating_all_foreign_key_indices, MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createForeignKeyIndices(modelEntity, messages);
        }

        // step 8 - create FKs
        Debug.logImportant(x.Creating_all_foreign_keys, MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createForeignKeys(modelEntity, modelEntities, messages);
        }

        // step 8 - create FKs
        Debug.logImportant(x.Creating_all_declared_indices, MODULE);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createDeclaredIndices(modelEntity, messages);
        }

        // step 8 - checkdb
        Debug.logImportant(x.Running_DB_check_with_add_missing_enabled, MODULE);
        dbUtil.checkDb(modelEntities, messages, true);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.messages, messages);
        return result;
    }

    public static Map<String, Object> unwrapByteWrappers(DispatchContext dctx, EntityDataServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String entityName = (String) context.get(x.entityName);
        String fieldName = (String) context.get(x.fieldName);
        Locale locale = (Locale) context.get(x.locale);

        try {
            List<GenericValue> rows = queryByCondition(delegator, entityName, Filters.alwaysTrue());
            for (GenericValue currentValue : rows) {
                byte[] bytes = currentValue.getBytes(fieldName);
                if (bytes != null) {
                    currentValue.setBytes(fieldName, bytes);
                    currentValue.store();
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Error_unwrapping_ByteWrapper_records + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtErrorUnwrappingRecords,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> reencryptPrivateKeys(DispatchContext dctx, EntityDataServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get(x.locale);

        // check permission
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        if (!security.hasPermission(x.ENTITY_MAINT, userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtServicePermissionNotGranted, locale));
        }
        String oldKey = (String) context.get(x.oldKey);
        String newKey = (String) context.get(x.newKey);
        AesCipherService cipherService = new AesCipherService();
        try {
            EntityKeyStoreDao entityKeyStoreDao = DaoRegistry.getDao(delegator, x.EntityKeyStore, EntityKeyStoreDao.class);
            List<EntityKeyStoreEntity> entityKeyStoreEntities = entityKeyStoreDao.list(Filters.alwaysTrue());
            for (EntityKeyStoreEntity entityKeyStoreEntity : entityKeyStoreEntities) {
                GenericValue row = delegator.makeValue(x.EntityKeyStore, Beans.beanToMap(entityKeyStoreEntity));
                byte[] keyBytes = Base64.decodeBase64(row.getString(x.keyText));
                Debug.logInfo(x.Processing_entry + row.getString(x.keyName) + x.with_key + row.getString(x.keyText), MODULE);
                if (oldKey != null) {
                    Debug.logInfo(x.Decrypting_with_old_key + oldKey, MODULE);
                    try {
                        keyBytes = cipherService.decrypt(keyBytes, Base64.decodeBase64(oldKey)).getClonedBytes();
                    } catch (Exception e) {
                        Debug.logInfo(x.Failed_to_decrypt_with_Shiro_cipher_trying_with_old_cipher, MODULE);
                        try {
                            keyBytes = DesCrypt.decrypt(DesCrypt.getDesKey(Base64.decodeBase64(oldKey)), keyBytes);
                        } catch (Exception e1) {
                            Debug.logError(e1, MODULE);
                            return ServiceUtil.returnError(e1.getMessage());
                        }
                    }
                }
                String newKeyText;
                if (newKey != null) {
                    Debug.logInfo(x.Encrypting_with_new_key + oldKey, MODULE);
                    newKeyText = cipherService.encrypt(keyBytes, Base64.decodeBase64(newKey)).toBase64();
                } else {
                    newKeyText = Base64.encodeBase64String(keyBytes);
                }
                Debug.logInfo(x.Storing_new_encrypted_value + newKeyText, MODULE);
                row.setString(x.keyText, newKeyText);
                row.store();
            }
        } catch (Exception gee) {
            Debug.logError(gee, MODULE);
            return ServiceUtil.returnError(gee.getMessage());
        }
        delegator.clearAllCaches();
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> reencryptFields(DispatchContext dctx, EntityDataServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get(x.locale);

        // check permission
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        if (!security.hasPermission(x.ENTITY_MAINT, userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtServicePermissionNotGranted, locale));
        }

        String groupName = (String) context.get(x.groupName);

        Map<String, ModelEntity> modelEntities;
        try {
            modelEntities = delegator.getModelEntityMapByGroup(groupName);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Error_getting_list_of_entities_in_group + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.EntityExtErrorGettingListOfEntityInGroup,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        for (ModelEntity modelEntity: modelEntities.values()) {
            List<ModelField> fields = modelEntity.getFieldsUnmodifiable();
            for (ModelField field: fields) {
                if (field.getEncryptMethod().isEncrypted()) {
                    try {
                        List<GenericValue> rows = queryByCondition(delegator, modelEntity.getEntityName(), Filters.alwaysTrue());
                        for (GenericValue row: rows) {
                            row.setString(field.getName(), row.getString(field.getName()));
                            row.store();
                        }
                    } catch (GenericEntityException gee) {
                        return ServiceUtil.returnError(gee.getMessage());
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    private static List<GenericValue> queryByCondition(Delegator delegator, String entityName, Condition condition)
            throws GenericEntityException {
        try {
            Dao<?, ?, ?> dao = resolveDao(delegator, entityName);
            return dao.query(condition, (rs, labels) -> toGenericValues(rs, labels, delegator, entityName));
        } catch (SQLException e) {
            throw new GenericEntityException(x.Failed_to_query_entity_via_DAO + entityName, e);
        }
    }

    private static List<GenericValue> toGenericValues(ResultSet rs, List<String> labels, Delegator delegator, String entityName)
            throws SQLException {
        List<GenericValue> rows = new LinkedList<>();
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
