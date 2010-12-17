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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.jmonitor.ui.util.ServletWrappedAsFilter;

/**
 * This filter is used to serve up the monitor UI so that we don't have to worry about other filters
 * intercepting and modifying our responses, e.g. decorating the responses, setting expiration
 * headers (the monitor UI handles this itself) or gzipping (also already handled by the monitor
 * UI).
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class MonitorUiFilter implements Filter {

    private final ServletWrappedAsFilter servletWrapper =
            new ServletWrappedAsFilter(new MonitorUiServlet(), "jmonitor-internal-servlet");

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws ServletException, IOException {

        servletWrapper.doFilter(servletRequest, servletResponse, chain);
    }

    public void init(final FilterConfig filterConfig) throws ServletException {
        servletWrapper.init(filterConfig);
    }

    public void destroy() {
        servletWrapper.destroy();
    }
}
