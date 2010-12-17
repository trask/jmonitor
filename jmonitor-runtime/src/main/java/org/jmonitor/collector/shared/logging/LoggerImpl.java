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

package org.jmonitor.collector.shared.logging;

import org.jmonitor.collector.client.CollectorServiceFactory;
import org.jmonitor.collector.service.CollectorService;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class LoggerImpl extends LoggerProxy {

    private volatile CollectorService collector;

    public LoggerImpl(Logger logger) {
        super(logger);
    }

    @Override
    public boolean isErrorEnabled() {
        // just in case logging code calls this directly (which is not typical for error level)
        // we don't want callers to short circuit error logging since we still want to send these
        // to the collector
        // underlying slf4j implementation (e.g. log4j) will still check it's own levels
        // on calls to error() so this will not prevent log4j configuration from suppressing
        // errors
        return true;
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        // see comment above in isErrorEnabled()
        return true;
    }

    @Override
    public void error(String message) {
        super.error(message);
        getCollector().collectError(message);
    }

    @Override
    public void error(String format, Object arg) {
        super.error(format, arg);
        FormattingTuple formattingTuple = MessageFormatter.format(format, arg);
        getCollector().collectError(formattingTuple.getMessage());
    }

    @Override
    public void error(String format, Object[] argArray) {
        super.error(format, argArray);
        FormattingTuple formattingTuple = MessageFormatter.format(format, argArray);
        getCollector().collectError(formattingTuple.getMessage());
    }

    @Override
    public void error(String message, Throwable t) {
        super.error(message, t);
        getCollector().collectError(message, t);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        FormattingTuple formattingTuple = MessageFormatter.format(format, arg1, arg2);
        getCollector().collectError(formattingTuple.getMessage());
    }

    @Override
    public void error(Marker marker, String message) {
        super.error(marker, message);
        // currently not supporting markers
        getCollector().collectError(message);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        super.error(marker, format, arg);
        // currently not supporting markers
        FormattingTuple formattingTuple = MessageFormatter.format(format, arg);
        getCollector().collectError(formattingTuple.getMessage());
    }

    @Override
    public void error(Marker marker, String format, Object[] argArray) {
        super.error(marker, format, argArray);
        // currently not supporting markers
        FormattingTuple formattingTuple = MessageFormatter.format(format, argArray);
        getCollector().collectError(formattingTuple.getMessage());
    }

    @Override
    public void error(Marker marker, String message, Throwable t) {
        super.error(marker, message, t);
        // currently not supporting markers
        getCollector().collectError(message, t);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        super.error(marker, format, arg1, arg2);
        // currently not supporting markers
        FormattingTuple formattingTuple = MessageFormatter.format(format, arg1, arg2);
        getCollector().collectError(formattingTuple.getMessage());
    }

    private CollectorService getCollector() {
        if (collector == null) {
            // try to get it from the service factory
            collector = CollectorServiceFactory.getService();
        }
        if (collector == null) {
            // it's still null (this can happen during start-up since there is a cyclic relationship
            // between logger and configuration)
            return new StderrCollectorServiceImpl();
        }
        return collector;
    }
}
