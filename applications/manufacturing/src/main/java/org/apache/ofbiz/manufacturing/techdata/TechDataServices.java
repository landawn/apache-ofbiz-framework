/*
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
 */
package org.apache.ofbiz.manufacturing.techdata;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.TechDataCalendarDao;
import org.apache.ofbiz.persistence.dao.WorkEffortAssocDao;
import org.apache.ofbiz.persistence.dao.WorkEffortDao;
import org.apache.ofbiz.persistence.entity.TechDataCalendarEntity;
import org.apache.ofbiz.persistence.entity.WorkEffortAssocEntity;
import org.apache.ofbiz.persistence.entity.WorkEffortEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;
import com.ibm.icu.util.Calendar;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.TechDataServicesContext;
/**
 * TechDataServices - TechData related Services
 *
 */
public class TechDataServices {

    private static final String MODULE = TechDataServices.class.getName();
    private static final String RESOURCE = x.ManufacturingUiLabels;

    /**
     * Used to retrieve some RoutingTasks (WorkEffort) selected by Name or MachineGroup ordered by Name
     * @param ctx the dispatch context
     * @param context a map containing workEffortName (routingTaskName) and fixedAssetId (MachineGroup or ANY)
     * @return result a map containing lookupResult (list of RoutingTask &lt;=&gt; workEffortId with currentStatusId =
     * "ROU_ACTIVE" and workEffortTypeId = "ROU_TASK"
     */
    public static Map<String, Object> lookupRoutingTask(DispatchContext ctx, TechDataServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> result = new HashMap<>();
        Locale locale = (Locale) context.get(x.locale);
        String workEffortName = (String) context.get(x.workEffortName);
        String fixedAssetId = (String) context.get(x.fixedAssetId);

        List<GenericValue> listRoutingTask = null;

        try {
            WorkEffortDao workEffortDao = DaoRegistry.getDao(delegator, x.WorkEffort, WorkEffortDao.class);
            List<WorkEffortEntity> workEffortEntities = workEffortDao.list(Filters.and(
                    Filters.eq(x.currentStatusId, x.ROU_ACTIVE),
                    Filters.eq(x.workEffortTypeId, x.ROU_TASK)));

            listRoutingTask = new LinkedList<>();
            for (WorkEffortEntity workEffortEntity : workEffortEntities) {
                if (UtilValidate.isNotEmpty(workEffortName)) {
                    String currentName = workEffortEntity.getWorkEffortName();
                    if (currentName == null || currentName.compareTo(workEffortName) < 0) {
                        continue;
                    }
                }
                if (UtilValidate.isNotEmpty(fixedAssetId) && !x.ANY.equals(fixedAssetId)
                        && !fixedAssetId.equals(workEffortEntity.getFixedAssetId())) {
                    continue;
                }
                listRoutingTask.add(delegator.makeValue(x.WorkEffort, Beans.beanToMap(workEffortEntity)));
            }
            listRoutingTask.sort(Comparator.comparing(routingTask -> routingTask.getString(x.workEffortName),
                    Comparator.nullsFirst(Comparator.naturalOrder())));
        } catch (Exception e) {
            Debug.logWarning(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingTechDataWorkEffortNotExist,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }
        if (listRoutingTask == null) {
            listRoutingTask = new LinkedList<>();
        }
        //if (listRoutingTask.size() == 0) {
            //FIXME is it correct ?
            // listRoutingTask.add(UtilMisc.toMap("label", "no Match", "value", "NO_MATCH"));
        //}
        result.put(x.lookupResult, listRoutingTask);
        return result;
    }

    /**
     * Used to check if there is not two routing task with the same SeqId valid at the same period
     * @param ctx            The DispatchContext that this service is operating in.
     * @param context    a map containing workEffortIdFrom (routing) and SeqId, fromDate thruDate
     * @return result      a map containing sequenceNumNotOk which is equal to "Y" if it's not Ok
     */
    public static Map<String, Object> checkRoutingTaskAssoc(DispatchContext ctx, TechDataServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> result = new HashMap<>();
        String sequenceNumNotOk = x.N;
        Locale locale = (Locale) context.get(x.locale);
        String workEffortIdFrom = (String) context.get(x.workEffortIdFrom);
        String workEffortIdTo = (String) context.get(x.workEffortIdTo);
        String workEffortAssocTypeId = (String) context.get(x.workEffortAssocTypeId);
        Long sequenceNum = (Long) context.get(x.sequenceNum);
        Timestamp fromDate = (Timestamp) context.get(x.fromDate);
        Timestamp thruDate = (Timestamp) context.get(x.thruDate);
        String create = (String) context.get(x.create);

        boolean createProcess = x.Y.equals(create);
        List<GenericValue> listRoutingTaskAssoc = null;

        try {
            WorkEffortAssocDao workEffortAssocDao = DaoRegistry.getDao(delegator, x.WorkEffortAssoc, WorkEffortAssocDao.class);
            List<WorkEffortAssocEntity> workEffortAssocEntities = workEffortAssocDao.list(Filters.and(
                    Filters.eq(x.workEffortIdFrom, workEffortIdFrom),
                    Filters.eq(x.sequenceNum, sequenceNum)));
            workEffortAssocEntities.sort(Comparator.comparing(WorkEffortAssocEntity::getFromDate,
                    Comparator.nullsFirst(Comparator.naturalOrder())));

            listRoutingTaskAssoc = new LinkedList<>();
            for (WorkEffortAssocEntity workEffortAssocEntity : workEffortAssocEntities) {
                listRoutingTaskAssoc.add(delegator.makeValue(x.WorkEffortAssoc, Beans.beanToMap(workEffortAssocEntity)));
            }
        } catch (Exception e) {
            Debug.logWarning(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingTechDataWorkEffortAssocNotExist,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        if (listRoutingTaskAssoc != null) {
            for (GenericValue routingTaskAssoc : listRoutingTaskAssoc) {
                if (!workEffortIdFrom.equals(routingTaskAssoc.getString(x.workEffortIdFrom))
                        || !workEffortIdTo.equals(routingTaskAssoc.getString(x.workEffortIdTo))
                        || !workEffortAssocTypeId.equals(routingTaskAssoc.getString(x.workEffortAssocTypeId))
                        || !sequenceNum.equals(routingTaskAssoc.getLong(x.sequenceNum))) {
                    if (routingTaskAssoc.getTimestamp(x.thruDate) == null && routingTaskAssoc.getTimestamp(x.fromDate) == null) {
                        sequenceNumNotOk = x.Y;
                    } else if (routingTaskAssoc.getTimestamp(x.thruDate) == null) {
                        if (thruDate == null) sequenceNumNotOk = x.Y;
                        else if (thruDate.after(routingTaskAssoc.getTimestamp(x.fromDate))) {
                            sequenceNumNotOk = x.Y;
                        }
                    } else if (routingTaskAssoc.getTimestamp(x.fromDate) == null) {
                        if (fromDate == null) sequenceNumNotOk = x.Y;
                        else if (fromDate.before(routingTaskAssoc.getTimestamp(x.thruDate))) {
                            sequenceNumNotOk = x.Y;
                        }
                    } else if (fromDate == null && thruDate == null) {
                        sequenceNumNotOk = x.Y;
                    } else if (thruDate == null) {
                        if (fromDate.before(routingTaskAssoc.getTimestamp(x.thruDate))) sequenceNumNotOk = x.Y;
                    } else if (fromDate == null) {
                        if (thruDate.after(routingTaskAssoc.getTimestamp(x.fromDate))) sequenceNumNotOk = x.Y;
                    } else if (routingTaskAssoc.getTimestamp(x.fromDate).before(thruDate) && fromDate.before(routingTaskAssoc.getTimestamp(
                            x.thruDate))) {
                        sequenceNumNotOk = x.Y;
                    }
                } else if (createProcess) {
                    sequenceNumNotOk = x.Y;
                }
            }
        }
        result.put(x.sequenceNumNotOk, sequenceNumNotOk);
        return result;
    }

    /**
     * Used to get the techDataCalendar for a routingTask, if there is a entity exception
     * or routingTask associated with no MachineGroup the DEFAULT TechDataCalendar is return.
     * @param routingTask    the routingTask for which we are looking for
     * @return the techDataCalendar associated
     */
    public static GenericValue getTechDataCalendar(GenericValue routingTask) {
        GenericValue machineGroup = null;
        GenericValue techDataCalendar = null;
        try {
            machineGroup = routingTask.getRelatedOne(x.FixedAsset, true);
        } catch (GenericEntityException e) {
            Debug.logError(x.Pb_reading_FixedAsset_associated_with_routingTask + e.getMessage(), MODULE);
        }
        if (machineGroup != null) {
            if (machineGroup.getString(x.calendarId) != null) {
                try {
                    techDataCalendar = machineGroup.getRelatedOne(x.TechDataCalendar, true);
                } catch (GenericEntityException e) {
                    Debug.logError(x.Pb_reading_TechDataCalendar_associated_with_machineGroup + e.getMessage(), MODULE);
                }
            } else {
                try {
                    List<GenericValue> machines = machineGroup.getRelated(x.ChildFixedAsset, null, null, true);
                    if (machines != null && !machines.isEmpty()) {
                        GenericValue machine = EntityUtil.getFirst(machines);
                        techDataCalendar = machine.getRelatedOne(x.TechDataCalendar, true);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(x.Pb_reading_machine_child_from_machineGroup + e.getMessage(), MODULE);
                }
            }
        }
        if (techDataCalendar == null) {
            try {
                Delegator delegator = routingTask.getDelegator();
                TechDataCalendarDao techDataCalendarDao = DaoRegistry.getDao(delegator, x.TechDataCalendar, TechDataCalendarDao.class);
                TechDataCalendarEntity techDataCalendarEntity = techDataCalendarDao.get(x.DEFAULT).orElse(null);
                if (techDataCalendarEntity != null) {
                    techDataCalendar = delegator.makeValue(x.TechDataCalendar, Beans.beanToMap(techDataCalendarEntity));
                }
            } catch (Exception e) {
                Debug.logError(x.Pb_reading_TechDataCalendar_DEFAULT + e.getMessage(), MODULE);
            }
        }
        return techDataCalendar;
    }

    /**
     * Used to find the first day in the TechDataCalendarWeek where capacity != 0, beginning at the given dateFrom
     * (included).
     * @param techDataCalendar       The TechDataCalendar entity that contains exceptions
     * @param techDataCalendarWeek   The TechDataCalendarWeek coverage data
     * @param dateFrom               The starting date to begin checking availability
     * @return a map with the capacity (Double) available, startTime (Time), and moveDay (int): the number of days to
     * move forward to find availability
     */
    public static Map<String, Object> dayStartCapacityAvailable(
            GenericValue techDataCalendar,
            GenericValue techDataCalendarWeek,
            Timestamp dateFrom) {
        Map<String, Object> result = new HashMap<>();
        int moveDay = 0;
        Double capacity = null;
        Time startTime = null;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateFrom);

        while (true) {
            Timestamp currentDate = new Timestamp(calendar.getTimeInMillis());

            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            switch (dayOfWeek) {
            case Calendar.MONDAY:
                capacity = techDataCalendarWeek.getDouble(x.mondayCapacity);
                startTime = techDataCalendarWeek.getTime(x.mondayStartTime);
                break;
            case Calendar.TUESDAY:
                capacity = techDataCalendarWeek.getDouble(x.tuesdayCapacity);
                startTime = techDataCalendarWeek.getTime(x.tuesdayStartTime);
                break;
            case Calendar.WEDNESDAY:
                capacity = techDataCalendarWeek.getDouble(x.wednesdayCapacity);
                startTime = techDataCalendarWeek.getTime(x.wednesdayStartTime);
                break;
            case Calendar.THURSDAY:
                capacity = techDataCalendarWeek.getDouble(x.thursdayCapacity);
                startTime = techDataCalendarWeek.getTime(x.thursdayStartTime);
                break;
            case Calendar.FRIDAY:
                capacity = techDataCalendarWeek.getDouble(x.fridayCapacity);
                startTime = techDataCalendarWeek.getTime(x.fridayStartTime);
                break;
            case Calendar.SATURDAY:
                capacity = techDataCalendarWeek.getDouble(x.saturdayCapacity);
                startTime = techDataCalendarWeek.getTime(x.saturdayStartTime);
                break;
            case Calendar.SUNDAY:
                capacity = techDataCalendarWeek.getDouble(x.sundayCapacity);
                startTime = techDataCalendarWeek.getTime(x.sundayStartTime);
                break;
            }

            // Check if exception capacity should override
            Double exceptionCapacity = getExceptionCapacityForDate(techDataCalendar, currentDate);
            if (exceptionCapacity != null) {
                capacity = exceptionCapacity;
            }

            if (capacity != null && capacity > 0) {
                break;
            }

            moveDay += 1;
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        result.put(x.capacity, capacity);
        result.put(x.startTime, startTime);
        result.put(x.moveDay, moveDay);
        return result;
    }
    /** Used to to request the remain capacity available for dateFrom in a TechDataCalenda,
     * If the dateFrom (param in) is not  in an available TechDataCalendar period, the return value is zero.
     * @param techDataCalendar        The TechDataCalendar cover
     * @param dateFrom                        the date
     * @return  long capacityRemaining
     */
    public static long capacityRemaining(GenericValue techDataCalendar, Timestamp dateFrom) {
        GenericValue techDataCalendarWeek = null;
        // TODO read TechDataCalendarExcWeek to manage execption week (maybe it's needed to refactor the entity definition
        try {
            techDataCalendarWeek = techDataCalendar.getRelatedOne(x.TechDataCalendarWeek, true);
        } catch (GenericEntityException e) {
            Debug.logError(x.Pb_reading_Calendar_Week_associated_with_calendar + e.getMessage(), MODULE);
            return 0;
        }
        // TODO read TechDataCalendarExcDay to manage execption day
        Calendar cDateTrav = Calendar.getInstance();
        cDateTrav.setTime(dateFrom);
        Map<String, Object> position = dayStartCapacityAvailable(techDataCalendar, techDataCalendarWeek, new Timestamp(cDateTrav.getTimeInMillis()));
        int moveDay = (Integer) position.get(x.moveDay);
        if (moveDay != 0) return 0;
        Time startTime = (Time) position.get(x.startTime);
        Double capacity = (Double) position.get(x.capacity);
        Timestamp startAvailablePeriod = new Timestamp(UtilDateTime.getDayStart(dateFrom).getTime() + startTime.getTime()
                + cDateTrav.get(Calendar.ZONE_OFFSET) + cDateTrav.get(Calendar.DST_OFFSET));
        if (dateFrom.before(startAvailablePeriod)) return 0;
        Timestamp endAvailablePeriod = new Timestamp(startAvailablePeriod.getTime() + capacity.longValue());
        if (dateFrom.after(endAvailablePeriod)) return 0;
        return endAvailablePeriod.getTime() - dateFrom.getTime();
    }
    /** Used to move in a TechDataCalenda, produce the Timestamp for the begining of the next day available and its associated capacity.
     * If the dateFrom (param in) is not  in an available TechDataCalendar period, the return value is the next day available
     * @param techDataCalendar        The TechDataCalendar cover
     * @param dateFrom                        the date
     * @return a map with Timestamp dateTo, Double nextCapacity
     */
    public static Map<String, Object> startNextDay(GenericValue techDataCalendar, Timestamp dateFrom) {
        Map<String, Object> result = new HashMap<>();
        Timestamp dateTo = null;
        GenericValue techDataCalendarWeek = null;
        // TODO read TechDataCalendarExcWeek to manage execption week (maybe it's needed to refactor the entity definition
        try {
            techDataCalendarWeek = techDataCalendar.getRelatedOne(x.TechDataCalendarWeek, true);
        } catch (GenericEntityException e) {
            Debug.logError(x.Pb_reading_Calendar_Week_associated_with_calendar + e.getMessage(), MODULE);
            return ServiceUtil.returnError(x.Pb_reading_Calendar_Week_associated_with_calendar);
        }
        // TODO read TechDataCalendarExcDay to manage execption day
        Calendar cDateTrav = Calendar.getInstance();
        cDateTrav.setTime(dateFrom);
        Map<String, Object> position = dayStartCapacityAvailable(techDataCalendar, techDataCalendarWeek, new Timestamp(cDateTrav.getTimeInMillis()));
        Time startTime = (Time) position.get(x.startTime);
        int moveDay = (Integer) position.get(x.moveDay);
        dateTo = (moveDay == 0) ? dateFrom : UtilDateTime.getDayStart(dateFrom, moveDay);
        Timestamp startAvailablePeriod = new Timestamp(UtilDateTime.getDayStart(dateTo).getTime() + startTime.getTime()
                + cDateTrav.get(Calendar.ZONE_OFFSET) + cDateTrav.get(Calendar.DST_OFFSET));
        if (dateTo.before(startAvailablePeriod)) {
            dateTo = startAvailablePeriod;
        } else {
            dateTo = UtilDateTime.getNextDayStart(dateTo);
            cDateTrav.setTime(dateTo);
            position = dayStartCapacityAvailable(techDataCalendar, techDataCalendarWeek, new Timestamp(cDateTrav.getTimeInMillis()));
            startTime = (Time) position.get(x.startTime);
            moveDay = (Integer) position.get(x.moveDay);
            if (moveDay != 0) dateTo = UtilDateTime.getDayStart(dateTo, moveDay);
            dateTo.setTime(dateTo.getTime() + startTime.getTime() + cDateTrav.get(Calendar.ZONE_OFFSET) + cDateTrav.get(Calendar.DST_OFFSET));
        }
        result.put(x.dateTo, dateTo);
        result.put(x.nextCapacity, position.get(x.capacity));
        return result;
    }
    /** Used to move forward in a TechDataCalenda, start from the dateFrom and move forward only on available period.
     * If the dateFrom (param in) is not  a available TechDataCalendar period, the startDate is the begining of the next  day available
     * @param techDataCalendar        The TechDataCalendar cover
     * @param dateFrom                        the start date
     * @param amount                           the amount of millisecond to move forward
     * @return the dateTo
     */
    public static Timestamp addForward(GenericValue techDataCalendar, Timestamp dateFrom, long amount) {
        Timestamp dateTo = (Timestamp) dateFrom.clone();
        long nextCapacity = capacityRemaining(techDataCalendar, dateFrom);
        if (amount <= nextCapacity) {
            dateTo.setTime(dateTo.getTime() + amount);
            amount = 0;
        } else amount -= nextCapacity;

        Map<String, Object> result = new HashMap<>();
        while (amount > 0) {
            result = startNextDay(techDataCalendar, dateTo);
            dateTo = (Timestamp) result.get(x.dateTo);
            nextCapacity = ((Double) result.get(x.nextCapacity)).longValue();
            if (amount <= nextCapacity) {
                dateTo.setTime(dateTo.getTime() + amount);
                amount = 0;
            } else amount -= nextCapacity;
        }
        return dateTo;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /** Used to find the last day in the TechDataCalendarWeek where capacity != 0, ending at dayEnd, dayEnd included.
     * @param techDataCalendarWeek        The TechDataCalendarWeek cover
     * @param dayEnd
     * @return a map with the  capacity (Double) available, the startTime and  moveDay (int): the number of day it's
     * necessary to move to have capacity available
     */
    public static Map<String, Object> dayEndCapacityAvailable(GenericValue techDataCalendarWeek, int dayEnd) {
        Map<String, Object> result = new HashMap<>();
        int moveDay = 0;
        Double capacity = null;
        Time startTime = null;
        while (capacity == null || capacity == 0) {
            switch (dayEnd) {
            case Calendar.MONDAY:
                capacity = techDataCalendarWeek.getDouble(x.mondayCapacity);
                startTime = techDataCalendarWeek.getTime(x.mondayStartTime);
                break;
            case Calendar.TUESDAY:
                capacity = techDataCalendarWeek.getDouble(x.tuesdayCapacity);
                startTime = techDataCalendarWeek.getTime(x.tuesdayStartTime);
                break;
            case Calendar.WEDNESDAY:
                capacity = techDataCalendarWeek.getDouble(x.wednesdayCapacity);
                startTime = techDataCalendarWeek.getTime(x.wednesdayStartTime);
                break;
            case Calendar.THURSDAY:
                capacity = techDataCalendarWeek.getDouble(x.thursdayCapacity);
                startTime = techDataCalendarWeek.getTime(x.thursdayStartTime);
                break;
            case Calendar.FRIDAY:
                capacity = techDataCalendarWeek.getDouble(x.fridayCapacity);
                startTime = techDataCalendarWeek.getTime(x.fridayStartTime);
                break;
            case Calendar.SATURDAY:
                capacity = techDataCalendarWeek.getDouble(x.saturdayCapacity);
                startTime = techDataCalendarWeek.getTime(x.saturdayStartTime);
                break;
            case Calendar.SUNDAY:
                capacity = techDataCalendarWeek.getDouble(x.sundayCapacity);
                startTime = techDataCalendarWeek.getTime(x.sundayStartTime);
                break;
            }
            if (capacity == null || capacity == 0) {
                moveDay -= 1;
                dayEnd = (dayEnd == 1) ? 7 : dayEnd - 1;
            }
        }
        result.put(x.capacity, capacity);
        result.put(x.startTime, startTime);
        result.put(x.moveDay, moveDay);
        return result;
    }
    /** Used to request the remaining capacity available for dateFrom in a TechDataCalenda,
     * If the dateFrom (param in) is not  in an available TechDataCalendar period, the return value is zero.
     * @param techDataCalendar        The TechDataCalendar cover
     * @param dateFrom                        the date
     * @return  long capacityRemaining
     */
    public static long capacityRemainingBackward(GenericValue techDataCalendar, Timestamp dateFrom) {
        GenericValue techDataCalendarWeek = null;
        // TODO read TechDataCalendarExcWeek to manage exception week (maybe it's needed to refactor the entity definition
        try {
            techDataCalendarWeek = techDataCalendar.getRelatedOne(x.TechDataCalendarWeek, true);
        } catch (GenericEntityException e) {
            Debug.logError(x.Pb_reading_Calendar_Week_associated_with_calendar + e.getMessage(), MODULE);
            return 0;
        }
        // TODO read TechDataCalendarExcDay to manage execption day
        Calendar cDateTrav = Calendar.getInstance();
        cDateTrav.setTime(dateFrom);
        Map<String, Object> position = dayEndCapacityAvailable(techDataCalendarWeek, cDateTrav.get(Calendar.DAY_OF_WEEK));
        int moveDay = (Integer) position.get(x.moveDay);
        if (moveDay != 0) return 0;
        Time startTime = (Time) position.get(x.startTime);
        Double capacity = (Double) position.get(x.capacity);
        Timestamp startAvailablePeriod = new Timestamp(UtilDateTime.getDayStart(dateFrom).getTime() + startTime.getTime()
                + cDateTrav.get(Calendar.ZONE_OFFSET) + cDateTrav.get(Calendar.DST_OFFSET));
        if (dateFrom.before(startAvailablePeriod)) return 0;
        Timestamp endAvailablePeriod = new Timestamp(startAvailablePeriod.getTime() + capacity.longValue());
        if (dateFrom.after(endAvailablePeriod)) return 0;
        return dateFrom.getTime() - startAvailablePeriod.getTime();
    }
    /** Used to move in a TechDataCalenda, produce the Timestamp for the end of the previous day available and its associated capacity.
     * If the dateFrom (param in) is not  in an available TechDataCalendar period, the return value is the previous day available
     * @param techDataCalendar        The TechDataCalendar cover
     * @param dateFrom                        the date
     * @return a map with Timestamp dateTo, Double previousCapacity
     */
    public static Map<String, Object> endPreviousDay(GenericValue techDataCalendar, Timestamp dateFrom) {
        Map<String, Object> result = new HashMap<>();
        Timestamp dateTo = null;
        GenericValue techDataCalendarWeek = null;
        // TODO read TechDataCalendarExcWeek to manage exception week (maybe it's needed to refactor the entity definition
        try {
            techDataCalendarWeek = techDataCalendar.getRelatedOne(x.TechDataCalendarWeek, true);
        } catch (GenericEntityException e) {
            Debug.logError(x.Pb_reading_Calendar_Week_associated_with_calendar + e.getMessage(), MODULE);
            return ServiceUtil.returnError(x.Pb_reading_Calendar_Week_associated_with_calendar);
        }
        // TODO read TechDataCalendarExcDay to manage execption day
        Calendar cDateTrav = Calendar.getInstance();
        cDateTrav.setTime(dateFrom);
        Map<String, Object> position = dayEndCapacityAvailable(techDataCalendarWeek, cDateTrav.get(Calendar.DAY_OF_WEEK));
        Time startTime = (Time) position.get(x.startTime);
        int moveDay = (Integer) position.get(x.moveDay);
        Double capacity = (Double) position.get(x.capacity);
        dateTo = (moveDay == 0) ? dateFrom : UtilDateTime.getDayEnd(dateFrom, (long) moveDay);
        Timestamp endAvailablePeriod = new Timestamp(UtilDateTime.getDayStart(dateTo).getTime() + startTime.getTime() + capacity.longValue()
                + cDateTrav.get(Calendar.ZONE_OFFSET) + cDateTrav.get(Calendar.DST_OFFSET));
        if (dateTo.after(endAvailablePeriod)) {
            dateTo = endAvailablePeriod;
        } else {
            dateTo = UtilDateTime.getDayStart(dateTo, -1);
            cDateTrav.setTime(dateTo);
            position = dayEndCapacityAvailable(techDataCalendarWeek, cDateTrav.get(Calendar.DAY_OF_WEEK));
            startTime = (Time) position.get(x.startTime);
            moveDay = (Integer) position.get(x.moveDay);
            capacity = (Double) position.get(x.capacity);
            if (moveDay != 0) dateTo = UtilDateTime.getDayStart(dateTo, moveDay);
            dateTo.setTime(dateTo.getTime() + startTime.getTime() + capacity.longValue() + cDateTrav.get(Calendar.ZONE_OFFSET)
                    + cDateTrav.get(Calendar.DST_OFFSET));
        }
        result.put(x.dateTo, dateTo);
        result.put(x.previousCapacity, position.get(x.capacity));
        return result;
    }
    /** Used to move backward in a TechDataCalendar, start from the dateFrom and move backward only on available period.
     * If the dateFrom (param in) is not  a available TechDataCalendar period, the startDate is the end of the previous day available
     * @param techDataCalendar        The TechDataCalendar cover
     * @param dateFrom                        the start date
     * @param amount                           the amount of millisecond to move backward
     * @return the dateTo
     */
    public static Timestamp addBackward(GenericValue techDataCalendar, Timestamp dateFrom, long amount) {
        Timestamp dateTo = (Timestamp) dateFrom.clone();
        long previousCapacity = capacityRemainingBackward(techDataCalendar, dateFrom);
        if (amount <= previousCapacity) {
            dateTo.setTime(dateTo.getTime() - amount);
            amount = 0;
        } else amount -= previousCapacity;

        Map<String, Object> result = new HashMap<>();
        while (amount > 0) {
            result = endPreviousDay(techDataCalendar, dateTo);
            dateTo = (Timestamp) result.get(x.dateTo);
            previousCapacity = ((Double) result.get(x.previousCapacity)).longValue();
            if (amount <= previousCapacity) {
                dateTo.setTime(dateTo.getTime() - amount);
                amount = 0;
            } else amount -= previousCapacity;
        }
        return dateTo;
    }

    /**
     * Retrieves the exception day capacity for a given date, if it exists.
     * If no exception exists for the date, returns null.
     * If exception day is found, its defined exceptionCapacity is returned.
     *
     * @param techDataCalendar the calendar entity
     * @param date the timestamp to check (date portion only)
     * @return Double exception capacity or null if not defined for this date
     */
    private static Double getExceptionCapacityForDate(GenericValue techDataCalendar, Timestamp date) {
        try {
            List<GenericValue> excDays = techDataCalendar.getRelated(x.TechDataCalendarExcDay, null, null, true);
            if (excDays != null) {
                Calendar checkCal = Calendar.getInstance();
                checkCal.setTime(date);
                checkCal.set(Calendar.HOUR_OF_DAY, 0);
                checkCal.set(Calendar.MINUTE, 0);
                checkCal.set(Calendar.SECOND, 0);
                checkCal.set(Calendar.MILLISECOND, 0);
                long checkMillis = checkCal.getTimeInMillis();

                for (GenericValue excDay : excDays) {
                    Timestamp excDate = excDay.getTimestamp(x.exceptionDateStartTime);
                    if (excDate != null) {
                        Calendar excCal = Calendar.getInstance();
                        excCal.setTime(excDate);
                        excCal.set(Calendar.HOUR_OF_DAY, 0);
                        excCal.set(Calendar.MINUTE, 0);
                        excCal.set(Calendar.SECOND, 0);
                        excCal.set(Calendar.MILLISECOND, 0);
                        long excMillis = excCal.getTimeInMillis();

                        if (excMillis == checkMillis) {
                            return excDay.getDouble(x.exceptionCapacity);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(x.Error_reading_exception_days + e.getMessage(), MODULE);
        }
        return null;
    }
}
