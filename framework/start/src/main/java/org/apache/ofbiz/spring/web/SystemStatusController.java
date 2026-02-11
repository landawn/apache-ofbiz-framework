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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.start.Config;
import org.apache.ofbiz.base.start.OfbizRuntime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("framework", "ofbiz");
        return result;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        final OfbizRuntime runtime = OfbizRuntime.getInstance();
        final Config config = runtime.getConfig();
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("started", runtime.isStarted());
        result.put("serverState", runtime.getCurrentState().toString());
        result.put("loaders", config == null ? List.of() : new ArrayList<>(config.getLoaders()));
        return result;
    }
}
