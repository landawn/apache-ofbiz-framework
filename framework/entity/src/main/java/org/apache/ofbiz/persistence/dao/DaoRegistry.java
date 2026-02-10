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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;

import com.landawn.abacus.jdbc.JdbcUtil;
import com.landawn.abacus.jdbc.dao.Dao;

/**
 * Registry for typed DAO instances created through Abacus.
 */
public final class DaoRegistry {

    private static final ConcurrentMap<DaoCacheKey, Dao<?, ?, ?>> DAO_CACHE = new ConcurrentHashMap<>();

    private DaoRegistry() {
        // utility class
    }

    public static <TD extends Dao<?, ?, TD>> TD getDao(Delegator delegator, String entityName, Class<TD> daoInterface) {
        Objects.requireNonNull(delegator, "delegator must not be null");
        Objects.requireNonNull(entityName, "entityName must not be null");
        Objects.requireNonNull(daoInterface, "daoInterface must not be null");

        String entityGroupName = delegator.getEntityGroupName(entityName);
        if (entityGroupName == null) {
            throw new IllegalArgumentException("No entity group found for entity: " + entityName);
        }

        GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(entityGroupName);
        if (helperInfo == null) {
            throw new IllegalStateException("No helper info found for entity group: " + entityGroupName + ", entity: " + entityName);
        }

        return getDao(helperInfo, daoInterface);
    }

    public static <TD extends Dao<?, ?, TD>> TD getDao(GenericHelperInfo helperInfo, Class<TD> daoInterface) {
        Objects.requireNonNull(helperInfo, "helperInfo must not be null");
        Objects.requireNonNull(daoInterface, "daoInterface must not be null");

        DaoCacheKey cacheKey = new DaoCacheKey(helperInfo.getHelperFullName(), daoInterface);
        Dao<?, ?, ?> dao = DAO_CACHE.computeIfAbsent(cacheKey,
                key -> JdbcUtil.createDao(daoInterface, new OfbizDataSourceAdapter(helperInfo)));

        return daoInterface.cast(dao);
    }

    public static void clear() {
        DAO_CACHE.clear();
    }

    public static int size() {
        return DAO_CACHE.size();
    }

    private static final class DaoCacheKey {
        private final String helperFullName;
        private final Class<?> daoInterface;

        private DaoCacheKey(String helperFullName, Class<?> daoInterface) {
            this.helperFullName = helperFullName;
            this.daoInterface = daoInterface;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DaoCacheKey)) {
                return false;
            }
            DaoCacheKey other = (DaoCacheKey) obj;
            return Objects.equals(helperFullName, other.helperFullName) && Objects.equals(daoInterface, other.daoInterface);
        }

        @Override
        public int hashCode() {
            return Objects.hash(helperFullName, daoInterface);
        }
    }
}
