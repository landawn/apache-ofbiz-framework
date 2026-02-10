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
package org.apache.ofbiz.manufacturing.jobshopmgt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.manufacturing.bom.BOMNode;
import org.apache.ofbiz.manufacturing.bom.BOMTree;
import org.apache.ofbiz.manufacturing.techdata.TechDataServices;
import org.apache.ofbiz.persistence.dao.CostComponentDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.InventoryItemDao;
import org.apache.ofbiz.persistence.dao.InventoryItemDetailDao;
import org.apache.ofbiz.persistence.dao.LotDao;
import org.apache.ofbiz.persistence.dao.OrderDeliveryScheduleDao;
import org.apache.ofbiz.persistence.dao.OrderHeaderDao;
import org.apache.ofbiz.persistence.dao.OrderItemDao;
import org.apache.ofbiz.persistence.dao.OrderItemShipGroupAssocDao;
import org.apache.ofbiz.persistence.dao.OrderItemShipGroupDao;
import org.apache.ofbiz.persistence.dao.OrderItemShipGrpInvResDao;
import org.apache.ofbiz.persistence.dao.ProductAssocDao;
import org.apache.ofbiz.persistence.dao.ProductCostComponentCalcDao;
import org.apache.ofbiz.persistence.dao.ProductDao;
import org.apache.ofbiz.persistence.dao.ProductFacilityDao;
import org.apache.ofbiz.persistence.dao.RequirementDao;
import org.apache.ofbiz.persistence.dao.WorkEffortAssocDao;
import org.apache.ofbiz.persistence.dao.WorkEffortCostCalcDao;
import org.apache.ofbiz.persistence.dao.WorkEffortDao;
import org.apache.ofbiz.persistence.dao.WorkEffortGoodStandardDao;
import org.apache.ofbiz.persistence.dao.WorkEffortInventoryAssignDao;
import org.apache.ofbiz.persistence.dao.WorkEffortPartyAssignmentDao;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.product.config.ProductConfigWrapper.ConfigOption;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ProductionRunServicesContext;
/**
 * Services for Production Run maintenance
 */
public class ProductionRunServices {

    private static final String MODULE = ProductionRunServices.class.getName();
    private static final String RESOURCE = x.ManufacturingUiLabels;
    private static final String RES_ORDER = x.OrderErrorUiLabels;
    private static final String RES_PRODUCT = x.ProductUiLabels;

    private static final int DECIMALS = UtilNumber.getBigDecimalScale(x.order_decimals);
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode(x.finaccount_rounding);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(DECIMALS, ROUNDING);

    /**
     * Cancels a ProductionRun.
     * @param ctx     The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> cancelProductionRun(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Map<String, Object> serviceResult = new HashMap<>();
        String productionRunId = (String) context.get(x.productionRunId);

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotExists, locale));
        }
        String currentStatusId = productionRun.getGenericValue().getString(x.currentStatusId);

        // PRUN_CREATED, PRUN_DOC_PRINTED --> PRUN_CANCELLED
        if (x.PRUN_CREATED.equals(currentStatusId) || x.PRUN_DOC_PRINTED.equals(currentStatusId) || x.PRUN_SCHEDULED.equals(currentStatusId)) {
            try {
                // First of all, make sure that there aren't production runs that depend on this one.
                List<ProductionRun> mandatoryWorkEfforts = new LinkedList<>();
                ProductionRunHelper.getLinkedProductionRuns(delegator, dispatcher, productionRunId, mandatoryWorkEfforts);
                for (int i = 1; i < mandatoryWorkEfforts.size(); i++) {
                    GenericValue mandatoryWorkEffort = (mandatoryWorkEfforts.get(i)).getGenericValue();
                    if (!(x.PRUN_CANCELLED.equals(mandatoryWorkEffort.getString(x.currentStatusId)))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                x.ManufacturingProductionRunStatusNotChangedMandatoryProductionRunFound, locale));
                    }
                }
                Map<String, Object> serviceContext = new HashMap<>();
                // change the production run (header) status to PRUN_CANCELLED
                serviceContext.put(x.workEffortId, productionRunId);
                serviceContext.put(x.currentStatusId, x.PRUN_CANCELLED);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                // Cancel the product promised
                List<GenericValue> products = DaoRegistry.getDao(delegator, x.WorkEffortGoodStandard, WorkEffortGoodStandardDao.class).findByAnd(
                        delegator, x.WorkEffortGoodStandard,
                        UtilMisc.toMap(x.workEffortId, productionRunId, x.workEffortGoodStdTypeId, x.PRUN_PROD_DELIV, x.statusId,
                                x.WEGS_CREATED),
                        null, false);
                if (UtilValidate.isNotEmpty(products)) {
                    for (GenericValue product : products) {
                        product.set(x.statusId, x.WEGS_CANCELLED);
                        product.store();
                    }
                }

                // change the tasks status to PRUN_CANCELLED
                List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
                String taskId = null;
                for (GenericValue oneTask : tasks) {
                    taskId = oneTask.getString(x.workEffortId);
                    serviceContext.clear();
                    serviceContext.put(x.workEffortId, taskId);
                    serviceContext.put(x.currentStatusId, x.PRUN_CANCELLED);
                    serviceContext.put(x.userLogin, userLogin);
                    serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    // cancel all the components
                    List<GenericValue> components = DaoRegistry.getDao(delegator, x.WorkEffortGoodStandard, WorkEffortGoodStandardDao.class)
                            .findByAnd(delegator, x.WorkEffortGoodStandard,
                                    UtilMisc.toMap(x.workEffortId, taskId, x.workEffortGoodStdTypeId, x.PRUNT_PROD_NEEDED, x.statusId,
                                            x.WEGS_CREATED),
                                    null, false);
                    if (UtilValidate.isNotEmpty(components)) {
                        for (GenericValue component : components) {
                            component.set(x.statusId, x.WEGS_CANCELLED);
                            component.store();
                        }
                    }
                }
            } catch (GenericEntityException | GenericServiceException e) {
                Debug.logError(e, x.Problem_accessing_WorkEffortGoodStandard_entity, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
            }
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusChanged, UtilMisc.toMap(
                    x.newStatusId, x.PRUN_DOC_PRINTED), locale));
            return result;
        }
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunCannotBeCancelled, locale));
    }

    /**
     * Creates a Production Run.
     * <ul>
     * <li> check if routing - product link exist</li>
     * <li> check if product have a Bill Of Material</li>
     * <li> check if routing have routingTask</li>
     * <li> create the workEffort for ProductionRun</li>
     * <li> create the WorkEffortGoodStandard for link between ProductionRun and the product it will produce</li>
     * <li> for each valid routingTask of the routing create a workeffort-task</li>
     * <li> for the first routingTask, create for all the valid productIdTo with no associateRoutingTask  a WorkEffortGoodStandard</li>
     * <li> for each valid routingTask of the routing and valid productIdTo associate with this RoutingTask create a WorkEffortGoodStandard</li>
     * </ul>
     * @param ctx     The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters, productId, routingId, pRQuantity, startDate, workEffortName, description
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> createProductionRun(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // TODO: security management  and finishing cleaning (ex copy from PartyServices.java)
        // Mandatory input fields
        String productId = (String) context.get(x.productId);
        Timestamp startDate = (Timestamp) context.get(x.startDate);
        BigDecimal pRQuantity = (BigDecimal) context.get(x.pRQuantity);
        String facilityId = (String) context.get(x.facilityId);
        // Optional input fields
        String workEffortId = (String) context.get(x.routingId);
        String workEffortName = (String) context.get(x.workEffortName);
        String description = (String) context.get(x.description);

        GenericValue routing = null;
        GenericValue product = null;
        List<GenericValue> routingTaskAssocs = null;
        try {
            // Find the product
            product = DaoRegistry.getDao(delegator, x.Product, ProductDao.class).findOne(delegator, x.Product, UtilMisc.toMap(x.productId,
                    productId), false);
            if (product == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductNotExist, locale));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // -------------------
        // Routing and routing tasks
        // -------------------
        // Select the product's routing
        try {
            Map<String, Object> routingInMap = UtilMisc.toMap(x.productId, productId, x.applicableDate, startDate, x.userLogin, userLogin);
            if (workEffortId != null) {
                routingInMap.put(x.workEffortId, workEffortId);
            }
            Map<String, Object> routingOutMap = dispatcher.runSync(x.getProductRouting, routingInMap);
            if (ServiceUtil.isError(routingOutMap)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(routingOutMap));
            }
            routing = (GenericValue) routingOutMap.get(x.routing);
            routingTaskAssocs = UtilGenerics.cast(routingOutMap.get(x.tasks));
        } catch (GenericServiceException gse) {
            Debug.logWarning(gse.getMessage(), MODULE);
        }
        if (routing == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductRoutingNotExist, locale));
        }
        if (UtilValidate.isEmpty(routingTaskAssocs)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingRoutingHasNoRoutingTask, locale));
        }

        // -------------------
        // Components
        // -------------------
        // The components are retrieved using the getManufacturingComponents service
        // (that performs a bom breakdown and if needed runs the configurator).
        List<BOMNode> components = null;
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.put(x.productId, productId); // the product that we want to manufacture
        serviceContext.put(x.quantity, pRQuantity); // the quantity that we want to manufacture
        serviceContext.put(x.userLogin, userLogin);
        Map<String, Object> serviceResult = null;
        try {
            serviceResult = dispatcher.runSync(x.getManufacturingComponents, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            components = UtilGenerics.cast(serviceResult.get(x.components)); // a list of objects representing the product's components
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_getManufacturingComponents_service, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // ProductionRun header creation,
        if (workEffortName == null) {
            String prdName = UtilValidate.isNotEmpty(product.getString(x.productName)) ? product.getString(x.productName) : product.getString(
                    x.productId);
            String wefName = UtilValidate.isNotEmpty(routing.getString(x.workEffortName)) ? routing.getString(x.workEffortName)
                    : routing.getString(x.workEffortId);
            workEffortName = prdName + x.str_3bc15c8a + wefName;
        }

        serviceContext.clear();
        serviceContext.put(x.workEffortTypeId, x.PROD_ORDER_HEADER);
        serviceContext.put(x.workEffortPurposeTypeId, x.WEPT_PRODUCTION_RUN);
        serviceContext.put(x.currentStatusId, x.PRUN_CREATED);
        serviceContext.put(x.workEffortName, workEffortName);
        serviceContext.put(x.description, description);
        serviceContext.put(x.facilityId, facilityId);
        serviceContext.put(x.estimatedStartDate, startDate);
        serviceContext.put(x.quantityToProduce, pRQuantity);
        serviceContext.put(x.userLogin, userLogin);
        try {
            serviceResult = dispatcher.runSync(x.createWorkEffort, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_createWorkEffort_service, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        String productionRunId = (String) serviceResult.get(x.workEffortId);
        if (Debug.infoOn()) {
            Debug.logInfo(x.ProductionRun_created + productionRunId, MODULE);
        }

        // ProductionRun, product will be produce creation = WorkEffortGoodStandard for the productId
        serviceContext.clear();
        serviceContext.put(x.workEffortId, productionRunId);
        serviceContext.put(x.productId, productId);
        serviceContext.put(x.workEffortGoodStdTypeId, x.PRUN_PROD_DELIV);
        serviceContext.put(x.statusId, x.WEGS_CREATED);
        serviceContext.put(x.estimatedQuantity, pRQuantity);
        serviceContext.put(x.fromDate, startDate);
        serviceContext.put(x.userLogin, userLogin);
        try {
            serviceResult = dispatcher.runSync(x.createWorkEffortGoodStandard, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_createWorkEffortGoodStandard_service, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // Multi creation (like clone) ProductionRunTask and GoodAssoc
        boolean first = true;
        for (GenericValue routingTaskAssoc : routingTaskAssocs) {
            if (EntityUtil.isValueActive(routingTaskAssoc, startDate)) {
                GenericValue routingTask = null;
                try {
                    routingTask = routingTaskAssoc.getRelatedOne(x.ToWorkEffort, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e.getMessage(), MODULE);
                }
                // Calculate the estimatedCompletionDate
                long totalTime = ProductionRun.getEstimatedTaskTime(routingTask, pRQuantity, dispatcher);
                Timestamp endDate = TechDataServices.addForward(TechDataServices.getTechDataCalendar(routingTask), startDate, totalTime);

                serviceContext.clear();
                serviceContext.put(x.priority, routingTaskAssoc.get(x.sequenceNum));
                serviceContext.put(x.workEffortPurposeTypeId, x.WEPT_PRODUCTION_RUN);
                serviceContext.put(x.workEffortName, routingTask.get(x.workEffortName));
                serviceContext.put(x.description, routingTask.get(x.description));
                serviceContext.put(x.fixedAssetId, routingTask.get(x.fixedAssetId));
                serviceContext.put(x.workEffortTypeId, x.PROD_ORDER_TASK);
                serviceContext.put(x.currentStatusId, x.PRUN_CREATED);
                serviceContext.put(x.workEffortParentId, productionRunId);
                serviceContext.put(x.facilityId, facilityId);
                serviceContext.put(x.reservPersons, routingTask.get(x.reservPersons));
                serviceContext.put(x.estimatedStartDate, startDate);
                serviceContext.put(x.estimatedCompletionDate, endDate);
                serviceContext.put(x.estimatedSetupMillis, routingTask.get(x.estimatedSetupMillis));
                serviceContext.put(x.estimatedMilliSeconds, routingTask.get(x.estimatedMilliSeconds));
                serviceContext.put(x.quantityToProduce, pRQuantity);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = null;
                try {
                    serviceResult = dispatcher.runSync(x.createWorkEffort, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_calling_the_createWorkEffort_service, MODULE);
                }
                String productionRunTaskId = (String) serviceResult.get(x.workEffortId);
                if (Debug.infoOn()) {
                    Debug.logInfo(x.ProductionRunTaskId_created + productionRunTaskId, MODULE);
                }

                // The newly created production run task is associated to the routing task
                // to keep track of the template used to generate it.
                serviceContext.clear();
                serviceContext.put(x.userLogin, userLogin);
                serviceContext.put(x.workEffortIdFrom, routingTask.getString(x.workEffortId));
                serviceContext.put(x.workEffortIdTo, productionRunTaskId);
                serviceContext.put(x.workEffortAssocTypeId, x.WORK_EFF_TEMPLATE);
                try {
                    serviceResult = dispatcher.runSync(x.createWorkEffortAssoc, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_calling_the_createWorkEffortAssoc_service, MODULE);
                }
                // clone associated objects from the routing task to the run task
                String routingTaskId = routingTaskAssoc.getString(x.workEffortIdTo);
                try {
                    cloneWorkEffortPartyAssignments(ctx, userLogin, routingTaskId, productionRunTaskId);
                    cloneWorkEffortCostCalcs(ctx, userLogin, routingTaskId, productionRunTaskId);
                } catch (GeneralException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
                // Now we iterate thru the components returned by the getManufacturingComponents service
                // TODO: if in the BOM a routingWorkEffortId is specified, but the task is not in the routing
                //       the component is not added to the production run.
                for (BOMNode node : components) {
                    // The components variable contains a list of BOMNodes:
                    // each node represents a product (component).
                    GenericValue productBom = node.getProductAssoc();
                    if ((productBom.getString(x.routingWorkEffortId) == null && first) || (productBom.getString(x.routingWorkEffortId) != null
                            && productBom.getString(x.routingWorkEffortId).equals(routingTask.getString(x.workEffortId)))) {
                        serviceContext.clear();
                        serviceContext.put(x.workEffortId, productionRunTaskId);
                        // Here we get the ProductAssoc record from the BOMNode
                        // object to be sure to use the
                        // right component (possibly configured).
                        serviceContext.put(x.productId, node.getProduct().get(x.productId));
                        serviceContext.put(x.workEffortGoodStdTypeId, x.PRUNT_PROD_NEEDED);
                        serviceContext.put(x.statusId, x.WEGS_CREATED);
                        serviceContext.put(x.fromDate, productBom.get(x.fromDate));
                        // Here we use the getQuantity method to get the quantity already
                        // computed by the getManufacturingComponents service
                        serviceContext.put(x.estimatedQuantity, node.getQuantity());
                        serviceContext.put(x.userLogin, userLogin);
                        serviceResult = null;
                        try {
                            serviceResult = dispatcher.runSync(x.createWorkEffortGoodStandard, serviceContext);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, x.Problem_calling_the_createWorkEffortGoodStandard_service, MODULE);
                        }
                        if (Debug.infoOn()) {
                            Debug.logInfo(x.ProductLink_created_for_productId + productBom.getString(x.productIdTo), MODULE);
                        }
                    }
                }
                first = false;
                startDate = endDate;
            }
        }

        // update the estimatedCompletionDate field for the productionRun
        serviceContext.clear();
        serviceContext.put(x.workEffortId, productionRunId);
        serviceContext.put(x.estimatedCompletionDate, startDate);
        serviceContext.put(x.userLogin, userLogin);
        serviceResult = null;
        try {
            serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
        }
        result.put(x.productionRunId, productionRunId);
        result.put(x.estimatedCompletionDate, startDate);
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunCreated, UtilMisc.toMap(
                x.productionRunId, productionRunId), locale));
        return result;
    }

    /**
     * Make a copy of the party assignments that were defined on the template routing task to the new production run task.
     */
    private static void cloneWorkEffortPartyAssignments(DispatchContext dctx, GenericValue userLogin,
                                                        String routingTaskId, String productionRunTaskId) throws GeneralException {
        List<GenericValue> workEffortPartyAssignments = null;
        try {
            workEffortPartyAssignments = EntityUtil.filterByDate(
                    dctx.getDelegator().findByAnd(x.WorkEffortPartyAssignment, UtilMisc.toMap(x.workEffortId, routingTaskId), null, false));
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
        }

        if (workEffortPartyAssignments != null) {
            for (GenericValue workEffortPartyAssignment : workEffortPartyAssignments) {
                Map<String, Object> partyToWorkEffort = UtilMisc.<String, Object>toMap(
                        x.workEffortId, productionRunTaskId,
                        x.partyId, workEffortPartyAssignment.getString(x.partyId),
                        x.roleTypeId, workEffortPartyAssignment.getString(x.roleTypeId),
                        x.fromDate, workEffortPartyAssignment.getTimestamp(x.fromDate),
                        x.statusId, workEffortPartyAssignment.getString(x.statusId),
                        x.userLogin, userLogin);
                try {
                    Map<String, Object> result = dctx.getDispatcher().runSync(x.assignPartyToWorkEffort, partyToWorkEffort);
                    if (ServiceUtil.isError(result)) {
                        String errorMessage = ServiceUtil.getErrorMessage(result);
                        Debug.logError(errorMessage, MODULE);
                        throw new GeneralException(errorMessage);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_calling_the_assignPartyToWorkEffort_service, MODULE);
                }
                if (Debug.infoOn()) {
                    Debug.logInfo(x.ProductionRunPartyassigment_for_party + workEffortPartyAssignment.get(x.partyId) + x.created_c79bef7c, MODULE);
                }
            }
        }
    }

    /**
     * Make a copy of the cost calc entities that were defined on the template routing task to the new production run task.
     */
    private static void cloneWorkEffortCostCalcs(DispatchContext dctx, GenericValue userLogin, String routingTaskId, String productionRunTaskId)
            throws GeneralException {
        List<GenericValue> workEffortCostCalcs = null;
        try {
            workEffortCostCalcs = EntityUtil.filterByDate(
                    dctx.getDelegator().findByAnd(x.WorkEffortCostCalc, UtilMisc.toMap(x.workEffortId, routingTaskId), null, false));
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
        }

        if (workEffortCostCalcs != null) {
            for (GenericValue costCalc : workEffortCostCalcs) {
                Map<String, Object> createCostCalc = UtilMisc.toMap(
                        x.workEffortId, productionRunTaskId,
                        x.costComponentTypeId, costCalc.getString(x.costComponentTypeId),
                        x.costComponentCalcId, costCalc.getString(x.costComponentCalcId),
                        x.fromDate, costCalc.get(x.fromDate),
                        x.thruDate, costCalc.get(x.thruDate),
                        x.userLogin, userLogin);

                try {
                    Map<String, Object> result = dctx.getDispatcher().runSync(x.createWorkEffortCostCalc, createCostCalc);
                    if (ServiceUtil.isError(result)) {
                        String errorMessage = ServiceUtil.getErrorMessage(result);
                        Debug.logError(errorMessage, MODULE);
                        throw new GeneralException(errorMessage);
                    }
                } catch (GenericServiceException gse) {
                    Debug.logError(gse, x.Problem_calling_the_createWorkEffortCostCalc_service, MODULE);
                }
                if (Debug.infoOn()) {
                    Debug.logInfo(x.ProductionRun_CostCalc_for_cost_calc + costCalc.getString(x.costComponentCalcId) + x.created_c79bef7c, MODULE);
                }
            }
        }
    }

    /**
     * Update a Production Run.
     * <ul>
     * <li> update field and after recalculate the entire ProductionRun data (routingTask and productComponent)</li>
     * <li> create the WorkEffortGoodStandard for link between ProductionRun and the product it will produce</li>
     * <li> for each valid routingTask of the routing create a workeffort-task</li>
     * <li> for the first routingTask, create for all the valid productIdTo with no associateRoutingTask  a WorkEffortGoodStandard</li>
     * <li> for each valid routingTask of the routing and valid productIdTo associate with this RoutingTask create a WorkEffortGoodStandard</li>
     * </ul>
     * @param ctx     The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters, productId, routingId, quantity, estimatedStartDate, workEffortName, description
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> updateProductionRun(DispatchContext ctx, ProductionRunServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productionRunId = (String) context.get(x.productionRunId);

        if (UtilValidate.isNotEmpty(productionRunId)) {
            ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
            if (productionRun.exist()) {

                if (!x.PRUN_CREATED.equals(productionRun.getGenericValue().getString(x.currentStatusId))
                        && !x.PRUN_SCHEDULED.equals(productionRun.getGenericValue().getString(x.currentStatusId))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunPrinted, locale));
                }

                BigDecimal quantity = (BigDecimal) context.get(x.quantity);
                if (quantity != null && quantity.compareTo(productionRun.getQuantity()) != 0) {
                    productionRun.setQuantity(quantity);
                }

                Timestamp estimatedStartDate = (Timestamp) context.get(x.estimatedStartDate);
                if (estimatedStartDate != null && !estimatedStartDate.equals(productionRun.getEstimatedStartDate())) {
                    productionRun.setEstimatedStartDate(estimatedStartDate);
                }

                String workEffortName = (String) context.get(x.workEffortName);
                if (workEffortName != null) {
                    productionRun.setProductionRunName(workEffortName);
                }

                String description = (String) context.get(x.description);
                if (description != null) {
                    productionRun.setDescription(description);
                }

                String facilityId = (String) context.get(x.facilityId);
                if (facilityId != null) {
                    productionRun.getGenericValue().set(x.facilityId, facilityId);
                }

                boolean updateEstimatedOrderDates = productionRun.isUpdateCompletionDate();
                if (productionRun.store()) {
                    if (updateEstimatedOrderDates && x.PRUN_SCHEDULED.equals(productionRun.getGenericValue().getString(x.currentStatusId))) {
                        try {
                            Map<String, Object> result = dispatcher.runSync(x.setEstimatedDeliveryDates,
                                    UtilMisc.toMap(x.userLogin, userLogin));
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, x.Problem_calling_the_setEstimatedDeliveryDates_service, MODULE);
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotUpdated, locale));
                        }
                    }
                    return ServiceUtil.returnSuccess();
                } else {
                    Debug.logError(x.productionRun_store_fail_for_productionRunId + productionRunId, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotUpdated, locale));
                }
            }
            Debug.logError(x.no_productionRun_for_productionRunId + productionRunId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotUpdated, locale));
        }
        Debug.logError(x.service_updateProductionRun_call_with_productionRunId_empty, MODULE);
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotUpdated, locale));
    }

    public static Map<String, Object> changeProductionRunStatus(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Map<String, Object> serviceResult = new HashMap<>();
        String productionRunId = (String) context.get(x.productionRunId);
        String statusId = (String) context.get(x.statusId);

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotExists, locale));
        }
        String currentStatusId = productionRun.getGenericValue().getString(x.currentStatusId);

        if (currentStatusId.equals(statusId)) {
            result.put(x.newStatusId, currentStatusId);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusChanged, UtilMisc.toMap(
                    x.newStatusId, currentStatusId), locale));
            return result;
        }

        // PRUN_CREATED --> PRUN_SCHEDULED
        if (x.PRUN_CREATED.equals(currentStatusId) && x.PRUN_SCHEDULED.equals(statusId)) {
            // change the production run status to PRUN_SCHEDULED
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put(x.workEffortId, productionRunId);
            serviceContext.put(x.currentStatusId, statusId);
            serviceContext.put(x.userLogin, userLogin);
            try {
                serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
            }
            // change the production run tasks status to PRUN_SCHEDULED
            for (GenericValue task : productionRun.getProductionRunRoutingTasks()) {
                serviceContext.clear();
                serviceContext.put(x.workEffortId, task.getString(x.workEffortId));
                serviceContext.put(x.currentStatusId, statusId);
                serviceContext.put(x.userLogin, userLogin);
                try {
                    serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            }
            result.put(x.newStatusId, statusId);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusChanged, UtilMisc.toMap(
                    x.newStatusId, x.PRUN_CLOSED), locale));
            return result;
        }

        // PRUN_CREATED or PRUN_SCHEDULED --> PRUN_DOC_PRINTED
        if ((x.PRUN_CREATED.equals(currentStatusId) || x.PRUN_SCHEDULED.equals(currentStatusId)) && (statusId == null
                || x.PRUN_DOC_PRINTED.equals(statusId))) {
            // change only the production run (header) status to PRUN_DOC_PRINTED
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put(x.workEffortId, productionRunId);
            serviceContext.put(x.currentStatusId, x.PRUN_DOC_PRINTED);
            serviceContext.put(x.userLogin, userLogin);
            try {
                serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
            }
            // change the production run tasks status to PRUN_DOC_PRINTED
            for (GenericValue task : productionRun.getProductionRunRoutingTasks()) {
                serviceContext.clear();
                serviceContext.put(x.workEffortId, task.getString(x.workEffortId));
                serviceContext.put(x.currentStatusId, x.PRUN_DOC_PRINTED);
                serviceContext.put(x.userLogin, userLogin);
                try {
                    serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            }
            result.put(x.newStatusId, x.PRUN_DOC_PRINTED);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusChanged, UtilMisc.toMap(
                    x.newStatusId, x.PRUN_DOC_PRINTED), locale));
            return result;
        }

        // PRUN_DOC_PRINTED --> PRUN_RUNNING
        // this should be called only when the first task is started
        if (x.PRUN_DOC_PRINTED.equals(currentStatusId) && (statusId == null || x.PRUN_RUNNING.equals(statusId))) {
            // change only the production run (header) status to PRUN_RUNNING
            // First check if there are production runs with precedence not still completed
            try {
                List<GenericValue> mandatoryWorkEfforts = DaoRegistry.getDao(delegator, x.WorkEffortAssoc, WorkEffortAssocDao.class).findByAnd(
                        delegator, x.WorkEffortAssoc,
                        UtilMisc.toMap(x.workEffortIdTo, productionRunId, x.workEffortAssocTypeId, x.WORK_EFF_PRECEDENCY), null, false);
                mandatoryWorkEfforts = EntityUtil.filterByDate(mandatoryWorkEfforts);
                for (GenericValue mandatoryWorkEffortAssoc : mandatoryWorkEfforts) {
                    GenericValue mandatoryWorkEffort = mandatoryWorkEffortAssoc.getRelatedOne(x.FromWorkEffort, false);
                    if (!(x.PRUN_COMPLETED.equals(mandatoryWorkEffort.getString(x.currentStatusId))
                            || x.PRUN_RUNNING.equals(mandatoryWorkEffort.getString(x.currentStatusId))
                            || x.PRUN_CLOSED.equals(mandatoryWorkEffort.getString(x.currentStatusId)))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                x.ManufacturingProductionRunStatusNotChangedMandatoryProductionRunNotCompleted, locale));
                    }
                }
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
            }

            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put(x.workEffortId, productionRunId);
            serviceContext.put(x.currentStatusId, x.PRUN_RUNNING);
            serviceContext.put(x.actualStartDate, UtilDateTime.nowTimestamp());
            serviceContext.put(x.userLogin, userLogin);
            try {
                serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
            }
            result.put(x.newStatusId, x.PRUN_RUNNING);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusChanged, UtilMisc.toMap(
                    x.newStatusId, x.PRUN_DOC_PRINTED), locale));
            return result;
        }

        // PRUN_RUNNING --> PRUN_COMPLETED
        // this should be called only when the last task is completed
        if (x.PRUN_RUNNING.equals(currentStatusId) && (statusId == null || x.PRUN_COMPLETED.equals(statusId))) {
            // change only the production run (header) status to PRUN_COMPLETED
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put(x.workEffortId, productionRunId);
            serviceContext.put(x.currentStatusId, x.PRUN_COMPLETED);
            serviceContext.put(x.actualCompletionDate, UtilDateTime.nowTimestamp());
            serviceContext.put(x.userLogin, userLogin);
            try {
                serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
            }
            result.put(x.newStatusId, x.PRUN_COMPLETED);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusChanged, UtilMisc.toMap(
                    x.newStatusId, x.PRUN_DOC_PRINTED), locale));
            return result;
        }

        // PRUN_COMPLETED --> PRUN_CLOSED
        if (x.PRUN_COMPLETED.equals(currentStatusId) && (statusId == null || x.PRUN_CLOSED.equals(statusId))) {
            // change the production run status to PRUN_CLOSED
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put(x.workEffortId, productionRunId);
            serviceContext.put(x.currentStatusId, x.PRUN_CLOSED);
            serviceContext.put(x.userLogin, userLogin);
            try {
                serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
            }
            // change the production run tasks status to PRUN_CLOSED
            for (GenericValue task : productionRun.getProductionRunRoutingTasks()) {
                serviceContext.clear();
                serviceContext.put(x.workEffortId, task.getString(x.workEffortId));
                serviceContext.put(x.currentStatusId, x.PRUN_CLOSED);
                serviceContext.put(x.userLogin, userLogin);
                try {
                    serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            }
            result.put(x.newStatusId, x.PRUN_CLOSED);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusChanged, UtilMisc.toMap(
                    x.newStatusId, x.PRUN_CLOSED), locale));
            return result;
        }
        result.put(x.newStatusId, currentStatusId);
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusChanged, UtilMisc.toMap(
                x.newStatusId, currentStatusId), locale));
        return result;
    }

    public static Map<String, Object> changeProductionRunTaskStatus(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productionRunId = (String) context.get(x.productionRunId);
        String taskId = (String) context.get(x.workEffortId);
        String statusId = (String) context.get(x.statusId);
        Map<String, Object> serviceResult = new HashMap<>();
        Boolean issueAllComponents = (Boolean) context.get(x.issueAllComponents);
        if (issueAllComponents == null) {
            issueAllComponents = Boolean.FALSE;
        }

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotExists, locale));
        }
        List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
        GenericValue theTask = null;
        GenericValue oneTask = null;
        boolean allTaskCompleted = true;
        boolean allPrecTaskCompletedOrRunning = true;
        for (GenericValue task : tasks) {
            oneTask = task;
            if (oneTask.getString(x.workEffortId).equals(taskId)) {
                theTask = oneTask;
            } else {
                if (theTask == null && allPrecTaskCompletedOrRunning
                        && (!x.PRUN_COMPLETED.equals(oneTask.getString(x.currentStatusId))
                        && !x.PRUN_RUNNING.equals(oneTask.getString(x.currentStatusId)))) {
                    allPrecTaskCompletedOrRunning = false;
                }
                if (allTaskCompleted && !x.PRUN_COMPLETED.equals(oneTask.getString(x.currentStatusId))) {
                    allTaskCompleted = false;
                }
            }
        }
        if (theTask == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskNotExists, locale));
        }

        String currentStatusId = theTask.getString(x.currentStatusId);
        String oldStatusId = theTask.getString(x.currentStatusId); // pass back old status for secas to check

        if (statusId != null && currentStatusId.equals(statusId)) {
            result.put(x.oldStatusId, oldStatusId);
            result.put(x.newStatusId, currentStatusId);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskStatusChanged,
                    UtilMisc.toMap(x.newStatusId, currentStatusId), locale));
            return result;
        }

        // PRUN_CREATED or PRUN_SCHEDULED or PRUN_DOC_PRINTED --> PRUN_RUNNING
        // this should be called only when the first task is started
        if ((x.PRUN_CREATED.equals(currentStatusId) || x.PRUN_SCHEDULED.equals(currentStatusId) || x.PRUN_DOC_PRINTED.equals(currentStatusId))
                && (statusId == null || x.PRUN_RUNNING.equals(statusId))) {
            // change the production run task status to PRUN_RUNNING
            // if necessary change the production run (header) status to PRUN_RUNNING
            if (!allPrecTaskCompletedOrRunning) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ManufacturingProductionRunTaskCannotStartPrevTasksNotCompleted, locale));
            }
            if (x.PRUN_CREATED.equals(productionRun.getGenericValue().getString(x.currentStatusId))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskCannotStartDocsNotPrinted,
                        locale));
            }
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put(x.workEffortId, taskId);
            serviceContext.put(x.currentStatusId, x.PRUN_RUNNING);
            serviceContext.put(x.actualStartDate, UtilDateTime.nowTimestamp());
            serviceContext.put(x.userLogin, userLogin);
            try {
                serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
            }
            if (!x.PRUN_RUNNING.equals(productionRun.getGenericValue().getString(x.currentStatusId))) {
                serviceContext.clear();
                serviceContext.put(x.productionRunId, productionRunId);
                serviceContext.put(x.statusId, x.PRUN_RUNNING);
                serviceContext.put(x.userLogin, userLogin);
                try {
                    serviceResult = dispatcher.runSync(x.changeProductionRunStatus, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_calling_the_changeProductionRunStatus_service, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            }
            result.put(x.oldStatusId, oldStatusId);
            result.put(x.newStatusId, x.PRUN_RUNNING);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusChanged, UtilMisc.toMap(
                    x.newStatusId, x.PRUN_DOC_PRINTED), locale));
            return result;
        }

        // PRUN_RUNNING --> PRUN_COMPLETED
        // this should be called only when the last task is completed
        if (x.PRUN_RUNNING.equals(currentStatusId) && (statusId == null || x.PRUN_COMPLETED.equals(statusId))) {
            Map<String, Object> serviceContext = new HashMap<>();
            if (issueAllComponents) {
                // Issue all the components, if this task needs components and they still need to be issued
                try {
                    List<GenericValue> inventoryAssigned = DaoRegistry.getDao(delegator, x.WorkEffortInventoryAssign,
                            WorkEffortInventoryAssignDao.class).findByAnd(delegator, x.WorkEffortInventoryAssign,
                                    UtilMisc.toMap(x.workEffortId, taskId), null, false);
                    if (UtilValidate.isEmpty(inventoryAssigned)) {
                        serviceContext.clear();
                        serviceContext.put(x.workEffortId, taskId);
                        serviceContext.put(x.userLogin, userLogin);
                        serviceResult = dispatcher.runSync(x.issueProductionRunTask, serviceContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                        }
                    }
                } catch (GenericEntityException | GenericServiceException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            }
            // change only the production run task status to PRUN_COMPLETED
            serviceContext.clear();
            serviceContext.put(x.workEffortId, taskId);
            serviceContext.put(x.currentStatusId, x.PRUN_COMPLETED);
            serviceContext.put(x.actualCompletionDate, UtilDateTime.nowTimestamp());
            BigDecimal quantityToProduce = theTask.getBigDecimal(x.quantityToProduce);
            if (quantityToProduce == null) {
                quantityToProduce = BigDecimal.ZERO;
            }
            BigDecimal quantityProduced = theTask.getBigDecimal(x.quantityProduced);
            if (quantityProduced == null) {
                quantityProduced = BigDecimal.ZERO;
            }
            BigDecimal quantityRejected = theTask.getBigDecimal(x.quantityRejected);
            if (quantityRejected == null) {
                quantityRejected = BigDecimal.ZERO;
            }
            BigDecimal totalQuantity = quantityProduced.add(quantityRejected);
            BigDecimal diffQuantity = quantityToProduce.subtract(totalQuantity);
            if (diffQuantity.compareTo(BigDecimal.ZERO) > 0) {
                quantityProduced = quantityProduced.add(diffQuantity);
            }
            serviceContext.put(x.quantityProduced, quantityProduced);
            if (theTask.get(x.actualSetupMillis) == null) {
                serviceContext.put(x.actualSetupMillis, theTask.get(x.estimatedSetupMillis));
            }
            if (theTask.get(x.actualMilliSeconds) == null) {
                Double autoMillis = null;
                if (theTask.get(x.estimatedMilliSeconds) != null) {
                    autoMillis = quantityProduced.doubleValue() * theTask.getDouble(x.estimatedMilliSeconds);
                }
                serviceContext.put(x.actualMilliSeconds, autoMillis);
            }
            serviceContext.put(x.userLogin, userLogin);
            try {
                serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
            }
            // Calculate and store the production run task actual costs
            serviceContext.clear();
            serviceContext.put(x.productionRunTaskId, taskId);
            serviceContext.put(x.userLogin, userLogin);
            try {
                serviceResult = dispatcher.runSync(x.createProductionRunTaskCosts, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_calling_the_createProductionRunTaskCosts_service, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
            }
            // If this is the last task, then the production run is marked as 'completed'
            if (allTaskCompleted) {
                serviceContext.clear();
                serviceContext.put(x.productionRunId, productionRunId);
                serviceContext.put(x.statusId, x.PRUN_COMPLETED);
                serviceContext.put(x.userLogin, userLogin);
                try {
                    serviceResult = dispatcher.runSync(x.changeProductionRunStatus, serviceContext);
                    if (ServiceUtil.isError(result)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
                // and compute the overhead costs associated to the finished product
                try {
                    // get the currency
                    GenericValue facility = productionRun.getGenericValue().getRelatedOne(x.Facility, false);
                    Map<String, Object> outputMap = dispatcher.runSync(x.getPartyAccountingPreferences,
                            UtilMisc.<String, Object>toMap(x.userLogin, userLogin,
                                    x.organizationPartyId, facility.getString(x.ownerPartyId)));
                    if (ServiceUtil.isError(outputMap)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
                    }
                    GenericValue partyAccountingPreference = (GenericValue) outputMap.get(x.partyAccountingPreference);
                    if (partyAccountingPreference == null) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunUnableToFindCosts, locale));
                    }
                    outputMap = dispatcher.runSync(x.getProductionRunCost, UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.workEffortId,
                            productionRunId));
                    if (ServiceUtil.isError(outputMap)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
                    }
                    BigDecimal totalCost = (BigDecimal) outputMap.get(x.totalCost);
                    if (totalCost == null) {
                        totalCost = ZERO;
                    }

                    List<GenericValue> productCostComponentCalcs = DaoRegistry.getDao(delegator, x.ProductCostComponentCalc,
                            ProductCostComponentCalcDao.class).findByAnd(delegator, x.ProductCostComponentCalc,
                                    UtilMisc.toMap(x.productId, productionRun.getProductProduced().get(x.productId)),
                                    UtilMisc.toList(x.sequenceNum), false);
                    for (GenericValue productCostComponentCalc : productCostComponentCalcs) {
                        GenericValue costComponentCalc = productCostComponentCalc.getRelatedOne(x.CostComponentCalc, false);
                        GenericValue customMethod = costComponentCalc.getRelatedOne(x.CustomMethod, false);
                        if (customMethod == null) {
                            // TODO: not supported for CostComponentCalc entries directly associated to a product
                            Debug.logWarning(x.Unable_to_create_cost_component_for_cost_component_calc_with_id + costComponentCalc.getString(
                                    x.costComponentCalcId) + x.because_customMethod_is_not_set, MODULE);
                        } else {
                            Map<String, Object> costMethodResult = dispatcher.runSync(customMethod.getString(x.customMethodName),
                                    UtilMisc.toMap(x.productCostComponentCalc, productCostComponentCalc,
                                            x.costComponentCalc, costComponentCalc,
                                            x.costComponentTypePrefix, x.ACTUAL,
                                            x.baseCost, totalCost,
                                            x.currencyUomId, (String) partyAccountingPreference.get(x.baseCurrencyUomId),
                                            x.userLogin, userLogin));
                            if (ServiceUtil.isError(costMethodResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(costMethodResult));
                            }
                            BigDecimal productCostAdjustment = (BigDecimal) costMethodResult.get(x.productCostAdjustment);
                            totalCost = totalCost.add(productCostAdjustment);
                            Map<String, Object> inMap = UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.workEffortId, productionRunId);
                            inMap.put(x.costComponentCalcId, costComponentCalc.getString(x.costComponentCalcId));
                            inMap.put(x.costComponentTypeId, x.ACTUAL_5edca1a0 + productCostComponentCalc.getString(x.costComponentTypeId));
                            inMap.put(x.costUomId, partyAccountingPreference.get(x.baseCurrencyUomId));
                            inMap.put(x.cost, productCostAdjustment);
                            serviceResult = dispatcher.runSync(x.createCostComponent, inMap);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        }
                    }
                } catch (GenericEntityException | GenericServiceException gse) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunUnableToFindOverheadCosts,
                            UtilMisc.toMap(x.errorString, gse.getMessage()), locale));
                }
            }

            result.put(x.oldStatusId, oldStatusId);
            result.put(x.newStatusId, x.PRUN_COMPLETED);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusChanged, UtilMisc.toMap(
                    x.newStatusId, x.PRUN_DOC_PRINTED), locale));
            return result;
        }
        result.put(x.oldStatusId, oldStatusId);
        result.put(x.newStatusId, currentStatusId);
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskStatusChanged, UtilMisc.toMap(
                x.newStatusId, currentStatusId), locale));
        return result;
    }

    public static Map<String, Object> getWorkEffortCosts(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        String workEffortId = (String) context.get(x.workEffortId);
        Locale locale = (Locale) context.get(x.locale);
        try {
            GenericValue workEffort = DaoRegistry.getDao(delegator, x.WorkEffort, WorkEffortDao.class).findOne(delegator, x.WorkEffort,
                    UtilMisc.toMap(x.workEffortId, workEffortId), false);
            if (workEffort == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingWorkEffortNotExist, locale) + x.str_b858cb28 + workEffortId);
            }
            // Get all the valid CostComponents entries
            List<GenericValue> costComponents = DaoRegistry.getDao(delegator, x.CostComponent, CostComponentDao.class).findByAnd(delegator,
                    x.CostComponent, UtilMisc.toMap(x.workEffortId, workEffortId), null, false);
            costComponents = EntityUtil.filterByDate(costComponents);
            result.put(x.costComponents, costComponents);
            // TODO: before doing these totals we should convert the cost components' costs to the
            //       base currency uom of the owner of the facility in which the task is running
            BigDecimal totalCost = ZERO;
            BigDecimal totalCostNoMaterials = ZERO;
            for (GenericValue costComponent : costComponents) {
                BigDecimal cost = costComponent.getBigDecimal(x.cost);
                totalCost = totalCost.add(cost);
                if (!x.ACTUAL_MAT_COST.equals(costComponent.getString(x.costComponentTypeId))) {
                    totalCostNoMaterials = totalCostNoMaterials.add(cost);
                }
            }
            result.put(x.totalCost, totalCost);
            result.put(x.totalCostNoMaterials, totalCostNoMaterials);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunUnableToFindCostsForWorkEffort,
                    UtilMisc.toMap(x.workEffortId, workEffortId, x.errorString, gee.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> getProductionRunCost(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String workEffortId = (String) context.get(x.workEffortId);
        Locale locale = (Locale) context.get(x.locale);
        try {
            List<GenericValue> tasks = DaoRegistry.getDao(delegator, x.WorkEffort, WorkEffortDao.class).findByAnd(delegator, x.WorkEffort,
                    UtilMisc.toMap(x.workEffortParentId, workEffortId), UtilMisc.toList(x.workEffortId), false);
            BigDecimal totalCost = ZERO;
            Map<String, Object> outputMap = dispatcher.runSync(x.getWorkEffortCosts,
                    UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.workEffortId, workEffortId));
            if (ServiceUtil.isError(outputMap)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
            }
            BigDecimal productionRunHeaderCost = (BigDecimal) outputMap.get(x.totalCost);
            totalCost = totalCost.add(productionRunHeaderCost);
            for (GenericValue task : tasks) {
                outputMap = dispatcher.runSync(x.getWorkEffortCosts,
                        UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.workEffortId, task.getString(x.workEffortId)));
                if (ServiceUtil.isError(outputMap)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
                }
                BigDecimal taskCost = (BigDecimal) outputMap.get(x.totalCost);
                totalCost = totalCost.add(taskCost);
            }
            result.put(x.totalCost, totalCost);
        } catch (GenericEntityException | GenericServiceException exc) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunUnableToFindCosts, locale)
                    + x.str_b858cb28 + workEffortId + x.str_b858cb28 + exc.getMessage());
        }
        return result;
    }

    public static Map<String, Object> createProductionRunTaskCosts(DispatchContext ctx, ProductionRunServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> serviceResult = new HashMap<>();
        // this is the id of the actual (real) production run task
        String productionRunTaskId = (String) context.get(x.productionRunTaskId);
        try {
            GenericValue workEffort = DaoRegistry.getDao(delegator, x.WorkEffort, WorkEffortDao.class).findOne(delegator, x.WorkEffort,
                    UtilMisc.toMap(x.workEffortId, productionRunTaskId), false);
            if (UtilValidate.isEmpty(workEffort)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskNotFound, UtilMisc.toMap(
                        x.productionRunTaskId, productionRunTaskId), locale));
            }
            double actualTotalMilliSeconds = 0.0;
            Double actualSetupMillis = workEffort.getDouble(x.actualSetupMillis);
            Double actualMilliSeconds = workEffort.getDouble(x.actualMilliSeconds);
            if (actualSetupMillis == null) {
                actualSetupMillis = 0.0;
            }
            if (actualMilliSeconds == null) {
                actualMilliSeconds = 0.0;
            }
            actualTotalMilliSeconds += actualSetupMillis;
            actualTotalMilliSeconds += actualMilliSeconds;
            // Get the template (aka routing task) of the work effort
            List<GenericValue> routingTaskAssocs = DaoRegistry.getDao(delegator, x.WorkEffortAssoc, WorkEffortAssocDao.class).findByAnd(
                    delegator, x.WorkEffortAssoc,
                    UtilMisc.toMap(x.workEffortIdTo, productionRunTaskId, x.workEffortAssocTypeId, x.WORK_EFF_TEMPLATE), null, false);
            routingTaskAssocs = EntityUtil.filterByDate(routingTaskAssocs);
            GenericValue routingTaskAssoc = EntityUtil.getFirst(routingTaskAssocs);
            GenericValue routingTask = null;
            if (routingTaskAssoc != null) {
                routingTask = routingTaskAssoc.getRelatedOne(x.FromWorkEffort, false);
            }

            // Get all the valid CostComponentCalc entries
            List<GenericValue> workEffortCostCalcs = DaoRegistry.getDao(delegator, x.WorkEffortCostCalc, WorkEffortCostCalcDao.class).findByAnd(
                    delegator, x.WorkEffortCostCalc, UtilMisc.toMap(x.workEffortId, productionRunTaskId), null, false);
            workEffortCostCalcs = EntityUtil.filterByDate(workEffortCostCalcs);

            for (GenericValue workEffortCostCalc : workEffortCostCalcs) {
                GenericValue costComponentCalc = workEffortCostCalc.getRelatedOne(x.CostComponentCalc, false);
                GenericValue customMethod = costComponentCalc.getRelatedOne(x.CustomMethod, false);
                if (UtilValidate.isEmpty(customMethod) || UtilValidate.isEmpty(customMethod.getString(x.customMethodName))) {
                    // compute the total time
                    double totalTime = actualTotalMilliSeconds;
                    if (costComponentCalc.get(x.perMilliSecond) != null) {
                        long perMilliSecond = costComponentCalc.getLong(x.perMilliSecond);
                        if (perMilliSecond != 0) {
                            totalTime = totalTime / perMilliSecond;
                        }
                    }
                    // compute the cost
                    BigDecimal fixedCost = costComponentCalc.getBigDecimal(x.fixedCost);
                    BigDecimal variableCost = costComponentCalc.getBigDecimal(x.variableCost);
                    if (fixedCost == null) {
                        fixedCost = BigDecimal.ZERO;
                    }
                    if (variableCost == null) {
                        variableCost = BigDecimal.ZERO;
                    }
                    BigDecimal totalCost = fixedCost.add(variableCost.multiply(BigDecimal.valueOf(totalTime))).setScale(DECIMALS, ROUNDING);
                    // store the cost
                    Map<String, Object> inMap = UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.workEffortId, productionRunTaskId);
                    inMap.put(x.costComponentTypeId, x.ACTUAL_5edca1a0 + workEffortCostCalc.getString(x.costComponentTypeId));
                    inMap.put(x.costComponentCalcId, costComponentCalc.getString(x.costComponentCalcId));
                    inMap.put(x.costUomId, costComponentCalc.getString(x.currencyUomId));
                    inMap.put(x.cost, totalCost);
                    serviceResult = dispatcher.runSync(x.createCostComponent, inMap);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } else {
                    // use the custom method (aka formula) to compute the costs
                    Map<String, Object> inMap = UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.workEffort, workEffort);
                    inMap.put(x.workEffortCostCalc, workEffortCostCalc);
                    inMap.put(x.costComponentCalc, costComponentCalc);
                    serviceResult = dispatcher.runSync(customMethod.getString(x.customMethodName), inMap);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
            }

            // Now get the cost information associated to the fixed asset and compute the costs
            GenericValue fixedAsset = workEffort.getRelatedOne(x.FixedAsset, false);
            if (fixedAsset != null && routingTask != null) {
                fixedAsset = routingTask.getRelatedOne(x.FixedAsset, false);
            }
            if (fixedAsset != null) {
                List<GenericValue> setupCosts = fixedAsset.getRelated(x.FixedAssetStdCost,
                        UtilMisc.toMap(x.fixedAssetStdCostTypeId, x.SETUP_COST), null, false);
                GenericValue setupCost = EntityUtil.getFirst(EntityUtil.filterByDate(setupCosts));
                List<GenericValue> usageCosts = fixedAsset.getRelated(x.FixedAssetStdCost, UtilMisc.toMap(x.fixedAssetStdCostTypeId, x.USAGE_COST),
                        null, false);
                GenericValue usageCost = EntityUtil.getFirst(EntityUtil.filterByDate(usageCosts));
                if (setupCost != null || usageCost != null) {
                    String currencyUomId = (setupCost != null ? setupCost.getString(x.amountUomId) : usageCost.getString(x.amountUomId));
                    BigDecimal setupCostAmount = ZERO;
                    if (setupCost != null) {
                        setupCostAmount = setupCost.getBigDecimal(x.amount).multiply(BigDecimal.valueOf(actualSetupMillis));
                    }
                    BigDecimal usageCostAmount = ZERO;
                    if (usageCost != null) {
                        usageCostAmount = usageCost.getBigDecimal(x.amount).multiply(BigDecimal.valueOf(actualMilliSeconds));
                    }
                    BigDecimal fixedAssetCost = setupCostAmount.add(usageCostAmount).setScale(DECIMALS, ROUNDING);
                    fixedAssetCost = fixedAssetCost.divide(BigDecimal.valueOf(3600000), DECIMALS, ROUNDING);
                    // store the cost
                    Map<String, Object> inMap = UtilMisc.<String, Object>toMap(x.userLogin, userLogin,
                            x.workEffortId, productionRunTaskId);
                    inMap.put(x.costComponentTypeId, x.ACTUAL_ROUTE_COST);
                    inMap.put(x.costUomId, currencyUomId);
                    inMap.put(x.cost, fixedAssetCost);
                    inMap.put(x.fixedAssetId, fixedAsset.get(x.fixedAssetId));
                    serviceResult = dispatcher.runSync(x.createCostComponent, inMap);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
            }
        } catch (GenericEntityException | GenericServiceException ge) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunUnableToCreateRoutingCosts,
                    UtilMisc.toMap(x.productionRunTaskId, productionRunTaskId, x.errorString, ge.getMessage()), locale));
        }
        // materials costs: these are the costs derived from the materials used by the production run task
        try {
            Map<String, BigDecimal> materialsCostByCurrency = new HashMap<>();
            for (GenericValue inventoryConsumed : DaoRegistry.getDao(delegator, x.WorkEffortAndInventoryAssign, WorkEffortDao.class).findByAnd(
                    delegator, x.WorkEffortAndInventoryAssign, UtilMisc.toMap(x.workEffortId, productionRunTaskId), null, false)) {
                BigDecimal quantity = inventoryConsumed.getBigDecimal(x.quantity);
                BigDecimal unitCost = inventoryConsumed.getBigDecimal(x.unitCost);
                if (UtilValidate.isEmpty(unitCost) || UtilValidate.isEmpty(quantity)) {
                    continue;
                }
                String currencyUomId = inventoryConsumed.getString(x.currencyUomId);
                if (!materialsCostByCurrency.containsKey(currencyUomId)) {
                    materialsCostByCurrency.put(currencyUomId, BigDecimal.ZERO);
                }
                BigDecimal materialsCost = materialsCostByCurrency.get(currencyUomId);
                materialsCost = materialsCost.add(unitCost.multiply(quantity)).setScale(DECIMALS, ROUNDING);
                materialsCostByCurrency.put(currencyUomId, materialsCost);
            }
            for (String currencyUomId : materialsCostByCurrency.keySet()) {
                BigDecimal materialsCost = materialsCostByCurrency.get(currencyUomId);
                Map<String, Object> inMap = UtilMisc.<String, Object>toMap(x.userLogin, userLogin,
                        x.workEffortId, productionRunTaskId);
                inMap.put(x.costComponentTypeId, x.ACTUAL_MAT_COST);
                inMap.put(x.costUomId, currencyUomId);
                inMap.put(x.cost, materialsCost);
                serviceResult = dispatcher.runSync(x.createCostComponent, inMap);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
        } catch (GenericEntityException | GenericServiceException ge) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunUnableToCreateMaterialsCosts,
                    UtilMisc.toMap(x.productionRunTaskId, productionRunTaskId, x.errorString, ge.getMessage()), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Check if field for routingTask update are correct and if need recalculated data in Production Run.
     * Check
     * <ul>
     * <li> if estimatedStartDate is not before Production Run estimatedStartDate.</li>
     * <li> if there is not a another routingTask with the same priority</li>
     * <li>If priority or estimatedStartDate has changed recalculated data for routingTask after that one.</li>
     * </ul>
     * Update the productionRun
     * @param ctx     The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters, productId, routingId, priority, estimatedStartDate, estimatedSetupMillis,
     *                estimatedMilliSeconds
     * @return Map with the result of the service, the output parameters, estimatedCompletionDate.
     */
    public static Map<String, Object> checkUpdatePrunRoutingTask(DispatchContext ctx, ProductionRunServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Map<String, Object> serviceResult = new HashMap<>();
        String productionRunId = (String) context.get(x.productionRunId);
        String routingTaskId = (String) context.get(x.routingTaskId);
        if (!UtilValidate.isEmpty(productionRunId) && !UtilValidate.isEmpty(routingTaskId)) {
            ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
            if (productionRun.exist()) {

                if (!x.PRUN_CREATED.equals(productionRun.getGenericValue().getString(x.currentStatusId))
                        && !x.PRUN_SCHEDULED.equals(productionRun.getGenericValue().getString(x.currentStatusId))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunPrinted, locale));
                }

                Timestamp estimatedStartDate = (Timestamp) context.get(x.estimatedStartDate);
                Timestamp pRestimatedStartDate = productionRun.getEstimatedStartDate();
                if (pRestimatedStartDate.after(estimatedStartDate)) {
                    try {
                        serviceResult = dispatcher.runSync(x.updateProductionRun, UtilMisc.toMap(x.productionRunId, productionRunId,
                                x.estimatedStartDate, estimatedStartDate, x.userLogin, userLogin));
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } catch (GenericServiceException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingRoutingTaskStartDateBeforePRun, locale));
                    }
                }

                Long priority = (Long) context.get(x.priority);
                List<GenericValue> pRRoutingTasks = productionRun.getProductionRunRoutingTasks();
                boolean first = true;
                for (GenericValue routingTask : pRRoutingTasks) {
                    if (priority.equals(routingTask.get(x.priority)) && !routingTaskId.equals(routingTask.get(x.workEffortId))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingRoutingTaskSeqIdAlreadyExist, locale));
                    }
                    if (routingTaskId.equals(routingTask.get(x.workEffortId))) {
                        routingTask.set(x.estimatedSetupMillis, ((BigDecimal) context.get(x.estimatedSetupMillis)).doubleValue());
                        routingTask.set(x.estimatedMilliSeconds, ((BigDecimal) context.get(x.estimatedMilliSeconds)).doubleValue());
                        if (first) {    // for the first routingTask the estimatedStartDate update imply estimatedStartDate productonRun update
                            if (!estimatedStartDate.equals(pRestimatedStartDate)) {
                                productionRun.setEstimatedStartDate(estimatedStartDate);
                            }
                        }
                        // the priority has been changed
                        if (!priority.equals(routingTask.get(x.priority))) {
                            routingTask.set(x.priority, priority);
                            // update the routingTask List and re-read it to be able to have it sorted with the new value
                            if (!productionRun.store()) {
                                Debug.logError(x.productionRun_store_in_routingTask_priority_update_fail_for_productionRunId
                                        + productionRunId, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotUpdated, locale));
                            }
                            productionRun.clearRoutingTasksList();
                        }
                    }
                    if (first) first = false;
                }
                productionRun.setEstimatedCompletionDate(productionRun.recalculateEstimatedCompletionDate(priority, estimatedStartDate));

                if (productionRun.store()) {
                    return ServiceUtil.returnSuccess();
                } else {
                    Debug.logError(x.productionRun_store_fail_for_productionRunId + productionRunId, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotUpdated, locale));
                }
            }
            Debug.logError(x.no_productionRun_for_productionRunId + productionRunId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotUpdated, locale));
        }
        Debug.logError(x.service_updateProductionRun_call_with_productionRunId_empty, MODULE);
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotUpdated, locale));
    }

    public static Map<String, Object> addProductionRunComponent(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Timestamp now = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String productionRunId = (String) context.get(x.productionRunId);
        String productId = (String) context.get(x.productId);
        BigDecimal quantity = (BigDecimal) context.get(x.estimatedQuantity);
        // Optional input fields
        String workEffortId = (String) context.get(x.workEffortId);

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
        if (UtilValidate.isEmpty(tasks)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskNotExists, locale));
        }

        if (!x.PRUN_CREATED.equals(productionRun.getGenericValue().getString(x.currentStatusId))
                && !x.PRUN_SCHEDULED.equals(productionRun.getGenericValue().getString(x.currentStatusId))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunPrinted, locale));
        }

        if (workEffortId != null) {
            boolean found = false;
            for (int i = 0; i < tasks.size(); i++) {
                GenericValue oneTask = tasks.get(i);
                if (oneTask.getString(x.workEffortId).equals(workEffortId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskNotExists, locale));
            }
        } else {
            workEffortId = EntityUtil.getFirst(tasks).getString(x.workEffortId);
        }

        try {
            // Find the product
            GenericValue product = DaoRegistry.getDao(delegator, x.Product, ProductDao.class).findOne(delegator, x.Product,
                    UtilMisc.toMap(x.productId, productId), false);
            if (product == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductNotExist, locale));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.clear();
        serviceContext.put(x.workEffortId, workEffortId);
        serviceContext.put(x.productId, productId);
        serviceContext.put(x.workEffortGoodStdTypeId, x.PRUNT_PROD_NEEDED);
        serviceContext.put(x.statusId, x.WEGS_CREATED);
        serviceContext.put(x.fromDate, now);
        serviceContext.put(x.estimatedQuantity, quantity);
        serviceContext.put(x.userLogin, userLogin);
        try {
            Map<String, Object> serviceResult = dispatcher.runSync(x.createWorkEffortGoodStandard, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_createWorkEffortGoodStandard_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunComponentNotAdded, locale));
        }
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE,
                x.ManufacturingProductionRunComponentAdded, UtilMisc.toMap(x.productionRunId, productionRunId), locale));
        return result;
    }

    public static Map<String, Object> updateProductionRunComponent(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String productionRunId = (String) context.get(x.productionRunId);
        String productId = (String) context.get(x.productId);
        // Optional input fields
        String workEffortId = (String) context.get(x.workEffortId); // the production run task
        BigDecimal quantity = (BigDecimal) context.get(x.estimatedQuantity);

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        List<GenericValue> components = productionRun.getProductionRunComponents();
        if (UtilValidate.isEmpty(components)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunComponentNotExists, locale));
        }

        if (!x.PRUN_CREATED.equals(productionRun.getGenericValue().getString(x.currentStatusId))
                && !x.PRUN_SCHEDULED.equals(productionRun.getGenericValue().getString(x.currentStatusId))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunPrinted, locale));
        }

        boolean found = false;
        GenericValue theComponent = null;
        for (int i = 0; i < components.size(); i++) {
            theComponent = components.get(i);
            if (theComponent.getString(x.productId).equals(productId)) {
                if (workEffortId != null) {
                    if (theComponent.getString(x.workEffortId).equals(workEffortId)) {
                        found = true;
                        break;
                    }
                } else {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskNotExists, locale));
        }

        try {
            // Find the product
            GenericValue product = DaoRegistry.getDao(delegator, x.Product, ProductDao.class).findOne(delegator, x.Product,
                    UtilMisc.toMap(x.productId, productId), false);
            if (product == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductNotExist, locale));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.clear();
        serviceContext.put(x.workEffortId, theComponent.getString(x.workEffortId));
        serviceContext.put(x.workEffortGoodStdTypeId, x.PRUNT_PROD_NEEDED);
        serviceContext.put(x.productId, productId);
        serviceContext.put(x.fromDate, theComponent.getTimestamp(x.fromDate));
        if (quantity != null) {
            serviceContext.put(x.estimatedQuantity, quantity);
        }
        serviceContext.put(x.userLogin, userLogin);
        try {
            Map<String, Object> serviceResult = dispatcher.runSync(x.updateWorkEffortGoodStandard, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_updateWorkEffortGoodStandard_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunComponentNotAdded, locale));
        }
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunComponentUpdated, UtilMisc.toMap(
                x.productionRunId, productionRunId), locale));
        return result;
    }

    public static Map<String, Object> addProductionRunRoutingTask(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String productionRunId = (String) context.get(x.productionRunId);
        String routingTaskId = (String) context.get(x.routingTaskId);
        Long priority = (Long) context.get(x.priority);

        // Optional input fields
        String workEffortName = (String) context.get(x.workEffortName);
        String description = (String) context.get(x.description);
        Timestamp estimatedStartDate = (Timestamp) context.get(x.estimatedStartDate);
        Timestamp estimatedCompletionDate = (Timestamp) context.get(x.estimatedCompletionDate);

        Double estimatedSetupMillis = null;
        if (context.get(x.estimatedSetupMillis) != null) {
            estimatedSetupMillis = ((BigDecimal) context.get(x.estimatedSetupMillis)).doubleValue();
        }
        Double estimatedMilliSeconds = null;
        if (context.get(x.estimatedMilliSeconds) != null) {
            estimatedMilliSeconds = ((BigDecimal) context.get(x.estimatedMilliSeconds)).doubleValue();
        }
        // The production run is loaded
        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        BigDecimal pRQuantity = productionRun.getQuantity();
        if (pRQuantity == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskNotExists, locale));
        }

        if (!x.PRUN_CREATED.equals(productionRun.getGenericValue().getString(x.currentStatusId))
                && !x.PRUN_SCHEDULED.equals(productionRun.getGenericValue().getString(x.currentStatusId))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunPrinted, locale));
        }

        if (estimatedStartDate != null) {
            Timestamp pRestimatedStartDate = productionRun.getEstimatedStartDate();
            if (pRestimatedStartDate.after(estimatedStartDate)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingRoutingTaskStartDateBeforePRun, locale));
            }
        }

        // The routing task is loaded
        GenericValue routingTask = null;
        try {
            routingTask = DaoRegistry.getDao(delegator, x.WorkEffort, WorkEffortDao.class).findOne(delegator, x.WorkEffort,
                    UtilMisc.toMap(x.workEffortId, routingTaskId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingRoutingTaskNotExists, locale));
        }
        if (routingTask == null) {
            Debug.logError(x.Routing_task + routingTaskId + x.is_null, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingRoutingTaskNotExists, locale));
        }

        if (workEffortName == null) {
            workEffortName = (String) routingTask.get(x.workEffortName);
        }
        if (description == null) {
            description = (String) routingTask.get(x.description);
        }
        if (estimatedSetupMillis == null) {
            estimatedSetupMillis = (Double) routingTask.get(x.estimatedSetupMillis);
        }
        if (estimatedMilliSeconds == null) {
            estimatedMilliSeconds = (Double) routingTask.get(x.estimatedMilliSeconds);
        }
        if (estimatedStartDate == null) {
            estimatedStartDate = productionRun.getEstimatedStartDate();
        }
        if (estimatedCompletionDate == null) {
            // Calculate the estimatedCompletionDate
            long totalTime = ProductionRun.getEstimatedTaskTime(routingTask, pRQuantity, dispatcher);
            estimatedCompletionDate = TechDataServices.addForward(TechDataServices.getTechDataCalendar(routingTask), estimatedStartDate, totalTime);
        }
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.clear();
        serviceContext.put(x.priority, priority);
        serviceContext.put(x.workEffortPurposeTypeId, routingTask.get(x.workEffortPurposeTypeId));
        serviceContext.put(x.workEffortName, workEffortName);
        serviceContext.put(x.description, description);
        serviceContext.put(x.fixedAssetId, routingTask.get(x.fixedAssetId));
        serviceContext.put(x.workEffortTypeId, x.PROD_ORDER_TASK);
        serviceContext.put(x.currentStatusId, x.PRUN_CREATED);
        serviceContext.put(x.workEffortParentId, productionRunId);
        serviceContext.put(x.facilityId, productionRun.getGenericValue().getString(x.facilityId));
        serviceContext.put(x.estimatedStartDate, estimatedStartDate);
        serviceContext.put(x.estimatedCompletionDate, estimatedCompletionDate);
        serviceContext.put(x.estimatedSetupMillis, estimatedSetupMillis);
        serviceContext.put(x.estimatedMilliSeconds, estimatedMilliSeconds);
        serviceContext.put(x.quantityToProduce, pRQuantity);
        serviceContext.put(x.userLogin, userLogin);
        Map<String, Object> serviceResult = null;
        try {
            serviceResult = dispatcher.runSync(x.createWorkEffort, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_createWorkEffort_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingAddProductionRunRoutingTaskNotCreated, locale));
        }
        String productionRunTaskId = (String) serviceResult.get(x.workEffortId);
        if (Debug.infoOn()) {
            Debug.logInfo(x.ProductionRunTaskId_created + productionRunTaskId, MODULE);
        }


        productionRun.setEstimatedCompletionDate(productionRun.recalculateEstimatedCompletionDate());
        if (!productionRun.store()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingAddProductionRunRoutingTaskNotCreated, locale));
        }

        // copy date valid WorkEffortPartyAssignments from the routing task to the run task
        List<GenericValue> workEffortPartyAssignments = null;
        try {
            workEffortPartyAssignments = DaoRegistry.getDao(delegator, x.WorkEffortPartyAssignment, WorkEffortPartyAssignmentDao.class).findByAnd(
                    delegator, x.WorkEffortPartyAssignment, UtilMisc.toMap(x.workEffortId, routingTaskId), null, false);
            workEffortPartyAssignments = EntityUtil.filterByDate(workEffortPartyAssignments);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
        }
        if (workEffortPartyAssignments != null) {
            for (GenericValue workEffortPartyAssignment : workEffortPartyAssignments) {
                Map<String, Object> partyToWorkEffort = UtilMisc.<String, Object>toMap(
                        x.workEffortId, productionRunTaskId,
                        x.partyId, workEffortPartyAssignment.getString(x.partyId),
                        x.roleTypeId, workEffortPartyAssignment.getString(x.roleTypeId),
                        x.fromDate, workEffortPartyAssignment.getTimestamp(x.fromDate),
                        x.statusId, workEffortPartyAssignment.getString(x.statusId),
                        x.userLogin, userLogin);
                try {
                    serviceResult = dispatcher.runSync(x.assignPartyToWorkEffort, partyToWorkEffort);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_calling_the_assignPartyToWorkEffort_service, MODULE);
                }
                if (Debug.infoOn()) {
                    Debug.logInfo(x.ProductionRunPartyassigment_for_party + workEffortPartyAssignment.get(x.partyId) + x.created_c79bef7c, MODULE);
                }
            }
        }

        result.put(x.routingTaskId, productionRunTaskId);
        result.put(x.estimatedStartDate, estimatedStartDate);
        result.put(x.estimatedCompletionDate, estimatedCompletionDate);
        return result;
    }

    public static Map<String, Object> productionRunProduce(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String productionRunId = (String) context.get(x.workEffortId);

        // Optional input fields
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        String inventoryItemTypeId = (String) context.get(x.inventoryItemTypeId);
        String lotId = (String) context.get(x.lotId);
        String uomId = (String) context.get(x.quantityUomId);
        String locationSeqId = (String) context.get(x.locationSeqId);
        Boolean createLotIfNeeded = (Boolean) context.get(x.createLotIfNeeded);
        Boolean autoCreateLot = (Boolean) context.get(x.autoCreateLot);

        // The default is non-serialized inventory item
        if (UtilValidate.isEmpty(inventoryItemTypeId)) {
            inventoryItemTypeId = x.NON_SERIAL_INV_ITEM;
        }
        // The default is to create a lot if the lotId is given, but the lot doesn't exist
        if (createLotIfNeeded == null) {
            createLotIfNeeded = Boolean.TRUE;
        }
        if (autoCreateLot == null) {
            autoCreateLot = Boolean.FALSE;
        }

        List<String> inventoryItemIds = new LinkedList<>();
        result.put(x.inventoryItemIds, inventoryItemIds);
        // The production run is loaded
        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        // The last task is loaded
        GenericValue lastTask = productionRun.getLastProductionRunRoutingTask();
        if (lastTask == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskNotExists, locale));
        }
        if (x.WIP.equals(productionRun.getProductProduced().getString(x.productTypeId))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductIsWIP, locale));
        }
        BigDecimal quantityProduced = productionRun.getGenericValue().getBigDecimal(x.quantityProduced);

        if (quantityProduced == null) {
            quantityProduced = BigDecimal.ZERO;
        }
        BigDecimal quantityDeclared = lastTask.getBigDecimal(x.quantityProduced);

        if (quantityDeclared == null) {
            quantityDeclared = BigDecimal.ZERO;
        }
        // If the quantity already produced is not lower than the quantity declared, no inventory is created.
        BigDecimal maxQuantity = quantityDeclared.subtract(quantityProduced);

        if (maxQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return result;
        }

        // If quantity was not passed, the max quantity is used
        if (quantity == null) {
            quantity = maxQuantity;
        }
        //
        if (quantity.compareTo(maxQuantity) > 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunProductProducedNotStillAvailable, locale));
        }

        if (lotId == null && autoCreateLot) {
            createLotIfNeeded = Boolean.TRUE;
        }
        try {
            // Find the lot
            GenericValue lot = DaoRegistry.getDao(delegator, x.Lot, LotDao.class).findOne(delegator, x.Lot, UtilMisc.toMap(x.lotId, lotId),
                    false);
            if (lot == null) {
                if (createLotIfNeeded) {
                    Map<String, Object> createLotCtx = ctx.makeValidContext(x.createLot, ModelService.IN_PARAM, context);
                    createLotCtx.put(x.creationDate, UtilDateTime.nowTimestamp());
                    Map<String, Object> serviceResults = dispatcher.runSync(x.createLot, createLotCtx);
                    if (ServiceUtil.isError(serviceResults)) {
                        Debug.logError(ServiceUtil.getErrorMessage(serviceResults), MODULE);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                    }
                    lotId = (String) serviceResults.get(x.lotId);
                } else if (UtilValidate.isNotEmpty(lotId)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingLotNotExists, locale));
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        GenericValue orderItem = null;
        try {
            // Find the related order item (if exists)
            List<GenericValue> orderItems = productionRun.getGenericValue().getRelated(x.WorkOrderItemFulfillment, null, null, false);
            orderItem = EntityUtil.getFirst(orderItems);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        // the inventory item unit cost is the product's standard cost
        BigDecimal unitCost = ZERO;
        GenericValue facility = null;
        try {
            // get the currency
            facility = productionRun.getGenericValue().getRelatedOne(x.Facility, false);
            Map<String, Object> outputMap = dispatcher.runSync(x.getPartyAccountingPreferences, UtilMisc.<String, Object>toMap(x.userLogin,
                    userLogin, x.organizationPartyId, facility.getString(x.ownerPartyId)));
            if (ServiceUtil.isError(outputMap)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
            }
            GenericValue partyAccountingPreference = (GenericValue) outputMap.get(x.partyAccountingPreference);
            if (partyAccountingPreference == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunUnableToFindCosts, locale));
            }
            outputMap = dispatcher.runSync(x.getProductCost, UtilMisc.<String, Object>toMap(x.userLogin, userLogin, x.productId,
                    productionRun.getProductProduced().getString(x.productId), x.currencyUomId,
                    (String) partyAccountingPreference.get(x.baseCurrencyUomId), x.costComponentTypePrefix, x.EST_STD));
            if (ServiceUtil.isError(outputMap)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
            }
            unitCost = (BigDecimal) outputMap.get(x.productCost);
            if (unitCost != null && unitCost.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal totalCost = ZERO;
                List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
                // generic_cost
                List<GenericValue> actualGenCosts = DaoRegistry.getDao(delegator, x.CostComponent, CostComponentDao.class).findByAnd(delegator,
                        x.CostComponent,
                        UtilMisc.toMap(x.workEffortId, productionRunId, x.costUomId, partyAccountingPreference.get(x.baseCurrencyUomId)), null,
                        false);
                for (GenericValue actualGenCost : actualGenCosts) {
                    totalCost = totalCost.add((BigDecimal) actualGenCost.get(x.cost));
                }
                for (GenericValue task : tasks) {
                    List<GenericValue> otherCosts = DaoRegistry.getDao(delegator, x.CostComponent, CostComponentDao.class).findByAnd(delegator,
                            x.CostComponent,
                            UtilMisc.toMap(x.workEffortId, task.get(x.workEffortId), x.costUomId,
                                    partyAccountingPreference.get(x.baseCurrencyUomId)),
                            null, false);
                    for (GenericValue otherCost : otherCosts) {
                        totalCost = totalCost.add((BigDecimal) otherCost.get(x.cost));
                    }
                }
                if (totalCost != null) {
                    unitCost = totalCost.divide(quantity, DECIMALS, ROUNDING);
                } else {
                    unitCost = BigDecimal.ZERO;
                }
            }
            // Before creating InvntoryItem and InventoryItemDetails, check weather the record of ProductFacility exist in the system or not
            GenericValue productFacility = DaoRegistry.getDao(delegator, x.ProductFacility, ProductFacilityDao.class).findOne(delegator,
                    x.ProductFacility,
                    UtilMisc.toMap(x.productId, productionRun.getProductProduced().getString(x.productId), x.facilityId,
                            facility.get(x.facilityId)),
                    false);
            if (productFacility == null) {
                Map<String, Object> createProductFacilityCtx = new HashMap<>();
                createProductFacilityCtx.put(x.productId, productionRun.getProductProduced().getString(x.productId));
                createProductFacilityCtx.put(x.facilityId, facility.get(x.facilityId));
                createProductFacilityCtx.put(x.userLogin, userLogin);
                Map<String, Object> serviceResult = dispatcher.runSync(x.createProductFacility, createProductFacilityCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }

        } catch (GenericEntityException | GenericServiceException gse) {
            Debug.logWarning(gse.getMessage(), MODULE);
            return ServiceUtil.returnError(gse.getMessage());
        }

        if (x.SERIALIZED_INV_ITEM.equals(inventoryItemTypeId)) {
            try {
                int numOfItems = quantity.intValue();
                for (int i = 0; i < numOfItems; i++) {
                    Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap(x.productId, productionRun.getProductProduced().getString(
                            x.productId),
                            x.inventoryItemTypeId, x.SERIALIZED_INV_ITEM,
                            x.statusId, x.INV_AVAILABLE);
                    serviceContext.put(x.facilityId, productionRun.getGenericValue().getString(x.facilityId));
                    serviceContext.put(x.datetimeReceived, UtilDateTime.nowTimestamp());
                    serviceContext.put(x.datetimeManufactured, UtilDateTime.nowTimestamp());
                    serviceContext.put(x.comments, x.Created_by_production_run + productionRunId);
                    if (unitCost.compareTo(ZERO) != 0) {
                        serviceContext.put(x.unitCost, unitCost);
                    }
                    serviceContext.put(x.lotId, lotId);
                    serviceContext.put(x.locationSeqId, locationSeqId);
                    serviceContext.put(x.uomId, uomId);
                    serviceContext.put(x.userLogin, userLogin);
                    Map<String, Object> serviceResult = dispatcher.runSync(x.createInventoryItem, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    String inventoryItemId = (String) serviceResult.get(x.inventoryItemId);
                    inventoryItemIds.add(inventoryItemId);
                    serviceContext.clear();
                    serviceContext.put(x.inventoryItemId, inventoryItemId);
                    serviceContext.put(x.workEffortId, productionRunId);
                    serviceContext.put(x.availableToPromiseDiff, BigDecimal.ONE);
                    serviceContext.put(x.quantityOnHandDiff, BigDecimal.ONE);
                    serviceContext.put(x.userLogin, userLogin);
                    serviceResult = dispatcher.runSync(x.createInventoryItemDetail, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    serviceContext.clear();
                    serviceContext.put(x.userLogin, userLogin);
                    serviceContext.put(x.workEffortId, productionRunId);
                    serviceContext.put(x.inventoryItemId, inventoryItemId);
                    serviceResult = dispatcher.runSync(x.createWorkEffortInventoryProduced, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    // Recompute reservations
                    serviceContext = new HashMap<>();
                    serviceContext.put(x.inventoryItemId, inventoryItemId);
                    serviceContext.put(x.userLogin, userLogin);
                    serviceResult = dispatcher.runSync(x.balanceInventoryItems, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
            } catch (GenericServiceException exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        } else {
            try {
                Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap(x.productId, productionRun.getProductProduced().getString(
                        x.productId),
                        x.inventoryItemTypeId, x.NON_SERIAL_INV_ITEM);
                serviceContext.put(x.facilityId, productionRun.getGenericValue().getString(x.facilityId));
                serviceContext.put(x.datetimeReceived, UtilDateTime.nowTimestamp());
                serviceContext.put(x.datetimeManufactured, UtilDateTime.nowTimestamp());
                serviceContext.put(x.comments, x.Created_by_production_run + productionRunId);
                serviceContext.put(x.lotId, lotId);
                serviceContext.put(x.locationSeqId, locationSeqId);
                serviceContext.put(x.uomId, uomId);
                if (unitCost.compareTo(ZERO) != 0) {
                    serviceContext.put(x.unitCost, unitCost);
                }
                serviceContext.put(x.userLogin, userLogin);
                Map<String, Object> serviceResult = dispatcher.runSync(x.createInventoryItem, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                String inventoryItemId = (String) serviceResult.get(x.inventoryItemId);
                inventoryItemIds.add(inventoryItemId);
                serviceContext.clear();
                serviceContext.put(x.inventoryItemId, inventoryItemId);
                serviceContext.put(x.workEffortId, productionRunId);
                serviceContext.put(x.availableToPromiseDiff, quantity);
                serviceContext.put(x.quantityOnHandDiff, quantity);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.createInventoryItemDetail, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                serviceContext.clear();
                serviceContext.put(x.userLogin, userLogin);
                serviceContext.put(x.workEffortId, productionRunId);
                serviceContext.put(x.inventoryItemId, inventoryItemId);
                serviceResult = dispatcher.runSync(x.createWorkEffortInventoryProduced, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                // Recompute reservations
                serviceContext = new HashMap<>();
                serviceContext.put(x.inventoryItemId, inventoryItemId);
                serviceContext.put(x.userLogin, userLogin);
                if (orderItem != null) {
                    // the reservations of this order item are privileged reservations
                    serviceContext.put(x.priorityOrderId, orderItem.getString(x.orderId));
                    serviceContext.put(x.priorityOrderItemSeqId, orderItem.getString(x.orderItemSeqId));
                }
                serviceResult = dispatcher.runSync(x.balanceInventoryItems, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        }
        // Now the production run's quantityProduced is updated
        Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap(x.workEffortId, productionRunId);
        serviceContext.put(x.quantityProduced, quantityProduced.add(quantity));
        serviceContext.put(x.actualCompletionDate, UtilDateTime.nowTimestamp());
        serviceContext.put(x.userLogin, userLogin);
        try {
            Map<String, Object> serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_updateWorkEffort_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
        }

        result.put(x.quantity, quantity);
        return result;
    }

    public static Map<String, Object> productionRunDeclareAndProduce(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String productionRunId = (String) context.get(x.workEffortId);

        // Optional input fields
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        Map<GenericPK, Object> componentsLocationMap = UtilGenerics.cast(context.get(x.componentsLocationMap));

        // The production run is loaded
        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);

        BigDecimal quantityProduced = productionRun.getGenericValue().getBigDecimal(x.quantityProduced);
        BigDecimal quantityToProduce = productionRun.getGenericValue().getBigDecimal(x.quantityToProduce);
        if (quantityProduced == null) {
            quantityProduced = BigDecimal.ZERO;
        }
        if (quantityToProduce == null) {
            quantityToProduce = BigDecimal.ZERO;
        }
        BigDecimal minimumQuantityProducedByTask = quantityProduced.add(quantity);
        if (minimumQuantityProducedByTask.compareTo(quantityToProduce) > 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingQuantityProducedIsHigherThanQuantityDeclared, locale));
        }

        List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
        for (int i = 0; i < tasks.size(); i++) {
            GenericValue oneTask = tasks.get(i);
            String taskId = oneTask.getString(x.workEffortId);
            if (x.PRUN_RUNNING.equals(oneTask.getString(x.currentStatusId))) {
                BigDecimal quantityDeclared = oneTask.getBigDecimal(x.quantityProduced);
                if (quantityDeclared == null) {
                    quantityDeclared = BigDecimal.ZERO;
                }
                if (minimumQuantityProducedByTask.compareTo(quantityDeclared) > 0) {
                    try {
                        Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap(x.productionRunId, productionRunId,
                                x.productionRunTaskId, taskId);
                        serviceContext.put(x.addQuantityProduced, minimumQuantityProducedByTask.subtract(quantityDeclared));
                        serviceContext.put(x.issueRequiredComponents, Boolean.TRUE);
                        serviceContext.put(x.componentsLocationMap, componentsLocationMap);
                        serviceContext.put(x.userLogin, userLogin);
                        Map<String, Object> serviceResult = dispatcher.runSync(x.updateProductionRunTask, serviceContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, x.Problem_calling_the_changeProductionRunTaskStatus_service, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                    }
                }
            }
        }
        try {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.putAll(context);
            inputMap.remove(x.componentsLocationMap);
            result = dispatcher.runSync(x.productionRunProduce, inputMap);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_changeProductionRunTaskStatus_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
        }
        return result;
    }

    public static Map<String, Object> productionRunTaskProduce(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String productionRunTaskId = (String) context.get(x.workEffortId);
        String productId = (String) context.get(x.productId);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);

        // Optional input fields
        String facilityId = (String) context.get(x.facilityId);
        String currencyUomId = (String) context.get(x.currencyUomId);
        BigDecimal unitCost = (BigDecimal) context.get(x.unitCost);
        String inventoryItemTypeId = (String) context.get(x.inventoryItemTypeId);
        String lotId = (String) context.get(x.lotId);
        String uomId = (String) context.get(x.quantityUomId);
        String isReturned = (String) context.get(x.isReturned);

        // The default is non-serialized inventory item
        if (UtilValidate.isEmpty(inventoryItemTypeId)) {
            inventoryItemTypeId = x.NON_SERIAL_INV_ITEM;
        }

        if (facilityId == null) {
            // The production run is loaded
            ProductionRun productionRun = new ProductionRun(productionRunTaskId, delegator, dispatcher);
            facilityId = productionRun.getGenericValue().getString(x.facilityId);
        }
        List<String> inventoryItemIds = new LinkedList<>();
        if (x.SERIALIZED_INV_ITEM.equals(inventoryItemTypeId)) {
            try {
                int numOfItems = quantity.intValue();
                for (int i = 0; i < numOfItems; i++) {
                    Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap(x.productId, productId,
                            x.inventoryItemTypeId, x.SERIALIZED_INV_ITEM,
                            x.statusId, x.INV_AVAILABLE);
                    serviceContext.put(x.facilityId, facilityId);
                    serviceContext.put(x.datetimeReceived, UtilDateTime.nowTimestamp());
                    serviceContext.put(x.datetimeManufactured, UtilDateTime.nowTimestamp());
                    serviceContext.put(x.comments, x.Created_by_production_run_task + productionRunTaskId);
                    if (unitCost != null) {
                        serviceContext.put(x.unitCost, unitCost);
                        serviceContext.put(x.currencyUomId, currencyUomId);
                    }
                    serviceContext.put(x.lotId, lotId);
                    serviceContext.put(x.uomId, uomId);
                    serviceContext.put(x.userLogin, userLogin);
                    serviceContext.put(x.isReturned, isReturned);
                    Map<String, Object> serviceResult = dispatcher.runSync(x.createInventoryItem, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    String inventoryItemId = (String) serviceResult.get(x.inventoryItemId);
                    serviceContext.clear();
                    serviceContext.put(x.inventoryItemId, inventoryItemId);
                    serviceContext.put(x.workEffortId, productionRunTaskId);
                    serviceContext.put(x.availableToPromiseDiff, BigDecimal.ONE);
                    serviceContext.put(x.quantityOnHandDiff, BigDecimal.ONE);
                    serviceContext.put(x.userLogin, userLogin);
                    serviceResult = dispatcher.runSync(x.createInventoryItemDetail, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    serviceContext.clear();
                    serviceContext.put(x.userLogin, userLogin);
                    serviceContext.put(x.workEffortId, productionRunTaskId);
                    serviceContext.put(x.inventoryItemId, inventoryItemId);
                    serviceResult = dispatcher.runSync(x.createWorkEffortInventoryProduced, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    inventoryItemIds.add(inventoryItemId);
                    // Recompute reservations
                    serviceContext = new HashMap<>();
                    serviceContext.put(x.inventoryItemId, inventoryItemId);
                    serviceContext.put(x.userLogin, userLogin);
                    serviceResult = dispatcher.runSync(x.balanceInventoryItems, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
            } catch (GenericServiceException exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        } else {
            try {
                Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap(x.productId, productId,
                        x.inventoryItemTypeId, x.NON_SERIAL_INV_ITEM);
                serviceContext.put(x.facilityId, facilityId);
                serviceContext.put(x.datetimeReceived, UtilDateTime.nowTimestamp());
                serviceContext.put(x.datetimeManufactured, UtilDateTime.nowTimestamp());
                serviceContext.put(x.comments, x.Created_by_production_run_task + productionRunTaskId);
                if (unitCost != null) {
                    serviceContext.put(x.unitCost, unitCost);
                    serviceContext.put(x.currencyUomId, currencyUomId);
                }
                serviceContext.put(x.lotId, lotId);
                serviceContext.put(x.uomId, uomId);
                serviceContext.put(x.userLogin, userLogin);
                serviceContext.put(x.isReturned, isReturned);
                Map<String, Object> serviceResult = dispatcher.runSync(x.createInventoryItem, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                String inventoryItemId = (String) serviceResult.get(x.inventoryItemId);

                serviceContext.clear();
                serviceContext.put(x.inventoryItemId, inventoryItemId);
                serviceContext.put(x.workEffortId, productionRunTaskId);
                serviceContext.put(x.availableToPromiseDiff, quantity);
                serviceContext.put(x.quantityOnHandDiff, quantity);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.createInventoryItemDetail, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                serviceContext.clear();
                serviceContext.put(x.userLogin, userLogin);
                serviceContext.put(x.workEffortId, productionRunTaskId);
                serviceContext.put(x.inventoryItemId, inventoryItemId);
                serviceResult = dispatcher.runSync(x.createWorkEffortInventoryProduced, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                inventoryItemIds.add(inventoryItemId);
                // Recompute reservations
                serviceContext = new HashMap<>();
                serviceContext.put(x.inventoryItemId, inventoryItemId);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.balanceInventoryItems, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        }
        result.put(x.inventoryItemIds, inventoryItemIds);
        return result;
    }

    public static Map<String, Object> productionRunTaskReturnMaterial(DispatchContext ctx, ProductionRunServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String productionRunTaskId = (String) context.get(x.workEffortId);
        String productId = (String) context.get(x.productId);
        // Optional input fields
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        String lotId = (String) context.get(x.lotId);
        String uomId = (String) context.get(x.quantityUomId);
        Locale locale = (Locale) context.get(x.locale);
        if (quantity == null || quantity.compareTo(ZERO) == 0) {
            return ServiceUtil.returnSuccess();
        }
        // Verify how many items of the given productId
        // are currently assigned to this task.
        // If less than passed quantity then return an error message.
        try {
            BigDecimal totalIssued = BigDecimal.ZERO;
            for (GenericValue issuance : DaoRegistry.getDao(delegator, x.WorkEffortAndInventoryAssign, WorkEffortDao.class).findByAnd(delegator,
                    x.WorkEffortAndInventoryAssign, UtilMisc.toMap(x.workEffortId, productionRunTaskId, x.productId, productId), null, false)) {
                BigDecimal issued = issuance.getBigDecimal(x.quantity);
                if (issued != null) {
                    totalIssued = totalIssued.add(issued);
                }
            }
            BigDecimal totalReturned = BigDecimal.ZERO;
            for (GenericValue returned : DaoRegistry.getDao(delegator, x.WorkEffortAndInventoryProduced, WorkEffortDao.class).findByAnd(
                    delegator, x.WorkEffortAndInventoryProduced,
                    UtilMisc.toMap(x.workEffortId, productionRunTaskId, x.productId, productId), null, false)) {
                GenericValue returnDetail = DaoRegistry.getDao(delegator, x.InventoryItemDetail, InventoryItemDetailDao.class).findFirstByCondition(
                        delegator, x.InventoryItemDetail,
                        EntityCondition.makeCondition(x.inventoryItemId, EntityOperator.EQUALS, returned.get(x.inventoryItemId)),
                        null, UtilMisc.toList(x.inventoryItemDetailSeqId), false);
                if (returnDetail != null) {
                    BigDecimal qtyReturned = returnDetail.getBigDecimal(x.quantityOnHandDiff);
                    if (qtyReturned != null) {
                        totalReturned = totalReturned.add(qtyReturned);
                    }
                }
            }
            if (quantity.compareTo(totalIssued.subtract(totalReturned)) > 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskCannotReturnMoreItems,
                        UtilMisc.toMap(x.productionRunTaskId, productionRunTaskId, x.quantity, quantity, x.quantityAllocated,
                                totalIssued.subtract(totalReturned)), locale));
            }
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(gee.getMessage());
        }
        String inventoryItemTypeId = (String) context.get(x.inventoryItemTypeId);

        // TODO: if the task is not running, then return an error message.

        try {
            Map<String, Object> inventoryResult = dispatcher.runSync(x.productionRunTaskProduce,
                    UtilMisc.<String, Object>toMap(x.workEffortId, productionRunTaskId,
                            x.productId, productId, x.quantity, quantity, x.lotId, lotId, x.currencyUomId, uomId, x.isReturned, x.Y,
                            x.inventoryItemTypeId, inventoryItemTypeId, x.userLogin, userLogin));
            if (ServiceUtil.isError(inventoryResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ManufacturingProductionRunTaskProduceError + ServiceUtil.getErrorMessage(inventoryResult), locale));
            }
        } catch (GenericServiceException exc) {
            return ServiceUtil.returnError(exc.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateProductionRunTask(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String productionRunId = (String) context.get(x.productionRunId);
        String workEffortId = (String) context.get(x.productionRunTaskId);
        String partyId = (String) context.get(x.partyId);
        if (UtilValidate.isEmpty(partyId)) {
            partyId = userLogin.getString(x.partyId);
        }

        // Optional input fields
        BigDecimal addQuantityProduced = (BigDecimal) context.get(x.addQuantityProduced);
        BigDecimal addQuantityRejected = (BigDecimal) context.get(x.addQuantityRejected);
        BigDecimal addSetupTime = (BigDecimal) context.get(x.addSetupTime);
        BigDecimal addTaskTime = (BigDecimal) context.get(x.addTaskTime);
        String comments = (String) context.get(x.comments);
        Boolean issueRequiredComponents = (Boolean) context.get(x.issueRequiredComponents);
        Map<GenericPK, Object> componentsLocationMap = UtilGenerics.cast(context.get(x.componentsLocationMap));

        if (issueRequiredComponents == null) {
            issueRequiredComponents = Boolean.FALSE;
        }
        if (addQuantityProduced == null) {
            addQuantityProduced = BigDecimal.ZERO;
        }
        if (addQuantityRejected == null) {
            addQuantityRejected = BigDecimal.ZERO;
        }
        if (comments == null) {
            comments = x.emptyString;
        }

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotExists, locale));
        }
        List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
        GenericValue theTask = null;
        GenericValue oneTask = null;
        for (int i = 0; i < tasks.size(); i++) {
            oneTask = tasks.get(i);
            if (oneTask.getString(x.workEffortId).equals(workEffortId)) {
                theTask = oneTask;
                break;
            }
        }
        if (theTask == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskNotExists, locale));
        }

        String currentStatusId = theTask.getString(x.currentStatusId);

        if (!x.PRUN_RUNNING.equals(currentStatusId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTaskNotRunning, locale));
        }

        BigDecimal quantityProduced = theTask.getBigDecimal(x.quantityProduced);
        if (quantityProduced == null) {
            quantityProduced = BigDecimal.ZERO;
        }
        BigDecimal quantityRejected = theTask.getBigDecimal(x.quantityRejected);
        if (quantityRejected == null) {
            quantityRejected = BigDecimal.ZERO;
        }
        BigDecimal totalQuantityProduced = quantityProduced.add(addQuantityProduced);
        BigDecimal totalQuantityRejected = quantityRejected.add(addQuantityRejected);

        if (issueRequiredComponents && addQuantityProduced.compareTo(ZERO) > 0) {
            BigDecimal quantityToProduce = theTask.getBigDecimal(x.quantityToProduce);
            if (quantityToProduce == null) {
                quantityToProduce = BigDecimal.ZERO;
            }
            if (quantityToProduce.compareTo(ZERO) > 0) {
                try {
                    List<GenericValue> components = theTask.getRelated(x.WorkEffortGoodStandard, null, null, false);
                    for (GenericValue component : components) {
                        BigDecimal totalRequiredMaterialQuantity =
                                component.getBigDecimal(x.estimatedQuantity).multiply(totalQuantityProduced).divide(quantityToProduce, ROUNDING);
                        // now get the units that have been already issued and subtract them
                        List<GenericValue> issuances = DaoRegistry.getDao(delegator, x.WorkEffortAndInventoryAssign, WorkEffortDao.class).findByAnd(
                                delegator, x.WorkEffortAndInventoryAssign,
                                UtilMisc.toMap(x.workEffortId, workEffortId, x.productId, component.get(x.productId)), null, false);
                        BigDecimal totalIssued = BigDecimal.ZERO;
                        for (GenericValue issuance : issuances) {
                            BigDecimal issued = issuance.getBigDecimal(x.quantity);
                            if (issued != null) {
                                totalIssued = totalIssued.add(issued);
                            }
                        }
                        BigDecimal requiredQuantity = totalRequiredMaterialQuantity.subtract(totalIssued);
                        if (requiredQuantity.compareTo(ZERO) > 0) {
                            GenericPK key = component.getPrimaryKey();
                            Map<String, Object> componentsLocation = null;
                            if (componentsLocationMap != null) {
                                componentsLocation = UtilGenerics.cast(componentsLocationMap.get(key));
                            }
                            Map<String, Object> serviceContext = UtilMisc.toMap(x.workEffortId, workEffortId,
                                    x.productId, component.getString(x.productId),
                                    x.fromDate, component.getTimestamp(x.fromDate));
                            serviceContext.put(x.quantity, requiredQuantity);
                            if (componentsLocation != null) {
                                serviceContext.put(x.locationSeqId, componentsLocation.get(x.locationSeqId));
                                serviceContext.put(x.secondaryLocationSeqId, componentsLocation.get(x.secondaryLocationSeqId));
                                serviceContext.put(x.failIfItemsAreNotAvailable, componentsLocation.get(x.failIfItemsAreNotAvailable));
                            }
                            serviceContext.put(x.userLogin, userLogin);
                            Map<String, Object> serviceResult = dispatcher.runSync(x.issueProductionRunTaskComponent,
                                    serviceContext);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        }
                    }
                } catch (GenericEntityException | GenericServiceException e) {
                    String errMsg = x.Problem_calling_the_updateProductionRunTaskStatus_service;
                    Debug.logError(e, errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }

        // Create a new TimeEntry
        try {
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put(x.workEffortId, workEffortId);
            if (addTaskTime != null) {
                Double actualMilliSeconds = theTask.getDouble(x.actualMilliSeconds);
                if (actualMilliSeconds == null) {
                    actualMilliSeconds = (double) 0;
                }
                serviceContext.put(x.actualMilliSeconds, actualMilliSeconds + addTaskTime.doubleValue());
            }
            if (addSetupTime != null) {
                Double actualSetupMillis = theTask.getDouble(x.actualSetupMillis);
                if (actualSetupMillis == null) {
                    actualSetupMillis = (double) 0;
                }
                serviceContext.put(x.actualSetupMillis, actualSetupMillis + addSetupTime.doubleValue());
            }
            serviceContext.put(x.quantityProduced, totalQuantityProduced);
            serviceContext.put(x.quantityRejected, totalQuantityRejected);
            serviceContext.put(x.userLogin, userLogin);
            Map<String, Object> serviceResult = dispatcher.runSync(x.updateWorkEffort, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException exc) {
            return ServiceUtil.returnError(exc.getMessage());
        }

        return result;
    }

    public static Map<String, Object> approveRequirement(DispatchContext ctx, ProductionRunServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String requirementId = (String) context.get(x.requirementId);
        GenericValue requirement = null;
        try {
            requirement = DaoRegistry.getDao(delegator, x.Requirement, RequirementDao.class).findOne(delegator, x.Requirement,
                    UtilMisc.toMap(x.requirementId, requirementId), false);
        } catch (GenericEntityException e) {
            String errMsg = x.Problem_calling_the_approveRequirement_service;
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        if (requirement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingRequirementNotExists, locale));
        }
        try {
            Map<String, Object> serviceResult = dispatcher.runSync(x.updateRequirement,
                    UtilMisc.<String, Object>toMap(x.requirementId, requirementId,
                            x.statusId, x.REQ_APPROVED, x.requirementTypeId, requirement.getString(x.requirementTypeId),
                            x.userLogin, userLogin));
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingRequirementNotUpdated, locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createProductionRunFromRequirement(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String requirementId = (String) context.get(x.requirementId);
        // Optional input fields
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);

        GenericValue requirement = null;
        try {
            requirement = DaoRegistry.getDao(delegator, x.Requirement, RequirementDao.class).findOne(delegator, x.Requirement,
                    UtilMisc.toMap(x.requirementId, requirementId), false);
        } catch (GenericEntityException e) {
            String errMsg = x.Problem_calling_the_createProductionRunFromRequirement_service;
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }
        if (requirement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingRequirementNotExists, locale));
        }
        if (!x.INTERNAL_REQUIREMENT.equals(requirement.getString(x.requirementTypeId))) {
            return ServiceUtil.returnSuccess();
        }

        if (quantity == null) {
            quantity = requirement.getBigDecimal(x.quantity);
        }
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.clear();
        serviceContext.put(x.productId, requirement.getString(x.productId));
        serviceContext.put(x.pRQuantity, quantity);
        serviceContext.put(x.startDate, requirement.getTimestamp(x.requirementStartDate));
        serviceContext.put(x.facilityId, requirement.getString(x.facilityId));
        String workEffortName = null;
        if (requirement.getString(x.description) != null) {
            workEffortName = requirement.getString(x.description);
            if (workEffortName.length() > 50) {
                workEffortName = workEffortName.substring(0, 50);
            }
        } else {
            workEffortName = x.Created_from_requirement + requirement.getString(x.requirementId);
        }
        serviceContext.put(x.workEffortName, workEffortName);
        serviceContext.put(x.userLogin, userLogin);
        Map<String, Object> serviceResult = null;
        try {
            serviceResult = dispatcher.runSync(x.createProductionRun, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotCreated, locale)
                        + x.str_ceca32e9 + ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotCreated, locale)
                    + x.str_ceca32e9 + e.getMessage());
        }
        if (ServiceUtil.isError(serviceResult)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
        }

        String productionRunId = (String) serviceResult.get(x.productionRunId);
        result.put(x.productionRunId, productionRunId);

        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunCreated, UtilMisc.toMap(
                x.productionRunId, productionRunId), locale));
        return result;
    }

    public static Map<String, Object> createProductionRunFromConfiguration(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String facilityId = (String) context.get(x.facilityId);
        // Optional input fields
        String configId = (String) context.get(x.configId);
        ProductConfigWrapper config = (ProductConfigWrapper) context.get(x.config);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        String orderId = (String) context.get(x.orderId);
        String orderItemSeqId = (String) context.get(x.orderItemSeqId);

        if (config == null && configId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingConfigurationNotAvailable, locale));
        }
        if (config == null
                || config == null && configId != null) {
            // TODO: load the configuration
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunFromConfigurationNotYetImplemented,
                    locale));
        }
        if (!config.isCompleted()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunFromConfigurationNotValid, locale));
        }
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        String instanceProductId = null;
        try {
            instanceProductId = ProductWorker.getAggregatedInstanceId(delegator, config.getProduct().getString(x.productId), config.getConfigId());
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.clear();
        serviceContext.put(x.productId, instanceProductId);
        serviceContext.put(x.pRQuantity, quantity);
        serviceContext.put(x.startDate, UtilDateTime.nowTimestamp());
        serviceContext.put(x.facilityId, facilityId);
        serviceContext.put(x.userLogin, userLogin);
        Map<String, Object> serviceResult = null;
        try {
            serviceResult = dispatcher.runSync(x.createProductionRun, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotCreated, locale));
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotCreated, locale));
        }
        String productionRunId = (String) serviceResult.get(x.productionRunId);
        result.put(x.productionRunId, productionRunId);

        Map<String, BigDecimal> components = new HashMap<>();
        for (ConfigOption co : config.getSelectedOptions()) {
            for (GenericValue selComponent : co.getComponents()) {
                BigDecimal componentQuantity = null;
                if (selComponent.get(x.quantity) != null) {
                    componentQuantity = selComponent.getBigDecimal(x.quantity);
                }
                if (componentQuantity == null) {
                    componentQuantity = BigDecimal.ONE;
                }
                String componentProductId = selComponent.getString(x.productId);
                if (co.isVirtualComponent(selComponent)) {
                    Map<String, String> componentOptions = co.getComponentOptions();
                    if (UtilValidate.isNotEmpty(componentOptions) && UtilValidate.isNotEmpty(componentOptions.get(componentProductId))) {
                        componentProductId = componentOptions.get(componentProductId);
                    }
                }
                componentQuantity = quantity.multiply(componentQuantity);
                if (components.containsKey(componentProductId)) {
                    BigDecimal totalQuantity = components.get(componentProductId);
                    componentQuantity = totalQuantity.add(componentQuantity);
                }

                // check if a bom exists
                List<GenericValue> bomList = null;
                try {
                    bomList = DaoRegistry.getDao(delegator, x.ProductAssoc, ProductAssocDao.class).findByAnd(delegator, x.ProductAssoc,
                            UtilMisc.toMap(x.productId, componentProductId, x.productAssocTypeId, x.MANUF_COMPONENT), null, false);
                    bomList = EntityUtil.filterByDate(bomList);
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTryToGetBomListError, locale));
                }
                // if so create a mandatory predecessor to this production run
                if (UtilValidate.isNotEmpty(bomList)) {
                    serviceContext.clear();
                    serviceContext.put(x.productId, componentProductId);
                    serviceContext.put(x.quantity, componentQuantity);
                    serviceContext.put(x.startDate, UtilDateTime.nowTimestamp());
                    serviceContext.put(x.facilityId, facilityId);
                    serviceContext.put(x.userLogin, userLogin);
                    serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync(x.createProductionRunsForProductBom, serviceContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                        GenericValue workEffortPreDecessor = delegator.makeValue(x.WorkEffortAssoc, UtilMisc.toMap(
                                x.workEffortIdTo, productionRunId, x.workEffortIdFrom, serviceResult.get(x.productionRunId),
                                x.workEffortAssocTypeId, x.WORK_EFF_PRECEDENCY, x.fromDate, UtilDateTime.nowTimestamp()));
                        workEffortPreDecessor.create();
                    } catch (GenericServiceException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotCreated, locale));
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunTryToCreateWorkEffortAssoc,
                                locale));
                    }

                } else {
                    components.put(componentProductId, componentQuantity);
                }

                //  create production run notes from comments
                String comments = co.getComments();
                if (UtilValidate.isNotEmpty(comments)) {
                    serviceResult.clear();
                    serviceContext.clear();
                    serviceContext.put(x.workEffortId, productionRunId);
                    serviceContext.put(x.internalNote, x.Y);
                    serviceContext.put(x.noteInfo, comments);
                    serviceContext.put(x.noteName, co.getDescription());
                    serviceContext.put(x.userLogin, userLogin);
                    serviceContext.put(x.noteParty, userLogin.getString(x.partyId));
                    try {
                        serviceResult = dispatcher.runSync(x.createWorkEffortNote, serviceContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logWarning(e.getMessage(), MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        }

        for (Map.Entry<String, BigDecimal> component : components.entrySet()) {
            String productId = component.getKey();
            BigDecimal componentQuantity = component.getValue();
            if (componentQuantity == null) {
                componentQuantity = BigDecimal.ONE;
            }
            serviceResult = null;
            serviceContext = new HashMap<>();
            serviceContext.put(x.productionRunId, productionRunId);
            serviceContext.put(x.productId, productId);
            serviceContext.put(x.estimatedQuantity, componentQuantity);
            serviceContext.put(x.userLogin, userLogin);
            try {
                serviceResult = dispatcher.runSync(x.addProductionRunComponent, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotCreated, locale));
                }
            } catch (GenericServiceException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotCreated, locale));
            }
        }
        try {
            if (productionRunId != null && orderId != null && orderItemSeqId != null) {
                delegator.create(x.WorkOrderItemFulfillment, UtilMisc.toMap(x.workEffortId, productionRunId, x.orderId, orderId, x.orderItemSeqId,
                        orderItemSeqId));
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingRequirementNotDeleted, locale));
        }

        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunCreated, UtilMisc.toMap(
                x.productionRunId, productionRunId), locale));
        return result;
    }

    public static Map<String, Object> createProductionRunForMktgPkg(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String facilityId = (String) context.get(x.facilityId);
        String orderId = (String) context.get(x.orderId);
        String orderItemSeqId = (String) context.get(x.orderItemSeqId);

        // Check if the order is to be immediately fulfilled, in which case the inventory
        // hasn't been reserved and ATP not yet decreased
        boolean isImmediatelyFulfilled = false;
        try {
            GenericValue order = DaoRegistry.getDao(delegator, x.OrderHeader, OrderHeaderDao.class).findOne(delegator, x.OrderHeader,
                    UtilMisc.toMap(x.orderId, orderId), false);
            GenericValue productStore = delegator.getRelatedOne(x.ProductStore, order, false);
            isImmediatelyFulfilled = x.Y.equals(productStore.getString(x.isImmediatelyFulfilled));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunForMarketingPackagesCreationError,
                    UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItemSeqId, x.errorString, e.getMessage()), locale));
        }

        GenericValue orderItem = null;
        try {
            orderItem = DaoRegistry.getDao(delegator, x.OrderItem, OrderItemDao.class).findOne(delegator, x.OrderItem,
                    UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItemSeqId), false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunForMarketingPackagesCreationError,
                    UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItemSeqId, x.errorString, e.getMessage()), locale));
        }
        if (orderItem == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunForMarketingPackagesOrderItemNotFound,
                    UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItemSeqId), locale));
        }
        if (orderItem.get(x.quantity) == null) {
            Debug.logWarning(x.No_quantity_found_for_orderItem + orderItem + x.skipping_production_run_of_this_marketing_package, MODULE);
            return ServiceUtil.returnSuccess();
        }

        try {
            // first figure out how much of this product we already have in stock (ATP)
            BigDecimal existingAtp = BigDecimal.ZERO;
            Map<String, Object> tmpResults = dispatcher.runSync(x.getInventoryAvailableByFacility,
                    UtilMisc.<String, Object>toMap(x.productId, orderItem.getString(x.productId),
                            x.facilityId, facilityId, x.userLogin, userLogin));
            if (ServiceUtil.isError(tmpResults)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResults));
            }
            if (tmpResults.get(x.availableToPromiseTotal) != null) {
                existingAtp = (BigDecimal) tmpResults.get(x.availableToPromiseTotal);
            }
            // if the order is immediately fulfilled, adjust the atp to compensate for it not reserved
            if (isImmediatelyFulfilled) {
                existingAtp = existingAtp.subtract(orderItem.getBigDecimal(x.quantity));
            }

            if (Debug.verboseOn()) {
                Debug.logVerbose(x.Order_item + orderItem + x.Existing_ATP + existingAtp + x.str_4ff447b8, MODULE);
            }
            // we only need to produce more marketing packages if there isn't enough in stock.
            if (existingAtp.compareTo(ZERO) < 0) {
                // how many should we produce?  If there already is some inventory, then just produce enough to bring ATP back up to zero.
                BigDecimal qtyRequired = BigDecimal.ZERO.subtract(existingAtp);
                // ok so that's how many we WANT to produce, but let's check how many we can actually produce based on the available components
                Map<String, Object> serviceContext = new HashMap<>();
                serviceContext.put(x.productId, orderItem.getString(x.productId));
                serviceContext.put(x.facilityId, facilityId);
                serviceContext.put(x.userLogin, userLogin);
                Map<String, Object> serviceResult = dispatcher.runSync(x.getMktgPackagesAvailable, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                BigDecimal mktgPackagesAvailable = (BigDecimal) serviceResult.get(x.availableToPromiseTotal);

                BigDecimal qtyToProduce = qtyRequired.min(mktgPackagesAvailable);
                /*
                    Creating production run job for remaining quantity in created status
                    This will handle cases like if production run job creates for partial quantities
                     or in case of fully backordered scenario.
                 */
                BigDecimal remainingQty = orderItem.getBigDecimal(x.quantity).subtract(qtyToProduce);
                if (remainingQty.compareTo(ZERO) > 0) {
                    serviceContext.clear();
                    serviceContext.put(x.facilityId, facilityId);
                    serviceContext.put(x.userLogin, userLogin);
                    serviceContext.put(x.productId, orderItem.getString(x.productId));
                    serviceContext.put(x.pRQuantity, remainingQty);
                    serviceContext.put(x.startDate, UtilDateTime.nowTimestamp());
                    serviceResult = dispatcher.runSync(x.createProductionRun, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    String productionRunIdForRemainingQty = (String) serviceResult.get(x.productionRunId);
                    try {
                        delegator.create(x.WorkOrderItemFulfillment,
                                UtilMisc.toMap(x.workEffortId, productionRunIdForRemainingQty,
                                        x.orderId, orderId, x.orderItemSeqId, orderItemSeqId));
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                x.ManufacturingProductionRunForMarketingPackagesCreationError,
                                UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItemSeqId,
                                        x.errorString, e.getMessage()), locale));
                    }
                }

                if (qtyToProduce.compareTo(ZERO) > 0) {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose(x.Required_quantity_all_orders + qtyRequired + x.quantity_to_produce + qtyToProduce + x.str_4ff447b8,
                                MODULE);
                    }

                    serviceContext.put(x.pRQuantity, qtyToProduce);
                    serviceContext.put(x.startDate, UtilDateTime.nowTimestamp());
                    serviceResult = dispatcher.runSync(x.createProductionRun, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    String productionRunId = (String) serviceResult.get(x.productionRunId);
                    result.put(x.productionRunId, productionRunId);

                    try {
                        delegator.create(x.WorkOrderItemFulfillment, UtilMisc.toMap(x.workEffortId, productionRunId, x.orderId, orderId,
                                x.orderItemSeqId, orderItemSeqId));
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                x.ManufacturingProductionRunForMarketingPackagesCreationError, UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId,
                                        orderItemSeqId, x.errorString, e.getMessage()), locale));
                    }

                    try {
                        serviceContext.clear();
                        serviceContext.put(x.productionRunId, productionRunId);
                        serviceContext.put(x.statusId, x.PRUN_COMPLETED);
                        serviceContext.put(x.userLogin, userLogin);
                        serviceResult = dispatcher.runSync(x.quickChangeProductionRunStatus, serviceContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                        serviceContext.clear();
                        serviceContext.put(x.workEffortId, productionRunId);
                        serviceContext.put(x.userLogin, userLogin);
                        serviceResult = dispatcher.runSync(x.productionRunProduce, serviceContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } catch (GenericServiceException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotCreated, locale));
                    }

                    result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunCreated,
                            UtilMisc.toMap(x.productionRunId, productionRunId), locale));
                    return result;
                } else {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose(x.There_are_not_enough_components_available_to_produce_any_marketing_packages + orderItem.getString(
                                x.productId) + x.str_4ff447b8, MODULE);
                    }
                    return ServiceUtil.returnSuccess();
                }
            } else {
                if (Debug.verboseOn()) {
                    Debug.logVerbose(x.No_marketing_packages_need_to_be_produced_ATP_is + existingAtp + x.str_4ff447b8, MODULE);
                }
                return ServiceUtil.returnSuccess();
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotCreated, locale));
        }
    }

    public static Map<String, Object> createProductionRunsForOrder(DispatchContext dctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String orderId = (String) context.get(x.orderId);
        String shipmentId = (String) context.get(x.shipmentId);
        String orderItemSeqId = (String) context.get(x.orderItemSeqId);
        String shipGroupSeqId = (String) context.get(x.shipGroupSeqId);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        String fromDateStr = (String) context.get(x.fromDate);
        Locale locale = (Locale) context.get(x.locale);

        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }

        List<GenericValue> orderItems = null;

        if (orderItemSeqId != null) {
            try {
                GenericValue orderItem = null;
                if (UtilValidate.isNotEmpty(shipGroupSeqId)) {
                    orderItem = DaoRegistry.getDao(delegator, x.OrderItemShipGroupAssoc, OrderItemShipGroupAssocDao.class).findOne(delegator,
                            x.OrderItemShipGroupAssoc,
                            UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItemSeqId, x.shipGroupSeqId, shipGroupSeqId), false);
                } else {
                    orderItem = DaoRegistry.getDao(delegator, x.OrderItem, OrderItemDao.class).findOne(delegator, x.OrderItem,
                            UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItemSeqId), false);
                }
                if (orderItem == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER, x.OrderErrorOrderItemNotFound, UtilMisc.toMap(x.orderId,
                            orderId, x.orderItemSeqId, x.emptyString), locale));
                }
                if (quantity != null) {
                    orderItem.set(x.quantity, quantity);
                }
                orderItems = UtilMisc.toList(orderItem);
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER, x.OrderProblemsReadingOrderItemInformation, UtilMisc.toMap(
                        x.errorString, gee.getMessage()), locale));
            }
        } else {
            try {
                orderItems = DaoRegistry.getDao(delegator, x.OrderItem, OrderItemDao.class).findByAnd(delegator, x.OrderItem,
                        UtilMisc.toMap(x.orderId, orderId), null, false);
                if (orderItems == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER, x.OrderErrorOrderItemNotFound, UtilMisc.toMap(x.orderId,
                            orderId, x.orderItemSeqId, x.emptyString), locale));
                }
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER, x.OrderProblemsReadingOrderItemInformation, UtilMisc.toMap(
                        x.errorString, gee.getMessage()), locale));
            }
        }
        List<String> productionRuns = new LinkedList<>();
        for (int i = 0; i < orderItems.size(); i++) {
            GenericValue orderItemOrShipGroupAssoc = orderItems.get(i);
            String productId = null;
            BigDecimal amount = null;
            GenericValue orderItem = null;
            if (x.OrderItemShipGroupAssoc.equals(orderItemOrShipGroupAssoc.getEntityName())) {
                try {
                    orderItem = orderItemOrShipGroupAssoc.getRelatedOne(x.OrderItem, false);
                } catch (GenericEntityException gee) {
                    Debug.logInfo(x.Unable_to_find_order_item_for + orderItemOrShipGroupAssoc, MODULE);
                }
            } else {
                orderItem = orderItemOrShipGroupAssoc;
            }
            if (orderItem == null || orderItem.get(x.productId) == null) {
                continue;
            } else {
                productId = orderItem.getString(x.productId);
            }
            if (orderItem.get(x.selectedAmount) != null) {
                amount = orderItem.getBigDecimal(x.selectedAmount);
            }
            if (amount == null) {
                amount = BigDecimal.ZERO;
            }
            if (orderItemOrShipGroupAssoc.get(x.quantity) != null) {
                quantity = orderItemOrShipGroupAssoc.getBigDecimal(x.quantity);
            } else {
                continue;
            }
            try {
                List<GenericValue> existingProductionRuns = null;
                if (UtilValidate.isNotEmpty(shipGroupSeqId)) {
                    existingProductionRuns = DaoRegistry.getDao(delegator, x.WorkAndOrderItemFulfillment, WorkEffortDao.class).findByCondition(
                            delegator, x.WorkAndOrderItemFulfillment,
                            EntityCondition.makeCondition(
                                    EntityCondition.makeCondition(x.orderId, EntityOperator.EQUALS, orderItemOrShipGroupAssoc.get(x.orderId)),
                                    EntityCondition.makeCondition(x.orderItemSeqId, EntityOperator.EQUALS,
                                            orderItemOrShipGroupAssoc.get(x.orderItemSeqId)),
                                    EntityCondition.makeCondition(x.shipGroupSeqId, EntityOperator.EQUALS,
                                            orderItemOrShipGroupAssoc.get(x.shipGroupSeqId)),
                                    EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.PRUN_CANCELLED)),
                            null, null, null, true);
                } else {
                    existingProductionRuns = DaoRegistry.getDao(delegator, x.WorkAndOrderItemFulfillment, WorkEffortDao.class).findByCondition(
                            delegator, x.WorkAndOrderItemFulfillment,
                            EntityCondition.makeCondition(
                                    EntityCondition.makeCondition(x.orderId, EntityOperator.EQUALS, orderItemOrShipGroupAssoc.get(x.orderId)),
                                    EntityCondition.makeCondition(x.orderItemSeqId, EntityOperator.EQUALS,
                                            orderItemOrShipGroupAssoc.get(x.orderItemSeqId)),
                                    EntityCondition.makeCondition(x.currentStatusId, EntityOperator.NOT_EQUAL, x.PRUN_CANCELLED)),
                            null, null, null, true);
                }
                if (UtilValidate.isNotEmpty(existingProductionRuns)) {
                    Debug.logWarning(x.Production_Run_for_order_item + orderItemOrShipGroupAssoc.getString(x.orderId) + x.str_42099b4a
                            + orderItemOrShipGroupAssoc.getString(x.orderItemSeqId) + x.and_ship_group + shipGroupSeqId + x.already_exists,
                            MODULE);
                    continue;
                }
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturinWorkOrderItemFulfillmentError, UtilMisc.toMap(
                        x.errorString, gee.getMessage()), locale));
            }
            try {
                List<BOMNode> components = new LinkedList<>();
                BOMTree tree = new BOMTree(productId, x.MANUF_COMPONENT, fromDate, BOMTree.EXPLOSION_MANUFACTURING, delegator, dispatcher, userLogin);
                tree.setRootQuantity(quantity);
                tree.setRootAmount(amount);
                tree.print(components);
                productionRuns.add(tree.createManufacturingOrders(null, fromDate, null, null, null, orderId, orderItem.getString(x.orderItemSeqId),
                        shipGroupSeqId, shipmentId, userLogin));
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorCreatingBillOfMaterialsTree,
                        UtilMisc.toMap(x.errorString, gee.getMessage()), locale));
            }
        }
        result.put(x.productionRuns, productionRuns);
        return result;
    }

    public static Map<String, Object> createProductionRunsForProductBom(DispatchContext dctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        String productId = (String) context.get(x.productId);
        Timestamp startDate = (Timestamp) context.get(x.startDate);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        String facilityId = (String) context.get(x.facilityId);
        String workEffortName = (String) context.get(x.workEffortName);
        String description = (String) context.get(x.description);
        String routingId = (String) context.get(x.routingId);
        String workEffortId = null;
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        try {
            List<BOMNode> components = new LinkedList<>();
            BOMTree tree = new BOMTree(productId, x.MANUF_COMPONENT, startDate, BOMTree.EXPLOSION_MANUFACTURING, delegator, dispatcher, userLogin);
            tree.setRootQuantity(quantity);
            tree.setRootAmount(BigDecimal.ZERO);
            tree.print(components);
            workEffortId = tree.createManufacturingOrders(facilityId, startDate, workEffortName, description, routingId, null, null, null, null,
                    userLogin);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingBomErrorCreatingBillOfMaterialsTree, UtilMisc.toMap(
                    x.errorString, gee.getMessage()), locale));
        }
        if (workEffortId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunIsNotRequiredForProductId,
                    UtilMisc.toMap(x.productId, productId, x.startDate, startDate), locale));
        }
        List<String> productionRuns = new LinkedList<>();
        result.put(x.productionRuns, productionRuns);
        result.put(x.productionRunId, workEffortId);
        return result;
    }

    /**
     * Quick runs a ProductionRun task to the completed status, also issuing components
     * if necessary.
     * @param ctx     The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> quickRunProductionRunTask(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String productionRunId = (String) context.get(x.productionRunId);
        String taskId = (String) context.get(x.taskId);

        try {
            Map<String, Object> serviceContext = new HashMap<>();
            Map<String, Object> serviceResult = null;
            GenericValue task = DaoRegistry.getDao(delegator, x.WorkEffort, WorkEffortDao.class).findOne(delegator, x.WorkEffort,
                    UtilMisc.toMap(x.workEffortId, taskId), false);
            String currentStatusId = task.getString(x.currentStatusId);
            String prevStatusId = x.emptyString;
            while (!x.PRUN_COMPLETED.equals(currentStatusId)) {
                serviceContext.put(x.productionRunId, productionRunId);
                serviceContext.put(x.workEffortId, taskId);
                serviceContext.put(x.issueAllComponents, Boolean.TRUE);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.changeProductionRunTaskStatus, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                currentStatusId = (String) serviceResult.get(x.newStatusId);
                if (currentStatusId.equals(prevStatusId)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunUnableToProgressTaskStatus,
                            UtilMisc.toMap(x.prevStatusId, prevStatusId, x.taskId, taskId), locale));
                } else {
                    prevStatusId = currentStatusId;
                }
                serviceContext.clear();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problem_accessing_the_WorkEffort_entity, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_changeProductionRunTaskStatus_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
        }
        return result;
    }

    /**
     * Quick runs all the tasks of a ProductionRun to the completed status,
     * also issuing components if necessary.
     * @param ctx     The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> quickRunAllProductionRunTasks(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String productionRunId = (String) context.get(x.productionRunId);

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotExists, locale));
        }
        List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
        GenericValue oneTask = null;
        String taskId = null;
        for (int i = 0; i < tasks.size(); i++) {
            oneTask = tasks.get(i);
            taskId = oneTask.getString(x.workEffortId);
            try {
                Map<String, Object> serviceContext = new HashMap<>();
                serviceContext.put(x.productionRunId, productionRunId);
                serviceContext.put(x.taskId, taskId);
                serviceContext.put(x.userLogin, userLogin);
                Map<String, Object> serviceResult = dispatcher.runSync(x.quickRunProductionRunTask, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, x.Problem_calling_the_quickRunProductionRunTask_service, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
            }
        }
        return result;
    }

    public static Map<String, Object> quickStartAllProductionRunTasks(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String productionRunId = (String) context.get(x.productionRunId);

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunNotExists, locale));
        }
        List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
        GenericValue oneTask = null;
        String taskId = null;
        for (int i = 0; i < tasks.size(); i++) {
            oneTask = tasks.get(i);
            taskId = oneTask.getString(x.workEffortId);
            if (x.PRUN_CREATED.equals(oneTask.getString(x.currentStatusId))
                    || x.PRUN_SCHEDULED.equals(oneTask.getString(x.currentStatusId))
                    || x.PRUN_DOC_PRINTED.equals(oneTask.getString(x.currentStatusId))) {
                try {
                    Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap(x.productionRunId, productionRunId,
                            x.workEffortId, taskId);
                    serviceContext.put(x.statusId, x.PRUN_RUNNING);
                    serviceContext.put(x.issueAllComponents, Boolean.FALSE);
                    serviceContext.put(x.userLogin, userLogin);
                    Map<String, Object> serviceResult = dispatcher.runSync(x.changeProductionRunTaskStatus, serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, x.Problem_calling_the_changeProductionRunTaskStatus_service, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
                }
            }
        }
        return result;
    }

    /**
     * Quick moves a ProductionRun to the passed in status, performing all
     * the needed tasks in the way.
     * @param ctx     The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> quickChangeProductionRunStatus(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Map<String, Object> serviceResult = new HashMap<>();
        String productionRunId = (String) context.get(x.productionRunId);
        String statusId = (String) context.get(x.statusId);
        String startAllTasks = (String) context.get(x.startAllTasks);

        try {
            Map<String, Object> serviceContext = new HashMap<>();
            // Change the task status to running
            if (x.PRUN_DOC_PRINTED.equals(statusId)
                    || x.PRUN_RUNNING.equals(statusId)
                    || x.PRUN_COMPLETED.equals(statusId)
                    || x.PRUN_CLOSED.equals(statusId)) {
                serviceContext.put(x.productionRunId, productionRunId);
                serviceContext.put(x.statusId, x.PRUN_DOC_PRINTED);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.changeProductionRunStatus, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
            if (x.PRUN_RUNNING.equals(statusId) && x.Y.equals(startAllTasks)) {
                serviceContext.clear();
                serviceContext.put(x.productionRunId, productionRunId);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.quickStartAllProductionRunTasks, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
            if (x.PRUN_COMPLETED.equals(statusId) || x.PRUN_CLOSED.equals(statusId)) {
                serviceContext.clear();
                serviceContext.put(x.productionRunId, productionRunId);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.quickRunAllProductionRunTasks, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
            if (x.PRUN_CLOSED.equals(statusId)) {
                // Put in warehouse the products manufactured
                serviceContext.clear();
                serviceContext.put(x.workEffortId, productionRunId);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.productionRunProduce, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                serviceContext.clear();
                serviceContext.put(x.productionRunId, productionRunId);
                serviceContext.put(x.statusId, x.PRUN_CLOSED);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.changeProductionRunStatus, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } else {
                serviceContext.put(x.productionRunId, productionRunId);
                serviceContext.put(x.statusId, statusId);
                serviceContext.put(x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.changeProductionRunStatus, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_changeProductionRunStatus_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunStatusNotChanged, locale));
        }
        return result;
    }

    /**
     * Given a productId and an optional date, returns the total qty
     * of productId reserved by production runs.
     * @param ctx     The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getProductionRunTotResQty(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);

        String productId = (String) context.get(x.productId);
        Timestamp startDate = (Timestamp) context.get(x.startDate);
        if (startDate == null) {
            startDate = UtilDateTime.nowTimestamp();
        }
        BigDecimal totQty = BigDecimal.ZERO;
        try {
            List<EntityCondition> findOutgoingProductionRunsConds = new LinkedList<>();

            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(x.productId, EntityOperator.EQUALS, productId));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(x.statusId, EntityOperator.EQUALS, x.WEGS_CREATED));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(x.estimatedStartDate, EntityOperator.LESS_THAN_EQUAL_TO, startDate));

            List<EntityCondition> findOutgoingProductionRunsStatusConds = new LinkedList<>();
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_CREATED));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_SCHEDULED));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_DOC_PRINTED));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition(x.currentStatusId, EntityOperator.EQUALS, x.PRUN_RUNNING));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(findOutgoingProductionRunsStatusConds, EntityOperator.OR));

            List<GenericValue> outgoingProductionRuns = DaoRegistry.getDao(delegator, x.WorkEffortAndGoods, WorkEffortDao.class).findByCondition(
                    delegator, x.WorkEffortAndGoods, EntityCondition.makeCondition(findOutgoingProductionRunsConds), null,
                    UtilMisc.toList(x.estimatedStartDate_2c48ab38), null, false);
            if (outgoingProductionRuns != null) {
                for (int i = 0; i < outgoingProductionRuns.size(); i++) {
                    GenericValue outgoingProductionRun = outgoingProductionRuns.get(i);
                    BigDecimal qty = outgoingProductionRun.getBigDecimal(x.estimatedQuantity);
                    qty = qty != null ? qty : BigDecimal.ZERO;
                    totQty = totQty.add(qty);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problem_calling_the_getProductionRunTotResQty_service, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionResQtyCalc, locale));
        }
        result.put(x.reservedQuantity, totQty);
        return result;
    }

    public static Map<String, Object> checkDecomposeInventoryItem(DispatchContext ctx, ProductionRunServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String inventoryItemId = (String) context.get(x.inventoryItemId);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> serviceResult = new HashMap<>();
        try {
            GenericValue inventoryItem = DaoRegistry.getDao(delegator, x.InventoryItem, InventoryItemDao.class).findOne(delegator, x.InventoryItem,
                    UtilMisc.toMap(x.inventoryItemId, inventoryItemId), false);
            if (inventoryItem == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_PRODUCT, x.ProductInventoryItemNotFound, UtilMisc.toMap(
                        x.inventoryItemId, inventoryItemId), locale));
            }
            if (inventoryItem.get(x.availableToPromiseTotal) != null && inventoryItem.getBigDecimal(x.availableToPromiseTotal).compareTo(ZERO) <= 0) {
                return ServiceUtil.returnSuccess();
            }
            GenericValue product = inventoryItem.getRelatedOne(x.Product, false);
            if (product == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_PRODUCT, x.ProductProductNotFound, locale) + x.str_b858cb28 + inventoryItem.get(
                        x.productId));
            }
            if (EntityTypeUtil.hasParentType(delegator, x.ProductType, x.productTypeId, product.getString(x.productTypeId), x.parentTypeId,
                    x.MARKETING_PKG_AUTO)) {
                Map<String, Object> serviceContext = UtilMisc.toMap(x.inventoryItemId, inventoryItemId,
                        x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.decomposeInventoryItem, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Problem_accessing_the_InventoryItem_entity, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_checkDecomposeInventoryItem_service, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> decomposeInventoryItem(DispatchContext ctx, ProductionRunServicesContext context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Timestamp now = UtilDateTime.nowTimestamp();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        // Mandatory input fields
        String inventoryItemId = (String) context.get(x.inventoryItemId);
        BigDecimal quantity = (BigDecimal) context.get(x.quantity);
        List<String> inventoryItemIds = new LinkedList<>();
        try {
            GenericValue inventoryItem = DaoRegistry.getDao(delegator, x.InventoryItem, InventoryItemDao.class).findOne(delegator, x.InventoryItem,
                    UtilMisc.toMap(x.inventoryItemId, inventoryItemId), false);
            if (inventoryItem == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunCannotDecomposingInventoryItem,
                        UtilMisc.toMap(x.inventoryItemId, inventoryItemId), locale));
            }
            // the work effort (disassemble order) is created
            Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap(x.workEffortTypeId, x.TASK,
                    x.workEffortPurposeTypeId, x.WEPT_PRODUCTION_RUN,
                    x.currentStatusId, x.CAL_COMPLETED);
            serviceContext.put(x.workEffortName,
                    x.Decomposing_product + inventoryItem.getString(x.productId) + x.inventory_item + inventoryItem.getString(x.inventoryItemId)
                            + x.str_4ff447b8);
            serviceContext.put(x.facilityId, inventoryItem.getString(x.facilityId));
            serviceContext.put(x.estimatedStartDate, now);
            serviceContext.put(x.userLogin, userLogin);
            Map<String, Object> serviceResult = dispatcher.runSync(x.createWorkEffort, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            String workEffortId = (String) serviceResult.get(x.workEffortId);
            // the inventory (marketing package) is issued
            serviceContext.clear();
            serviceContext = UtilMisc.toMap(x.inventoryItem, inventoryItem,
                    x.workEffortId, workEffortId, x.userLogin, userLogin);
            if (quantity != null) {
                serviceContext.put(x.quantity, quantity);
            }
            serviceResult = dispatcher.runSync(x.issueInventoryItemToWorkEffort, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            BigDecimal issuedQuantity = (BigDecimal) serviceResult.get(x.quantityIssued);
            if (issuedQuantity.compareTo(ZERO) == 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ManufacturingProductionRunCannotDecomposingInventoryItemNoMarketingPackagesFound, UtilMisc.toMap(x.inventoryItemId,
                                inventoryItem.getString(x.inventoryItemId)), locale));
            }
            // get the package's unit cost to compute a cost coefficient ratio which is the marketing package's actual unit cost divided by its
            // standard cost
            // this ratio will be used to determine the cost of the marketing package components when they are returned to inventory
            serviceContext.clear();
            serviceContext = UtilMisc.toMap(x.productId, inventoryItem.getString(x.productId),
                    x.currencyUomId, inventoryItem.getString(x.currencyUomId),
                    x.costComponentTypePrefix, x.EST_STD,
                    x.userLogin, userLogin);
            serviceResult = dispatcher.runSync(x.getProductCost, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            BigDecimal packageCost = (BigDecimal) serviceResult.get(x.productCost);
            BigDecimal inventoryItemCost = inventoryItem.getBigDecimal(x.unitCost);
            BigDecimal costCoefficient = null;
            if (packageCost == null || packageCost.compareTo(ZERO) == 0 || inventoryItemCost == null) {
                // if the actual cost of the item (marketing package) that we are decomposing is not available, or
                // if the standard cost of the marketing package is not available then
                // the cost coefficient ratio is set to 1.0:
                // this means that the unit costs of the inventory items of the components
                // will be equal to the components' standard costs
                costCoefficient = BigDecimal.ONE;
            } else {
                costCoefficient = inventoryItemCost.divide(packageCost, 10, ROUNDING);
            }

            // the components are retrieved
            serviceContext.clear();
            serviceContext = UtilMisc.toMap(x.productId, inventoryItem.getString(x.productId),
                    x.quantity, issuedQuantity,
                    x.userLogin, userLogin);
            serviceResult = dispatcher.runSync(x.getManufacturingComponents, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            List<Map<String, Object>> components = UtilGenerics.cast(serviceResult.get(x.componentsMap));
            if (UtilValidate.isEmpty(components)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ManufacturingProductionRunCannotDecomposingInventoryItemNoComponentsFound, UtilMisc.toMap(x.productId,
                                inventoryItem.getString(x.productId)), locale));
            }
            for (Map<String, Object> component : components) {
                // get the component's standard cost
                serviceContext.clear();
                serviceContext = UtilMisc.toMap(x.productId, ((GenericValue) component.get(x.product)).getString(x.productId),
                        x.currencyUomId, inventoryItem.getString(x.currencyUomId),
                        x.costComponentTypePrefix, x.EST_STD,
                        x.userLogin, userLogin);
                serviceResult = dispatcher.runSync(x.getProductCost, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                BigDecimal componentCost = (BigDecimal) serviceResult.get(x.productCost);

                // return the component to inventory at its standard cost multiplied by the cost coefficient from above
                BigDecimal componentInventoryItemCost = costCoefficient.multiply(componentCost);
                serviceContext.clear();
                serviceContext = UtilMisc.toMap(x.productId, ((GenericValue) component.get(x.product)).getString(x.productId),
                        x.quantity, component.get(x.quantity),
                        x.facilityId, inventoryItem.getString(x.facilityId),
                        x.unitCost, componentInventoryItemCost,
                        x.userLogin, userLogin);
                serviceContext.put(x.workEffortId, workEffortId);
                serviceResult = dispatcher.runSync(x.productionRunTaskProduce, serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                List<String> newInventoryItemIds = UtilGenerics.cast(serviceResult.get(x.inventoryItemIds));
                inventoryItemIds.addAll(newInventoryItemIds);
            }
            // the components are put in warehouse
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, x.Problem_calling_the_createWorkEffort_service, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        result.put(x.inventoryItemIds, inventoryItemIds);
        return result;
    }

    public static Map<String, Object> setEstimatedDeliveryDates(DispatchContext ctx, ProductionRunServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get(x.locale);
        Map<String, TreeMap<Timestamp, Object>> products = new HashMap<>();

        try {
            List<GenericValue> resultList = DaoRegistry.getDao(delegator, x.WorkEffortAndGoods, WorkEffortDao.class).findByAnd(delegator,
                    x.WorkEffortAndGoods,
                    UtilMisc.toMap(x.workEffortGoodStdTypeId, x.PRUN_PROD_DELIV, x.statusId, x.WEGS_CREATED, x.workEffortTypeId,
                            x.PROD_ORDER_HEADER),
                    null, false);
            for (GenericValue genericResult : resultList) {
                if (x.PRUN_CLOSED.equals(genericResult.getString(x.currentStatusId))
                        || x.PRUN_CREATED.equals(genericResult.getString(x.currentStatusId))) {
                    continue;
                }
                BigDecimal qtyToProduce = genericResult.getBigDecimal(x.quantityToProduce);
                if (qtyToProduce == null) {
                    qtyToProduce = BigDecimal.ZERO;
                }
                BigDecimal qtyProduced = genericResult.getBigDecimal(x.quantityProduced);
                if (qtyProduced == null) {
                    qtyProduced = BigDecimal.ZERO;
                }
                if (qtyProduced.compareTo(qtyToProduce) >= 0) {
                    continue;
                }
                BigDecimal qtyDiff = qtyToProduce.subtract(qtyProduced);
                String productId = genericResult.getString(x.productId);
                Timestamp estimatedShipDate = genericResult.getTimestamp(x.estimatedCompletionDate);
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }
                if (!products.containsKey(productId)) {
                    products.put(productId, new TreeMap<>());
                }
                TreeMap<Timestamp, Object> productMap = products.get(productId);
                if (!productMap.containsKey(estimatedShipDate)) {
                    productMap.put(estimatedShipDate,
                            UtilMisc.toMap(x.remainingQty, BigDecimal.ZERO, x.reservations, new LinkedList<>()));
                }
                Map<String, Object> dateMap = UtilGenerics.cast(productMap.get(estimatedShipDate));
                BigDecimal remainingQty = (BigDecimal) dateMap.get(x.remainingQty);
                remainingQty = remainingQty.add(qtyDiff);
                dateMap.put(x.remainingQty, remainingQty);
            }

            // Approved purchase orders
            resultList = DaoRegistry.getDao(delegator, x.OrderHeaderAndItems, OrderHeaderDao.class).findByAnd(delegator, x.OrderHeaderAndItems,
                    UtilMisc.toMap(x.orderTypeId, x.PURCHASE_ORDER, x.itemStatusId, x.ITEM_APPROVED), UtilMisc.toList(x.orderId), false);
            String orderId = null;
            GenericValue orderDeliverySchedule = null;
            for (GenericValue genericResult : resultList) {
                String newOrderId = genericResult.getString(x.orderId);
                if (!newOrderId.equals(orderId)) {
                    orderDeliverySchedule = null;
                    orderId = newOrderId;
                    orderDeliverySchedule = DaoRegistry.getDao(delegator, x.OrderDeliverySchedule, OrderDeliveryScheduleDao.class).findOne(
                            delegator, x.OrderDeliverySchedule, UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, x.NA), false);
                }
                String productId = genericResult.getString(x.productId);
                BigDecimal orderQuantity = genericResult.getBigDecimal(x.quantity);
                GenericValue orderItemDeliverySchedule = null;
                orderItemDeliverySchedule = DaoRegistry.getDao(delegator, x.OrderDeliverySchedule, OrderDeliveryScheduleDao.class).findOne(
                        delegator, x.OrderDeliverySchedule,
                        UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, genericResult.getString(x.orderItemSeqId)), false);
                Timestamp estimatedShipDate = null;
                if (orderItemDeliverySchedule != null && orderItemDeliverySchedule.get(x.estimatedReadyDate) != null) {
                    estimatedShipDate = orderItemDeliverySchedule.getTimestamp(x.estimatedReadyDate);
                } else if (orderDeliverySchedule != null && orderDeliverySchedule.get(x.estimatedReadyDate) != null) {
                    estimatedShipDate = orderDeliverySchedule.getTimestamp(x.estimatedReadyDate);
                } else {
                    estimatedShipDate = genericResult.getTimestamp(x.estimatedDeliveryDate);
                }
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }
                if (!products.containsKey(productId)) {
                    products.put(productId, new TreeMap<>());
                }
                TreeMap<Timestamp, Object> productMap = products.get(productId);
                if (!productMap.containsKey(estimatedShipDate)) {
                    productMap.put(estimatedShipDate,
                            UtilMisc.toMap(x.remainingQty, BigDecimal.ZERO, x.reservations, new LinkedList<>()));
                }
                Map<String, Object> dateMap = UtilGenerics.cast(productMap.get(estimatedShipDate));
                BigDecimal remainingQty = (BigDecimal) dateMap.get(x.remainingQty);
                remainingQty = remainingQty.add(orderQuantity);
                dateMap.put(x.remainingQty, remainingQty);
            }

            // backorders
            List<EntityCondition> backordersCondList = new LinkedList<>();
            backordersCondList.add(EntityCondition.makeCondition(x.quantityNotAvailable, EntityOperator.NOT_EQUAL, null));
            backordersCondList.add(EntityCondition.makeCondition(x.quantityNotAvailable, EntityOperator.GREATER_THAN, BigDecimal.ZERO));

            List<GenericValue> backorders = DaoRegistry.getDao(delegator, x.OrderItemAndShipGrpInvResAndItem, OrderItemDao.class).findByCondition(
                    delegator, x.OrderItemAndShipGrpInvResAndItem,
                    EntityCondition.makeCondition(
                            EntityCondition.makeCondition(x.quantityNotAvailable, EntityOperator.NOT_EQUAL, null),
                            EntityCondition.makeCondition(x.quantityNotAvailable, EntityOperator.GREATER_THAN, BigDecimal.ZERO)),
                    null, UtilMisc.toList(x.shipBeforeDate), null, false);
            for (GenericValue genericResult : backorders) {
                String productId = genericResult.getString(x.productId);
                GenericValue orderItemShipGroup = DaoRegistry.getDao(delegator, x.OrderItemShipGroup, OrderItemShipGroupDao.class).findOne(
                        delegator, x.OrderItemShipGroup,
                        UtilMisc.toMap(x.orderId, genericResult.get(x.orderId), x.shipGroupSeqId, genericResult.get(x.shipGroupSeqId)), false);
                Timestamp requiredByDate = orderItemShipGroup.getTimestamp(x.shipByDate);

                BigDecimal quantityNotAvailable = genericResult.getBigDecimal(x.quantityNotAvailable);
                BigDecimal quantityNotAvailableRem = quantityNotAvailable;
                if (requiredByDate == null) {
                    // If shipByDate is not set, 'now' is assumed.
                    requiredByDate = now;
                }
                if (!products.containsKey(productId)) {
                    continue;
                }
                TreeMap<Timestamp, Object> productMap = products.get(productId);
                SortedMap<Timestamp, Object> subsetMap = productMap.headMap(requiredByDate);
                // iterate and 'reserve'
                for (Timestamp currentDate : subsetMap.keySet()) {
                    Map<String, Object> currentDateMap = UtilGenerics.cast(subsetMap.get(currentDate));
                    BigDecimal remainingQty = (BigDecimal) currentDateMap.get(x.remainingQty);
                    if (remainingQty.compareTo(ZERO) == 0) {
                        continue;
                    }
                    if (remainingQty.compareTo(quantityNotAvailableRem) >= 0) {
                        remainingQty = remainingQty.subtract(quantityNotAvailableRem);
                        currentDateMap.put(x.remainingQty, remainingQty);
                        GenericValue orderItemShipGrpInvRes = DaoRegistry.getDao(delegator, x.OrderItemShipGrpInvRes,
                                OrderItemShipGrpInvResDao.class).findOne(delegator, x.OrderItemShipGrpInvRes,
                                        UtilMisc.toMap(x.orderId, genericResult.get(x.orderId), x.shipGroupSeqId,
                                                genericResult.get(x.shipGroupSeqId), x.orderItemSeqId,
                                                genericResult.get(x.orderItemSeqId), x.inventoryItemId,
                                                genericResult.get(x.inventoryItemId)),
                                        false);
                        orderItemShipGrpInvRes.set(x.promisedDatetime, currentDate);
                        orderItemShipGrpInvRes.store();
                        // TODO: set the reservation
                        break;
                    } else {
                        quantityNotAvailableRem = quantityNotAvailableRem.subtract(remainingQty);
                        remainingQty = BigDecimal.ZERO;
                        currentDateMap.put(x.remainingQty, remainingQty);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Error, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ManufacturingProductionRunErrorRunningSetEstimatedDeliveryDates,
                    locale));
        }
        return ServiceUtil.returnSuccess();
    }
}
