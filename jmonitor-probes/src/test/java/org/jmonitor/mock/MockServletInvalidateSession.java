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

package org.jmonitor.mock;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmonitor.api.probe.ProbeExecutionManagerFactory;
import org.jmonitor.extension.probe.ServletProbeExecution;

/**
 * Mock servlet.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class MockServletInvalidateSession extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private ServletProbeExecution rootServletProbeExecution;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().invalidate();
        rootServletProbeExecution =
                (ServletProbeExecution) ProbeExecutionManagerFactory.getManager().getRootProbeExecution();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().invalidate();
        rootServletProbeExecution =
                (ServletProbeExecution) ProbeExecutionManagerFactory.getManager().getRootProbeExecution();
    }

    public ServletProbeExecution getRootServletProbeExecution() {
        return rootServletProbeExecution;
    }
}