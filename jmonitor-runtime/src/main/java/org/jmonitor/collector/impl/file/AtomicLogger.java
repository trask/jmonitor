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

import java.io.File;
import java.io.PrintWriter;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

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
public class AtomicLogger {

    private final String divider;

    // access to appender is guarded by lock
    private volatile RollingFileAppender<LoggerCallback> appender;

    private volatile String archiveFilenamePattern;
    private volatile String activeFilename;
    private volatile int maxHistory;
    private volatile String maxFileSize;

    // the underlying logback appender is already atomic
    // this lock is just used in order to update configuration on the fly
    private final Object lock = new Object();

    public AtomicLogger(String archiveFilenamePattern, String activeFilename, int maxHistory,
            String maxFileSize, String divider) {

        this.divider = divider;

        this.archiveFilenamePattern = archiveFilenamePattern;
        this.activeFilename = activeFilename;
        this.maxHistory = maxHistory;
        this.maxFileSize = maxFileSize;
    }

    public void execute(LoggerCallback command) {
        synchronized (lock) {
            ensureAppenderReady();
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
            ensureAppenderReady();
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
            ensureAppenderReady();
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

            this.archiveFilenamePattern = archiveFilenamePattern;
            this.activeFilename = activeFilename;
            this.maxHistory = maxHistory;
            this.maxFileSize = maxFileSize;
        }
    }

    // must be called under lock
    private void ensureAppenderReady() {
        if (appender == null) {
            appender = createAndStartAppender();
        }
        ensureFileSizeIsChecked();
    }

    // logback intentionally checks file size (for rollover) only every 16 log statements
    // but since our log statements are so large, it makes sense to check the
    // file size on every log statement
    // TODO submit logback issue to make this configurable
    // TODO don't perform this check if not using maxFileSize
    private void ensureFileSizeIsChecked() {
        for (int i = 0; i < 15; i++) {
            appender.doAppend(new LoggerCallback() {
                public void doWithLogger(PrintWriter logger) {}
            });
        }
    }

    private RollingFileAppender<LoggerCallback> createAndStartAppender() {

        boolean sizeBasedRolloverToo = StringUtils.isNotBlank(maxFileSize);

        LoggerContext context = new LoggerContext();

        // basic pattern for programmatically configuring logback seems to be
        // 1. instantiate (appender, rolling policy, triggering policy)
        // 2. configure
        // 3. link
        // 4. set context
        // 5. start

        // 1. instantiate
        RollingFileAppender<LoggerCallback> appender = new RollingFileAppender<LoggerCallback>();
        SizeAndTimeBasedFNATP<LoggerCallback> triggeringPolicy = null;
        if (sizeBasedRolloverToo) {
            triggeringPolicy = new SizeAndTimeBasedFNATP<LoggerCallback>();
        }
        TimeBasedRollingPolicy<LoggerCallback> rollingPolicy =
                new TimeBasedRollingPolicy<LoggerCallback>();

        // 2. configure
        if (StringUtils.isNotEmpty(activeFilename)) {
            appender.setFile(activeFilename);
        }
        appender.setEncoder(new AtomicLoggerEncoder());
        if (!archiveFilenamePattern.contains("/") && !archiveFilenamePattern.contains("\\")) {
            // there's an issue with logback SizeAndTimeBasedArchiveRemover.clean()
            // where if fileNamePattern has no explicit parent directory
            // it will not remove old archived logs (when maxHistory is used)
            rollingPolicy.setFileNamePattern(new File(".").getAbsolutePath() + "/"
                    + archiveFilenamePattern);
        } else {
            rollingPolicy.setFileNamePattern(archiveFilenamePattern);
        }
        if (maxHistory != 0) {
            rollingPolicy.setMaxHistory(maxHistory);
        }
        if (sizeBasedRolloverToo) {
            triggeringPolicy.setMaxFileSize(maxFileSize);
        }

        // 3. link
        rollingPolicy.setParent(appender);
        appender.setRollingPolicy(rollingPolicy);
        if (sizeBasedRolloverToo) {
            triggeringPolicy.setTimeBasedRollingPolicy(rollingPolicy);
            rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(triggeringPolicy);
        }

        // 4. set context
        appender.setContext(context);
        rollingPolicy.setContext(context);
        if (sizeBasedRolloverToo) {
            triggeringPolicy.setContext(context);
        }

        // 5. start - must be started in this order
        rollingPolicy.start();
        if (sizeBasedRolloverToo) {
            triggeringPolicy.start();
        }
        appender.start();

        return appender;
    }

    /**
     * Callback interface used by {@link AtomicLogger#execute(LoggerCallback)}.
     */
    public interface LoggerCallback {
        void doWithLogger(PrintWriter logger);
    }
}
