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
import org.jmonitor.ui.shared.BadCredentialsException;
import org.jmonitor.ui.shared.NotAuthenticated;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
@RemoteServiceRelativePath("uiservice")
public interface UiService extends RemoteService {

    String authenticate(String username, String password) throws BadCredentialsException;

    String getActivityText(String sessionId) throws NotAuthenticated;

    FullConfigurationImpl getFullConfiguration(String sessionId) throws NotAuthenticated;

    void updateFullConfiguration(String sessionId, FullConfigurationImpl configuration)
            throws NotAuthenticated;

    void changeAdminPassword(String sessionId, String oldAdminPassword, String newAdminPassword)
            throws NotAuthenticated, BadCredentialsException;

    void generateTestLogStatement(String sessionId) throws NotAuthenticated;

    void generateTestEmail(String sessionId) throws NotAuthenticated;
}
