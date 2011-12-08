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
package org.jmonitor.collector.shared.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public class LoggerProxy implements Logger {

    private final Logger logger;

    public LoggerProxy(Logger logger) {
        this.logger = logger;
    }

    public String getName() {
        return logger.getName();
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled(marker);
    }

    public boolean isInfoEnabled() {
        return isInfoEnabled();
    }

    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled(marker);
    }

    public boolean isTraceEnabled() {
        return isTraceEnabled();
    }

    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled(marker);
    }

    public boolean isWarnEnabled() {
        return isWarnEnabled();
    }

    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled(marker);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String format, Object arg) {
        logger.error(format, arg);
    }

    public void error(String format, Object[] argArray) {
        logger.error(format, argArray);
    }

    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    public void error(Marker marker, String message) {
        logger.error(marker, message);
    }

    public void error(String format, Object arg1, Object arg2) {
        logger.error(format, arg1, arg2);
    }

    public void error(Marker marker, String format, Object arg) {
        logger.error(marker, format, arg);
    }

    public void error(Marker marker, String format, Object[] argArray) {
        logger.error(marker, format, argArray);
    }

    public void error(Marker marker, String message, Throwable t) {
        logger.error(marker, message, t);
    }

    public void error(Marker marker, String format, Object arg1, Object arg2) {
        logger.error(marker, format, arg1, arg2);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String format, Object arg) {
        logger.warn(format, arg);
    }

    public void warn(String format, Object[] argArray) {
        logger.warn(format, argArray);
    }

    public void warn(String message, Throwable t) {
        logger.warn(message, t);
    }

    public void warn(Marker marker, String message) {
        logger.warn(marker, message);
    }

    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(format, arg1, arg2);
    }

    public void warn(Marker marker, String format, Object arg) {
        logger.warn(marker, format, arg);
    }

    public void warn(Marker marker, String format, Object[] argArray) {
        logger.warn(marker, format, argArray);
    }

    public void warn(Marker marker, String message, Throwable t) {
        logger.warn(marker, message, t);
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        logger.warn(marker, format, arg1, arg2);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void info(String format, Object arg) {
        logger.info(format, arg);
    }

    public void info(String format, Object[] argArray) {
        logger.info(format, argArray);
    }

    public void info(String message, Throwable t) {
        logger.info(message, t);
    }

    public void info(Marker marker, String message) {
        logger.info(marker, message);
    }

    public void info(String format, Object arg1, Object arg2) {
        logger.info(format, arg1, arg2);
    }

    public void info(Marker marker, String format, Object arg) {
        logger.info(marker, format, arg);
    }

    public void info(Marker marker, String format, Object[] argArray) {
        logger.info(marker, format, argArray);
    }

    public void info(Marker marker, String message, Throwable t) {
        logger.info(marker, message, t);
    }

    public void info(Marker marker, String format, Object arg1, Object arg2) {
        logger.info(marker, format, arg1, arg2);
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void debug(String format, Object arg) {
        logger.debug(format, arg);
    }

    public void debug(String format, Object[] argArray) {
        logger.debug(format, argArray);
    }

    public void debug(String message, Throwable t) {
        logger.debug(message, t);
    }

    public void debug(Marker marker, String message) {
        logger.debug(marker, message);
    }

    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(format, arg1, arg2);
    }

    public void debug(Marker marker, String format, Object arg) {
        logger.debug(marker, format, arg);
    }

    public void debug(Marker marker, String format, Object[] argArray) {
        logger.debug(marker, format, argArray);
    }

    public void debug(Marker marker, String message, Throwable t) {
        logger.debug(marker, message, t);
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        logger.debug(marker, format, arg1, arg2);
    }

    public void trace(String message) {
        logger.trace(message);
    }

    public void trace(String format, Object arg) {
        logger.trace(format, arg);
    }

    public void trace(String format, Object[] argArray) {
        logger.trace(format, argArray);
    }

    public void trace(String message, Throwable t) {
        logger.trace(message, t);
    }

    public void trace(Marker marker, String message) {
        logger.trace(marker, message);
    }

    public void trace(String format, Object arg1, Object arg2) {
        logger.trace(format, arg1, arg2);
    }

    public void trace(Marker marker, String format, Object arg) {
        logger.trace(marker, format, arg);
    }

    public void trace(Marker marker, String format, Object[] argArray) {
        logger.trace(marker, format, argArray);
    }

    public void trace(Marker marker, String message, Throwable t) {
        logger.trace(marker, message, t);
    }

    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        logger.trace(marker, format, arg1, arg2);
    }
}
