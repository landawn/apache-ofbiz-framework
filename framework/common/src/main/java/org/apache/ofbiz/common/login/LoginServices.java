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

package org.apache.ofbiz.common.login;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import javax.transaction.Transaction;

import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.authentication.AuthHelper;
import org.apache.ofbiz.common.authentication.api.AuthenticatorException;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.dao.UserLoginDao;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.security.SecurityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.control.LoginWorker;
import org.apache.tomcat.util.res.StringManager;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.LdapAuthenticationServicesContext;
import org.apache.ofbiz.model.LoginServicesContext;
/**
 * <b>Title:</b> Login Services
 */
public class LoginServices {

    private static final String MODULE = LoginServices.class.getName();
    private static final String RESOURCE = x.SecurityextUiLabels;

    /**
     * Login service to authenticate username and password
     * @return Map of results including (userLogin) GenericValue object
     */
    public static Map<String, Object> userLogin(DispatchContext ctx, LoginServicesContext context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = ctx.getDelegator();

        // Keep track of two different kinds of errors (UserOnly and DebugLog) and set the RESPONSE_MESSAGE of the
        // service according to what kind of errors where thrown
        List<String> userErrMsgs = new ArrayList<>();
        List<String> debugErrMsgs = new ArrayList<>();

        // load the external auth modules -- note: this will only run once and cache the objects
        if (!AuthHelper.authenticatorsLoaded()) {
            AuthHelper.loadAuthenticators(dispatcher);
        }

        // Authenticate to LDAP if configured to do so
        // TODO: this should be moved to using the NEW Authenticator API
        if (x._true.equals(EntityUtilProperties.getPropertyValue(x.security, x.security_ldap_enable, delegator))) {
            if (!LdapAuthenticationServices.userLogin(ctx, new LdapAuthenticationServicesContext(context))) {
                String errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_ldap_authentication_failed, locale);
                if (x._true.equals(EntityUtilProperties.getPropertyValue(x.security, x.security_ldap_fail_login, delegator))) {
                    return ServiceUtil.returnError(errMsg);
                }
                Debug.logInfo(errMsg, MODULE);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        boolean useEncryption = x._true.equals(EntityUtilProperties.getPropertyValue(x.security, x.password_encrypt, delegator));

        // if isServiceAuth is not specified, default to not a service auth
        boolean isServiceAuth = context.get(x.isServiceAuth) != null && (Boolean) context.get(x.isServiceAuth);

        String username = (String) context.get(x.login_username);
        if (username == null) {
            username = (String) context.get(x.username);
        }
        String password = (String) context.get(x.login_password);
        if (password == null) {
            password = (String) context.get(x.password);
        }
        String jwtToken = (String) context.get(x.login_token);
        if (jwtToken == null) {
            jwtToken = (String) context.get(x.token);
        }

        // get the visitId for the history entity
        String visitId = (String) context.get(x.visitId);

        if (UtilValidate.isEmpty(username)) {
            userErrMsgs.add(UtilProperties.getMessage(RESOURCE, x.loginservices_username_missing, locale));
        } else if (UtilValidate.isEmpty(password) && UtilValidate.isEmpty(jwtToken)) {
            userErrMsgs.add(UtilProperties.getMessage(RESOURCE, x.loginservices_password_missing, locale));
        } else {

            if (x._true.equalsIgnoreCase(EntityUtilProperties.getPropertyValue(x.security, x.username_lowercase, delegator))) {
                username = username.toLowerCase(Locale.getDefault());
            }
            if (x._true.equalsIgnoreCase(EntityUtilProperties.getPropertyValue(x.security, x.password_lowercase, delegator))) {
                password = password.toLowerCase(Locale.getDefault());
            }

            boolean repeat = true;
            // starts at zero but it incremented at the beginning so in the first pass passNumber will be 1
            int passNumber = 0;

            while (repeat) {
                repeat = false;
                // pass number is incremented here because there are continues in this loop so it may never get to the end
                passNumber++;

                GenericValue userLogin = null;

                try {
                    // only get userLogin from cache for service calls; for web and other manual logins there is less time sensitivity
                    UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
                    userLogin = userLoginDao.findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, username), isServiceAuth);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, x.emptyString, MODULE);
                }

                // see if any external auth modules want to sync the user info
                if (userLogin == null) {
                    try {
                        AuthHelper.syncUser(username);
                    } catch (AuthenticatorException e) {
                        Debug.logWarning(e, MODULE);
                    }

                    // check the user login object again
                    try {
                        UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
                        userLogin = userLoginDao.findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, username), isServiceAuth);
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e, x.emptyString, MODULE);
                    }
                }

                if (userLogin != null) {
                    String ldmStr = EntityUtilProperties.getPropertyValue(x.security, x.login_disable_minutes, delegator);
                    long loginDisableMinutes;

                    try {
                        loginDisableMinutes = Long.parseLong(ldmStr);
                    } catch (Exception e) {
                        loginDisableMinutes = 30;
                        Debug.logWarning(x.Could_not_parse_login_disable_minutes_from_security_properties_using_default_of_30, MODULE);
                    }

                    Timestamp disabledDateTime = userLogin.getTimestamp(x.disabledDateTime);
                    Timestamp reEnableTime = null;

                    if (loginDisableMinutes > 0 && disabledDateTime != null) {
                        reEnableTime = new Timestamp(disabledDateTime.getTime() + loginDisableMinutes * 60000);
                    }

                    boolean doStore = true;
                    // we might change & store this userLogin, so we should clone it here to get a mutable copy
                    userLogin = GenericValue.create(userLogin);

                    // get the is system flag -- system accounts can only be used for service authentication
                    boolean isSystem = (isServiceAuth && userLogin.get(x.isSystem) != null) ? x.Y.equalsIgnoreCase(userLogin.getString(x.isSystem))
                                                                                            : false;

                    // grab the hasLoggedOut flag
                    Boolean hasLoggedOut = userLogin.getBoolean(x.hasLoggedOut);

                    if ((UtilValidate.isEmpty(userLogin.getString(x.enabled)) || x.Y.equals(userLogin.getString(x.enabled))
                            || (reEnableTime != null && reEnableTime.before(UtilDateTime.nowTimestamp())) || (isSystem))
                            && UtilValidate.isEmpty(userLogin.getString(x.disabledBy))) {
                        String successfulLogin;
                        if (!isSystem) {
                            userLogin.set(x.enabled, x.Y);
                            userLogin.set(x.disabledBy, null);
                        }
                        // attempt to authenticate with Authenticator class(es)
                        boolean authFatalError = false;
                        boolean externalAuth = false;
                        try {
                            externalAuth = AuthHelper.authenticate(username, password, isServiceAuth);
                        } catch (AuthenticatorException e) {
                            // fatal error -- or single authenticator found -- fail now
                            Debug.logWarning(e, MODULE);
                            authFatalError = true;

                        }

                        // check whether to sign in with Tomcat SSO
                        boolean useTomcatSSO = EntityUtilProperties.propertyValueEquals(x.security, x.security_login_tomcat_sso, x._true);
                        HttpServletRequest request = (jakarta.servlet.http.HttpServletRequest) context.get(x.request);
                        // when request is not supplied, we will treat that SSO is not required as
                        // in the usage of userLogin service in ICalWorker.java
                        useTomcatSSO = useTomcatSSO && (request != null);

                        // resolve the key for decrypt the token and control the validity
                        boolean jwtTokenValid = SecurityUtil.authenticateUserLoginByJWT(delegator, username, jwtToken);

                        // if the password.accept.encrypted.and.plain property in security is set to true allow plain or encrypted passwords
                        // if this is a system account don't bother checking the passwords
                        // if externalAuth passed; this is run as well
                        if ((!authFatalError && externalAuth) || (useTomcatSSO && tomcatSSOLogin(request, username, password))
                                || (jwtToken != null && jwtTokenValid)
                                || (password != null && checkPassword(userLogin.getString(x.currentPassword), useEncryption, password))) {
                            Debug.logVerbose(x.LoginServices_userLogin_Password_Matched_or_Token_Validated, MODULE);

                            // update the hasLoggedOut flag
                            if (hasLoggedOut == null || hasLoggedOut) {
                                userLogin.set(x.hasLoggedOut, x.N);
                            }

                            // reset failed login count if necessary
                            Long currentFailedLogins = userLogin.getLong(x.successiveFailedLogins);
                            if (currentFailedLogins != null && currentFailedLogins > 0) {
                                userLogin.set(x.successiveFailedLogins, 0L);
                            } else if (hasLoggedOut != null && !hasLoggedOut) {
                                // successful login & no logout flag, no need to change anything, so don't do the store
                                doStore = false;
                            }

                            successfulLogin = x.Y;

                            if (!isServiceAuth) {
                                // get the UserLoginSession if this is not a service auth
                                Map<?, ?> userLoginSessionMap = LoginWorker.getUserLoginSession(userLogin);

                                // return the UserLoginSession Map
                                if (userLoginSessionMap != null) {
                                    result.put(x.userLoginSession, userLoginSessionMap);
                                }
                            }

                            result.put(x.userLogin, userLogin);
                            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                        } else {
                            // password is incorrect, but this may be the result of a stale cache entry,
                            // so lets clear the cache and try again if this is the first pass
                            // but only if authFatalError is not true; this would mean the single authenticator failed
                            if (!authFatalError && isServiceAuth && passNumber <= 1) {
                                delegator.clearCacheLine(x.UserLogin, UtilMisc.toMap(x.userLoginId, username));
                                repeat = true;
                                continue;
                            }
                            Debug.logInfo(x.LoginServices_userLogin_Password_Incorrect, MODULE);
                            // password invalid...
                            if (password != null) {
                                userErrMsgs.add(UtilProperties.getMessage(RESOURCE, x.loginservices_password_incorrect, locale));
                            } else if (jwtToken != null) {
                                userErrMsgs.add(UtilProperties.getMessage(RESOURCE, x.loginservices_token_incorrect, locale));
                            }
                            // increment failed login count
                            Long currentFailedLogins = userLogin.getLong(x.successiveFailedLogins);

                            if (currentFailedLogins == null) {
                                currentFailedLogins = 1L;
                            } else {
                                currentFailedLogins = currentFailedLogins + 1;
                            }
                            userLogin.set(x.successiveFailedLogins, currentFailedLogins);

                            // if failed logins over amount in properties file, disable account
                            String mflStr = EntityUtilProperties.getPropertyValue(x.security, x.max_failed_logins, delegator);
                            long maxFailedLogins = 3;
                            try {
                                maxFailedLogins = Long.parseLong(mflStr);
                            } catch (Exception e) {
                                maxFailedLogins = 3;
                                Debug.logWarning(x.Could_not_parse_max_failed_logins_from_security_properties_using_default_of_3, MODULE);
                            }

                            if (maxFailedLogins > 0 && currentFailedLogins >= maxFailedLogins) {
                                userLogin.set(x.enabled, x.N);
                                userLogin.set(x.disabledDateTime, UtilDateTime.nowTimestamp());
                            }

                            successfulLogin = x.N;
                        }

                        // this section is being done in its own transaction rather than in the
                        // current/existing transaction because we may return error and we don't
                        // want that to stop this from getting stored
                        Transaction parentTx = null;
                        boolean beganTransaction = false;

                        try {
                            try {
                                parentTx = TransactionUtil.suspend();
                            } catch (GenericTransactionException e) {
                                Debug.logError(e, x.Could_not_suspend_transaction + e.getMessage(), MODULE);
                            }

                            try {
                                beganTransaction = TransactionUtil.begin();

                                if (doStore) {
                                    userLogin.store();
                                }

                                if (x._true.equals(EntityUtilProperties.getPropertyValue(x.security, x.store_login_history, delegator))) {
                                    boolean createHistory = true;

                                    // only save info on service auth if option set to true to do so
                                    if (isServiceAuth && !x._true.equals(
                                            EntityUtilProperties.getPropertyValue(x.security, x.store_login_history_on_service_auth, delegator))) {
                                        createHistory = false;
                                    }

                                    if (createHistory) {
                                        Map<String, Object> ulhCreateMap = UtilMisc.toMap(x.userLoginId, username, x.visitId, visitId, x.fromDate,
                                                UtilDateTime.nowTimestamp(), x.successfulLogin, successfulLogin);

                                        ModelEntity modelUserLogin = userLogin.getModelEntity();
                                        if (modelUserLogin.isField(x.partyId)) {
                                            ulhCreateMap.put(x.partyId, userLogin.get(x.partyId));
                                        }

                                        // ONLY save the password if it was incorrect
                                        // we will check in the hash size isn't too huge for the store other wise store a fix string
                                        if (x.N.equals(successfulLogin) && !x._false.equals(EntityUtilProperties.getPropertyValue(x.security,
                                                x.store_login_history_incorrect_password, delegator))) {
                                            ulhCreateMap.put(x.passwordUsed, isGivenPasswordCanBeStored(delegator, password)
                                                    ? x.TOO_LONG_FOR_STORAGE
                                                    : password);
                                        }

                                        delegator.create(x.UserLoginHistory, ulhCreateMap);
                                    }
                                }
                            } catch (GenericEntityException e) {
                                String geeErrMsg = x.Error_saving_UserLoginHistory;
                                if (doStore) {
                                    geeErrMsg += x.and_updating_login_status_to_reset_hasLoggedOut_unsuccessful_login_count_etc;
                                }
                                try {
                                    TransactionUtil.rollback(beganTransaction, geeErrMsg, e);
                                } catch (GenericTransactionException e2) {
                                    Debug.logError(e2, x.Could_not_rollback_nested_transaction + e2.getMessage(), MODULE);
                                }

                                // if doStore is true then this error should not be ignored and we shouldn't consider it a successful login if this
                                // happens as there is something very wrong lower down that will bite us again later
                                if (doStore) {
                                    return ServiceUtil.returnError(geeErrMsg);
                                }
                            } finally {
                                try {
                                    TransactionUtil.commit(beganTransaction);
                                } catch (GenericTransactionException e) {
                                    Debug.logError(e, x.Could_not_commit_nested_transaction + e.getMessage(), MODULE);
                                }
                            }
                        } finally {
                            // resume/restore parent transaction
                            if (parentTx != null) {
                                try {
                                    TransactionUtil.resume(parentTx);
                                    Debug.logVerbose(x.Resumed_the_parent_transaction, MODULE);
                                } catch (GenericTransactionException e) {
                                    Debug.logError(e, x.Could_not_resume_parent_nested_transaction + e.getMessage(), MODULE);
                                }
                            }
                        }
                    } else {
                        // account is disabled, but this may be the result of a stale cache entry,
                        // so lets clear the cache and try again if this is the first pass
                        if (isServiceAuth && passNumber <= 1) {
                            delegator.clearCacheLine(x.UserLogin, UtilMisc.toMap(x.userLoginId, username));
                            repeat = true;
                            continue;
                        }
                        Map<String, Object> messageMap = UtilMisc.<String, Object>toMap(x.username, username);
                        userErrMsgs.add(UtilProperties.getMessage(RESOURCE, x.loginservices_account_for_user_login_id_disabled, messageMap, locale));
                        StringBuilder tmpErrMsg = new StringBuilder();
                        if (disabledDateTime != null) {
                            messageMap = UtilMisc.<String, Object>toMap(x.disabledDateTime, disabledDateTime);
                            tmpErrMsg.append(x.str_b858cb28);
                            tmpErrMsg.append(UtilProperties.getMessage(RESOURCE, x.loginservices_since_datetime, messageMap, locale));
                        } else {
                            tmpErrMsg.append(x.str_3a52ce78);
                        }

                        if (loginDisableMinutes > 0 && reEnableTime != null) {
                            messageMap = UtilMisc.<String, Object>toMap(x.reEnableTime, reEnableTime);
                            tmpErrMsg.append(x.str_b858cb28);
                            tmpErrMsg.append(UtilProperties.getMessage(RESOURCE, x.loginservices_will_be_reenabled, messageMap, locale));
                        } else {
                            tmpErrMsg.append(x.str_b858cb28);
                            tmpErrMsg.append(UtilProperties.getMessage(RESOURCE, x.loginservices_not_scheduled_to_be_reenabled, locale));
                        }
                        userErrMsgs.add(tmpErrMsg.toString());
                    }
                } else {
                    // no userLogin object; there may be a non-syncing authenticator
                    boolean externalAuth = false;
                    try {
                        externalAuth = AuthHelper.authenticate(username, password, isServiceAuth);
                    } catch (AuthenticatorException e) {
                        debugErrMsgs.add(e.getMessage());
                        Debug.logError(e, x.External_Authenticator_had_fatal_exception + e.getMessage(), MODULE);
                    }
                    if (externalAuth) {
                        // external auth passed - create a placeholder object for session
                        userLogin = delegator.makeValue(x.UserLogin);
                        userLogin.set(x.userLoginId, username);
                        userLogin.set(x.enabled, x.Y);
                        userLogin.set(x.hasLoggedOut, x.N);
                        result.put(x.userLogin, userLogin);
                        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                        // TODO: more than this is needed to support 100% external authentication
                        // TODO: party + security information is needed; Userlogin will need to be stored
                    } else {
                        // userLogin record not found, user does not exist
                        String errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_user_not_found, locale);
                        userErrMsgs.add(errMsg);
                        Debug.logInfo(x.LoginServices_userLogin_Invalid_User + username + x.str_fa497ce8 + errMsg, MODULE);
                    }
                }
            }
        }

        if (debugErrMsgs.size() > 0) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        } else if (userErrMsgs.size() > 0) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
        }
        // if a technical error occurred then log all error message
        List<String> messages = new ArrayList<>();
        if (!debugErrMsgs.isEmpty()) {
            messages.add(String.join(x.str_0d0c4ddd, debugErrMsgs));
        }
        if (!userErrMsgs.isEmpty()) {
            messages.add(String.join(x.str_0d0c4ddd, userErrMsgs));
        }
        String allErrMsg = null;
        if (!messages.isEmpty()) {
            allErrMsg = String.join(x.str_0d0c4ddd, messages);
        }
        if (allErrMsg != null) {
            result.put(ModelService.ERROR_MESSAGE, allErrMsg);
        }
        return result;
    }

    /**
     * To escape an exception when the password store due to limitation size for passwordUsed field, we analyse if it's possible.
     * @param delegator
     * @param password
     * @return
     * @throws GenericEntityException
     */
    private static boolean isGivenPasswordCanBeStored(Delegator delegator, String password)
            throws GenericEntityException {
        ModelEntity modelEntityUserLoginHistory = delegator.getModelEntity(x.UserLoginHistory);
        ModelField passwordUsedField = modelEntityUserLoginHistory.getField(x.passwordUsed);
        int maxPasswordSize = delegator.getEntityFieldType(
                modelEntityUserLoginHistory,
                passwordUsedField.getType()).stringLength();
        int passwordUsedCurrentSize = password.length();

        // if the field is encrypted, we check the size of the hashed result
        ModelField.EncryptMethod encryptMethod = passwordUsedField.getEncryptMethod();
        if (encryptMethod.isEncrypted()) {
            passwordUsedCurrentSize = delegator.encryptFieldValue(x.UserLoginHistory, encryptMethod, password).toString().length();
        }
        return passwordUsedCurrentSize > maxPasswordSize;
    }

    /**
     * Login service to authenticate a username without password, storing history
     * @return Map of results including (userLogin) GenericValue object
     */
    public static Map<String, Object> userImpersonate(DispatchContext ctx, LoginServicesContext context) {
        Locale locale = (Locale) context.get(x.locale);
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String userLoginIdToImpersonate = (String) context.get(x.userLoginIdToImpersonate);
        GenericValue originUserLogin = (GenericValue) context.get(x.userLogin);
        // get the visitId for the history entity
        String visitId = (String) context.get(x.visitId);

        if (x._true.equalsIgnoreCase(EntityUtilProperties.getPropertyValue(x.security, x.username_lowercase, delegator))) {
            userLoginIdToImpersonate = userLoginIdToImpersonate.toLowerCase();
        }

        GenericValue userLogin;
        try {
            UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
            userLogin = userLoginDao.findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, userLoginIdToImpersonate), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // check impersonation controls
        String errorMessage = checkImpersonationControls(delegator, originUserLogin, userLogin, locale);
        if (errorMessage != null) {
            return ServiceUtil.returnError(errorMessage);
        }

        // return the UserLoginSession Map
        Map<String, Object> userLoginSessionMap = LoginWorker.getUserLoginSession(userLogin);
        if (userLoginSessionMap != null) {
            result.put(x.userLoginSession, userLoginSessionMap);
        }

        // grab the hasLoggedOut flag
        boolean hasLoggedOut = x.Y.equalsIgnoreCase(userLogin.getString(x.hasLoggedOut));
        if (hasLoggedOut || UtilValidate.isEmpty(userLogin.getString(x.hasLoggedOut))) {
            userLogin.set(x.hasLoggedOut, x.N);
            try {
                userLogin.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // Log impersonation in UserLoginHistory
        Map<String, Object> historyCreateMap = UtilMisc.toMap(x.userLoginId, userLoginIdToImpersonate);
        historyCreateMap.put(x.visitId, visitId);
        historyCreateMap.put(x.fromDate, UtilDateTime.nowTimestamp());
        historyCreateMap.put(x.successfulLogin, x.Y);
        historyCreateMap.put(x.partyId, userLogin.get(x.partyId));
        historyCreateMap.put(x.originUserLoginId, originUserLogin.get(x.userLoginId));
        // End impersonation in one hour max
        historyCreateMap.put(x.thruDate, UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.HOUR, 1));
        try {
            delegator.create(x.UserLoginHistory, historyCreateMap);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        result.put(x.userLogin, userLogin);
        result.put(x.originUserLogin, originUserLogin);
        return result;
    }

    /**
     * Return error message if a needed control has failed : userLoginToImpersonate must exist Impersonation have to be enabled Check
     * userLoginIdToImpersonate is active, not Admin and not equals to userLogin Check userLogin has enough permission
     * @param delegator
     * @param userLogin
     * @param userLoginToImpersonate
     * @param locale
     * @return
     */
    private static String checkImpersonationControls(Delegator delegator, GenericValue userLogin, GenericValue userLoginToImpersonate,
            Locale locale) {
        if (userLoginToImpersonate == null) {
            return UtilProperties.getMessage(RESOURCE, x.loginservices_username_missing, locale);
        }
        String userLoginId = userLogin.getString(x.userLoginId);
        String userLoginIdToImpersonate = userLoginToImpersonate.getString(x.userLoginId);

        if (UtilProperties.getPropertyAsBoolean(x.security, x.security_disable_impersonation, true)) {
            return UtilProperties.getMessage(RESOURCE, x.loginevents_impersonation_disabled, locale);
        }

        if (!LoginWorker.isUserLoginActive(userLoginToImpersonate)) {
            Map<String, Object> messageMap = UtilMisc.toMap(x.username, userLoginIdToImpersonate);
            return UtilProperties.getMessage(RESOURCE, x.loginservices_account_for_user_login_id_disabled, messageMap, locale);
        }

        if (SecurityUtil.hasUserLoginAdminPermission(delegator, userLoginIdToImpersonate)) {
            return UtilProperties.getMessage(RESOURCE, x.loginevents_impersonate_notAdmin, locale);
        }

        if (userLoginIdToImpersonate.equals(userLoginId)) {
            return UtilProperties.getMessage(RESOURCE, x.loginevents_impersonate_yourself, locale);
        }

        // Cannot impersonate more privileged user
        List<String> missingNeededPermissions = SecurityUtil.hasUserLoginMorePermissionThan(delegator, userLoginId, userLoginIdToImpersonate);
        if (UtilValidate.isNotEmpty(missingNeededPermissions)) {
            String missingPermissionListString = missingNeededPermissions.stream().collect(Collectors.joining(x.str_d3bc9a37));
            return UtilProperties.getMessage(RESOURCE, x.loginevents_impersonate_notEnoughPermission,
                    UtilMisc.toMap(x.missingPermissions, missingPermissionListString), locale);
        }

        return null;
    }

    public static void createUserLoginPasswordHistory(GenericValue userLogin) throws GenericEntityException {
        int passwordChangeHistoryLimit = 0;
        Delegator delegator = userLogin.getDelegator();
        String userLoginId = userLogin.getString(x.userLoginId);
        String currentPassword = userLogin.getString(x.currentPassword);
        try {
            passwordChangeHistoryLimit = EntityUtilProperties.getPropertyAsInteger(x.security, x.password_change_history_limit, 0);
        } catch (NumberFormatException nfe) {
            // No valid value is found so don't bother to save any password history
            passwordChangeHistoryLimit = 0;
        }
        if (passwordChangeHistoryLimit == 0 || passwordChangeHistoryLimit < 0) {
            // Not saving password history, so return from here.
            return;
        }
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        UserLoginDao userLoginPasswordHistoryDao = DaoRegistry.getDao(delegator, x.UserLoginPasswordHistory, UserLoginDao.class);
        EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toMap(x.userLoginId, userLoginId));
        EntityFindOptions findOptions = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE,
                EntityFindOptions.CONCUR_READ_ONLY, true);
        try (EntityListIterator eli = userLoginPasswordHistoryDao.findIteratorByCondition(delegator, x.UserLoginPasswordHistory, condition,
                null, UtilMisc.toList(x.fromDate_f5440273), findOptions)) {
            GenericValue pwdHist;
            pwdHist = eli.next();
            if (pwdHist != null) {
                // updating password so set end date on previous password in history
                pwdHist.set(x.thruDate, nowTimestamp);
                pwdHist.store();
                // check if we have hit the limit on number of password changes to be saved. If we did then delete the oldest password from history.
                eli.last();
                int rowIndex = eli.currentIndex();
                if (rowIndex == passwordChangeHistoryLimit) {
                    eli.afterLast();
                    pwdHist = eli.previous();
                    pwdHist.remove();
                }
            }
        }

        // save this password in history
        GenericValue userLoginPwdHistToCreate = delegator.makeValue(x.UserLoginPasswordHistory,
                UtilMisc.toMap(x.userLoginId, userLoginId, x.fromDate, nowTimestamp));
        userLoginPwdHistToCreate.set(x.currentPassword, currentPassword);
        userLoginPwdHistToCreate.create();
    }

    /**
     * Creates a UserLogin
     * @param ctx
     *            The DispatchContext that this service is operating in
     * @param context
     *            Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createUserLogin(DispatchContext ctx, LoginServicesContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue loggedInUserLogin = (GenericValue) context.get(x.userLogin);
        List<String> errorMessageList = new LinkedList<>();
        Locale locale = (Locale) context.get(x.locale);

        boolean useEncryption = x._true.equals(EntityUtilProperties.getPropertyValue(x.security, x.password_encrypt, delegator));

        String userLoginId = (String) context.get(x.userLoginId);
        String partyId = (String) context.get(x.partyId);
        String currentPassword = (String) context.get(x.currentPassword);
        String currentPasswordVerify = (String) context.get(x.currentPasswordVerify);
        String enabled = (String) context.get(x.enabled);
        String passwordHint = (String) context.get(x.passwordHint);
        String requirePasswordChange = (String) context.get(x.requirePasswordChange);
        String externalAuthId = (String) context.get(x.externalAuthId);
        String errMsg = null;

        // security: don't create a user login if the specified partyId (if not empty) already exists
        // unless the logged in user has permission to do so (same partyId or PARTYMGR_CREATE)
        if (UtilValidate.isNotEmpty(partyId)) {
            GenericValue party = null;

            try {
                UserLoginDao partyDao = DaoRegistry.getDao(delegator, x.Party, UserLoginDao.class);
                party = partyDao.findOne(delegator, x.Party, UtilMisc.toMap(x.partyId, partyId), false);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, x.emptyString, MODULE);
            }

            if (party != null) {
                if (loggedInUserLogin != null) {
                    // <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
                    if (!partyId.equals(loggedInUserLogin.getString(x.partyId))) {
                        if (!security.hasEntityPermission(x.PARTYMGR, x.CREATE, loggedInUserLogin)) {

                            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_party_with_specified_party_ID_exists_not_have_permission,
                                    locale);
                            errorMessageList.add(errMsg);
                        }
                    }
                } else {
                    errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_must_be_logged_in_and_permission_create_login_party_ID_exists,
                            locale);
                    errorMessageList.add(errMsg);
                }
            }
        }

        GenericValue userLoginToCreate = delegator.makeValue(x.UserLogin, UtilMisc.toMap(x.userLoginId, userLoginId));
        checkNewPassword(userLoginToCreate, null, currentPassword, currentPasswordVerify, passwordHint, errorMessageList, true, locale);
        userLoginToCreate.set(x.externalAuthId, externalAuthId);
        userLoginToCreate.set(x.passwordHint, passwordHint);
        userLoginToCreate.set(x.enabled, enabled);
        userLoginToCreate.set(x.requirePasswordChange, requirePasswordChange);
        userLoginToCreate.set(x.currentPassword, useEncryption ? HashCrypt.cryptUTF8(getHashType(), null, currentPassword) : currentPassword);
        try {
            userLoginToCreate.set(x.partyId, partyId);
        } catch (Exception e) {
            // Will get thrown in framework-only installation
            Debug.logInfo(e, x.Exception_thrown_while_setting_UserLogin_partyId_field, MODULE);
        }

        try {
            EntityCondition condition = EntityCondition.makeCondition(EntityFunction.upperField(x.userLoginId), EntityOperator.EQUALS,
                    EntityFunction.upper(userLoginId));
            UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
            if (UtilValidate.isNotEmpty(userLoginDao.findByCondition(delegator, x.UserLogin, condition, null, null, null, false))) {
                Map<String, String> messageMap = UtilMisc.toMap(x.userLoginId, userLoginId);
                errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_create_login_user_with_ID_exists, messageMap, locale);
                errorMessageList.add(errMsg);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, x.emptyString, MODULE);
            Map<String, String> messageMap = UtilMisc.toMap(x.errorMessage, e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_create_login_user_read_failure, messageMap, locale);
            errorMessageList.add(errMsg);
        }

        if (!errorMessageList.isEmpty()) {
            return ServiceUtil.returnError(errorMessageList);
        }

        try {
            userLoginToCreate.create();
            createUserLoginPasswordHistory(userLoginToCreate);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, x.emptyString, MODULE);
            Map<String, String> messageMap = UtilMisc.toMap(x.errorMessage, e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_create_login_user_write_failure, messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates UserLogin Password info
     * @param ctx
     *            The DispatchContext that this service is operating in
     * @param context
     *            Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updatePassword(DispatchContext ctx, LoginServicesContext context) {
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue loggedInUserLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);
        Map<String, Object> result = ServiceUtil
                .returnSuccess(UtilProperties.getMessage(RESOURCE, x.loginevents_password_was_changed_with_success, locale));

        // load the external auth modules -- note: this will only run once and cache the objects
        if (!AuthHelper.authenticatorsLoaded()) {
            AuthHelper.loadAuthenticators(ctx.getDispatcher());
        }

        boolean useEncryption = x._true.equals(EntityUtilProperties.getPropertyValue(x.security, x.password_encrypt, delegator));
        boolean adminUser = false;

        String userLoginId = (String) context.get(x.userLoginId);
        String errMsg = null;

        if (UtilValidate.isEmpty(userLoginId)) {
            userLoginId = loggedInUserLogin.getString(x.userLoginId);
        }

        GenericValue userLoginToUpdate;

        try {
            UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
            userLoginToUpdate = userLoginDao.findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, userLoginId), false);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap(x.errorMessage, e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_change_password_read_failure, messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        // <b>security check</b>: userLogin userLoginId must equal userLoginId, or must have PARTYMGR_UPDATE permission
        // NOTE: must check permission first so that admin users can set own password without specifying old password
        // TODO: change this security group because we can't use permission groups defined in the applications from the framework.
        if (!security.hasEntityPermission(x.PARTYMGR, x.UPDATE_f97c688e, loggedInUserLogin)) {
            if (!userLoginId.equals(loggedInUserLogin.getString(x.userLoginId))) {
                errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_not_have_permission_update_password_for_user_login, locale);
                return ServiceUtil.returnError(errMsg);
            }
            if (UtilValidate.isNotEmpty(context.get(x.login_token))) {
                adminUser = SecurityUtil.authenticateUserLoginByJWT(delegator, userLoginId, (String) context.get(x.login_token));
            }
        } else {
            adminUser = true;
        }

        String currentPassword = (String) context.get(x.currentPassword);
        String newPassword = (String) context.get(x.newPassword);
        String newPasswordVerify = (String) context.get(x.newPasswordVerify);
        String passwordHint = (String) context.get(x.passwordHint);

        if (userLoginToUpdate == null) {
            // this may be a full external authenticator; first try authenticating
            boolean authenticated = false;
            try {
                authenticated = AuthHelper.authenticate(userLoginId, currentPassword, true);
            } catch (AuthenticatorException e) {
                // safe to ignore this; but we'll log it just in case
                Debug.logWarning(e, e.getMessage(), MODULE);
            }

            // call update password if auth passed
            if (authenticated) {
                try {
                    AuthHelper.updatePassword(userLoginId, currentPassword, newPassword);
                } catch (AuthenticatorException e) {
                    Debug.logError(e, e.getMessage(), MODULE);
                    Map<String, String> messageMap = UtilMisc.toMap(x.userLoginId, userLoginId);
                    errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_change_password_userlogin_with_id_not_exist, messageMap,
                            locale);
                    return ServiceUtil.returnError(errMsg);
                }
                // result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                result.put(x.updatedUserLogin, null);
                return result;
            }
            Map<String, String> messageMap = UtilMisc.toMap(x.userLoginId, userLoginId);
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_change_password_userlogin_with_id_not_exist, messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (x._true.equals(EntityUtilProperties.getPropertyValue(x.security, x.password_lowercase, delegator))) {
            currentPassword = currentPassword.toLowerCase(Locale.getDefault());
            newPassword = newPassword.toLowerCase(Locale.getDefault());
            newPasswordVerify = newPasswordVerify.toLowerCase(Locale.getDefault());
        }

        List<String> errorMessageList = new LinkedList<>();
        if (newPassword != null) {
            checkNewPassword(userLoginToUpdate, currentPassword, newPassword, newPasswordVerify, passwordHint, errorMessageList, adminUser, locale);
        }

        if (!errorMessageList.isEmpty()) {
            return ServiceUtil.returnError(errorMessageList);
        }

        String externalAuthId = userLoginToUpdate.getString(x.externalAuthId);
        if (UtilValidate.isNotEmpty(externalAuthId)) {
            // external auth is set; don't update the database record
            try {
                AuthHelper.updatePassword(externalAuthId, currentPassword, newPassword);
            } catch (AuthenticatorException e) {
                Debug.logError(e, e.getMessage(), MODULE);
                Map<String, String> messageMap = UtilMisc.toMap(x.errorMessage, e.getMessage());
                errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_change_password_write_failure, messageMap, locale);
                return ServiceUtil.returnError(errMsg);
            }
        } else {
            userLoginToUpdate.set(x.currentPassword, useEncryption ? HashCrypt.cryptUTF8(getHashType(), null, newPassword) : newPassword, false);
            userLoginToUpdate.set(x.passwordHint, passwordHint, false);
            // optional parameter in service definition "requirePasswordChange" to update a password to a new generated value that has to be changed
            // by the user
            userLoginToUpdate.set(x.requirePasswordChange, (x.Y.equals(context.get(x.requirePasswordChange)) ? x.Y : x.N));

            try {
                userLoginToUpdate.store();
                createUserLoginPasswordHistory(userLoginToUpdate);
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap(x.errorMessage, e.getMessage());
                errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_change_password_write_failure, messageMap, locale);
                return ServiceUtil.returnError(errMsg);
            }
        }

        result.put(x.updatedUserLogin, userLoginToUpdate);
        return result;
    }

    /**
     * Updates the UserLoginId for a party, replicating password, etc from current login and expiring the old login.
     * @param ctx
     *            The DispatchContext that this service is operating in
     * @param context
     *            Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateUserLoginId(DispatchContext ctx, LoginServicesContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        Delegator delegator = ctx.getDelegator();
        GenericValue loggedInUserLogin = (GenericValue) context.get(x.userLogin);
        List<String> errorMessageList = new LinkedList<>();
        Locale locale = (Locale) context.get(x.locale);

        String userLoginId = (String) context.get(x.userLoginId);
        String errMsg = null;

        if ((userLoginId != null) && (x._true.equals(EntityUtilProperties.getPropertyValue(x.security, x.username_lowercase, delegator)))) {
            userLoginId = userLoginId.toLowerCase(Locale.getDefault());
        }

        String partyId = loggedInUserLogin.getString(x.partyId);
        String password = loggedInUserLogin.getString(x.currentPassword);
        String passwordHint = loggedInUserLogin.getString(x.passwordHint);

        // security: don't create a user login if the specified partyId (if not empty) already exists
        // unless the logged in user has permission to do so (same partyId or PARTYMGR_CREATE)
        if (UtilValidate.isNotEmpty(partyId)) {
            if (!loggedInUserLogin.isEmpty()) {
                // security check: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
                if (!partyId.equals(loggedInUserLogin.getString(x.partyId))) {
                    errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_party_with_party_id_exists_not_permission_create_user_login, locale);
                    errorMessageList.add(errMsg);
                }
            } else {
                errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_must_logged_in_have_permission_create_user_login_exists, locale);
                errorMessageList.add(errMsg);
            }
        }

        GenericValue newUserLogin = null;
        boolean doCreate = true;

        // check to see if there's a matching login and use it if it's for the same party
        try {
            UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
            newUserLogin = userLoginDao.findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, userLoginId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, x.emptyString, MODULE);
            Map<String, String> messageMap = UtilMisc.toMap(x.errorMessage, e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_create_login_user_read_failure, messageMap, locale);
            errorMessageList.add(errMsg);
        }

        if (newUserLogin != null) {
            if (!newUserLogin.get(x.partyId).equals(partyId)) {
                Map<String, String> messageMap = UtilMisc.toMap(x.userLoginId, userLoginId);
                errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_create_login_user_with_ID_exists, messageMap, locale);
                errorMessageList.add(errMsg);
            } else {
                doCreate = false;
            }
        } else {
            newUserLogin = delegator.makeValue(x.UserLogin, UtilMisc.toMap(x.userLoginId, userLoginId));
        }

        newUserLogin.set(x.passwordHint, passwordHint);
        newUserLogin.set(x.partyId, partyId);
        newUserLogin.set(x.currentPassword, password);
        newUserLogin.set(x.enabled, x.Y);
        newUserLogin.set(x.disabledDateTime, null);

        if (!errorMessageList.isEmpty()) {
            return ServiceUtil.returnError(errorMessageList);
        }

        try {
            if (doCreate) {
                newUserLogin.create();
            } else {
                newUserLogin.store();
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, x.emptyString, MODULE);
            Map<String, String> messageMap = UtilMisc.toMap(x.errorMessage, e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_create_login_user_write_failure, messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        // Deactivate 'old' UserLogin and do not set disabledDateTime here, otherwise the 'old' UserLogin would be reenabled by next login
        loggedInUserLogin.set(x.enabled, x.N);
        loggedInUserLogin.set(x.disabledDateTime, null);

        try {
            loggedInUserLogin.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, x.emptyString, MODULE);
            Map<String, String> messageMap = UtilMisc.toMap(x.errorMessage, e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_disable_old_login_user_write_failure, messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        result.put(x.newUserLogin, newUserLogin);
        return result;
    }

    /**
     * Updates UserLogin Security info
     * @param ctx
     *            The DispatchContext that this service is operating in
     * @param context
     *            Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateUserLoginSecurity(DispatchContext ctx, LoginServicesContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue loggedInUserLogin = (GenericValue) context.get(x.userLogin);
        Locale locale = (Locale) context.get(x.locale);

        String userLoginId = (String) context.get(x.userLoginId);
        String errMsg = null;

        if (UtilValidate.isEmpty(userLoginId)) {
            userLoginId = loggedInUserLogin.getString(x.userLoginId);
        }

        // <b>security check</b>: must have PARTYMGR_UPDATE permission
        if (!security.hasEntityPermission(x.PARTYMGR, x.UPDATE_f97c688e, loggedInUserLogin)
                && !security.hasEntityPermission(x.SECURITY, x.UPDATE_f97c688e, loggedInUserLogin)) {
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_not_permission_update_security_info_for_user_login, locale);
            return ServiceUtil.returnError(errMsg);
        }

        GenericValue userLoginToUpdate = null;

        try {
            UserLoginDao userLoginDao = DaoRegistry.getDao(delegator, x.UserLogin, UserLoginDao.class);
            userLoginToUpdate = userLoginDao.findOne(delegator, x.UserLogin, UtilMisc.toMap(x.userLoginId, userLoginId), false);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap(x.errorMessage, e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_change_password_read_failure, messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (userLoginToUpdate == null) {
            Map<String, String> messageMap = UtilMisc.toMap(x.userLoginId, userLoginId);
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_change_password_userlogin_with_id_not_exist, messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        boolean wasEnabled = !x.N.equals(userLoginToUpdate.get(x.enabled));

        if (context.containsKey(x.enabled)) {
            userLoginToUpdate.set(x.enabled, context.get(x.enabled), true);
        }
        if (context.containsKey(x.disabledDateTime)) {
            userLoginToUpdate.set(x.disabledDateTime, context.get(x.disabledDateTime), true);
        }
        if (context.containsKey(x.successiveFailedLogins)) {
            userLoginToUpdate.set(x.successiveFailedLogins, context.get(x.successiveFailedLogins), true);
        }
        if (context.containsKey(x.externalAuthId)) {
            userLoginToUpdate.set(x.externalAuthId, context.get(x.externalAuthId), true);
        }
        if (context.containsKey(x.userLdapDn)) {
            userLoginToUpdate.set(x.userLdapDn, context.get(x.userLdapDn), true);
        }
        if (context.containsKey(x.requirePasswordChange)) {
            userLoginToUpdate.set(x.requirePasswordChange, context.get(x.requirePasswordChange), true);
        }

        // if was disabled and we are enabling it, clear disabledDateTime
        if (!wasEnabled && x.Y.equals(context.get(x.enabled))) {
            userLoginToUpdate.set(x.disabledDateTime, null);
            userLoginToUpdate.set(x.disabledBy, null);
        }

        if (x.N.equals(context.get(x.enabled))) {
            userLoginToUpdate.set(x.disabledBy, loggedInUserLogin.getString(x.userLoginId));
        }

        try {
            userLoginToUpdate.store();
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap(x.errorMessage, e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_could_not_change_password_write_failure, messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static void checkNewPassword(GenericValue userLogin, String currentPassword, String newPassword, String newPasswordVerify,
            String passwordHint, List<String> errorMessageList, boolean ignoreCurrentPassword, Locale locale) {
        Delegator delegator = userLogin.getDelegator();
        boolean useEncryption = x._true.equals(EntityUtilProperties.getPropertyValue(x.security, x.password_encrypt, delegator));

        String errMsg = null;

        // if it's a system account (aka adminUser) don't bother checking the passwords
        if (!ignoreCurrentPassword) {
            // if the password.accept.encrypted.and.plain property in security is set to true allow plain or encrypted passwords
            boolean passwordMatches = checkPassword(userLogin.getString(x.currentPassword), useEncryption, currentPassword);
            if ((currentPassword == null) || (!passwordMatches)) {
                errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_old_password_not_correct_reenter, locale);
                errorMessageList.add(errMsg);
            }
            if (checkPassword(userLogin.getString(x.currentPassword), useEncryption, newPassword)) {
                errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_new_password_is_equal_to_old_password, locale);
                errorMessageList.add(errMsg);
            }

        }

        if (UtilValidate.isEmpty(newPassword) || UtilValidate.isEmpty(newPasswordVerify)) {
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_password_or_verify_missing, locale);
            errorMessageList.add(errMsg);
        } else if (!newPassword.equals(newPasswordVerify)) {
            errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_password_did_not_match_verify_password, locale);
            errorMessageList.add(errMsg);
        }

        int passwordChangeHistoryLimit = 0;
        try {
            passwordChangeHistoryLimit = EntityUtilProperties.getPropertyAsInteger(x.security, x.password_change_history_limit, 0);
        } catch (NumberFormatException nfe) {
            // No valid value is found so don't bother to save any password history
            passwordChangeHistoryLimit = 0;
        }
        Debug.logInfo(x.password_change_history_limit_is_set_to + passwordChangeHistoryLimit, MODULE);
        if (passwordChangeHistoryLimit > 0) {
            Debug.logInfo(x.checkNewPassword_Checking_if_user_is_tyring_to_use_old_password + passwordChangeHistoryLimit, MODULE);
            try {
                UserLoginDao userLoginPasswordHistoryDao = DaoRegistry.getDao(delegator, x.UserLoginPasswordHistory, UserLoginDao.class);
                List<GenericValue> pwdHistList = userLoginPasswordHistoryDao.findByAnd(delegator, x.UserLoginPasswordHistory,
                        UtilMisc.toMap(x.userLoginId, userLogin.getString(x.userLoginId)), UtilMisc.toList(x.fromDate_f5440273), false);
                for (GenericValue pwdHistValue : pwdHistList) {
                    if (checkPassword(pwdHistValue.getString(x.currentPassword), useEncryption, newPassword)) {
                        Map<String, Integer> messageMap = UtilMisc.toMap(x.passwordChangeHistoryLimit, passwordChangeHistoryLimit);
                        errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_password_must_be_different_from_last_passwords, messageMap,
                                locale);
                        errorMessageList.add(errMsg);
                        break;
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, x.emptyString, MODULE);
                Map<String, String> messageMap = UtilMisc.toMap(x.errorMessage, e.getMessage());
                errMsg = UtilProperties.getMessage(RESOURCE, x.loginevents_error_accessing_password_change_history, messageMap, locale);
            }
        }
        int minPasswordLength = 0;

        try {
            minPasswordLength = EntityUtilProperties.getPropertyAsInteger(x.security, x.password_length_min, 0);
        } catch (NumberFormatException nfe) {
            minPasswordLength = 0;
        }

        if (newPassword != null) {
            // Matching password with pattern
            String passwordPattern = EntityUtilProperties.getPropertyValue(x.security, x.security_login_password_pattern, x._5_fb93d267,
                    delegator);
            boolean usePasswordPattern = UtilProperties.getPropertyAsBoolean(x.security, x.security_login_password_pattern_enable, true);
            if (usePasswordPattern) {
                Pattern pattern = Pattern.compile(passwordPattern);
                Matcher matcher = pattern.matcher(newPassword);
                boolean matched = matcher.matches();
                if (!matched) {
                    // This is a mix to handle the OOTB pattern which is only a fixed length
                    Map<String, String> messageMap = UtilMisc.toMap(x.minPasswordLength, Integer.toString(minPasswordLength));
                    String passwordPatternMessage = EntityUtilProperties.getPropertyValue(x.security, x.security_login_password_pattern_description,
                            x.loginservices_password_must_be_least_characters_long, delegator);
                    errMsg = UtilProperties.getMessage(RESOURCE, passwordPatternMessage, messageMap, locale);
                    errorMessageList.add(errMsg);
                }
            } else {
                if (!(newPassword.length() >= minPasswordLength)) {
                    Map<String, String> messageMap = UtilMisc.toMap(x.minPasswordLength, Integer.toString(minPasswordLength));
                    errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_password_must_be_least_characters_long, messageMap, locale);
                    errorMessageList.add(errMsg);
                }
            }
            if (newPassword.equalsIgnoreCase(userLogin.getString(x.userLoginId))) {
                errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_password_may_not_equal_username, locale);
                errorMessageList.add(errMsg);
            }
            if (UtilValidate.isNotEmpty(passwordHint)
                    && (passwordHint.toUpperCase(Locale.getDefault()).indexOf(newPassword.toUpperCase(Locale.getDefault())) >= 0)) {
                errMsg = UtilProperties.getMessage(RESOURCE, x.loginservices_password_hint_may_not_contain_password, locale);
                errorMessageList.add(errMsg);
            }
        }
    }

    public static String getHashType() {
        String hashType = UtilProperties.getPropertyValue(x.security, x.password_encrypt_hash_type);

        if (UtilValidate.isEmpty(hashType)) {
            Debug.logWarning(x.Password_encrypt_hash_type_is_not_specified_in_security_properties_use_SHA, MODULE);
            hashType = x.SHA;
        }

        return hashType;
    }

    public static boolean checkPassword(String oldPassword, boolean useEncryption, String currentPassword) {
        boolean passwordMatches = false;
        if (oldPassword != null) {
            if (useEncryption) {
                passwordMatches = HashCrypt.comparePassword(oldPassword, getHashType(), currentPassword);
            } else {
                passwordMatches = oldPassword.equals(currentPassword);
            }
        }
        if (!passwordMatches && x._true.equals(UtilProperties.getPropertyValue(x.security, x.password_accept_encrypted_and_plain))) {
            passwordMatches = currentPassword.equals(oldPassword);
        }
        return passwordMatches;
    }

    private static boolean tomcatSSOLogin(HttpServletRequest request, String userName, String currentPassword) {
        try {
            request.login(userName, currentPassword);
        } catch (ServletException e) {

            StringManager sm = StringManager.getManager(x.org_apache_catalina_connector);
            if (sm.getString(x.coyoteRequest_alreadyAuthenticated).equals(e.getMessage())) {
                return true;
            } else {
                Debug.logError(e, MODULE);
                return false;
            }
        }
        return true;
    }
}
