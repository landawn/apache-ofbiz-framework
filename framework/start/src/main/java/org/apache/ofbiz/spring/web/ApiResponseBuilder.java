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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ofbiz.service.ServiceUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class ApiResponseBuilder {
    private ApiResponseBuilder() {
    }

    static ResponseEntity<Map<String, Object>> fromServiceResult(
            Map<String, Object> serviceResult,
            HttpStatus successStatus) {
        return fromServiceResult(serviceResult, successStatus, HttpStatus.BAD_REQUEST);
    }

    static ResponseEntity<Map<String, Object>> fromServiceResult(
            Map<String, Object> serviceResult,
            HttpStatus successStatus,
            HttpStatus errorStatus) {
        final HttpStatus status = ServiceUtil.isError(serviceResult) || ServiceUtil.isFailure(serviceResult)
                ? errorStatus
                : successStatus;
        return ResponseEntity.status(status).body(serviceResult);
    }

    static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, Object detail) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("responseMessage", "error");
        payload.put("errorMessage", message);
        payload.put("detail", detail);
        return ResponseEntity.status(status).body(payload);
    }
}
