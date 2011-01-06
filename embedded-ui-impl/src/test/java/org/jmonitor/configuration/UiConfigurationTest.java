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

package org.jmonitor.configuration;

import static org.junit.Assert.assertTrue;

import org.jmonitor.ui.configuration.UiConfiguration;
import org.jmonitor.ui.configuration.UiConfigurationServiceImpl;
import org.jmonitor.util.EncryptionUtils;
import org.junit.Test;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class UiConfigurationTest {

    @Test
    public void testAdminPassword() {

        UiConfiguration configuration = UiConfigurationServiceImpl.getInstance().getUiConfiguration();

        String oldPassword = configuration.getAdminPassword();

        // ensure new password is different from old password
        String newPlainPassword;
        if (oldPassword.length() > 20) {
            newPlainPassword = "password";
        } else {
            newPlainPassword = oldPassword + "x";
        }

        configuration.setAdminPassword(EncryptionUtils.encryptPassword(newPlainPassword));

        assertTrue(EncryptionUtils.checkPassword(configuration.getAdminPassword(),
                newPlainPassword));

        // save configuration
        UiConfigurationServiceImpl.getInstance().updateUiConfiguration(configuration);

        // check new configuration
        UiConfiguration updatedConfiguration =
               UiConfigurationServiceImpl.getInstance().getUiConfiguration();

        assertTrue(EncryptionUtils.checkPassword(updatedConfiguration.getAdminPassword(),
                newPlainPassword));
    }
}
