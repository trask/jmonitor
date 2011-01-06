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

package org.jmonitor.ui.client.configuration.widget;

import org.jmonitor.ui.client.UiEntryPoint;
import org.jmonitor.ui.client.UiService;
import org.jmonitor.ui.client.UiServiceAsync;
import org.jmonitor.ui.client.configuration.model.AgentConfigurationFormModel;
import org.jmonitor.ui.client.configuration.model.CollectorConfigurationFormModel;
import org.jmonitor.ui.client.configuration.model.ProbeConfigurationFormModel;
import org.jmonitor.ui.client.configuration.model.UiConfigurationFormModel;
import org.jmonitor.ui.shared.FullConfiguration;
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
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO break this up into 4 separate widgets for the 4 different components
public class ConfigurationWidget extends Composite { // NOPMD for too many fields (for now)

    // TODO what's the significance of the first generic parameter?
    interface Binder extends UiBinder<Widget, ConfigurationWidget> {
    }

    private static final Binder BINDER = GWT.create(Binder.class);

    @UiField
    CheckBox enabledCheckBox;

    // TODO implement jmonitor:NumberBox for all of the number fields below
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

    private final FormBinder formBinder = new FormBinder();

    private final UiServiceAsync uiService = GWT.create(UiService.class);

    private final FullConfiguration fullConfiguration;

    private final ProbeConfigurationFormModel probeConfiguration = new ProbeConfigurationFormModel();
    private final AgentConfigurationFormModel agentConfiguration = new AgentConfigurationFormModel();
    private final CollectorConfigurationFormModel collectorConfiguration =
            new CollectorConfigurationFormModel();
    private final UiConfigurationFormModel uiConfiguration = new UiConfigurationFormModel();

    // TODO use MVP event management
    private UpdateCallback updateCallback;

    // TODO use MVP event management
    private CancelCallback cancelCallback;

    public ConfigurationWidget(FullConfiguration configuration) {

        Widget widget = BINDER.createAndBindUi(this);

        probeConfiguration.setBean(configuration.getProbeConfiguration());
        agentConfiguration.setBean(configuration.getAgentConfiguration());
        collectorConfiguration.setBean(configuration.getCollectorConfiguration());
        uiConfiguration.setBean(configuration.getUiConfiguration());

        fullConfiguration = configuration;

        // set up binding for agent configuration
        formBinder.bind(agentConfiguration.isEnabled()).to(enabledCheckBox);
        formBinder.bind(agentConfiguration.getThresholdMillis()).to(thresholdMillisTextBox);
        formBinder.bind(agentConfiguration.getStuckThresholdMillis()).to(stuckThresholdMillisTextBox);
        formBinder.bind(agentConfiguration.getStackTraceInitialDelayMillis()).to(
                stackTraceInitialDelayMillisTextBox);
        formBinder.bind(agentConfiguration.getStackTracePeriodMillis()).to(
                stackTracePeriodMillisTextBox);
        formBinder.bind(agentConfiguration.getMaxTraceEventsPerOperation()).to(
                maxTraceEventsPerOperationTextBox);

        // set up binding for collector configuration
        formBinder.bind(collectorConfiguration.getLogArchiveFilenamePattern()).to(
                logArchiveFilenamePatternTextBox);
        formBinder.bind(collectorConfiguration.getLogActiveFilename()).to(logActiveFilenameTextBox);
        formBinder.bind(collectorConfiguration.getLogMaxHistory()).to(logMaxHistoryTextBox);
        // COMMENTED OUT UNTIL LOGBACK DEFECT http://jira.qos.ch/browse/LBCORE-152 IS FIXED
        // formBinder.bind(collectorConfiguration.getLogMaxFileSize()).to(logMaxFileSizeTextBox);
        formBinder.bind(collectorConfiguration.getEmailHost()).to(emailHostTextBox);
        formBinder.bind(collectorConfiguration.getEmailSmtpPort()).to(emailSmtpPortTextBox);
        formBinder.bind(collectorConfiguration.getEmailUsername()).to(emailUsernameTextBox);
        formBinder.bind(collectorConfiguration.getEmailPassword()).to(emailPasswordTextBox);
        formBinder.bind(collectorConfiguration.isEmailSsl()).to(emailSslCheckBox);
        formBinder.bind(collectorConfiguration.getEmailFromAddress()).to(emailFromAddressTextBox);
        formBinder.bind(collectorConfiguration.getEmailToAddresses()).to(emailToAddressesTextBox);
        formBinder.bind(collectorConfiguration.getMaxTraceEventsPerEmail()).to(maxTraceEventsPerEmail);

        // set email confirmation default
        emailPasswordConfirmationTextBox.setText(emailPasswordTextBox.getText());

        initWidget(widget);
    }

    @UiHandler("updateButton")
    void handleUpdate(ClickEvent event) {

        if (!emailPasswordTextBox.getText().equals("")
                && !emailPasswordTextBox.getText().equals(
                        emailPasswordConfirmationTextBox.getText())) {

            // email password was changed, but doesn't match confirmation field

            // TODO display message, use pectin validation
            Window.alert("Email passwords don't match");
            return;
        }

        probeConfiguration.commit();
        agentConfiguration.commit();
        collectorConfiguration.commit();
        uiConfiguration.commit();

        uiService.updateFullConfiguration(UiEntryPoint.getSessionId(), fullConfiguration,
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
    void handleCancel(ClickEvent event) {
        cancelCallback.onCancel();
    }

    public void registerUpdateCallback(UpdateCallback callback) {
        this.updateCallback = callback;
    }

    public void registerCancelCallback(CancelCallback cancelCallback) {
        this.cancelCallback = cancelCallback;
    }

    public interface UpdateCallback {
        void onUpdate();
    }

    public interface CancelCallback {
        void onCancel();
    }
}
