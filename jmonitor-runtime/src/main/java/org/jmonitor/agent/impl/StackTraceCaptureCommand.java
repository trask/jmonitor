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

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;

import org.jmonitor.agent.impl.model.OperationSafeImpl;
import org.jmonitor.agent.impl.model.SampledCallTreeSafeImpl;
import org.jmonitor.collector.shared.logging.CollectorServiceLoggerFactory;
import org.slf4j.Logger;

/**
 * Captures a stack trace for the thread executing an operation and stores the stack trace in the
 * {@link OperationSafeImpl}'s {@link SampledCallTreeSafeImpl}.
 * 
 * Designed to be scheduled and run in a separate thread as soon as the operation exceeds a given
 * threshold, and then again at specified intervals after that (e.g. via
 * {@link ScheduledExecutorService#scheduleWithFixedDelay}).
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class StackTraceCaptureCommand implements Runnable {

    private static final Logger logger = CollectorServiceLoggerFactory
            .getLogger(StackTraceCaptureCommand.class);

    // since it's possible for this scheduled command to live for a while after the operation has
    // completed we use a weak reference to make sure it won't prevent larger operations from being
    // garbage collected
    private final WeakReference<OperationSafeImpl> operationHolder;

    public StackTraceCaptureCommand(OperationSafeImpl operation) {
        this.operationHolder = new WeakReference<OperationSafeImpl>(operation);
    }

    public void run() {

        OperationSafeImpl operation = operationHolder.get();

        if (operation == null || operation.isCompleted()) {
            throw new IllegalStateException("don't log me, just want to tell"
                    + " the scheduler to cancel subsequent executions");
        }

        try {
            operation.captureStackTrace();
        } catch (Exception e) {
            // log and terminate this thread successfully
            logger.error(e.getMessage(), e);
        } catch (Error e) {
            // log and re-throw serious error which will terminate subsequent scheduled executions
            // (see ScheduledExecutorService.scheduleWithFixedDelay())
            logger.error(e.getMessage(), e);
            throw e;
        }
    }
}
