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

import java.util.Map;

import org.apache.ofbiz.spring.service.OfbizServiceFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring-native order endpoints backed by OFBiz order services.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderApiController {
    private final OfbizServiceFacade serviceFacade;

    public OrderApiController(OfbizServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> findOrders(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, null);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "findOrders",
                parameters,
                userLoginId,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrderHeader(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, body);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "createOrderHeader",
                parameters,
                userLoginId,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.CREATED);
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> updateOrderHeader(
            @PathVariable("orderId") String orderId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, body);
        parameters.put("orderId", orderId);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "updateOrderHeader",
                parameters,
                userLoginId,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.OK);
    }

    @PostMapping("/{orderId}/items/{orderItemSeqId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrderItem(
            @PathVariable("orderId") String orderId,
            @PathVariable("orderItemSeqId") String orderItemSeqId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, body);
        parameters.put("orderId", orderId);
        parameters.put("orderItemSeqId", orderItemSeqId);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "cancelOrderItem",
                parameters,
                userLoginId,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.OK);
    }

    @PostMapping("/{orderId}/items/{orderItemSeqId}/status")
    public ResponseEntity<Map<String, Object>> changeOrderItemStatus(
            @PathVariable("orderId") String orderId,
            @PathVariable("orderItemSeqId") String orderItemSeqId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, body);
        parameters.put("orderId", orderId);
        parameters.put("orderItemSeqId", orderItemSeqId);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "changeOrderItemStatus",
                parameters,
                userLoginId,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.OK);
    }
}

