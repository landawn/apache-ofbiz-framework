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
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.manufacturing.bom.BOMNode;
import org.apache.ofbiz.manufacturing.bom.BOMTree;
import org.apache.ofbiz.manufacturing.techdata.TechDataServices;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.product.config.ProductConfigWrapper.ConfigOption;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import org.apache.ofbiz.persistence.entity.x;
/**
 * Services for Production Run maintenance
 */
public class ProductionRunServices {

    private static final String MODULE = ProductionRunServices.class.getName();
    private static final String RESOURCE = "ManufacturingUiLabels";
    private static final String RES_ORDER = "OrderErrorUiLabels";
    private static final String RES_PRODUCT = "ProductUiLabels";

    private static final int DECIMALS = UtilNumber.getBigDecimalScale("order.decimals");
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode("finaccount.rounding");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(DECIMALS, ROUNDING);

    /**
     * Cancels a ProductionRun.
     * @param ctx     The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> cancelProductionRun(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Map<String, Object> serviceResult = new HashMap<>();
        String productionRunId = (String) context.get(x.productionRunId);

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotExists", locale));
        }
        String currentStatusId = productionRun.getGenericValue().getString("currentStatusId");

        // PRUN_CREATED, PRUN_DOC_PRINTED --> PRUN_CANCELLED
        if ("PRUN_CREATED".equals(currentStatusId) || "PRUN_DOC_PRINTED".equals(currentStatusId) || "PRUN_SCHEDULED".equals(currentStatusId)) {
            try {
                // First of all, make sure that there aren't production runs that depend on this one.
                List<ProductionRun> mandatoryWorkEfforts = new LinkedList<>();
                ProductionRunHelper.getLinkedProductionRuns(delegator, dispatcher, productionRunId, mandatoryWorkEfforts);
                for (int i = 1; i < mandatoryWorkEfforts.size(); i++) {
                    GenericValue mandatoryWorkEffort = (mandatoryWorkEfforts.get(i)).getGenericValue();
                    if (!("PRUN_CANCELLED".equals(mandatoryWorkEffort.getString(x.currentStatusId)))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "ManufacturingProductionRunStatusNotChangedMandatoryProductionRunFound", locale));
                    }
                }
                Map<String, Object> serviceContext = new HashMap<>();
                // change the production run (header) status to PRUN_CANCELLED
                serviceContext.put("workEffortId", productionRunId);
                serviceContext.put("currentStatusId", "PRUN_CANCELLED");
                serviceContext.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                // Cancel the product promised
                List<GenericValue> products = EntityQuery.use(delegator).from("WorkEffortGoodStandard")
                        .where("workEffortId", productionRunId,
                                "workEffortGoodStdTypeId", "PRUN_PROD_DELIV",
                                "statusId", "WEGS_CREATED")
                        .queryList();
                if (UtilValidate.isNotEmpty(products)) {
                    for (GenericValue product : products) {
                        product.set(x.statusId, "WEGS_CANCELLED");
                        product.store();
                    }
                }

                // change the tasks status to PRUN_CANCELLED
                List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
                String taskId = null;
                for (GenericValue oneTask : tasks) {
                    taskId = oneTask.getString(x.workEffortId);
                    serviceContext.clear();
                    serviceContext.put("workEffortId", taskId);
                    serviceContext.put("currentStatusId", "PRUN_CANCELLED");
                    serviceContext.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    // cancel all the components
                    List<GenericValue> components = EntityQuery.use(delegator).from("WorkEffortGoodStandard")
                            .where("workEffortId", taskId,
                                    "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED",
                                    "statusId", "WEGS_CREATED")
                            .queryList();
                    if (UtilValidate.isNotEmpty(components)) {
                        for (GenericValue component : components) {
                            component.set(x.statusId, "WEGS_CANCELLED");
                            component.store();
                        }
                    }
                }
            } catch (GenericEntityException | GenericServiceException e) {
                Debug.logError(e, "Problem accessing WorkEffortGoodStandard entity", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusChanged", UtilMisc.toMap(
                    "newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunCannotBeCancelled", locale));
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
    public static Map<String, Object> createProductionRun(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
            if (product == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductNotExist", locale));
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
            Map<String, Object> routingInMap = UtilMisc.toMap("productId", productId, "applicableDate", startDate, "userLogin", userLogin);
            if (workEffortId != null) {
                routingInMap.put("workEffortId", workEffortId);
            }
            Map<String, Object> routingOutMap = dispatcher.runSync("getProductRouting", routingInMap);
            if (ServiceUtil.isError(routingOutMap)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(routingOutMap));
            }
            routing = (GenericValue) routingOutMap.get("routing");
            routingTaskAssocs = UtilGenerics.cast(routingOutMap.get("tasks"));
        } catch (GenericServiceException gse) {
            Debug.logWarning(gse.getMessage(), MODULE);
        }
        if (routing == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductRoutingNotExist", locale));
        }
        if (UtilValidate.isEmpty(routingTaskAssocs)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingRoutingHasNoRoutingTask", locale));
        }

        // -------------------
        // Components
        // -------------------
        // The components are retrieved using the getManufacturingComponents service
        // (that performs a bom breakdown and if needed runs the configurator).
        List<BOMNode> components = null;
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.put("productId", productId); // the product that we want to manufacture
        serviceContext.put("quantity", pRQuantity); // the quantity that we want to manufacture
        serviceContext.put("userLogin", userLogin);
        Map<String, Object> serviceResult = null;
        try {
            serviceResult = dispatcher.runSync("getManufacturingComponents", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            components = UtilGenerics.cast(serviceResult.get("components")); // a list of objects representing the product's components
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the getManufacturingComponents service", MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // ProductionRun header creation,
        if (workEffortName == null) {
            String prdName = UtilValidate.isNotEmpty(product.getString(x.productName)) ? product.getString(x.productName) : product.getString(
                    x.productId);
            String wefName = UtilValidate.isNotEmpty(routing.getString(x.workEffortName)) ? routing.getString(x.workEffortName)
                    : routing.getString(x.workEffortId);
            workEffortName = prdName + "-" + wefName;
        }

        serviceContext.clear();
        serviceContext.put("workEffortTypeId", "PROD_ORDER_HEADER");
        serviceContext.put("workEffortPurposeTypeId", "WEPT_PRODUCTION_RUN");
        serviceContext.put("currentStatusId", "PRUN_CREATED");
        serviceContext.put("workEffortName", workEffortName);
        serviceContext.put("description", description);
        serviceContext.put("facilityId", facilityId);
        serviceContext.put("estimatedStartDate", startDate);
        serviceContext.put("quantityToProduce", pRQuantity);
        serviceContext.put("userLogin", userLogin);
        try {
            serviceResult = dispatcher.runSync("createWorkEffort", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the createWorkEffort service", MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        String productionRunId = (String) serviceResult.get("workEffortId");
        if (Debug.infoOn()) {
            Debug.logInfo("ProductionRun created: " + productionRunId, MODULE);
        }

        // ProductionRun, product will be produce creation = WorkEffortGoodStandard for the productId
        serviceContext.clear();
        serviceContext.put("workEffortId", productionRunId);
        serviceContext.put("productId", productId);
        serviceContext.put("workEffortGoodStdTypeId", "PRUN_PROD_DELIV");
        serviceContext.put("statusId", "WEGS_CREATED");
        serviceContext.put("estimatedQuantity", pRQuantity);
        serviceContext.put("fromDate", startDate);
        serviceContext.put("userLogin", userLogin);
        try {
            serviceResult = dispatcher.runSync("createWorkEffortGoodStandard", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the createWorkEffortGoodStandard service", MODULE);
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
                serviceContext.put("priority", routingTaskAssoc.get(x.sequenceNum));
                serviceContext.put("workEffortPurposeTypeId", "WEPT_PRODUCTION_RUN");
                serviceContext.put("workEffortName", routingTask.get(x.workEffortName));
                serviceContext.put("description", routingTask.get(x.description));
                serviceContext.put("fixedAssetId", routingTask.get(x.fixedAssetId));
                serviceContext.put("workEffortTypeId", "PROD_ORDER_TASK");
                serviceContext.put("currentStatusId", "PRUN_CREATED");
                serviceContext.put("workEffortParentId", productionRunId);
                serviceContext.put("facilityId", facilityId);
                serviceContext.put("reservPersons", routingTask.get(x.reservPersons));
                serviceContext.put("estimatedStartDate", startDate);
                serviceContext.put("estimatedCompletionDate", endDate);
                serviceContext.put("estimatedSetupMillis", routingTask.get(x.estimatedSetupMillis));
                serviceContext.put("estimatedMilliSeconds", routingTask.get(x.estimatedMilliSeconds));
                serviceContext.put("quantityToProduce", pRQuantity);
                serviceContext.put("userLogin", userLogin);
                serviceResult = null;
                try {
                    serviceResult = dispatcher.runSync("createWorkEffort", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the createWorkEffort service", MODULE);
                }
                String productionRunTaskId = (String) serviceResult.get("workEffortId");
                if (Debug.infoOn()) {
                    Debug.logInfo("ProductionRunTaskId created: " + productionRunTaskId, MODULE);
                }

                // The newly created production run task is associated to the routing task
                // to keep track of the template used to generate it.
                serviceContext.clear();
                serviceContext.put("userLogin", userLogin);
                serviceContext.put("workEffortIdFrom", routingTask.getString(x.workEffortId));
                serviceContext.put("workEffortIdTo", productionRunTaskId);
                serviceContext.put("workEffortAssocTypeId", "WORK_EFF_TEMPLATE");
                try {
                    serviceResult = dispatcher.runSync("createWorkEffortAssoc", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the createWorkEffortAssoc service", MODULE);
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
                        serviceContext.put("workEffortId", productionRunTaskId);
                        // Here we get the ProductAssoc record from the BOMNode
                        // object to be sure to use the
                        // right component (possibly configured).
                        serviceContext.put("productId", node.getProduct().get("productId"));
                        serviceContext.put("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED");
                        serviceContext.put("statusId", "WEGS_CREATED");
                        serviceContext.put("fromDate", productBom.get(x.fromDate));
                        // Here we use the getQuantity method to get the quantity already
                        // computed by the getManufacturingComponents service
                        serviceContext.put("estimatedQuantity", node.getQuantity());
                        serviceContext.put("userLogin", userLogin);
                        serviceResult = null;
                        try {
                            serviceResult = dispatcher.runSync("createWorkEffortGoodStandard", serviceContext);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Problem calling the createWorkEffortGoodStandard service", MODULE);
                        }
                        if (Debug.infoOn()) {
                            Debug.logInfo("ProductLink created for productId: " + productBom.getString(x.productIdTo), MODULE);
                        }
                    }
                }
                first = false;
                startDate = endDate;
            }
        }

        // update the estimatedCompletionDate field for the productionRun
        serviceContext.clear();
        serviceContext.put("workEffortId", productionRunId);
        serviceContext.put("estimatedCompletionDate", startDate);
        serviceContext.put("userLogin", userLogin);
        serviceResult = null;
        try {
            serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
        }
        result.put("productionRunId", productionRunId);
        result.put("estimatedCompletionDate", startDate);
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunCreated", UtilMisc.toMap(
                "productionRunId", productionRunId), locale));
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
                    dctx.getDelegator().findByAnd("WorkEffortPartyAssignment", UtilMisc.toMap("workEffortId", routingTaskId), null, false));
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
        }

        if (workEffortPartyAssignments != null) {
            for (GenericValue workEffortPartyAssignment : workEffortPartyAssignments) {
                Map<String, Object> partyToWorkEffort = UtilMisc.<String, Object>toMap(
                        "workEffortId", productionRunTaskId,
                        "partyId", workEffortPartyAssignment.getString(x.partyId),
                        "roleTypeId", workEffortPartyAssignment.getString(x.roleTypeId),
                        "fromDate", workEffortPartyAssignment.getTimestamp(x.fromDate),
                        "statusId", workEffortPartyAssignment.getString(x.statusId),
                        "userLogin", userLogin);
                try {
                    Map<String, Object> result = dctx.getDispatcher().runSync("assignPartyToWorkEffort", partyToWorkEffort);
                    if (ServiceUtil.isError(result)) {
                        String errorMessage = ServiceUtil.getErrorMessage(result);
                        Debug.logError(errorMessage, MODULE);
                        throw new GeneralException(errorMessage);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the assignPartyToWorkEffort service", MODULE);
                }
                if (Debug.infoOn()) {
                    Debug.logInfo("ProductionRunPartyassigment for party: " + workEffortPartyAssignment.get(x.partyId) + " created", MODULE);
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
                    dctx.getDelegator().findByAnd("WorkEffortCostCalc", UtilMisc.toMap("workEffortId", routingTaskId), null, false));
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
        }

        if (workEffortCostCalcs != null) {
            for (GenericValue costCalc : workEffortCostCalcs) {
                Map<String, Object> createCostCalc = UtilMisc.toMap(
                        "workEffortId", productionRunTaskId,
                        "costComponentTypeId", costCalc.getString(x.costComponentTypeId),
                        "costComponentCalcId", costCalc.getString(x.costComponentCalcId),
                        "fromDate", costCalc.get(x.fromDate),
                        "thruDate", costCalc.get(x.thruDate),
                        "userLogin", userLogin);

                try {
                    Map<String, Object> result = dctx.getDispatcher().runSync("createWorkEffortCostCalc", createCostCalc);
                    if (ServiceUtil.isError(result)) {
                        String errorMessage = ServiceUtil.getErrorMessage(result);
                        Debug.logError(errorMessage, MODULE);
                        throw new GeneralException(errorMessage);
                    }
                } catch (GenericServiceException gse) {
                    Debug.logError(gse, "Problem calling the createWorkEffortCostCalc service", MODULE);
                }
                if (Debug.infoOn()) {
                    Debug.logInfo("ProductionRun CostCalc for cost calc: " + costCalc.getString(x.costComponentCalcId) + " created", MODULE);
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
    public static Map<String, Object> updateProductionRun(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productionRunId = (String) context.get(x.productionRunId);

        if (UtilValidate.isNotEmpty(productionRunId)) {
            ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
            if (productionRun.exist()) {

                if (!"PRUN_CREATED".equals(productionRun.getGenericValue().getString("currentStatusId"))
                        && !"PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunPrinted", locale));
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
                    productionRun.getGenericValue().set("facilityId", facilityId);
                }

                boolean updateEstimatedOrderDates = productionRun.isUpdateCompletionDate();
                if (productionRun.store()) {
                    if (updateEstimatedOrderDates && "PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
                        try {
                            Map<String, Object> result = dispatcher.runSync("setEstimatedDeliveryDates",
                                    UtilMisc.toMap("userLogin", userLogin));
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Problem calling the setEstimatedDeliveryDates service", MODULE);
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotUpdated", locale));
                        }
                    }
                    return ServiceUtil.returnSuccess();
                } else {
                    Debug.logError("productionRun.store() fail for productionRunId =" + productionRunId, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotUpdated", locale));
                }
            }
            Debug.logError("no productionRun for productionRunId =" + productionRunId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotUpdated", locale));
        }
        Debug.logError("service updateProductionRun call with productionRunId empty", MODULE);
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotUpdated", locale));
    }

    public static Map<String, Object> changeProductionRunStatus(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotExists", locale));
        }
        String currentStatusId = productionRun.getGenericValue().getString("currentStatusId");

        if (currentStatusId.equals(statusId)) {
            result.put("newStatusId", currentStatusId);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusChanged", UtilMisc.toMap(
                    "newStatusId", currentStatusId), locale));
            return result;
        }

        // PRUN_CREATED --> PRUN_SCHEDULED
        if ("PRUN_CREATED".equals(currentStatusId) && "PRUN_SCHEDULED".equals(statusId)) {
            // change the production run status to PRUN_SCHEDULED
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("currentStatusId", statusId);
            serviceContext.put("userLogin", userLogin);
            try {
                serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            // change the production run tasks status to PRUN_SCHEDULED
            for (GenericValue task : productionRun.getProductionRunRoutingTasks()) {
                serviceContext.clear();
                serviceContext.put("workEffortId", task.getString(x.workEffortId));
                serviceContext.put("currentStatusId", statusId);
                serviceContext.put("userLogin", userLogin);
                try {
                    serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            }
            result.put("newStatusId", statusId);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusChanged", UtilMisc.toMap(
                    "newStatusId", "PRUN_CLOSED"), locale));
            return result;
        }

        // PRUN_CREATED or PRUN_SCHEDULED --> PRUN_DOC_PRINTED
        if (("PRUN_CREATED".equals(currentStatusId) || "PRUN_SCHEDULED".equals(currentStatusId)) && (statusId == null
                || "PRUN_DOC_PRINTED".equals(statusId))) {
            // change only the production run (header) status to PRUN_DOC_PRINTED
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("currentStatusId", "PRUN_DOC_PRINTED");
            serviceContext.put("userLogin", userLogin);
            try {
                serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            // change the production run tasks status to PRUN_DOC_PRINTED
            for (GenericValue task : productionRun.getProductionRunRoutingTasks()) {
                serviceContext.clear();
                serviceContext.put("workEffortId", task.getString(x.workEffortId));
                serviceContext.put("currentStatusId", "PRUN_DOC_PRINTED");
                serviceContext.put("userLogin", userLogin);
                try {
                    serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            }
            result.put("newStatusId", "PRUN_DOC_PRINTED");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusChanged", UtilMisc.toMap(
                    "newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }

        // PRUN_DOC_PRINTED --> PRUN_RUNNING
        // this should be called only when the first task is started
        if ("PRUN_DOC_PRINTED".equals(currentStatusId) && (statusId == null || "PRUN_RUNNING".equals(statusId))) {
            // change only the production run (header) status to PRUN_RUNNING
            // First check if there are production runs with precedence not still completed
            try {
                List<GenericValue> mandatoryWorkEfforts = EntityQuery.use(delegator).from("WorkEffortAssoc")
                        .where("workEffortIdTo", productionRunId,
                                "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY")
                        .filterByDate().queryList();
                for (GenericValue mandatoryWorkEffortAssoc : mandatoryWorkEfforts) {
                    GenericValue mandatoryWorkEffort = mandatoryWorkEffortAssoc.getRelatedOne(x.FromWorkEffort, false);
                    if (!("PRUN_COMPLETED".equals(mandatoryWorkEffort.getString(x.currentStatusId))
                            || "PRUN_RUNNING".equals(mandatoryWorkEffort.getString(x.currentStatusId))
                            || "PRUN_CLOSED".equals(mandatoryWorkEffort.getString(x.currentStatusId)))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "ManufacturingProductionRunStatusNotChangedMandatoryProductionRunNotCompleted", locale));
                    }
                }
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
            }

            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("currentStatusId", "PRUN_RUNNING");
            serviceContext.put("actualStartDate", UtilDateTime.nowTimestamp());
            serviceContext.put("userLogin", userLogin);
            try {
                serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            result.put("newStatusId", "PRUN_RUNNING");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusChanged", UtilMisc.toMap(
                    "newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }

        // PRUN_RUNNING --> PRUN_COMPLETED
        // this should be called only when the last task is completed
        if ("PRUN_RUNNING".equals(currentStatusId) && (statusId == null || "PRUN_COMPLETED".equals(statusId))) {
            // change only the production run (header) status to PRUN_COMPLETED
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("currentStatusId", "PRUN_COMPLETED");
            serviceContext.put("actualCompletionDate", UtilDateTime.nowTimestamp());
            serviceContext.put("userLogin", userLogin);
            try {
                serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            result.put("newStatusId", "PRUN_COMPLETED");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusChanged", UtilMisc.toMap(
                    "newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }

        // PRUN_COMPLETED --> PRUN_CLOSED
        if ("PRUN_COMPLETED".equals(currentStatusId) && (statusId == null || "PRUN_CLOSED".equals(statusId))) {
            // change the production run status to PRUN_CLOSED
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("currentStatusId", "PRUN_CLOSED");
            serviceContext.put("userLogin", userLogin);
            try {
                serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            // change the production run tasks status to PRUN_CLOSED
            for (GenericValue task : productionRun.getProductionRunRoutingTasks()) {
                serviceContext.clear();
                serviceContext.put("workEffortId", task.getString(x.workEffortId));
                serviceContext.put("currentStatusId", "PRUN_CLOSED");
                serviceContext.put("userLogin", userLogin);
                try {
                    serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            }
            result.put("newStatusId", "PRUN_CLOSED");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusChanged", UtilMisc.toMap(
                    "newStatusId", "PRUN_CLOSED"), locale));
            return result;
        }
        result.put("newStatusId", currentStatusId);
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusChanged", UtilMisc.toMap(
                "newStatusId", currentStatusId), locale));
        return result;
    }

    public static Map<String, Object> changeProductionRunTaskStatus(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotExists", locale));
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
                        && (!"PRUN_COMPLETED".equals(oneTask.getString(x.currentStatusId))
                        && !"PRUN_RUNNING".equals(oneTask.getString(x.currentStatusId)))) {
                    allPrecTaskCompletedOrRunning = false;
                }
                if (allTaskCompleted && !"PRUN_COMPLETED".equals(oneTask.getString(x.currentStatusId))) {
                    allTaskCompleted = false;
                }
            }
        }
        if (theTask == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskNotExists", locale));
        }

        String currentStatusId = theTask.getString(x.currentStatusId);
        String oldStatusId = theTask.getString(x.currentStatusId); // pass back old status for secas to check

        if (statusId != null && currentStatusId.equals(statusId)) {
            result.put("oldStatusId", oldStatusId);
            result.put("newStatusId", currentStatusId);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskStatusChanged",
                    UtilMisc.toMap("newStatusId", currentStatusId), locale));
            return result;
        }

        // PRUN_CREATED or PRUN_SCHEDULED or PRUN_DOC_PRINTED --> PRUN_RUNNING
        // this should be called only when the first task is started
        if (("PRUN_CREATED".equals(currentStatusId) || "PRUN_SCHEDULED".equals(currentStatusId) || "PRUN_DOC_PRINTED".equals(currentStatusId))
                && (statusId == null || "PRUN_RUNNING".equals(statusId))) {
            // change the production run task status to PRUN_RUNNING
            // if necessary change the production run (header) status to PRUN_RUNNING
            if (!allPrecTaskCompletedOrRunning) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ManufacturingProductionRunTaskCannotStartPrevTasksNotCompleted", locale));
            }
            if ("PRUN_CREATED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskCannotStartDocsNotPrinted",
                        locale));
            }
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put("workEffortId", taskId);
            serviceContext.put("currentStatusId", "PRUN_RUNNING");
            serviceContext.put("actualStartDate", UtilDateTime.nowTimestamp());
            serviceContext.put("userLogin", userLogin);
            try {
                serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            if (!"PRUN_RUNNING".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
                serviceContext.clear();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("statusId", "PRUN_RUNNING");
                serviceContext.put("userLogin", userLogin);
                try {
                    serviceResult = dispatcher.runSync("changeProductionRunStatus", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the changeProductionRunStatus service", MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            }
            result.put("oldStatusId", oldStatusId);
            result.put("newStatusId", "PRUN_RUNNING");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusChanged", UtilMisc.toMap(
                    "newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }

        // PRUN_RUNNING --> PRUN_COMPLETED
        // this should be called only when the last task is completed
        if ("PRUN_RUNNING".equals(currentStatusId) && (statusId == null || "PRUN_COMPLETED".equals(statusId))) {
            Map<String, Object> serviceContext = new HashMap<>();
            if (issueAllComponents) {
                // Issue all the components, if this task needs components and they still need to be issued
                try {
                    List<GenericValue> inventoryAssigned = EntityQuery.use(delegator).from("WorkEffortInventoryAssign")
                            .where("workEffortId", taskId)
                            .queryList();
                    if (UtilValidate.isEmpty(inventoryAssigned)) {
                        serviceContext.clear();
                        serviceContext.put("workEffortId", taskId);
                        serviceContext.put("userLogin", userLogin);
                        serviceResult = dispatcher.runSync("issueProductionRunTask", serviceContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                        }
                    }
                } catch (GenericEntityException | GenericServiceException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            }
            // change only the production run task status to PRUN_COMPLETED
            serviceContext.clear();
            serviceContext.put("workEffortId", taskId);
            serviceContext.put("currentStatusId", "PRUN_COMPLETED");
            serviceContext.put("actualCompletionDate", UtilDateTime.nowTimestamp());
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
            serviceContext.put("quantityProduced", quantityProduced);
            if (theTask.get(x.actualSetupMillis) == null) {
                serviceContext.put("actualSetupMillis", theTask.get(x.estimatedSetupMillis));
            }
            if (theTask.get(x.actualMilliSeconds) == null) {
                Double autoMillis = null;
                if (theTask.get(x.estimatedMilliSeconds) != null) {
                    autoMillis = quantityProduced.doubleValue() * theTask.getDouble(x.estimatedMilliSeconds);
                }
                serviceContext.put("actualMilliSeconds", autoMillis);
            }
            serviceContext.put("userLogin", userLogin);
            try {
                serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            // Calculate and store the production run task actual costs
            serviceContext.clear();
            serviceContext.put("productionRunTaskId", taskId);
            serviceContext.put("userLogin", userLogin);
            try {
                serviceResult = dispatcher.runSync("createProductionRunTaskCosts", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the createProductionRunTaskCosts service", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            // If this is the last task, then the production run is marked as 'completed'
            if (allTaskCompleted) {
                serviceContext.clear();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("statusId", "PRUN_COMPLETED");
                serviceContext.put("userLogin", userLogin);
                try {
                    serviceResult = dispatcher.runSync("changeProductionRunStatus", serviceContext);
                    if (ServiceUtil.isError(result)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                }
                // and compute the overhead costs associated to the finished product
                try {
                    // get the currency
                    GenericValue facility = productionRun.getGenericValue().getRelatedOne("Facility", false);
                    Map<String, Object> outputMap = dispatcher.runSync("getPartyAccountingPreferences",
                            UtilMisc.<String, Object>toMap("userLogin", userLogin,
                                    "organizationPartyId", facility.getString(x.ownerPartyId)));
                    if (ServiceUtil.isError(outputMap)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
                    }
                    GenericValue partyAccountingPreference = (GenericValue) outputMap.get("partyAccountingPreference");
                    if (partyAccountingPreference == null) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunUnableToFindCosts", locale));
                    }
                    outputMap = dispatcher.runSync("getProductionRunCost", UtilMisc.<String, Object>toMap("userLogin", userLogin, "workEffortId",
                            productionRunId));
                    if (ServiceUtil.isError(outputMap)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
                    }
                    BigDecimal totalCost = (BigDecimal) outputMap.get("totalCost");
                    if (totalCost == null) {
                        totalCost = ZERO;
                    }

                    List<GenericValue> productCostComponentCalcs = EntityQuery.use(delegator).from("ProductCostComponentCalc")
                            .where("productId", productionRun.getProductProduced().get("productId"))
                            .orderBy("sequenceNum").queryList();
                    for (GenericValue productCostComponentCalc : productCostComponentCalcs) {
                        GenericValue costComponentCalc = productCostComponentCalc.getRelatedOne(x.CostComponentCalc, false);
                        GenericValue customMethod = costComponentCalc.getRelatedOne(x.CustomMethod, false);
                        if (customMethod == null) {
                            // TODO: not supported for CostComponentCalc entries directly associated to a product
                            Debug.logWarning("Unable to create cost component for cost component calc with id [" + costComponentCalc.getString(
                                    x.costComponentCalcId) + "] because customMethod is not set", MODULE);
                        } else {
                            Map<String, Object> costMethodResult = dispatcher.runSync(customMethod.getString(x.customMethodName),
                                    UtilMisc.toMap("productCostComponentCalc", productCostComponentCalc,
                                            "costComponentCalc", costComponentCalc,
                                            "costComponentTypePrefix", "ACTUAL",
                                            "baseCost", totalCost,
                                            "currencyUomId", (String) partyAccountingPreference.get(x.baseCurrencyUomId),
                                            "userLogin", userLogin));
                            if (ServiceUtil.isError(costMethodResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(costMethodResult));
                            }
                            BigDecimal productCostAdjustment = (BigDecimal) costMethodResult.get("productCostAdjustment");
                            totalCost = totalCost.add(productCostAdjustment);
                            Map<String, Object> inMap = UtilMisc.<String, Object>toMap("userLogin", userLogin, "workEffortId", productionRunId);
                            inMap.put("costComponentCalcId", costComponentCalc.getString(x.costComponentCalcId));
                            inMap.put("costComponentTypeId", "ACTUAL_" + productCostComponentCalc.getString(x.costComponentTypeId));
                            inMap.put("costUomId", partyAccountingPreference.get(x.baseCurrencyUomId));
                            inMap.put("cost", productCostAdjustment);
                            serviceResult = dispatcher.runSync("createCostComponent", inMap);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        }
                    }
                } catch (GenericEntityException | GenericServiceException gse) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunUnableToFindOverheadCosts",
                            UtilMisc.toMap("errorString", gse.getMessage()), locale));
                }
            }

            result.put("oldStatusId", oldStatusId);
            result.put("newStatusId", "PRUN_COMPLETED");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusChanged", UtilMisc.toMap(
                    "newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }
        result.put("oldStatusId", oldStatusId);
        result.put("newStatusId", currentStatusId);
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskStatusChanged", UtilMisc.toMap(
                "newStatusId", currentStatusId), locale));
        return result;
    }

    public static Map<String, Object> getWorkEffortCosts(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        String workEffortId = (String) context.get(x.workEffortId);
        Locale locale = (Locale) context.get(x.locale);
        try {
            GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
            if (workEffort == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingWorkEffortNotExist", locale) + " " + workEffortId);
            }
            // Get all the valid CostComponents entries
            List<GenericValue> costComponents = EntityQuery.use(delegator).from("CostComponent")
                    .where("workEffortId", workEffortId)
                    .filterByDate().queryList();
            result.put("costComponents", costComponents);
            // TODO: before doing these totals we should convert the cost components' costs to the
            //       base currency uom of the owner of the facility in which the task is running
            BigDecimal totalCost = ZERO;
            BigDecimal totalCostNoMaterials = ZERO;
            for (GenericValue costComponent : costComponents) {
                BigDecimal cost = costComponent.getBigDecimal(x.cost);
                totalCost = totalCost.add(cost);
                if (!"ACTUAL_MAT_COST".equals(costComponent.getString(x.costComponentTypeId))) {
                    totalCostNoMaterials = totalCostNoMaterials.add(cost);
                }
            }
            result.put("totalCost", totalCost);
            result.put("totalCostNoMaterials", totalCostNoMaterials);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunUnableToFindCostsForWorkEffort",
                    UtilMisc.toMap("workEffortId", workEffortId, "errorString", gee.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> getProductionRunCost(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String workEffortId = (String) context.get(x.workEffortId);
        Locale locale = (Locale) context.get(x.locale);
        try {
            List<GenericValue> tasks = EntityQuery.use(delegator).from("WorkEffort")
                    .where("workEffortParentId", workEffortId)
                    .orderBy("workEffortId")
                    .queryList();
            BigDecimal totalCost = ZERO;
            Map<String, Object> outputMap = dispatcher.runSync("getWorkEffortCosts",
                    UtilMisc.<String, Object>toMap("userLogin", userLogin, "workEffortId", workEffortId));
            if (ServiceUtil.isError(outputMap)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
            }
            BigDecimal productionRunHeaderCost = (BigDecimal) outputMap.get("totalCost");
            totalCost = totalCost.add(productionRunHeaderCost);
            for (GenericValue task : tasks) {
                outputMap = dispatcher.runSync("getWorkEffortCosts",
                        UtilMisc.<String, Object>toMap("userLogin", userLogin, "workEffortId", task.getString(x.workEffortId)));
                if (ServiceUtil.isError(outputMap)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
                }
                BigDecimal taskCost = (BigDecimal) outputMap.get("totalCost");
                totalCost = totalCost.add(taskCost);
            }
            result.put("totalCost", totalCost);
        } catch (GenericEntityException | GenericServiceException exc) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunUnableToFindCosts", locale)
                    + " " + workEffortId + " " + exc.getMessage());
        }
        return result;
    }

    public static Map<String, Object> createProductionRunTaskCosts(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> serviceResult = new HashMap<>();
        // this is the id of the actual (real) production run task
        String productionRunTaskId = (String) context.get(x.productionRunTaskId);
        try {
            GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", productionRunTaskId).queryOne();
            if (UtilValidate.isEmpty(workEffort)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskNotFound", UtilMisc.toMap(
                        "productionRunTaskId", productionRunTaskId), locale));
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
            GenericValue routingTaskAssoc = EntityQuery.use(delegator).from("WorkEffortAssoc")
                    .where("workEffortIdTo", productionRunTaskId,
                            "workEffortAssocTypeId", "WORK_EFF_TEMPLATE")
                    .filterByDate().queryFirst();
            GenericValue routingTask = null;
            if (routingTaskAssoc != null) {
                routingTask = routingTaskAssoc.getRelatedOne(x.FromWorkEffort, false);
            }

            // Get all the valid CostComponentCalc entries
            List<GenericValue> workEffortCostCalcs = EntityQuery.use(delegator).from("WorkEffortCostCalc")
                    .where("workEffortId", productionRunTaskId)
                    .filterByDate().queryList();

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
                    Map<String, Object> inMap = UtilMisc.<String, Object>toMap("userLogin", userLogin, "workEffortId", productionRunTaskId);
                    inMap.put("costComponentTypeId", "ACTUAL_" + workEffortCostCalc.getString(x.costComponentTypeId));
                    inMap.put("costComponentCalcId", costComponentCalc.getString(x.costComponentCalcId));
                    inMap.put("costUomId", costComponentCalc.getString(x.currencyUomId));
                    inMap.put("cost", totalCost);
                    serviceResult = dispatcher.runSync("createCostComponent", inMap);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } else {
                    // use the custom method (aka formula) to compute the costs
                    Map<String, Object> inMap = UtilMisc.<String, Object>toMap("userLogin", userLogin, "workEffort", workEffort);
                    inMap.put("workEffortCostCalc", workEffortCostCalc);
                    inMap.put("costComponentCalc", costComponentCalc);
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
                        UtilMisc.toMap("fixedAssetStdCostTypeId", "SETUP_COST"), null, false);
                GenericValue setupCost = EntityUtil.getFirst(EntityUtil.filterByDate(setupCosts));
                List<GenericValue> usageCosts = fixedAsset.getRelated(x.FixedAssetStdCost, UtilMisc.toMap("fixedAssetStdCostTypeId", "USAGE_COST"),
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
                    Map<String, Object> inMap = UtilMisc.<String, Object>toMap("userLogin", userLogin,
                            "workEffortId", productionRunTaskId);
                    inMap.put("costComponentTypeId", "ACTUAL_ROUTE_COST");
                    inMap.put("costUomId", currencyUomId);
                    inMap.put("cost", fixedAssetCost);
                    inMap.put("fixedAssetId", fixedAsset.get(x.fixedAssetId));
                    serviceResult = dispatcher.runSync("createCostComponent", inMap);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
            }
        } catch (GenericEntityException | GenericServiceException ge) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunUnableToCreateRoutingCosts",
                    UtilMisc.toMap("productionRunTaskId", productionRunTaskId, "errorString", ge.getMessage()), locale));
        }
        // materials costs: these are the costs derived from the materials used by the production run task
        try {
            Map<String, BigDecimal> materialsCostByCurrency = new HashMap<>();
            for (GenericValue inventoryConsumed : EntityQuery.use(delegator).from("WorkEffortAndInventoryAssign")
                    .where("workEffortId", productionRunTaskId).queryList()) {
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
                Map<String, Object> inMap = UtilMisc.<String, Object>toMap("userLogin", userLogin,
                        "workEffortId", productionRunTaskId);
                inMap.put("costComponentTypeId", "ACTUAL_MAT_COST");
                inMap.put("costUomId", currencyUomId);
                inMap.put("cost", materialsCost);
                serviceResult = dispatcher.runSync("createCostComponent", inMap);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
        } catch (GenericEntityException | GenericServiceException ge) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunUnableToCreateMaterialsCosts",
                    UtilMisc.toMap("productionRunTaskId", productionRunTaskId, "errorString", ge.getMessage()), locale));
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
    public static Map<String, Object> checkUpdatePrunRoutingTask(DispatchContext ctx, Map<String, ? extends Object> context) {
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

                if (!"PRUN_CREATED".equals(productionRun.getGenericValue().getString("currentStatusId"))
                        && !"PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunPrinted", locale));
                }

                Timestamp estimatedStartDate = (Timestamp) context.get(x.estimatedStartDate);
                Timestamp pRestimatedStartDate = productionRun.getEstimatedStartDate();
                if (pRestimatedStartDate.after(estimatedStartDate)) {
                    try {
                        serviceResult = dispatcher.runSync("updateProductionRun", UtilMisc.toMap("productionRunId", productionRunId,
                                "estimatedStartDate", estimatedStartDate, "userLogin", userLogin));
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } catch (GenericServiceException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingRoutingTaskStartDateBeforePRun", locale));
                    }
                }

                Long priority = (Long) context.get(x.priority);
                List<GenericValue> pRRoutingTasks = productionRun.getProductionRunRoutingTasks();
                boolean first = true;
                for (GenericValue routingTask : pRRoutingTasks) {
                    if (priority.equals(routingTask.get(x.priority)) && !routingTaskId.equals(routingTask.get(x.workEffortId))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingRoutingTaskSeqIdAlreadyExist", locale));
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
                                Debug.logError("productionRun.store(), in routingTask.priority update, fail for productionRunId ="
                                        + productionRunId, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotUpdated", locale));
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
                    Debug.logError("productionRun.store() fail for productionRunId =" + productionRunId, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotUpdated", locale));
                }
            }
            Debug.logError("no productionRun for productionRunId =" + productionRunId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotUpdated", locale));
        }
        Debug.logError("service updateProductionRun call with productionRunId empty", MODULE);
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotUpdated", locale));
    }

    public static Map<String, Object> addProductionRunComponent(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskNotExists", locale));
        }

        if (!"PRUN_CREATED".equals(productionRun.getGenericValue().getString("currentStatusId"))
                && !"PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunPrinted", locale));
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
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskNotExists", locale));
            }
        } else {
            workEffortId = EntityUtil.getFirst(tasks).getString("workEffortId");
        }

        try {
            // Find the product
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
            if (product == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductNotExist", locale));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.clear();
        serviceContext.put("workEffortId", workEffortId);
        serviceContext.put("productId", productId);
        serviceContext.put("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED");
        serviceContext.put("statusId", "WEGS_CREATED");
        serviceContext.put("fromDate", now);
        serviceContext.put("estimatedQuantity", quantity);
        serviceContext.put("userLogin", userLogin);
        try {
            Map<String, Object> serviceResult = dispatcher.runSync("createWorkEffortGoodStandard", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the createWorkEffortGoodStandard service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunComponentNotAdded", locale));
        }
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE,
                "ManufacturingProductionRunComponentAdded", UtilMisc.toMap("productionRunId", productionRunId), locale));
        return result;
    }

    public static Map<String, Object> updateProductionRunComponent(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunComponentNotExists", locale));
        }

        if (!"PRUN_CREATED".equals(productionRun.getGenericValue().getString("currentStatusId"))
                && !"PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunPrinted", locale));
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskNotExists", locale));
        }

        try {
            // Find the product
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
            if (product == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductNotExist", locale));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.clear();
        serviceContext.put("workEffortId", theComponent.getString(x.workEffortId));
        serviceContext.put("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED");
        serviceContext.put("productId", productId);
        serviceContext.put("fromDate", theComponent.getTimestamp(x.fromDate));
        if (quantity != null) {
            serviceContext.put("estimatedQuantity", quantity);
        }
        serviceContext.put("userLogin", userLogin);
        try {
            Map<String, Object> serviceResult = dispatcher.runSync("updateWorkEffortGoodStandard", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the updateWorkEffortGoodStandard service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunComponentNotAdded", locale));
        }
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunComponentUpdated", UtilMisc.toMap(
                "productionRunId", productionRunId), locale));
        return result;
    }

    public static Map<String, Object> addProductionRunRoutingTask(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskNotExists", locale));
        }

        if (!"PRUN_CREATED".equals(productionRun.getGenericValue().getString("currentStatusId"))
                && !"PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunPrinted", locale));
        }

        if (estimatedStartDate != null) {
            Timestamp pRestimatedStartDate = productionRun.getEstimatedStartDate();
            if (pRestimatedStartDate.after(estimatedStartDate)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingRoutingTaskStartDateBeforePRun", locale));
            }
        }

        // The routing task is loaded
        GenericValue routingTask = null;
        try {
            routingTask = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", routingTaskId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingRoutingTaskNotExists", locale));
        }
        if (routingTask == null) {
            Debug.logError("Routing task: " + routingTaskId + " is null.", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingRoutingTaskNotExists", locale));
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
        serviceContext.put("priority", priority);
        serviceContext.put("workEffortPurposeTypeId", routingTask.get(x.workEffortPurposeTypeId));
        serviceContext.put("workEffortName", workEffortName);
        serviceContext.put("description", description);
        serviceContext.put("fixedAssetId", routingTask.get(x.fixedAssetId));
        serviceContext.put("workEffortTypeId", "PROD_ORDER_TASK");
        serviceContext.put("currentStatusId", "PRUN_CREATED");
        serviceContext.put("workEffortParentId", productionRunId);
        serviceContext.put("facilityId", productionRun.getGenericValue().getString("facilityId"));
        serviceContext.put("estimatedStartDate", estimatedStartDate);
        serviceContext.put("estimatedCompletionDate", estimatedCompletionDate);
        serviceContext.put("estimatedSetupMillis", estimatedSetupMillis);
        serviceContext.put("estimatedMilliSeconds", estimatedMilliSeconds);
        serviceContext.put("quantityToProduce", pRQuantity);
        serviceContext.put("userLogin", userLogin);
        Map<String, Object> serviceResult = null;
        try {
            serviceResult = dispatcher.runSync("createWorkEffort", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the createWorkEffort service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingAddProductionRunRoutingTaskNotCreated", locale));
        }
        String productionRunTaskId = (String) serviceResult.get("workEffortId");
        if (Debug.infoOn()) {
            Debug.logInfo("ProductionRunTaskId created: " + productionRunTaskId, MODULE);
        }


        productionRun.setEstimatedCompletionDate(productionRun.recalculateEstimatedCompletionDate());
        if (!productionRun.store()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingAddProductionRunRoutingTaskNotCreated", locale));
        }

        // copy date valid WorkEffortPartyAssignments from the routing task to the run task
        List<GenericValue> workEffortPartyAssignments = null;
        try {
            workEffortPartyAssignments = EntityQuery.use(delegator).from("WorkEffortPartyAssignment")
                    .where("workEffortId", routingTaskId)
                    .filterByDate().queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
        }
        if (workEffortPartyAssignments != null) {
            for (GenericValue workEffortPartyAssignment : workEffortPartyAssignments) {
                Map<String, Object> partyToWorkEffort = UtilMisc.<String, Object>toMap(
                        "workEffortId", productionRunTaskId,
                        "partyId", workEffortPartyAssignment.getString(x.partyId),
                        "roleTypeId", workEffortPartyAssignment.getString(x.roleTypeId),
                        "fromDate", workEffortPartyAssignment.getTimestamp(x.fromDate),
                        "statusId", workEffortPartyAssignment.getString(x.statusId),
                        "userLogin", userLogin);
                try {
                    serviceResult = dispatcher.runSync("assignPartyToWorkEffort", partyToWorkEffort);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the assignPartyToWorkEffort service", MODULE);
                }
                if (Debug.infoOn()) {
                    Debug.logInfo("ProductionRunPartyassigment for party: " + workEffortPartyAssignment.get(x.partyId) + " created", MODULE);
                }
            }
        }

        result.put("routingTaskId", productionRunTaskId);
        result.put("estimatedStartDate", estimatedStartDate);
        result.put("estimatedCompletionDate", estimatedCompletionDate);
        return result;
    }

    public static Map<String, Object> productionRunProduce(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
        }
        // The default is to create a lot if the lotId is given, but the lot doesn't exist
        if (createLotIfNeeded == null) {
            createLotIfNeeded = Boolean.TRUE;
        }
        if (autoCreateLot == null) {
            autoCreateLot = Boolean.FALSE;
        }

        List<String> inventoryItemIds = new LinkedList<>();
        result.put("inventoryItemIds", inventoryItemIds);
        // The production run is loaded
        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        // The last task is loaded
        GenericValue lastTask = productionRun.getLastProductionRunRoutingTask();
        if (lastTask == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskNotExists", locale));
        }
        if ("WIP".equals(productionRun.getProductProduced().getString("productTypeId"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductIsWIP", locale));
        }
        BigDecimal quantityProduced = productionRun.getGenericValue().getBigDecimal("quantityProduced");

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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunProductProducedNotStillAvailable", locale));
        }

        if (lotId == null && autoCreateLot) {
            createLotIfNeeded = Boolean.TRUE;
        }
        try {
            // Find the lot
            GenericValue lot = EntityQuery.use(delegator).from("Lot").where("lotId", lotId).queryOne();
            if (lot == null) {
                if (createLotIfNeeded) {
                    Map<String, Object> createLotCtx = ctx.makeValidContext("createLot", ModelService.IN_PARAM, context);
                    createLotCtx.put("creationDate", UtilDateTime.nowTimestamp());
                    Map<String, Object> serviceResults = dispatcher.runSync("createLot", createLotCtx);
                    if (ServiceUtil.isError(serviceResults)) {
                        Debug.logError(ServiceUtil.getErrorMessage(serviceResults), MODULE);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                    }
                    lotId = (String) serviceResults.get("lotId");
                } else if (UtilValidate.isNotEmpty(lotId)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingLotNotExists", locale));
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        GenericValue orderItem = null;
        try {
            // Find the related order item (if exists)
            List<GenericValue> orderItems = productionRun.getGenericValue().getRelated("WorkOrderItemFulfillment", null, null, false);
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
            facility = productionRun.getGenericValue().getRelatedOne("Facility", false);
            Map<String, Object> outputMap = dispatcher.runSync("getPartyAccountingPreferences", UtilMisc.<String, Object>toMap("userLogin",
                    userLogin, "organizationPartyId", facility.getString(x.ownerPartyId)));
            if (ServiceUtil.isError(outputMap)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
            }
            GenericValue partyAccountingPreference = (GenericValue) outputMap.get("partyAccountingPreference");
            if (partyAccountingPreference == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunUnableToFindCosts", locale));
            }
            outputMap = dispatcher.runSync("getProductCost", UtilMisc.<String, Object>toMap("userLogin", userLogin, "productId",
                    productionRun.getProductProduced().getString("productId"), "currencyUomId",
                    (String) partyAccountingPreference.get(x.baseCurrencyUomId), "costComponentTypePrefix", "EST_STD"));
            if (ServiceUtil.isError(outputMap)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(outputMap));
            }
            unitCost = (BigDecimal) outputMap.get("productCost");
            if (unitCost != null && unitCost.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal totalCost = ZERO;
                List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
                // generic_cost
                List<GenericValue> actualGenCosts = EntityQuery.use(delegator)
                        .from("CostComponent")
                        .where("workEffortId", productionRunId,
                                "costUomId", partyAccountingPreference.get(x.baseCurrencyUomId))
                        .queryList();
                for (GenericValue actualGenCost : actualGenCosts) {
                    totalCost = totalCost.add((BigDecimal) actualGenCost.get(x.cost));
                }
                for (GenericValue task : tasks) {
                    List<GenericValue> otherCosts = EntityQuery.use(delegator)
                            .from("CostComponent")
                            .where("workEffortId", task.get(x.workEffortId),
                                    "costUomId", partyAccountingPreference.get(x.baseCurrencyUomId))
                            .queryList();
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
            GenericValue productFacility = EntityQuery.use(delegator).from("ProductFacility").where("productId",
                    productionRun.getProductProduced().getString("productId"),
                    "facilityId", facility.get(x.facilityId)).queryOne();
            if (productFacility == null) {
                Map<String, Object> createProductFacilityCtx = new HashMap<>();
                createProductFacilityCtx.put("productId", productionRun.getProductProduced().getString("productId"));
                createProductFacilityCtx.put("facilityId", facility.get(x.facilityId));
                createProductFacilityCtx.put("userLogin", userLogin);
                Map<String, Object> serviceResult = dispatcher.runSync("createProductFacility", createProductFacilityCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }

        } catch (GenericEntityException | GenericServiceException gse) {
            Debug.logWarning(gse.getMessage(), MODULE);
            return ServiceUtil.returnError(gse.getMessage());
        }

        if ("SERIALIZED_INV_ITEM".equals(inventoryItemTypeId)) {
            try {
                int numOfItems = quantity.intValue();
                for (int i = 0; i < numOfItems; i++) {
                    Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("productId", productionRun.getProductProduced().getString(
                            "productId"),
                            "inventoryItemTypeId", "SERIALIZED_INV_ITEM",
                            "statusId", "INV_AVAILABLE");
                    serviceContext.put("facilityId", productionRun.getGenericValue().getString("facilityId"));
                    serviceContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                    serviceContext.put("datetimeManufactured", UtilDateTime.nowTimestamp());
                    serviceContext.put("comments", "Created by production run " + productionRunId);
                    if (unitCost.compareTo(ZERO) != 0) {
                        serviceContext.put("unitCost", unitCost);
                    }
                    serviceContext.put("lotId", lotId);
                    serviceContext.put("locationSeqId", locationSeqId);
                    serviceContext.put("uomId", uomId);
                    serviceContext.put("userLogin", userLogin);
                    Map<String, Object> serviceResult = dispatcher.runSync("createInventoryItem", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    String inventoryItemId = (String) serviceResult.get("inventoryItemId");
                    inventoryItemIds.add(inventoryItemId);
                    serviceContext.clear();
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceContext.put("workEffortId", productionRunId);
                    serviceContext.put("availableToPromiseDiff", BigDecimal.ONE);
                    serviceContext.put("quantityOnHandDiff", BigDecimal.ONE);
                    serviceContext.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("createInventoryItemDetail", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    serviceContext.clear();
                    serviceContext.put("userLogin", userLogin);
                    serviceContext.put("workEffortId", productionRunId);
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceResult = dispatcher.runSync("createWorkEffortInventoryProduced", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    // Recompute reservations
                    serviceContext = new HashMap<>();
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceContext.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("balanceInventoryItems", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
            } catch (GenericServiceException exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        } else {
            try {
                Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("productId", productionRun.getProductProduced().getString(
                        "productId"),
                        "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
                serviceContext.put("facilityId", productionRun.getGenericValue().getString("facilityId"));
                serviceContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                serviceContext.put("datetimeManufactured", UtilDateTime.nowTimestamp());
                serviceContext.put("comments", "Created by production run " + productionRunId);
                serviceContext.put("lotId", lotId);
                serviceContext.put("locationSeqId", locationSeqId);
                serviceContext.put("uomId", uomId);
                if (unitCost.compareTo(ZERO) != 0) {
                    serviceContext.put("unitCost", unitCost);
                }
                serviceContext.put("userLogin", userLogin);
                Map<String, Object> serviceResult = dispatcher.runSync("createInventoryItem", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                String inventoryItemId = (String) serviceResult.get("inventoryItemId");
                inventoryItemIds.add(inventoryItemId);
                serviceContext.clear();
                serviceContext.put("inventoryItemId", inventoryItemId);
                serviceContext.put("workEffortId", productionRunId);
                serviceContext.put("availableToPromiseDiff", quantity);
                serviceContext.put("quantityOnHandDiff", quantity);
                serviceContext.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("createInventoryItemDetail", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                serviceContext.clear();
                serviceContext.put("userLogin", userLogin);
                serviceContext.put("workEffortId", productionRunId);
                serviceContext.put("inventoryItemId", inventoryItemId);
                serviceResult = dispatcher.runSync("createWorkEffortInventoryProduced", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                // Recompute reservations
                serviceContext = new HashMap<>();
                serviceContext.put("inventoryItemId", inventoryItemId);
                serviceContext.put("userLogin", userLogin);
                if (orderItem != null) {
                    // the reservations of this order item are privileged reservations
                    serviceContext.put("priorityOrderId", orderItem.getString(x.orderId));
                    serviceContext.put("priorityOrderItemSeqId", orderItem.getString(x.orderItemSeqId));
                }
                serviceResult = dispatcher.runSync("balanceInventoryItems", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        }
        // Now the production run's quantityProduced is updated
        Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("workEffortId", productionRunId);
        serviceContext.put("quantityProduced", quantityProduced.add(quantity));
        serviceContext.put("actualCompletionDate", UtilDateTime.nowTimestamp());
        serviceContext.put("userLogin", userLogin);
        try {
            Map<String, Object> serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the updateWorkEffort service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
        }

        result.put("quantity", quantity);
        return result;
    }

    public static Map<String, Object> productionRunDeclareAndProduce(DispatchContext ctx, Map<String, ? extends Object> context) {
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

        BigDecimal quantityProduced = productionRun.getGenericValue().getBigDecimal("quantityProduced");
        BigDecimal quantityToProduce = productionRun.getGenericValue().getBigDecimal("quantityToProduce");
        if (quantityProduced == null) {
            quantityProduced = BigDecimal.ZERO;
        }
        if (quantityToProduce == null) {
            quantityToProduce = BigDecimal.ZERO;
        }
        BigDecimal minimumQuantityProducedByTask = quantityProduced.add(quantity);
        if (minimumQuantityProducedByTask.compareTo(quantityToProduce) > 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingQuantityProducedIsHigherThanQuantityDeclared", locale));
        }

        List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
        for (int i = 0; i < tasks.size(); i++) {
            GenericValue oneTask = tasks.get(i);
            String taskId = oneTask.getString(x.workEffortId);
            if ("PRUN_RUNNING".equals(oneTask.getString(x.currentStatusId))) {
                BigDecimal quantityDeclared = oneTask.getBigDecimal(x.quantityProduced);
                if (quantityDeclared == null) {
                    quantityDeclared = BigDecimal.ZERO;
                }
                if (minimumQuantityProducedByTask.compareTo(quantityDeclared) > 0) {
                    try {
                        Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("productionRunId", productionRunId,
                                "productionRunTaskId", taskId);
                        serviceContext.put("addQuantityProduced", minimumQuantityProducedByTask.subtract(quantityDeclared));
                        serviceContext.put("issueRequiredComponents", Boolean.TRUE);
                        serviceContext.put("componentsLocationMap", componentsLocationMap);
                        serviceContext.put("userLogin", userLogin);
                        Map<String, Object> serviceResult = dispatcher.runSync("updateProductionRunTask", serviceContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Problem calling the changeProductionRunTaskStatus service", MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
                    }
                }
            }
        }
        try {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.putAll(context);
            inputMap.remove("componentsLocationMap");
            result = dispatcher.runSync("productionRunProduce", inputMap);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the changeProductionRunTaskStatus service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
        }
        return result;
    }

    public static Map<String, Object> productionRunTaskProduce(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
        }

        if (facilityId == null) {
            // The production run is loaded
            ProductionRun productionRun = new ProductionRun(productionRunTaskId, delegator, dispatcher);
            facilityId = productionRun.getGenericValue().getString("facilityId");
        }
        List<String> inventoryItemIds = new LinkedList<>();
        if ("SERIALIZED_INV_ITEM".equals(inventoryItemTypeId)) {
            try {
                int numOfItems = quantity.intValue();
                for (int i = 0; i < numOfItems; i++) {
                    Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("productId", productId,
                            "inventoryItemTypeId", "SERIALIZED_INV_ITEM",
                            "statusId", "INV_AVAILABLE");
                    serviceContext.put("facilityId", facilityId);
                    serviceContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                    serviceContext.put("datetimeManufactured", UtilDateTime.nowTimestamp());
                    serviceContext.put("comments", "Created by production run task " + productionRunTaskId);
                    if (unitCost != null) {
                        serviceContext.put("unitCost", unitCost);
                        serviceContext.put("currencyUomId", currencyUomId);
                    }
                    serviceContext.put("lotId", lotId);
                    serviceContext.put("uomId", uomId);
                    serviceContext.put("userLogin", userLogin);
                    serviceContext.put("isReturned", isReturned);
                    Map<String, Object> serviceResult = dispatcher.runSync("createInventoryItem", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    String inventoryItemId = (String) serviceResult.get("inventoryItemId");
                    serviceContext.clear();
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceContext.put("workEffortId", productionRunTaskId);
                    serviceContext.put("availableToPromiseDiff", BigDecimal.ONE);
                    serviceContext.put("quantityOnHandDiff", BigDecimal.ONE);
                    serviceContext.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("createInventoryItemDetail", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    serviceContext.clear();
                    serviceContext.put("userLogin", userLogin);
                    serviceContext.put("workEffortId", productionRunTaskId);
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceResult = dispatcher.runSync("createWorkEffortInventoryProduced", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    inventoryItemIds.add(inventoryItemId);
                    // Recompute reservations
                    serviceContext = new HashMap<>();
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceContext.put("userLogin", userLogin);
                    serviceResult = dispatcher.runSync("balanceInventoryItems", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
            } catch (GenericServiceException exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        } else {
            try {
                Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("productId", productId,
                        "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
                serviceContext.put("facilityId", facilityId);
                serviceContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                serviceContext.put("datetimeManufactured", UtilDateTime.nowTimestamp());
                serviceContext.put("comments", "Created by production run task " + productionRunTaskId);
                if (unitCost != null) {
                    serviceContext.put("unitCost", unitCost);
                    serviceContext.put("currencyUomId", currencyUomId);
                }
                serviceContext.put("lotId", lotId);
                serviceContext.put("uomId", uomId);
                serviceContext.put("userLogin", userLogin);
                serviceContext.put("isReturned", isReturned);
                Map<String, Object> serviceResult = dispatcher.runSync("createInventoryItem", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                String inventoryItemId = (String) serviceResult.get("inventoryItemId");

                serviceContext.clear();
                serviceContext.put("inventoryItemId", inventoryItemId);
                serviceContext.put("workEffortId", productionRunTaskId);
                serviceContext.put("availableToPromiseDiff", quantity);
                serviceContext.put("quantityOnHandDiff", quantity);
                serviceContext.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("createInventoryItemDetail", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                serviceContext.clear();
                serviceContext.put("userLogin", userLogin);
                serviceContext.put("workEffortId", productionRunTaskId);
                serviceContext.put("inventoryItemId", inventoryItemId);
                serviceResult = dispatcher.runSync("createWorkEffortInventoryProduced", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                inventoryItemIds.add(inventoryItemId);
                // Recompute reservations
                serviceContext = new HashMap<>();
                serviceContext.put("inventoryItemId", inventoryItemId);
                serviceContext.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("balanceInventoryItems", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        }
        result.put("inventoryItemIds", inventoryItemIds);
        return result;
    }

    public static Map<String, Object> productionRunTaskReturnMaterial(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            for (GenericValue issuance : EntityQuery.use(delegator).from("WorkEffortAndInventoryAssign")
                    .where("workEffortId", productionRunTaskId, "productId", productId).queryList()) {
                BigDecimal issued = issuance.getBigDecimal(x.quantity);
                if (issued != null) {
                    totalIssued = totalIssued.add(issued);
                }
            }
            BigDecimal totalReturned = BigDecimal.ZERO;
            for (GenericValue returned : EntityQuery.use(delegator).from("WorkEffortAndInventoryProduced")
                    .where("workEffortId", productionRunTaskId, "productId", productId).queryList()) {
                GenericValue returnDetail = EntityQuery.use(delegator).from("InventoryItemDetail")
                        .where("inventoryItemId", returned.get(x.inventoryItemId))
                        .orderBy("inventoryItemDetailSeqId")
                        .queryFirst();
                if (returnDetail != null) {
                    BigDecimal qtyReturned = returnDetail.getBigDecimal(x.quantityOnHandDiff);
                    if (qtyReturned != null) {
                        totalReturned = totalReturned.add(qtyReturned);
                    }
                }
            }
            if (quantity.compareTo(totalIssued.subtract(totalReturned)) > 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskCannotReturnMoreItems",
                        UtilMisc.toMap("productionRunTaskId", productionRunTaskId, "quantity", quantity, "quantityAllocated",
                                totalIssued.subtract(totalReturned)), locale));
            }
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(gee.getMessage());
        }
        String inventoryItemTypeId = (String) context.get(x.inventoryItemTypeId);

        // TODO: if the task is not running, then return an error message.

        try {
            Map<String, Object> inventoryResult = dispatcher.runSync("productionRunTaskProduce",
                    UtilMisc.<String, Object>toMap("workEffortId", productionRunTaskId,
                            "productId", productId, "quantity", quantity, "lotId", lotId, "currencyUomId", uomId, "isReturned", "Y",
                            "inventoryItemTypeId", inventoryItemTypeId, "userLogin", userLogin));
            if (ServiceUtil.isError(inventoryResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ManufacturingProductionRunTaskProduceError" + ServiceUtil.getErrorMessage(inventoryResult), locale));
            }
        } catch (GenericServiceException exc) {
            return ServiceUtil.returnError(exc.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateProductionRunTask(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            comments = "";
        }

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotExists", locale));
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskNotExists", locale));
        }

        String currentStatusId = theTask.getString(x.currentStatusId);

        if (!"PRUN_RUNNING".equals(currentStatusId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTaskNotRunning", locale));
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
                        List<GenericValue> issuances = EntityQuery.use(delegator).from("WorkEffortAndInventoryAssign")
                                .where("workEffortId", workEffortId,
                                        "productId", component.get(x.productId))
                                .queryList();
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
                            Map<String, Object> serviceContext = UtilMisc.toMap("workEffortId", workEffortId,
                                    "productId", component.getString(x.productId),
                                    "fromDate", component.getTimestamp(x.fromDate));
                            serviceContext.put("quantity", requiredQuantity);
                            if (componentsLocation != null) {
                                serviceContext.put("locationSeqId", componentsLocation.get("locationSeqId"));
                                serviceContext.put("secondaryLocationSeqId", componentsLocation.get("secondaryLocationSeqId"));
                                serviceContext.put("failIfItemsAreNotAvailable", componentsLocation.get("failIfItemsAreNotAvailable"));
                            }
                            serviceContext.put("userLogin", userLogin);
                            Map<String, Object> serviceResult = dispatcher.runSync("issueProductionRunTaskComponent",
                                    serviceContext);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                            }
                        }
                    }
                } catch (GenericEntityException | GenericServiceException e) {
                    String errMsg = "Problem calling the updateProductionRunTaskStatus service";
                    Debug.logError(e, errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }

        // Create a new TimeEntry
        try {
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.clear();
            serviceContext.put("workEffortId", workEffortId);
            if (addTaskTime != null) {
                Double actualMilliSeconds = theTask.getDouble(x.actualMilliSeconds);
                if (actualMilliSeconds == null) {
                    actualMilliSeconds = (double) 0;
                }
                serviceContext.put("actualMilliSeconds", actualMilliSeconds + addTaskTime.doubleValue());
            }
            if (addSetupTime != null) {
                Double actualSetupMillis = theTask.getDouble(x.actualSetupMillis);
                if (actualSetupMillis == null) {
                    actualSetupMillis = (double) 0;
                }
                serviceContext.put("actualSetupMillis", actualSetupMillis + addSetupTime.doubleValue());
            }
            serviceContext.put("quantityProduced", totalQuantityProduced);
            serviceContext.put("quantityRejected", totalQuantityRejected);
            serviceContext.put("userLogin", userLogin);
            Map<String, Object> serviceResult = dispatcher.runSync("updateWorkEffort", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException exc) {
            return ServiceUtil.returnError(exc.getMessage());
        }

        return result;
    }

    public static Map<String, Object> approveRequirement(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        // Mandatory input fields
        String requirementId = (String) context.get(x.requirementId);
        GenericValue requirement = null;
        try {
            requirement = EntityQuery.use(delegator).from("Requirement").where("requirementId", requirementId).queryOne();
        } catch (GenericEntityException e) {
            String errMsg = "Problem calling the approveRequirement service";
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        if (requirement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingRequirementNotExists", locale));
        }
        try {
            Map<String, Object> serviceResult = dispatcher.runSync("updateRequirement",
                    UtilMisc.<String, Object>toMap("requirementId", requirementId,
                            "statusId", "REQ_APPROVED", "requirementTypeId", requirement.getString(x.requirementTypeId),
                            "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingRequirementNotUpdated", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createProductionRunFromRequirement(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            requirement = EntityQuery.use(delegator).from("Requirement").where("requirementId", requirementId).queryOne();
        } catch (GenericEntityException e) {
            String errMsg = "Problem calling the createProductionRunFromRequirement service";
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }
        if (requirement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingRequirementNotExists", locale));
        }
        if (!"INTERNAL_REQUIREMENT".equals(requirement.getString(x.requirementTypeId))) {
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
        serviceContext.put("productId", requirement.getString(x.productId));
        serviceContext.put("pRQuantity", quantity);
        serviceContext.put("startDate", requirement.getTimestamp(x.requirementStartDate));
        serviceContext.put("facilityId", requirement.getString(x.facilityId));
        String workEffortName = null;
        if (requirement.getString(x.description) != null) {
            workEffortName = requirement.getString(x.description);
            if (workEffortName.length() > 50) {
                workEffortName = workEffortName.substring(0, 50);
            }
        } else {
            workEffortName = "Created from requirement " + requirement.getString(x.requirementId);
        }
        serviceContext.put("workEffortName", workEffortName);
        serviceContext.put("userLogin", userLogin);
        Map<String, Object> serviceResult = null;
        try {
            serviceResult = dispatcher.runSync("createProductionRun", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotCreated", locale)
                        + ": " + ServiceUtil.getErrorMessage(serviceResult));
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotCreated", locale)
                    + ": " + e.getMessage());
        }
        if (ServiceUtil.isError(serviceResult)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
        }

        String productionRunId = (String) serviceResult.get("productionRunId");
        result.put("productionRunId", productionRunId);

        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunCreated", UtilMisc.toMap(
                "productionRunId", productionRunId), locale));
        return result;
    }

    public static Map<String, Object> createProductionRunFromConfiguration(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingConfigurationNotAvailable", locale));
        }
        if (config == null
                || config == null && configId != null) {
            // TODO: load the configuration
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunFromConfigurationNotYetImplemented",
                    locale));
        }
        if (!config.isCompleted()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunFromConfigurationNotValid", locale));
        }
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        String instanceProductId = null;
        try {
            instanceProductId = ProductWorker.getAggregatedInstanceId(delegator, config.getProduct().getString("productId"), config.getConfigId());
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.clear();
        serviceContext.put("productId", instanceProductId);
        serviceContext.put("pRQuantity", quantity);
        serviceContext.put("startDate", UtilDateTime.nowTimestamp());
        serviceContext.put("facilityId", facilityId);
        serviceContext.put("userLogin", userLogin);
        Map<String, Object> serviceResult = null;
        try {
            serviceResult = dispatcher.runSync("createProductionRun", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotCreated", locale));
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotCreated", locale));
        }
        String productionRunId = (String) serviceResult.get("productionRunId");
        result.put("productionRunId", productionRunId);

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
                    bomList = EntityQuery.use(delegator).from("ProductAssoc")
                            .where("productId", componentProductId,
                                    "productAssocTypeId", "MANUF_COMPONENT")
                            .filterByDate().queryList();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTryToGetBomListError", locale));
                }
                // if so create a mandatory predecessor to this production run
                if (UtilValidate.isNotEmpty(bomList)) {
                    serviceContext.clear();
                    serviceContext.put("productId", componentProductId);
                    serviceContext.put("quantity", componentQuantity);
                    serviceContext.put("startDate", UtilDateTime.nowTimestamp());
                    serviceContext.put("facilityId", facilityId);
                    serviceContext.put("userLogin", userLogin);
                    serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync("createProductionRunsForProductBom", serviceContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                        GenericValue workEffortPreDecessor = delegator.makeValue("WorkEffortAssoc", UtilMisc.toMap(
                                "workEffortIdTo", productionRunId, "workEffortIdFrom", serviceResult.get("productionRunId"),
                                "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY", "fromDate", UtilDateTime.nowTimestamp()));
                        workEffortPreDecessor.create();
                    } catch (GenericServiceException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotCreated", locale));
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunTryToCreateWorkEffortAssoc",
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
                    serviceContext.put("workEffortId", productionRunId);
                    serviceContext.put("internalNote", "Y");
                    serviceContext.put("noteInfo", comments);
                    serviceContext.put("noteName", co.getDescription());
                    serviceContext.put("userLogin", userLogin);
                    serviceContext.put("noteParty", userLogin.getString(x.partyId));
                    try {
                        serviceResult = dispatcher.runSync("createWorkEffortNote", serviceContext);
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
            serviceContext.put("productionRunId", productionRunId);
            serviceContext.put("productId", productId);
            serviceContext.put("estimatedQuantity", componentQuantity);
            serviceContext.put("userLogin", userLogin);
            try {
                serviceResult = dispatcher.runSync("addProductionRunComponent", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotCreated", locale));
                }
            } catch (GenericServiceException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotCreated", locale));
            }
        }
        try {
            if (productionRunId != null && orderId != null && orderItemSeqId != null) {
                delegator.create("WorkOrderItemFulfillment", UtilMisc.toMap("workEffortId", productionRunId, "orderId", orderId, "orderItemSeqId",
                        orderItemSeqId));
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingRequirementNotDeleted", locale));
        }

        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunCreated", UtilMisc.toMap(
                "productionRunId", productionRunId), locale));
        return result;
    }

    public static Map<String, Object> createProductionRunForMktgPkg(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            GenericValue order = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            GenericValue productStore = delegator.getRelatedOne("ProductStore", order, false);
            isImmediatelyFulfilled = "Y".equals(productStore.getString(x.isImmediatelyFulfilled));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunForMarketingPackagesCreationError",
                    UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "errorString", e.getMessage()), locale));
        }

        GenericValue orderItem = null;
        try {
            orderItem = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryOne();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunForMarketingPackagesCreationError",
                    UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "errorString", e.getMessage()), locale));
        }
        if (orderItem == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunForMarketingPackagesOrderItemNotFound",
                    UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId), locale));
        }
        if (orderItem.get(x.quantity) == null) {
            Debug.logWarning("No quantity found for orderItem [" + orderItem + "], skipping production run of this marketing package", MODULE);
            return ServiceUtil.returnSuccess();
        }

        try {
            // first figure out how much of this product we already have in stock (ATP)
            BigDecimal existingAtp = BigDecimal.ZERO;
            Map<String, Object> tmpResults = dispatcher.runSync("getInventoryAvailableByFacility",
                    UtilMisc.<String, Object>toMap("productId", orderItem.getString(x.productId),
                            "facilityId", facilityId, "userLogin", userLogin));
            if (ServiceUtil.isError(tmpResults)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResults));
            }
            if (tmpResults.get("availableToPromiseTotal") != null) {
                existingAtp = (BigDecimal) tmpResults.get("availableToPromiseTotal");
            }
            // if the order is immediately fulfilled, adjust the atp to compensate for it not reserved
            if (isImmediatelyFulfilled) {
                existingAtp = existingAtp.subtract(orderItem.getBigDecimal(x.quantity));
            }

            if (Debug.verboseOn()) {
                Debug.logVerbose("Order item [" + orderItem + "] Existing ATP = [" + existingAtp + "]", MODULE);
            }
            // we only need to produce more marketing packages if there isn't enough in stock.
            if (existingAtp.compareTo(ZERO) < 0) {
                // how many should we produce?  If there already is some inventory, then just produce enough to bring ATP back up to zero.
                BigDecimal qtyRequired = BigDecimal.ZERO.subtract(existingAtp);
                // ok so that's how many we WANT to produce, but let's check how many we can actually produce based on the available components
                Map<String, Object> serviceContext = new HashMap<>();
                serviceContext.put("productId", orderItem.getString(x.productId));
                serviceContext.put("facilityId", facilityId);
                serviceContext.put("userLogin", userLogin);
                Map<String, Object> serviceResult = dispatcher.runSync("getMktgPackagesAvailable", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                BigDecimal mktgPackagesAvailable = (BigDecimal) serviceResult.get("availableToPromiseTotal");

                BigDecimal qtyToProduce = qtyRequired.min(mktgPackagesAvailable);
                /*
                    Creating production run job for remaining quantity in created status
                    This will handle cases like if production run job creates for partial quantities
                     or in case of fully backordered scenario.
                 */
                BigDecimal remainingQty = orderItem.getBigDecimal(x.quantity).subtract(qtyToProduce);
                if (remainingQty.compareTo(ZERO) > 0) {
                    serviceContext.clear();
                    serviceContext.put("facilityId", facilityId);
                    serviceContext.put("userLogin", userLogin);
                    serviceContext.put("productId", orderItem.getString(x.productId));
                    serviceContext.put("pRQuantity", remainingQty);
                    serviceContext.put("startDate", UtilDateTime.nowTimestamp());
                    serviceResult = dispatcher.runSync("createProductionRun", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    String productionRunIdForRemainingQty = (String) serviceResult.get("productionRunId");
                    try {
                        delegator.create("WorkOrderItemFulfillment",
                                UtilMisc.toMap("workEffortId", productionRunIdForRemainingQty,
                                        "orderId", orderId, "orderItemSeqId", orderItemSeqId));
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "ManufacturingProductionRunForMarketingPackagesCreationError",
                                UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId,
                                        "errorString", e.getMessage()), locale));
                    }
                }

                if (qtyToProduce.compareTo(ZERO) > 0) {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Required quantity (all orders) = [" + qtyRequired + "] quantity to produce = [" + qtyToProduce + "]",
                                MODULE);
                    }

                    serviceContext.put("pRQuantity", qtyToProduce);
                    serviceContext.put("startDate", UtilDateTime.nowTimestamp());
                    serviceResult = dispatcher.runSync("createProductionRun", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                    String productionRunId = (String) serviceResult.get("productionRunId");
                    result.put("productionRunId", productionRunId);

                    try {
                        delegator.create("WorkOrderItemFulfillment", UtilMisc.toMap("workEffortId", productionRunId, "orderId", orderId,
                                "orderItemSeqId", orderItemSeqId));
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "ManufacturingProductionRunForMarketingPackagesCreationError", UtilMisc.toMap("orderId", orderId, "orderItemSeqId",
                                        orderItemSeqId, "errorString", e.getMessage()), locale));
                    }

                    try {
                        serviceContext.clear();
                        serviceContext.put("productionRunId", productionRunId);
                        serviceContext.put("statusId", "PRUN_COMPLETED");
                        serviceContext.put("userLogin", userLogin);
                        serviceResult = dispatcher.runSync("quickChangeProductionRunStatus", serviceContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                        serviceContext.clear();
                        serviceContext.put("workEffortId", productionRunId);
                        serviceContext.put("userLogin", userLogin);
                        serviceResult = dispatcher.runSync("productionRunProduce", serviceContext);
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    } catch (GenericServiceException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotCreated", locale));
                    }

                    result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunCreated",
                            UtilMisc.toMap("productionRunId", productionRunId), locale));
                    return result;
                } else {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("There are not enough components available to produce any marketing packages [" + orderItem.getString(
                                x.productId) + "]", MODULE);
                    }
                    return ServiceUtil.returnSuccess();
                }
            } else {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("No marketing packages need to be produced - ATP is [" + existingAtp + "]", MODULE);
                }
                return ServiceUtil.returnSuccess();
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotCreated", locale));
        }
    }

    public static Map<String, Object> createProductionRunsForOrder(DispatchContext dctx, Map<String, ? extends Object> context) {
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
                    orderItem = EntityQuery.use(delegator).from("OrderItemShipGroupAssoc").where("orderId", orderId, "orderItemSeqId",
                            orderItemSeqId, "shipGroupSeqId", shipGroupSeqId).queryOne();
                } else {
                    orderItem = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryOne();
                }
                if (orderItem == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER, "OrderErrorOrderItemNotFound", UtilMisc.toMap("orderId",
                            orderId, "orderItemSeqId", ""), locale));
                }
                if (quantity != null) {
                    orderItem.set(x.quantity, quantity);
                }
                orderItems = UtilMisc.toList(orderItem);
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER, "OrderProblemsReadingOrderItemInformation", UtilMisc.toMap(
                        "errorString", gee.getMessage()), locale));
            }
        } else {
            try {
                orderItems = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId).queryList();
                if (orderItems == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER, "OrderErrorOrderItemNotFound", UtilMisc.toMap("orderId",
                            orderId, "orderItemSeqId", ""), locale));
                }
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ORDER, "OrderProblemsReadingOrderItemInformation", UtilMisc.toMap(
                        "errorString", gee.getMessage()), locale));
            }
        }
        List<String> productionRuns = new LinkedList<>();
        for (int i = 0; i < orderItems.size(); i++) {
            GenericValue orderItemOrShipGroupAssoc = orderItems.get(i);
            String productId = null;
            BigDecimal amount = null;
            GenericValue orderItem = null;
            if ("OrderItemShipGroupAssoc".equals(orderItemOrShipGroupAssoc.getEntityName())) {
                try {
                    orderItem = orderItemOrShipGroupAssoc.getRelatedOne(x.OrderItem, false);
                } catch (GenericEntityException gee) {
                    Debug.logInfo("Unable to find order item for " + orderItemOrShipGroupAssoc, MODULE);
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
                    existingProductionRuns = EntityQuery.use(delegator).from("WorkAndOrderItemFulfillment")
                            .where(
                                    EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderItemOrShipGroupAssoc.get(x.orderId)),
                                    EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, orderItemOrShipGroupAssoc.get(
                                            x.orderItemSeqId)),
                                    EntityCondition.makeCondition("shipGroupSeqId", EntityOperator.EQUALS, orderItemOrShipGroupAssoc.get(
                                            x.shipGroupSeqId)),
                                    EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CANCELLED"))
                            .cache().queryList();
                } else {
                    existingProductionRuns = EntityQuery.use(delegator).from("WorkAndOrderItemFulfillment")
                            .where(
                                    EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderItemOrShipGroupAssoc.get(x.orderId)),
                                    EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, orderItemOrShipGroupAssoc.get(
                                            x.orderItemSeqId)),
                                    EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CANCELLED"))
                            .cache().queryList();
                }
                if (UtilValidate.isNotEmpty(existingProductionRuns)) {
                    Debug.logWarning("Production Run for order item [" + orderItemOrShipGroupAssoc.getString(x.orderId) + "/"
                            + orderItemOrShipGroupAssoc.getString(x.orderItemSeqId) + "] and ship group [" + shipGroupSeqId + "] already exists.",
                            MODULE);
                    continue;
                }
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturinWorkOrderItemFulfillmentError", UtilMisc.toMap(
                        "errorString", gee.getMessage()), locale));
            }
            try {
                List<BOMNode> components = new LinkedList<>();
                BOMTree tree = new BOMTree(productId, "MANUF_COMPONENT", fromDate, BOMTree.EXPLOSION_MANUFACTURING, delegator, dispatcher, userLogin);
                tree.setRootQuantity(quantity);
                tree.setRootAmount(amount);
                tree.print(components);
                productionRuns.add(tree.createManufacturingOrders(null, fromDate, null, null, null, orderId, orderItem.getString(x.orderItemSeqId),
                        shipGroupSeqId, shipmentId, userLogin));
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingBomErrorCreatingBillOfMaterialsTree",
                        UtilMisc.toMap("errorString", gee.getMessage()), locale));
            }
        }
        result.put("productionRuns", productionRuns);
        return result;
    }

    public static Map<String, Object> createProductionRunsForProductBom(DispatchContext dctx, Map<String, ? extends Object> context) {
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
            BOMTree tree = new BOMTree(productId, "MANUF_COMPONENT", startDate, BOMTree.EXPLOSION_MANUFACTURING, delegator, dispatcher, userLogin);
            tree.setRootQuantity(quantity);
            tree.setRootAmount(BigDecimal.ZERO);
            tree.print(components);
            workEffortId = tree.createManufacturingOrders(facilityId, startDate, workEffortName, description, routingId, null, null, null, null,
                    userLogin);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingBomErrorCreatingBillOfMaterialsTree", UtilMisc.toMap(
                    "errorString", gee.getMessage()), locale));
        }
        if (workEffortId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunIsNotRequiredForProductId",
                    UtilMisc.toMap("productId", productId, "startDate", startDate), locale));
        }
        List<String> productionRuns = new LinkedList<>();
        result.put("productionRuns", productionRuns);
        result.put("productionRunId", workEffortId);
        return result;
    }

    /**
     * Quick runs a ProductionRun task to the completed status, also issuing components
     * if necessary.
     * @param ctx     The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> quickRunProductionRunTask(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            GenericValue task = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", taskId).queryOne();
            String currentStatusId = task.getString(x.currentStatusId);
            String prevStatusId = "";
            while (!"PRUN_COMPLETED".equals(currentStatusId)) {
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("workEffortId", taskId);
                serviceContext.put("issueAllComponents", Boolean.TRUE);
                serviceContext.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("changeProductionRunTaskStatus", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                currentStatusId = (String) serviceResult.get("newStatusId");
                if (currentStatusId.equals(prevStatusId)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunUnableToProgressTaskStatus",
                            UtilMisc.toMap("prevStatusId", prevStatusId, "taskId", taskId), locale));
                } else {
                    prevStatusId = currentStatusId;
                }
                serviceContext.clear();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem accessing the WorkEffort entity", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the changeProductionRunTaskStatus service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
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
    public static Map<String, Object> quickRunAllProductionRunTasks(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String productionRunId = (String) context.get(x.productionRunId);

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotExists", locale));
        }
        List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
        GenericValue oneTask = null;
        String taskId = null;
        for (int i = 0; i < tasks.size(); i++) {
            oneTask = tasks.get(i);
            taskId = oneTask.getString(x.workEffortId);
            try {
                Map<String, Object> serviceContext = new HashMap<>();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("taskId", taskId);
                serviceContext.put("userLogin", userLogin);
                Map<String, Object> serviceResult = dispatcher.runSync("quickRunProductionRunTask", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the quickRunProductionRunTask service", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
            }
        }
        return result;
    }

    public static Map<String, Object> quickStartAllProductionRunTasks(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);

        String productionRunId = (String) context.get(x.productionRunId);

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunNotExists", locale));
        }
        List<GenericValue> tasks = productionRun.getProductionRunRoutingTasks();
        GenericValue oneTask = null;
        String taskId = null;
        for (int i = 0; i < tasks.size(); i++) {
            oneTask = tasks.get(i);
            taskId = oneTask.getString(x.workEffortId);
            if ("PRUN_CREATED".equals(oneTask.getString(x.currentStatusId))
                    || "PRUN_SCHEDULED".equals(oneTask.getString(x.currentStatusId))
                    || "PRUN_DOC_PRINTED".equals(oneTask.getString(x.currentStatusId))) {
                try {
                    Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("productionRunId", productionRunId,
                            "workEffortId", taskId);
                    serviceContext.put("statusId", "PRUN_RUNNING");
                    serviceContext.put("issueAllComponents", Boolean.FALSE);
                    serviceContext.put("userLogin", userLogin);
                    Map<String, Object> serviceResult = dispatcher.runSync("changeProductionRunTaskStatus", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the changeProductionRunTaskStatus service", MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
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
    public static Map<String, Object> quickChangeProductionRunStatus(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            if ("PRUN_DOC_PRINTED".equals(statusId)
                    || "PRUN_RUNNING".equals(statusId)
                    || "PRUN_COMPLETED".equals(statusId)
                    || "PRUN_CLOSED".equals(statusId)) {
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("statusId", "PRUN_DOC_PRINTED");
                serviceContext.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("changeProductionRunStatus", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
            if ("PRUN_RUNNING".equals(statusId) && "Y".equals(startAllTasks)) {
                serviceContext.clear();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("quickStartAllProductionRunTasks", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
            if ("PRUN_COMPLETED".equals(statusId) || "PRUN_CLOSED".equals(statusId)) {
                serviceContext.clear();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("quickRunAllProductionRunTasks", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
            if ("PRUN_CLOSED".equals(statusId)) {
                // Put in warehouse the products manufactured
                serviceContext.clear();
                serviceContext.put("workEffortId", productionRunId);
                serviceContext.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("productionRunProduce", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                serviceContext.clear();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("statusId", "PRUN_CLOSED");
                serviceContext.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("changeProductionRunStatus", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } else {
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("statusId", statusId);
                serviceContext.put("userLogin", userLogin);
                serviceResult = dispatcher.runSync("changeProductionRunStatus", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the changeProductionRunStatus service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunStatusNotChanged", locale));
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
    public static Map<String, Object> getProductionRunTotResQty(DispatchContext ctx, Map<String, ? extends Object> context) {
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

            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "WEGS_CREATED"));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition("estimatedStartDate", EntityOperator.LESS_THAN_EQUAL_TO, startDate));

            List<EntityCondition> findOutgoingProductionRunsStatusConds = new LinkedList<>();
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_CREATED"));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_SCHEDULED"));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_DOC_PRINTED"));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_RUNNING"));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(findOutgoingProductionRunsStatusConds, EntityOperator.OR));

            List<GenericValue> outgoingProductionRuns = EntityQuery.use(delegator).from("WorkEffortAndGoods")
                    .where(findOutgoingProductionRunsConds)
                    .orderBy("-estimatedStartDate")
                    .queryList();
            if (outgoingProductionRuns != null) {
                for (int i = 0; i < outgoingProductionRuns.size(); i++) {
                    GenericValue outgoingProductionRun = outgoingProductionRuns.get(i);
                    BigDecimal qty = outgoingProductionRun.getBigDecimal(x.estimatedQuantity);
                    qty = qty != null ? qty : BigDecimal.ZERO;
                    totQty = totQty.add(qty);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem calling the getProductionRunTotResQty service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionResQtyCalc", locale));
        }
        result.put("reservedQuantity", totQty);
        return result;
    }

    public static Map<String, Object> checkDecomposeInventoryItem(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String inventoryItemId = (String) context.get(x.inventoryItemId);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> serviceResult = new HashMap<>();
        try {
            GenericValue inventoryItem = EntityQuery.use(delegator).from("InventoryItem").where("inventoryItemId", inventoryItemId).queryOne();
            if (inventoryItem == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_PRODUCT, "ProductInventoryItemNotFound", UtilMisc.toMap(
                        "inventoryItemId", inventoryItemId), locale));
            }
            if (inventoryItem.get(x.availableToPromiseTotal) != null && inventoryItem.getBigDecimal(x.availableToPromiseTotal).compareTo(ZERO) <= 0) {
                return ServiceUtil.returnSuccess();
            }
            GenericValue product = inventoryItem.getRelatedOne(x.Product, false);
            if (product == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_PRODUCT, "ProductProductNotFound", locale) + " " + inventoryItem.get(
                        x.productId));
            }
            if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.getString(x.productTypeId), "parentTypeId",
                    "MARKETING_PKG_AUTO")) {
                Map<String, Object> serviceContext = UtilMisc.toMap("inventoryItemId", inventoryItemId,
                        "userLogin", userLogin);
                serviceResult = dispatcher.runSync("decomposeInventoryItem", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem accessing the InventoryItem entity", MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the checkDecomposeInventoryItem service", MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> decomposeInventoryItem(DispatchContext ctx, Map<String, ? extends Object> context) {
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
            GenericValue inventoryItem = EntityQuery.use(delegator).from("InventoryItem")
                    .where("inventoryItemId", inventoryItemId).queryOne();
            if (inventoryItem == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunCannotDecomposingInventoryItem",
                        UtilMisc.toMap("inventoryItemId", inventoryItemId), locale));
            }
            // the work effort (disassemble order) is created
            Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("workEffortTypeId", "TASK",
                    "workEffortPurposeTypeId", "WEPT_PRODUCTION_RUN",
                    "currentStatusId", "CAL_COMPLETED");
            serviceContext.put("workEffortName",
                    "Decomposing product [" + inventoryItem.getString(x.productId) + "] inventory item [" + inventoryItem.getString(x.inventoryItemId)
                            + "]");
            serviceContext.put("facilityId", inventoryItem.getString(x.facilityId));
            serviceContext.put("estimatedStartDate", now);
            serviceContext.put("userLogin", userLogin);
            Map<String, Object> serviceResult = dispatcher.runSync("createWorkEffort", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            String workEffortId = (String) serviceResult.get("workEffortId");
            // the inventory (marketing package) is issued
            serviceContext.clear();
            serviceContext = UtilMisc.toMap("inventoryItem", inventoryItem,
                    "workEffortId", workEffortId, "userLogin", userLogin);
            if (quantity != null) {
                serviceContext.put("quantity", quantity);
            }
            serviceResult = dispatcher.runSync("issueInventoryItemToWorkEffort", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            BigDecimal issuedQuantity = (BigDecimal) serviceResult.get("quantityIssued");
            if (issuedQuantity.compareTo(ZERO) == 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ManufacturingProductionRunCannotDecomposingInventoryItemNoMarketingPackagesFound", UtilMisc.toMap("inventoryItemId",
                                inventoryItem.getString(x.inventoryItemId)), locale));
            }
            // get the package's unit cost to compute a cost coefficient ratio which is the marketing package's actual unit cost divided by its
            // standard cost
            // this ratio will be used to determine the cost of the marketing package components when they are returned to inventory
            serviceContext.clear();
            serviceContext = UtilMisc.toMap("productId", inventoryItem.getString(x.productId),
                    "currencyUomId", inventoryItem.getString(x.currencyUomId),
                    "costComponentTypePrefix", "EST_STD",
                    "userLogin", userLogin);
            serviceResult = dispatcher.runSync("getProductCost", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            BigDecimal packageCost = (BigDecimal) serviceResult.get("productCost");
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
            serviceContext = UtilMisc.toMap("productId", inventoryItem.getString(x.productId),
                    "quantity", issuedQuantity,
                    "userLogin", userLogin);
            serviceResult = dispatcher.runSync("getManufacturingComponents", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            List<Map<String, Object>> components = UtilGenerics.cast(serviceResult.get("componentsMap"));
            if (UtilValidate.isEmpty(components)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ManufacturingProductionRunCannotDecomposingInventoryItemNoComponentsFound", UtilMisc.toMap("productId",
                                inventoryItem.getString(x.productId)), locale));
            }
            for (Map<String, Object> component : components) {
                // get the component's standard cost
                serviceContext.clear();
                serviceContext = UtilMisc.toMap("productId", ((GenericValue) component.get(x.product)).getString("productId"),
                        "currencyUomId", inventoryItem.getString(x.currencyUomId),
                        "costComponentTypePrefix", "EST_STD",
                        "userLogin", userLogin);
                serviceResult = dispatcher.runSync("getProductCost", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                BigDecimal componentCost = (BigDecimal) serviceResult.get("productCost");

                // return the component to inventory at its standard cost multiplied by the cost coefficient from above
                BigDecimal componentInventoryItemCost = costCoefficient.multiply(componentCost);
                serviceContext.clear();
                serviceContext = UtilMisc.toMap("productId", ((GenericValue) component.get(x.product)).getString("productId"),
                        "quantity", component.get(x.quantity),
                        "facilityId", inventoryItem.getString(x.facilityId),
                        "unitCost", componentInventoryItemCost,
                        "userLogin", userLogin);
                serviceContext.put("workEffortId", workEffortId);
                serviceResult = dispatcher.runSync("productionRunTaskProduce", serviceContext);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
                List<String> newInventoryItemIds = UtilGenerics.cast(serviceResult.get("inventoryItemIds"));
                inventoryItemIds.addAll(newInventoryItemIds);
            }
            // the components are put in warehouse
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, "Problem calling the createWorkEffort service", MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        result.put("inventoryItemIds", inventoryItemIds);
        return result;
    }

    public static Map<String, Object> setEstimatedDeliveryDates(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get(x.locale);
        Map<String, TreeMap<Timestamp, Object>> products = new HashMap<>();

        try {
            List<GenericValue> resultList = EntityQuery.use(delegator).from("WorkEffortAndGoods")
                    .where("workEffortGoodStdTypeId", "PRUN_PROD_DELIV",
                            "statusId", "WEGS_CREATED",
                            "workEffortTypeId", "PROD_ORDER_HEADER")
                    .queryList();
            for (GenericValue genericResult : resultList) {
                if ("PRUN_CLOSED".equals(genericResult.getString(x.currentStatusId))
                        || "PRUN_CREATED".equals(genericResult.getString(x.currentStatusId))) {
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
                            UtilMisc.toMap("remainingQty", BigDecimal.ZERO, "reservations", new LinkedList<>()));
                }
                Map<String, Object> dateMap = UtilGenerics.cast(productMap.get(estimatedShipDate));
                BigDecimal remainingQty = (BigDecimal) dateMap.get("remainingQty");
                remainingQty = remainingQty.add(qtyDiff);
                dateMap.put("remainingQty", remainingQty);
            }

            // Approved purchase orders
            resultList = EntityQuery.use(delegator).from("OrderHeaderAndItems")
                    .where("orderTypeId", "PURCHASE_ORDER",
                            "itemStatusId", "ITEM_APPROVED")
                    .orderBy("orderId").queryList();
            String orderId = null;
            GenericValue orderDeliverySchedule = null;
            for (GenericValue genericResult : resultList) {
                String newOrderId = genericResult.getString(x.orderId);
                if (!newOrderId.equals(orderId)) {
                    orderDeliverySchedule = null;
                    orderId = newOrderId;
                    orderDeliverySchedule = EntityQuery.use(delegator).from("OrderDeliverySchedule").where("orderId", orderId, "orderItemSeqId",
                            "_NA_").queryOne();
                }
                String productId = genericResult.getString(x.productId);
                BigDecimal orderQuantity = genericResult.getBigDecimal(x.quantity);
                GenericValue orderItemDeliverySchedule = null;
                orderItemDeliverySchedule = EntityQuery.use(delegator).from("OrderDeliverySchedule").where("orderId", orderId, "orderItemSeqId",
                        genericResult.getString(x.orderItemSeqId)).queryOne();
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
                            UtilMisc.toMap("remainingQty", BigDecimal.ZERO, "reservations", new LinkedList<>()));
                }
                Map<String, Object> dateMap = UtilGenerics.cast(productMap.get(estimatedShipDate));
                BigDecimal remainingQty = (BigDecimal) dateMap.get("remainingQty");
                remainingQty = remainingQty.add(orderQuantity);
                dateMap.put("remainingQty", remainingQty);
            }

            // backorders
            List<EntityCondition> backordersCondList = new LinkedList<>();
            backordersCondList.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.NOT_EQUAL, null));
            backordersCondList.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.GREATER_THAN, BigDecimal.ZERO));

            List<GenericValue> backorders = EntityQuery.use(delegator).from("OrderItemAndShipGrpInvResAndItem")
                    .where(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.NOT_EQUAL, null),
                            EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.GREATER_THAN, BigDecimal.ZERO))
                    .orderBy("shipBeforeDate").queryList();
            for (GenericValue genericResult : backorders) {
                String productId = genericResult.getString(x.productId);
                GenericValue orderItemShipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup")
                        .where("orderId", genericResult.get(x.orderId),
                                "shipGroupSeqId", genericResult.get(x.shipGroupSeqId))
                        .queryOne();
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
                    BigDecimal remainingQty = (BigDecimal) currentDateMap.get("remainingQty");
                    if (remainingQty.compareTo(ZERO) == 0) {
                        continue;
                    }
                    if (remainingQty.compareTo(quantityNotAvailableRem) >= 0) {
                        remainingQty = remainingQty.subtract(quantityNotAvailableRem);
                        currentDateMap.put("remainingQty", remainingQty);
                        GenericValue orderItemShipGrpInvRes = EntityQuery.use(delegator).from("OrderItemShipGrpInvRes")
                                .where("orderId", genericResult.get(x.orderId),
                                        "shipGroupSeqId", genericResult.get(x.shipGroupSeqId),
                                        "orderItemSeqId", genericResult.get(x.orderItemSeqId),
                                        "inventoryItemId", genericResult.get(x.inventoryItemId))
                                .queryOne();
                        orderItemShipGrpInvRes.set(x.promisedDatetime, currentDate);
                        orderItemShipGrpInvRes.store();
                        // TODO: set the reservation
                        break;
                    } else {
                        quantityNotAvailableRem = quantityNotAvailableRem.subtract(remainingQty);
                        remainingQty = BigDecimal.ZERO;
                        currentDateMap.put("remainingQty", remainingQty);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingProductionRunErrorRunningSetEstimatedDeliveryDates",
                    locale));
        }
        return ServiceUtil.returnSuccess();
    }
}
