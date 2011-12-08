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
package org.jmonitor.test.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public class SessionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String pauseSecondsText = request.getParameter("pauseTime");
        int pauseSeconds = 0;
        if (pauseSecondsText != null) {
            try {
                pauseSeconds = Integer.parseInt(pauseSecondsText);
            } catch (NumberFormatException e) {
                buildResponse(response, "Invalid pauseTime: '" + pauseSecondsText + "'");
                return;
            }
        }

        try {
            Thread.sleep(1000 * pauseSeconds);
        } catch (InterruptedException e) {
            // restore the interrupted status
            Thread.currentThread().interrupt();
        }

        for (Enumeration<?> e = request.getParameterNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            if (name.equals("pauseTime")) {
                continue;
            }
            String value = request.getParameter(name);
            if (StringUtils.isEmpty(value)) {
                request.getSession().removeAttribute(name);
            } else {
                request.getSession().setAttribute(name, value);
            }
        }

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("Paused for " + pauseSeconds + " seconds.<br/>");
        out.println("<br/>");
        out.println("Session attributes:<br/>");
        for (Enumeration<?> e = request.getSession().getAttributeNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            String value = request.getSession().getAttribute(name).toString();
            out.print(name);
            out.print(": ");
            out.print(value);
            out.println("<br/>");
        }
        out.println("</body></html>");
    }

    private void buildResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println(message);
        out.println("</body></html>");
        return;
    }
}
