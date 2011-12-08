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
package org.jmonitor.util;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public class ConfigurationUtils {

    public static void load(Object object, String filename, String defaultsResourceName,
            Class<?> declaredClass, String prefix) {

        URL url = ConfigurationUtils.class.getClassLoader().getResource(defaultsResourceName);

        if (url != null) {
            try {
                PropertiesConfiguration defaultConfiguration = new PropertiesConfiguration(url);
                load(object, defaultConfiguration, declaredClass, prefix);
            } catch (ConfigurationException e) {
                throw new IllegalStateException(e);
            }
        }

        File file = new File(filename);
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) {
                    FileUtils.forceMkdir(file.getParentFile());
                }
                file.createNewFile();
            } catch (IOException e) {
                throw new IllegalStateException("Could not create file '" + file.getAbsolutePath()
                        + "'", e);
            }
        }

        PropertiesConfiguration configuration;
        try {
            configuration = new PropertiesConfiguration(filename);
        } catch (ConfigurationException e) {
            throw new IllegalStateException(e);
        }

        load(object, configuration, declaredClass, prefix);
    }

    private static void load(Object object, PropertiesConfiguration configuration,
            Class<?> declaredClass, String prefix) {

        for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(declaredClass)) {

            String propertyName = descriptor.getName();
            Class<?> propertyType = UncheckedPropertyUtils.getPropertyType(object, propertyName);

            if (propertyType.equals(List.class)) {
                List<String> values = new ArrayList<String>();
                for (Object value : configuration.getList(prefix + "." + propertyName)) {
                    values.add((String) value);
                }
                if (values.size() == 1 && values.get(0).equals("")) {
                    values = new ArrayList<String>();
                }
                UncheckedPropertyUtils.setProperty(object, propertyName, values);
            } else {
                if (configuration.getList(prefix + "." + propertyName).size() > 1) {
                    // TODO log warning
                }
                String value = configuration.getString(prefix + "." + propertyName);
                if (StringUtils.isNotEmpty(value)) {
                    UncheckedPropertyUtils.setProperty(object, propertyName, value);
                }
            }
        }
        // TODO log warning with any invalid property names from configuration file with same prefix
    }

    public static void saveIfNecessary(Object object, String filename, Class<?> declaredClass,
            String prefix) {

        PropertiesConfiguration configuration;
        try {
            configuration = new PropertiesConfiguration(filename);
        } catch (ConfigurationException e) {
            throw new IllegalStateException(e);
        }

        boolean saveIsNecessary = false;
        for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(declaredClass)) {

            String propertyName = descriptor.getName();

            Object newValue = UncheckedPropertyUtils.getProperty(object, propertyName);
            if (newValue == null) {

                // null would remove this property from the properties file
                // which is not what we want(?)
                if (descriptor.getPropertyType().equals(String.class)) {
                    newValue = "";
                } else if (descriptor.getPropertyType().equals(List.class)) {
                    // empty list would remove this property from the properties file
                    newValue = Collections.singletonList("");
                } else {
                    throw new IllegalStateException("Unexpected type '"
                            + descriptor.getPropertyType().equals(String.class) + "'");
                }
            }
            if (newValue instanceof List<?> && ((List<?>) newValue).isEmpty()) {
                // empty list would remove this property from the properties file
                newValue = Collections.singletonList("");
            }
            Object oldValue;
            if (descriptor.getPropertyType().equals(List.class)) {
                oldValue = configuration.getList(prefix + "." + propertyName);
            } else {
                oldValue = configuration.getProperty(prefix + "." + propertyName);
                newValue = newValue.toString();
            }
            if (!newValue.equals(oldValue)) {
                configuration.setProperty(prefix + "." + propertyName, newValue);
                // TODO get comment from annotation on AgentConfiguration?
                // Method method = descriptor.getReadMethod();
                // agentConfiguration.getLayout().setComment(descriptor.getName(), "a comment");
                saveIsNecessary = true;
            }
        }

        if (saveIsNecessary) {
            try {
                configuration.save();
            } catch (ConfigurationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static Map<String, String> load(String filename, String defaultsResourceName,
            String prefix) {

        Map<String, String> map = new HashMap<String, String>();

        URL url = ConfigurationUtils.class.getClassLoader().getResource(defaultsResourceName);

        if (url != null) {
            try {
                PropertiesConfiguration defaultConfiguration = new PropertiesConfiguration(url);
                load(map, defaultConfiguration, prefix);
            } catch (ConfigurationException e) {
                throw new IllegalStateException(e);
            }
        }

        PropertiesConfiguration configuration;
        try {
            configuration = new PropertiesConfiguration(filename);
        } catch (ConfigurationException e) {
            throw new IllegalStateException(e);
        }

        load(map, configuration, prefix);

        return map;
    }

    private static void load(Map<String, String> map, PropertiesConfiguration configuration,
            String prefix) {

        Iterator<?> iterator = configuration.getKeys(prefix);
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            map.put(StringUtils.substringAfter(key, prefix + "."), configuration.getString(key));
        }
    }

    public static void saveIfNecessary(Map<String, String> propertyMap, String filename,
            String prefix) {

        PropertiesConfiguration configuration;
        try {
            configuration = new PropertiesConfiguration(filename);
        } catch (ConfigurationException e) {
            throw new IllegalStateException(e);
        }

        boolean saveIsNecessary = false;

        for (String propertyName : propertyMap.keySet()) {
            String oldValue = configuration.getString(prefix + "." + propertyName);
            String newValue = propertyMap.get(propertyName);
            if (!newValue.equals(oldValue)) {
                configuration.setProperty(prefix + "." + propertyName, newValue);
                // TODO enhance somehow with comment from custom probe??
                saveIsNecessary = true;
            }
        }

        if (saveIsNecessary) {
            try {
                configuration.save();
            } catch (ConfigurationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
