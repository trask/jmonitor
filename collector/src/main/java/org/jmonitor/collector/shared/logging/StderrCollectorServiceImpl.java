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

package org.jmonitor.collector.shared.logging;

import org.jmonitor.collector.service.CollectorService;
import org.jmonitor.collector.service.model.CollectorConfiguration;
import org.jmonitor.collector.service.model.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class StderrCollectorServiceImpl implements CollectorService {

    // use regular slf4j logger factory
    private static final Logger LOGGER = LoggerFactory.getLogger(StderrCollectorServiceImpl.class);

    public void collectError(String msg) {
        LOGGER.error(msg);
    }

    public void collectError(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }

    public void collect(Operation operation) {
        throw new UnsupportedOperationException();
    }

    public void collectFirstStuck(Operation operation) {
        throw new UnsupportedOperationException();
    }

    public void updateConfiguration(CollectorConfiguration configuration) {
        throw new UnsupportedOperationException();
    }
}
