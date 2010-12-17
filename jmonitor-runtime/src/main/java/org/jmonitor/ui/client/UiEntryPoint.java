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

package org.jmonitor.ui.client;

import org.jmonitor.configuration.shared.model.FullConfigurationImpl;
import org.jmonitor.ui.client.configuration.widget.ConfigurationWidget;
import org.jmonitor.ui.client.configuration.widget.ConfigurationWidget.UpdateCallback;
import org.jmonitor.ui.client.widget.ChangePasswordWidget;
import org.jmonitor.ui.client.widget.LoginWidget;
import org.jmonitor.ui.client.widget.MainWidget;
import org.jmonitor.ui.client.widget.LoginWidget.LoginCallback;
import org.jmonitor.ui.client.widget.MainWidget.UpdateConfigurationCallback;
import org.jmonitor.ui.shared.NotAuthenticated;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class UiEntryPoint implements EntryPoint { // NOPMD for too many fields (for now)

    private static String sessionId;

    private VerticalPanel mainPanel;

    private final UiServiceAsync uiService = GWT.create(UiService.class);

    public void onModuleLoad() {

        mainPanel = new VerticalPanel();

        displayLoginWidget();

        RootPanel.get("ui").add(mainPanel);
    }

    private void displayLoginWidget() {

        LoginWidget loginWidget = new LoginWidget();

        loginWidget.registerLoginCallback(new LoginCallback() {
            public void onLogin(String sessionId) {
                UiEntryPoint.sessionId = sessionId;
                displayMainWidget();
            }
        });

        mainPanel.clear();
        mainPanel.add(loginWidget);
    }

    private void displayMainWidget() {

        MainWidget mainWidget = new MainWidget();

        mainWidget.registerUpdateConfigurationCallback(new UpdateConfigurationCallback() {
            public void onUpdateConfiguration() {
                displayConfigurationWidget();
            }
        });

        mainWidget.registerChangePasswordCallback(new MainWidget.ChangePasswordCallback() {
            public void onChangePassword() {
                displayChangePasswordWidget();
            }
        });

        mainPanel.clear();
        mainPanel.add(mainWidget);
    }

    private void displayConfigurationWidget() {

        uiService.getFullConfiguration(UiEntryPoint.sessionId,
                new AsyncCallback<FullConfigurationImpl>() {
                    public void onSuccess(FullConfigurationImpl configuration) {
                        // TODO this is a little odd
                        ConfigurationWidget configurationWidget = new ConfigurationWidget(configuration);
                        configurationWidget.registerUpdateCallback(new UpdateCallback() {
                            public void onUpdate() {
                                displayMainWidget();
                            }
                        });
                        configurationWidget.registerCancelCallback(new ConfigurationWidget.CancelCallback() {
                            public void onCancel() {
                                displayMainWidget();
                            }
                        });

                        mainPanel.clear();
                        mainPanel.add(configurationWidget);
                    }
                    public void onFailure(Throwable t) {
                        if (t instanceof NotAuthenticated) {
                            handleNotAuthenticatedError();
                        } else {
                            handleError(t);
                        }
                    }
                });
    }

    // this is called from various places when the session expires (after displaying a
    // "Your session has expired" alert)
    // TODO clean up this mechanism
    public static void handleNotAuthenticatedError() {
        Window.alert("Your session has expired");
        reload();
    }

    private static native void reload() /*-{
        $wnd.location.reload();
    }-*/;

    public static void handleError(Throwable t) {
        Window.alert("An error has occurred");
        reload();
    }

    private void displayChangePasswordWidget() {

        ChangePasswordWidget changePasswordWidget = new ChangePasswordWidget();

        changePasswordWidget.registerChangePasswordCallback(new ChangePasswordWidget.ChangePasswordCallback() {
            public void onChangePassword() {
                displayMainWidget();
            }
        });

        changePasswordWidget.registerCancelCallback(new ChangePasswordWidget.CancelCallback() {
            public void onCancel() {
                displayMainWidget();
            }
        });

        mainPanel.clear();
        mainPanel.add(changePasswordWidget);
    }

    public static String getSessionId() {
        return sessionId;
    }
}
