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

package org.jmonitor.collector.service.model;

import java.io.Serializable;
import java.util.List;

import org.jmonitor.util.annotation.Comment;
import org.jmonitor.util.annotation.Password;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// currently supports single application / clustered configuration
// in the future may support multiple applications
public class CollectorConfiguration implements Serializable {

	private static final long serialVersionUID = 1L;

    private String logArchiveFilenamePattern = "jmonitor.%d{yyyy-MM-dd}.log";

    private String logActiveFilename = "";

    private int logMaxHistory = 0;

    // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
    // private String logMaxFileSize = "";

    private String emailHost = "";

    private int emailSmtpPort = 25;

    private String emailUsername = "";

    @Password
    private String emailPassword = "";

    private boolean emailSsl;

    private String emailFromAddress = "";

    private List<String> emailToAddresses;

    @Comment("used to reduce the size of alert emails, 0 means don't email any operations, "
            + "-1 means no limit")
    private int maxTraceEventsPerEmail = 100;

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
		this.logActiveFilename = logActiveFilename;
	}

	public int getLogMaxHistory() {
		return logMaxHistory;
	}

	public void setLogMaxHistory(int logMaxHistory) {
		this.logMaxHistory = logMaxHistory;
	}

	public String getEmailHost() {
		return emailHost;
	}

	public void setEmailHost(String emailHost) {
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
		this.emailUsername = emailUsername;
	}

	public String getEmailPassword() {
		return emailPassword;
	}

	public void setEmailPassword(String emailPassword) {
		this.emailPassword = emailPassword;
	}

	public boolean isEmailSsl() {
		return emailSsl;
	}

	public void setEmailSsl(boolean emailSsl) {
		this.emailSsl = emailSsl;
	}

	public String getEmailFromAddress() {
		return emailFromAddress;
	}

	public void setEmailFromAddress(String emailFromAddress) {
		this.emailFromAddress = emailFromAddress;
	}

	public List<String> getEmailToAddresses() {
		return emailToAddresses;
	}

	public void setEmailToAddresses(List<String> emailToAddresses) {
		this.emailToAddresses = emailToAddresses;
	}

	public int getMaxTraceEventsPerEmail() {
		return maxTraceEventsPerEmail;
	}

	public void setMaxTraceEventsPerEmail(int maxTraceEventsPerEmail) {
		this.maxTraceEventsPerEmail = maxTraceEventsPerEmail;
	}
}
