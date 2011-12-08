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
package org.jmonitor.configuration.shared.model;

import java.io.Serializable;

import org.jmonitor.configuration.service.model.UiConfiguration;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public class UiConfigurationImpl implements UiConfiguration, Serializable {

    private static final long serialVersionUID = 1L;

    private String adminUsername = "admin";

    // TODO revisit how default password is set
    private String adminPassword;

    private boolean immutable;

    public void makeImmutable() {
        immutable = true;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    // takes encrypted password
    public void setAdminPassword(String adminPassword) {
        if (immutable) {
            throw new IllegalStateException("this instance is immutable");
        }
        this.adminPassword = adminPassword;
    }
}
