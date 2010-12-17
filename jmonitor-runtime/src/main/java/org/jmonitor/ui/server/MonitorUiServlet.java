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

package org.jmonitor.ui.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jmonitor.agent.client.AgentServiceFactory;
import org.jmonitor.collector.client.CollectorServiceFactory;
import org.jmonitor.collector.impl.EmailAlertDestination;
import org.jmonitor.collector.impl.file.OperationPrinter;
import org.jmonitor.collector.service.model.Operation;
import org.jmonitor.collector.shared.logging.CollectorServiceLoggerFactory;
import org.jmonitor.configuration.client.ConfigurationServiceFactory;
import org.jmonitor.configuration.service.model.CollectorConfiguration;
import org.jmonitor.configuration.service.model.FullConfiguration;
import org.jmonitor.configuration.service.model.UiConfiguration;
import org.jmonitor.configuration.shared.ConfigurationImplHelper;
import org.jmonitor.configuration.shared.model.FullConfigurationImpl;
import org.jmonitor.configuration.shared.model.UiConfigurationImpl;
import org.jmonitor.ui.client.UiService;
import org.jmonitor.ui.shared.BadCredentialsException;
import org.jmonitor.ui.shared.NotAuthenticated;
import org.jmonitor.ui.util.ClassLoaderBasedGwtRpcServlet;
import org.jmonitor.ui.util.ExpirationUtils;
import org.jmonitor.util.EncryptionUtils;
import org.slf4j.Logger;

/**
 * Very basic administrative servlet to view and set monitor properties and to view current
 * in-flight operations that are being tracked (including those that have not yet met the configured
 * threshold).
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class MonitorUiServlet extends ClassLoaderBasedGwtRpcServlet implements UiService {

    // need to convert to long data type otherwise calculation will overflow
    private static final int ONE_YEAR_IN_SECONDS = 365 * 24 * 60 * 60;

    private static final Logger LOGGER =
            CollectorServiceLoggerFactory.getLogger(MonitorUiServlet.class);

    private static final long serialVersionUID = 1L;

    private static final String AUTHENTICATED_SESSION_ATTRIBUTE_NAME =
            "jmonitorSessionAuthenticated";

    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<String, String>();

    static {
        CONTENT_TYPE_MAP.put("html", "text/html; charset=UTF-8");
        CONTENT_TYPE_MAP.put("js", "text/javascript; charset=UTF-8");
        CONTENT_TYPE_MAP.put("css", "text/css; charset=UTF-8");
        CONTENT_TYPE_MAP.put("rpc", "text/plain; charset=UTF-8");
        CONTENT_TYPE_MAP.put("png", "image/png");
        CONTENT_TYPE_MAP.put("gif", "image/gif");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        try {
            doGetInternal(request, response);
        } catch (Exception e) {
            // log and return basic error message to user
            LOGGER.error(e.getMessage(), e);
            response.reset();
            response.getWriter().println(e.getMessage());
        } catch (Error e) {
            // log and re-throw serious error
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }

    private void doGetInternal(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // TODO use ehcache SimplePageCachingFilter for caching / gzip compression

        if (request.getPathInfo() == null) {

            writeMainPage(request, response);

        } else if (request.getPathInfo().startsWith("/org.jmonitor.ui.Monitor/")) {

            String resourcePath =
                    "org.jmonitor.ui.Monitor/"
                            + StringUtils.substringAfter(request.getPathInfo(),
                                    "/org.jmonitor.ui.Monitor/");

            writeResource(response, resourcePath);

        } else {

            // return page not found
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void writeResource(HttpServletResponse response, String resourcePath)
            throws IOException {

        // these resources are provided by jmonitor so we use the same class loader
        // that loaded this class (which should have access to resources in the same jar file)
        // NOPMD below is for not using Thread.currentThread().getContextClassLoader()
        ClassLoader classLoader = MonitorUiServlet.class.getClassLoader(); // NOPMD
        InputStream input = classLoader.getResourceAsStream(resourcePath);

        if (input == null) {
            throw new IllegalStateException("Could not find '" + resourcePath + "'");
        }

        // set the content-type header correctly
        String resourceExtension = StringUtils.substringAfterLast(resourcePath, ".");
        String contentType = CONTENT_TYPE_MAP.get(resourceExtension);
        if (contentType == null) {
            // TODO log and throw error for unknown resource extension
        }
        response.setContentType(contentType);

        // use gwt naming convention to apply correct caching behavior
        if (resourcePath.contains(".cache.")) {
            applyCacheSeconds(response, ONE_YEAR_IN_SECONDS);
        } else if (resourcePath.contains(".nocache.")) {
            preventCaching(response);
        } else {
            // TODO what to do with gwt static resources that don't declare themselves as cache
            // or nocache
        }

        IOUtils.copy(input, response.getOutputStream());
    }

    private void writeMainPage(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // set the content-type header correctly
        response.setContentType("text/html; charset=UTF-8");

        // prevent caching of this page, it is small and not hit that often so should be
        // acceptable from performance perspective
        preventCaching(response);

        PrintWriter out = response.getWriter();
        out.println("<!doctype html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>JMonitor</title>");
        out.print("<script type=\"text/javascript\" language=\"javascript\"");
        out.print(" src=\"");
        out.println(request.getContextPath() + request.getServletPath());
        out.println("/org.jmonitor.ui.Monitor/org.jmonitor.ui.Monitor.nocache.js\"></script>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div id=\"ui\" />");
        out.println("</body>");
        out.println("</html>");
    }

    public String authenticate(String username, String password) throws BadCredentialsException {

        if (username != null && password != null) {

            UiConfiguration configuration =
                    ConfigurationServiceFactory.getService().getUiConfiguration();

            if (EncryptionUtils.authenticate(username, password, configuration.getAdminUsername(),
                    configuration.getAdminPassword())) {

                HttpSession session = getThreadLocalRequest().getSession();
                session.setAttribute(AUTHENTICATED_SESSION_ATTRIBUTE_NAME, true);
                return session.getId();
            }
        }
        throw new BadCredentialsException();
    }

    public String getActivityText(String sessionId) throws NotAuthenticated {

        checkSessionIdFromGwtRpc(sessionId);

        CollectorConfiguration configuration =
                ConfigurationServiceFactory.getService().getCollectorConfiguration();
        int maxTraceEventsPerOperation = configuration.getMaxTraceEventsPerEmail();

        Collection<? extends Operation> operations =
                AgentServiceFactory.getService().getOperationsExceptCurrent();
        StringWriter stringWriter = new StringWriter();
        OperationPrinter operationHelper =
                new OperationPrinter(new PrintWriter(stringWriter), maxTraceEventsPerOperation);
        for (Operation operation : operations) {
            operationHelper.collect(operation);
        }
        return stringWriter.toString();
    }

    public FullConfigurationImpl getFullConfiguration(String sessionId) throws NotAuthenticated {

        checkSessionIdFromGwtRpc(sessionId);

        FullConfiguration configuration =
                ConfigurationServiceFactory.getService().getFullConfiguration();

        FullConfigurationImpl configurationCopy = ConfigurationImplHelper.copyOf(configuration);

        // email password is sent back and forth as plain text
        String emailPasswordPlainText = getEmailPasswordPlainText(configurationCopy);
        configurationCopy.getCollectorConfiguration().setEmailPassword(emailPasswordPlainText);

        // admin password must be changed via changeAdminPassword() method
        // plus we don't want to send admin password (even hashed) to client
        configurationCopy.getUiConfiguration().setAdminPassword(null);

        return configurationCopy;
    }

    public void updateFullConfiguration(String sessionId, FullConfigurationImpl configuration)
            throws NotAuthenticated {

        checkSessionIdFromGwtRpc(sessionId);

        // TODO move into configuration service impl?

        // handle email password
        // re-encrypting the same plain text value will typically result the different encrypted
        // values due to random salt generator usage, see StandardPBEStringEncryptor.encrypt()
        // we don't want to change the stored value of the password unless the plain text value
        // has changed since this could seem confusing
        // TODO validate that we are using random salt generator and that the above statement is
        // correct
        FullConfiguration currentFullConfiguration =
                ConfigurationServiceFactory.getService().getFullConfiguration();
        String currentEmailPasswordPlainText = getEmailPasswordPlainText(currentFullConfiguration);
        String newEmailPasswordPlainText =
                configuration.getCollectorConfiguration().getEmailPassword();
        if (!newEmailPasswordPlainText.equals(currentEmailPasswordPlainText)) {
            configuration.getCollectorConfiguration().setEmailPassword(
                    EncryptionUtils.encrypt(newEmailPasswordPlainText));
        } else {
            // re-use previously encrypted value
            String currentEmailPasswordEncrypted =
                    currentFullConfiguration.getCollectorConfiguration().getEmailPassword();
            configuration.getCollectorConfiguration().setEmailPassword(
                    currentEmailPasswordEncrypted);
        }

        // admin password must be changed via changeAdminPassword() method
        String adminPassword = currentFullConfiguration.getUiConfiguration().getAdminPassword();
        configuration.getUiConfiguration().setAdminPassword(adminPassword);

        ConfigurationServiceFactory.getService().updateFullConfiguration(configuration);
    }

    public void changeAdminPassword(String sessionId, String currentAdminPassword,
            String newAdminPassword) throws NotAuthenticated, BadCredentialsException {

        checkSessionIdFromGwtRpc(sessionId);

        UiConfiguration configuration =
                ConfigurationServiceFactory.getService().getUiConfiguration();
        String encryptedAdminPassword = configuration.getAdminPassword();
        if (EncryptionUtils.checkPassword(encryptedAdminPassword, currentAdminPassword)) {
            // current password is correct, proceed and change password
            UiConfigurationImpl mutableConfiguration =
                    ConfigurationImplHelper.copyOf(configuration);
            mutableConfiguration.setAdminPassword(EncryptionUtils.encryptPassword(newAdminPassword));
            ConfigurationServiceFactory.getService().updateUiConfiguration(mutableConfiguration);
        } else {
            throw new BadCredentialsException();
        }
    }

    public void generateTestEmail(String sessionId) throws NotAuthenticated {

        checkSessionIdFromGwtRpc(sessionId);

        // TODO technically should go through CollectorService to respect separation of tiers
        EmailAlertDestination.sendMessage("Test Alert",
                "This is a test generated by the jmonitor administrator.");
    }

    public void generateTestLogStatement(String sessionId) throws NotAuthenticated {

        checkSessionIdFromGwtRpc(sessionId);

        CollectorServiceFactory.getService().collectError(
                "This is a test generated by the jmonitor administrator.");
    }

    private void checkSessionIdFromGwtRpc(String sessionId) throws NotAuthenticated {

        HttpSession session = getThreadLocalRequest().getSession(false);

        if (session == null || session.getAttribute(AUTHENTICATED_SESSION_ATTRIBUTE_NAME) == null) {
            throw new NotAuthenticated();
        }

        if (!session.getId().equals(sessionId)) {
            // XSRF, see
            // http://groups.google.com/group/Google-Web-Toolkit/web/security-for-gwt-applications
            // TODO review http://gwt-code-reviews.appspot.com/179801/show
            throw new NotAuthenticated();
        }
    }

    private String getEmailPasswordPlainText(FullConfiguration configurationCopy) {
        return EncryptionUtils.decrypt(configurationCopy.getCollectorConfiguration().getEmailPassword());
    }

    private static void preventCaching(HttpServletResponse response) {
        ExpirationUtils.preventCaching(response);
    }

    private static void applyCacheSeconds(HttpServletResponse response, int seconds) {
        ExpirationUtils.applyCacheSeconds(response, seconds);
    }
}
