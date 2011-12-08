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
package org.jmonitor.configuration.shared;

import org.jmonitor.configuration.service.model.AgentConfiguration;
import org.jmonitor.configuration.service.model.CollectorConfiguration;
import org.jmonitor.configuration.service.model.FullConfiguration;
import org.jmonitor.configuration.service.model.ProbeConfiguration;
import org.jmonitor.configuration.service.model.UiConfiguration;
import org.jmonitor.configuration.shared.model.AgentConfigurationImpl;
import org.jmonitor.configuration.shared.model.CollectorConfigurationImpl;
import org.jmonitor.configuration.shared.model.FullConfigurationImpl;
import org.jmonitor.configuration.shared.model.ProbeConfigurationImpl;
import org.jmonitor.configuration.shared.model.UiConfigurationImpl;
import org.jmonitor.util.UncheckedPropertyUtils;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
// cannot put these directly inside impl classes (as constructors or statics)
// because the impl classes are used by GWT client and so cannot depend on spring BeanUtils
public class ConfigurationImplHelper {

    public static ProbeConfigurationImpl copyOf(ProbeConfiguration configuration) {
        ProbeConfigurationImpl copy = new ProbeConfigurationImpl();
        for (String probeName : configuration.getProbeNames()) {
            for (String propertyName : configuration.getPropertyNames(probeName)) {
                copy.setProperty(probeName, propertyName, configuration.getProperty(probeName,
                        propertyName));
            }
        }
        return copy;
    }

    public static AgentConfigurationImpl copyOf(AgentConfiguration configuration) {
        AgentConfigurationImpl copy = new AgentConfigurationImpl();
        // copy over all properties
        UncheckedPropertyUtils.copyProperties(copy, configuration);
        return copy;
    }

    public static CollectorConfigurationImpl copyOf(CollectorConfiguration configuration) {
        CollectorConfigurationImpl copy = new CollectorConfigurationImpl();
        // copy over all properties
        UncheckedPropertyUtils.copyProperties(copy, configuration);
        return copy;
    }

    public static UiConfigurationImpl copyOf(UiConfiguration configuration) {
        UiConfigurationImpl copy = new UiConfigurationImpl();
        // copy over all properties
        UncheckedPropertyUtils.copyProperties(copy, configuration);
        return copy;
    }

    public static FullConfigurationImpl copyOf(FullConfiguration configuration) {
        FullConfigurationImpl copy = new FullConfigurationImpl();
        copy.setProbeConfiguration(copyOf(configuration.getProbeConfiguration()));
        copy.setAgentConfiguration(copyOf(configuration.getAgentConfiguration()));
        copy.setCollectorConfiguration(copyOf(configuration.getCollectorConfiguration()));
        copy.setUiConfiguration(copyOf(configuration.getUiConfiguration()));
        return copy;
    }
}
