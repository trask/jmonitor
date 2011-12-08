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
package org.jmonitor.collector.impl.file;

import java.io.PrintWriter;

import org.apache.commons.lang.StringUtils;
import org.jmonitor.collector.impl.DataDestination;
import org.jmonitor.collector.impl.ErrorDestination;
import org.jmonitor.collector.impl.file.AtomicLogger.LoggerCallback;
import org.jmonitor.collector.service.model.Operation;
import org.jmonitor.configuration.service.model.AgentConfiguration;
import org.jmonitor.configuration.service.model.CollectorConfiguration;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public class FileDataDestination implements DataDestination, ErrorDestination {

    private final AtomicLogger atomicLogger;

    // cache log configuration so we know when a configuration update requires re-configuring the
    // atomic logger
    private String activeFilename;
    private String archiveFilenamePattern;
    private int maxHistory;
    private String maxFileSize;

    public FileDataDestination(CollectorConfiguration configuration) {

        activeFilename = configuration.getLogActiveFilename();
        archiveFilenamePattern = configuration.getLogArchiveFilenamePattern();
        maxHistory = configuration.getLogMaxHistory();
        // maxFileSize = configuration.getLogMaxFileSize();

        if (archiveFilenamePattern == null) {
            throw new NullPointerException();
        }

        this.atomicLogger = new AtomicLogger(archiveFilenamePattern, activeFilename, maxHistory,
                maxFileSize, OperationPrinter.HEADING1);
    }

    public void collect(final Operation operation) {

        atomicLogger.execute(new LoggerCallback() {
            public void doWithLogger(PrintWriter logger) {
                OperationPrinterHelper helper = new OperationPrinterHelper(operation, logger);
                helper.logOperation(AgentConfiguration.TRACE_ELEMENT_LIMIT_DISABLED);
            }
        });
    }

    public void collectFirstStuck(Operation operation) {
        collect(operation);
    }

    public void logError(String message) {
        atomicLogger.logError(message);
    }

    public void logError(String message, Throwable t) {
        atomicLogger.logError(message, t);
    }

    public void updateConfiguration(CollectorConfiguration configuration) {

        if (StringUtils.equals(configuration.getLogActiveFilename(), activeFilename)
                && StringUtils.equals(configuration.getLogArchiveFilenamePattern(),
                        archiveFilenamePattern) && configuration.getLogMaxHistory() == maxHistory) {
            // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
            // && StringUtils.equals(configuration.getLogMaxFileSize(), maxFileSize)) {

            // no changes to log configuration
            return;
        }

        // update cached log configuration (so we can continue to track changes)
        activeFilename = configuration.getLogActiveFilename();
        archiveFilenamePattern = configuration.getLogArchiveFilenamePattern();
        maxHistory = configuration.getLogMaxHistory();
        // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
        // maxFileSize = configuration.getLogMaxFileSize();

        // update logger
        atomicLogger.updateConfiguration(archiveFilenamePattern, activeFilename, maxHistory,
                maxFileSize);
    }
}
