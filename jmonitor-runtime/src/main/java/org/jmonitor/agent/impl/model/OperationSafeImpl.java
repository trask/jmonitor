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

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jmonitor.api.probe.ProbeExecution;
import org.jmonitor.api.probe.RootProbeExecution;
import org.jmonitor.collector.service.model.MetricData;
import org.jmonitor.collector.service.model.Operation;
import org.jmonitor.collector.service.model.SampledHotspotTree;
import org.jmonitor.collector.service.model.Trace;

/**
 * Contains all data that the agent has captured for a given tracked operation (e.g. servlet
 * request).
 * 
 * This class needs to be thread safe, only one thread updates it, but multiple threads can read it
 * at the same time as it is being updated.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class OperationSafeImpl implements Operation { // NOPMD for too many methods, ok for now

    // used to populate unique id (below)
    // updates to this field are guarded by "lock" below (so AtomicLong is not necessary)
    // TODO probably this could be int now that we only increment
    private static volatile long uniqueIdCounter = 1;

    // timing data is tracked in nano seconds which cannot be converted into dates
    // (see javadoc for System.nanoTime())
    // so we also track operation startTimeDate as a date object here
    private final Date startTime;

    private final AtomicBoolean stuck = new AtomicBoolean();

    // unique id to identify operations across multiple flushed / completed log entries
    // this is lazy created since it is only needed to match an operation if it is flushed
    // prior to completion
    private volatile long uniqueId;

    // updates guarded by "lock"
    private volatile int flushCount;

    // this stores the thread name(s) at operation start and at each stack trace capture
    private final Set<String> threadNames = new CopyOnWriteArraySet<String>();

    // store timing info so we can report on summary data for a given operation
    private final MetricDataSafeImpl metricData = new MetricDataSafeImpl();

    // contextual trace constructed from aspectj pointcuts
    private final TraceSafeImpl trace;

    // stack trace data constructed from captured stack trace samplings
    private final SampledHotspotTreeSafeImpl sampledHotspotTree;

    // the thread is needed so that we can take stack traces from a different thread
    // a weak reference is used just to be safe and make sure it can't accidentally prevent a thread
    // from being garbage collected
    private final WeakReference<Thread> threadHolder =
            new WeakReference<Thread>(Thread.currentThread());

    // these are stored in the operation so that they can be cancelled
    private volatile ScheduledFuture<?> captureStackTraceScheduledFuture;
    private volatile ScheduledFuture<?> stuckCommandScheduledFuture;

    private final Object lock = new Object();

    public OperationSafeImpl(ProbeExecution probeExecution) {

        startTime = new Date();
        trace = new TraceSafeImpl(probeExecution);
        sampledHotspotTree = new SampledHotspotTreeSafeImpl();
        addThreadName(Thread.currentThread());
    }

    public Date getStartTime() {
        return startTime;
    }

    public long getUniqueId() {
        return uniqueId;
    }

    // a couple of properties make sense to expose as part of operation
    public long getStartNanoTime() {
        return trace.getStartNanoTime();
    }

    public long getEndNanoTime() {
        return trace.getEndNanoTime();
    }

    public long getDurationInNanoseconds() {
        return trace.getDurationInNanoseconds();
    }

    public boolean isCompleted() {
        return trace.isCompleted();
    }

    public boolean isStuck() {
        return stuck.get();
    }

    public boolean getAndSetStuck() {
        return stuck.getAndSet(true);
    }

    public String getUsername() {
        ProbeExecution probeExecution = trace.getRootElement().getProbeExecution();
        if (probeExecution instanceof RootProbeExecution) {
            return ((RootProbeExecution) probeExecution).getUsername();
        } else {
            return null;
        }
    }

    public TraceSafeImpl getTrace() {
        return trace;
    }

    public SampledHotspotTreeSafeImpl getSampledHotspotTree() {
        return sampledHotspotTree;
    }

    public MetricData getMetricData() {
        return metricData;
    }

    public Iterable<String> getThreadNames() {
        return threadNames;
    }

    public ScheduledFuture<?> getCaptureStackTraceScheduledFuture() {
        return captureStackTraceScheduledFuture;
    }

    public boolean wasCompletedBy(long nanoTime) {
        return trace.wasCompletedBy(nanoTime);
    }

    public ScheduledFuture<?> getStuckCommandScheduledFuture() {
        return stuckCommandScheduledFuture;
    }

    public boolean isPreviouslyFlushed() {
        return flushCount > 0;
    }

    // this is intentionally not synchronized since it can measure very fine
    // grained actions and can be called very often
    public void recordOperationSummaryData(String operationSummaryKey, long timeInNanoseconds) {
        metricData.recordData(operationSummaryKey, timeInNanoseconds);
    }

    // this method doesn't need to be synchronized
    public void setCaptureStackTraceScheduledFuture(ScheduledFuture<?> stackTraceScheduledFuture) {
        this.captureStackTraceScheduledFuture = stackTraceScheduledFuture;
    }

    // this method doesn't need to be synchronized
    public void setStuckCommandScheduledFuture(ScheduledFuture<?> stuckCommandScheduledFuture) {
        this.stuckCommandScheduledFuture = stuckCommandScheduledFuture;
    }

    public void captureStackTrace() {
        Thread thread = threadHolder.get();
        if (thread != null) {
            // TODO gather thread names at different point? maybe during contextual trace?
            addThreadName(thread);
            sampledHotspotTree.captureStackTrace(thread);
        }
    }

    public Operation flush() {

        // TODO synchronize elsewhere appropriately
        synchronized (lock) {

            flushCount++;

            // we only need unique ids when the operation is flushed prior to completion
            uniqueId = uniqueIdCounter++;

            return new FlushedOperation(trace.flush());
        }
    }

    private void addThreadName(Thread thread) {
        String threadName = thread.getName();
        if (!threadNames.contains(threadName)) {
            // intentionally calling contains first for performance even though add() performs a
            // similar check, this is because of the specific implementation of
            // CopyOnWriteArraySet.add() which performs copying at the same time as checking for
            // a duplicate since (as the implementors wrote)
            // "This wins in the most common case where [the value] is not present".
            // however for us, typically the thread name will already be present (since it
            // typically doesn't change), so this would be inefficient in our case
            threadNames.add(threadName);
        }
    }

    // TODO does this object need to be immutable? (reference only immutable data)
    private class FlushedOperation implements Operation {

        private final Trace flushedTrace;

        public FlushedOperation(Trace flushedTrace) {
            this.flushedTrace = flushedTrace;
        }

        public long getUniqueId() {
            return uniqueId;
        }

        public Date getStartTime() {
            return startTime;
        }

        public long getStartNanoTime() {
            return trace.getStartNanoTime();
        }

        public long getEndNanoTime() {
            return trace.getEndNanoTime();
        }

        public long getDurationInNanoseconds() {
            return trace.getDurationInNanoseconds();
        }

        public boolean isCompleted() {
            return trace.isCompleted();
        }

        public boolean isStuck() {
            return stuck.get();
        }

        public Iterable<String> getThreadNames() {
            return threadNames;
        }

        public String getUsername() {
            return OperationSafeImpl.this.getUsername();
        }

        public Trace getTrace() {
            return flushedTrace;
        }

        public SampledHotspotTree getSampledHotspotTree() {
            return sampledHotspotTree;
        }

        public MetricData getMetricData() {
            return metricData;
        }
    }
}
