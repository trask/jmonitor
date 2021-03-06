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

package org.jmonitor.collector.impl.file;

import java.io.PrintWriter;

import org.apache.commons.lang.StringUtils;
import org.jmonitor.collector.service.model.Operation;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class OperationPrinter {

    public static final String HEADING1 = StringUtils.repeat("=", 80);
    public static final String HEADING2 = StringUtils.repeat("-", 80);

    private final PrintWriter out;
    private final int maxTraceEvents;

    public OperationPrinter(PrintWriter out, int maxTraceEvents) {

        if (out == null) {
            throw new IllegalArgumentException("out must be not null.");
        }

        this.out = out;
        this.maxTraceEvents = maxTraceEvents;
    }

    public void collect(final Operation operation) {

        OperationPrinterHelper helper =
                new OperationPrinterHelper(operation, out);
        helper.logOperation(maxTraceEvents);
        out.flush();
    }

    public void collectFirstStuck(Operation operation) {
        collect(operation);
    }
}
