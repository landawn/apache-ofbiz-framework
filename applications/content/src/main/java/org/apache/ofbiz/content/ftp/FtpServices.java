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
package org.apache.ofbiz.content.ftp;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.ContactMechDao;
import org.apache.ofbiz.persistence.dao.ContentDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.FtpAddressDao;
import org.apache.ofbiz.persistence.entity.ContactMechEntity;
import org.apache.ofbiz.persistence.entity.ContentEntity;
import org.apache.ofbiz.persistence.entity.FtpAddressEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.FtpServicesContext;
/**
 * FtpServices class
 * This class provide Ftp transfer services for content
 */
public class FtpServices {

    private static final String MODULE = FtpServices.class.getName();
    private static final String RESOURCE = x.ContentUiLabels;

    private static FtpClientInterface createFtpClient(String serverType)
            throws GeneralException {
        FtpClientInterface ftpClient = null;
        switch (serverType) {
        case x.ftp:
            ftpClient = new SimpleFtpClient();
            break;
        case x.ftps:
            //TODO : to implements
            throw new GeneralException(x.Ftp_secured_transfer_protocol_not_yet_implemented);
        case x.sftp:
            ftpClient = new SshFtpClient();
            break;
        }
        return ftpClient;
    }

    public static Map<String, Object> sendContentToFtp(DispatchContext dctx, FtpServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        String contactMechId = (String) context.get(x.contactMechId);
        String contentId = (String) context.get(x.contentId);
        String communicationEventId = (String) context.get(x.communicationEventId);
        boolean forceTransferControlSuccess = EntityUtilProperties.propertyValueEqualsIgnoreCase(x.ftp,
                x.ftp_force_transfer_control, x.Y, delegator);
        boolean ftpNotificationEnabled = EntityUtilProperties.propertyValueEqualsIgnoreCase(x.ftp,
                x.ftp_notifications_enabled, x.Y, delegator);

        if (!ftpNotificationEnabled) return ServiceUtil.returnSuccess();

        // for ECA communicationEvent process
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        resultMap.put(x.communicationEventId, communicationEventId);

        FtpClientInterface ftpClient = null;

        try {
            //Retrieve and check contactMechType
            ContactMechDao contactMechDao = DaoRegistry.getDao(delegator, x.ContactMech, ContactMechDao.class);
            FtpAddressDao ftpAddressDao = DaoRegistry.getDao(delegator, x.FtpAddress, FtpAddressDao.class);
            ContentDao contentDao = DaoRegistry.getDao(delegator, x.Content, ContentDao.class);

            ContactMechEntity contactMechEntity = contactMechDao.get(contactMechId).orElse(null);
            FtpAddressEntity ftpAddressEntity = ftpAddressDao.get(contactMechId).orElse(null);
            GenericValue contactMech = contactMechEntity == null ? null : delegator.makeValue(x.ContactMech, Beans.beanToMap(contactMechEntity));
            GenericValue ftpAddress = ftpAddressEntity == null ? null : delegator.makeValue(x.FtpAddress, Beans.beanToMap(ftpAddressEntity));
            if (null == contactMech || null == ftpAddress || !x.FTP_ADDRESS.equals(contactMech.getString(x.contactMechTypeId))) {
                String errMsg = UtilProperties.getMessage(x.ContentErrorUiLabels, x.ftpservices_contact_mech_must_be_ftp, locale);
                return ServiceUtil.returnError(errMsg + x.str_b858cb28 + contactMechId);
            }

            //Validate content
            ContentEntity contentEntity = contentDao.get(contentId).orElse(null);
            GenericValue content = contentEntity == null ? null : delegator.makeValue(x.Content, Beans.beanToMap(contentEntity));
            if (null == content) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ContentNoContentFound,
                        UtilMisc.toMap(x.contentId, contentId), locale));
            }

            //ftp redirection
            if (x.Y.equalsIgnoreCase(UtilProperties.getPropertyValue(x.ftp, x.ftp_notifications_redirectTo_enabled))) {
                ftpAddress = delegator.makeValue(x.FtpAddress);
                ftpAddress.put(x.defaultTimeout, UtilProperties.getPropertyAsLong(x.ftp, x.ftp_notifications_redirectTo_defaultTimeout, 30000));
                ftpAddress.put(x.hostname, UtilProperties.getPropertyValue(x.ftp, x.ftp_notifications_redirectTo_hostname));
                ftpAddress.put(x.filePath, UtilProperties.getPropertyValue(x.ftp, x.ftp_notifications_redirectTo_filePath));
                ftpAddress.put(x.port, UtilProperties.getPropertyAsLong(x.ftp, x.ftp_notifications_redirectTo_port, 65535));
                ftpAddress.put(x.username, UtilProperties.getPropertyValue(x.ftp, x.ftp_notifications_redirectTo_username));
                ftpAddress.put(x.ftpPassword, UtilProperties.getPropertyValue(x.ftp, x.ftp_notifications_redirectTo_ftpPassword));
                ftpAddress.put(x.binaryTransfer, UtilProperties.getPropertyValue(x.ftp, x.ftp_notifications_redirectTo_binaryTransfer));
                ftpAddress.put(x.passiveMode, UtilProperties.getPropertyValue(x.ftp, x.ftp_notifications_redirectTo_passiveMode));
                ftpAddress.put(x.zipFile, UtilProperties.getPropertyValue(x.ftp, x.ftp_notifications_redirectTo_zipFile));
            }

            String hostname = ftpAddress.getString(x.hostname);
            if (UtilValidate.isEmpty(hostname)) {
                return ServiceUtil.returnError(x.Ftp_destination_server_is_null);
            } else if (hostname.indexOf(x.str_ef81042e) == -1) {
                return ServiceUtil.returnError(x.No_protocol_defined_in_ftp_destination_address);
            }

            String serverType = hostname.split(x.str_ef81042e)[0];
            hostname = hostname.split(x.str_ef81042e)[1];

            ftpClient = createFtpClient(serverType);
            if (null == ftpClient) {
                return ServiceUtil.returnError(x.Server_type + serverType + x.not_supported_for_hostname + hostname);
            }

            Long defaultTimeout = ftpAddress.getLong(x.defaultTimeout);
            Long port = ftpAddress.getLong(x.port);
            String username = ftpAddress.getString(x.username);
            String password = ftpAddress.getString(x.ftpPassword);

            if (Debug.infoOn()) {
                Debug.logInfo(x.connecting_to + username + x.str_9a782114 + ftpAddress.getString(x.hostname) + x.str_05a79f06 + port, MODULE);
            }
            ftpClient.connect(hostname, username, password, port, defaultTimeout);
            boolean binary = x.Y.equalsIgnoreCase(ftpAddress.getString(x.binaryTransfer));
            ftpClient.setBinaryTransfer(binary);
            boolean passive = x.Y.equalsIgnoreCase(ftpAddress.getString(x.passiveMode));
            ftpClient.setPassiveMode(passive);

            GenericValue dataResource = delegator.findOne(x.DataResource, true, x.dataResourceId, content.getString(x.dataResourceId));
            Map<String, Object> resultStream = DataResourceWorker.getDataResourceStream(dataResource, null, null, locale, null, true);
            InputStream contentStream = (InputStream) resultStream.get(x.stream);
            if (contentStream == null) {
                return ServiceUtil.returnError(x.DataResource_f3c3987a + content.getString(x.dataResourceId) + x.return_an_empty_stream);
            }

            String path = ftpAddress.getString(x.filePath);
            if (Debug.infoOn()) {
                Debug.logInfo(x.storing_local_file_remotely_as + (UtilValidate.isNotEmpty(path) ? path + x.str_42099b4a : x.emptyString)
                        + content.getString(x.contentName), MODULE);
            }
            String fileName = content.getString(x.contentName);
            String remoteFileName = fileName;
            boolean zipFile = x.Y.equalsIgnoreCase(ftpAddress.getString(x.zipFile));
            if (zipFile) {
                //Create zip file from content input stream
                ByteArrayInputStream zipStream = FileUtil.zipFileStream(contentStream, fileName);
                remoteFileName = fileName + (fileName.endsWith(x.zip) ? x.emptyString : x.zip_da112e73);
                ftpClient.copy(path, remoteFileName, zipStream);

                zipStream.close();
            } else {
                ftpClient.copy(path, remoteFileName, contentStream);
            }
            contentStream.close();

            //test if the file is correctly sent
            if (forceTransferControlSuccess) {
                if (Debug.infoOn()) {
                    Debug.logInfo(x.Control_if_service_really_success_the_transfer, MODULE);
                }

                //recreate the connection
                ftpClient.closeConnection();
                ftpClient = createFtpClient(serverType);
                ftpClient.connect(hostname, username, password, port, defaultTimeout);
                ftpClient.setBinaryTransfer(binary);
                ftpClient.setPassiveMode(passive);

                //check the file name previously copy
                List<String> fileNames = ftpClient.list(path);
                if (Debug.infoOn()) {
                    Debug.logInfo(x.For_the_path + path + x.we_found + fileNames, MODULE);
                }

                if (fileNames == null || !fileNames.contains(remoteFileName)) {
                    return ServiceUtil.returnError(x.DataResource_f3c3987a + content.getString(x.dataResourceId) + x.return_an_empty_stream);
                }
                if (Debug.infoOn()) {
                    Debug.logInfo(x.Ok_the_file + content.getString(x.contentName) + x.is_present, MODULE);
                }
            }
        } catch (GeneralException | IOException | SQLException e) {
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            try {
                if (ftpClient != null) {
                    ftpClient.closeConnection();
                }
            } catch (Exception e) {
                Debug.logWarning(e, x.getFile_Problem_with_FTP_disconnect, MODULE);
            }
        }
        return resultMap;
    }

}
