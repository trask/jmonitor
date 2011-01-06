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

package org.jmonitor.agent.configuration.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jmonitor.agent.configuration.AgentConfiguration;
import org.jmonitor.agent.configuration.AgentConfigurationService;
import org.jmonitor.agent.configuration.MetricConfiguration;
import org.jmonitor.agent.configuration.ProbeConfiguration;
import org.jmonitor.util.Bootstrap;
import org.jmonitor.util.ConfigurationUtils;
import org.jmonitor.util.UncheckedPropertyUtils;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO should this service only allow updates to full configuration?
// then we could store only full configuration here and make atomic updates
// instead of piece meal updates
// or we could still allow updates to individual portions, but just create
// and store new full configuration each time
// also need to version full(?) configuration and prevent clobbering
public final class AgentConfigurationServiceImpl implements
		AgentConfigurationService {

	private static final String CONFIGURATION_DEFAULTS_RESOURCE_NAME = "jmonitor-defaults.properties";

	private static final AgentConfigurationServiceImpl INSTANCE = new AgentConfigurationServiceImpl();

	// TODO consider not caching these instances when it comes to supporting a
	// cluster of jmonitor central instances (due to synchronizing the cache
	// across the cluster)
	private volatile AgentConfiguration agentConfiguration;
	private volatile ProbeConfiguration probeConfiguration;
	private volatile MetricConfiguration metricConfiguration;

	// used to prevent concurrent access to the configuration file
	private final Object lock = new Object();

	private AgentConfigurationServiceImpl() {
	}

	// returns defensive copy
	public AgentConfiguration getAgentConfiguration() {
		if (agentConfiguration == null) {
			// thread-safe, not ideal, but ok if multiple threads call this
			// method concurrently
			// (it has internal synchronization to prevent configuration file
			// clobbering)
			agentConfiguration = loadAgentConfiguration();
		}
		return copyOf(agentConfiguration);
	}

	// returns defensive copy
	public ProbeConfiguration getProbeConfiguration() {
		if (probeConfiguration == null) {
			// thread-safe, not ideal, but ok if multiple threads call this
			// method concurrently
			// (it has internal synchronization to prevent configuration file
			// clobbering)
			probeConfiguration = loadProbeConfiguration();
		}
		return copyOf(probeConfiguration);
	}

	// returns defensive copy
	public MetricConfiguration getMetricConfiguration() {
		if (metricConfiguration == null) {
			// thread-safe, not ideal, but ok if multiple threads call this
			// method concurrently
			// (it has internal synchronization to prevent configuration file
			// clobbering)
			metricConfiguration = loadMetricConfiguration();
		}
		return copyOf(metricConfiguration);
	}

	public void updateAgentConfiguration(AgentConfiguration configuration) {

		// save
		ConfigurationUtils.saveIfNecessary(configuration,
				Bootstrap.getConfigurationFilename(), AgentConfiguration.class,
				"agent");

		// clear cached instance, it will be re-loaded on next access
		agentConfiguration = null;
	}

	public void updateProbeConfiguration(ProbeConfiguration configuration) {

		Map<String, String> propertyMap = new HashMap<String, String>();
		for (String probeName : configuration.getProbeNames()) {
			for (String propertyName : configuration
					.getPropertyNames(probeName)) {
				propertyMap.put(probeName + "." + propertyName,
						configuration.getProperty(probeName, propertyName));
			}
		}

		// save
		ConfigurationUtils.saveIfNecessary(propertyMap,
				Bootstrap.getConfigurationFilename(), "probe");

		// clear cached instance, it will be re-loaded on next access
		probeConfiguration = null;
	}

	public void updateMetricConfiguration(MetricConfiguration configuration) {

		Map<String, String> propertyMap = new HashMap<String, String>();
		for (String metricName : configuration.getMetricNames()) {
			for (String propertyName : configuration
					.getPropertyNames(metricName)) {
				propertyMap.put(metricName + "." + propertyName,
						configuration.getProperty(metricName, propertyName));
			}
		}

		// save
		ConfigurationUtils.saveIfNecessary(propertyMap,
				Bootstrap.getConfigurationFilename(), "metric");

		// clear cached instance, it will be re-loaded on next access
		metricConfiguration = null;
	}

	private AgentConfiguration loadAgentConfiguration() {

		AgentConfiguration configuration = new AgentConfiguration();
		String configurationFilename = Bootstrap.getConfigurationFilename();

		synchronized (lock) {
			ConfigurationUtils.load(configuration, configurationFilename,
					CONFIGURATION_DEFAULTS_RESOURCE_NAME,
					AgentConfiguration.class, "agent");

			ConfigurationUtils.saveIfNecessary(configuration,
					configurationFilename, AgentConfiguration.class, "agent");
		}
		return configuration;
	}

	private ProbeConfiguration loadProbeConfiguration() {

		String configurationFilename = Bootstrap.getConfigurationFilename();

		Map<String, String> probeConfigurationMap = ConfigurationUtils.load(
				configurationFilename, CONFIGURATION_DEFAULTS_RESOURCE_NAME,
				"probe");

		ProbeConfiguration configuration = new ProbeConfiguration();
		for (Map.Entry<String, String> entry : probeConfigurationMap.entrySet()) {
			String probeName = StringUtils.substringBefore(entry.getKey(), ".");
			String propertyName = StringUtils.substringAfter(entry.getKey(),
					".");
			configuration
					.setProperty(probeName, propertyName, entry.getValue());
		}
		return configuration;
	}

	private MetricConfiguration loadMetricConfiguration() {

		String configurationFilename = Bootstrap.getConfigurationFilename();

		Map<String, String> metricConfigurationMap = ConfigurationUtils.load(
				configurationFilename, CONFIGURATION_DEFAULTS_RESOURCE_NAME,
				"metric");

		MetricConfiguration configuration = new MetricConfiguration();
		for (Map.Entry<String, String> entry : metricConfigurationMap
				.entrySet()) {
			String metricName = StringUtils
					.substringBefore(entry.getKey(), ".");
			String propertyName = StringUtils.substringAfter(entry.getKey(),
					".");
			configuration.setProperty(metricName, propertyName,
					entry.getValue());
		}
		return configuration;
	}

	private static AgentConfiguration copyOf(AgentConfiguration configuration) {
		
		AgentConfiguration copy = new AgentConfiguration();
		// copy over all properties
		UncheckedPropertyUtils.copyProperties(copy, configuration);
		return copy;
	}

	private static ProbeConfiguration copyOf(ProbeConfiguration configuration) {
		
		ProbeConfiguration copy = new ProbeConfiguration();
		for (String probeName : configuration.getProbeNames()) {
			for (String propertyName : configuration
					.getPropertyNames(probeName)) {
				copy.setProperty(probeName, propertyName,
						configuration.getProperty(probeName, propertyName));
			}
		}
		return copy;
	}

	private static MetricConfiguration copyOf(
			MetricConfiguration configuration) {
		
		MetricConfiguration copy = new MetricConfiguration();
		for (String metricName : configuration.getMetricNames()) {
			for (String propertyName : configuration
					.getPropertyNames(metricName)) {
				copy.setProperty(metricName, propertyName,
						configuration.getProperty(metricName, propertyName));
			}
		}
		return copy;
	}

	public static AgentConfigurationServiceImpl getInstance() {
		return INSTANCE;
	}
}
