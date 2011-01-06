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

package org.jmonitor.ui.configuration;

import org.apache.commons.lang.StringUtils;
import org.jmonitor.util.Bootstrap;
import org.jmonitor.util.ConfigurationUtils;
import org.jmonitor.util.EncryptionUtils;
import org.jmonitor.util.UncheckedPropertyUtils;

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
public final class UiConfigurationServiceImpl implements UiConfigurationService {

    private static final String CONFIGURATION_DEFAULTS_RESOURCE_NAME =
            "jmonitor-defaults.properties";

    private static final UiConfigurationServiceImpl INSTANCE = new UiConfigurationServiceImpl();

    private volatile UiConfiguration uiConfiguration;

    // used to prevent concurrent access to the configuration file
    private final Object lock = new Object();

    private UiConfigurationServiceImpl() {
    }

    // returns defensive copy
    public UiConfiguration getUiConfiguration() {
        if (uiConfiguration == null) {
            // thread-safe, not ideal, but ok if multiple threads call this method concurrently
            // (it has internal synchronization to prevent configuration file clobbering)
            uiConfiguration = loadUiConfiguration();
        }
        return copyOf(uiConfiguration);
    }

    public void updateUiConfiguration(UiConfiguration configuration) {

        // save
        ConfigurationUtils.saveIfNecessary(configuration, Bootstrap.getConfigurationFilename(),
                UiConfiguration.class, "ui");

		// clear cached instance, it will be re-loaded on next access
        uiConfiguration = null;
    }

    private UiConfiguration loadUiConfiguration() {

        UiConfiguration configuration = new UiConfiguration();

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

    private static UiConfiguration copyOf(UiConfiguration configuration) {
    
    		UiConfiguration copy = new UiConfiguration();
        // copy over all properties
        UncheckedPropertyUtils.copyProperties(copy, configuration);
        return copy;
    }

    public static UiConfigurationService getInstance() {
        return INSTANCE;
    }
}
