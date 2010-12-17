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

package org.jmonitor.configuration.shared.model;

import java.io.Serializable;
import java.util.List;

import org.jmonitor.configuration.service.model.CollectorConfiguration;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class CollectorConfigurationImpl implements CollectorConfiguration, Serializable {

    private static final long serialVersionUID = 1L;

    private String logArchiveFilenamePattern = "jmonitor.%d{yyyy-MM-dd}.log";

    private String logActiveFilename = "";

    private int logMaxHistory = 0;

    // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
    // private String logMaxFileSize = "";

    private String emailHost = "";

    private int emailSmtpPort = 25;

    private String emailUsername = "";

    private String emailPassword = "";

    private boolean emailSsl;

    private String emailFromAddress = "";

    private List<String> emailToAddresses;

    private int maxTraceEventsPerEmail = 100;

    // TODO research other strategies for creating immutable objects with lots of properties
    // (builders?)
    private boolean immutable;

    public void makeImmutable() {
        immutable = true;
    }

    public String getLogArchiveFilenamePattern() {
        return logArchiveFilenamePattern;
    }

    public void setLogArchiveFilenamePattern(String logArchiveFilenamePattern) {
        this.logArchiveFilenamePattern = logArchiveFilenamePattern;
    }

    public String getLogActiveFilename() {
        return logActiveFilename;
    }

    public void setLogActiveFilename(String logActiveFilename) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.logActiveFilename = logActiveFilename;
    }

    public int getLogMaxHistory() {
        return logMaxHistory;
    }

    public void setLogMaxHistory(int logMaxHistory) {
        this.logMaxHistory = logMaxHistory;
    }

    // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
    // public String getLogMaxFileSize() {
    // return logMaxFileSize;
    // }

    // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
    // public void setLogMaxFileSize(String logMaxFileSize) {
    // this.logMaxFileSize = logMaxFileSize;
    // }

    public String getEmailHost() {
        return emailHost;
    }

    public void setEmailHost(String emailHost) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.emailHost = emailHost;
    }

    public int getEmailSmtpPort() {
        return emailSmtpPort;
    }

    public void setEmailSmtpPort(int emailSmtpPort) {
        this.emailSmtpPort = emailSmtpPort;
    }

    public String getEmailUsername() {
        return emailUsername;
    }

    public void setEmailUsername(String emailUsername) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.emailUsername = emailUsername;
    }

    public String getEmailPassword() {
        return emailPassword;
    }

    // takes encrypted password
    public void setEmailPassword(String emailPassword) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.emailPassword = emailPassword;
    }

    public boolean isEmailSsl() {
        return emailSsl;
    }

    public void setEmailSsl(boolean emailSsl) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.emailSsl = emailSsl;
    }

    public String getEmailFromAddress() {
        return emailFromAddress;
    }

    public void setEmailFromAddress(String emailFromAddress) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.emailFromAddress = emailFromAddress;
    }

    public List<String> getEmailToAddresses() {
        return emailToAddresses;
    }

    public void setEmailToAddresses(List<String> emailToAddresses) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.emailToAddresses = emailToAddresses;
    }

    public int getMaxTraceEventsPerEmail() {
        return maxTraceEventsPerEmail;
    }

    public void setMaxTraceEventsPerEmail(int maxTraceEventsPerEmail) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.maxTraceEventsPerEmail = maxTraceEventsPerEmail;
    }
}
