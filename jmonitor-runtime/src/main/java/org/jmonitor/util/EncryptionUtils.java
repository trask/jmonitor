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

package org.jmonitor.util;

import org.jasypt.digest.StandardStringDigester;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class EncryptionUtils {

    private static final String ENCRYPTED_PREFIX = "encrypted;";

    // TODO allow this to be configured
    private static final String TEXT_ENCRYPTOR_PASSWORD = "!*Jm0N1t@R&^";

    // utility class
    private EncryptionUtils() {
    }

    public static boolean authenticate(String username, String plainPassword,
            String correctUsername, String correctEncryptedPassword) {

        if (username == null || username.length() == 0 || plainPassword == null
                || plainPassword.length() == 0) {
            // username or password is empty
            return false;
        }
        if (username.equals(correctUsername)) {
            // username is correct, now check password
            return checkPassword(correctEncryptedPassword, plainPassword);
        } else {
            // username is incorrect
            return false;
        }
    }

    public static String encryptPassword(String plainPassword) {

        if (isUnencrypted(plainPassword)) {
            // encrypt password
            StandardStringDigester digester = new StandardStringDigester();
            digester.initialize();
            return ENCRYPTED_PREFIX + digester.digest(plainPassword);
        } else {
            // TODO what is this conditional for?
            return plainPassword;
        }
    }

    public static boolean checkPassword(String encryptedPassword, String plainPassword) {

        if (encryptedPassword == null || encryptedPassword.length() == 0) {
            return false;
        }

        // strip off the "encrypted;" prefix
        String rawEncryptedPassword = encryptedPassword.substring(ENCRYPTED_PREFIX.length());

        // compare plainPassword and encryptedPassword
        StandardStringDigester digester = new StandardStringDigester();
        digester.initialize();
        return digester.matches(plainPassword, rawEncryptedPassword);
    }

    public static String encrypt(String text) {

        // encrypt text
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        encryptor.setPassword(TEXT_ENCRYPTOR_PASSWORD);
        return ENCRYPTED_PREFIX + encryptor.encrypt(text);
    }

    public static String decrypt(String encryptedText) {

        if (encryptedText == null || encryptedText.length() == 0) {
            return "";
        }

        // strip off the "encrypted;" prefix
        String rawEncryptedText = encryptedText.substring(ENCRYPTED_PREFIX.length());

        // return decrypted text
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        encryptor.setPassword(TEXT_ENCRYPTOR_PASSWORD);
        return encryptor.decrypt(rawEncryptedText);
    }

    public static boolean isUnencrypted(String text) {
        return text != null && text.length() != 0 && !text.startsWith(ENCRYPTED_PREFIX);
    }
}
