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
package org.jmonitor.configuration.service;

import org.jmonitor.configuration.service.model.AgentConfiguration;
import org.jmonitor.configuration.service.model.CollectorConfiguration;
import org.jmonitor.configuration.service.model.FullConfiguration;
import org.jmonitor.configuration.service.model.ProbeConfiguration;
import org.jmonitor.configuration.service.model.UiConfiguration;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public interface ConfigurationService {

    ProbeConfiguration getProbeConfiguration();

    AgentConfiguration getAgentConfiguration();

    CollectorConfiguration getCollectorConfiguration();

    UiConfiguration getUiConfiguration();

    FullConfiguration getFullConfiguration();

    void updateAgentConfiguration(AgentConfiguration configuration);

    void updateCollectorConfiguration(CollectorConfiguration configuration);

    void updateUiConfiguration(UiConfiguration configuration);

    void updateProbeConfiguration(ProbeConfiguration configuration);

    void updateFullConfiguration(FullConfiguration configuration);
}
