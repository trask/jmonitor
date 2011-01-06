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

package org.jmonitor.collector.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.jmonitor.collector.configuration.CollectorConfigurationServiceFactory;
import org.jmonitor.collector.impl.common.AlertDestination;
import org.jmonitor.collector.impl.file.OperationPrinter;
import org.jmonitor.collector.service.model.CollectorConfiguration;
import org.jmonitor.collector.service.model.Operation;
import org.jmonitor.collector.shared.logging.CollectorServiceLoggerFactory;
import org.jmonitor.util.EncryptionUtils;
import org.slf4j.Logger;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class EmailAlertDestination implements AlertDestination {

    private static final Logger LOGGER =
            CollectorServiceLoggerFactory.getLogger(EmailAlertDestination.class);

    public void collect(Operation operation) {
        if (operation.isStuck() && operation.isCompleted()) {
            sendOperation("Unstuck Alert", operation);
        }
    }

    public void collectFirstStuck(Operation operation) {
        sendOperation("Stuck Alert", operation);
    }

    private static void sendOperation(String subject, Operation operation) {

        // create unstuck report for email
        StringWriter stuckReportWriter = new StringWriter();

        CollectorConfiguration configuration =
                CollectorConfigurationServiceFactory.getService().getCollectorConfiguration();

        // limit max trace elements to keep email size down
        OperationPrinter operationPrinter =
                new OperationPrinter(new PrintWriter(stuckReportWriter),
                        configuration.getMaxTraceEventsPerEmail());

        operationPrinter.collect(operation);

        // send unstuck alert email

        sendMessage(subject, stuckReportWriter.toString());
    }

    public static void sendMessage(String subject, String message) {

        CollectorConfiguration configuration =
                CollectorConfigurationServiceFactory.getService().getCollectorConfiguration();

        // get email parameters
        List<String> toAddresses = configuration.getEmailToAddresses();
        if (toAddresses == null || toAddresses.isEmpty()) {
            return;
        }

        String host = configuration.getEmailHost();
        int smtpPort = configuration.getEmailSmtpPort();
        String username = configuration.getEmailUsername();
        String password = EncryptionUtils.decrypt(configuration.getEmailPassword());
        boolean ssl = configuration.isEmailSsl();
        String fromAddress = configuration.getEmailFromAddress();

        boolean authenticate =
                username != null && username.length() != 0 && password != null
                        && password.length() != 0;

        // send email
        try {
            SimpleEmail email = new SimpleEmail();
            email.setHostName(host);
            email.setSmtpPort(smtpPort);
            for (String toAddress : toAddresses) {
                email.addTo(toAddress);
            }
            email.setFrom(fromAddress);
            email.setSubject(subject);
            email.setMsg(message);

            if (authenticate) {
                email.setAuthentication(username, password);
            }
            if (ssl) {
                email.setSSL(true);
            }
            email.send();
        } catch (EmailException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
