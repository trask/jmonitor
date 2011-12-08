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
package org.jmonitor.ui.client;

import org.jmonitor.configuration.shared.model.FullConfigurationImpl;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public interface UiServiceAsync {

    void authenticate(String username, String password, AsyncCallback<String> callback);

    void getActivityText(String sessionId, AsyncCallback<String> callback);

    void getFullConfiguration(String sessionId, AsyncCallback<FullConfigurationImpl> callback);

    void updateFullConfiguration(String sessionId, FullConfigurationImpl configuration,
            AsyncCallback<Void> callback);

    void changeAdminPassword(String sessionId, String oldAdminPassword, String newAdminPassword,
            AsyncCallback<Void> callback);

    void generateTestLogStatement(String sessionId, AsyncCallback<Void> callback);

    void generateTestEmail(String sessionId, AsyncCallback<Void> callback);
}
