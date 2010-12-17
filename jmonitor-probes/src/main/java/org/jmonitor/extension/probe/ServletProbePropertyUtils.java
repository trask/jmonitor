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

package org.jmonitor.extension.probe;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jmonitor.api.probe.ProbeExecutionManagerFactory;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class ServletProbePropertyUtils {

    private static final String SERVLET_PROBE_NAME = "servlet";

    // TODO support nested paths
    private static final String USERNAME_SESSION_ATTRIBUTE_PATH_PROPERTY_NAME =
            "usernameSessionAttribute";

    // a special single value of "*" means capture all session attributes
    // this can be useful for finding the session attribute that represents the username
    // TODO support nested paths
    // TODO support "*.*", "*.*.*", etc
    // TODO support partial wildcards, e.g. "context*"
    private static final String SESSION_ATTRIBUTE_PATHS_PROPERTY_NAME = "sessionAttributes";

    private static final String CAPTURE_NESTED_EXECUTIONS_PROPERTY_NAME = "captureNestedExecutions";

    // optimization
    private static volatile Set<String> cachedSessionAttributes;
    private static volatile String cachedSessionAttributesText;

    // utility class
    private ServletProbePropertyUtils() {
    }

    public static boolean isCaptureNestedExecutions() {
        return Boolean.valueOf(getProperty(CAPTURE_NESTED_EXECUTIONS_PROPERTY_NAME));
    }

    public static String getUsernameSessionAttributePath() {
        return getProperty(USERNAME_SESSION_ATTRIBUTE_PATH_PROPERTY_NAME);
    }

    public static Set<String> getSessionAttributePaths() {

        String sessionAttributesText = getProperty(SESSION_ATTRIBUTE_PATHS_PROPERTY_NAME);

        if (!sessionAttributesText.equals(cachedSessionAttributesText)) {
            String[] sessionAttributesArray = StringUtils.split(sessionAttributesText, ',');

            Set<String> sessionAttributes = new HashSet<String>();
            for (String sessionAttribute : sessionAttributesArray) {
                sessionAttributes.add(sessionAttribute);
            }

            // update cachedSessionAttributes first so that another thread cannot come into this
            // method and get a positive match for text but then get the old cached attributes
            cachedSessionAttributes = sessionAttributes;
            cachedSessionAttributesText = sessionAttributesText;
        }

        return cachedSessionAttributes;
    }

    private static String getProperty(String propertyName) {
        return ProbeExecutionManagerFactory.getManager().getProperty(SERVLET_PROBE_NAME,
                propertyName);
    }
}
