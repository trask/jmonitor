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

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.jmonitor.collector.impl.file.AtomicLogger.LoggerCallback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class AtomicLoggerConfiguration {

    // configuration elements
    private final String archiveFilenamePattern;
    private final String activeFilename;
    private final int maxHistory;
    private final String maxFileSize;

    // calculated field based on presence of maxFileSize
    private final boolean sizeBasedRolloverToo;

    // logback components created based on above configuration
    private RollingFileAppender<LoggerCallback> appender;
    private SizeAndTimeBasedFNATP<LoggerCallback> triggeringPolicy;
    private TimeBasedRollingPolicy<LoggerCallback> rollingPolicy;

    public AtomicLoggerConfiguration(String archiveFilenamePattern, String activeFilename,
            int maxHistory, String maxFileSize) {

        this.archiveFilenamePattern = archiveFilenamePattern;
        this.activeFilename = activeFilename;
        this.maxHistory = maxHistory;
        this.maxFileSize = maxFileSize;

        sizeBasedRolloverToo = StringUtils.isNotBlank(maxFileSize);
    }

    public RollingFileAppender<LoggerCallback> createAppender() {

        // basic pattern for programmatically configuring logback seems to be
        // 1. instantiate (appender, rolling policy, triggering policy)
        // 2. configure
        // 3. link
        // 4. inject context
        // 5. start

        instantiateComponents();
        configureComponents();
        linkComponents();
        injectContextIntoComponents();
        startComponents();

        return appender;
    }

    private void instantiateComponents() {

        appender = new RollingFileAppender<LoggerCallback>();
        triggeringPolicy = null;
        if (sizeBasedRolloverToo) {
            triggeringPolicy = new SizeAndTimeBasedFNATP<LoggerCallback>();
        }
        rollingPolicy = new TimeBasedRollingPolicy<LoggerCallback>();
    }

    private void configureComponents() {

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
    }

    private void linkComponents() {

        rollingPolicy.setParent(appender);
        appender.setRollingPolicy(rollingPolicy);
        if (sizeBasedRolloverToo) {
            triggeringPolicy.setTimeBasedRollingPolicy(rollingPolicy);
            rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(triggeringPolicy);
        }
    }

    private void injectContextIntoComponents() {

        LoggerContext context = new LoggerContext();

        appender.setContext(context);
        rollingPolicy.setContext(context);
        if (sizeBasedRolloverToo) {
            triggeringPolicy.setContext(context);
        }
    }

    private void startComponents() {

        // must be started in this order
        rollingPolicy.start();
        if (sizeBasedRolloverToo) {
            triggeringPolicy.start();
        }
    }
}
