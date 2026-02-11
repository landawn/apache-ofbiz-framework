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
 * Spring-native product endpoints backed by OFBiz services.
 */
@RestController
@RequestMapping("/api/products")
public class ProductApiController {
    private final OfbizServiceFacade serviceFacade;

    public ProductApiController(OfbizServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findProductById(
            @PathVariable("id") String idToFind,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, null);
        parameters.put("idToFind", idToFind);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "findProductById",
                parameters,
                userLoginId,
                true,
                "GET");
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(
            @RequestBody Map<String, Object> body,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, body);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "createProduct",
                parameters,
                userLoginId,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.CREATED);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable("productId") String productId,
            @RequestBody Map<String, Object> body,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, body);
        parameters.put("productId", productId);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "updateProduct",
                parameters,
                userLoginId,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.OK);
    }
}

