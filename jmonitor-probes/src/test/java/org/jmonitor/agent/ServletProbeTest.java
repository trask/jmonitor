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

package org.jmonitor.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.jmonitor.agent.impl.Agent;
import org.jmonitor.agent.impl.model.OperationSafeImpl;
import org.jmonitor.agent.impl.model.TraceEventSafeImpl;
import org.jmonitor.api.probe.ProbeExecution;
import org.jmonitor.extension.probe.ServletProbeExecution;
import org.jmonitor.mock.MockFilter;
import org.jmonitor.mock.MockFilterWithServlet;
import org.jmonitor.mock.MockProbeExecution;
import org.jmonitor.mock.MockServlet;
import org.jmonitor.mock.MockServletInvalidateSession;
import org.jmonitor.mock.WrapInMockProbeExecution;
import org.jmonitor.test.configuration.ConfigureAgentEnabled;
import org.jmonitor.test.configuration.ConfigureServletProbeUsernameSessionAttribute;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

/**
 * Basic test of ServletAspect.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class ServletProbeTest {

    @Test
    @WrapInMockProbeExecution
    public void testServlet() throws ServletException, IOException {

        Servlet servlet = new MockServlet();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);

        ServletProbeExecution servletProbeExecution = getRootServletProbeExecution();
        assertEquals(MockServlet.class, servletProbeExecution.getClazz());
        assertNotNull(servletProbeExecution.getRequestURI());
    }

    @Test
    @WrapInMockProbeExecution
    public void testFilter() throws ServletException, IOException {

        Filter filter = new MockFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        ServletProbeExecution servletProbeExecution = getRootServletProbeExecution();
        assertEquals(MockFilter.class, servletProbeExecution.getClazz());
        assertNotNull(servletProbeExecution.getRequestURI());
    }

    @Test
    @WrapInMockProbeExecution
    public void testCombination() throws ServletException, IOException {

        Filter filter = new MockFilterWithServlet();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        // perform assertions
        OperationSafeImpl operation = Agent.getInstance().getCurrentOperation();
        Iterator<TraceEventSafeImpl> iterator =
                operation.getTrace().getEvents().iterator();
        assertTrue(iterator.hasNext());
        ProbeExecution mockProbeExecution = iterator.next().getProbeExecution();
        assertNotNull(mockProbeExecution);
        assertTrue(mockProbeExecution instanceof MockProbeExecution);
        assertTrue(iterator.hasNext());
        ProbeExecution probeExecution = iterator.next().getProbeExecution();
        assertTrue(probeExecution instanceof ServletProbeExecution);
        ServletProbeExecution filterProbeExecution = (ServletProbeExecution) probeExecution;
        assertTrue(filterProbeExecution.getClazz().equals(MockFilterWithServlet.class));
        assertNotNull(filterProbeExecution.getRequestURI());
        assertTrue(iterator.hasNext());
        ProbeExecution nestedProbeExecution = iterator.next().getProbeExecution();
        assertTrue(nestedProbeExecution instanceof ServletProbeExecution);
        ServletProbeExecution servletProbeExecution = (ServletProbeExecution) nestedProbeExecution;
        assertEquals(MockServlet.class, servletProbeExecution.getClazz());
    }

    @Test
    @ConfigureAgentEnabled(false)
    @ConfigureServletProbeUsernameSessionAttribute("username")
    @WrapInMockProbeExecution
    public void testUsernameSessionAttributeCaptureUnderDisabledAgent() throws ServletException,
            IOException {

        Servlet servlet = new MockServlet();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("username", "abc");

        // perform assertions
        OperationSafeImpl operation = Agent.getInstance().getCurrentOperation();
        assertNull(operation);
    }

    // TODO build out this unit test
//    @Test
//    @WrapInMockProbeExecution
//    public void testRequestParameters() {
//
//        // TODO use "combination" filter / servlet, because there was a previous bug due to this
//        // with TC request parameters
//    }

    // TODO build out this unit test
//    @Test
//    @WrapInMockProbeExecution
//    public void testUsername() {
//    }

    // TODO build out this unit test
//    @Test
//    @WrapInMockProbeExecution
//    public void testSessionAttributes() {
//    }

    // TODO implement all(?) of these tests by injecting custom collector and setting threshold=0
    // and then checking data as it comes in to the collector
    // this would test collector data, as well as avoid issue of not being able to get operation
    // after it completes (see hack below, servlet.getRootProbeExecution())
    @Test
    public void testSessionInvalidate() throws IOException, ServletException {

        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletInvalidateSession servlet = new MockServletInvalidateSession();
        servlet.service(request, response);

        // perform assertions
        ServletProbeExecution servletProbeExecution = servlet.getRootServletProbeExecution();
        assertTrue(StringUtils.isNotEmpty(servletProbeExecution.getSessionIdInitialValue()));
        assertTrue(StringUtils.isEmpty(servletProbeExecution.getSessionIdUpdatedValue()));
    }

    private ServletProbeExecution getRootServletProbeExecution() {

        OperationSafeImpl operation = Agent.getInstance().getCurrentOperation();
        Iterator<TraceEventSafeImpl> iterator =
                operation.getTrace().getEvents().iterator();
        assertTrue(iterator.hasNext());
        ProbeExecution mockProbeExecution = iterator.next().getProbeExecution();
        assertNotNull(mockProbeExecution);
        assertTrue(mockProbeExecution instanceof MockProbeExecution);
        assertTrue(iterator.hasNext());
        ProbeExecution probeExecution = iterator.next().getProbeExecution();
        assertTrue(probeExecution instanceof ServletProbeExecution);
        return (ServletProbeExecution) probeExecution;
    }
}
