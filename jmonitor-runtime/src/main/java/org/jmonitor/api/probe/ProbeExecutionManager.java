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
package org.jmonitor.api.probe;

import org.aspectj.lang.ProceedingJoinPoint;
import org.jmonitor.configuration.service.model.ProbeConfiguration;
import org.slf4j.Logger;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public interface ProbeExecutionManager {

    ProbeConfiguration getProbeConfiguration();

    Logger getProbeLogger(Class<?> clazz);

    Object execute(ProbeExecutionCreator probeExecutionCreator, ProceedingJoinPoint joinPoint)
            throws Throwable;

    Object execute(ProbeExecutionCreator probeExecutionCreator, ProceedingJoinPoint joinPoint,
            String operationSummaryKey) throws Throwable;

    Object execute(ProbeExecutionCreator probeExecutionCreator, ProceedingJoinPoint joinPoint,
            String operationSummaryKey, boolean requiresExistingOperation) throws Throwable;

    Object proceedAndRecordMetricData(ProceedingJoinPoint joinPoint, String operationSummaryKey)
            throws Throwable;

    void handleProbeExecutionUpdate(ProbeExecutionWithUpdate probeExecution) throws Exception;

    ProbeExecution getRootProbeExecution();

    boolean isEnabled();
}
