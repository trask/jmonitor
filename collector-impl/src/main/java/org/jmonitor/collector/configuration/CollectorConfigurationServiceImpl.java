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

package org.jmonitor.collector.configuration;

import org.jmonitor.collector.service.model.CollectorConfiguration;
import org.jmonitor.util.Bootstrap;
import org.jmonitor.util.ConfigurationUtils;
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
public final class CollectorConfigurationServiceImpl implements CollectorConfigurationService {

    private static final String CONFIGURATION_DEFAULTS_RESOURCE_NAME =
            "jmonitor-defaults.properties";

    private static final CollectorConfigurationServiceImpl INSTANCE = new CollectorConfigurationServiceImpl();

    private volatile CollectorConfiguration collectorConfiguration;

    // used to prevent concurrent access to the configuration file
    private final Object lock = new Object();

    private CollectorConfigurationServiceImpl() {
    }

    // returns defensive copy
    public CollectorConfiguration getCollectorConfiguration() {
        if (collectorConfiguration == null) {
            // thread-safe, not ideal, but ok if multiple threads call this method concurrently
            // (it has internal synchronization to prevent configuration file clobbering)
            collectorConfiguration = loadCollectorConfiguration();
        }
        return copyOf(collectorConfiguration);
    }

    public void updateCollectorConfiguration(CollectorConfiguration configuration) {

        // save
        ConfigurationUtils.saveIfNecessary(configuration, Bootstrap.getConfigurationFilename(),
                CollectorConfiguration.class, "collector");

		// clear cached instance, it will be re-loaded on next access
        collectorConfiguration = null;
    }

    private CollectorConfiguration loadCollectorConfiguration() {

        CollectorConfiguration configuration = new CollectorConfiguration();
        String configurationFilename = Bootstrap.getConfigurationFilename();
		
		synchronized (lock) {
		    ConfigurationUtils.load(configuration, configurationFilename,
		            CONFIGURATION_DEFAULTS_RESOURCE_NAME, CollectorConfiguration.class, "collector");
		
		    ConfigurationUtils.saveIfNecessary(configuration, configurationFilename, CollectorConfiguration.class,
		            "collector");
		}
        return configuration;
    }

    private static CollectorConfiguration copyOf(CollectorConfiguration configuration) {
    	
        CollectorConfiguration copy = new CollectorConfiguration();
        // copy over all properties
        UncheckedPropertyUtils.copyProperties(copy, configuration);
        return copy;
    }

    public static CollectorConfigurationServiceImpl getInstance() {
        return INSTANCE;
    }
}
