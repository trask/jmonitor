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
package org.jmonitor.collector.service.model;

import org.jmonitor.api.probe.ProbeExecutionContext;

/**
 * This interface is used to allow collectors to run locally (in addition to remotely) without
 * requiring object copy in the local case.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public interface ExecutionTraceElement {

    String getDescription();

    ProbeExecutionContext getContext();

    long getOffsetInNanoseconds();
    long getDurationInNanoseconds();

    boolean isCompleted();

    // index is per operation and starts at 0
    int getIndex();
    int getParentIndex();

    // level is just a convenience for output
    int getLevel();
}
