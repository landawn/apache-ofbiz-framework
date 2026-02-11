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
 * Spring-native party endpoints backed by OFBiz party services.
 */
@RestController
@RequestMapping("/api/parties")
public class PartyApiController {
    private final OfbizServiceFacade serviceFacade;

    public PartyApiController(OfbizServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    @PostMapping("/persons")
    public ResponseEntity<Map<String, Object>> createPerson(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, body);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "createPerson",
                parameters,
                null,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.CREATED);
    }

    @PutMapping("/persons/{partyId}")
    public ResponseEntity<Map<String, Object>> updatePerson(
            @PathVariable("partyId") String partyId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, body);
        parameters.put("partyId", partyId);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "updatePerson",
                parameters,
                userLoginId,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.OK);
    }

    @PostMapping("/groups")
    public ResponseEntity<Map<String, Object>> createPartyGroup(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, body);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "createPartyGroup",
                parameters,
                null,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.CREATED);
    }

    @PutMapping("/groups/{partyId}")
    public ResponseEntity<Map<String, Object>> updatePartyGroup(
            @PathVariable("partyId") String partyId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, body);
        parameters.put("partyId", partyId);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "updatePartyGroup",
                parameters,
                userLoginId,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.OK);
    }

    @GetMapping("/lookup/email")
    public ResponseEntity<Map<String, Object>> findPartyFromEmail(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, null);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "findPartyFromEmailAddress",
                parameters,
                userLoginId,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.OK);
    }

    @GetMapping("/lookup/telephone")
    public ResponseEntity<Map<String, Object>> findPartyFromTelephone(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestHeader(name = "X-UserLoginId", required = false) String userLoginId) {
        final Map<String, Object> parameters = serviceFacade.mergeParams(queryParams, null);
        final Map<String, Object> serviceResult = serviceFacade.invokeService(
                "findPartyFromTelephone",
                parameters,
                userLoginId,
                false,
                null);
        return ApiResponseBuilder.fromServiceResult(serviceResult, HttpStatus.OK);
    }
}

