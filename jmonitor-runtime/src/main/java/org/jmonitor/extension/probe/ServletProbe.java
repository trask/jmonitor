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
package org.jmonitor.extension.probe;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.jmonitor.api.probe.ProbeExecution;
import org.jmonitor.api.probe.ProbeExecutionCreator;
import org.jmonitor.api.probe.ProbeExecutionManagerFactory;
import org.jmonitor.configuration.service.model.AgentConfiguration;

/**
 * Defines pointcuts and captures data on
 * {@link HttpServlet#service(ServletRequest, ServletResponse)} and
 * {@link Filter#doFilter(ServletRequest, ServletResponse, FilterChain)} calls.
 * 
 * By default only calls to the top-most Filter and to the top-most Servlet are captured.
 * {@link AgentConfiguration#isWarnOnTraceEventOutsideOperation()} can be used to enable capturing
 * of nested Filters and nested Servlets as well.
 * 
 * This probe is careful not to rely on request or session objects being threadsafe.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
@Aspect
@SuppressAjWarnings("adviceDidNotMatch")
public class ServletProbe {

    private static final String ROOT_PROBE_EXECUTION_ATTRIBUTE_NAME =
            "jmonitor.probe.servlet.RootProbeExecution";

    @Pointcut("execution(void javax.servlet.Filter.doFilter(javax.servlet.ServletRequest,"
            + " javax.servlet.ServletResponse, javax.servlet.FilterChain))")
    void filterPointcut() {}

    @Pointcut("execution(void javax.servlet.Servlet.service(javax.servlet.ServletRequest,"
            + " javax.servlet.ServletResponse)) || execution(void javax.servlet.http.HttpServlet"
            + ".do*(javax.servlet.http.HttpServletRequest,"
            + " javax.servlet.http.HttpServletResponse))")
    void servletPointcut() {}

    @Pointcut("filterPointcut() && !cflowbelow(filterPointcut())")
    void topLevelFilterPointcut() {}

    @Pointcut("servletPointcut() && !cflowbelow(servletPointcut())")
    void topLevelServletPointcut() {}

    @Pointcut("filterPointcut() && cflowbelow(filterPointcut())")
    void nestedFilterPointcut() {}

    @Pointcut("servletPointcut() && cflowbelow(servletPointcut())")
    void nestedServletPointcut() {}

    // conveniently the same code can handle both Servlet.service() and Filter.doFilter()

    @Around("(topLevelServletPointcut() || topLevelFilterPointcut()) && target(target)"
            + " && args(request, ..)")
    public void aroundTopLevelServletPointcut(ProceedingJoinPoint joinPoint, Object target,
            HttpServletRequest request) throws Throwable {

        aroundServletPointcutInternal(joinPoint, target, request);
    }

    @Around("(nestedServletPointcut() || nestedFilterPointcut()) && target(target)"
            + " && args(request, ..)")
    public void aroundNestedServletPointcut(ProceedingJoinPoint joinPoint, Object target,
            HttpServletRequest request) throws Throwable {

        if (ServletProbeProperty.isCaptureNestedExecutions()) {
            aroundServletPointcutInternal(joinPoint, target, request);
        } else {
            joinPoint.proceed();
        }
    }

    private void aroundServletPointcutInternal(ProceedingJoinPoint joinPoint, final Object target,
            final HttpServletRequest request) throws Throwable {

        ProbeExecutionCreator probeExecution = new ProbeExecutionCreator() {
            public ProbeExecution createProbeExecution() {
                ServletProbeExecution probeExecution;
                if (ProbeExecutionManagerFactory.getManager().getRootProbeExecution() == null) {
                    // capture more expensive data (request parameter map and session info)
                    // only for root trace elements since that is where this detailed
                    // info is logged

                    // be careful not to pass "false" so it won't create session if request doesn't
                    // already have one
                    HttpSession session = request.getSession(false);
                    if (session == null) {
                        probeExecution = new ServletProbeExecution(target.getClass(),
                                request.getMethod(), request.getRequestURI());
                    } else {

                        String username = getSessionAttributeTextValue(session,
                                ServletProbeProperty.getUsernameSessionAttributePath());

                        probeExecution = new ServletProbeExecution(target.getClass(),
                                request.getMethod(), request.getRequestURI(), username,
                                session.getId(), getSessionAttributes(session));
                    }
                    // TODO doc this as potentially invasive
                    request.setAttribute(ROOT_PROBE_EXECUTION_ATTRIBUTE_NAME, probeExecution);
                } else {
                    probeExecution = new ServletProbeExecution(target.getClass(),
                            request.getMethod(), request.getRequestURI());
                }
                return probeExecution;
            }
        };

        if (ProbeExecutionManagerFactory.getManager().getRootProbeExecution() == null) {
            // only record aggregate timing data for the top most servlet or filter
            ProbeExecutionManagerFactory.getManager().execute(probeExecution, joinPoint,
                    "http request");
        } else {
            ProbeExecutionManagerFactory.getManager().execute(probeExecution, joinPoint);
        }
    }
    /*
     * ================== Http Servlet Request Parameters ==================
     */

    @Pointcut("call(* javax.servlet.ServletRequest.getParameter*(..))")
    void requestGetParameterPointcut() {}

    @AfterReturning("requestGetParameterPointcut() && !cflowbelow(requestGetParameterPointcut())"
            + " && target(request) && !within(org.jmonitor.extension.probe.ServletProbe)")
    public void afterReturningRequestGetParameterPointcut(HttpServletRequest request) {

        // only now is it safe to get parameters (if we get parameters before this we could prevent
        // a servlet from choosing to read the underlying stream instead of using the getParameter*
        // methods) see SRV.3.1.1 "When Parameters Are Available"

        ServletProbeExecution probeExecution = getRootServletProbeExecution(request);

        if (probeExecution != null && !probeExecution.isRequestParameterMapCaptured()) {
            // we are monitoring this request and the request parameter map hasn't been captured yet
            probeExecution.captureRequestParameterMap(request.getParameterMap());
        }
    }

    /*
     * ================== Http Session Attributes ==================
     */

    @Pointcut("call(javax.servlet.http.HttpSession"
            + " javax.servlet.http.HttpServletRequest.getSession(..))")
    void requestGetSessionPointcut() {}

    @AfterReturning(pointcut = "requestGetSessionPointcut()"
            + " && !cflowbelow(requestGetSessionPointcut()) && target(request)",
            returning = "session")
    public void afterReturningRequestGetSession(HttpServletRequest request, HttpSession session) {

        ServletProbeExecution probeExecution = getRootServletProbeExecution(request);

        if (probeExecution != null && session != null && session.isNew()) {
            probeExecution.setSessionIdUpdatedValue(session.getId());
        }
    }

    @Pointcut("call(void javax.servlet.http.HttpSession.invalidate())")
    void sessionInvalidatePointcut() {}

    @Before("sessionInvalidatePointcut() && !cflowbelow(sessionInvalidatePointcut())"
            + " && target(session)")
    public void beforeSessionInvalidatePointcut(HttpSession session) {

        ServletProbeExecution probeExecution = getRootServletProbeExecution(session);

        if (probeExecution != null) {
            probeExecution.setSessionIdUpdatedValue("");
        }
    }

    // TODO support deprecated HttpSession.putValue()?

    @Pointcut("call(void javax.servlet.http.HttpSession.setAttribute(String, Object))")
    void sessionSetAttributePointcut() {}

    @AfterReturning("sessionSetAttributePointcut() && !cflowbelow(sessionSetAttributePointcut())"
            + " && target(session) && args(name, value)")
    public void afterReturningSessionSetAttributePointcut(HttpSession session, String name,
            Object value) {

        // both name and value are non-null per HttpSession.setAttribute() specification

        ServletProbeExecution probeExecution = getRootServletProbeExecution(session);

        if (probeExecution != null) {
            // check for username attribute
            // TODO handle possible nested path here
            if (name.equals(ServletProbeProperty.getUsernameSessionAttributePath())) {
                // value should be a String, but we call toString() just to be safe
                probeExecution.setUsername(value.toString());
            }

            // update session attribute in ServletProbeExecution if necessary
            Set<String> sessionAttributePaths = ServletProbeProperty.getSessionAttributePaths();
            if (isSingleWildcard(sessionAttributePaths) || sessionAttributePaths.contains(name)) {

                probeExecution.putSessionAttributeChangedValue(name, value.toString());
            }
        }
    }

    @Pointcut("call(void javax.servlet.http.HttpSession.removeAttribute(String))")
    void sessionRemoveAttributePointcut() {}

    @AfterReturning("sessionRemoveAttributePointcut()"
            + " && !cflowbelow(sessionRemoveAttributePointcut()) && target(session) && args(name)")
    public void afterReturningSessionRemoveAttributePointcut(HttpSession session, String name) {

        // update session attribute in ServletProbeExecution if necessary
        Set<String> sessionAttributePaths = ServletProbeProperty.getSessionAttributePaths();
        if (isSingleWildcard(sessionAttributePaths) || sessionAttributePaths.contains(name)) {

            ServletProbeExecution probeExecution = getRootServletProbeExecution(session);
            if (probeExecution != null) {
                probeExecution.putSessionAttributeChangedValue(name, "");
            }
        }
    }

    private ServletProbeExecution getRootServletProbeExecution(HttpServletRequest request) {
        return (ServletProbeExecution) request.getAttribute(ROOT_PROBE_EXECUTION_ATTRIBUTE_NAME);
    }

    private ServletProbeExecution getRootServletProbeExecution(HttpSession session) {

        ProbeExecution rootProbeExecution = ProbeExecutionManagerFactory.getManager()
                .getRootProbeExecution();

        if (!(rootProbeExecution instanceof ServletProbeExecution)) {
            return null;
        }

        ServletProbeExecution rootServletProbeExecution =
                (ServletProbeExecution) rootProbeExecution;

        String sessionId;
        if (rootServletProbeExecution.getSessionIdUpdatedValue() != null) {
            sessionId = rootServletProbeExecution.getSessionIdUpdatedValue();
        } else {
            sessionId = rootServletProbeExecution.getSessionIdInitialValue();
        }

        if (!session.getId().equals(sessionId)) {
            // the target session for this pointcut is not the same as the ProbeExecution
            return null;
        }

        return rootServletProbeExecution;
    }

    private Map<String, String> getSessionAttributes(HttpSession session) {

        Set<String> sessionAttributePaths = ServletProbeProperty.getSessionAttributePaths();

        if (sessionAttributePaths == null || sessionAttributePaths.isEmpty()) {
            return null;
        }

        if (isSingleWildcard(sessionAttributePaths)) {

            // special single value of "*" means dump all http session attributes

            Map<String, String> sessionAttributeMap = new HashMap<String, String>();

            for (Enumeration<?> e = session.getAttributeNames(); e.hasMoreElements();) {
                String attributeName = (String) e.nextElement();
                Object attributeValue = session.getAttribute(attributeName);
                sessionAttributeMap.put(attributeName, attributeValue.toString());
            }
            return sessionAttributeMap;

        } else {

            // optimize hashmap sizing since we know how many session attributes are going to be
            // stored and because this is called for every request so it is worth optimizing
            Map<String, String> sessionAttributeMap = new HashMap<String, String>(
                    sessionAttributePaths.size());

            // dump only http session attributes in list
            for (String attributePath : sessionAttributePaths) {
                String attributeValue = getSessionAttributeTextValue(session, attributePath);
                sessionAttributeMap.put(attributePath, attributeValue);
            }
            return sessionAttributeMap;
        }
    }

    private String getSessionAttributeTextValue(HttpSession session, String attributePath) {
        Object value = session.getAttribute(attributePath);
        if (value == null) {
            return null;
        } else {
            return value.toString();
        }
    }

    private boolean isSingleWildcard(Set<String> sessionAttributePaths) {
        return sessionAttributePaths.size() == 1
                && sessionAttributePaths.iterator().next().equals("*");
    }
}
