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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed configuration properties for OFBiz, bound from {@code application.yml}
 * under the {@code ofbiz} prefix.
 */
@Component
@ConfigurationProperties(prefix = "ofbiz")
public class OfbizProperties {

    private String home = ".";
    private SpringConfig spring = new SpringConfig();
    private Admin admin = new Admin();
    private Log log = new Log();
    private Entity entity = new Entity();
    private Start start = new Start();

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public SpringConfig getSpring() {
        return spring;
    }

    public void setSpring(SpringConfig spring) {
        this.spring = spring;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Start getStart() {
        return start;
    }

    public void setStart(Start start) {
        this.start = start;
    }

    public static class Admin {
        private String host = "127.0.0.1";
        private int port = 0;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class SpringConfig {
        private LegacyWebapps legacyWebapps = new LegacyWebapps();

        public LegacyWebapps getLegacyWebapps() {
            return legacyWebapps;
        }

        public void setLegacyWebapps(LegacyWebapps legacyWebapps) {
            this.legacyWebapps = legacyWebapps;
        }
    }

    public static class LegacyWebapps {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Log {
        private String dir = "runtime/logs";

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }
    }

    public static class Entity {
        private String delegator = "default";

        public String getDelegator() {
            return delegator;
        }

        public void setDelegator(String delegator) {
            this.delegator = delegator;
        }
    }

    public static class Start {
        private String loaders = "spring";

        public String getLoaders() {
            return loaders;
        }

        public void setLoaders(String loaders) {
            this.loaders = loaders;
        }
    }
}
