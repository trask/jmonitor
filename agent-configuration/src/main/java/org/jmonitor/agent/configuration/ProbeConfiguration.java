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

package org.jmonitor.agent.configuration;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class ProbeConfiguration implements Serializable {

	private static final long serialVersionUID = 1L;

	// TODO add versioning and perform optimistic locking when updating
	// configuration
	// private long configurationVersion = 0;

	private volatile Map<String, Map<String, String>> propertyMaps = new HashMap<String, Map<String, String>>();

	// never returns null
	public String getProperty(String probeName, String propertyName) {
		Map<String, String> propertyMap = propertyMaps.get(probeName);
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

	public Iterable<String> getProbeNames() {
		return propertyMaps.keySet();
	}

	public Iterable<String> getPropertyNames(String probeName) {
		Map<String, String> propertyMap = propertyMaps.get(probeName);
		if (propertyMap == null) {
			return Collections.emptySet();
		} else {
			return propertyMap.keySet();
		}
	}

	public void setProperty(String probeName, String propertyName,
			String propertyValue) {
		Map<String, String> propertyMap = propertyMaps.get(probeName);
		if (propertyMap == null) {
			propertyMap = new HashMap<String, String>();
			propertyMaps.put(probeName, propertyMap);
		}
		propertyMap.put(propertyName, propertyValue);
	}
}
