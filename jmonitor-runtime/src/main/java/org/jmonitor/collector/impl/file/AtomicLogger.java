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

package org.jmonitor.collector.impl.file;

import java.io.PrintWriter;
import java.util.Date;

import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;

/**
 * A simple atomic logger with roll over. It allows large log statements (via
 * {@link #execute(LoggerCallback)}) to be streamed as a single atomic group without being
 * interspersed with other log statements. Roll over support is provided by Logback's
 * RollingFileAppender.
 * 
 * Using logback under the covers is about 5% slower than raw file logging, which is more than an
 * acceptable trade-off because we can leverage logback's roll over choices, as well as logback's
 * handling of internal logging issues such as IOExceptions. (see AtomicLoggerPerformanceMain under
 * unit test directory for details on overhead measurement)
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class AtomicLogger { // NOPMD for too many methods

    private final String divider;

    // access to appender is guarded by lock
    private volatile RollingFileAppender<LoggerCallback> appender;

    private volatile AtomicLoggerConfiguration configuration;

    // the underlying logback appender is already atomic
    // this lock is just used in order to update configuration on the fly
    private final Object lock = new Object();

    public AtomicLogger(String archiveFilenamePattern, String activeFilename, int maxHistory,
            String maxFileSize, String divider) {

        this.divider = divider;
        this.configuration =
                new AtomicLoggerConfiguration(archiveFilenamePattern, activeFilename, maxHistory,
                        maxFileSize);
    }

    public void execute(LoggerCallback command) {
        synchronized (lock) {
            ensureAppenderIsReady();
            appender.doAppend(command);
        }
    }

    // for logging warning messages
    public void logWarning(Object message) {
        logWarning(message, null);
    }

    // for logging warning messages
    public void logWarning(final Object message, final Throwable throwable) {
        synchronized (lock) {
            ensureAppenderIsReady();
            appender.doAppend(new LoggerCallback() {
                public void doWithLogger(PrintWriter out) {
                    out.println(divider);
                    out.println("WARNING");
                    out.println(FormatUtils.formatWithMilliseconds(new Date()));
                    out.println(message);
                    if (throwable != null) {
                        if (throwable.getMessage() != null) {
                            out.println(throwable.getMessage());
                        }
                        throwable.printStackTrace(out);
                    }
                    out.println(divider);
                    out.println();
                }
            });
        }
    }

    // for logging warning messages
    public void logError(String message) {
        logError(message, null);
    }

    // for logging warning messages
    public void logError(final String message, final Throwable throwable) {
        synchronized (lock) {
            ensureAppenderIsReady();
            appender.doAppend(new LoggerCallback() {
                public void doWithLogger(PrintWriter out) {
                    out.println(divider);
                    out.println("ERROR");
                    out.println(FormatUtils.formatWithMilliseconds(new Date()));
                    out.println(message);
                    if (throwable != null) {
                        out.println(throwable.getMessage());
                        throwable.printStackTrace(out);
                    }
                    out.println(divider);
                    out.println();
                }
            });
        }
    }

    public void rollover() {
        synchronized (lock) {
            if (appender != null) {
                appender.rollover();
            }
        }
    }

    public void stop() {
        synchronized (lock) {
            if (appender != null) {
                appender.stop();
            }
        }
    }

    public void updateConfiguration(String archiveFilenamePattern, String activeFilename,
            int maxHistory, String maxFileSize) {

        synchronized (lock) {
            if (appender != null) {
                appender.stop();
                appender = null;
            }
            configuration =
                    new AtomicLoggerConfiguration(archiveFilenamePattern, activeFilename,
                            maxHistory, maxFileSize);
        }
    }

    // must be called under the internal lock
    private void ensureAppenderIsReady() {

        if (appender == null) {
            appender = configuration.createAppender();
            appender.start();
        }

        if (appender.getTriggeringPolicy() instanceof SizeAndTimeBasedFNATP<?>) {

            // SizeAndTimeBasedFNATP intentionally checks file size (for rollover) only every 16 log
            // statements, see SizeAndTimeBasedFNATP.isTriggeringEvent()
            // but since our log statements are so large, it makes sense to check the
            // file size on every log statement
            // TODO submit logback issue to make this configurable
            // TODO don't perform this check if not using maxFileSize
            final int logbackInvocationsPerFileSizeCheck = 16;

            // we make enough fake calls to ensure that either one of the fake calls or the
            // subsequent
            // real call will trigger the file size check in logback
            for (int i = 0; i < logbackInvocationsPerFileSizeCheck - 1; i++) {
                appender.doAppend(new LoggerCallback() {
                    public void doWithLogger(PrintWriter logger) {
                    }
                });
            }
        }
    }

    /**
     * Callback interface used by {@link AtomicLogger#execute(LoggerCallback)}.
     */
    public interface LoggerCallback {
        void doWithLogger(PrintWriter logger);
    }
}
