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

package org.jmonitor.ui.shared;

import java.io.Serializable;

import org.jmonitor.agent.configuration.AgentConfiguration;
import org.jmonitor.agent.configuration.MetricConfiguration;
import org.jmonitor.agent.configuration.ProbeConfiguration;
import org.jmonitor.collector.service.model.CollectorConfiguration;
import org.jmonitor.ui.configuration.UiConfiguration;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class FullConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    private AgentConfiguration agentConfiguration;
    private ProbeConfiguration probeConfiguration;
    private MetricConfiguration metricConfiguration;
    private CollectorConfiguration collectorConfiguration;
    private UiConfiguration uiConfiguration;

    public AgentConfiguration getAgentConfiguration() {
        return agentConfiguration;
    }

    public void setAgentConfiguration(AgentConfiguration agentConfiguration) {
        this.agentConfiguration = agentConfiguration;
    }

    public ProbeConfiguration getProbeConfiguration() {
        return probeConfiguration;
    }

    public void setProbeConfiguration(ProbeConfiguration probeConfiguration) {
        this.probeConfiguration = probeConfiguration;
    }

    public MetricConfiguration getMetricConfiguration() {
        return metricConfiguration;
    }

    public void setMetricConfiguration(MetricConfiguration metricConfiguration) {
        this.metricConfiguration = metricConfiguration;
    }

    public CollectorConfiguration getCollectorConfiguration() {
        return collectorConfiguration;
    }

    public void setCollectorConfiguration(CollectorConfiguration collectorConfiguration) {
        this.collectorConfiguration = collectorConfiguration;
    }

    public UiConfiguration getUiConfiguration() {
        return uiConfiguration;
    }

    public void setUiConfiguration(UiConfiguration uiConfiguration) {
        this.uiConfiguration = uiConfiguration;
    }
}
