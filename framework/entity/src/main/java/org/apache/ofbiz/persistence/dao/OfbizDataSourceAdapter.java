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
package org.apache.ofbiz.persistence.dao;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;
import org.apache.ofbiz.entity.transaction.TransactionFactoryLoader;

/**
 * DataSource implementation backed by OFBiz entity transaction infrastructure.
 */
public final class OfbizDataSourceAdapter implements DataSource {

    private static final Logger LOGGER = Logger.getLogger(OfbizDataSourceAdapter.class.getName());

    private final GenericHelperInfo helperInfo;
    private volatile PrintWriter logWriter;
    private volatile int loginTimeoutSeconds;

    public OfbizDataSourceAdapter(GenericHelperInfo helperInfo) {
        this.helperInfo = Objects.requireNonNull(helperInfo, "helperInfo must not be null");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(helperInfo);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        GenericHelperInfo overrideHelperInfo = new GenericHelperInfo(helperInfo.getEntityGroupName(), helperInfo.getHelperBaseName());
        overrideHelperInfo.setTenantId(helperInfo.getTenantId());
        overrideHelperInfo.setOverrideJdbcUri(helperInfo.getOverrideJdbcUri());
        overrideHelperInfo.setOverrideUsername(username != null ? username : helperInfo.getOverrideUsername());
        overrideHelperInfo.setOverridePassword(password != null ? password : helperInfo.getOverridePassword());

        return getConnection(overrideHelperInfo);
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) {
        this.loginTimeoutSeconds = seconds;
    }

    @Override
    public int getLoginTimeout() {
        return loginTimeoutSeconds;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return LOGGER;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return iface.cast(this);
        }
        throw new SQLException("Unsupported unwrap type: " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isAssignableFrom(getClass());
    }

    private static Connection getConnection(GenericHelperInfo helperInfo) throws SQLException {
        try {
            Connection connection = TransactionFactoryLoader.getInstance().getConnection(helperInfo);
            if (connection == null) {
                throw new SQLException("No connection available for helper: " + helperInfo.getHelperFullName());
            }
            return connection;
        } catch (GenericEntityException e) {
            throw new SQLException("Failed to get connection for helper: " + helperInfo.getHelperFullName(), e);
        }
    }
}
