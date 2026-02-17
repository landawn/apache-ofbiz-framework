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
package org.apache.ofbiz.spring;

import java.util.Arrays;

import org.apache.ofbiz.base.start.OfbizRuntime;
import org.apache.ofbiz.base.start.StartupException;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Starts/stops OFBiz containers under Spring Boot lifecycle management.
 * <p>
 * Supports Spring Boot profiles to select the OFBiz loader mode:
 * <ul>
 *   <li>{@code loaddata} profile → sets {@code ofbiz.start.loaders=load-data}</li>
 *   <li>{@code test} profile → sets {@code ofbiz.start.loaders=test}</li>
 *   <li>default → uses {@code spring} loader</li>
 * </ul>
 */
@Component
public class OfbizRuntimeLifecycle implements SmartLifecycle {
    private volatile boolean running;
    private final Environment environment;

    public OfbizRuntimeLifecycle(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void start() {
        try {
            applyProfileOverrides();
            OfbizRuntime.getInstance().start();
            running = true;
        } catch (StartupException e) {
            throw new IllegalStateException("Unable to start OFBiz runtime", e);
        }
    }

    @Override
    public void stop() {
        OfbizRuntime.getInstance().stop();
        running = false;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running && OfbizRuntime.getInstance().isStarted();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    /**
     * Maps active Spring Boot profiles to OFBiz loader system properties.
     */
    private void applyProfileOverrides() {
        String ofbizHome = environment.getProperty("ofbiz.home", ".");
        System.setProperty("ofbiz.home", ofbizHome);

        String[] profiles = environment.getActiveProfiles();
        if (Arrays.asList(profiles).contains("loaddata")) {
            System.setProperty("ofbiz.start.loaders", "load-data");
        } else if (Arrays.asList(profiles).contains("test")) {
            System.setProperty("ofbiz.start.loaders", "test");
        }
        // Also pick up ofbiz.start.loaders from application.yml if set
        String loaders = environment.getProperty("ofbiz.start.loaders");
        if (loaders != null && !loaders.isEmpty()) {
            System.setProperty("ofbiz.start.loaders", loaders);
        }
    }
}
