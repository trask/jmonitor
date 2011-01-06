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

import org.jmonitor.collector.configuration.CollectorConfigurationServiceFactory;
import org.jmonitor.collector.impl.common.AlertDestination;
import org.jmonitor.collector.impl.common.DataDestination;
import org.jmonitor.collector.impl.common.ErrorDestination;
import org.jmonitor.collector.impl.file.FileDataDestination;
import org.jmonitor.collector.service.CollectorService;
import org.jmonitor.collector.service.model.CollectorConfiguration;
import org.jmonitor.collector.service.model.Operation;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class CollectorServiceImpl implements CollectorService {

    private static final CollectorServiceImpl INSTANCE = new CollectorServiceImpl();

    private final DataDestination dataDestination;
    private final ErrorDestination errorDestination;
    private final AlertDestination alertDestination;

    private volatile CollectorConfiguration configuration;

    private CollectorServiceImpl() {

        configuration = CollectorConfigurationServiceFactory.getService().getCollectorConfiguration();

        // TODO add configuration listener, on change inspect if fileCollectorFilename
        // then call fileDataDestination.changeFilename()

        FileDataDestination fileDataDestination = new FileDataDestination(configuration);

        dataDestination = fileDataDestination;
        errorDestination = fileDataDestination;
        alertDestination = new EmailAlertDestination();
    }

    public void collect(Operation operation) {
        dataDestination.collect(operation);
        alertDestination.collect(operation);
    }

    public void collectFirstStuck(Operation operation) {
        dataDestination.collectFirstStuck(operation);
        alertDestination.collectFirstStuck(operation);
    }

    public void collectError(String message) {
        errorDestination.logError(message);
    }

    public void collectError(String message, Throwable t) {  // NOPMD for short variable name
        errorDestination.logError(message, t);
    }

    public void updateConfiguration(CollectorConfiguration configuration) {
        this.configuration = configuration;
        dataDestination.updateConfiguration(configuration);
    }

    public static CollectorServiceImpl getInstance() {
        return INSTANCE;
    }
}
