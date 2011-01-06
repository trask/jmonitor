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

package org.jmonitor.collector.service.model;

import java.util.Date;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public interface Operation {

    Date getStartTime();

    long getStartNanoTime();
    long getEndNanoTime();
    long getDurationInNanoseconds();

    boolean isStuck();

    boolean isCompleted();

    long getUniqueId();

    Iterable<String> getThreadNames();

    String getUsername();

    Trace getTrace();

    SampledHotspotTree getSampledHotspotTree();

    MetricData getMetricData();
}
