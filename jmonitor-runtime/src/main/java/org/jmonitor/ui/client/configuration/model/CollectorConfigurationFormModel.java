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
package org.jmonitor.ui.client.configuration.model;

import java.util.List;

import org.jmonitor.configuration.shared.model.CollectorConfigurationImpl;

import com.google.gwt.core.client.GWT;
import com.pietschy.gwt.pectin.client.bean.BeanModelProvider;
import com.pietschy.gwt.pectin.client.form.FieldModel;
import com.pietschy.gwt.pectin.client.form.FormModel;
import com.pietschy.gwt.pectin.client.form.FormattedFieldModel;
import com.pietschy.gwt.pectin.client.format.IntegerFormat;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public class CollectorConfigurationFormModel extends FormModel {

    public static abstract class CollectorConfigurationProvider extends
            BeanModelProvider<CollectorConfigurationImpl> {}

    private CollectorConfigurationProvider configurationProvider =
            GWT.create(CollectorConfigurationProvider.class);

    private FieldModel<String> logArchiveFilenamePattern;
    private FieldModel<String> logActiveFilename;
    private FormattedFieldModel<Integer> logMaxHistory;
    // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
    // private FieldModel<String> logMaxFileSize;
    private FieldModel<String> emailHost;
    private FormattedFieldModel<Integer> emailSmtpPort;
    private FieldModel<String> emailUsername;
    private FieldModel<String> emailPassword;
    private FieldModel<Boolean> emailSsl;
    private FieldModel<String> emailFromAddress;
    // TODO use FormattedFieldModel<List<String>> (may need to modify pectin library)
    @SuppressWarnings("rawtypes")
    private FormattedFieldModel<List> emailToAddresses;
    private FormattedFieldModel<Integer> maxTraceEventsPerEmail;

    public CollectorConfigurationFormModel() {

        logArchiveFilenamePattern =
                fieldOfType(String.class).boundTo(configurationProvider,
                        "logArchiveFilenamePattern");
        logActiveFilename =
                fieldOfType(String.class).boundTo(configurationProvider, "logActiveFilename");
        logMaxHistory =
                formattedFieldOfType(Integer.class).using(new IntegerFormat()).boundTo(
                        configurationProvider, "logMaxHistory");
        // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
        // logMaxFileSize = fieldOfType(String.class).boundTo(configurationProvider,
        // "logMaxFileSize");
        emailHost = fieldOfType(String.class).boundTo(configurationProvider, "emailHost");
        emailSmtpPort =
                formattedFieldOfType(Integer.class).using(new IntegerFormat()).boundTo(
                        configurationProvider, "emailSmtpPort");
        emailUsername = fieldOfType(String.class).boundTo(configurationProvider, "emailUsername");
        emailPassword = fieldOfType(String.class).boundTo(configurationProvider, "emailPassword");
        emailSsl = fieldOfType(Boolean.class).boundTo(configurationProvider, "emailSsl");
        emailFromAddress =
                fieldOfType(String.class).boundTo(configurationProvider, "emailFromAddress");
        emailToAddresses = formattedFieldOfType(List.class)
                .using(new CommaSeparatedListFormatter()).boundTo(configurationProvider,
                        "emailToAddresses");
        maxTraceEventsPerEmail =
                formattedFieldOfType(Integer.class).using(new IntegerFormat()).boundTo(
                        configurationProvider, "maxTraceEventsPerEmail");
    }

    public FieldModel<String> getLogArchiveFilenamePattern() {
        return logArchiveFilenamePattern;
    }

    public FieldModel<String> getLogActiveFilename() {
        return logActiveFilename;
    }

    public FormattedFieldModel<Integer> getLogMaxHistory() {
        return logMaxHistory;
    }

    // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
    // public FieldModel<String> getLogMaxFileSize() {
    // return logMaxFileSize;
    // }

    public FieldModel<String> getEmailHost() {
        return emailHost;
    }

    public FormattedFieldModel<Integer> getEmailSmtpPort() {
        return emailSmtpPort;
    }

    public FieldModel<String> getEmailUsername() {
        return emailUsername;
    }

    public FieldModel<String> getEmailPassword() {
        return emailPassword;
    }

    public FieldModel<Boolean> isEmailSsl() {
        return emailSsl;
    }

    public FieldModel<String> getEmailFromAddress() {
        return emailFromAddress;
    }

    // TODO use FormattedFieldModel<List<String>> (may need to modify pectin library)
    @SuppressWarnings("rawtypes")
    public FormattedFieldModel<List> getEmailToAddresses() {
        return emailToAddresses;
    }

    public FormattedFieldModel<Integer> getMaxTraceEventsPerEmail() {
        return maxTraceEventsPerEmail;
    }

    public void setValue(CollectorConfigurationImpl configuration) {
        configurationProvider.setValue(configuration);
    }

    public void commit() {
        configurationProvider.commit();
    }
}
