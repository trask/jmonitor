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
package org.jmonitor.ui.client.configuration.widget;

import org.jmonitor.configuration.shared.model.FullConfigurationImpl;
import org.jmonitor.ui.client.UiEntryPoint;
import org.jmonitor.ui.client.UiService;
import org.jmonitor.ui.client.UiServiceAsync;
import org.jmonitor.ui.client.configuration.model.AgentConfigurationFormModel;
import org.jmonitor.ui.client.configuration.model.CollectorConfigurationFormModel;
import org.jmonitor.ui.client.configuration.model.ProbeConfigurationFormModel;
import org.jmonitor.ui.client.configuration.model.UiConfigurationFormModel;
import org.jmonitor.ui.shared.NotAuthenticated;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.pietschy.gwt.pectin.client.form.binding.FormBinder;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public class ConfigurationWidget extends Composite {

    interface Binder extends UiBinder<Widget, ConfigurationWidget> {}

    private static final Binder binder = GWT.create(Binder.class);

    private FormBinder wbinder = new FormBinder();

    private final UiServiceAsync uiService = GWT.create(UiService.class);

    @UiField
    CheckBox enabledCheckBox;

    @UiField
    TextBox thresholdMillisTextBox;

    @UiField
    TextBox stuckThresholdMillisTextBox;

    @UiField
    TextBox stackTraceInitialDelayMillisTextBox;

    @UiField
    TextBox stackTracePeriodMillisTextBox;

    @UiField
    TextBox maxTraceEventsPerOperationTextBox;

    @UiField
    TextBox logArchiveFilenamePatternTextBox;

    @UiField
    TextBox logActiveFilenameTextBox;

    @UiField
    TextBox logMaxHistoryTextBox;

    // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
    // @UiField
    // TextBox logMaxFileSizeTextBox;

    @UiField
    TextBox emailHostTextBox;

    @UiField
    TextBox emailSmtpPortTextBox;

    @UiField
    TextBox emailUsernameTextBox;

    @UiField
    TextBox emailPasswordTextBox;

    @UiField
    TextBox emailPasswordConfirmationTextBox;

    @UiField
    CheckBox emailSslCheckBox;

    @UiField
    TextBox emailFromAddressTextBox;

    @UiField
    TextBox emailToAddressesTextBox;

    @UiField
    TextBox maxTraceEventsPerEmail;

    @UiField
    Button updateButton;

    @UiField
    Button cancelButton;

    private FullConfigurationImpl fullConfiguration;

    private ProbeConfigurationFormModel probeConfiguration = new ProbeConfigurationFormModel();
    private AgentConfigurationFormModel agentConfiguration = new AgentConfigurationFormModel();
    private CollectorConfigurationFormModel collectorConfiguration =
            new CollectorConfigurationFormModel();
    private UiConfigurationFormModel uiConfiguration = new UiConfigurationFormModel();

    private Widget widget;

    public ConfigurationWidget(FullConfigurationImpl configuration) {

        widget = binder.createAndBindUi(this);

        probeConfiguration.setValue(configuration.getProbeConfiguration());
        agentConfiguration.setValue(configuration.getAgentConfiguration());
        collectorConfiguration.setValue(configuration.getCollectorConfiguration());
        uiConfiguration.setValue(configuration.getUiConfiguration());

        fullConfiguration = configuration;

        // set up binding for agent configuration
        wbinder.bind(agentConfiguration.isEnabled()).to(enabledCheckBox);
        wbinder.bind(agentConfiguration.getThresholdMillis()).to(thresholdMillisTextBox);
        wbinder.bind(agentConfiguration.getStuckThresholdMillis()).to(stuckThresholdMillisTextBox);
        wbinder.bind(agentConfiguration.getStackTraceInitialDelayMillis()).to(
                stackTraceInitialDelayMillisTextBox);
        wbinder.bind(agentConfiguration.getStackTracePeriodMillis()).to(
                stackTracePeriodMillisTextBox);
        wbinder.bind(agentConfiguration.getMaxTraceEventsPerOperation()).to(
                maxTraceEventsPerOperationTextBox);

        // set up binding for collector configuration
        wbinder.bind(collectorConfiguration.getLogArchiveFilenamePattern()).to(
                logArchiveFilenamePatternTextBox);
        wbinder.bind(collectorConfiguration.getLogActiveFilename()).to(logActiveFilenameTextBox);
        wbinder.bind(collectorConfiguration.getLogMaxHistory()).to(logMaxHistoryTextBox);
        // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
        // wbinder.bind(collectorConfiguration.getLogMaxFileSize()).to(logMaxFileSizeTextBox);
        wbinder.bind(collectorConfiguration.getEmailHost()).to(emailHostTextBox);
        wbinder.bind(collectorConfiguration.getEmailSmtpPort()).to(emailSmtpPortTextBox);
        wbinder.bind(collectorConfiguration.getEmailUsername()).to(emailUsernameTextBox);
        wbinder.bind(collectorConfiguration.getEmailPassword()).to(emailPasswordTextBox);
        wbinder.bind(collectorConfiguration.isEmailSsl()).to(emailSslCheckBox);
        wbinder.bind(collectorConfiguration.getEmailFromAddress()).to(emailFromAddressTextBox);
        wbinder.bind(collectorConfiguration.getEmailToAddresses()).to(emailToAddressesTextBox);
        wbinder.bind(collectorConfiguration.getMaxTraceEventsPerEmail()).to(maxTraceEventsPerEmail);

        // set email confirmation default
        emailPasswordConfirmationTextBox.setText(emailPasswordTextBox.getText());

        initWidget(widget);
    }

    @UiHandler("updateButton")
    void handleUpdate(ClickEvent e) {

        if (!emailPasswordTextBox.getText().equals("") && !emailPasswordTextBox.getText().equals(
                emailPasswordConfirmationTextBox.getText())) {

            // email password was changed, but doesn't match confirmation field
            Window.alert("Email passwords don't match");
            return;
        }

        probeConfiguration.commit();
        agentConfiguration.commit();
        collectorConfiguration.commit();
        uiConfiguration.commit();

        uiService.updateFullConfiguration(UiEntryPoint.sessionId, fullConfiguration,
                new AsyncCallback<Void>() {
                    public void onSuccess(Void result) {
                        updateCallback.onUpdate();
                    }
                    public void onFailure(Throwable t) {
                        if (t instanceof NotAuthenticated) {
                            UiEntryPoint.handleNotAuthenticatedError();
                        } else {
                            UiEntryPoint.handleError(t);
                        }
                    }
                });
    }

    @UiHandler("cancelButton")
    void handleCancel(ClickEvent e) {
        cancelCallback.onCancel();
    }

    private UpdateCallback updateCallback;

    public void registerUpdateCallback(UpdateCallback callback) {
        this.updateCallback = callback;
    }

    public static interface UpdateCallback {
        void onUpdate();
    }

    private CancelCallback cancelCallback;

    public void registerCancelCallback(CancelCallback cancelCallback) {
        this.cancelCallback = cancelCallback;
    }

    public static interface CancelCallback {
        void onCancel();
    }
}
