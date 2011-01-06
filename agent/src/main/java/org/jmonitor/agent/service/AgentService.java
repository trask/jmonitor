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

package org.jmonitor.agent.service;

import java.util.Collection;

import org.jmonitor.agent.configuration.AgentConfiguration;
import org.jmonitor.agent.configuration.MetricConfiguration;
import org.jmonitor.agent.configuration.ProbeConfiguration;
import org.jmonitor.collector.service.model.Operation;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public interface AgentService {

	Collection<? extends Operation> getOperationsExceptCurrent();

	// agent service needs to cache its configuration for performance
	// (at least when pulling its configuration from jmonitor central)
	// and this method allows it to be notified (by jmonitor central) of changes
	// to the configuration
	void updateConfiguration(AgentConfiguration agentConfiguration);

	void updateConfiguration(ProbeConfiguration agentConfiguration);

	void updateConfiguration(MetricConfiguration agentConfiguration);

	// get current operations

	// get current stats, etc
}
