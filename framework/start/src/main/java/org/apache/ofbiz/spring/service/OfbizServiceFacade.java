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
package org.apache.ofbiz.spring.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

/**
 * Spring wrapper around OFBiz service dispatcher.
 * <p>
 * Marked {@link Lazy} because the injected Delegator and LocalDispatcher beans
 * depend on the OFBiz runtime being fully started (via SmartLifecycle).
 */
@Component
@Lazy
public class OfbizServiceFacade {
    private final Delegator delegator;
    private final LocalDispatcher dispatcher;

    public OfbizServiceFacade(Delegator delegator, LocalDispatcher dispatcher) {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
    }

    public LocalDispatcher getDispatcher() {
        return dispatcher;
    }

    public DispatchContext getDispatchContext() {
        return dispatcher.getDispatchContext();
    }

    public Map<String, Object> invokeService(
            String serviceName,
            Map<String, Object> parameters,
            String userLoginId,
            boolean requireExport,
            String methodConstraint) {
        try {
            final LocalDispatcher dispatcher = getDispatcher();
            final DispatchContext dctx = dispatcher.getDispatchContext();
            final ModelService model = dctx.getModelService(serviceName);
            if (model == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Service not found", serviceName);
            }
            if (requireExport && !model.isExport()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Service is not exported", serviceName);
            }

            final Map<String, Object> serviceContext = new HashMap<>();
            if (parameters != null) {
                serviceContext.putAll(parameters);
            }

            final String effectiveUserLoginId = resolveUserLoginId(userLoginId, serviceContext);
            if (!requireExport && !model.isExport() && UtilValidate.isEmpty(effectiveUserLoginId)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED,
                        "Invoking non-exported service requires user login",
                        "Provide X-UserLoginId header or userLoginId in request.");
            }
            if (UtilValidate.isNotEmpty(effectiveUserLoginId)) {
                final GenericValue userLogin = lookupUserLogin(dispatcher, effectiveUserLoginId);
                serviceContext.put("userLogin", userLogin);
            } else if (model.isAuth()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED,
                        "Authenticated service requires user login",
                        "Provide X-UserLoginId header or userLoginId in request.");
            }

            if (UtilValidate.isNotEmpty(methodConstraint)) {
                final String action = model.getAction();
                if (UtilValidate.isNotEmpty(action) && !actionMatchesHttpMethod(action, methodConstraint)) {
                    throw new ApiException(HttpStatus.METHOD_NOT_ALLOWED,
                            "HTTP method does not match service action",
                            "expected=" + action + ", actual=" + methodConstraint);
                }
            }

            final List<Object> validationErrors = new LinkedList<>();
            final Map<String, Object> validContext = model.makeValid(serviceContext,
                    ModelService.IN_PARAM,
                    true,
                    validationErrors,
                    TimeZone.getDefault(),
                    Locale.getDefault());
            if (!validationErrors.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid service parameters", validationErrors);
            }

            return dispatcher.runSync(serviceName, validContext);
        } catch (GenericServiceException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Service invocation failed", e.getMessage());
        } catch (GenericEntityException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to resolve user login", e.getMessage());
        }
    }

    public Map<String, Object> mergeParams(MultiValueMap<String, String> queryParams, Map<String, Object> body) {
        final Map<String, Object> context = new HashMap<>();
        if (body != null) {
            context.putAll(body);
        }
        if (queryParams == null) {
            return context;
        }
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            final List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            context.put(entry.getKey(), values.size() == 1 ? values.get(0) : values);
        }
        return context;
    }

    private static String resolveUserLoginId(String userLoginId, Map<String, Object> serviceContext) {
        if (UtilValidate.isNotEmpty(userLoginId)) {
            return userLoginId;
        }
        final Object fromBody = serviceContext.get("userLoginId");
        return fromBody == null ? null : String.valueOf(fromBody);
    }

    private static GenericValue lookupUserLogin(LocalDispatcher dispatcher, String userLoginId) throws GenericEntityException {
        final GenericValue userLogin = EntityQuery.use(dispatcher.getDelegator())
                .from("UserLogin")
                .where("userLoginId", userLoginId)
                .queryOne();
        if (userLogin == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid user login", userLoginId);
        }
        return userLogin;
    }

    private static boolean actionMatchesHttpMethod(String action, String methodConstraint) {
        final String normalizedAction = action.trim().toUpperCase(Locale.ROOT);
        final String normalizedMethod = methodConstraint.trim().toUpperCase(Locale.ROOT);
        if (normalizedAction.equals(normalizedMethod)) {
            return true;
        }
        return switch (normalizedAction) {
        case "VIEW", "FIND", "QUERY", "READ" -> "GET".equals(normalizedMethod);
        case "CREATE", "POST", "STORE" -> "POST".equals(normalizedMethod);
        case "UPDATE", "CHANGE", "SET" -> "PUT".equals(normalizedMethod)
                || "PATCH".equals(normalizedMethod)
                || "POST".equals(normalizedMethod);
        case "DELETE", "REMOVE" -> "DELETE".equals(normalizedMethod);
        default -> false;
        };
    }

    public static final class ApiException extends RuntimeException {
        private final HttpStatus status;
        private final Object detail;

        public ApiException(HttpStatus status, String message, Object detail) {
            super(message);
            this.status = status;
            this.detail = detail;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public Object getDetail() {
            return detail;
        }
    }
}
