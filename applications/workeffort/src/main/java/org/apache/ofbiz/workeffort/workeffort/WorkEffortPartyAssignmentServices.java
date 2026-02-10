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

package org.apache.ofbiz.workeffort.workeffort;

import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.WorkEffortDao;
import org.apache.ofbiz.persistence.entity.WorkEffortEntity;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
/**
 * WorkEffortPartyAssignmentServices - Services to handle form input and other data changes.
 */
public class WorkEffortPartyAssignmentServices {

    private static final String MODULE = WorkEffortPartyAssignmentServices.class.getName();

    public static void updateWorkflowEngine(GenericValue wepa, GenericValue userLogin, LocalDispatcher dispatcher) {
        // if the WorkEffort is an ACTIVITY, check for accept or complete new status...
        Delegator delegator = wepa.getDelegator();
        GenericValue workEffort = null;

        try {
            WorkEffortDao workEffortDao = DaoRegistry.getDao(delegator, x.WorkEffort, WorkEffortDao.class);
            WorkEffortEntity workEffortEntity = workEffortDao.get((String) wepa.get(x.workEffortId)).orElse(null);
            if (workEffortEntity != null) {
                workEffort = delegator.makeValue(x.WorkEffort, Beans.beanToMap(workEffortEntity));
            }
        } catch (Exception e) {
            Debug.logWarning(e, MODULE);
        }
        if (workEffort != null && x.ACTIVITY.equals(workEffort.getString(x.workEffortTypeId))) {
            // TODO: restrict status transitions

            String statusId = (String) wepa.get(x.statusId);
            ServiceContext context = new ServiceContext(UtilMisc.toMap(x.workEffortId, wepa.get(x.workEffortId), x.partyId,
                    wepa.get(x.partyId), x.roleTypeId, wepa.get(x.roleTypeId), x.fromDate, wepa.get(x.fromDate), x.userLogin, userLogin));

            if (x.CAL_ACCEPTED.equals(statusId)) {
                // accept the activity assignment
                try {
                    Map<String, Object> results = dispatcher.runSync(x.wfAcceptAssignment, context);
                    if (ServiceUtil.isError(results)) {
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), MODULE);
                    }
                    if (results != null && results.get(ModelService.ERROR_MESSAGE) != null) {
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), MODULE);
                    }
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, MODULE);
                }
            } else if (x.CAL_COMPLETED.equals(statusId)) {
                // complete the activity assignment
                try {
                    Map<String, Object> results = dispatcher.runSync(x.wfCompleteAssignment, context);
                    if (ServiceUtil.isError(results)) {
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), MODULE);
                    }
                    if (results != null && results.get(ModelService.ERROR_MESSAGE) != null) {
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), MODULE);
                    }
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, MODULE);
                }
            } else if (x.CAL_DECLINED.equals(statusId)) {
                // decline the activity assignment
                try {
                    Map<String, Object> results = dispatcher.runSync(x.wfDeclineAssignment, context);

                    if (results != null && results.get(ModelService.ERROR_MESSAGE) != null) {
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), MODULE);
                    }
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, MODULE);
                }
            }
        }
    }
}

