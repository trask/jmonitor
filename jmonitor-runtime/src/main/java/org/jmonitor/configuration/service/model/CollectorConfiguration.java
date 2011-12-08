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
package org.jmonitor.configuration.service.model;

import java.util.List;

import org.jmonitor.util.annotation.Comment;
import org.jmonitor.util.annotation.Password;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
// currently supports single application / clustered configuration
// in the future may support multiple applications
public interface CollectorConfiguration {

    String getLogArchiveFilenamePattern();

    String getLogActiveFilename();

    int getLogMaxHistory();

    // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
    // String getLogMaxFileSize();

    String getEmailHost();

    int getEmailSmtpPort();

    String getEmailUsername();

    @Password
    String getEmailPassword();

    boolean isEmailSsl();

    String getEmailFromAddress();

    List<String> getEmailToAddresses();

    @Comment("used to reduce the size of alert emails, 0 means don't email any operations, "
            + "-1 means no limit")
    int getMaxTraceEventsPerEmail();
}
