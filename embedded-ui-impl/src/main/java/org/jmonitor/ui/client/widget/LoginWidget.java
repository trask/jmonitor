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
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class LoginWidget extends Composite {

    // TODO what's the significance of the first generic parameter?
    interface Binder extends UiBinder<Widget, LoginWidget> {
    }

    private static final Binder BINDER = GWT.create(Binder.class);

    @UiField
    TextBox usernameTextBox;

    @UiField
    PasswordTextBox passwordTextBox;

    @UiField
    Button loginButton;

    private final UiServiceAsync uiService = GWT.create(UiService.class);

    // TODO use MVP event management
    private LoginCallback callback;

    public LoginWidget() {

        initWidget(BINDER.createAndBindUi(this));

        passwordTextBox.addKeyDownHandler(new KeyDownHandler() {
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    handleLogin();
                }
            }
        });
    }

    protected void onLoad() {
        super.onLoad();
        usernameTextBox.setFocus(true);
    }

    @UiHandler("loginButton")
    void handleLogin(ClickEvent event) {
        handleLogin();
    }

    private void handleLogin() {
        String username = usernameTextBox.getText();
        String password = passwordTextBox.getText();

        uiService.authenticate(username, password, new AsyncCallback<String>() {
            public void onSuccess(String sessionId) {
                if (callback != null) {
                    callback.onLogin(sessionId);
                }
                // Window.alert("success: " + sessionId);
            }
            public void onFailure(Throwable t) {
                if (t instanceof BadCredentialsException) {
                    Window.alert("Incorrect login");
                } else {
                    UiEntryPoint.handleError(t);
                }
            }
        });
    }

    public void registerLoginCallback(LoginCallback callback) {
        this.callback = callback;
    }

    public interface LoginCallback {
        void onLogin(String sessionId);
    }
}
