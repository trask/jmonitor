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
package org.jmonitor.collector.client;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.MethodUtils;
import org.jmonitor.collector.service.CollectorService;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public abstract class CollectorServiceFactory {

    private static final String COLLECTOR_SERVICE_IMPL_CLASS_NAME =
            "org.jmonitor.collector.impl.CollectorServiceImpl";

    // cached for performance
    private static volatile CollectorService service;

    public static CollectorService getService() {
        if (service == null) {
            Class<?> collectorServiceImplClass;
            try {
                collectorServiceImplClass = Class.forName(COLLECTOR_SERVICE_IMPL_CLASS_NAME);
            } catch (ClassNotFoundException e) {
                // serious problem
                throw new IllegalStateException("Error looking up CollectorService", e);
            }
            try {
                service = (CollectorService) MethodUtils.invokeStaticMethod(
                        collectorServiceImplClass, "getInstance", null);
            } catch (NoSuchMethodException e) {
                // serious problem
                throw new IllegalStateException("Error looking up AgentService", e);
            } catch (IllegalAccessException e) {
                // serious problem
                throw new IllegalStateException("Error looking up AgentService", e);
            } catch (InvocationTargetException e) {
                // serious problem
                throw new IllegalStateException("Error looking up AgentService", e);
            }
        }
        return service;
    }
}
