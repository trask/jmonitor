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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.commons.lang.StringUtils;
import org.jmonitor.api.probe.ProbeExecutionContext;
import org.jmonitor.api.probe.RootProbeExecution;

/**
 * Servlet trace element captured by AspectJ pointcut.
 * 
 * Similar thread safety issues as {@link JdbcProbeExecution}, see documentation in that class for
 * more details.
 * 
 * This trace element gets to piggyback on the happens-before relationships created by putting other
 * trace elements into the concurrent queue which ensures that session state is visible at least up
 * to the start of the most recent trace element.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class ServletProbeExecution implements RootProbeExecution {

    // this must extend either Servlet or Filter
    private final Class<?> clazz;

    // TODO allow additional notation for session attributes to capture, e.g.
    // +currentControllerContext.key which would denote to capture the value of that attribute at
    // the beginning of the request

    // it would be convenient to just store the request object here
    // but it appears that tomcat (at least, maybe others) clears out those
    // objects after the response is complete
    // so it can reuse the request object for future requests
    // since we log the data in a separate thread to avoid slowing up the user's
    // request, the request object
    // could have been cleared before we are able to log the request data
    // so instead we must cache the request parts we are interested in tracking
    //
    // another problem with storing the request object here is that it may not be thread safe
    //
    // we also cannot store the http session object here because it may be marked
    // expired when we try to log the session attributes, so instead we must store
    // (references to) the session attributes
    private final String requestMethod;
    private final String requestURI;
    private volatile Map<String, String[]> requestParameterMap;

    private volatile String username;

    // the initial value is the sessionId as it was present at the beginning of the request
    private final String sessionIdInitialValue;

    private volatile String sessionIdUpdatedValue;

    // session attributes may not be thread safe, so we need to convert them to thread-safe Strings
    // within the request processing thread, which can then be used by the flushing / real-time
    // monitoring threads
    // the initial value map contains the session attributes as they were present at the beginning
    // of the request
    private final Map<String, String> sessionAttributeInitialValueMap;

    private volatile Map<String, String> sessionAttributeUpdatedValueMap;

    public ServletProbeExecution(Class<?> clazz, String requestMethod, String requestURI) {

        this(clazz, requestMethod, requestURI, null, null, null);
    }

    public ServletProbeExecution(Class<?> clazz, String requestMethod, String requestURI,
            String username, String sessionId, Map<String, String> sessionAttributeMap) {

        if (!Servlet.class.isAssignableFrom(clazz) && !Filter.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("clazz must be a Servlet or a Filter");
        }

        this.clazz = clazz;
        this.requestMethod = requestMethod;
        this.requestURI = requestURI;
        this.username = username;
        this.sessionIdInitialValue = sessionId;
        if (sessionAttributeMap == null || sessionAttributeMap.isEmpty()) {
            this.sessionAttributeInitialValueMap = null;
        } else {
            this.sessionAttributeInitialValueMap = sessionAttributeMap;
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isRequestParameterMapCaptured() {
        return requestParameterMap != null;
    }

    public void captureRequestParameterMap(Map<?, ?> requestParameterMap) {

        // shallow copy is necessary because request may not be thread safe
        // shallow copy is also necessary because of the note about tomcat above
        Map<String, String[]> map = new HashMap<String, String[]>(requestParameterMap.size());
        for (Entry<?, ?> entry : requestParameterMap.entrySet()) {
            map.put((String) entry.getKey(), (String[]) entry.getValue());
        }
        this.requestParameterMap = map;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setSessionIdUpdatedValue(String sessionId) {
        this.sessionIdUpdatedValue = sessionId;
    }

    public void putSessionAttributeChangedValue(String name, String value) {
        if (sessionAttributeUpdatedValueMap == null) {
            sessionAttributeUpdatedValueMap = new ConcurrentHashMap<String, String>();
        }
        sessionAttributeUpdatedValueMap.put(name, value);
    }

    public Class<?> getClazz() {
        return clazz;
    }

    // only called by tests
    public String getRequestMethod() {
        return requestMethod;
    }

    // only called by tests
    public String getRequestURI() {
        return requestURI;
    }

    // only called by tests
    public Map<String, String[]> getRequestParameterMap() {
        return requestParameterMap;
    }

    public String getSessionIdInitialValue() {
        return sessionIdInitialValue;
    }

    public String getSessionIdUpdatedValue() {
        return sessionIdUpdatedValue;
    }

    public void printDescription(PrintWriter out) {
        if (Servlet.class.isAssignableFrom(clazz)) {
            out.print("servlet: ");
            out.print(clazz.getName());
            out.print(".service()");
        } else if (Filter.class.isAssignableFrom(clazz)) {
            out.print("filter: ");
            out.print(clazz.getName());
            out.print(".doFilter()");
        } else {
            throw new IllegalStateException("clazz must be a Servlet or a Filter");
        }
    }

    public ProbeExecutionContext createContext() {

        ProbeExecutionContext context = new ProbeExecutionContext();

        addRequestContext(context);
        addHttpSessionContext(context);

        return context;
    }

    private void addRequestContext(ProbeExecutionContext context) {

        context.put("request method", requestMethod);
        context.put("request uri", requestURI);

        if (requestParameterMap != null && !requestParameterMap.isEmpty()) {
            ProbeExecutionContext nestedContext = new ProbeExecutionContext();
            for (String parameterName : requestParameterMap.keySet()) {
                String[] values = requestParameterMap.get(parameterName);
                for (String value : values) {
                    nestedContext.put(parameterName, value);
                }
            }
            context.putNested("request parameters", nestedContext);
        }
    }

    private void addHttpSessionContext(ProbeExecutionContext context) {

        if (sessionIdUpdatedValue != null) {
            context.put("session id (at beginning of this request)",
                    StringUtils.defaultString(sessionIdInitialValue));
            context.put("session id (updated during this request)", sessionIdUpdatedValue);
        } else if (sessionIdInitialValue != null) {
            context.put("session id", sessionIdInitialValue);
        }

        if (sessionAttributeUpdatedValueMap != null) {

            // create nested context with session attribute initial values
            ProbeExecutionContext initialValuesNestedContext = new ProbeExecutionContext();
            for (Entry<String, String> entry : sessionAttributeUpdatedValueMap.entrySet()) {
                if (entry.getValue() != null) {
                    // put empty values for all updated attributes then we overwrite those that had
                    // initial values below
                    initialValuesNestedContext.put(entry.getKey(), "");
                }
            }
            if (sessionAttributeInitialValueMap != null) {
                for (Entry<String, String> entry : sessionAttributeInitialValueMap.entrySet()) {
                    if (entry.getValue() != null) {
                        initialValuesNestedContext.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            context.putNested("session attributes (at beginning of this request)",
                    initialValuesNestedContext);

            // create nested context with session attribute updated values
            ProbeExecutionContext updatedValuesNestedContext = new ProbeExecutionContext();
            for (Entry<String, String> entry : sessionAttributeUpdatedValueMap.entrySet()) {
                if (entry.getValue() != null) {
                    updatedValuesNestedContext.put(entry.getKey(), entry.getValue());
                }
            }
            context.putNested("session attributes (updated during this request)",
                    updatedValuesNestedContext);

        } else if (sessionAttributeInitialValueMap != null) {

            ProbeExecutionContext nestedContext = new ProbeExecutionContext();
            for (Entry<String, String> entry : sessionAttributeInitialValueMap.entrySet()) {
                if (entry.getValue() != null) {
                    nestedContext.put(entry.getKey(), entry.getValue());
                }
            }
            context.putNested("session attributes", nestedContext);
        }
    }
}
