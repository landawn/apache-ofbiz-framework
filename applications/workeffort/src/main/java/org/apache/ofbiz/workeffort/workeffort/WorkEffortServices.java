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

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.ofbiz.base.util.DateRange;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.TimeDuration;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityJoinOperator;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.calendar.TemporalExpression;
import org.apache.ofbiz.service.calendar.TemporalExpressionWorker;

import com.ibm.icu.util.Calendar;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.WorkEffortServicesContext;
/**
 * WorkEffortServices - WorkEffort related Services
 */
public class WorkEffortServices {

    private static final String MODULE = WorkEffortServices.class.getName();
    private static final String RES_ERROR = x.WorkEffortUiLabels;

    public static Map<String, Object> getWorkEffortAssignedEventsForRole(DispatchContext ctx, WorkEffortServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String roleTypeId = (String) context.get(x.roleTypeId);
        Locale locale = (Locale) context.get(x.locale);

        List<GenericValue> validWorkEfforts = null;

        if (userLogin != null && userLogin.get(x.partyId) != null) {
            try {
                EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(
                        EntityOperator.AND,
                        EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, userLogin.get(x.partyId)),
                        EntityCondition.makeCondition(x.roleTypeId, EntityOperator.EQUALS, roleTypeId),
                        EntityCondition.makeCondition(x.workEffortTypeId, EntityOperator.EQUALS, x.EVENT),
                        EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_DECLINED),
                        EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_DELEGATED),
                        EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_COMPLETED),
                        EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_CANCELLED));
                validWorkEfforts = DaoRegistry.getDao(delegator, x.WorkEffortAndPartyAssign, UserLoginDao.class).findByCondition(delegator,
                        x.WorkEffortAndPartyAssign, ecl, null, UtilMisc.toList(x.estimatedStartDate, x.priority), null, false);
                validWorkEfforts = EntityUtil.filterByDate(validWorkEfforts);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.WorkEffortNotFound, UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (validWorkEfforts == null) {
            validWorkEfforts = new LinkedList<>();
        }
        result.put(x.events, validWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffortAssignedEventsForRoleOfAllParties(DispatchContext ctx, WorkEffortServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        String roleTypeId = (String) context.get(x.roleTypeId);
        Locale locale = (Locale) context.get(x.locale);

        List<GenericValue> validWorkEfforts = null;

        try {
            List<EntityExpr> conditionList = new LinkedList<>();
            conditionList.add(EntityCondition.makeCondition(x.roleTypeId, EntityOperator.EQUALS, roleTypeId));
            conditionList.add(EntityCondition.makeCondition(x.workEffortTypeId, EntityOperator.EQUALS, x.EVENT));
            conditionList.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_DECLINED));
            conditionList.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_DELEGATED));
            conditionList.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_COMPLETED));
            conditionList.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_CANCELLED));

            EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(conditionList, EntityOperator.AND);
            validWorkEfforts = DaoRegistry.getDao(delegator, x.WorkEffortAndPartyAssign, UserLoginDao.class).findByCondition(delegator,
                    x.WorkEffortAndPartyAssign, ecl, null, UtilMisc.toList(x.estimatedStartDate, x.priority), null, false);
            validWorkEfforts = EntityUtil.filterByDate(validWorkEfforts);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.WorkEffortNotFound, UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        Map<String, Object> result = new HashMap<>();
        if (validWorkEfforts == null) {
            validWorkEfforts = new LinkedList<>();
        }
        result.put(x.events, validWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffortAssignedTasks(DispatchContext ctx, WorkEffortServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        List<GenericValue> validWorkEfforts = null;

        if (userLogin != null && userLogin.get(x.partyId) != null) {
            try {
                EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(
                        EntityOperator.AND,
                        EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, userLogin.get(x.partyId)),
                        EntityCondition.makeCondition(x.workEffortTypeId, EntityOperator.EQUALS, x.TASK),
                        EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_DECLINED),
                        EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_DELEGATED),
                        EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_COMPLETED),
                        EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_CANCELLED),
                        EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.PRTYASGN_UNASSIGNED));
                validWorkEfforts = DaoRegistry.getDao(delegator, x.WorkEffortAndPartyAssign, UserLoginDao.class).findByCondition(delegator,
                        x.WorkEffortAndPartyAssign, ecl, null, UtilMisc.toList(x.priority), null, false);
                validWorkEfforts = EntityUtil.filterByDate(validWorkEfforts);
                ecl = EntityCondition.makeCondition(
                        EntityOperator.AND,
                        EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, userLogin.get(x.partyId)),
                        EntityCondition.makeCondition(x.workEffortTypeId, EntityOperator.EQUALS, x.PROD_ORDER_TASK),
                        EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.PRUN_CANCELLED_f71536b9),
                        EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.PRUN_COMPLETED),
                        EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.PRUN_CLOSED));
                List<GenericValue> prodOrderTasks = DaoRegistry.getDao(delegator, x.WorkEffortAndPartyAssign, UserLoginDao.class)
                        .findByCondition(delegator, x.WorkEffortAndPartyAssign, ecl, null, UtilMisc.toList(x.createdDate_DESC), null, false);
                validWorkEfforts.addAll(EntityUtil.filterByDate(prodOrderTasks));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.WorkEffortNotFound, UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (validWorkEfforts == null) {
            validWorkEfforts = new LinkedList<>();
        }
        validWorkEfforts = WorkEffortWorker.removeDuplicateWorkEfforts(validWorkEfforts);
        result.put(x.tasks, validWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffortAssignedActivities(DispatchContext ctx, WorkEffortServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        List<GenericValue> validWorkEfforts = null;

        if (userLogin != null && userLogin.get(x.partyId) != null) {
            try {
                List<EntityExpr> constraints = new LinkedList<>();

                constraints.add(EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, userLogin.get(x.partyId)));
                constraints.add(EntityCondition.makeCondition(x.workEffortTypeId, EntityOperator.EQUALS, x.ACTIVITY));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_DECLINED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_DELEGATED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_COMPLETED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_CANCELLED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.PRTYASGN_UNASSIGNED));
                constraints.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.WF_COMPLETED));
                constraints.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.WF_TERMINATED));
                constraints.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.WF_ABORTED));

                validWorkEfforts = DaoRegistry.getDao(delegator, x.WorkEffortAndPartyAssign, UserLoginDao.class).findByCondition(delegator,
                        x.WorkEffortAndPartyAssign, EntityCondition.makeCondition(constraints, EntityOperator.AND), null,
                        UtilMisc.toList(x.priority), null, false);
                validWorkEfforts = EntityUtil.filterByDate(validWorkEfforts);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.WorkEffortNotFound, UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (validWorkEfforts == null) {
            validWorkEfforts = new LinkedList<>();
        }
        result.put(x.activities, validWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffortAssignedActivitiesByRole(DispatchContext ctx, WorkEffortServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        List<GenericValue> roleWorkEfforts = null;

        if (userLogin != null && userLogin.get(x.partyId) != null) {
            try {
                List<EntityExpr> constraints = new LinkedList<>();

                constraints.add(EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, userLogin.get(x.partyId)));
                constraints.add(EntityCondition.makeCondition(x.workEffortTypeId, EntityOperator.EQUALS, x.ACTIVITY));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_DECLINED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_DELEGATED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_COMPLETED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_CANCELLED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.PRTYASGN_UNASSIGNED));
                constraints.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.WF_COMPLETED));
                constraints.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.WF_TERMINATED));
                constraints.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.WF_ABORTED));

                roleWorkEfforts = DaoRegistry.getDao(delegator, x.WorkEffortPartyAssignByRole, UserLoginDao.class).findByCondition(delegator,
                        x.WorkEffortPartyAssignByRole, EntityCondition.makeCondition(constraints, EntityOperator.AND), null,
                        UtilMisc.toList(x.priority), null, false);
                roleWorkEfforts = EntityUtil.filterByDate(roleWorkEfforts);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.WorkEffortNotFound, UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (roleWorkEfforts == null) {
            roleWorkEfforts = new LinkedList<>();
        }
        result.put(x.roleActivities, roleWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffortAssignedActivitiesByGroup(DispatchContext ctx, WorkEffortServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        List<GenericValue> groupWorkEfforts = null;

        if (userLogin != null && userLogin.get(x.partyId) != null) {
            try {
                List<EntityExpr> constraints = new LinkedList<>();

                constraints.add(EntityCondition.makeCondition(x.partyId, EntityOperator.EQUALS, userLogin.get(x.partyId)));
                constraints.add(EntityCondition.makeCondition(x.workEffortTypeId, EntityOperator.EQUALS, x.ACTIVITY));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_DECLINED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_DELEGATED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_COMPLETED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.CAL_CANCELLED));
                constraints.add(EntityCondition.makeCondition(x.statusId, EntityOperator.NOT_EQUAL, x.PRTYASGN_UNASSIGNED));
                constraints.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.WF_COMPLETED));
                constraints.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.WF_TERMINATED));
                constraints.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.WF_ABORTED));

                groupWorkEfforts = DaoRegistry.getDao(delegator, x.WorkEffortPartyAssignByGroup, UserLoginDao.class).findByCondition(delegator,
                        x.WorkEffortPartyAssignByGroup, EntityCondition.makeCondition(constraints, EntityOperator.AND), null,
                        UtilMisc.toList(x.priority), null, false);
                groupWorkEfforts = EntityUtil.filterByDate(groupWorkEfforts);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.WorkEffortNotFound, UtilMisc.toMap(x.errorString, e.toString()), locale));
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (groupWorkEfforts == null) {
            groupWorkEfforts = new LinkedList<>();
        }
        result.put(x.groupActivities, groupWorkEfforts);
        return result;
    }

    public static Map<String, Object> getWorkEffort(DispatchContext ctx, WorkEffortServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Security security = ctx.getSecurity();
        Map<String, Object> resultMap = new HashMap<>();

        String workEffortId = (String) context.get(x.workEffortId);
        GenericValue workEffort = null;

        try {
            workEffort = DaoRegistry.getDao(delegator, x.WorkEffort, UserLoginDao.class).findOne(delegator, x.WorkEffort,
                    UtilMisc.toMap(x.workEffortId, workEffortId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        Boolean canView = null;
        List<GenericValue> workEffortPartyAssignments = null;
        Boolean tryEntity = null;
        GenericValue currentStatus = null;

        if (workEffort == null) {
            tryEntity = Boolean.FALSE;
            canView = Boolean.TRUE;

            String statusId = (String) context.get(x.currentStatusId);

            if (UtilValidate.isNotEmpty(statusId)) {
                try {
                    currentStatus = DaoRegistry.getDao(delegator, x.StatusItem, UserLoginDao.class).findOne(delegator, x.StatusItem,
                            UtilMisc.toMap(x.statusId, statusId), true);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
            }
        } else {
            // get a list of workEffortPartyAssignments, if empty then this user CANNOT view the event, unless they have permission to view all
            if (userLogin != null && userLogin.get(x.partyId) != null && workEffortId != null) {
                try {
                    workEffortPartyAssignments = DaoRegistry.getDao(delegator, x.WorkEffortPartyAssignment, UserLoginDao.class).findByAnd(
                            delegator, x.WorkEffortPartyAssignment,
                            UtilMisc.toMap(x.workEffortId, workEffortId, x.partyId, userLogin.get(x.partyId)), null, false);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
            }
            canView = (UtilValidate.isNotEmpty(workEffortPartyAssignments)) ? Boolean.TRUE : Boolean.FALSE;
            if (!canView && security.hasEntityPermission(x.WORKEFFORTMGR, x.VIEW, userLogin)) {
                canView = Boolean.TRUE;
            }

            tryEntity = Boolean.TRUE;

            if (workEffort.get(x.currentStatusId) != null) {
                try {
                    currentStatus = DaoRegistry.getDao(delegator, x.StatusItem, UserLoginDao.class).findOne(delegator, x.StatusItem,
                            UtilMisc.toMap(x.statusId, workEffort.get(x.currentStatusId)), true);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
            }
        }

        if (workEffortId != null) {
            resultMap.put(x.workEffortId, workEffortId);
        }
        if (workEffort != null) {
            resultMap.put(x.workEffort, workEffort);
        }
        if (canView != null) {
            resultMap.put(x.canView, canView);
        }
        if (workEffortPartyAssignments != null) {
            resultMap.put(x.partyAssigns, workEffortPartyAssignments);
        }
        if (tryEntity != null) {
            resultMap.put(x.tryEntity, tryEntity);
        }
        if (currentStatus != null) {
            resultMap.put(x.currentStatusItem, currentStatus);
        }
        return resultMap;
    }

    private static TreeMap<DateRange, List<Map<String, Object>>> groupCalendarEntriesByDateRange(DateRange inDateRange,
            List<Map<String, Object>> calendarEntries) {
        TreeMap<DateRange, List<Map<String, Object>>> calendarEntriesByDateRange = new TreeMap<>();
        Set<Date> dateBoundaries = new TreeSet<>();
        if (inDateRange != null) {
            dateBoundaries.add(inDateRange.start());
            dateBoundaries.add(inDateRange.end());
        }
        for (Map<String, Object> calendarEntry: calendarEntries) {
            DateRange calEntryRange = (DateRange) calendarEntry.get(x.calEntryRange);
            dateBoundaries.add(calEntryRange.start());
            dateBoundaries.add(calEntryRange.end());
        }
        Date prevDateBoundary = null;
        for (Date dateBoundary: dateBoundaries) {
            if (prevDateBoundary != null) {
                DateRange dateRange = new DateRange(prevDateBoundary, dateBoundary);
                for (Map<String, Object> calendarEntry: calendarEntries) {
                    DateRange calEntryRange = (DateRange) calendarEntry.get(x.calEntryRange);
                    if (calEntryRange.intersectsRange(dateRange) && !(calEntryRange.end().equals(dateRange.start())
                            || calEntryRange.start().equals(dateRange.end()))) {
                        List<Map<String, Object>> calendarEntryByDateRangeList = calendarEntriesByDateRange.get(dateRange);
                        if (calendarEntryByDateRangeList == null) {
                            calendarEntryByDateRangeList = new LinkedList<>();
                        }
                        calendarEntryByDateRangeList.add(calendarEntry);
                        calendarEntriesByDateRange.put(dateRange, calendarEntryByDateRangeList);
                    }
                }
            }
            prevDateBoundary = dateBoundary;
        }
        return calendarEntriesByDateRange;
    }

    private static List<EntityCondition> getDefaultWorkEffortExprList(String calendarType, Collection<String> partyIds,
            String workEffortTypeId, List<EntityCondition> cancelledCheckAndList) {
        List<EntityCondition> entityExprList = new LinkedList<>();
        if (cancelledCheckAndList != null) {
            entityExprList.addAll(cancelledCheckAndList);
        }
        List<EntityExpr> typesList = new LinkedList<>();
        if (UtilValidate.isNotEmpty(workEffortTypeId)) {
            typesList.add(EntityCondition.makeCondition(x.workEffortTypeId, EntityOperator.EQUALS, workEffortTypeId));
        }
        if (x.CAL_PERSONAL.equals(calendarType)) {
            // public events are always included to the "personal calendar"
            List<EntityCondition> publicEvents = UtilMisc.<EntityCondition>toList(
                    EntityCondition.makeCondition(x.scopeEnumId, EntityOperator.EQUALS, x.WES_PUBLIC),
                    EntityCondition.makeCondition(x.parentTypeId, EntityOperator.EQUALS, x.EVENT));
            if (UtilValidate.isNotEmpty(partyIds)) {
                entityExprList.add(
                        EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition(x.partyId, EntityOperator.IN, partyIds),
                                EntityCondition.makeCondition(publicEvents, EntityJoinOperator.AND)), EntityJoinOperator.OR));
            }
        }
        if (x.CAL_MANUFACTURING.equals(calendarType)) {
            entityExprList.add(
                    EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition(x.workEffortTypeId, EntityOperator.EQUALS, x.PROD_ORDER_HEADER),
                            EntityCondition.makeCondition(x.workEffortTypeId, EntityOperator.EQUALS, x.PROD_ORDER_TASK)), EntityJoinOperator.OR));
        }
        EntityCondition typesCondition = null;
        if (typesList.isEmpty()) {
            return entityExprList;
        } else if (typesList.size() == 1) {
            typesCondition = typesList.get(0);
        } else {
            typesCondition = EntityCondition.makeCondition(typesList, EntityJoinOperator.OR);
        }
        entityExprList.add(typesCondition);
        return entityExprList;
    }

    /**
     * Get Work Efforts by period.
     * <p>
     * This method takes the following parameters:
     *
     * <ul>
     *   <li>start - TimeStamp (Period start date/time)</li>
     *   <li>numPeriods - Integer</li>
     *   <li>periodType - Integer (see java.util.Calendar)</li>
     *   <li>eventStatus - String</li>
     *   <li>partyId - String</li>
     *   <li>partyIds - List</li>
     *   <li>facilityId - String</li>
     *   <li>fixedAssetId - String</li>
     *   <li>filterOutCanceledEvents - Boolean</li>
     *   <li>entityExprList - List</li>
     * </ul>
     * <p>
     * The method will find all matching Work Effort events and return them as a List called
     * <b>periods</b> - one List element per period. It also returns a
     * <b>maxConcurrentEntries</b> Integer - which indicates the maximum number of
     * Work Efforts found in one period.
     *
     * <p>
     * Each <b>periods</b> list element is a Map containing the following
     * key/value pairs:
     *
     * <ul>
     *   <li>start - TimeStamp (Period start date/time)</li>
     *   <li>end - TimeStamp (Period end date/time)</li>
     *   <li>calendarEntries - List of Maps. Each Map contains the following key/value pairs</li>
     *   <li><ul>
     *       <li>workEffort - GenericValue</li>
     *       <li>periodSpan - Integer (Number of periods this Work Effort spans)</li>
     *       <li>startOfPeriod - Boolean (true if this is the first occurrence in the period range)</li>
     *   </ul></li>
     * </ul>
     */

    public static Map<String, Object> getWorkEffortEventsByPeriod(DispatchContext ctx, WorkEffortServicesContext context) {

        /*
         To create testdata for  this function for  fixedasset/facility

        1) go to Manufacturing -> JobShop, then click on "create new Production run":
                https://localhost:8443/manufacturing/control/CreateProductionRun
        2) enter as productId "PROD_MANUF", quantity 1, start date tomorrow and press the submit button
    `    3) in the next screen, click on the "Confirm" link (top part of the sccreen)

        Now you have a confirmed production run (starting tomorrow) happening in facility "WebStoreWarehouse",
        with a task happening in fixed asset "WORKCENTER_COST"

        In the calendars screen, selecting the proper facility you should see the work effort associated to the production run;
        if you select the proper fixed asset you should see the task.

         */
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        TimeZone timeZone = (TimeZone) context.get(x.timeZone);

        Timestamp startDay = (Timestamp) context.get(x.start);
        Integer numPeriodsInteger = (Integer) context.get(x.numPeriods);

        String calendarType = (String) context.get(x.calendarType);
        if (UtilValidate.isEmpty(calendarType)) {
            // This is a bad idea. This causes the service to return only those work efforts that are assigned
            // to the current user even when the service parameters have nothing to do with the current user.
            calendarType = x.CAL_PERSONAL;
        }
        String partyId = (String) context.get(x.partyId);
        Collection<String> partyIds = UtilGenerics.cast(context.get(x.partyIds));
        String facilityId = (String) context.get(x.facilityId);
        String fixedAssetId = (String) context.get(x.fixedAssetId);
        String workEffortTypeId = (String) context.get(x.workEffortTypeId);
        Boolean filterOutCanceledEvents = (Boolean) context.get(x.filterOutCanceledEvents);
        if (filterOutCanceledEvents == null) {
            filterOutCanceledEvents = Boolean.FALSE;
        }

        // To be returned, the max concurrent entries for a single period
        int maxConcurrentEntries = 0;

        Integer periodTypeObject = (Integer) context.get(x.periodType);
        int periodType = 0;
        if (periodTypeObject != null) {
            periodType = periodTypeObject;
        }

        int numPeriods = 0;
        if (numPeriodsInteger != null) {
            numPeriods = numPeriodsInteger;
        }

        // get a timestamp (date) for the beginning of today and for beginning of numDays+1 days from now
        // Commenting this out because it interferes with periods that do not start at the beginning of the day
        Timestamp startStamp = startDay;
        Timestamp endStamp = UtilDateTime.adjustTimestamp(startStamp, periodType, 1, timeZone, locale);
        long periodLen = endStamp.getTime() - startStamp.getTime();
        endStamp = UtilDateTime.adjustTimestamp(startStamp, periodType, numPeriods, timeZone, locale);

        // Get the WorkEfforts
        List<GenericValue> validWorkEfforts = null;
        Collection<String> partyIdsToUse = partyIds;
        if (partyIdsToUse == null) {
            partyIdsToUse = new HashSet<>();
        }
        if (UtilValidate.isNotEmpty(partyId)) {
            if (partyId.equals(userLogin.getString(x.partyId)) || security.hasEntityPermission(x.WORKEFFORTMGR, x.VIEW, userLogin)) {
                partyIdsToUse.add(partyId);
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.WorkEffortPartyPermissionError, UtilMisc.toMap(x.partyId, partyId), locale));
            }
        } else {
            if (x.CAL_PERSONAL.equals(calendarType) && UtilValidate.isNotEmpty(userLogin.getString(x.partyId))) {
                partyIdsToUse.add(userLogin.getString(x.partyId));
            }
        }

        // cancelled status id's
        List<EntityCondition> cancelledCheckAndList = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.EVENT_CANCELLED),
                EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.CAL_CANCELLED),
                EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.PRUN_CANCELLED));


        List<EntityCondition> entityExprList = UtilGenerics.cast(context.get(x.entityExprList));
        if (entityExprList == null) {
            entityExprList = getDefaultWorkEffortExprList(calendarType, partyIdsToUse, workEffortTypeId, cancelledCheckAndList);
        }

        if (UtilValidate.isNotEmpty(facilityId)) {
            entityExprList.add(EntityCondition.makeCondition(x.facilityId, EntityOperator.EQUALS, facilityId));
        }
        if (UtilValidate.isNotEmpty(fixedAssetId)) {
            entityExprList.add(EntityCondition.makeCondition(x.fixedAssetId, EntityOperator.EQUALS, fixedAssetId));
        }

        // should have at least a start date
        EntityCondition startDateRequired = EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition(x.estimatedStartDate, EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition(x.actualStartDate, EntityOperator.NOT_EQUAL, null)), EntityJoinOperator.OR);

        List<EntityCondition> periodCheckAndlList = UtilMisc.<EntityCondition>toList(
                startDateRequired,
                // the startdate should be less than the period end
                EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                        EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                                EntityCondition.makeCondition(x.actualStartDate, EntityOperator.EQUALS, null),
                                EntityCondition.makeCondition(x.estimatedStartDate, EntityOperator.NOT_EQUAL, null),
                                EntityCondition.makeCondition(x.estimatedStartDate, EntityOperator.LESS_THAN_EQUAL_TO, endStamp)),
                                EntityJoinOperator.AND),
                        EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                                EntityCondition.makeCondition(x.actualStartDate, EntityOperator.NOT_EQUAL, null),
                                EntityCondition.makeCondition(x.actualStartDate, EntityOperator.LESS_THAN_EQUAL_TO, endStamp)),
                                EntityJoinOperator.AND)),
                        EntityJoinOperator.OR),
                // if the completion date is not null then it should be larger than the period start
                EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                        // can also be empty
                        EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                                EntityCondition.makeCondition(x.estimatedCompletionDate, EntityOperator.EQUALS, null),
                                EntityCondition.makeCondition(x.actualCompletionDate, EntityOperator.EQUALS, null)),
                                EntityJoinOperator.AND),
                        // check estimated value if the actual is not provided
                        EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                                EntityCondition.makeCondition(x.actualCompletionDate, EntityOperator.EQUALS, null),
                                EntityCondition.makeCondition(x.estimatedCompletionDate, EntityOperator.NOT_EQUAL, null),
                                EntityCondition.makeCondition(x.estimatedCompletionDate, EntityOperator.GREATER_THAN_EQUAL_TO, startStamp)),
                                EntityJoinOperator.AND),
                        // at last check the actual value
                        EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                                EntityCondition.makeCondition(x.actualCompletionDate, EntityOperator.NOT_EQUAL, null),
                                EntityCondition.makeCondition(x.actualCompletionDate, EntityOperator.GREATER_THAN_EQUAL_TO, startStamp)),
                                EntityJoinOperator.AND)), EntityJoinOperator.OR));

        entityExprList.addAll(periodCheckAndlList);

        try {
            List<GenericValue> tempWorkEfforts = null;
            if (UtilValidate.isNotEmpty(partyIdsToUse)) {
                tempWorkEfforts = DaoRegistry.getDao(delegator, x.WorkEffortAndPartyAssignAndType, UserLoginDao.class).findByCondition(delegator,
                        x.WorkEffortAndPartyAssignAndType, EntityCondition.makeCondition(entityExprList, EntityOperator.AND), null,
                        UtilMisc.toList(x.estimatedStartDate), null, false);
                tempWorkEfforts = EntityUtil.filterByDate(tempWorkEfforts);
            } else {
                tempWorkEfforts = DaoRegistry.getDao(delegator, x.WorkEffort, UserLoginDao.class).findByCondition(delegator, x.WorkEffort,
                        EntityCondition.makeCondition(entityExprList, EntityOperator.AND), null, UtilMisc.toList(x.estimatedStartDate), null,
                        false);
            }
            if (!x.CAL_PERSONAL.equals(calendarType) && UtilValidate.isNotEmpty(fixedAssetId)) {
                // Get "new style" work efforts
                List<GenericValue> fixedAssetWorkEfforts = DaoRegistry.getDao(delegator, x.WorkEffortAndFixedAssetAssign, UserLoginDao.class)
                        .findByCondition(delegator, x.WorkEffortAndFixedAssetAssign,
                                EntityCondition.makeCondition(entityExprList, EntityOperator.AND), null,
                                UtilMisc.toList(x.estimatedStartDate), null, false);
                tempWorkEfforts.addAll(EntityUtil.filterByDate(fixedAssetWorkEfforts));
            }
            validWorkEfforts = WorkEffortWorker.removeDuplicateWorkEfforts(tempWorkEfforts);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        // Split the WorkEffort list into a map with entries for each period, period start is the key
        List<Map<String, Object>> periods = new LinkedList<>();
        if (validWorkEfforts != null) {
            List<DateRange> periodRanges = new LinkedList<>();
            for (int i = 0; i < numPeriods; i++) {
                Timestamp curPeriodStart = UtilDateTime.adjustTimestamp(startStamp, periodType, i, timeZone, locale);
                Timestamp curPeriodEnd = UtilDateTime.adjustTimestamp(curPeriodStart, periodType, 1, timeZone, locale);
                curPeriodEnd = new Timestamp(curPeriodEnd.getTime() - 1);
                periodRanges.add(new DateRange(curPeriodStart, curPeriodEnd));
            }
            try {
                // Process recurring work efforts
                Set<GenericValue> exclusions = new HashSet<>();
                Set<GenericValue> inclusions = new HashSet<>();
                DateRange range = new DateRange(startStamp, endStamp);
                Calendar cal = UtilDateTime.toCalendar(startStamp, timeZone, locale);
                for (GenericValue workEffort : validWorkEfforts) {
                    if (UtilValidate.isNotEmpty(workEffort.getString(x.tempExprId))) {
                        // check if either the workeffort is public or the requested party is a member
                        if (UtilValidate.isNotEmpty(partyIdsToUse) && !x.WES_PUBLIC.equals(workEffort.getString(x.scopeEnumId))
                                && !partyIdsToUse.contains(workEffort.getString(x.partyId))) {
                            continue;
                        }
                        // if the workeffort has actual date time, using temporal expression has no sense
                        if (UtilValidate.isNotEmpty(workEffort.getTimestamp(x.actualStartDate))
                                || UtilValidate.isNotEmpty(workEffort.getTimestamp(x.actualCompletionDate))) {
                            continue;
                        }
                        TemporalExpression tempExpr = TemporalExpressionWorker.getTemporalExpression(delegator, workEffort.getString(x.tempExprId));
                        DateRange weRange = new DateRange(workEffort.getTimestamp(x.estimatedStartDate),
                                workEffort.getTimestamp(x.estimatedCompletionDate));

                        Set<Date> occurrences = tempExpr.getRange(range, cal);
                        for (Date occurrence : occurrences) {
                            for (DateRange periodRange : periodRanges) {
                                if (periodRange.includesDate(occurrence)) {
                                    GenericValue cloneWorkEffort = (GenericValue) workEffort.clone();
                                    TimeDuration duration = TimeDuration.fromNumber(workEffort.getDouble(x.estimatedMilliSeconds));
                                    if (!duration.isZero()) {
                                        Calendar endCal = UtilDateTime.toCalendar(occurrence, timeZone, locale);
                                        Date endDate = duration.addToCalendar(endCal).getTime();
                                        cloneWorkEffort.set(x.estimatedStartDate, new Timestamp(occurrence.getTime()));
                                        cloneWorkEffort.set(x.estimatedCompletionDate, new Timestamp(endDate.getTime()));
                                    } else {
                                        cloneWorkEffort.set(x.estimatedStartDate, periodRange.startStamp());
                                        cloneWorkEffort.set(x.estimatedCompletionDate, periodRange.endStamp());
                                    }
                                    if (weRange.includes(cloneWorkEffort.getTimestamp(x.estimatedStartDate))) {
                                        inclusions.add(cloneWorkEffort);
                                    }
                                }
                            }
                        }
                        exclusions.add(workEffort);
                    }
                }
                validWorkEfforts.removeAll(exclusions);
                validWorkEfforts.addAll(inclusions);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }

            // For each period in the set we check all work efforts to see if they fall within range
            boolean firstEntry = true;
            for (DateRange periodRange : periodRanges) {
                List<Map<String, Object>> curWorkEfforts = new LinkedList<>();
                Map<String, Object> entry = new HashMap<>();
                for (GenericValue workEffort : validWorkEfforts) {
                    Timestamp startDate = workEffort.getTimestamp(x.estimatedStartDate);
                    if (workEffort.getTimestamp(x.actualStartDate) != null) {
                        startDate = workEffort.getTimestamp(x.actualStartDate);
                    }
                    Timestamp endDate = workEffort.getTimestamp(x.estimatedCompletionDate);
                    if (workEffort.getTimestamp(x.actualCompletionDate) != null) {
                        endDate = workEffort.getTimestamp(x.actualCompletionDate);
                    }
                    if (endDate == null) {
                        endDate = startDate;
                    }
                    DateRange weRange = new DateRange(startDate, endDate);
                    if (periodRange.intersectsRange(weRange)) {
                        Map<String, Object> calEntry = new HashMap<>();
                        calEntry.put(x.workEffort, workEffort);
                        long length = ((weRange.end().after(endStamp) ? endStamp.getTime() : weRange.end().getTime())
                                - (weRange.start().before(startStamp) ? startStamp.getTime() : weRange.start().getTime()));
                        int periodSpan = (int) Math.ceil((double) length / periodLen);
                        if (length % periodLen == 0 && startDate.getTime() > periodRange.start().getTime()) {
                            periodSpan++;
                        }
                        calEntry.put(x.periodSpan, periodSpan);
                        DateRange calEntryRange = new DateRange((weRange.start().before(startStamp) ? startStamp
                                : weRange.start()), (weRange.end().after(endStamp) ? endStamp : weRange.end()));
                        calEntry.put(x.calEntryRange, calEntryRange);
                        if (firstEntry) {
                            // If this is the first period any valid entry is starting here
                            calEntry.put(x.startOfPeriod, Boolean.TRUE);
                            firstEntry = false;
                        } else {
                            boolean startOfPeriod = ((weRange.start().getTime() - periodRange.start().getTime()) >= 0);
                            calEntry.put(x.startOfPeriod, startOfPeriod);
                        }
                        curWorkEfforts.add(calEntry);
                    }
                }
                int numEntries = curWorkEfforts.size();
                if (numEntries > maxConcurrentEntries) {
                    maxConcurrentEntries = numEntries;
                }
                entry.put(x.start, periodRange.startStamp());
                entry.put(x.end, periodRange.endStamp());
                entry.put(x.calendarEntries, curWorkEfforts);
                entry.put(x.calendarEntriesByDateRange, groupCalendarEntriesByDateRange(periodRange, curWorkEfforts));
                periods.add(entry);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put(x.periods, periods);
        result.put(x.maxConcurrentEntries, maxConcurrentEntries);
        return result;
    }

    public static Map<String, Object> getProductManufacturingSummaryByFacility(DispatchContext ctx, WorkEffortServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        String productId = (String) context.get(x.productId);
        String facilityId = (String) context.get(x.facilityId); // optional
        Locale locale = (Locale) context.get(x.locale);

        Map<String, Map<String, Object>> summaryInByFacility = new HashMap<>();
        Map<String, Map<String, Object>> summaryOutByFacility = new HashMap<>();
        try {
            //
            // Information about the running production runs that are going
            // to produce units of productId by facility.
            //
            List<EntityCondition> findIncomingProductionRunsConds = new LinkedList<>();

            findIncomingProductionRunsConds.add(EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, productId));
            findIncomingProductionRunsConds.add(EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.WEGS_CREATED));
            findIncomingProductionRunsConds.add(EntityCondition.makeCondition(x.workEffortGoodStdTypeId, EntityOperator.EQUALS, x.PRUN_PROD_DELIV));
            if (facilityId != null) {
                findIncomingProductionRunsConds.add(EntityCondition.makeCondition(x.facilityId, EntityOperator.EQUALS, facilityId));
            }

            List<EntityCondition> findIncomingProductionRunsStatusConds = new LinkedList<>();
            findIncomingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_CREATED));
            findIncomingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_SCHEDULED));
            findIncomingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_DOC_PRINTED));
            findIncomingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_RUNNING));
            findIncomingProductionRunsConds.add(EntityCondition.makeCondition(findIncomingProductionRunsStatusConds, EntityOperator.OR));

            List<GenericValue> incomingProductionRuns = DaoRegistry.getDao(delegator, x.WorkEffortAndGoods, UserLoginDao.class).findByCondition(
                    delegator, x.WorkEffortAndGoods, EntityCondition.makeCondition(findIncomingProductionRunsConds, EntityOperator.AND), null,
                    UtilMisc.toList(x.estimatedCompletionDate_7e07b896), null, false);
            for (GenericValue incomingProductionRun: incomingProductionRuns) {
                double producedQtyTot = 0.0;
                if (x.PRUN_COMPLETED.equals(incomingProductionRun.getString(x.currentStatusId))) {
                    List<GenericValue> inventoryItems = DaoRegistry.getDao(delegator, x.WorkEffortAndInventoryProduced, UserLoginDao.class)
                            .findByAnd(delegator, x.WorkEffortAndInventoryProduced,
                                    UtilMisc.toMap(x.productId, productId, x.workEffortId, incomingProductionRun.getString(x.workEffortId)), null,
                                    false);
                    for (GenericValue inventoryItem: inventoryItems) {
                        GenericValue inventoryItemDetail = DaoRegistry.getDao(delegator, x.InventoryItemDetail, UserLoginDao.class)
                                .findFirstByCondition(delegator, x.InventoryItemDetail,
                                        EntityCondition.makeCondition(x.inventoryItemId, EntityOperator.EQUALS,
                                                inventoryItem.getString(x.inventoryItemId)),
                                        null, UtilMisc.toList(x.inventoryItemDetailSeqId), false);
                        if (inventoryItemDetail != null && inventoryItemDetail.get(x.quantityOnHandDiff) != null) {
                            Double inventoryItemQty = inventoryItemDetail.getDouble(x.quantityOnHandDiff);
                            producedQtyTot = producedQtyTot + inventoryItemQty;
                        }
                    }
                }
                double estimatedQuantity = 0.0;
                if (incomingProductionRun.get(x.estimatedQuantity) != null) {
                    estimatedQuantity = incomingProductionRun.getDouble(x.estimatedQuantity);
                }
                double remainingQuantity = estimatedQuantity - producedQtyTot; // the qty that still needs to be produced
                if (remainingQuantity > 0) {
                    incomingProductionRun.set(x.estimatedQuantity, remainingQuantity);
                } else {
                    continue;
                }
                String weFacilityId = incomingProductionRun.getString(x.facilityId);

                Map<String, Object> quantitySummary = UtilGenerics.cast(summaryInByFacility.get(weFacilityId));
                if (quantitySummary == null) {
                    quantitySummary = new HashMap<>();
                    quantitySummary.put(x.facilityId, weFacilityId);
                    summaryInByFacility.put(weFacilityId, quantitySummary);
                }
                Double remainingQuantityTot = (Double) quantitySummary.get(x.estimatedQuantityTotal);
                if (remainingQuantityTot == null) {
                    quantitySummary.put(x.estimatedQuantityTotal, remainingQuantity);
                } else {
                    quantitySummary.put(x.estimatedQuantityTotal, remainingQuantity + remainingQuantityTot);
                }

                List<GenericValue> incomingProductionRunList = UtilGenerics.cast(quantitySummary.get(x.incomingProductionRunList));
                if (incomingProductionRunList == null) {
                    incomingProductionRunList = new LinkedList<>();
                    quantitySummary.put(x.incomingProductionRunList, incomingProductionRunList);
                }
                incomingProductionRunList.add(incomingProductionRun);
            }
            //
            // Information about the running production runs that are going
            // to consume units of productId by facility.
            //
            List<EntityCondition> findOutgoingProductionRunsConds = new LinkedList<>();

            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, productId));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.WEGS_CREATED));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(x.workEffortGoodStdTypeId, EntityOperator.EQUALS, x.PRUNT_PROD_NEEDED));
            if (facilityId != null) {
                findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(x.facilityId, EntityOperator.EQUALS, facilityId));
            }

            List<EntityCondition> findOutgoingProductionRunsStatusConds = new LinkedList<>();
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_CREATED));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_SCHEDULED));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_DOC_PRINTED));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_RUNNING));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(findOutgoingProductionRunsStatusConds, EntityOperator.OR));

            List<GenericValue> outgoingProductionRuns = DaoRegistry.getDao(delegator, x.WorkEffortAndGoods, UserLoginDao.class).findByCondition(
                    delegator, x.WorkEffortAndGoods, EntityCondition.makeCondition(findOutgoingProductionRunsConds, EntityOperator.AND), null,
                    UtilMisc.toList(x.estimatedStartDate_2c48ab38), null, false);
            for (GenericValue outgoingProductionRun: outgoingProductionRuns) {
                String weFacilityId = outgoingProductionRun.getString(x.facilityId);
                Double neededQuantity = outgoingProductionRun.getDouble(x.estimatedQuantity);
                if (neededQuantity == null) {
                    neededQuantity = (double) 0;
                }

                Map<String, Object> quantitySummary = UtilGenerics.cast(summaryOutByFacility.get(weFacilityId));
                if (quantitySummary == null) {
                    quantitySummary = new HashMap<>();
                    quantitySummary.put(x.facilityId, weFacilityId);
                    summaryOutByFacility.put(weFacilityId, quantitySummary);
                }
                Double remainingQuantityTot = (Double) quantitySummary.get(x.estimatedQuantityTotal);
                if (remainingQuantityTot == null) {
                    quantitySummary.put(x.estimatedQuantityTotal, neededQuantity);
                } else {
                    quantitySummary.put(x.estimatedQuantityTotal, neededQuantity + remainingQuantityTot);
                }

                List<GenericValue> outgoingProductionRunList = UtilGenerics.cast(quantitySummary.get(x.outgoingProductionRunList));
                if (outgoingProductionRunList == null) {
                    outgoingProductionRunList = new LinkedList<>();
                    quantitySummary.put(x.outgoingProductionRunList, outgoingProductionRunList);
                }
                outgoingProductionRunList.add(outgoingProductionRun);
            }

        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.WorkEffortManufacturingError, UtilMisc.toMap(x.productId, productId, x.errorString, gee.getMessage()), locale));
        }
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        resultMap.put(x.summaryInByFacility, summaryInByFacility);
        resultMap.put(x.summaryOutByFacility, summaryOutByFacility);
        return resultMap;
    }

    /** Process work effort event reminders. This service is used by the job scheduler.
     * @param ctx the dispatch context
     * @param context the context
     * @return returns the result of the service execution
     */
    public static Map<String, Object> processWorkEffortEventReminders(DispatchContext ctx, WorkEffortServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale localePar = (Locale) context.get(x.locale);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<GenericValue> eventReminders = null;
        try {
            eventReminders = DaoRegistry.getDao(delegator, x.WorkEffortEventReminder, UserLoginDao.class).findByCondition(delegator,
                    x.WorkEffortEventReminder,
                    EntityCondition.makeCondition(UtilMisc.<EntityCondition>toList(
                            EntityCondition.makeCondition(x.reminderDateTime, EntityOperator.EQUALS, null),
                            EntityCondition.makeCondition(x.reminderDateTime, EntityOperator.LESS_THAN_EQUAL_TO, now)), EntityOperator.OR),
                    null, null, null, false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.WorkEffortEventRemindersRetrivingError, UtilMisc.toMap(x.errorString, e), localePar));
        }
        for (GenericValue reminder : eventReminders) {
            if (UtilValidate.isEmpty(reminder.get(x.contactMechId))) {
                continue;
            }
            int repeatCount = reminder.get(x.repeatCount) == null ? 0 : reminder.getLong(x.repeatCount).intValue();
            int currentCount = reminder.get(x.currentCount) == null ? 0 : reminder.getLong(x.currentCount).intValue();
            GenericValue workEffort = null;
            try {
                workEffort = reminder.getRelatedOne(x.WorkEffort, false);
            } catch (GenericEntityException e) {
                Debug.logWarning(x.Error_while_getting_work_effort + e, MODULE);
            }
            if (workEffort == null) {
                try {
                    reminder.remove();
                } catch (GenericEntityException e) {
                    Debug.logWarning(x.Error_while_removing_work_effort_event_reminder + e, MODULE);
                }
                continue;
            }
            Locale locale = reminder.getString(x.localeId) == null ? Locale.getDefault() : new Locale(reminder.getString(x.localeId));
            TimeZone timeZone = reminder.getString(x.timeZoneId) == null ? TimeZone.getDefault()
                    : TimeZone.getTimeZone(reminder.getString(x.timeZoneId));
            Map<String, Object> parameters = UtilMisc.toMap(x.locale, locale, x.timeZone, timeZone, x.workEffortId, reminder.get(x.workEffortId));

            Map<String, Object> processCtx = UtilMisc.toMap(x.reminder, reminder, x.bodyParameters, parameters,
                    x.userLogin, context.get(x.userLogin));

            Calendar cal = UtilDateTime.toCalendar(now, timeZone, locale);
            Timestamp reminderStamp = reminder.getTimestamp(x.reminderDateTime);
            Date eventDateTime = workEffort.getTimestamp(x.estimatedStartDate);
            String tempExprId = workEffort.getString(x.tempExprId);
            if (UtilValidate.isNotEmpty(tempExprId)) {
                TemporalExpression temporalExpression = null;
                try {
                    temporalExpression = TemporalExpressionWorker.getTemporalExpression(delegator, tempExprId);
                } catch (GenericEntityException e) {
                    Debug.logWarning(x.Error_while_getting_temporal_expression_id + tempExprId + x.str_ceca32e9 + e, MODULE);
                }
                if (temporalExpression != null) {
                    eventDateTime = temporalExpression.first(cal).getTime();
                    Date reminderDateTime = null;
                    long reminderOffset = reminder.get(x.reminderOffset) == null ? 0 : reminder.getLong(x.reminderOffset);
                    if (reminderStamp == null) {
                        if (reminderOffset != 0) {
                            cal.setTime(eventDateTime);
                            TimeDuration duration = TimeDuration.fromLong(reminderOffset);
                            duration.addToCalendar(cal);
                            reminderDateTime = cal.getTime();
                        } else {
                            reminderDateTime = eventDateTime;
                        }
                    } else {
                        reminderDateTime = new Date(reminderStamp.getTime());
                    }
                    if (reminderDateTime.before(now) && reminderStamp != null) {
                        try {
                            parameters.put(x.eventDateTime, new Timestamp(eventDateTime.getTime()));

                            Map<String, Object> result = dispatcher.runSync(x.processWorkEffortEventReminder, processCtx);
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                            if (repeatCount != 0 && currentCount + 1 >= repeatCount) {
                                reminder.remove();
                            } else {
                                cal.setTime(reminderDateTime);
                                Date newReminderDateTime = null;
                                if (reminderOffset != 0) {
                                    TimeDuration duration = TimeDuration.fromLong(-reminderOffset);
                                    duration.addToCalendar(cal);
                                    cal.setTime(temporalExpression.next(cal).getTime());
                                    duration = TimeDuration.fromLong(reminderOffset);
                                    duration.addToCalendar(cal);
                                    newReminderDateTime = cal.getTime();
                                } else {
                                    newReminderDateTime = temporalExpression.next(cal).getTime();
                                }
                                reminder.set(x.currentCount, (long) (currentCount + 1));
                                reminder.set(x.reminderDateTime, new Timestamp(newReminderDateTime.getTime()));
                                reminder.store();
                            }
                        } catch (GenericEntityException e) {
                            Debug.logWarning(x.Error_while_processing_temporal_expression_reminder_id + tempExprId + x.str_ceca32e9 + e, MODULE);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, MODULE);
                        }
                    } else if (reminderStamp == null) {
                        try {
                            reminder.set(x.reminderDateTime, new Timestamp(reminderDateTime.getTime()));
                            reminder.store();
                        } catch (GenericEntityException e) {
                            Debug.logWarning(x.Error_while_processing_temporal_expression_reminder_id + tempExprId + x.str_ceca32e9 + e, MODULE);
                        }
                    }
                }
                continue;
            }
            if (reminderStamp != null) {
                Date reminderDateTime = new Date(reminderStamp.getTime());
                if (reminderDateTime.before(now)) {
                    try {
                        parameters.put(x.eventDateTime, eventDateTime);
                        Map<String, Object> result = dispatcher.runSync(x.processWorkEffortEventReminder, processCtx);
                        if (ServiceUtil.isError(result)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                        }
                        TimeDuration duration = TimeDuration.fromNumber(reminder.getLong(x.repeatInterval));
                        if ((repeatCount != 0 && currentCount + 1 >= repeatCount) || duration.isZero()) {
                            reminder.remove();
                        } else {
                            cal.setTime(now);
                            duration.addToCalendar(cal);
                            reminderDateTime = cal.getTime();
                            reminder.set(x.currentCount, (long) (currentCount + 1));
                            reminder.set(x.reminderDateTime, new Timestamp(reminderDateTime.getTime()));
                            reminder.store();
                        }
                    } catch (GenericEntityException e) {
                        Debug.logWarning(x.Error_while_processing_event_reminder + e, MODULE);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, MODULE);
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> processWorkEffortEventReminder(DispatchContext dctx, WorkEffortServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> parameters = UtilGenerics.cast(context.get(x.bodyParameters));
        GenericValue reminder = (GenericValue) context.get(x.reminder);
        GenericValue contactMech = null;
        try {
            contactMech = reminder.getRelatedOne(x.ContactMech, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (contactMech != null && x.EMAIL_ADDRESS.equals(contactMech.get(x.contactMechTypeId))) {
            String toAddress = contactMech.getString(x.infoString);

            GenericValue emailTemplateSetting = null;
            try {
                emailTemplateSetting = DaoRegistry.getDao(delegator, x.EmailTemplateSetting, UserLoginDao.class).findOne(delegator,
                        x.EmailTemplateSetting, UtilMisc.toMap(x.emailTemplateSettingId, x.WEFF_EVENT_REMINDER), true);
            } catch (GenericEntityException e1) {
                Debug.logError(e1, MODULE);
            }
            if (emailTemplateSetting != null) {
                Map<String, Object> emailCtx = UtilMisc.toMap(x.emailTemplateSettingId, x.WEFF_EVENT_REMINDER, x.sendTo, toAddress,
                        x.bodyParameters, parameters);
                try {
                    dispatcher.runAsync(x.sendMailFromTemplateSetting, emailCtx);
                } catch (GenericServiceException e) {
                    Debug.logWarning(x.Error_while_emailing_event_reminder_workEffortId + reminder.get(x.workEffortId) + x.contactMechId_70cdf547
                            + reminder.get(x.contactMechId) + x.str_ceca32e9 + e, MODULE);
                }
            } else {
                Debug.logError(x.No_email_template_WEFF_EVENT_REMINDER_has_been_configured_reminder_cannot_be_send, MODULE);
            }
            return ServiceUtil.returnSuccess();
        }
        // TODO: Other contact mechanism types
        Debug.logWarning(x.Invalid_event_reminder_contact_mech_workEffortId + reminder.get(x.workEffortId) + x.contactMechId_70cdf547
                + reminder.get(x.contactMechId), MODULE);
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> removeDuplicateWorkEfforts(DispatchContext ctx, WorkEffortServicesContext context) {
        List<GenericValue> resultList = null;
        try (EntityListIterator eli = (EntityListIterator) context.get(x.workEffortIterator)) {
            if (eli != null) {
                Set<String> keys = new HashSet<>();
                resultList = new LinkedList<>();
                GenericValue workEffort = eli.next();
                while (workEffort != null) {
                    String workEffortId = workEffort.getString(x.workEffortId);
                    if (!keys.contains(workEffortId)) {
                        resultList.add(workEffort);
                        keys.add(workEffortId);
                    }
                    workEffort = eli.next();
                }
            } else {
                List<GenericValue> workEfforts = UtilGenerics.cast(context.get(x.workEfforts));
                if (workEfforts != null) {
                    resultList = WorkEffortWorker.removeDuplicateWorkEfforts(workEfforts);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.workEfforts, resultList);
        return result;
    }
}
