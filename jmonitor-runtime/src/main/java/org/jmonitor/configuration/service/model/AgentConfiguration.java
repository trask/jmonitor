/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmonitor.configuration.service.model;

import org.jmonitor.util.annotation.Comment;
import org.jmonitor.util.annotation.DefaultValue;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
// most uses of configuration don't need to modify
// this interface is exposed to avoid accidental modification
//
// default values should be conservative
public interface AgentConfiguration {

    int TRACE_ELEMENT_LIMIT_DISABLED = -1;
    int THRESHOLD_DISABLED = -1;

    // if monitoring is disabled mid-operation there should be no issue
    // active operations will not accumulate additional contextual trace elements
    // but they will be logged / emailed if they exceed the defined thresholds
    //
    // if monitoring is enabled mid-operation there should be no issue
    // active operations that were not captured at their start will
    // continue not to accumulate contextual trace elements
    // and they will not be logged / emailed even if they exceed the defined thresholds
    // TODO change this to isEnabled with default value true (to match GUI)
    boolean isEnabled();

    @Comment("0 means log all operations, -1 means log no operations "
            + "(though stuck threshold can still be used in this case)")
    int getThresholdMillis();

    // minimum is imposed because of Agent#AGENT_POLLING_INTERVAL_MILLIS
    @Comment("-1 means no stuck messages are gathered, should be minimum 100 milliseconds")
    int getStuckThresholdMillis();

    // minimum is imposed because of Agent#AGENT_POLLING_INTERVAL_MILLIS
    @DefaultValue("5000")
    @Comment("-1 means no stack traces are gathered, should be minimum 100 milliseconds")
    int getStackTraceInitialDelayMillis();

    int getStackTracePeriodMillis();

    @Comment("used to limit memory requirement, also used to help limit log file size, "
            + "0 means don't capture any operations, -1 means no limit")
    int getMaxTraceEventsPerOperation();

    boolean isWarnOnTraceEventOutsideOperation();
}
