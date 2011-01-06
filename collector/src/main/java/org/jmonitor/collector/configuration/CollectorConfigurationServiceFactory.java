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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class CollectorConfigurationServiceFactory {

	// TODO break "cyclic" dependency by getting impl class name from
	// META-INF/services/org.jmonitor.collector.configuration.CollectorConfigurationService
	// ??
	private static final String COLLECTOR_CONFIGURATION_SERVICE_IMPL = "org.jmonitor.collector.configuration.impl.CollectorConfigurationImpl";

	// cached for performance
	private static final CollectorConfigurationService SERVICE = createService();

	// utility class
	private CollectorConfigurationServiceFactory() {
	}

	public static CollectorConfigurationService getService() {
		return SERVICE;
	}

	private static CollectorConfigurationService createService() {
        Class<?> agentServiceImplClass;
        try {
            agentServiceImplClass = Class.forName(COLLECTOR_CONFIGURATION_SERVICE_IMPL);
        } catch (ClassNotFoundException e) {
            // serious problem
            throw new IllegalStateException("Error looking up CollectorConfigurationService", e);
        }
        try {
            Method instanceGetterMethod =
                    agentServiceImplClass.getDeclaredMethod("getInstance");
            return (CollectorConfigurationService) instanceGetterMethod.invoke(null);
        } catch (NoSuchMethodException e) {
            // serious problem
            throw new IllegalStateException("Error looking up CollectorConfigurationService", e);
        } catch (IllegalAccessException e) {
            // serious problem
            throw new IllegalStateException("Error looking up CollectorConfigurationService", e);
        } catch (InvocationTargetException e) {
            // serious problem
            throw new IllegalStateException("Error looking up CollectorConfigurationService", e);
        }
	}
}
