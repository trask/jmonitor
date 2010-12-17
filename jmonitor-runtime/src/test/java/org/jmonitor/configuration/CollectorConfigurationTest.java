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

import static org.junit.Assert.assertEquals;

import org.jmonitor.configuration.impl.ConfigurationServiceImpl;
import org.jmonitor.configuration.service.model.CollectorConfiguration;
import org.jmonitor.configuration.shared.ConfigurationImplHelper;
import org.jmonitor.configuration.shared.model.CollectorConfigurationImpl;
import org.jmonitor.util.EncryptionUtils;
import org.junit.Test;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class CollectorConfigurationTest {

    @Test
    public void testEmailPassword() {

        CollectorConfiguration configuration =
                ConfigurationServiceImpl.getInstance().getCollectorConfiguration();

        String oldPassword = configuration.getEmailPassword();

        // ensure new password is different from old password
        String newPlainPassword;
        if (oldPassword == null || oldPassword.length() > 20) {
            newPlainPassword = "password";
        } else {
            newPlainPassword = oldPassword + "x";
        }

        // get an updatable copy
        CollectorConfigurationImpl mutableConfiguration =
                ConfigurationImplHelper.copyOf(configuration);

        mutableConfiguration.setEmailPassword(EncryptionUtils.encrypt(newPlainPassword));

        assertEquals(newPlainPassword,
                EncryptionUtils.decrypt(mutableConfiguration.getEmailPassword()));

        // save configuration
        ConfigurationServiceImpl.getInstance().updateCollectorConfiguration(mutableConfiguration);

        // check new configuration
        CollectorConfiguration updatedConfiguration =
                ConfigurationServiceImpl.getInstance().getCollectorConfiguration();

        assertEquals(newPlainPassword,
                EncryptionUtils.decrypt(updatedConfiguration.getEmailPassword()));
    }
}
