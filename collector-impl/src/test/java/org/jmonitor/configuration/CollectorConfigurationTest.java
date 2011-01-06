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

import org.jmonitor.collector.configuration.CollectorConfigurationServiceImpl;
import org.jmonitor.collector.service.model.CollectorConfiguration;
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

		CollectorConfiguration configuration = CollectorConfigurationServiceImpl
				.getInstance().getCollectorConfiguration();

		String oldPassword = configuration.getEmailPassword();

		// ensure new password is different from old password
		String newPlainPassword;
		if (oldPassword == null || oldPassword.length() > 20) {
			newPlainPassword = "password";
		} else {
			newPlainPassword = oldPassword + "x";
		}

		configuration.setEmailPassword(EncryptionUtils
				.encrypt(newPlainPassword));

		assertEquals(
				newPlainPassword,
				EncryptionUtils.decrypt(configuration.getEmailPassword()));

		// save configuration
		CollectorConfigurationServiceImpl.getInstance()
				.updateCollectorConfiguration(configuration);

		// check new configuration
		CollectorConfiguration updatedConfiguration = CollectorConfigurationServiceImpl
				.getInstance().getCollectorConfiguration();

		assertEquals(
				newPlainPassword,
				EncryptionUtils.decrypt(updatedConfiguration.getEmailPassword()));
	}
}
