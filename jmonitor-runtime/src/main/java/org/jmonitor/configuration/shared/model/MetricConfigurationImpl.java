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

package org.jmonitor.configuration.shared.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jmonitor.configuration.service.model.MetricConfiguration;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class MetricConfigurationImpl implements MetricConfiguration, Serializable {

    private static final long serialVersionUID = 1L;

    private volatile Map<String, Map<String, String>> propertyMaps =
            new HashMap<String, Map<String, String>>();

    // TODO research other strategies for creating immutable objects with lots of properties
    // (builders?)
    private boolean immutable;

    public void makeImmutable() {
        immutable = true;
    }

    // never returns null
    public String getProperty(String metricName, String propertyName) {
        Map<String, String> propertyMap = propertyMaps.get(metricName);
        if (propertyMap == null) {
            return "";
        }
        String propertyValue = propertyMap.get(propertyName);
        if (propertyValue == null) {
            return "";
        } else {
            return propertyValue;
        }
    }

    public Iterable<String> getMetricNames() {
        return propertyMaps.keySet();
    }

    public Iterable<String> getPropertyNames(String metricName) {
        Map<String, String> propertyMap = propertyMaps.get(metricName);
        if (propertyMap == null) {
            return Collections.emptySet();
        } else {
            return propertyMap.keySet();
        }
    }

    public void setProperty(String metricName, String propertyName, String propertyValue) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        synchronized (propertyMaps) {
            Map<String, String> propertyMap = propertyMaps.get(metricName);
            if (propertyMap == null) {
                propertyMap = new HashMap<String, String>();
                propertyMaps.put(metricName, propertyMap);
            }
            propertyMap.put(propertyName, propertyValue);
        }
    }
}
