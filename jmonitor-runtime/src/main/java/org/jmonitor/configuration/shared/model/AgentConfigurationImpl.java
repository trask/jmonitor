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

package org.jmonitor.configuration.shared.model;

import java.io.Serializable;

import org.jmonitor.configuration.service.model.AgentConfiguration;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// default values should be conservative
public class AgentConfigurationImpl implements AgentConfiguration, Serializable {

    private static final long serialVersionUID = 1L;

    private boolean enabled = false;

    // TODO convert from millis to seconds, support 0.1, etc
    private int thresholdMillis = 30000;

    private int stuckThresholdMillis = 600000;

    private int stackTraceInitialDelayMillis = 10000;

    private int stackTracePeriodMillis = 1000;

    private int maxTraceEventsPerOperation = 1000;

    private boolean warnOnTraceEventOutsideOperation = false;

    // TODO research other strategies for creating immutable objects with lots of properties
    // (builders?)
    private boolean immutable;

    public void makeImmutable() {
        immutable = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.enabled = enabled;
    }

    public int getThresholdMillis() {
        return thresholdMillis;
    }

    public void setThresholdMillis(int thresholdMillis) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.thresholdMillis = thresholdMillis;
    }

    public int getStuckThresholdMillis() {
        return stuckThresholdMillis;
    }

    public void setStuckThresholdMillis(int stuckThresholdMillis) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.stuckThresholdMillis = stuckThresholdMillis;
    }

    public int getStackTraceInitialDelayMillis() {
        return stackTraceInitialDelayMillis;
    }

    public void setStackTraceInitialDelayMillis(int stackTraceInitialDelayMillis) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.stackTraceInitialDelayMillis = stackTraceInitialDelayMillis;
    }

    public int getStackTracePeriodMillis() {
        return stackTracePeriodMillis;
    }

    public void setStackTracePeriodMillis(int stackTracePeriodMillis) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.stackTracePeriodMillis = stackTracePeriodMillis;
    }

    public int getMaxTraceEventsPerOperation() {
        return maxTraceEventsPerOperation;
    }

    public void setMaxTraceEventsPerOperation(int maxTraceEventsPerOperation) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.maxTraceEventsPerOperation = maxTraceEventsPerOperation;
    }

    public boolean isWarnOnTraceEventOutsideOperation() {
        return warnOnTraceEventOutsideOperation;
    }

    public void setWarnOnTraceEventOutsideOperation(boolean warnOnTraceEventOutsideOperation) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.warnOnTraceEventOutsideOperation = warnOnTraceEventOutsideOperation;
    }
}
