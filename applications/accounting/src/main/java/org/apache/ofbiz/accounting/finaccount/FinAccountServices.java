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

package org.apache.ofbiz.accounting.finaccount;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.order.finaccount.FinAccountHelper;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.FinAccountServicesContext;
public class FinAccountServices {

    private static final String MODULE = FinAccountServices.class.getName();
    private static final String RES_ERROR = x.AccountingErrorUiLabels;

    public static Map<String, Object> createAccountAndCredit(DispatchContext dctx, FinAccountServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String finAccountTypeId = (String) context.get(x.finAccountTypeId);
        String accountName = (String) context.get(x.accountName);
        String finAccountId = (String) context.get(x.finAccountId);
        Locale locale = (Locale) context.get(x.locale);

        // check the type
        if (finAccountTypeId == null) {
            finAccountTypeId = x.SVCCRED_ACCOUNT;
        }
        if (accountName == null) {
            if (x.SVCCRED_ACCOUNT.equals(finAccountTypeId)) {
                accountName = x.Customer_Service_Credit_Account;
            } else {
                accountName = x.Financial_Account;
            }
        }

        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        try {
            // find the most recent (active) service credit account for the specified party
            String partyId = (String) context.get(x.partyId);
            Map<String, String> lookupMap = UtilMisc.toMap(x.finAccountTypeId, finAccountTypeId, x.ownerPartyId,
                    partyId);

            // if a productStoreId is present, restrict the accounts returned using the
            // store's payToPartyId
            String productStoreId = (String) context.get(x.productStoreId);
            if (UtilValidate.isNotEmpty(productStoreId)) {
                String payToPartyId = ProductStoreWorker.getProductStorePayToPartyId(productStoreId, delegator);
                if (UtilValidate.isNotEmpty(payToPartyId)) {
                    lookupMap.put(x.organizationPartyId, payToPartyId);
                }
            }

            // if a currencyUomId is present, use it to restrict the accounts returned
            String currencyUomId = (String) context.get(x.currencyUomId);
            if (UtilValidate.isNotEmpty(currencyUomId)) {
                lookupMap.put(x.currencyUomId, currencyUomId);
            }

            // check for an existing account
            GenericValue creditAccount;
            UserLoginDao finAccountDao = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class);
            if (finAccountId != null) {
                creditAccount = finAccountDao.findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, finAccountId), false);
            } else {
                List<GenericValue> finAccounts = finAccountDao.findByAnd(delegator, x.FinAccount, lookupMap, UtilMisc.toList(x.fromDate_f5440273), false);
                creditAccount = EntityUtil.getFirst(EntityUtil.filterByDate(finAccounts, true));
            }

            if (creditAccount == null) {
                // create a new service credit account
                String createAccountServiceName = x.createFinAccount;
                if (UtilValidate.isNotEmpty(productStoreId)) {
                    createAccountServiceName = x.createFinAccountForStore;
                }
                // automatically set the parameters
                ModelService createAccountService = dctx.getModelService(createAccountServiceName);
                Map<String, Object> createAccountContext = createAccountService.makeValid(context, ModelService.IN_PARAM);
                createAccountContext.put(x.finAccountTypeId, finAccountTypeId);
                createAccountContext.put(x.finAccountName, accountName);
                createAccountContext.put(x.ownerPartyId, partyId);
                createAccountContext.put(x.userLogin, userLogin);

                Map<String, Object> createAccountResult = dispatcher.runSync(createAccountServiceName, createAccountContext);
                if (ServiceUtil.isError(createAccountResult) || ServiceUtil.isFailure(createAccountResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createAccountResult));
                }

                if (createAccountResult != null) {
                    String creditAccountId = (String) createAccountResult.get(x.finAccountId);
                    if (UtilValidate.isNotEmpty(creditAccountId)) {
                        creditAccount = finAccountDao.findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, creditAccountId), false);

                        // create the owner role
                        Map<String, Object> roleCtx = new HashMap<>();
                        roleCtx.put(x.partyId, partyId);
                        roleCtx.put(x.roleTypeId, x.OWNER);
                        roleCtx.put(x.finAccountId, creditAccountId);
                        roleCtx.put(x.userLogin, userLogin);
                        roleCtx.put(x.fromDate, UtilDateTime.nowTimestamp());
                        Map<String, Object> roleResp;
                        try {
                            roleResp = dispatcher.runSync(x.createFinAccountRole, roleCtx);
                        } catch (GenericServiceException e) {
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        if (ServiceUtil.isError(roleResp)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(roleResp));
                        }
                        finAccountId = creditAccountId; // update the finAccountId for return parameter
                    }
                }
                if (creditAccount == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            x.AccountingFinAccountCannotCreditAccount, locale));
                }
            }

            // create the credit transaction
            Map<String, Object> transactionMap = new HashMap<>();
            transactionMap.put(x.finAccountTransTypeId, x.ADJUSTMENT);
            transactionMap.put(x.finAccountId, creditAccount.getString(x.finAccountId));
            transactionMap.put(x.partyId, partyId);
            transactionMap.put(x.amount, context.get(x.amount));
            transactionMap.put(x.reasonEnumId, context.get(x.reasonEnumId));
            transactionMap.put(x.comments, context.get(x.comments));
            transactionMap.put(x.userLogin, userLogin);

            Map<String, Object> creditTransResult = dispatcher.runSync(x.createFinAccountTrans, transactionMap);
            if (ServiceUtil.isError(creditTransResult) || ServiceUtil.isFailure(creditTransResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(creditTransResult));
            }
        } catch (GenericEntityException | GenericServiceException ge) {
            return ServiceUtil.returnError(ge.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.finAccountId, finAccountId);
        return result;
    }

    public static Map<String, Object> createFinAccountForStore(DispatchContext dctx, FinAccountServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String productStoreId = (String) context.get(x.productStoreId);
        String finAccountTypeId = (String) context.get(x.finAccountTypeId);
        Locale locale = (Locale) context.get(x.locale);
        GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);

        try {
            // get the product store id and use it to generate a unique fin account code
            UserLoginDao productStoreFinActSettingDao = DaoRegistry.getDao(delegator, x.ProductStoreFinActSetting, UserLoginDao.class);
            GenericValue productStoreFinAccountSetting = productStoreFinActSettingDao.findOne(delegator, x.ProductStoreFinActSetting,
                    UtilMisc.toMap(x.productStoreId, productStoreId, x.finAccountTypeId, finAccountTypeId), true);
            if (productStoreFinAccountSetting == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingFinAccountSetting,
                        UtilMisc.toMap(x.productStoreId, productStoreId, x.finAccountTypeId, finAccountTypeId),
                        locale));
            }

            Long accountCodeLength = productStoreFinAccountSetting.getLong(x.accountCodeLength);
            Long accountValidDays = productStoreFinAccountSetting.getLong(x.accountValidDays);
            Long pinCodeLength = productStoreFinAccountSetting.getLong(x.pinCodeLength);
            String requirePinCode = productStoreFinAccountSetting.getString(x.requirePinCode);

            // automatically set the parameters for the create fin account service
            ModelService createService = dctx.getModelService(x.createFinAccount);
            Map<String, Object> inContext = createService.makeValid(context, ModelService.IN_PARAM);
            Timestamp now = UtilDateTime.nowTimestamp();

            // now use our values
            String finAccountCode = null;
            if (UtilValidate.isNotEmpty(accountCodeLength)) {
                finAccountCode = FinAccountHelper.getNewFinAccountCode(accountCodeLength.intValue(), delegator);
                inContext.put(x.finAccountCode, finAccountCode);
            }

            // with pin codes, the account code becomes the ID and the pin becomes the code
            if (x.Y.equalsIgnoreCase(requirePinCode)) {
                String pinCode = FinAccountHelper.getNewFinAccountCode(pinCodeLength.intValue(), delegator);
                inContext.put(x.finAccountPin, pinCode);
            }

            // set the dates/userlogin
            if (UtilValidate.isNotEmpty(accountValidDays)) {
                inContext.put(x.thruDate, UtilDateTime.getDayEnd(now, accountValidDays));
            }
            inContext.put(x.fromDate, now);
            inContext.put(x.userLogin, userLogin);

            // product store payToPartyId
            String payToPartyId = ProductStoreWorker.getProductStorePayToPartyId(productStoreId, delegator);
            inContext.put(x.organizationPartyId, payToPartyId);
            inContext.put(x.currencyUomId, productStore.get(x.defaultCurrencyUomId));

            Map<String, Object> createResult = dispatcher.runSync(x.createFinAccount, inContext);
            if (ServiceUtil.isError(createResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createResult));
            }
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put(x.finAccountId, createResult.get(x.finAccountId));
            result.put(x.finAccountCode, finAccountCode);
            return result;
        } catch (GenericEntityException | GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    public static Map<String, Object> checkFinAccountBalance(DispatchContext dctx, FinAccountServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String finAccountId = (String) context.get(x.finAccountId);
        String finAccountCode = (String) context.get(x.finAccountCode);
        Locale locale = (Locale) context.get(x.locale);

        GenericValue finAccount;
        if (finAccountId == null) {
            try {
                finAccount = FinAccountHelper.getFinAccountFromCode(finAccountCode, delegator);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            try {
                UserLoginDao finAccountDao = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class);
                finAccount = finAccountDao.findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, finAccountId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        if (finAccount == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNotFound, UtilMisc.toMap(x.finAccountId, finAccountId), locale));
        }

        // get the balance
        BigDecimal availableBalance = finAccount.getBigDecimal(x.availableBalance);
        BigDecimal balance = finAccount.getBigDecimal(x.actualBalance);
        if (availableBalance == null) {
            availableBalance = FinAccountHelper.getZero();
        }
        if (balance == null) {
            balance = FinAccountHelper.getZero();
        }

        String statusId = finAccount.getString(x.statusId);
        Debug.logInfo(x.FinAccount_Balance + balance + x.Available + availableBalance + x.Status + statusId,
                MODULE);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put(x.availableBalance, availableBalance);
        result.put(x.balance, balance);
        result.put(x.statusId, statusId);
        return result;
    }

    public static Map<String, Object> checkFinAccountStatus(DispatchContext dctx, FinAccountServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        String finAccountId = (String) context.get(x.finAccountId);
        Locale locale = (Locale) context.get(x.locale);

        if (finAccountId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    x.AccountingFinAccountNotFound, UtilMisc.toMap(x.finAccountId, x.emptyString), locale));
        }

        GenericValue finAccount;
        try {
            UserLoginDao finAccountDao = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class);
            finAccount = finAccountDao.findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, finAccountId), false);
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

        if (finAccount != null) {
            String statusId = finAccount.getString(x.statusId);
            if (statusId == null) {
                statusId = x.FNACT_ACTIVE;
            }

            BigDecimal balance = finAccount.getBigDecimal(x.actualBalance);
            if (balance == null) {
                balance = FinAccountHelper.getZero();
            }

            Debug.logInfo(x.Account + finAccountId + x.Balance + balance + x.Status_a82f3e48 + statusId, MODULE);

            if (x.FNACT_ACTIVE.equals(statusId) && balance.compareTo(FinAccountHelper.getZero()) < 1) {
                finAccount.set(x.statusId, x.FNACT_MANFROZEN);
                Debug.logInfo(x.Financial_account + finAccountId + x.has_passed_its_threshold + balance
                        + x.Frozen, MODULE);
            } else if (x.FNACT_MANFROZEN.equals(statusId) && balance.compareTo(FinAccountHelper.getZero()) > 0) {
                finAccount.set(x.statusId, x.FNACT_ACTIVE);
                Debug.logInfo(x.Financial_account + finAccountId + x.has_been_made_current + balance
                        + x.Un_Frozen, MODULE);
            }
            try {
                finAccount.store();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> refundFinAccount(DispatchContext dctx, FinAccountServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String finAccountId = (String) context.get(x.finAccountId);
        Map<String, Object> result = null;

        GenericValue finAccount;
        try {
            UserLoginDao finAccountDao = DaoRegistry.getDao(delegator, x.FinAccount, UserLoginDao.class);
            finAccount = finAccountDao.findOne(delegator, x.FinAccount, UtilMisc.toMap(x.finAccountId, finAccountId), false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        if (finAccount != null) {
            // check to make sure the account is refundable
            if (!x.Y.equals(finAccount.getString(x.isRefundable))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingFinAccountIsNotRefundable, locale));
            }

            // get the actual and available balance
            BigDecimal availableBalance = finAccount.getBigDecimal(x.availableBalance);
            BigDecimal actualBalance = finAccount.getBigDecimal(x.actualBalance);

            // if they do not match, then there are outstanding authorizations which need to
            // be settled first
            if (actualBalance.compareTo(availableBalance) != 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        x.AccountingFinAccountCannotBeRefunded, locale));
            }

            // now we make sure there is something to refund
            if (actualBalance.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal remainingBalance = new BigDecimal(actualBalance.toString());
                BigDecimal refundAmount = BigDecimal.ZERO;

                List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition(x.finAccountTransTypeId,
                        EntityOperator.EQUALS, x.DEPOSIT),
                        EntityCondition.makeCondition(x.finAccountId, EntityOperator.EQUALS, finAccountId));
                EntityCondition condition = EntityCondition.makeCondition(exprs, EntityOperator.AND);

                UserLoginDao finAccountTransDao = DaoRegistry.getDao(delegator, x.FinAccountTrans, UserLoginDao.class);
                EntityFindOptions findOptions = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE,
                        EntityFindOptions.CONCUR_READ_ONLY, true);
                try (EntityListIterator eli = finAccountTransDao.findIteratorByCondition(delegator, x.FinAccountTrans, condition, null,
                        UtilMisc.toList(x.transactionDate_5a2f7760), findOptions)) {
                    GenericValue trans;
                    while (remainingBalance.compareTo(FinAccountHelper.getZero()) < 0 && (trans = eli.next()) != null) {
                        String orderId = trans.getString(x.orderId);
                        String orderItemSeqId = trans.getString(x.orderItemSeqId);

                        // make sure there is an order available to refund
                        if (orderId != null && orderItemSeqId != null) {
                            UserLoginDao orderHeaderDao = DaoRegistry.getDao(delegator, x.OrderHeader, UserLoginDao.class);
                            GenericValue orderHeader = orderHeaderDao.findOne(delegator, x.OrderHeader, UtilMisc.toMap(x.orderId, orderId), false);
                            GenericValue productStore = orderHeader.getRelatedOne(x.ProductStore, false);
                            UserLoginDao orderItemDao = DaoRegistry.getDao(delegator, x.OrderItem, UserLoginDao.class);
                            GenericValue orderItem = orderItemDao.findOne(delegator, x.OrderItem,
                                    UtilMisc.toMap(x.orderId, orderId, x.orderItemSeqId, orderItemSeqId), false);
                            if (!x.ITEM_CANCELLED.equals(orderItem.getString(x.statusId))) {

                                // make sure the item hasn't already been returned
                                List<GenericValue> returnItems = orderItem.getRelated(x.ReturnItem, null, null, false);
                                if (UtilValidate.isEmpty(returnItems)) {
                                    BigDecimal txAmt = trans.getBigDecimal(x.amount);
                                    BigDecimal refAmt = txAmt;
                                    if (remainingBalance.compareTo(txAmt) == -1) {
                                        refAmt = remainingBalance;
                                    }
                                    remainingBalance = remainingBalance.subtract(refAmt);
                                    refundAmount = refundAmount.add(refAmt);

                                    // create the return header
                                    Map<String, Object> rhCtx = UtilMisc.toMap(x.returnHeaderTypeId, x.CUSTOMER_RETURN,
                                            x.fromPartyId, finAccount.getString(x.ownerPartyId), x.toPartyId,
                                            productStore.getString(x.payToPartyId), x.userLogin, userLogin);
                                    Map<String, Object> rhResp = dispatcher.runSync(x.createReturnHeader, rhCtx);
                                    if (ServiceUtil.isError(rhResp)) {
                                        throw new GeneralException(ServiceUtil.getErrorMessage(rhResp));
                                    }
                                    String returnId = (String) rhResp.get(x.returnId);

                                    // create the return item
                                    Map<String, Object> returnItemCtx = new HashMap<>();
                                    returnItemCtx.put(x.returnId, returnId);
                                    returnItemCtx.put(x.orderId, orderId);
                                    returnItemCtx.put(x.description, orderItem.getString(x.itemDescription));
                                    returnItemCtx.put(x.orderItemSeqId, orderItemSeqId);
                                    returnItemCtx.put(x.returnQuantity, BigDecimal.ONE);
                                    returnItemCtx.put(x.receivedQuantity, BigDecimal.ONE);
                                    returnItemCtx.put(x.returnPrice, refAmt);
                                    returnItemCtx.put(x.returnReasonId, x.RTN_NOT_WANT);
                                    returnItemCtx.put(x.returnTypeId, x.RTN_REFUND); // refund return
                                    returnItemCtx.put(x.returnItemTypeId, x.RET_NPROD_ITEM);
                                    returnItemCtx.put(x.userLogin, userLogin);

                                    Map<String, Object> retItResp = dispatcher.runSync(x.createReturnItem,
                                            returnItemCtx);
                                    if (ServiceUtil.isError(retItResp)) {
                                        throw new GeneralException(ServiceUtil.getErrorMessage(retItResp));
                                    }
                                    String returnItemSeqId = (String) retItResp.get(x.returnItemSeqId);

                                    // approve the return
                                    Map<String, Object> appRet = UtilMisc.toMap(x.statusId, x.RETURN_ACCEPTED,
                                            x.returnId, returnId, x.userLogin, userLogin);
                                    Map<String, Object> appResp = dispatcher.runSync(x.updateReturnHeader, appRet);
                                    if (ServiceUtil.isError(appResp)) {
                                        throw new GeneralException(ServiceUtil.getErrorMessage(appResp));
                                    }

                                    // "receive" the return - should trigger the refund
                                    Map<String, Object> recRet = UtilMisc.toMap(x.statusId, x.RETURN_RECEIVED,
                                            x.returnId, returnId, x.userLogin, userLogin);
                                    Map<String, Object> recResp = dispatcher.runSync(x.updateReturnHeader, recRet);
                                    if (ServiceUtil.isError(recResp)) {
                                        throw new GeneralException(ServiceUtil.getErrorMessage(recResp));
                                    }

                                    // get the return item
                                    UserLoginDao returnItemDao = DaoRegistry.getDao(delegator, x.ReturnItem, UserLoginDao.class);
                                    GenericValue returnItem = returnItemDao.findOne(delegator, x.ReturnItem,
                                            UtilMisc.toMap(x.returnId, returnId, x.returnItemSeqId, returnItemSeqId), false);
                                    GenericValue response = returnItem.getRelatedOne(x.ReturnItemResponse, false);
                                    if (response == null) {
                                        throw new GeneralException(x.No_return_response_found_for + returnItem
                                                .getPrimaryKey());
                                    }
                                    String paymentId = response.getString(x.paymentId);

                                    // create the adjustment transaction
                                    Map<String, Object> txCtx = new HashMap<>();
                                    txCtx.put(x.finAccountTransTypeId, x.ADJUSTMENT);
                                    txCtx.put(x.finAccountId, finAccountId);
                                    txCtx.put(x.orderId, orderId);
                                    txCtx.put(x.orderItemSeqId, orderItemSeqId);
                                    txCtx.put(x.paymentId, paymentId);
                                    txCtx.put(x.amount, refAmt.negate());
                                    txCtx.put(x.partyId, finAccount.getString(x.ownerPartyId));
                                    txCtx.put(x.userLogin, userLogin);

                                    Map<String, Object> txResp = dispatcher.runSync(x.createFinAccountTrans, txCtx);
                                    if (ServiceUtil.isError(txResp)) {
                                        throw new GeneralException(ServiceUtil.getErrorMessage(txResp));
                                    }
                                }
                            }
                        }
                    }
                } catch (GeneralException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // check to make sure we balanced out
                if (remainingBalance.compareTo(FinAccountHelper.getZero()) == 1) {
                    result = ServiceUtil.returnSuccess(UtilProperties.getMessage(RES_ERROR,
                            x.AccountingFinAccountPartiallyRefunded, locale));
                }
            }
        }

        if (result == null) {
            result = ServiceUtil.returnSuccess();
        }

        return result;
    }
}
