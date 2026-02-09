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
package org.apache.ofbiz.entity.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import com.landawn.abacus.jdbc.JdbcUtil;

/**
 * DAO for sequence bank operations used by {@link org.apache.ofbiz.entity.util.SequenceUtil}.
 */
public final class SequenceBankDao {

    private final String lockSql;
    private final String insertSql;
    private final String selectSql;
    private final String incrementSql;

    public SequenceBankDao(String tableName, String nameColName, String idColName) {
        this.lockSql = "UPDATE " + tableName + " SET " + idColName + "=" + idColName + " WHERE " + nameColName + "=?";
        this.insertSql = "INSERT INTO " + tableName + " (" + nameColName + ", " + idColName + ") VALUES (?, ?)";
        this.selectSql = "SELECT " + idColName + " FROM " + tableName + " WHERE " + nameColName + "=?";
        this.incrementSql = "UPDATE " + tableName + " SET " + idColName + "=" + idColName + "+? WHERE " + nameColName + "=?";
    }

    public int lockByName(Connection connection, String seqName) throws SQLException {
        return JdbcUtil.prepareQuery(connection, lockSql)
                .setString(1, seqName)
                .update();
    }

    public int insertIfMissing(Connection connection, String seqName, long startSeqId) throws SQLException {
        return JdbcUtil.prepareQuery(connection, insertSql)
                .setString(1, seqName)
                .setLong(2, startSeqId)
                .update();
    }

    public Long selectCurrentSeqId(Connection connection, String seqName) throws SQLException {
        final var seqId = JdbcUtil.prepareQuery(connection, selectSql)
                .setString(1, seqName)
                .queryForLong();
        return seqId.isPresent() ? seqId.getAsLong() : null;
    }

    public int incrementBy(Connection connection, String seqName, long increment) throws SQLException {
        return JdbcUtil.prepareQuery(connection, incrementSql)
                .setLong(1, increment)
                .setString(2, seqName)
                .update();
    }
}
