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
package org.apache.ofbiz.entityext.cache;

import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.persistence.entity.UserLoginEntity;
import org.apache.ofbiz.entity.util.DistributedCacheClear;
import org.apache.ofbiz.entityext.EntityServiceFactory;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.EntityCacheServicesContext;
/**
 * Entity Engine Cache Services
 */
public class EntityCacheServices implements DistributedCacheClear {

    private static final String MODULE = EntityCacheServices.class.getName();

    private Delegator delegator = null;
    private LocalDispatcher dispatcher = null;
    private String userLoginId = null;

    public EntityCacheServices() { }

    @Override
    public void setDelegator(Delegator delegator, String userLoginId) {
        this.delegator = delegator;
        this.dispatcher = EntityServiceFactory.getLocalDispatcher(delegator);
        this.userLoginId = userLoginId;
    }

    /**
     * Gets auth user login.
     * @return the auth user login
     */
    public GenericValue getAuthUserLogin() {
        GenericValue userLogin = null;
        try {
            UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
            UserLoginEntity userLoginEntity = userLoginDao.get(userLoginId).orElse(null);
            if (userLoginEntity != null) {
                userLogin = delegator.makeValue(x.UserLogin, Beans.beanToMap(userLoginEntity));
            }
        } catch (Exception e) {
            Debug.logError(e, x.Error_finding_the_userLogin_for_distributed_cache_clear, MODULE);
        }
        return userLogin;
    }

    @Override
    public void distributedClearCacheLine(GenericValue value) {
        // Debug.logInfo("running distributedClearCacheLine for value: " + value, MODULE);
        if (this.dispatcher == null) {
            Debug.logWarning(x.No_dispatcher_is_available_somehow_the_setDelegator_which_also_creates_a_dispatcher_was_not_called
                    + x.not_running_distributed_cache_clear, MODULE);
            return;
        }

        GenericValue userLogin = getAuthUserLogin();
        if (userLogin == null) {
            Debug.logWarning(x.The_userLogin_for_distributed_cache_clear_was_not_found_with_userLoginId + userLoginId
                    + x.not_clearing_remote_caches, MODULE);
            return;
        }

        try {
            this.dispatcher.runAsync(x.distributedClearCacheLineByValue, UtilMisc.toMap(x.value, value, x.userLogin, userLogin), false);
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_running_the_distributedClearCacheLineByValue_service, MODULE);
        }
    }

    @Override
    public void distributedClearCacheLineFlexible(GenericEntity dummyPK) {
        // Debug.logInfo("running distributedClearCacheLineFlexible for dummyPK: " + dummyPK, MODULE);
        if (this.dispatcher == null) {
            Debug.logWarning(x.No_dispatcher_is_available_somehow_the_setDelegator_which_also_creates_a_dispatcher_was_not_called_55220f41
                    + x.not_running_distributed_cache_clear_11441b12, MODULE);
            return;
        }

        GenericValue userLogin = getAuthUserLogin();
        if (userLogin == null) {
            Debug.logWarning(x.The_userLogin_for_distributed_cache_clear_was_not_found_with_userLoginId + userLoginId
                    + x.not_clearing_remote_caches, MODULE);
            return;
        }

        try {
            this.dispatcher.runAsync(x.distributedClearCacheLineByDummyPK, UtilMisc.toMap(x.dummyPK, dummyPK, x.userLogin, userLogin), false);
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_running_the_distributedClearCacheLineByDummyPK_service, MODULE);
        }
    }

    @Override
    public void distributedClearCacheLineByCondition(String entityName, EntityCondition condition) {
        // Debug.logInfo("running distributedClearCacheLineByCondition for (name, condition): " + entityName + ", " + condition + ")", MODULE);
        if (this.dispatcher == null) {
            Debug.logWarning(x.No_dispatcher_is_available_somehow_the_setDelegator_which_also_creates_a_dispatcher
                    + x.was_not_called_not_running_distributed_cache_clear, MODULE);
            return;
        }

        GenericValue userLogin = getAuthUserLogin();
        if (userLogin == null) {
            Debug.logWarning(x.The_userLogin_for_distributed_cache_clear_was_not_found_with_userLoginId + userLoginId
                    + x.not_clearing_remote_caches, MODULE);
            return;
        }

        try {
            this.dispatcher.runAsync(x.distributedClearCacheLineByCondition, UtilMisc.toMap(x.entityName, entityName, x.condition,
                    condition, x.userLogin, userLogin), false);
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_running_the_distributedClearCacheLineByCondition_service, MODULE);
        }
    }

    @Override
    public void distributedClearCacheLine(GenericPK primaryKey) {
        // Debug.logInfo("running distributedClearCacheLine for primaryKey: " + primaryKey, MODULE);
        if (this.dispatcher == null) {
            Debug.logWarning(x.No_dispatcher_is_available_somehow_the_setDelegator_which_also_creates_a_dispatcher_was_not_called_55220f41
                    + x.not_running_distributed_cache_clear_11441b12, MODULE);
            return;
        }

        GenericValue userLogin = getAuthUserLogin();
        if (userLogin == null) {
            Debug.logWarning(x.The_userLogin_for_distributed_cache_clear_was_not_found_with_userLoginId + userLoginId
                    + x.not_clearing_remote_caches, MODULE);
            return;
        }

        try {
            this.dispatcher.runAsync(x.distributedClearCacheLineByPrimaryKey, UtilMisc.toMap(x.primaryKey,
                    primaryKey, x.userLogin, userLogin), false);
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_running_the_distributedClearCacheLineByPrimaryKey_service, MODULE);
        }
    }

    @Override
    public void clearAllCaches() {
        if (this.dispatcher == null) {
            Debug.logWarning(x.No_dispatcher_is_available_somehow_the_setDelegator_which_also_creates_a_dispatcher
                    + x.was_not_called_not_running_distributed_clear_all_caches, MODULE);
            return;
        }

        GenericValue userLogin = getAuthUserLogin();
        if (userLogin == null) {
            Debug.logWarning(x.The_userLogin_for_distributed_cache_clear_was_not_found_with_userLoginId + userLoginId
                    + x.not_clearing_remote_caches, MODULE);
            return;
        }

        try {
            this.dispatcher.runAsync(x.distributedClearAllEntityCaches, UtilMisc.toMap(x.userLogin, userLogin), false);
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Error_running_the_distributedClearAllCaches_service, MODULE);
        }
    }

    /**
     * Clear All Entity Caches Service
     * @param dctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> clearAllEntityCaches(DispatchContext dctx, EntityCacheServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Boolean distributeBool = (Boolean) context.get(x.distribute);
        boolean distribute = false;
        if (distributeBool != null) distribute = distributeBool;

        delegator.clearAllCaches(distribute);

        return ServiceUtil.returnSuccess();
    }

    /**
     * Clear Cache Line Service: one of the following context parameters is required: value, dummyPK or primaryKey
     * @param dctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> clearCacheLine(DispatchContext dctx, EntityCacheServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Boolean distributeBool = (Boolean) context.get(x.distribute);
        boolean distribute = false;
        if (distributeBool != null) distribute = distributeBool;

        if (context.containsKey(x.value)) {
            GenericValue value = (GenericValue) context.get(x.value);
            if (Debug.infoOn()) {
                Debug.logInfo(x.Got_a_clear_cache_line_by_value_service_call_entityName + value.getEntityName(), MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose(x.Got_a_clear_cache_line_by_value_service_call_value + value, MODULE);
            }
            delegator.clearCacheLine(value, distribute);
        } else if (context.containsKey(x.dummyPK)) {
            GenericEntity dummyPK = (GenericEntity) context.get(x.dummyPK);
            if (Debug.infoOn()) {
                Debug.logInfo(x.Got_a_clear_cache_line_by_dummyPK_service_call_entityName + dummyPK.getEntityName(), MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose(x.Got_a_clear_cache_line_by_dummyPK_service_call_dummyPK + dummyPK, MODULE);
            }
            delegator.clearCacheLineFlexible(dummyPK, distribute);
        } else if (context.containsKey(x.primaryKey)) {
            GenericPK primaryKey = (GenericPK) context.get(x.primaryKey);
            if (Debug.infoOn()) {
                Debug.logInfo(x.Got_a_clear_cache_line_by_primaryKey_service_call_entityName + primaryKey.getEntityName(), MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose(x.Got_a_clear_cache_line_by_primaryKey_service_call_primaryKey + primaryKey, MODULE);
            }
            delegator.clearCacheLine(primaryKey, distribute);
        } else if (context.containsKey(x.condition)) {
            String entityName = (String) context.get(x.entityName);
            EntityCondition condition = (EntityCondition) context.get(x.condition);
            if (Debug.infoOn()) {
                Debug.logInfo(x.Got_a_clear_cache_line_by_condition_service_call_entityName + entityName, MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose(x.Got_a_clear_cache_line_by_condition_service_call_condition + condition, MODULE);
            }
            delegator.clearCacheLineByCondition(entityName, condition, distribute);
        }
        return ServiceUtil.returnSuccess();
    }
}

