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
package org.jmonitor.configuration.client;

import org.jmonitor.configuration.impl.ConfigurationServiceImpl;
import org.jmonitor.configuration.service.ConfigurationService;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public abstract class ConfigurationServiceFactory {

    // TODO remove cyclic dependency
    public static ConfigurationService getService() {
        return ConfigurationServiceImpl.getInstance();
    }
}
