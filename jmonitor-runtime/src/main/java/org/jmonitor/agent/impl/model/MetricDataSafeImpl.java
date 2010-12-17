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

package org.jmonitor.agent.impl.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jmonitor.collector.service.model.MetricData;

/**
 * Used to track summary data for a given operation (e.g. total jdbc execution time for a given
 * operation)
 * 
 * @author Trask Stalnaker
 * @see OperationSafeImpl#recordOperationSummaryData(String, long)
 * @since 1.0
 */
public class MetricDataSafeImpl implements MetricData {

    // store timing info per aggregation key
    private final ConcurrentMap<String, MetricDataItemSafeImpl> map =
            new ConcurrentHashMap<String, MetricDataItemSafeImpl>();

    public void recordData(String name, long timeInNanoseconds) {
        MetricDataItemSafeImpl summaryDataItem = map.get(name);
        if (summaryDataItem == null) {
            // it's possible that two threads both instantiate a new OperationSummaryDataItem
            // but only one of the SummaryDataItems will get set via putIfAbsent which is why we
            // retrieve the value afterwards
            map.putIfAbsent(name, new MetricDataItemSafeImpl(name));
            summaryDataItem = map.get(name);
        }
        summaryDataItem.recordData(timeInNanoseconds);
    }

    public Iterable<MetricDataItemSafeImpl> getItems() {
        return map.values();
    }
}
