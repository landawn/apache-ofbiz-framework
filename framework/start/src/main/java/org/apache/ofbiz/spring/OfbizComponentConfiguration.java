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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Exposes OFBiz component discovery data as Spring beans for injection.
 * <p>
 * Marked {@link Lazy} because component data is only available after
 * the OFBiz runtime completes its SmartLifecycle startup.
 */
@Configuration
@Lazy
public class OfbizComponentConfiguration {

    @Bean
    public Collection<ComponentConfig> allComponents() {
        return ComponentConfig.getAllComponents();
    }

    @Bean
    public List<WebappInfo> allWebappInfos() {
        return ComponentConfig.getAllWebappResourceInfos();
    }

    @Bean
    public Map<String, WebappInfo> webappInfoByMountPoint() {
        Map<String, WebappInfo> map = new LinkedHashMap<>();
        for (WebappInfo info : ComponentConfig.getAllWebappResourceInfos()) {
            String mount = info.getMountPoint();
            if (mount.endsWith("/*")) {
                mount = mount.substring(0, mount.length() - 2);
            }
            map.putIfAbsent(mount, info);
        }
        return map;
    }
}
