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
import org.jmonitor.ui.shared.BadCredentialsException;
import org.jmonitor.ui.shared.NotAuthenticated;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class ChangePasswordWidget extends Composite {

    // TODO what's the significance of the first generic parameter?
    interface Binder extends UiBinder<Widget, ChangePasswordWidget> {
    }

    private static final Binder BINDER = GWT.create(Binder.class);

    @UiField
    PasswordTextBox currentPasswordTextBox;

    @UiField
    PasswordTextBox newPasswordTextBox;

    @UiField
    PasswordTextBox confirmNewPasswordTextBox;

    @UiField
    Button changePasswordButton;

    private final UiServiceAsync uiService = GWT.create(UiService.class);

    // TODO use MVP event management
    private ChangePasswordCallback changePasswordCallback;

    // TODO use MVP event management
    private CancelCallback cancelCallback;

    public ChangePasswordWidget() {

        initWidget(BINDER.createAndBindUi(this));

        confirmNewPasswordTextBox.addKeyDownHandler(new KeyDownHandler() {
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    handleChangePassword();
                }
            }
        });
    }

    protected void onLoad() {
        super.onLoad();
        currentPasswordTextBox.setFocus(true);
    }

    @UiHandler("changePasswordButton")
    void handleChangePassword(ClickEvent event) {
        handleChangePassword();
    }

    private void handleChangePassword() {

        String currentPassword = currentPasswordTextBox.getText();
        String newPassword = newPasswordTextBox.getText();
        String confirmNewPassword = confirmNewPasswordTextBox.getText();

        if (!confirmNewPassword.equals(newPassword)) {
            Window.alert("New password fields do not match.");
            return;
        }

        uiService.changeAdminPassword(UiEntryPoint.getSessionId(), currentPassword, newPassword,
                new AsyncCallback<Void>() {
                    public void onSuccess(Void result) {
                        if (changePasswordCallback != null) {
                            changePasswordCallback.onChangePassword();
                        }
                    }
                    public void onFailure(Throwable t) {
                        if (t instanceof BadCredentialsException) {
                            Window.alert("Current password is incorrect.");
                        } else if (t instanceof NotAuthenticated) {
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

    public void registerChangePasswordCallback(ChangePasswordCallback changePasswordCallback) {
        this.changePasswordCallback = changePasswordCallback;
    }

    public void registerCancelCallback(CancelCallback cancelCallback) {
        this.cancelCallback = cancelCallback;
    }

    public interface ChangePasswordCallback {
        void onChangePassword();
    }

    public interface CancelCallback {
        void onCancel();
    }
}
