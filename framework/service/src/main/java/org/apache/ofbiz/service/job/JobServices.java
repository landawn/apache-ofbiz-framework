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
package org.apache.ofbiz.service.job;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.JobSandboxDao;
import org.apache.ofbiz.persistence.entity.JobSandboxEntity;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.config.ServiceConfigUtil;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.JobServicesContext;
public class JobServices {

    private static final String MODULE = JobServices.class.getName();
    private static final String RESOURCE = x.ServiceErrorUiLabels;

    public static Map<String, Object> cancelJob(DispatchContext dctx, JobServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = ServiceUtil.getLocale(context);

        String jobId = (String) context.get(x.jobId);
        Map<String, Object> fields = UtilMisc.<String, Object>toMap(x.jobId, jobId);

        GenericValue job = null;
        try {
            JobSandboxDao jobSandboxDao = DaoRegistry.getDao(delegator, x.JobSandbox, JobSandboxDao.class);
            JobSandboxEntity jobEntity = jobSandboxDao.get(jobId).orElse(null);
            if (jobEntity != null) {
                job = delegator.makeValue(x.JobSandbox, Beans.beanToMap(jobEntity));
            }
            if (job != null) {
                job.set(x.cancelDateTime, UtilDateTime.nowTimestamp());
                job.set(x.statusId, x.SERVICE_CANCELLED);
                job.store();
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            String errMsg = UtilProperties.getMessage(RESOURCE, x.serviceUtil_unable_to_cancel_job, locale) + x.str_d98411eb + fields;
            return ServiceUtil.returnError(errMsg);
        }

        if (job != null) {
            Timestamp cancelDate = job.getTimestamp(x.cancelDateTime);
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.cancelDateTime, cancelDate);
            result.put(x.statusId, x.SERVICE_PENDING); // To more easily see current pending jobs and possibly cancel some others
            return result;
        }
        String errMsg = UtilProperties.getMessage(RESOURCE, x.serviceUtil_unable_to_cancel_job, locale) + x.str_d98411eb + null;
        return ServiceUtil.returnError(errMsg);
    }

    public static Map<String, Object> cancelJobRetries(DispatchContext dctx, JobServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = ServiceUtil.getLocale(context);
        if (!security.hasPermission(x.SERVICE_INVOKE_ANY, userLogin)) {
            String errMsg = UtilProperties.getMessage(RESOURCE, x.serviceUtil_no_permission_to_run, locale) + x.str_3a52ce78;
            return ServiceUtil.returnError(errMsg);
        }

        String jobId = (String) context.get(x.jobId);
        Map<String, Object> fields = UtilMisc.<String, Object>toMap(x.jobId, jobId);

        GenericValue job = null;
        try {
            JobSandboxDao jobSandboxDao = DaoRegistry.getDao(delegator, x.JobSandbox, JobSandboxDao.class);
            JobSandboxEntity jobEntity = jobSandboxDao.get(jobId).orElse(null);
            if (jobEntity != null) {
                job = delegator.makeValue(x.JobSandbox, Beans.beanToMap(jobEntity));
            }
            if (job != null) {
                job.set(x.maxRetry, 0L);
                job.store();
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            String errMsg = UtilProperties.getMessage(RESOURCE, x.serviceUtil_unable_to_cancel_job_retries, locale) + x.str_d98411eb + fields;
            return ServiceUtil.returnError(errMsg);
        }

        if (job != null) {
            return ServiceUtil.returnSuccess();
        }
        String errMsg = UtilProperties.getMessage(RESOURCE, x.serviceUtil_unable_to_cancel_job_retries, locale) + x.str_d98411eb + null;
        return ServiceUtil.returnError(errMsg);
    }

    public static Map<String, Object> purgeOldJobs(DispatchContext dctx, JobServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        String sendPool = (String) context.get(x.poolId);
        Integer daysToKeep = (Integer) context.get(x.daysToKeep);
        Integer limit = (Integer) context.get(x.limit);
        try {
            if (sendPool == null) sendPool = ServiceConfigUtil.getServiceEngine().getThreadPool().getSendToPool();
            if (daysToKeep == null) daysToKeep = ServiceConfigUtil.getServiceEngine().getThreadPool().getPurgeJobDays();
            if (limit == null) limit = ServiceConfigUtil.getServiceEngine().getThreadPool().getMaxThreads();
        } catch (GenericConfigException e) {
            Debug.logWarning(e, x.Exception_thrown_while_getting_service_configuration, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceExceptionThrownWhileGettingServiceConfiguration,
                    UtilMisc.toMap(x.errorString, e), locale));
        }
        Delegator delegator = dctx.getDelegator();
        Timestamp purgeTime = Timestamp.from(Instant.now().minus(Duration.ofDays(daysToKeep)));
        try {
            JobManager.getJobsToPurge(delegator, sendPool, null, limit, purgeTime)
                    .forEach(JobUtil::removeJob);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> resetJob(DispatchContext dctx, JobServicesContext context) {
        Delegator delegator = dctx.getDelegator();

        String jobId = (String) context.get(x.jobId);
        GenericValue job;
        try {
            JobSandboxDao jobSandboxDao = DaoRegistry.getDao(delegator, x.JobSandbox, JobSandboxDao.class);
            JobSandboxEntity jobEntity = jobSandboxDao.get(jobId).orElse(null);
            job = jobEntity == null ? null : delegator.makeValue(x.JobSandbox, Beans.beanToMap(jobEntity));
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the job
        if (job != null) {
            job.set(x.statusId, x.SERVICE_PENDING);
            job.set(x.startDateTime, null);
            job.set(x.finishDateTime, null);
            job.set(x.cancelDateTime, null);
            job.set(x.runByInstanceId, null);

            // save the job
            try {
                job.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        return ServiceUtil.returnSuccess();
    }
}
