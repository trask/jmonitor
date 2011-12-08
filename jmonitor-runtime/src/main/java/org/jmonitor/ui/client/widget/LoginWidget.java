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
package org.jmonitor.ui.client.widget;

import org.jmonitor.ui.client.UiEntryPoint;
import org.jmonitor.ui.client.UiService;
import org.jmonitor.ui.client.UiServiceAsync;
import org.jmonitor.ui.shared.BadCredentialsException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
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
 * @author Trask Stalnaker
 * @since 1.0
 */
public class LoginWidget extends Composite {

    interface Binder extends UiBinder<Widget, LoginWidget> {}

    private static final Binder binder = GWT.create(Binder.class);

    private final UiServiceAsync uiService = GWT.create(UiService.class);

    @UiField
    TextBox usernameTextBox;

    @UiField
    PasswordTextBox passwordTextBox;

    @UiField
    Button loginButton;

    public LoginWidget() {

        initWidget(binder.createAndBindUi(this));

        passwordTextBox.addKeyDownHandler(new KeyDownHandler() {
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == 13) {
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
    void handleLogin(ClickEvent e) {
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

    private LoginCallback callback;

    public void registerLoginCallback(LoginCallback callback) {
        this.callback = callback;
    }

    public static interface LoginCallback {
        void onLogin(String sessionId);
    }
}
