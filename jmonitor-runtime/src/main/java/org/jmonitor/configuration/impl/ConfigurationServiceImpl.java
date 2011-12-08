/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmonitor.configuration.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jmonitor.collector.client.CollectorServiceFactory;
import org.jmonitor.configuration.service.ConfigurationService;
import org.jmonitor.configuration.service.model.AgentConfiguration;
import org.jmonitor.configuration.service.model.CollectorConfiguration;
import org.jmonitor.configuration.service.model.FullConfiguration;
import org.jmonitor.configuration.service.model.ProbeConfiguration;
import org.jmonitor.configuration.service.model.UiConfiguration;
import org.jmonitor.configuration.shared.ConfigurationImplHelper;
import org.jmonitor.configuration.shared.model.AgentConfigurationImpl;
import org.jmonitor.configuration.shared.model.CollectorConfigurationImpl;
import org.jmonitor.configuration.shared.model.ProbeConfigurationImpl;
import org.jmonitor.configuration.shared.model.UiConfigurationImpl;
import org.jmonitor.util.Bootstrap;
import org.jmonitor.util.ConfigurationUtils;
import org.jmonitor.util.EncryptionUtils;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO should this service only allow updates to full configuration?
// then we could store only full configuration here and make atomic updates
// instead of piece meal updates
// or we could still allow updates to individual portions, but just create
// and store new full configuration each time
// also need to version full(?) configuration and prevent clobbering
public class ConfigurationServiceImpl implements ConfigurationService {

    private static final String CONFIGURATION_DEFAULTS_RESOURCE_NAME =
            "jmonitor-defaults.properties";

    private volatile ProbeConfiguration probeConfiguration;
    private volatile AgentConfiguration agentConfiguration;
    private volatile CollectorConfiguration collectorConfiguration;
    private volatile UiConfiguration uiConfiguration;

    private final FullConfiguration fullConfiguration = new FullConfiguration() {
        public ProbeConfiguration getProbeConfiguration() {
            return ConfigurationServiceImpl.this.getProbeConfiguration();
        }
        public AgentConfiguration getAgentConfiguration() {
            return ConfigurationServiceImpl.this.getAgentConfiguration();
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

    private static final ConfigurationServiceImpl instance = new ConfigurationServiceImpl();

    private ConfigurationServiceImpl() {}

    public ProbeConfiguration getProbeConfiguration() {
        if (probeConfiguration == null) {
            // thread-safe, not ideal, but ok if multiple threads call this method concurrently
            // (it has internal synchronization to prevent configuration file clobbering)
            probeConfiguration = loadProbeConfiguration();
        }
        return probeConfiguration;
    }

    public AgentConfiguration getAgentConfiguration() {
        if (agentConfiguration == null) {
            // thread-safe, not ideal, but ok if multiple threads call this method concurrently
            // (it has internal synchronization to prevent configuration file clobbering)
            agentConfiguration = loadAgentConfiguration();
        }
        return agentConfiguration;
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

    public void updateProbeConfiguration(ProbeConfiguration configuration) {

        // TODO for now not writing probe configuration to properties file because we want to be
        // able
        // to change application-specific defaults (jmonitor-defaults.properties) and have the new
        // defaults take effect (which wouldn't happen if we write out the probe configuration
        // since then those values will always override the defaults)

        // Map<String, String> propertyMap = new HashMap<String, String>();
        // for (String probeName : configuration.getProbeNames()) {
        // for (String propertyName : configuration.getPropertyNames(probeName)) {
        // propertyMap.put(probeName + "." + propertyName, configuration.getProperty(
        // probeName, propertyName));
        // }
        // }
        //
        // // save
        // ConfigurationUtils.saveIfNecessary(propertyMap, Bootstrap.getConfigurationFilename(),
        // "probe");
        //
        // ProbeConfigurationImpl immutableConfiguration =
        // ConfigurationImplHelper.copyOf(configuration);
        // immutableConfiguration.makeImmutable();
        //
        // // update cached instance
        // probeConfiguration = immutableConfiguration;
    }

    public void updateAgentConfiguration(AgentConfiguration configuration) {

        // save
        ConfigurationUtils.saveIfNecessary(configuration, Bootstrap.getConfigurationFilename(),
                AgentConfiguration.class, "agent");

        AgentConfigurationImpl immutableConfiguration = ConfigurationImplHelper
                .copyOf(configuration);
        immutableConfiguration.makeImmutable();

        // update cached instance
        agentConfiguration = immutableConfiguration;
    }

    public void updateCollectorConfiguration(CollectorConfiguration configuration) {

        // save
        ConfigurationUtils.saveIfNecessary(configuration, Bootstrap.getConfigurationFilename(),
                CollectorConfiguration.class, "collector");

        CollectorConfigurationImpl immutableConfiguration = ConfigurationImplHelper
                .copyOf(configuration);
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
    }

    public void updateFullConfiguration(FullConfiguration configuration) {
        updateProbeConfiguration(configuration.getProbeConfiguration());
        updateAgentConfiguration(configuration.getAgentConfiguration());
        updateCollectorConfiguration(configuration.getCollectorConfiguration());
        updateUiConfiguration(configuration.getUiConfiguration());
    }

    public static ConfigurationServiceImpl getInstance() {
        return instance;
    }

    private ProbeConfiguration loadProbeConfiguration() {

        String configurationFilename = Bootstrap.getConfigurationFilename();

        Map<String, String> probeConfigurationMap = ConfigurationUtils.load(configurationFilename,
                CONFIGURATION_DEFAULTS_RESOURCE_NAME, "probe");

        ProbeConfigurationImpl configuration = new ProbeConfigurationImpl();
        for (String propertyNameWithProbePrefix : probeConfigurationMap.keySet()) {
            String probeName = StringUtils.substringBefore(propertyNameWithProbePrefix, ".");
            String propertyName = StringUtils.substringAfter(propertyNameWithProbePrefix, ".");
            String propertyValue = probeConfigurationMap.get(propertyNameWithProbePrefix);
            configuration.setProperty(probeName, propertyName, propertyValue);
        }
        return configuration;
    }

    private AgentConfiguration loadAgentConfiguration() {

        AgentConfigurationImpl configuration = new AgentConfigurationImpl();
        loadConfiguration(configuration, AgentConfiguration.class, "agent");
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
                configuration.setAdminPassword(EncryptionUtils.encryptPassword(configuration
                        .getAdminPassword()));
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
}
