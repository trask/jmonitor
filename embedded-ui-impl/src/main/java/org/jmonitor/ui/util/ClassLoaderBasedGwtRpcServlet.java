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

package org.jmonitor.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;
import com.google.gwt.user.server.rpc.SerializationPolicyProvider;

/**
 * A GWT RemoteServiceServlet that loads the SerializationPolicy from the classpath instead of from
 * the servlet context.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// NOPMD below is for cyclomatic complexity
public class ClassLoaderBasedGwtRpcServlet extends RemoteServiceServlet // NOPMD
        implements SerializationPolicyProvider {

    private static final long serialVersionUID = 1L;

    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request,
            String moduleBaseURL, String strongName) {
        return customLoadSerializationPolicy(this, request, moduleBaseURL, strongName);
    }

    // copied from com.google.gwt.user.server.rpc.RemoteServiceServlet (version 2.0.3) with some
    // modifications noted below
    // NOPMD below is for cyclomatic complexity
    private static SerializationPolicy customLoadSerializationPolicy(HttpServlet servlet, // NOPMD
            HttpServletRequest request, String moduleBaseURL, String strongName) {

        // The request can tell you the path of the web app relative to the
        // container root.
        String contextPath = request.getContextPath();

        String modulePath = null;
        if (moduleBaseURL != null) {
            try {
                modulePath = new URL(moduleBaseURL).getPath();
            } catch (MalformedURLException ex) {
                // log the information, we will default
                servlet.log("Malformed moduleBaseURL: " + moduleBaseURL, ex);
            }
        }

        SerializationPolicy serializationPolicy = null;

        /*
         * Check that the module path must be in the same web app as the servlet itself. If you need
         * to implement a scheme different than this, override this method.
         */
        if (modulePath == null || !modulePath.startsWith(contextPath)) {
            String message =
                    "ERROR: The module path requested, " + modulePath
                            + ", is not in the same web application as this servlet, "
                            + contextPath + ".  Your module may not be properly configured"
                            + " or your client and server code maybe out of date.";
            servlet.log(message);
        } else {

            String contextRelativePath = modulePath.substring(contextPath.length());

            // MODIFICATION TO RemoteServiceServlet.loadSerializationPolicy() STARTS HERE

            // strip off servlet path
            String servletRelativePath =
                    StringUtils.substringAfter(contextRelativePath, request.getServletPath() + "/");

            String serializationPolicyResourcePath =
                    SerializationPolicyLoader.getSerializationPolicyFileName(servletRelativePath
                            + strongName);

            // read serialization policy via ClassLoader instead of via ServletContext
            //
            // the serialization policy resources is provided by jmonitor so we use the same class
            // loader that loaded this class (which should have access to resources in the same jar
            // file)
            // NOPMD below is for not using Thread.currentThread().getContextClassLoader()
            ClassLoader classLoader = ClassLoaderBasedGwtRpcServlet.class.getClassLoader(); // NOPMD
            InputStream input = classLoader.getResourceAsStream(serializationPolicyResourcePath);

            // MODIFICATION TO RemoteServiceServlet.loadSerializationPolicy() ENDS HERE

            try {
                if (input != null) {
                    try {
                        serializationPolicy = SerializationPolicyLoader.loadFromStream(input, null);
                    } catch (ParseException e) {
                        servlet.log("ERROR: Failed to parse the policy file '"
                                + serializationPolicyResourcePath + "'", e);
                    } catch (IOException e) {
                        servlet.log("ERROR: Could not read the policy file '"
                                + serializationPolicyResourcePath + "'", e);
                    }
                } else {
                    String message =
                            "ERROR: The serialization policy file '"
                                    + serializationPolicyResourcePath
                                    + "' was not found; did you forget to include it in this deployment?";
                    servlet.log(message);
                }
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        // Ignore this error
                    }
                }
            }
        }

        return serializationPolicy;
    }
}
