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
package org.jmonitor.agent.impl;

import org.aspectj.lang.ProceedingJoinPoint;
import org.jmonitor.agent.impl.model.ExecutionTraceElementSafeImpl;
import org.jmonitor.agent.impl.model.OperationSafeImpl;
import org.jmonitor.api.probe.ProbeExecution;
import org.jmonitor.api.probe.ProbeExecutionCreator;
import org.jmonitor.api.probe.ProbeExecutionManager;
import org.jmonitor.api.probe.ProbeExecutionWithUpdate;
import org.jmonitor.collector.shared.logging.CollectorServiceLoggerFactory;
import org.jmonitor.collector.shared.logging.LoggerImpl;
import org.jmonitor.configuration.client.ConfigurationServiceFactory;
import org.jmonitor.configuration.service.model.AgentConfiguration;
import org.jmonitor.configuration.service.model.ProbeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class used by AspectJ pointcuts.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class ProbeExecutionManagerImpl implements ProbeExecutionManager {

    private static final Logger logger =
            CollectorServiceLoggerFactory.getLogger(ProbeExecutionManagerImpl.class);

    public ProbeConfiguration getProbeConfiguration() {
        return ConfigurationServiceFactory.getService().getProbeConfiguration();
    }

    public Logger getProbeLogger(Class<?> clazz) {
        return new LoggerImpl(LoggerFactory.getLogger(clazz));
    }

    public Object execute(ProbeExecutionCreator probeExecutionCreator,
            ProceedingJoinPoint joinPoint) throws Throwable {

        return execute(probeExecutionCreator, joinPoint, null);
    }

    public Object execute(ProbeExecutionCreator probeExecutionCreator,
            ProceedingJoinPoint joinPoint, String operationSummaryKey) throws Throwable {

        return execute(probeExecutionCreator, joinPoint, operationSummaryKey, false);
    }

    public Object execute(ProbeExecutionCreator probeExecutionCreator,
            ProceedingJoinPoint joinPoint, String operationSummaryKey,
            boolean requiresExistingOperation) throws Throwable {

        AgentConfiguration configuration = ConfigurationServiceFactory.getService()
                .getAgentConfiguration();

        // this should be the first check to avoid any additional overhead when monitoring is
        // disabled
        if (!configuration.isEnabled()) {
            return proceedAndDisableNested(joinPoint);
        }

        if (requiresExistingOperation && !isInOperation()) {
            if (configuration.isWarnOnTraceEventOutsideOperation()) {
                logger.warn("jdbc call occurred outside of operation", new IllegalStateException());
            }
            return proceedAndDisableNested(joinPoint);
        }

        if (Agent.getInstance().isCurrentOperationDisabled()) {
            // monitor was enabled after the current operation had started
            // we still gather metric data
            return proceedAndRecordMetricData(joinPoint, operationSummaryKey);
        }

        OperationSafeImpl currentOperation = Agent.getInstance().getCurrentOperation();
        if (currentOperation != null
                && currentOperation.getExecutionTrace().getSize() >= configuration
                        .getMaxTraceEventsPerOperation()) {
            // trace limit has been exceeded
            // we still gather metric data
            return proceedAndRecordMetricData(joinPoint, operationSummaryKey);
        }

        ProbeExecution probeExecution = probeExecutionCreator.createProbeExecution();

        // start trace element
        ExecutionTraceElementSafeImpl traceElement = Agent.getInstance().pushTraceElement(
                probeExecution);

        try {
            return joinPoint.proceed();

        } finally {

            if (probeExecution instanceof ProbeExecutionWithUpdate) {
                ((ProbeExecutionWithUpdate) probeExecution).setProbeExecutionHolder(traceElement);
            }

            // we want to minimize the number of calls to the clock timer since they are relatively
            // expensive
            long endNanoTime = System.nanoTime();

            // record aggregate timing data
            if (operationSummaryKey != null) {
                // only record aggregate timing data for the top most servlet or filter
                Agent.getInstance().recordOperationSummaryData(operationSummaryKey,
                        endNanoTime - traceElement.getStartNanoTime());
            }

            // end trace element needs to be the last thing we do, at least when this is a root
            // trace element
            Agent.getInstance().popTraceElement(traceElement, endNanoTime);
        }
    }

    public Object proceedAndRecordMetricData(ProceedingJoinPoint joinPoint,
            String operationSummaryKey) throws Throwable {

        long startNanoTime = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long endNanoTime = System.nanoTime();
            // record aggregate timing data
            Agent.getInstance().recordOperationSummaryData(operationSummaryKey,
                    endNanoTime - startNanoTime);
        }
    }

    public void handleProbeExecutionUpdate(ProbeExecutionWithUpdate probeExecution)
            throws Exception {

        Agent.getInstance().getCurrentOperation().getExecutionTrace().justUpdatedCompletedElement(
                (ExecutionTraceElementSafeImpl) probeExecution.getProbeExecutionHolder());
    }

    public ProbeExecution getRootProbeExecution() {
        ExecutionTraceElementSafeImpl element = Agent.getInstance().getCurrentTraceElement();
        if (element == null) {
            return null;
        } else {
            return element.getProbeExecution();
        }
    }

    public boolean isEnabled() {
        return Agent.getInstance().isEnabled() && !Agent.getInstance().isCurrentOperationDisabled();
    }

    private boolean isInOperation() {
        return Agent.getInstance().getCurrentOperation() != null;
    }

    private Object proceedAndDisableNested(ProceedingJoinPoint joinPoint) throws Throwable {

        boolean previouslyDisabled = Agent.getInstance().isCurrentOperationDisabled();
        try {
            // disable current operation so that nested trace elements will not be captured even
            // if monitoring is re-enabled mid-operation
            Agent.getInstance().setCurrentOperationDisabled(true);
            return joinPoint.proceed();
        } finally {
            Agent.getInstance().setCurrentOperationDisabled(previouslyDisabled);
        }
    }
}
