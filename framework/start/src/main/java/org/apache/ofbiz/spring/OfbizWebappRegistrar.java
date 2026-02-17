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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.catalina.Host;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.start.OfbizRuntime;
import org.apache.ofbiz.base.util.Debug;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Deploys OFBiz legacy webapps (accounting, ordermgr, partymgr, catalog, etc.)
 * as Tomcat child contexts inside Spring Boot's embedded Tomcat.
 * <p>
 * This replaces the role of CatalinaContainer in the legacy startup path.
 * Each webapp declared in ofbiz-component.xml files is deployed with its own
 * ControlServlet, filters, and listeners as defined in its web.xml.
 * <p>
 * The deployment happens after the web server is initialized but before
 * the application starts accepting requests.
 */
@Component
@ConditionalOnProperty(name = "ofbiz.spring.legacy-webapps.enabled", havingValue = "true")
public class OfbizWebappRegistrar {

    private static final String MODULE = OfbizWebappRegistrar.class.getName();
    private final AtomicBoolean deployed = new AtomicBoolean(false);
    private volatile TomcatWebServer tomcatWebServer;

    @EventListener
    @Order(0)
    public void onWebServerInitialized(WebServerInitializedEvent event) {
        if (!(event.getWebServer() instanceof TomcatWebServer webServer)) {
            Debug.logError("Expected TomcatWebServer but got: "
                    + event.getWebServer().getClass().getName(), MODULE);
            return;
        }
        tomcatWebServer = webServer;
        deployIfReady("WebServerInitializedEvent");
    }

    @EventListener
    @Order(1)
    public void onApplicationReady(ApplicationReadyEvent event) {
        deployIfReady("ApplicationReadyEvent");
    }

    private synchronized void deployIfReady(String trigger) {
        if (deployed.get()) {
            return;
        }
        if (tomcatWebServer == null) {
            Debug.logWarning("Tomcat web server not initialized yet; cannot deploy legacy webapps (" + trigger + ")", MODULE);
            return;
        }
        if (!OfbizRuntime.getInstance().isStarted()) {
            Debug.logInfo("OFBiz runtime not started yet; deferring legacy webapp deployment (" + trigger + ")", MODULE);
            return;
        }

        org.apache.catalina.startup.Tomcat tomcat = tomcatWebServer.getTomcat();
        Host host = (Host) tomcat.getEngine().findChild(tomcat.getEngine().getDefaultHost());
        if (host == null) {
            Debug.logError("Could not find default Tomcat host", MODULE);
            return;
        }
        deployOfbizWebapps(host);
        deployed.set(true);
    }

    private Host resolveHost() {
        org.apache.catalina.startup.Tomcat tomcat = tomcatWebServer.getTomcat();
        Host host = (Host) tomcat.getEngine().findChild(tomcat.getEngine().getDefaultHost());
        return host;
    }

    /**
     * Iterates over all OFBiz component webapp declarations and deploys each
     * as a StandardContext in the given Tomcat Host. Mirrors the logic from
     * CatalinaContainer.loadWebapps() and prepareContext().
     */
    private static void deployOfbizWebapps(Host host) {
        List<ComponentConfig.WebappInfo> webResourceInfos = ComponentConfig.getAllWebappResourceInfos();
        Collections.reverse(webResourceInfos);

        Set<String> loadedMounts = new HashSet<>();
        int deployed = 0;

        for (ComponentConfig.WebappInfo appInfo : webResourceInfos) {
            String mount = getWebappMountPoint(appInfo);
            String key = appInfo.getServer() + ":DEFAULT:" + mount;

            if (!loadedMounts.add(key)) {
                appInfo.setAppBarDisplay(false);
                Debug.logInfo("Duplicate webapp mount (overriding); not loading: "
                        + appInfo.getName() + " / " + appInfo.getLocation(), MODULE);
                continue;
            }

            if (appInfo.getLocation().isEmpty()) {
                continue;
            }

            try {
                StandardContext context = createContext(appInfo, host);
                host.addChild(context);
                deployed++;
                Debug.logInfo("Deployed OFBiz webapp: " + appInfo.getName()
                        + " at " + mount, MODULE);
            } catch (Exception e) {
                Debug.logError(e, "Failed to deploy webapp: " + appInfo.getName(), MODULE);
            }
        }
        Debug.logInfo("Deployed " + deployed + " OFBiz webapps", MODULE);
    }

    /**
     * Creates a StandardContext for a single OFBiz webapp, mirroring
     * CatalinaContainer.prepareContext().
     */
    private static StandardContext createContext(ComponentConfig.WebappInfo appInfo, Host host) {
        StandardContext context = new StandardContext();

        String ofbizHome = System.getProperty("ofbiz.home", ".");
        context.setDefaultWebXml(ofbizHome + "/framework/catalina/config/web.xml");
        Tomcat.initWebappDefaults(context);

        String location = getWebappRootLocation(appInfo);
        context.setParent(host);
        context.setDocBase(location);
        context.setDisplayName(appInfo.getName());
        context.setPath(getWebappMountPoint(appInfo));
        context.addLifecycleListener(new ContextConfig());
        context.setJ2EEApplication("OFBiz");
        context.setJ2EEServer("OFBiz Spring");
        context.setParentClassLoader(Thread.currentThread().getContextClassLoader());
        context.setReloadable(false);
        context.setDistributable(false);
        context.setCrossContext(true);
        context.setPrivileged(appInfo.isPrivileged());
        context.getServletContext().setAttribute("_serverId", appInfo.getServer());
        context.getServletContext().setAttribute("componentName",
                appInfo.getComponentConfig().getComponentName());

        StandardRoot resources = new StandardRoot(context);
        resources.setAllowLinking(true);
        context.setResources(resources);

        if (context.getJarScanner() instanceof StandardJarScanner standardJarScanner) {
            standardJarScanner.setScanManifest(false);
            standardJarScanner.setScanClassPath(true);
        }

        Map<String, String> initParameters = appInfo.getInitParameters();
        initParameters.forEach(context::addParameter);

        return context;
    }

    private static String getWebappRootLocation(ComponentConfig.WebappInfo appInfo) {
        return appInfo.getComponentConfig().rootLocation()
                .resolve(appInfo.getLocation().replace('\\', '/'))
                .normalize()
                .toString();
    }

    private static String getWebappMountPoint(ComponentConfig.WebappInfo appInfo) {
        String mount = appInfo.getMountPoint();
        if (mount.endsWith("/*")) {
            mount = mount.substring(0, mount.length() - 2);
        }
        return mount;
    }
}
