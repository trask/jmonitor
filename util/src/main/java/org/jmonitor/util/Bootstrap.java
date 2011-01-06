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

package org.jmonitor.util;

import java.net.URL;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class Bootstrap {

    private static final String BOOTSTRAP_PROPERTY_FILE_NAME = "jmonitor-bootstrap.properties";

    private static final String CONFIGURATION_FILE_PROPERTY_NAME = "jmonitor.configuration.file";

    private static final String CONFIGURATION_FILE_DEFAULT = "jmonitor.properties";

    private Bootstrap() {
    }

    public static boolean isEmbeddedMode() {
        // TODO auto-detect?
        return true;
    }

    public static String getConfigurationFilename() {

        return getBootstrapProperty(CONFIGURATION_FILE_PROPERTY_NAME, CONFIGURATION_FILE_DEFAULT);
    }

    private static String getBootstrapProperty(String propertyName, String defaultValue) {

        URL bootstrapResource = Bootstrap.class.getResource("/" + BOOTSTRAP_PROPERTY_FILE_NAME);

        if (bootstrapResource == null) {
            return defaultValue;
        }

        // jmonitor-bootstrap.properties is read-only
        Configuration bootstrapConfiguration;
        try {
            bootstrapConfiguration = new PropertiesConfiguration(bootstrapResource);
        } catch (ConfigurationException e) {
            throw new IllegalStateException(e);
        }

        String filename = bootstrapConfiguration.getString(propertyName);

        return StringUtils.defaultIfEmpty(filename, defaultValue);
    }
}
