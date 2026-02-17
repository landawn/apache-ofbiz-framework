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
package org.apache.ofbiz.base.start;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ofbiz.base.container.ContainerLoader;
import org.apache.ofbiz.base.start.Start.ServerState;

/**
 * Programmatic OFBiz runtime bootstrap for Spring Boot.
 */
public final class OfbizRuntime {
    private static final OfbizRuntime INSTANCE = new OfbizRuntime();

    private final ContainerLoader loader = new ContainerLoader();
    private final AtomicReference<ServerState> serverState = new AtomicReference<>(ServerState.STARTING);
    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile Config config;

    private OfbizRuntime() {
    }

    public static OfbizRuntime getInstance() {
        return INSTANCE;
    }

    /**
     * Starts OFBiz containers.
     */
    public synchronized void start() throws StartupException {
        if (started.get()) {
            return;
        }
        // Always use "spring" loader — Spring Boot manages the web server.
        // Data-loading and test modes override this via Spring Boot profiles.
        System.setProperty("ofbiz.start.loaders", System.getProperty("ofbiz.start.loaders", "spring"));
        if (System.getProperty("ofbiz.admin.port") == null) {
            System.setProperty("ofbiz.admin.port", "0");
        }

        serverState.set(ServerState.STARTING);
        final List<StartupCommand> commands = List.of(
                new StartupCommand.Builder(StartupCommandUtil.StartupOption.START.getName()).build());
        config = StartupControlPanel.init(commands);
        Start.getInstance().setConfig(config);
        StartupControlPanel.start(config, serverState, commands, loader);
        started.set(true);
    }

    /**
     * Stops OFBiz containers.
     */
    public synchronized void stop() {
        if (!started.get()) {
            return;
        }
        StartupControlPanel.shutdownServer(loader, serverState);
        started.set(false);
    }

    public boolean isStarted() {
        return started.get();
    }

    public ServerState getCurrentState() {
        return serverState.get();
    }

    public Config getConfig() {
        return config;
    }
}
