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
package org.jmonitor.mock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Mock servlet that makes jdbc calls.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class MockServletWithJdbcCall extends MockServlet {

    private static final int NUMBER_OF_PAUSE_SLICES = 7;

    private static final long serialVersionUID = 1L;

    private final int pauseTimeMillis;
    private final SimpleJdbcTemplate simpleJdbcTemplate;

    public MockServletWithJdbcCall(int pauseTimeMillis, SimpleJdbcTemplate simpleJdbcTemplate) {
        this.pauseTimeMillis = pauseTimeMillis;
        this.simpleJdbcTemplate = simpleJdbcTemplate;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {

        // execute before pause (shouldn't get stack traces for these)
        executeStatement();
        executePreparedStatement();

        // test flushing
        for (int i = 0; i < 100; i++) {
            executeStatement();
            executePreparedStatement();
        }

        if (pauseTimeMillis > 0) {
            try {
                // break the pause into separate lines of code in order to generate a couple of
                // different stack trace elements
                Thread.sleep(pauseTimeMillis / NUMBER_OF_PAUSE_SLICES);
                Thread.sleep(pauseTimeMillis / NUMBER_OF_PAUSE_SLICES);
                Thread.sleep(pauseTimeMillis / NUMBER_OF_PAUSE_SLICES);
                Thread.sleep(pauseTimeMillis / NUMBER_OF_PAUSE_SLICES);
                Thread.sleep(pauseTimeMillis / NUMBER_OF_PAUSE_SLICES);
                Thread.sleep(pauseTimeMillis / NUMBER_OF_PAUSE_SLICES);
                Thread.sleep(pauseTimeMillis / NUMBER_OF_PAUSE_SLICES);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        // execute before pause (should get stack traces for these)
        executeStatement();
        executePreparedStatement();
    }

    private void executeStatement() {
        simpleJdbcTemplate.queryForList("select * from employee");
    }

    private void executePreparedStatement() {
        simpleJdbcTemplate.queryForList("select * from employee where name like ?", "trask%");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }
}
