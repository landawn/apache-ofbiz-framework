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

import org.apache.ofbiz.base.start.OfbizRuntime;
import org.apache.ofbiz.base.start.StartupException;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Starts/stops OFBiz containers under Spring Boot lifecycle management.
 */
@Component
public class OfbizRuntimeLifecycle implements SmartLifecycle {
    private volatile boolean running;

    @Override
    public void start() {
        try {
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
}
