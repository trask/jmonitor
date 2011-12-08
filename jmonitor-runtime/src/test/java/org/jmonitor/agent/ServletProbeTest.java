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
package org.jmonitor.agent;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.jmonitor.agent.impl.Agent;
import org.jmonitor.agent.impl.model.ExecutionTraceElementSafeImpl;
import org.jmonitor.agent.impl.model.OperationSafeImpl;
import org.jmonitor.api.probe.ProbeExecution;
import org.jmonitor.extension.probe.ServletProbeExecution;
import org.jmonitor.mock.MockFilter;
import org.jmonitor.mock.MockFilterWithServlet;
import org.jmonitor.mock.MockProbeExecution;
import org.jmonitor.mock.MockServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Basic test of ServletAspect.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class ServletProbeTest {

    private ExecutionTraceElementSafeImpl rootTraceElement;

    @Before
    public void setUp() {
        // start MockTraceElement to capture trace elements
        rootTraceElement = Agent.getInstance().pushTraceElement(new MockProbeExecution());
    }

    @After
    public void tearDown() {
        // end MockTraceElement
        Agent.getInstance().popTraceElement(rootTraceElement);
    }

    @Test
    public void testServlet() throws ServletException, IOException {
        Servlet servlet = new MockServlet();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);

        // perform assertions
        OperationSafeImpl operation = Agent.getInstance().getCurrentOperation();
        Iterator<ExecutionTraceElementSafeImpl> iterator = operation.getExecutionTrace()
                .getElements().iterator();
        assertTrue(iterator.hasNext());
        ProbeExecution mockProbeExecution = iterator.next().getProbeExecution();
        assertNotNull(mockProbeExecution);
        assertTrue(mockProbeExecution instanceof MockProbeExecution);
        assertTrue(iterator.hasNext());
        ProbeExecution probeExecution = iterator.next().getProbeExecution();
        assertTrue(probeExecution instanceof ServletProbeExecution);
        ServletProbeExecution servletProbeExecution = (ServletProbeExecution) probeExecution;
        assertTrue(servletProbeExecution.getClazz().equals(MockServlet.class));
        assertNotNull(servletProbeExecution.getRequestURI());
    }

    @Test
    public void testFilter() throws ServletException, IOException {
        Filter filter = new MockFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        // perform assertions
        OperationSafeImpl operation = Agent.getInstance().getCurrentOperation();
        Iterator<ExecutionTraceElementSafeImpl> iterator = operation.getExecutionTrace()
                .getElements().iterator();
        assertTrue(iterator.hasNext());
        ProbeExecution mockProbeExecution = iterator.next().getProbeExecution();
        assertNotNull(mockProbeExecution);
        assertTrue(mockProbeExecution instanceof MockProbeExecution);
        assertTrue(iterator.hasNext());
        ProbeExecution traceElement = iterator.next().getProbeExecution();
        assertTrue(traceElement instanceof ServletProbeExecution);
        ServletProbeExecution servletProbeExecution = (ServletProbeExecution) traceElement;
        assertTrue(servletProbeExecution.getClazz().equals(MockFilter.class));
        assertNotNull(servletProbeExecution.getRequestURI());
    }

    @Test
    public void testCombination() throws ServletException, IOException {
        Filter filter = new MockFilterWithServlet();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        // perform assertions
        OperationSafeImpl operation = Agent.getInstance().getCurrentOperation();
        Iterator<ExecutionTraceElementSafeImpl> iterator = operation.getExecutionTrace()
                .getElements().iterator();
        assertTrue(iterator.hasNext());
        ProbeExecution mockProbeExecution = iterator.next().getProbeExecution();
        assertNotNull(mockProbeExecution);
        assertTrue(mockProbeExecution instanceof MockProbeExecution);
        assertTrue(iterator.hasNext());
        ProbeExecution probeExecution = iterator.next().getProbeExecution();
        assertTrue(probeExecution instanceof ServletProbeExecution);
        ServletProbeExecution filterTraceElement = (ServletProbeExecution) probeExecution;
        assertTrue(filterTraceElement.getClazz().equals(MockFilterWithServlet.class));
        assertNotNull(filterTraceElement.getRequestURI());
        assertTrue(iterator.hasNext());
        ProbeExecution nestedProbeExecution = iterator.next().getProbeExecution();
        assertTrue(nestedProbeExecution instanceof ServletProbeExecution);
        ServletProbeExecution servletTraceElement = (ServletProbeExecution) nestedProbeExecution;
        assertTrue(servletTraceElement.getClazz().equals(MockServlet.class));
    }
}
