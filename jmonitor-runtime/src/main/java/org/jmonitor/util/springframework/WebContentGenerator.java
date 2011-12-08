/**
 * Copyright 2002-2010 the original author or authors.
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
package org.jmonitor.util.springframework;

import javax.servlet.http.HttpServletResponse;

/**
 * Copied from org.springframework.web.servlet.support.WebContentGenerator
 * 
 * Removed dependencies on other spring classes and exposed preventCaching() and cacheForSeconds()
 * as public methods.
 * 
 * Original authors Rod Johnson and Juergen Hoeller.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO move to org.jmonitor.util package, convert to ExpirationUtils class
public class WebContentGenerator {

    /** HTTP method "GET" */
    public static final String METHOD_GET = "GET";

    /** HTTP method "HEAD" */
    public static final String METHOD_HEAD = "HEAD";

    /** HTTP method "POST" */
    public static final String METHOD_POST = "POST";

    private static final String HEADER_PRAGMA = "Pragma";

    private static final String HEADER_EXPIRES = "Expires";

    private static final String HEADER_CACHE_CONTROL = "Cache-Control";

    /** Use HTTP 1.0 expires header? */
    private boolean useExpiresHeader = true;

    /** Use HTTP 1.1 cache-control header? */
    private boolean useCacheControlHeader = true;

    /** Use HTTP 1.1 cache-control header value "no-store"? */
    private boolean useCacheControlNoStore = true;

    /**
     * Prevent the response from being cached. See <code>http://www.mnot.net/cache_docs</code>.
     */
    public final void preventCaching(HttpServletResponse response) {
        response.setHeader(HEADER_PRAGMA, "no-cache");
        if (this.useExpiresHeader) {
            // HTTP 1.0 header
            response.setDateHeader(HEADER_EXPIRES, 1L);
        }
        if (this.useCacheControlHeader) {
            // HTTP 1.1 header: "no-cache" is the standard value,
            // "no-store" is necessary to prevent caching on FireFox.
            response.setHeader(HEADER_CACHE_CONTROL, "no-cache");
            if (this.useCacheControlNoStore) {
                response.addHeader(HEADER_CACHE_CONTROL, "no-store");
            }
        }
    }

    /**
     * Set HTTP headers to allow caching for the given number of seconds. Does not tell the browser
     * to revalidate the resource.
     * 
     * @param response
     *            current HTTP response
     * @param seconds
     *            number of seconds into the future that the response should be cacheable for
     * @see #cacheForSeconds(javax.servlet.http.HttpServletResponse, int, boolean)
     */
    protected final void cacheForSeconds(HttpServletResponse response, int seconds) {
        cacheForSeconds(response, seconds, false);
    }

    /**
     * Set HTTP headers to allow caching for the given number of seconds. Tells the browser to
     * revalidate the resource if mustRevalidate is <code>true</code>.
     * 
     * @param response
     *            the current HTTP response
     * @param seconds
     *            number of seconds into the future that the response should be cacheable for
     * @param mustRevalidate
     *            whether the client should revalidate the resource (typically only necessary for
     *            controllers with last-modified support)
     */
    protected final void cacheForSeconds(HttpServletResponse response, int seconds,
            boolean mustRevalidate) {

        if (this.useExpiresHeader) {
            // HTTP 1.0 header
            response.setDateHeader(HEADER_EXPIRES, System.currentTimeMillis() + seconds * 1000L);
        }
        if (this.useCacheControlHeader) {
            // HTTP 1.1 header
            String headerValue = "max-age=" + seconds;
            if (mustRevalidate) {
                headerValue += ", must-revalidate";
            }
            response.setHeader(HEADER_CACHE_CONTROL, headerValue);
        }
    }

    /**
     * Apply the given cache seconds and generate corresponding HTTP headers, i.e. allow caching for
     * the given number of seconds in case of a positive value, prevent caching if given a 0 value,
     * do nothing else. Does not tell the browser to revalidate the resource.
     * 
     * @param response
     *            current HTTP response
     * @param seconds
     *            positive number of seconds into the future that the response should be cacheable
     *            for, 0 to prevent caching
     * @see #cacheForSeconds(javax.servlet.http.HttpServletResponse, int, boolean)
     */
    public final void applyCacheSeconds(HttpServletResponse response, int seconds) {
        applyCacheSeconds(response, seconds, false);
    }

    /**
     * Apply the given cache seconds and generate respective HTTP headers.
     * <p>
     * That is, allow caching for the given number of seconds in the case of a positive value,
     * prevent caching if given a 0 value, else do nothing (i.e. leave caching to the client).
     * 
     * @param response
     *            the current HTTP response
     * @param seconds
     *            the (positive) number of seconds into the future that the response should be
     *            cacheable for; 0 to prevent caching; and a negative value to leave caching to the
     *            client.
     * @param mustRevalidate
     *            whether the client should revalidate the resource (typically only necessary for
     *            controllers with last-modified support)
     */
    protected final void applyCacheSeconds(HttpServletResponse response, int seconds,
            boolean mustRevalidate) {

        if (seconds > 0) {
            cacheForSeconds(response, seconds, mustRevalidate);
        } else if (seconds == 0) {
            preventCaching(response);
        }
        // Leave caching to the client otherwise.
    }

}
