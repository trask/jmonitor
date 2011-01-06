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

package org.jmonitor.api.probe;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// doesn't need to be thread safe b/c it should only be generated on the fly
// and used by a single thread
public final class ProbeExecutionContext {

    private Map<String, String> map;
    private Map<String, ProbeExecutionContext> nestedMaps;

    public ProbeExecutionContext() {
        this(true);
    }

    public ProbeExecutionContext(boolean autoSort) {
        if (autoSort) {
            map = new TreeMap<String, String>();
        } else {
            // using linked hash map so probe can maintain its own ordering
            map = new LinkedHashMap<String, String>();
        }
    }

    public void put(String key, String value) {
        map.put(key, value);
    }

    public void putNested(String key, ProbeExecutionContext value) {
        if (nestedMaps == null) {
            if (map instanceof TreeMap<?, ?>) {
                // this context was created with auto sort
                nestedMaps = new TreeMap<String, ProbeExecutionContext>();
            } else {
                // using linked hash map so probe can maintain its own ordering
                nestedMaps = new LinkedHashMap<String, ProbeExecutionContext>();
            }
        }
        nestedMaps.put(key, value);
    }

    public Map<String, String> getMap() {
        return map;
    }

    public Map<String, ProbeExecutionContext> getNestedMaps() {
        if (nestedMaps == null) {
            return Collections.emptyMap();
        }
        return nestedMaps;
    }
}
