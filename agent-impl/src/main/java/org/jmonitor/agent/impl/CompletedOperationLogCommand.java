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

import java.util.concurrent.ExecutorService;

import org.jmonitor.agent.impl.model.OperationSafeImpl;
import org.jmonitor.collector.service.CollectorService;
import org.jmonitor.collector.service.CollectorServiceFactory;
import org.jmonitor.collector.shared.logging.CollectorServiceLoggerFactory;
import org.slf4j.Logger;

/**
 * Logs all data captured for a completed {@link OperationSafeImpl}.
 * 
 * Designed to be run in a separate thread to minimize impact to application response times (e.g.
 * via {@link ExecutorService#execute(Runnable)}).
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class CompletedOperationLogCommand implements Runnable {

    private static final Logger LOGGER =
            CollectorServiceLoggerFactory.getLogger(CompletedOperationLogCommand.class);

    private final CollectorService collector = CollectorServiceFactory.getService();

    // this command is scheduled for immediate execution so we don't need to bother with weak
    // references to the Operation (which we use in StackTraceCaptureCommand and
    // StuckOperationLogCommand)
    private final OperationSafeImpl operation;

    public CompletedOperationLogCommand(OperationSafeImpl operation) {
        this.operation = operation;
    }

    public void run() {
        try {
            collector.collect(operation);
        } catch (Exception e) {
            // log and terminate this thread successfully
            LOGGER.error(e.getMessage(), e);
        } catch (Error e) {
            // log and re-throw serious error
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }
}
