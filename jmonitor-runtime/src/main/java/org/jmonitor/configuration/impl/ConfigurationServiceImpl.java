/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jmonitor.configuration.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jmonitor.collector.client.CollectorServiceFactory;
import org.jmonitor.configuration.service.ConfigurationService;
import org.jmonitor.configuration.service.model.AgentConfiguration;
import org.jmonitor.configuration.service.model.CollectorConfiguration;
import org.jmonitor.configuration.service.model.FullConfiguration;
import org.jmonitor.configuration.service.model.MetricConfiguration;
import org.jmonitor.configuration.service.model.ProbeConfiguration;
import org.jmonitor.configuration.service.model.UiConfiguration;
import org.jmonitor.configuration.shared.ConfigurationImplHelper;
import org.jmonitor.configuration.shared.model.AgentConfigurationImpl;
import org.jmonitor.configuration.shared.model.CollectorConfigurationImpl;
import org.jmonitor.configuration.shared.model.MetricConfigurationImpl;
import org.jmonitor.configuration.shared.model.ProbeConfigurationImpl;
import org.jmonitor.configuration.shared.model.UiConfigurationImpl;
import org.jmonitor.util.Bootstrap;
import org.jmonitor.util.ConfigurationUtils;
import org.jmonitor.util.EncryptionUtils;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO should this service only allow updates to full configuration?
// then we could store only full configuration here and make atomic updates
// instead of piece meal updates
// or we could still allow updates to individual portions, but just create
// and store new full configuration each time
// also need to version full(?) configuration and prevent clobbering
public final class ConfigurationServiceImpl implements ConfigurationService {

    private static final String CONFIGURATION_DEFAULTS_RESOURCE_NAME =
            "jmonitor-defaults.properties";

    private static final ConfigurationServiceImpl INSTANCE = new ConfigurationServiceImpl();

    private volatile AgentConfiguration agentConfiguration;
    private volatile ProbeConfiguration probeConfiguration;
    private volatile MetricConfiguration metricConfiguration;
    private volatile CollectorConfiguration collectorConfiguration;
    private volatile UiConfiguration uiConfiguration;

    private final FullConfiguration fullConfiguration = new FullConfiguration() {
        public AgentConfiguration getAgentConfiguration() {
            return ConfigurationServiceImpl.this.getAgentConfiguration();
        }
        public ProbeConfiguration getProbeConfiguration() {
            return ConfigurationServiceImpl.this.getProbeConfiguration();
        }
        public MetricConfiguration getMetricConfiguration() {
            return ConfigurationServiceImpl.this.getMetricConfiguration();
        }
        public CollectorConfiguration getCollectorConfiguration() {
            return ConfigurationServiceImpl.this.getCollectorConfiguration();
        }
        public UiConfiguration getUiConfiguration() {
            return ConfigurationServiceImpl.this.getUiConfiguration();
        }
    };

    // used to prevent concurrent access to the configuration file
    private final Object lock = new Object();

    private ConfigurationServiceImpl() {
    }

    public AgentConfiguration getAgentConfiguration() {
        if (agentConfiguration == null) {
            // thread-safe, not ideal, but ok if multiple threads call this method concurrently
            // (it has internal synchronization to prevent configuration file clobbering)
            agentConfiguration = loadAgentConfiguration();
        }
        return agentConfiguration;
    }

    public ProbeConfiguration getProbeConfiguration() {
        if (probeConfiguration == null) {
            // thread-safe, not ideal, but ok if multiple threads call this method concurrently
            // (it has internal synchronization to prevent configuration file clobbering)
            probeConfiguration = loadProbeConfiguration();
        }
        return probeConfiguration;
    }

    public MetricConfiguration getMetricConfiguration() {
        if (metricConfiguration == null) {
            // thread-safe, not ideal, but ok if multiple threads call this method concurrently
            // (it has internal synchronization to prevent configuration file clobbering)
            metricConfiguration = loadMetricConfiguration();
        }
        return metricConfiguration;
    }

    public CollectorConfiguration getCollectorConfiguration() {
        if (collectorConfiguration == null) {
            // thread-safe, not ideal, but ok if multiple threads call this method concurrently
            // (it has internal synchronization to prevent configuration file clobbering)
            collectorConfiguration = loadCollectorConfiguration();
        }
        return collectorConfiguration;
    }

    public UiConfiguration getUiConfiguration() {
        if (uiConfiguration == null) {
            // thread-safe, not ideal, but ok if multiple threads call this method concurrently
            // (it has internal synchronization to prevent configuration file clobbering)
            uiConfiguration = loadUiConfiguration();
        }
        return uiConfiguration;
    }

    public FullConfiguration getFullConfiguration() {
        return fullConfiguration;
    }

    public void updateAgentConfiguration(AgentConfiguration configuration) {

        // save
        ConfigurationUtils.saveIfNecessary(configuration, Bootstrap.getConfigurationFilename(),
                AgentConfiguration.class, "agent");

        AgentConfigurationImpl immutableConfiguration =
                ConfigurationImplHelper.copyOf(configuration);
        immutableConfiguration.makeImmutable();

        // update cached instance
        agentConfiguration = immutableConfiguration;

        // TODO if not embedded push update to agent
        // but only if there were real changes, e.g. don't push when only collector configuration
        // changes
    }

    public void updateProbeConfiguration(ProbeConfiguration configuration) {

        Map<String, String> propertyMap = new HashMap<String, String>();
        for (String probeName : configuration.getProbeNames()) {
            for (String propertyName : configuration.getPropertyNames(probeName)) {
                propertyMap.put(probeName + "." + propertyName, configuration.getProperty(
                        probeName, propertyName));
            }
        }

        // save
        ConfigurationUtils.saveIfNecessary(propertyMap, Bootstrap.getConfigurationFilename(),
                "probe");

        ProbeConfigurationImpl immutableConfiguration =
                ConfigurationImplHelper.copyOf(configuration);
        immutableConfiguration.makeImmutable();

        // update cached instance
        probeConfiguration = immutableConfiguration;

        // TODO if not embedded push update to agent
        // but only if there were real changes, e.g. don't push when only collector configuration
        // changes
    }

    public void updateMetricConfiguration(MetricConfiguration configuration) {

        Map<String, String> propertyMap = new HashMap<String, String>();
        for (String metricName : configuration.getMetricNames()) {
            for (String propertyName : configuration.getPropertyNames(metricName)) {
                propertyMap.put(metricName + "." + propertyName, configuration.getProperty(
                        metricName, propertyName));
            }
        }

        // save
        ConfigurationUtils.saveIfNecessary(propertyMap, Bootstrap.getConfigurationFilename(),
                "metric");

        MetricConfigurationImpl immutableConfiguration =
                ConfigurationImplHelper.copyOf(configuration);
        immutableConfiguration.makeImmutable();

        // update cached instance
        metricConfiguration = immutableConfiguration;

        // TODO if not embedded push update to agent
        // but only if there were real changes, e.g. don't push when only collector configuration
        // changes
    }

    public void updateCollectorConfiguration(CollectorConfiguration configuration) {

        // save
        ConfigurationUtils.saveIfNecessary(configuration, Bootstrap.getConfigurationFilename(),
                CollectorConfiguration.class, "collector");

        CollectorConfigurationImpl immutableConfiguration =
                ConfigurationImplHelper.copyOf(configuration);
        immutableConfiguration.makeImmutable();

        // update cached instance
        collectorConfiguration = immutableConfiguration;

        // notify collector service
        CollectorServiceFactory.getService().updateConfiguration(immutableConfiguration);
    }

    public void updateUiConfiguration(UiConfiguration configuration) {

        // save
        ConfigurationUtils.saveIfNecessary(configuration, Bootstrap.getConfigurationFilename(),
                UiConfiguration.class, "ui");

        UiConfigurationImpl immutableConfiguration = ConfigurationImplHelper.copyOf(configuration);
        immutableConfiguration.makeImmutable();

        // update cached instance
        uiConfiguration = immutableConfiguration;

        // TODO if not embedded push update to agent?
    }

    public void updateFullConfiguration(FullConfiguration configuration) {

        // TODO for now not writing probe configuration to properties file because we want to be
        // able
        // to change application-specific defaults (jmonitor-defaults.properties) and have the new
        // defaults take effect (which wouldn't happen if we write out the probe configuration
        // since then those values will always override the defaults)

        // updateProbeConfiguration(configuration.getProbeConfiguration());
        updateAgentConfiguration(configuration.getAgentConfiguration());
        updateCollectorConfiguration(configuration.getCollectorConfiguration());
        updateUiConfiguration(configuration.getUiConfiguration());
    }

    private AgentConfiguration loadAgentConfiguration() {

        AgentConfigurationImpl configuration = new AgentConfigurationImpl();
        loadConfiguration(configuration, AgentConfiguration.class, "agent");
        return configuration;
    }

    private ProbeConfiguration loadProbeConfiguration() {

        String configurationFilename = Bootstrap.getConfigurationFilename();

        Map<String, String> probeConfigurationMap =
                ConfigurationUtils.load(configurationFilename,
                        CONFIGURATION_DEFAULTS_RESOURCE_NAME, "probe");

        ProbeConfigurationImpl configuration = new ProbeConfigurationImpl();
        for (Map.Entry<String, String> entry : probeConfigurationMap.entrySet()) {
            String probeName = StringUtils.substringBefore(entry.getKey(), ".");
            String propertyName = StringUtils.substringAfter(entry.getKey(), ".");
            configuration.setProperty(probeName, propertyName, entry.getValue());
        }
        return configuration;
    }

    private MetricConfiguration loadMetricConfiguration() {

        String configurationFilename = Bootstrap.getConfigurationFilename();

        Map<String, String> metricConfigurationMap =
                ConfigurationUtils.load(configurationFilename,
                        CONFIGURATION_DEFAULTS_RESOURCE_NAME, "metric");

        MetricConfigurationImpl configuration = new MetricConfigurationImpl();
        for (Map.Entry<String, String> entry : metricConfigurationMap.entrySet()) {
            String metricName = StringUtils.substringBefore(entry.getKey(), ".");
            String propertyName = StringUtils.substringAfter(entry.getKey(), ".");
            configuration.setProperty(metricName, propertyName, entry.getValue());
        }
        return configuration;
    }

    private CollectorConfiguration loadCollectorConfiguration() {

        CollectorConfigurationImpl configuration = new CollectorConfigurationImpl();
        loadConfiguration(configuration, CollectorConfiguration.class, "collector");
        return configuration;
    }

    private UiConfiguration loadUiConfiguration() {

        UiConfigurationImpl configuration = new UiConfigurationImpl();

        String configurationFilename = Bootstrap.getConfigurationFilename();

        synchronized (lock) {
            ConfigurationUtils.load(configuration, configurationFilename,
                    CONFIGURATION_DEFAULTS_RESOURCE_NAME, UiConfiguration.class, "ui");

            // check default admin password
            if (StringUtils.isEmpty(configuration.getAdminPassword())) {
                // TODO constant-ize default password?
                configuration.setAdminPassword(EncryptionUtils.encryptPassword("admin"));
            } else if (EncryptionUtils.isUnencrypted(configuration.getAdminPassword())) {
                // someone modified the password externally, we need to encrypt
                configuration.setAdminPassword(EncryptionUtils.encryptPassword(configuration.getAdminPassword()));
            }
        }

        ConfigurationUtils.saveIfNecessary(configuration, configurationFilename,
                UiConfiguration.class, "ui");

        return configuration;
    }

    private void loadConfiguration(Object configuration, Class<?> declaredClass, String prefix) {

        String configurationFilename = Bootstrap.getConfigurationFilename();

        synchronized (lock) {
            ConfigurationUtils.load(configuration, configurationFilename,
                    CONFIGURATION_DEFAULTS_RESOURCE_NAME, declaredClass, prefix);

            ConfigurationUtils.saveIfNecessary(configuration, configurationFilename, declaredClass,
                    prefix);
        }
    }

    public static ConfigurationServiceImpl getInstance() {
        return INSTANCE;
    }
}
