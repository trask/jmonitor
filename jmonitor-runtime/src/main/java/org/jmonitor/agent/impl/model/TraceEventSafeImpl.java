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

import org.jmonitor.agent.impl.util.collection.IndexedElement;
import org.jmonitor.api.probe.ProbeExecution;
import org.jmonitor.api.probe.ProbeExecutionContext;
import org.jmonitor.collector.service.model.TraceEvent;
import org.jmonitor.util.NanoUtils;

/**
 * Base class for all calls captured by AspectJ pointcuts.
 * 
 * This and all subclasses must support updating by a single thread and reading by multiple threads.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class TraceEventSafeImpl implements TraceEvent, IndexedElement {

    private final ProbeExecution probeExecution;

    private final long operationStartNanoTime;
    private final long startNanoTime;
    private volatile long endNanoTime;

    private volatile boolean completed;

    // index is per operation and starts at 0
    private final int index;
    private final int parentIndex;

    // level is just a convenience for output
    private final int level;

    public TraceEventSafeImpl(ProbeExecution probeExecution,
            long operationStartNanoTime, long startNanoTime, int index, int parentIndex, int level) {

        this.probeExecution = probeExecution;
        this.operationStartNanoTime = operationStartNanoTime;
        this.startNanoTime = startNanoTime;
        this.index = index;
        this.parentIndex = parentIndex;
        this.level = level;
    }

    public String getDescription() {
        return probeExecution.getDescription();
    }

    public ProbeExecutionContext getContext() {
        return probeExecution.createContext();
    }

    public ProbeExecution getProbeExecution() {
        return probeExecution;
    }

    public long getStartNanoTime() {
        return startNanoTime;
    }

    public long getEndNanoTime() {
        return endNanoTime;
    }

    public long getOffsetInNanoseconds() {
        return startNanoTime - operationStartNanoTime;
    }

    public long getDurationInNanoseconds() {
        return endNanoTime - startNanoTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean wasCompletedBy(long nanoTime) {
        return completed && NanoUtils.isLessThan(endNanoTime, nanoTime);
    }

    public boolean wasStartedBy(long nanoTime) {
        return NanoUtils.isLessThan(startNanoTime, nanoTime);
    }

    public int getIndex() {
        return index;
    }

    public int getParentIndex() {
        return parentIndex;
    }

    public int getLevel() {
        return level;
    }

    void setEndNanoTime(long endNanoTime) {
        this.endNanoTime = endNanoTime;
    }

    void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
