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
package org.jmonitor.collector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.codec.digest.DigestUtils;
import org.jmonitor.configuration.client.ConfigurationServiceFactory;
import org.jmonitor.configuration.service.model.AgentConfiguration;
import org.jmonitor.configuration.service.model.CollectorConfiguration;
import org.jmonitor.configuration.shared.ConfigurationImplHelper;
import org.jmonitor.configuration.shared.model.AgentConfigurationImpl;
import org.jmonitor.configuration.shared.model.CollectorConfigurationImpl;
import org.jmonitor.mock.MockServletWithJdbcCall;
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

    private static final int STACK_TRACE_INITIAL_DELAY_MILLIS = 2000;
    private static final int STACK_TRACE_PERIOD_MILLIS = 1000;
    private static final int THRESHOLD_MILLIS = 4000;
    private static final int STUCK_THRESHOLD_MILLIS = 5000;

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
    public void testOperationLogging() throws ServletException, IOException, InterruptedException {

        String initialLogContentHash = "";
        if (new File(LOG_ACTIVE_FILENAME).exists()) {
            initialLogContentHash = DigestUtils.md5Hex(new FileInputStream(LOG_ACTIVE_FILENAME));
        }

        AgentConfiguration agentConfiguration = ConfigurationServiceFactory.getService()
                .getAgentConfiguration();

        AgentConfigurationImpl mutableAgentConfiguration = ConfigurationImplHelper
                .copyOf(agentConfiguration);
        mutableAgentConfiguration.setStackTraceInitialDelayMillis(STACK_TRACE_INITIAL_DELAY_MILLIS);
        mutableAgentConfiguration.setStackTracePeriodMillis(STACK_TRACE_PERIOD_MILLIS);
        mutableAgentConfiguration.setThresholdMillis(THRESHOLD_MILLIS);
        mutableAgentConfiguration.setStuckThresholdMillis(STUCK_THRESHOLD_MILLIS);
        ConfigurationServiceFactory.getService()
                .updateAgentConfiguration(mutableAgentConfiguration);

        CollectorConfiguration collectorConfiguration = ConfigurationServiceFactory.getService()
                .getCollectorConfiguration();
        CollectorConfigurationImpl mutableCollectorConfiguration = ConfigurationImplHelper
                .copyOf(collectorConfiguration);
        mutableCollectorConfiguration.setLogActiveFilename(LOG_ACTIVE_FILENAME);
        ConfigurationServiceFactory.getService().updateCollectorConfiguration(
                mutableCollectorConfiguration);

        Servlet servlet = new MockServletWithJdbcCall(MOCK_SERVLET_PAUSE_TIME_IN_MILLIS,
                simpleJdbcTemplate);
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

        assertTrue(new File(LOG_ACTIVE_FILENAME).exists());
        String logContentHash = DigestUtils.md5Hex(new FileInputStream(LOG_ACTIVE_FILENAME));
        assertFalse(logContentHash.equals(initialLogContentHash));
    }
}
