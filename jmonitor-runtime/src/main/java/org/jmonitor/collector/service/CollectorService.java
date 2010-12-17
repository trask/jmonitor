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

package org.jmonitor.collector.service;

import org.jmonitor.collector.service.model.Operation;
import org.jmonitor.configuration.service.model.CollectorConfiguration;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public interface CollectorService {

    // trace elements can include previously collected trace elements that have been updated since
    // last collection, e.g. there may have been an uncompleted trace element at the last
    // collection that has now been collected, or a more complicated example is jdbc trace element
    // which are updated after they have been completed in order to track the number of records read
    // via next() and the total time spent in next()
    void collect(Operation operation);

    void collectFirstStuck(Operation operation);

    // void collectMetricData(MetricData metricData);

    // void collectTrace(Trace trace)

    // void collectMetricData(StackTraceData stackTraceData)

    // void collectSystemData(SystemData systemData)

    void collectError(String msg);

    void collectError(String msg, Throwable t); // NOPMD for short variable name

    void updateConfiguration(CollectorConfiguration configuration);
}
