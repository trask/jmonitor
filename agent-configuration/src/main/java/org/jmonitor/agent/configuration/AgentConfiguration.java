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

import org.jmonitor.util.annotation.Comment;
import org.jmonitor.util.annotation.DefaultValue;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// most uses of configuration don't need to modify. This interface is exposed to
// avoid accidental modification.
// default values should be conservative
public class AgentConfiguration implements Serializable {

	private static final long serialVersionUID = 1L;

	public static int TRACE_EVENTS_LIMIT_DISABLED = -1;
	public static int THRESHOLD_DISABLED = -1;

	// TODO add versioning and perform optimistic locking when updating
	// configuration
	// private long configurationVersion = 0;

	// if monitoring is disabled mid-operation there should be no issue
	// active operations will not accumulate additional contextual trace
	// elements
	// but they will be logged / emailed if they exceed the defined thresholds
	//
	// if monitoring is enabled mid-operation there should be no issue
	// active operations that were not captured at their start will
	// continue not to accumulate contextual trace elements
	// and they will not be logged / emailed even if they exceed the defined
	// thresholds
	private boolean enabled = false;

	// TODO convert from millis to seconds, support 0.1, etc
	@Comment("0 means log all operations, -1 means log no operations "
			+ "(though stuck threshold can still be used in this case)")
	private int thresholdMillis = 30000;

	// minimum is imposed because of Agent#AGENT_POLLING_INTERVAL_MILLIS
	@Comment("-1 means no stuck messages are gathered, should be minimum 100 milliseconds")
	private int stuckThresholdMillis = 600000;

	// minimum is imposed because of Agent#AGENT_POLLING_INTERVAL_MILLIS
	@DefaultValue("5000")
	@Comment("-1 means no stack traces are gathered, should be minimum 100 milliseconds")
	private int stackTraceInitialDelayMillis = 10000;

	private int stackTracePeriodMillis = 1000;

	@Comment("used to limit memory requirement, also used to help limit log file size, "
			+ "0 means don't capture any operations, -1 means no limit")
	private int maxTraceEventsPerOperation = 1000;

	private boolean warnOnTraceEventOutsideOperation = false;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getThresholdMillis() {
		return thresholdMillis;
	}

	public void setThresholdMillis(int thresholdMillis) {
		this.thresholdMillis = thresholdMillis;
	}

	public int getStuckThresholdMillis() {
		return stuckThresholdMillis;
	}

	public void setStuckThresholdMillis(int stuckThresholdMillis) {
		this.stuckThresholdMillis = stuckThresholdMillis;
	}

	public int getStackTraceInitialDelayMillis() {
		return stackTraceInitialDelayMillis;
	}

	public void setStackTraceInitialDelayMillis(int stackTraceInitialDelayMillis) {
		this.stackTraceInitialDelayMillis = stackTraceInitialDelayMillis;
	}

	public int getStackTracePeriodMillis() {
		return stackTracePeriodMillis;
	}

	public void setStackTracePeriodMillis(int stackTracePeriodMillis) {
		this.stackTracePeriodMillis = stackTracePeriodMillis;
	}

	public int getMaxTraceEventsPerOperation() {
		return maxTraceEventsPerOperation;
	}

	public void setMaxTraceEventsPerOperation(int maxTraceEventsPerOperation) {
		this.maxTraceEventsPerOperation = maxTraceEventsPerOperation;
	}

	public boolean isWarnOnTraceEventOutsideOperation() {
		return warnOnTraceEventOutsideOperation;
	}

	public void setWarnOnTraceEventOutsideOperation(
			boolean warnOnTraceEventOutsideOperation) {
		this.warnOnTraceEventOutsideOperation = warnOnTraceEventOutsideOperation;
	}
}
