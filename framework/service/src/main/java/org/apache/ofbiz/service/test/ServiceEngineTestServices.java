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
package org.apache.ofbiz.service.test;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.TestingTypeDao;
import org.apache.ofbiz.persistence.entity.TestingTypeEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericResultWaiter;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import com.landawn.abacus.util.Beans;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.ServiceEngineTestServicesContext;
public class ServiceEngineTestServices {

    private static final String MODULE = ServiceEngineTestServices.class.getName();
    private static final String RESOURCE = x.ServiceErrorUiLabels;

    public static Map<String, Object> testServiceDeadLockRetry(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            // NOTE using persist=false so that the lock retry will have to fix the problem instead of the job poller picking it up again
            GenericResultWaiter threadAWaiter = dispatcher.runAsyncWait(x.testServiceDeadLockRetryThreadA, null, false);
            GenericResultWaiter threadBWaiter = dispatcher.runAsyncWait(x.testServiceDeadLockRetryThreadB, null, false);
            // make sure to wait for these to both finish to make sure results aren't checked until they are done
            Map<String, Object> threadAResult = threadAWaiter.waitForResult();
            Map<String, Object> threadBResult = threadBWaiter.waitForResult();
            List<Object> errorList = new LinkedList<>();
            if (ServiceUtil.isError(threadAResult)) {
                errorList.add(UtilProperties.getMessage(RESOURCE, x.ServiceTestDeadLockThreadA, UtilMisc.toMap(x.errorString,
                        ServiceUtil.getErrorMessage(threadAResult)), locale));
            }
            if (ServiceUtil.isError(threadBResult)) {
                errorList.add(UtilProperties.getMessage(RESOURCE, x.ServiceTestDeadLockThreadB, UtilMisc.toMap(x.errorString,
                        ServiceUtil.getErrorMessage(threadBResult)), locale));
            }
            if (!errorList.isEmpty()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestDeadLockRetry, locale), errorList, null, null);
            }
        } catch (Exception e) {
            Debug.logError(e, x.Error_running_deadlock_test_services + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestDeadLockError, UtilMisc.toMap(x.errorString,
                    e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> testServiceDeadLockRetryThreadA(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        try {
            // grab entity SVCLRT_A by changing, then wait, then find and change SVCLRT_B
            GenericValue testingTypeA = getTestingTypeValue(delegator, x.SVCLRT_A);
            testingTypeA.set(x.description, x.New_description_for_SVCLRT_A);
            testingTypeA.store();

            // wait at least long enough for the other method to have locked resource B
            Debug.logInfo(x.In_testServiceDeadLockRetryThreadA_just_updated_SVCLRT_A_beginning_wait, MODULE);
            Thread.sleep(100);

            Debug.logInfo(x.In_testServiceDeadLockRetryThreadA_done_with_wait_updating_SVCLRT_B, MODULE);
            GenericValue testingTypeB = getTestingTypeValue(delegator, x.SVCLRT_B);
            testingTypeB.set(x.description, x.New_description_for_SVCLRT_B);
            testingTypeB.store();

            Debug.logInfo(x.In_testServiceDeadLockRetryThreadA_done_with_updating_SVCLRT_B_updating_SVCLRT_AONLY, MODULE);
            GenericValue testingTypeAOnly = getTestingTypeValue(delegator, x.SVCLRT_AONLY);
            testingTypeAOnly.set(x.description, x.New_description_for_SVCLRT_AONLY_this_is_only_changed_by_thread_A_so_if_it_doesn_t_match
                    + x.something_happened_to_thread_A);
            testingTypeAOnly.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Entity_Engine_Exception_running_dead_lock_test_thread_A + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestEntityEngineExceptionThreadA, UtilMisc.toMap(
                    x.errorString, e.toString()), locale));
        } catch (InterruptedException e) {
            Debug.logError(e, x.Wait_Interrupted_Exception_running_dead_lock_test_thread_A + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestEntityEngineWaitInterruptedExceptionThreadA,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> testServiceDeadLockRetryThreadB(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        try {
            // grab entity SVCLRT_B by changing, then wait, then change SVCLRT_A
            GenericValue testingTypeB = getTestingTypeValue(delegator, x.SVCLRT_B);
            testingTypeB.set(x.description, x.New_description_for_SVCLRT_B);
            testingTypeB.store();

            // wait at least long enough for the other method to have locked resource B
            Debug.logInfo(x.In_testServiceDeadLockRetryThreadB_just_updated_SVCLRT_B_beginning_wait, MODULE);
            Thread.sleep(100);

            Debug.logInfo(x.In_testServiceDeadLockRetryThreadB_done_with_wait_updating_SVCLRT_A, MODULE);
            GenericValue testingTypeA = getTestingTypeValue(delegator, x.SVCLRT_A);
            testingTypeA.set(x.description, x.New_description_for_SVCLRT_A);
            testingTypeA.store();

            Debug.logInfo(x.In_testServiceDeadLockRetryThreadA_done_with_updating_SVCLRT_A_updating_SVCLRT_BONLY, MODULE);
            GenericValue testingTypeAOnly = getTestingTypeValue(delegator, x.SVCLRT_BONLY);
            testingTypeAOnly.set(x.description, x.New_description_for_SVCLRT_BONLY_this_is_only_changed_by_thread_B_so_if_it_doesn_t_match
                    + x.something_happened_to_thread_B);
            testingTypeAOnly.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Entity_Engine_Exception_running_dead_lock_test_thread_B + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestEntityEngineExceptionThreadB, UtilMisc.toMap(
                    x.errorString, e.toString()), locale));
        } catch (InterruptedException e) {
            Debug.logError(e, x.Wait_Interrupted_Exception_running_dead_lock_test_thread_B + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestEntityEngineWaitInterruptedExceptionThreadB,
                    UtilMisc.toMap(x.errorString, e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    // ==================================================

    public static Map<String, Object> testServiceLockWaitTimeoutRetry(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        try {
            // NOTE using persist=false so that the lock retry will have to fix the problem instead of the job poller picking it up again
            GenericResultWaiter grabberWaiter = dispatcher.runAsyncWait(x.testServiceLockWaitTimeoutRetryGrabber, null, false);
            GenericResultWaiter waiterWaiter = dispatcher.runAsyncWait(x.testServiceLockWaitTimeoutRetryWaiter, null, false);
            // make sure to wait for these to both finish to make sure results aren't checked until they are done
            Map<String, Object> grabberResult = grabberWaiter.waitForResult();
            Map<String, Object> waiterResult = waiterWaiter.waitForResult();
            List<Object> errorList = new LinkedList<>();
            if (ServiceUtil.isError(grabberResult)) {
                errorList.add(x.Error_running_testServiceLockWaitTimeoutRetryGrabber + ServiceUtil.getErrorMessage(grabberResult));
            }
            if (ServiceUtil.isError(waiterResult)) {
                errorList.add(x.Error_running_testServiceLockWaitTimeoutRetryWaiter + ServiceUtil.getErrorMessage(waiterResult));
            }
            if (!errorList.isEmpty()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestLockWaitTimeoutRetry, locale), errorList, null, null);
            }
        } catch (Exception e) {
            Debug.logError(e, x.Error_running_deadlock_test_services + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestDeadLockError, UtilMisc.toMap(x.errorString,
                    e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> testServiceLockWaitTimeoutRetryGrabber(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        try {
            // grab entity SVCLWTRT by changing, then wait a LONG time, ie more than the wait timeout
            GenericValue testingType = getTestingTypeValue(delegator, x.SVCLWTRT);
            testingType.set(x.description, x.New_description_for_SVCLWTRT_from_the_GRABBER_service_this_should_be_replaced_by_Waiter_service_in_the
                    + x.service_engine_auto_retry);
            testingType.store();

            Debug.logInfo(x.In_testServiceLockWaitTimeoutRetryGrabber_just_updated_SVCLWTRT_beginning_wait, MODULE);

            // wait at least long enough for the other method to have locked resource wait time out
            // (tx timeout 6s on this the Grabber and 2s on the Waiter): wait 4 seconds because timeout on this
            Thread.sleep(4 * 1000);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Entity_Engine_Exception_running_lock_wait_timeout_test_Grabber_thread + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestLockWaitTimeoutRetryGrabber, UtilMisc.toMap(
                    x.errorString, e.toString()), locale));
        } catch (InterruptedException e) {
            Debug.logError(e, x.Wait_Interrupted_Exception_running_lock_wait_timeout_test_Grabber_thread + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestLockInterruptedExceptionRetryGrabber, UtilMisc.toMap(
                    x.errorString, e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> testServiceLockWaitTimeoutRetryWaiter(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        try {
            // wait for a small amount of time to make sure the grabber does it's thing first
            Thread.sleep(100);

            Debug.logInfo(x.In_testServiceLockWaitTimeoutRetryWaiter_about_to_update_SVCLWTRT_wait_starts_here, MODULE);

            // TRY grab entity SVCLWTRT by looking up and changing, should get a lock wait timeout exception because of the Grabber thread
            GenericValue testingType = getTestingTypeValue(delegator, x.SVCLWTRT);
            testingType.set(x.description, x.New_description_for_SVCLWTRT_from_Waiter_service_this_is_the_value_that_should_be_there);
            testingType.store();

            Debug.logInfo(x.In_testServiceLockWaitTimeoutRetryWaiter_successfully_updated_SVCLWTRT, MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Entity_Engine_Exception_running_lock_wait_timeout_test_Waiter_thread + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestLockWaitTimeoutRetryWaiter, UtilMisc.toMap(
                    x.errorString, e.toString()), locale));
        } catch (InterruptedException e) {
            Debug.logError(e, x.Wait_Interrupted_Exception_running_lock_wait_timeout_test_Waiter_thread + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestLockInterruptedExceptionRetryWaiter, UtilMisc.toMap(
                    x.errorString, e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    // ==================================================

    /**
     * NOTE that this is a funny case where the auto-retry in the service engine for the call to
     * testServiceLockWaitTimeoutRetryCantRecoverWaiter would NOT be able to recover because it would try again
     * given the new transaction and all, but the lock for the waiting thread would still be there... so it will fail
     * repeatedly.
     * <p>
     * TODO: there's got to be some way to do this, but how?!? :(
     * <p>
     * NOTE: maybe this will work: create a list that the service engine maintains of services it will run after the
     * current service run is complete, and AFTER it has committed or rolled back its transaction; if a service finds
     * it has a lock wait timeout, add itself to the list for its parent service (somehow...) and off we go!
     * @param dctx    the dispatch context
     * @param context the context
     * @return returns the results of the service execution
     */
    public static Map<String, Object> testServiceLockWaitTimeoutRetryCantRecover(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        try {
            // grab entity SVCLWTRTCR by changing, then wait a LONG time, ie more than the wait timeout
            GenericValue testingType = getTestingTypeValue(delegator, x.SVCLWTRTCR);
            testingType.set(x.description, x.New_description_for_SVCLWTRTCR_from_Lock_Wait_Timeout_Lock_GRABBER_this_should_be_replaced_by_the_one
                    + x.in_the_Waiter_service);
            testingType.store();

            Debug.logInfo(x.In_testServiceLockWaitTimeoutRetryCantRecover_grabber_just_updated_SVCLWTRTCR_running_sub_service_in_own_transaction,
                    MODULE);
            // timeout is 5 seconds so it is longer than the tx timeout for this service, so will fail quickly; with this transaction keeping a
            // lock on the record and that one trying to get it, bam we cause the error
            Map<String, Object> waiterResult = dispatcher.runSync(x.testServiceLockWaitTimeoutRetryCantRecoverWaiter, null, 5, true);
            if (ServiceUtil.isError(waiterResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestLockWaitTimeoutRetryCantRecoverWaiter, locale),
                        null, null, waiterResult);
            }

            Debug.logInfo(x.In_testServiceLockWaitTimeoutRetryCantRecover_grabber_successfully_finished_running_sub_service_in_own_transaction,
                    MODULE);
        } catch (GenericServiceException e) {
            String errMsg = x.Error_running_deadlock_test_services + e.toString();
            Debug.logError(e, errMsg, MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Entity_Engine_Exception_running_lock_wait_timeout_test_main_Grabber_thread + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestLockInterruptedExceptionRetryGrabber, UtilMisc.toMap(
                    x.errorString, e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> testServiceLockWaitTimeoutRetryCantRecoverWaiter(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        try {
            Debug.logInfo(x.In_testServiceLockWaitTimeoutRetryCantRecoverWaiter_updating_SVCLWTRTCR, MODULE);

            // TRY grab entity SVCLWTRTCR by looking up and changing, should get a lock wait timeout exception because of the Grabber thread
            GenericValue testingType = getTestingTypeValue(delegator, x.SVCLWTRTCR);
            testingType.set(x.description, x.New_description_for_SVCLWTRTCR_from_Lock_Wait_Timeout_Lock_Waiter_this_is_the_value_that_should_be
                    + x.there);
            testingType.store();

            Debug.logInfo(x.In_testServiceLockWaitTimeoutRetryCantRecoverWaiter_successfully_updated_SVCLWTRTCR, MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Entity_Engine_Exception_running_lock_wait_timeout_test_Waiter_thread + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestLockInterruptedExceptionRetryWaiter, UtilMisc.toMap(
                    x.errorString, e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    // ==================================================

    public static Map<String, Object> testServiceOwnTxSubServiceAfterSetRollbackOnlyInParentErrorCatchWrapper(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        try {
            Map<String, Object> resultMap = dispatcher.runSync(x.testServiceOwnTxSubServiceAfterSetRollbackOnlyInParent, null, 60, true);
            if (ServiceUtil.isError(resultMap)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        x.ServiceTestOwnTxSubServiceAfterSetRollbackOnlyInParentErrorCatchWrapper, locale), null, null, resultMap);
            }
        } catch (GenericServiceException e) {
            String errMsg = x.This_is_the_expected_error_running_sub_service_with_own_tx_after_the_parent_has_set_rollback_only_logging_and
                    + x.ignoring + e.toString();
            Debug.logError(e, errMsg, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> testServiceOwnTxSubServiceAfterSetRollbackOnlyInParent(DispatchContext dctx,
                                                                                             ServiceEngineTestServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        try {
            // change the SVC_SRBO value first to test that the rollback really does revert/reset
            GenericValue testingType = getTestingTypeValue(delegator, x.SVC_SRBO);
            testingType.set(x.description, x.New_description_for_SVC_SRBO_this_should_be_reset_on_the_rollback_if_this_is_in_the_db_then_the_test
                    + x.failed);
            testingType.store();

            TransactionUtil.setRollbackOnly(x.Intentionally_setting_rollback_only_for_testing_purposes, null);

            Map<String, Object> resultMap = dispatcher.runSync(x.testServiceOwnTxSubServiceAfterSetRollbackOnlyInParentSubService, null, 60, true);
            if (ServiceUtil.isError(resultMap)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestOwnTxSubServiceAfterSetRollbackOnlyInParent,
                        locale), null, null, resultMap);
            }
        } catch (Exception e) {
            Debug.logError(e, x.Error_running_sub_service_with_own_tx + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestOwnTxError, UtilMisc.toMap(x.errorString, e.toString()),
                    locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> testServiceOwnTxSubServiceAfterSetRollbackOnlyInParentSubService(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        // this service doesn't actually have to do anything, the problem was in just pausing and resuming the transaciton with setRollbackOnly
        return ServiceUtil.returnSuccess();
    }


    // ==================================================

    public static Map<String, Object> testServiceEcaGlobalEventExec(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        try {
            // this will return an error, but we'll ignore the result
            dispatcher.runSync(x.testServiceEcaGlobalEventExecToRollback, null, 60, true);
        } catch (GenericServiceException e) {
            Debug.logError(e,
                    x.Error_calling_sub_service_it_should_return_an_error_but_not_throw_an_exception_so_something_went_wrong + e.toString(),
                    MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestSubServiceError, UtilMisc.toMap(x.errorString,
                    e.toString()), locale));
        }

        // this service doesn't actually have to do anything, just a placeholder for ECA rules, this one should commit
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> testServiceEcaGlobalEventExecOnCommit(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        try {
            GenericValue testingType = getTestingTypeValue(delegator, x.SVC_SECAGC);
            testingType.set(x.description, x.New_description_for_SVC_SECAGC_what_it_should_be_after_the_global_commit_test);
            testingType.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Entity_Engine_Exception + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestEntityEngineError, UtilMisc.toMap(x.errorString,
                    e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> testServiceEcaGlobalEventExecToRollback(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        // this service doesn't actually have to do anything, just a placeholder for ECA rules, this one should rollback
        Locale locale = (Locale) context.get(x.locale);
        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestRollback, locale));
    }

    public static Map<String, Object> testServiceEcaGlobalEventExecOnRollback(DispatchContext dctx, ServiceEngineTestServicesContext context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get(x.locale);
        try {
            GenericValue testingType = getTestingTypeValue(delegator, x.SVC_SECAGR);
            testingType.set(x.description, x.New_description_for_SVC_SECAGR_what_it_should_be_after_the_global_rollback_test);
            testingType.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, x.Entity_Engine_Exception + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, x.ServiceTestEntityEngineError, UtilMisc.toMap(x.errorString,
                    e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }
    private static GenericValue getTestingTypeValue(Delegator delegator, String testingTypeId) throws GenericEntityException {
        TestingTypeDao testingTypeDao = DaoRegistry.getDao(delegator, x.TestingType, TestingTypeDao.class);
        try {
            TestingTypeEntity testingTypeEntity = testingTypeDao.get(testingTypeId).orElse(null);
            return testingTypeEntity == null ? null : delegator.makeValue(x.TestingType, Beans.beanToMap(testingTypeEntity));
        } catch (java.sql.SQLException e) {
            throw new GenericEntityException(e);
        }
    }
}

