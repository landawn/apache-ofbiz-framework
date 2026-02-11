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

import org.apache.tomcat.util.scan.StandardJarScanner;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot/Tomcat scanning tuning.
 */
@Configuration
public class TomcatScanningConfiguration {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatJarScannerCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            if (context.getJarScanner() instanceof StandardJarScanner standardJarScanner) {
                // Avoid scanning Manifest Class-Path entries from third-party jars.
                // Some jars list sidecar artifacts that are not present in Gradle cache layout.
                standardJarScanner.setScanManifest(false);
            }
        });
    }
}
