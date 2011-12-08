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
package org.jmonitor.extension.probe;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Used by JdbcAspect to capture and mirror the state of prepared statements since we cannot inspect
 * the underlying {@link PreparedStatement} values after they have been set.
 * 
 * TODO does this need to be thread safe? Need to research JDBC spec, can one thread create a
 * PreparedStatement and set some parameters into it, and then have another thread execute it (with
 * those previously set parameters), if there is nothing that says no, then need to make this thread
 * safe I guess
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class PreparedStatementInfo extends StatementInfo {

    private String sql;
    private List<Object> parameters;
    private Collection<List<Object>> batchedParameters;

    public PreparedStatementInfo() {
        parameters = new ArrayList<Object>();
    }

    public void addBatch() {
        // synchronization isn't an issue here as this method is called only by the monitored thread
        if (batchedParameters == null) {
            batchedParameters = new ConcurrentLinkedQueue<List<Object>>();
        }
        batchedParameters.add(parameters);
        // the ArrayList is optimized an initial capacity since we know it should be the same size
        // as the previous one
        parameters = new ArrayList<Object>(parameters.size());
    }

    @Override
    public void clearBatch() {
        // create new arrays / queues so that the old ones can be stored into
        // the JdbcTraceElementSafeImpl without being subsequently overwritten
        parameters.clear();
        batchedParameters.clear();
    }

    public Collection<List<Object>> getBatchedParametersCopy() {
        // batched parameters cannot be changed after calling addBatch(), so it is safe to copy only
        // the outer list (we do not have to copy inner list of previously batched parameters)
        return new ArrayList<List<Object>>(batchedParameters);
    }

    public boolean isUsingBatchedParameters() {
        return batchedParameters != null;
    }

    public List<Object> getParametersCopy() {
        return new ArrayList<Object>(parameters);
    }

    public String getSql() {
        return sql;
    }

    // remember parameterIndex starts at 1 not 0
    public void setParameterValue(int parameterIndex, Object object) {
        if (parameterIndex == parameters.size() + 1) {
            // common path
            parameters.add(object);
        } else if (parameterIndex < parameters.size() + 1) {
            // overwrite existing value
            parameters.set(parameterIndex, object);
        } else {
            // expand list with nulls
            for (int i = parameters.size() + 1; i < parameterIndex; i++) {
                parameters.add(null);
            }
            parameters.add(object);
        }
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
