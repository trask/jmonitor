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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public class PauseServlet extends HttpServlet {

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

        if (pauseSecondsText != null) {
            buildResponse(response, "Paused for " + pauseSeconds + " seconds.");
        } else {
            buildResponse(response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        doGet(request, response);
    }

    private void buildResponse(HttpServletResponse response) throws IOException {
        buildResponse(response, null);
    }

    private void buildResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        if (message != null) {
            out.print(message);
            out.println("<br />");
            out.println("<br />");
        }
        out.println("<form method=POST>");
        out.println("Pause: <input type=\"text\" name=\"pauseTime\" /><br />");
        out.println("<input type=\"submit\" name=\"Submit Form\" />");
        out.println("</form>");
        out.println("</body></html>");
        return;
    }
}
