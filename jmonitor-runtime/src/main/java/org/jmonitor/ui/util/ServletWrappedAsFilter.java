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
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang.StringUtils;

/**
 * Wraps a servlet as a filter.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class ServletWrappedAsFilter implements Filter {

    private final Servlet servlet;
    private final String servletName;

    public ServletWrappedAsFilter(Servlet servlet, String servletName) {
        this.servlet = servlet;
        this.servletName = servletName;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws IOException, ServletException {

        if (servletRequest instanceof HttpServletRequest) {

            HttpServletRequest request = (HttpServletRequest) servletRequest;

            // we assume that there are no servlet mappings that could match this request
            // (TODO what about *.png type servlet mappings?)
            // which means that servletPath represents the full path after contextPath
            final String[] components = StringUtils.split(request.getServletPath(), "/", 2);

            HttpServletRequestWrapper servletRequestWrapper =
                    new HttpServletRequestWrapper(request) {
                        public String getServletPath() {
                            return "/" + components[0];
                        }
                        public String getPathInfo() {
                            if (components.length == 1) {
                                return null;
                            } else {
                                return "/" + components[1];
                            }
                        }
                    };

            servlet.service(servletRequestWrapper, servletResponse);

        } else {
            servlet.service(servletRequest, servletResponse);
        }
    }

    public void init(final FilterConfig filterConfig) throws ServletException {
        servlet.init(new ServletConfig() {
            public String getInitParameter(String name) {
                return null;
            }
            public Enumeration<?> getInitParameterNames() {
                return Collections.enumeration(Collections.emptySet());
            }
            public ServletContext getServletContext() {
                return filterConfig.getServletContext();
            }
            public String getServletName() {
                return servletName;
            }
        });
    }

    public void destroy() {
        servlet.destroy();
    }
}
