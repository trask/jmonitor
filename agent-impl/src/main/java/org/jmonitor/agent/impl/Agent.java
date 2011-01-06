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

package org.jmonitor.agent.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jmonitor.agent.configuration.AgentConfiguration;
import org.jmonitor.agent.configuration.AgentConfigurationServiceFactory;
import org.jmonitor.agent.configuration.MetricConfiguration;
import org.jmonitor.agent.configuration.ProbeConfiguration;
import org.jmonitor.agent.impl.model.OperationSafeImpl;
import org.jmonitor.agent.impl.model.TraceEventSafeImpl;
import org.jmonitor.agent.service.AgentService;
import org.jmonitor.api.probe.ProbeExecution;
import org.jmonitor.collector.shared.logging.CollectorServiceLoggerFactory;
import org.jmonitor.util.NanoUtils;
import org.slf4j.Logger;

/**
 * This singleton holds the agent state, including all currently executing operation, executor
 * services for capturing stack traces, logging completed operations and logging stuck operations.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class Agent implements AgentService, Runnable {

    private static final Logger LOGGER = CollectorServiceLoggerFactory.getLogger(Agent.class);

    private static final int AGENT_POLLING_INTERVAL_MILLIS = 100;

    private static final Agent INSTANCE = new Agent();

    // collection of active running operations
    private final Collection<OperationSafeImpl> operations =
            new ConcurrentLinkedQueue<OperationSafeImpl>();

    // active running operation being executed by the current thread
    private final ThreadLocal<OperationSafeImpl> currentOperationHolder =
            new ThreadLocal<OperationSafeImpl>();

    // this is used to disable monitoring of the current operation
    // it is used in case monitoring is later enabled while this operation is still active
    // in which case monitoring should stay disabled for the operation
    //
    // we interpret null as false so we can clear out the ThreadLocal in a finally block to make
    // sure we don't leave the thread local lying around attached to an inactive thread
    private final ThreadLocal<Boolean> currentOperationDisabledHolder = new ThreadLocal<Boolean>();

    // use a separate thread to log (and email as needed) completed operations
    // so that we don't impact user response time
    private final ExecutorService completedOperationLogExecutor =
            Executors.newSingleThreadExecutor(new DaemonThreadFactory());

    // used for scheduling stack traces to be captured and stuck operation logging to occur
    private final ScheduledExecutorService stackTraceCaptureScheduledExecutor =
            Executors.newScheduledThreadPool(1, new DaemonThreadFactory());

    // this needs its own thread pool since logging stuck threads can take some
    // time if there is lots of data to log
    private final ScheduledExecutorService stuckOperationLogScheduledExecutor =
            Executors.newScheduledThreadPool(1, new DaemonThreadFactory());

    private Agent() {
        // we cannot schedule stack trace and stuck thread commands for every thread b/c there's no
        // way to preemptively remove those commands from the scheduled queue until their time has
        // come (we can cancel them, but they remain in the scheduled queue taking up memory until
        // their time has come at which point they are not run if they have already been cancelled)
        // so we create a single scheduled command to poll the current operation and wait to
        // schedule stack trace and stuck thread commands until they are within
        // AGENT_POLLING_INTERVAL_MILLIS from needing to start
        stackTraceCaptureScheduledExecutor.scheduleWithFixedDelay(this, 0,
                AGENT_POLLING_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void run() {
        try {
            runInternal();
        } catch (Exception e) {
            // log and terminate this thread successfully
            LOGGER.error(e.getMessage(), e);
        } catch (Error e) {
            // log and re-throw serious error which will terminate subsequent scheduled executions
            // (see ScheduledExecutorService.scheduleWithFixedDelay())
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }

    // look for operations that will exceed the stack trace initial delay threshold
    // or stuck threshold within the next polling interval and respectively schedule stack
    // trace capture or stuck message generation to occur at the appropriate time(s)
    private void runInternal() {

        AgentConfiguration configuration =
                AgentConfigurationServiceFactory.getService().getAgentConfiguration();

        long currentNanoTime = System.nanoTime();

        if (configuration.getStackTraceInitialDelayMillis() != AgentConfiguration.THRESHOLD_DISABLED) {

            // stack trace threshold is not disabled

            long stackTraceThresholdNanoTime =
                    currentNanoTime
                            - TimeUnit.MILLISECONDS.toNanos(configuration.getStackTraceInitialDelayMillis()
                                    - AGENT_POLLING_INTERVAL_MILLIS);

            for (OperationSafeImpl operation : operations) {

                // if the operation is within AGENT_POLLING_INTERVAL_MILLIS from hitting
                // the stack trace initial delay threshold
                // and the stack trace capture hasn't already been scheduled then schedule it
                if (NanoUtils.isLessThan(operation.getStartNanoTime(), stackTraceThresholdNanoTime)
                        && operation.getCaptureStackTraceScheduledFuture() == null) {

                    // schedule stack traces to be taken every X seconds
                    long initialDelayMillis =
                            getMillisUntilOperationReachesThreshold(operation,
                                    configuration.getStackTraceInitialDelayMillis());
                    ScheduledFuture<?> captureStackTraceScheduledFuture =
                            stackTraceCaptureScheduledExecutor.scheduleWithFixedDelay(
                                    new StackTraceCaptureCommand(operation), initialDelayMillis,
                                    configuration.getStackTracePeriodMillis(),
                                    TimeUnit.MILLISECONDS);
                    operation.setCaptureStackTraceScheduledFuture(captureStackTraceScheduledFuture);

                } else {

                    // since we are iterating over a queue ordered by start time, if this operation
                    // didn't meet the threshold then we know that no subsequent operations will
                    // meet the threshold and we can break here
                    break;
                }
            }
        }

        if (configuration.getStuckThresholdMillis() != AgentConfiguration.THRESHOLD_DISABLED) {

            // stuck threshold is not disabled

            long stuckMessageThresholdNanoTime =
                    currentNanoTime
                            - TimeUnit.MILLISECONDS.toNanos(configuration.getStuckThresholdMillis()
                                    - AGENT_POLLING_INTERVAL_MILLIS);

            for (OperationSafeImpl operation : operations) {

                // if the operation is within AGENT_POLLING_INTERVAL_MILLIS from hitting the stuck
                // thread threshold and the stuck thread messaging hasn't already been scheduled
                // then schedule it
                if (NanoUtils.isLessThan(operation.getStartNanoTime(),
                        stuckMessageThresholdNanoTime)
                        && operation.getStuckCommandScheduledFuture() == null) {

                    // schedule stuck thread message
                    long initialDelayMillis =
                            getMillisUntilOperationReachesThreshold(operation,
                                    configuration.getStuckThresholdMillis());
                    ScheduledFuture<?> stuckCommandScheduledFuture =
                            stuckOperationLogScheduledExecutor.schedule(new StuckOperationCommand(
                                    operation), initialDelayMillis, TimeUnit.MILLISECONDS);
                    operation.setStuckCommandScheduledFuture(stuckCommandScheduledFuture);

                } else {

                    // since we are iterating over a queue ordered by start time, if this operation
                    // didn't meet the threshold then we know that no subsequent operations will
                    // meet the threshold and we can break here
                    break;
                }
            }
        }
    }

    private long getMillisUntilOperationReachesThreshold(OperationSafeImpl operation,
            int thresholdMillis) {
        long operationDurationNanoTime = System.nanoTime() - operation.getStartNanoTime();
        return thresholdMillis - TimeUnit.NANOSECONDS.toMillis(operationDurationNanoTime);
    }

    // it is very important that calls to pushTraceEvent() are wrapped in try block with
    // a finally block executing popTraceEvent()
    public TraceEventSafeImpl pushTraceEvent(ProbeExecution probeExecution) {

        // trace element limit is handled inside ProbeExecutionManagerImpl

        OperationSafeImpl currentOperation = currentOperationHolder.get();

        if (currentOperation == null) {
            currentOperation = new OperationSafeImpl(probeExecution);
            currentOperationHolder.set(currentOperation);
            operations.add(currentOperation);
            return currentOperation.getTrace().getRootElement();
        } else {
            return currentOperation.getTrace().pushElement(probeExecution);
        }
    }

    public void popTraceEvent(TraceEventSafeImpl traceEvent) {
        popTraceEvent(traceEvent, System.nanoTime());
    }

    // typically pop() methods don't require the element to pop, but for safety we are passing
    // in the element to pop just to make sure it is the one on top
    // (and if not we pop until we find it, preventing any nasty bugs from a forgotten pop
    // which could lead to an operation never being marked as completed)
    public void popTraceEvent(TraceEventSafeImpl traceEvent, long elementEndNanoTime) {

        OperationSafeImpl currentOperation = currentOperationHolder.get();
        currentOperation.getTrace().popElement(traceEvent, elementEndNanoTime);

        if (currentOperation.isCompleted()) {
            // we have popped off the root trace element
            cancelScheduledFuture(currentOperation.getCaptureStackTraceScheduledFuture());
            cancelScheduledFuture(currentOperation.getStuckCommandScheduledFuture());
            currentOperationHolder.remove();
            operations.remove(currentOperation);
            handleCompletedOperation(currentOperation);
        }
    }

    private void cancelScheduledFuture(ScheduledFuture<?> scheduledFuture) {

        if (scheduledFuture == null) {
            return;
        }

        boolean success = scheduledFuture.cancel(false);
        if (!success) {
            // execution failed due to an error (probably programming error)
            try {
                scheduledFuture.get();
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (ExecutionException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void handleCompletedOperation(OperationSafeImpl completedOperation) {

        AgentConfiguration configuration =
                AgentConfigurationServiceFactory.getService().getAgentConfiguration();

        long durationInNanoseconds =
                completedOperation.getTrace().getDurationInNanoseconds();

        // if the completed operation exceeded the given threshold then it is flushed to the
        // collector
        // we also check whether the completed operation was previously flushed in case threshold
        // was disabled or increased in the meantime in which case we still need to flush the rest
        int thresholdMillis = configuration.getThresholdMillis();
        boolean thresholdDisabled = (thresholdMillis == AgentConfiguration.THRESHOLD_DISABLED);
        if ((!thresholdDisabled && durationInNanoseconds >= TimeUnit.MILLISECONDS.toNanos(thresholdMillis))
                || completedOperation.isPreviouslyFlushed()) {

            // to prevent the thread from being held up while logging is performed, the logging is
            // queued up to be performed in a separate thread
            completedOperationLogExecutor.execute(new CompletedOperationLogCommand(
                    completedOperation));
        }
    }

    public void recordOperationSummaryData(String operationSummaryKey, long timeInNanoseconds) {
        OperationSafeImpl currentOperation = currentOperationHolder.get();
        // we only track aggregate info within an active operation
        if (currentOperation != null) {
            currentOperation.recordOperationSummaryData(operationSummaryKey, timeInNanoseconds);
        }
        // TODO also send to the metric subsystem
    }

    public TraceEventSafeImpl getCurrentTraceEvent() {
        OperationSafeImpl currentOperation = currentOperationHolder.get();
        if (currentOperation == null) {
            return null;
        } else {
            return currentOperation.getTrace().getCurrentElement();
        }
    }

    public OperationSafeImpl getCurrentOperation() {
        return currentOperationHolder.get();
    }

    public boolean isEnabled() {

        AgentConfiguration configuration =
                AgentConfigurationServiceFactory.getService().getAgentConfiguration();

        return configuration.isEnabled();
    }

    public boolean isCurrentOperationDisabled() {
        return currentOperationDisabledHolder.get() != null && currentOperationDisabledHolder.get();
    }

    public void setCurrentOperationDisabled(boolean disabled) {
        if (disabled) {
            currentOperationDisabledHolder.set(true);
        } else {
            // clear out thread local
            currentOperationDisabledHolder.set(null);
        }
    }

    // used by tests only
    public void clearCurrentOperation() {
        OperationSafeImpl currentOperation = currentOperationHolder.get();
        currentOperationHolder.remove();
        operations.remove(currentOperation);
    }

    public Collection<OperationSafeImpl> getOperationsExceptCurrent() {
        OperationSafeImpl currentOperation = currentOperationHolder.get();
        if (currentOperation == null) {
            return operations;
        } else {
            List<OperationSafeImpl> operationsExceptCurrent =
                    new ArrayList<OperationSafeImpl>(operations);
            operationsExceptCurrent.remove(currentOperation);
            return operationsExceptCurrent;
        }
    }

    public static Agent getInstance() {
        return INSTANCE;
    }

    // use daemon threads for executors so that they will not prevent JVM from
    // exiting normally
    private static final class DaemonThreadFactory implements ThreadFactory {

        private final ThreadFactory defaultThreadFactory;

        private DaemonThreadFactory() {
            defaultThreadFactory = Executors.defaultThreadFactory();
        }

        public Thread newThread(Runnable runnable) {
            Thread thread = defaultThreadFactory.newThread(runnable);
            thread.setDaemon(true);
            return thread;
        }
    }
}
