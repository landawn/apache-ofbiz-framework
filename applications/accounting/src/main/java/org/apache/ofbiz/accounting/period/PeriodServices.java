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

package org.apache.ofbiz.accounting.period;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.CustomTimePeriodDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.entity.CustomTimePeriodEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.util.Beans;

import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.PeriodServicesContext;
public class PeriodServices {

    private static final String MODULE = PeriodServices.class.getName();
    private static final String RESOURCE = x.AccountingUiLabels;

    /*
     * find the date of the last closed CustomTimePeriod, or, if none available, the
     * earliest date available of any CustomTimePeriod
     */
    public static Map<String, Object> findLastClosedDate(DispatchContext dctx, PeriodServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String organizationPartyId = (String) context.get(x.organizationPartyId); // input parameters
        String periodTypeId = (String) context.get(x.periodTypeId);
        Timestamp findDate = (Timestamp) context.get(x.findDate);
        Locale locale = (Locale) context.get(x.locale);

        // default findDate to now
        if (findDate == null) {
            findDate = UtilDateTime.nowTimestamp();
        }

        Timestamp lastClosedDate = null; // return parameters
        GenericValue lastClosedTimePeriod = null;
        Map<String, Object> result = ServiceUtil.returnSuccess();

        try {
            CustomTimePeriodDao customTimePeriodDao = DaoRegistry.getDao(delegator, x.CustomTimePeriod, CustomTimePeriodDao.class);
            // try to get the ending date of the most recent accounting time period before
            // findDate which has been closed
            List<Condition> findClosedConditions = new LinkedList<>();
            findClosedConditions.add(Filters.eq(x.organizationPartyId, organizationPartyId));
            findClosedConditions.add(Filters.le(x.thruDate, findDate));
            findClosedConditions.add(Filters.eq(x.isClosed, x.Y));
            if (UtilValidate.isNotEmpty(periodTypeId)) {
                // if a periodTypeId was supplied, use it
                findClosedConditions.add(Filters.eq(x.periodTypeId, periodTypeId));
            }
            CustomTimePeriodEntity closedTimePeriodEntity = customTimePeriodDao.list(Filters.and(findClosedConditions)).stream()
                    .sorted(Comparator.comparing(CustomTimePeriodEntity::getThruDate, Comparator.nullsLast(Timestamp::compareTo)).reversed())
                    .findFirst().orElse(null);
            GenericValue closedTimePeriod = closedTimePeriodEntity == null ? null
                    : delegator.makeValue(x.CustomTimePeriod, Beans.beanToMap(closedTimePeriodEntity));

            if (UtilValidate.isNotEmpty(closedTimePeriod) && UtilValidate.isNotEmpty(closedTimePeriod.get(x.thruDate))) {
                lastClosedTimePeriod = closedTimePeriod;
                lastClosedDate = lastClosedTimePeriod.getTimestamp(x.thruDate);
            } else {
                // uh oh, no time periods have been closed? in that case, just find the earliest
                // beginning of a time period for this organization and optionally, for this period type
                List<Condition> findParams = new LinkedList<>();
                findParams.add(Filters.eq(x.organizationPartyId, organizationPartyId));
                if (UtilValidate.isNotEmpty(periodTypeId)) {
                    findParams.add(Filters.eq(x.periodTypeId, periodTypeId));
                }
                CustomTimePeriodEntity timePeriodEntity = customTimePeriodDao.list(Filters.and(findParams)).stream()
                        .sorted(Comparator.comparing(CustomTimePeriodEntity::getFromDate, Comparator.nullsLast(Timestamp::compareTo)))
                        .findFirst().orElse(null);
                GenericValue timePeriod = timePeriodEntity == null ? null : delegator.makeValue(x.CustomTimePeriod, Beans.beanToMap(timePeriodEntity));
                if (timePeriod != null && UtilValidate.isNotEmpty(timePeriod.get(x.fromDate))) {
                    lastClosedDate = timePeriod.getTimestamp(x.fromDate);
                } else {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.AccountingPeriodCannotGet, locale));
                }
            }

            result.put(x.lastClosedTimePeriod, lastClosedTimePeriod); // ok if this is null - no time periods have been closed
            result.put(x.lastClosedDate, lastClosedDate); // should have a value - not null
            return result;
        } catch (Exception ex) {
            return (ServiceUtil.returnError(ex.getMessage()));
        }
    }
}
