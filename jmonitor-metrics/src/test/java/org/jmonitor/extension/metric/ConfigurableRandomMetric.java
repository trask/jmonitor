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

import java.util.Random;

import org.jmonitor.api.ConfigurableProperty;
import org.jmonitor.api.metric.Metric;
import org.jmonitor.api.metric.MetricPool;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class ConfigurableRandomMetric implements Metric {

    private static Random random = new Random();
    
    // default value is 1
    private int multiplier = 1;
    
    @ConfigurableProperty(description = "Multiplier")
    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }
    
    public boolean isAvailable() {
        return true;
    }

    public String getName() {
        return "test-configurable";
    }

    public String getDescription() {
        return "Test Configurable";
    }

    public MetricPool getPool() {
        return MetricPool.FAST;
    }

    public double collect() {
        return random.nextDouble() * multiplier;
    }
}
