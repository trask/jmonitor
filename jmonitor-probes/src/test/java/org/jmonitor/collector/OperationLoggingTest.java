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

package org.jmonitor.collector;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.jmonitor.mock.MockServletWithJdbcCall;
import org.jmonitor.test.configuration.ConfigureAgentStackTraceInitialDelayMillis;
import org.jmonitor.test.configuration.ConfigureAgentStackTracePeriodMillis;
import org.jmonitor.test.configuration.ConfigureAgentStuckThresholdMillis;
import org.jmonitor.test.configuration.ConfigureAgentThresholdMillis;
import org.jmonitor.test.configuration.ConfigureCollectorLogActiveFilename;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

/**
 * Basic test that monitor captures and logs operation exceeding defined threshold.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
@ContextConfiguration(locations = "/applicationContext-test.xml")
public class OperationLoggingTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final String LOG_ACTIVE_FILENAME = "jmonitor.log";

    private static final int MOCK_SERVLET_PAUSE_TIME_IN_MILLIS = 10000;

    @Before
    public void setUp() {

        simpleJdbcTemplate.getJdbcOperations().execute("create table employee (name varchar(100))");
        simpleJdbcTemplate.getJdbcOperations().execute(
                "insert into employee (name) values ('trask stalnaker')");
    }

    @After
    public void tearDown() {

        // clean up database
        simpleJdbcTemplate.getJdbcOperations().execute("drop table employee");
    }

    // check that long response gets logged
    @Test
    @ConfigureAgentStackTraceInitialDelayMillis(2000)
    @ConfigureAgentStackTracePeriodMillis(1000)
    @ConfigureAgentThresholdMillis(4000)
    @ConfigureAgentStuckThresholdMillis(5000)
    @ConfigureCollectorLogActiveFilename(LOG_ACTIVE_FILENAME)
    public void testOperationLogging() throws ServletException, IOException, InterruptedException {

        File logFile = new File(LOG_ACTIVE_FILENAME);
        long initialLastModified = logFile.lastModified();
        long initialFileLength = logFile.length();

        if (initialLastModified == System.currentTimeMillis()) {
            // it was just modified by a previous test, pause two milliseconds
            // to make sure we get a new 'last modified' timestamp to check
            TimeUnit.MILLISECONDS.sleep(2);
        }

        Servlet servlet =
                new MockServletWithJdbcCall(MOCK_SERVLET_PAUSE_TIME_IN_MILLIS, simpleJdbcTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setParameter("testparam1", "value1");
        request.setParameter("testparam2", "value2");
        request.setParameter("testparam3", "value3");
        request.setParameter("testparam4", "value4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);

        // check that operation was logged
        // logging happens in a separate thread so we wait one second
        TimeUnit.SECONDS.sleep(2);

        assertTrue(logFile.exists());
        // TODO trying to debug occasional test failures
        System.out.println("old modified: " + new Date(initialLastModified));
        System.out.println("new modified: " + new Date(logFile.lastModified()));
        System.out.println("old size: " + initialFileLength);
        System.out.println("new size: " + logFile.length());
        assertTrue(logFile.lastModified() > initialLastModified);
    }
}
