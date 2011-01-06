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

package org.jmonitor.ui.client.widget;

import org.jmonitor.ui.client.UiEntryPoint;
import org.jmonitor.ui.client.UiService;
import org.jmonitor.ui.client.UiServiceAsync;
import org.jmonitor.ui.shared.NotAuthenticated;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class MainWidget extends Composite { // NOPMD for too many methods

    // TODO what's the significance of the first generic parameter?
    interface Binder extends UiBinder<Widget, MainWidget> {
    }

    private static final Binder BINDER = GWT.create(Binder.class);

    @UiField
    Button refreshButton;

    @UiField
    Button updateConfigurationButton;

    @UiField
    Button changePasswordButton;

    @UiField
    Button generateTestLogStatementButton;

    @UiField
    Button generateTestEmailButton;

    @UiField
    Label activityTextLabel;

    private final UiServiceAsync uiService = GWT.create(UiService.class);

    // TODO use MVP event management
    private UpdateConfigurationCallback updateConfigurationCallback;

    // TODO use MVP event management
    private ChangePasswordCallback changePasswordCallback;

    public MainWidget() {
        initWidget(BINDER.createAndBindUi(this));
        updateActivityText();
    }

    @UiHandler("refreshButton")
    void handleRefresh(ClickEvent event) {
        updateActivityText();
    }

    @UiHandler("updateConfigurationButton")
    void handleUpdateConfiguration(ClickEvent event) {
        if (updateConfigurationCallback != null) {
            updateConfigurationCallback.onUpdateConfiguration();
        }
    }

    @UiHandler("changePasswordButton")
    void handleChangePassword(ClickEvent event) {
        if (changePasswordCallback != null) {
            changePasswordCallback.onChangePassword();
        }
    }

    @UiHandler("generateTestLogStatementButton")
    void handleGenerateTestLogStatement(ClickEvent event) {
        uiService.generateTestLogStatement(UiEntryPoint.getSessionId(), new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
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

    @UiHandler("generateTestEmailButton")
    void handleGenerateTestEmail(ClickEvent event) {
        uiService.generateTestEmail(UiEntryPoint.getSessionId(), new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
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

    private void updateActivityText() {
        uiService.getActivityText(UiEntryPoint.getSessionId(), new AsyncCallback<String>() {
            public void onSuccess(String activityText) {
                activityTextLabel.setText(activityText);
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

    public void registerUpdateConfigurationCallback(
            UpdateConfigurationCallback updateConfigurationCallback) {
        this.updateConfigurationCallback = updateConfigurationCallback;
    }

    public void registerChangePasswordCallback(ChangePasswordCallback changePasswordCallback) {
        this.changePasswordCallback = changePasswordCallback;
    }

    public interface UpdateConfigurationCallback {
        void onUpdateConfiguration();
    }

    public interface ChangePasswordCallback {
        void onChangePassword();
    }
}
