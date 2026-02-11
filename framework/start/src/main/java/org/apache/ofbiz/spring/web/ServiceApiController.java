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
package org.apache.ofbiz.spring.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.spring.service.OfbizServiceFacade;
import org.apache.ofbiz.spring.service.OfbizServiceFacade.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generic REST facade for exported OFBiz services.
 */
@RestController
@RequestMapping("/api/services")
public class ServiceApiController {
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;

    private final OfbizServiceFacade serviceFacade;

    public ServiceApiController(OfbizServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listExportedServices(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "200") int limit,
            @RequestParam(name = "namePrefix", required = false) String namePrefix) {
        return ResponseEntity.ok(buildServiceListPayload(true, offset, limit, namePrefix));
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> listAllServices(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "200") int limit,
            @RequestParam(name = "namePrefix", required = false) String namePrefix) {
        return ResponseEntity.ok(buildServiceListPayload(false, offset, limit, namePrefix));
    }

    @GetMapping("/{serviceName}")
    public ResponseEntity<Map<String, Object>> invokeGet(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, null, userLoginId, true, "GET");
    }

    @PostMapping("/{serviceName}")
    public ResponseEntity<Map<String, Object>> invokePost(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, body, userLoginId, true, "POST");
    }

    @PutMapping("/{serviceName}")
    public ResponseEntity<Map<String, Object>> invokePut(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, body, userLoginId, true, "PUT");
    }

    @PatchMapping("/{serviceName}")
    public ResponseEntity<Map<String, Object>> invokePatch(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, body, userLoginId, true, "PATCH");
    }

    @DeleteMapping("/{serviceName}")
    public ResponseEntity<Map<String, Object>> invokeDelete(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, body, userLoginId, true, "DELETE");
    }

    /**
     * Migration bridge endpoint: allows invoking any exported service via POST
     * even if the service has no REST action metadata yet.
     */
    @PostMapping("/{serviceName}/invoke")
    public ResponseEntity<Map<String, Object>> invokePostBridge(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, body, userLoginId, true, null);
    }

    @GetMapping("/all/{serviceName}")
    public ResponseEntity<Map<String, Object>> invokeAnyGet(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, null, userLoginId, false, "GET");
    }

    @PostMapping("/all/{serviceName}")
    public ResponseEntity<Map<String, Object>> invokeAnyPost(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, body, userLoginId, false, "POST");
    }

    @PutMapping("/all/{serviceName}")
    public ResponseEntity<Map<String, Object>> invokeAnyPut(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, body, userLoginId, false, "PUT");
    }

    @PatchMapping("/all/{serviceName}")
    public ResponseEntity<Map<String, Object>> invokeAnyPatch(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, body, userLoginId, false, "PATCH");
    }

    @DeleteMapping("/all/{serviceName}")
    public ResponseEntity<Map<String, Object>> invokeAnyDelete(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, body, userLoginId, false, "DELETE");
    }

    @PostMapping("/all/{serviceName}/invoke")
    public ResponseEntity<Map<String, Object>> invokeAnyPostBridge(
            @PathVariable("serviceName") String serviceName,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        return invokeService(serviceName, queryParams, body, userLoginId, false, null);
    }

    private ResponseEntity<Map<String, Object>> invokeService(
            String serviceName,
            MultiValueMap<String, String> queryParams,
            Map<String, Object> body,
            String userLoginId,
            boolean requireExport,
            String methodConstraint) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, body);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                serviceName,
                parameters,
                userLoginId,
                requireExport,
                methodConstraint);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.OK);
    }

    private Map<String, Object> buildServiceListPayload(
            boolean exportedOnly,
            int offset,
            int limit,
            String namePrefix) {
        final int safeOffset = Math.max(0, offset);
        final int safeLimit = Math.min(MAX_LIMIT, Math.max(1, limit <= 0 ? DEFAULT_LIMIT : limit));
        final String normalizedPrefix = UtilValidate.isEmpty(namePrefix)
                ? null
                : namePrefix.toLowerCase(Locale.ROOT);

        final List<Map<String, Object>> services = new ArrayList<>();
        int total = 0;
        try {
            final DispatchContext dctx = serviceFacade.getDispatchContext();
            final List<String> serviceNames = new ArrayList<>(dctx.getAllServiceNames());
            Collections.sort(serviceNames);
            for (String serviceName : serviceNames) {
                final ModelService model = dctx.getModelService(serviceName);
                if (model == null) {
                    continue;
                }
                if (exportedOnly && !model.isExport()) {
                    continue;
                }
                if (normalizedPrefix != null && !model.getName().toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                    continue;
                }
                total++;
                if (total <= safeOffset) {
                    continue;
                }
                if (services.size() >= safeLimit) {
                    continue;
                }
                services.add(toServiceSummary(model));
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load service metadata", e.getMessage());
        }

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exportedOnly", exportedOnly);
        payload.put("offset", safeOffset);
        payload.put("limit", safeLimit);
        payload.put("count", total);
        payload.put("returned", services.size());
        payload.put("total", total);
        payload.put("services", services);
        return payload;
    }

    private static Map<String, Object> toServiceSummary(ModelService model) {
        final Map<String, Object> service = new LinkedHashMap<>();
        service.put("name", model.getName());
        service.put("action", model.getAction());
        service.put("auth", model.isAuth());
        service.put("engine", model.getEngineName());
        service.put("exported", model.isExport());
        return service;
    }
}

