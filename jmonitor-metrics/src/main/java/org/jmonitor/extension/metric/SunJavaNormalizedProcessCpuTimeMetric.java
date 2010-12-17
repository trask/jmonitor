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

package org.jmonitor.extension.metric;

import java.lang.management.ManagementFactory;

import org.jmonitor.api.metric.Metric;
import org.jmonitor.api.metric.MetricPool;

import com.sun.management.OperatingSystemMXBean;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class SunJavaNormalizedProcessCpuTimeMetric implements Metric {

    public boolean isAvailable() {
        return (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean);
    }

    public String getName() {
        return "cpu";
    }

    public String getDescription() {
        return "CPU utilization";
    }

    public MetricPool getPool() {
        return MetricPool.FAST;
    }

    public double collect() {
        OperatingSystemMXBean mxbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return mxbean.getProcessCpuTime() / mxbean.getAvailableProcessors();
    }

}
