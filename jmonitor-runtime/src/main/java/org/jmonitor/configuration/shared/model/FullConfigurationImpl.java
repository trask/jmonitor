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
package org.jmonitor.configuration.shared.model;

import java.io.Serializable;

import org.jmonitor.configuration.service.model.FullConfiguration;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public class FullConfigurationImpl implements FullConfiguration, Serializable {

    private static final long serialVersionUID = 1L;

    private ProbeConfigurationImpl probeConfiguration;
    private AgentConfigurationImpl agentConfiguration;
    private CollectorConfigurationImpl collectorConfiguration;
    private UiConfigurationImpl uiConfiguration;

    public ProbeConfigurationImpl getProbeConfiguration() {
        return probeConfiguration;
    }

    public void setProbeConfiguration(ProbeConfigurationImpl probeConfiguration) {
        this.probeConfiguration = probeConfiguration;
    }

    public AgentConfigurationImpl getAgentConfiguration() {
        return agentConfiguration;
    }

    public void setAgentConfiguration(AgentConfigurationImpl agentConfiguration) {
        this.agentConfiguration = agentConfiguration;
    }

    public CollectorConfigurationImpl getCollectorConfiguration() {
        return collectorConfiguration;
    }

    public void setCollectorConfiguration(CollectorConfigurationImpl collectorConfiguration) {
        this.collectorConfiguration = collectorConfiguration;
    }

    public UiConfigurationImpl getUiConfiguration() {
        return uiConfiguration;
    }

    public void setUiConfiguration(UiConfigurationImpl uiConfiguration) {
        this.uiConfiguration = uiConfiguration;
    }
}
