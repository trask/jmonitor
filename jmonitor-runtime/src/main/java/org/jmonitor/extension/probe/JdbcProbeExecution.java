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

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jmonitor.api.probe.ProbeExecutionContext;
import org.jmonitor.api.probe.ProbeExecutionWithUpdate;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

/**
 * Jdbc trace element captured by AspectJ pointcut.
 * 
 * Objects in the parameter array, batchedParameters collections and batchedSqls collection aren't
 * necessarily thread safe so users of this class must adhere to the following contract:
 * 
 * 1. The arrays / collections should not be modified after constructing the
 * JdbcTraceElementSafeImpl.
 * 
 * 2. None of the elements in these arrays / collections should be modified after constructing the
 * JdbcTraceElementSafeImpl.
 * 
 * 3. There should be some kind of coordination between the threads to ensure visibility of the
 * objects in these arrays / collections. In our case, one thread is putting the
 * JdbcTraceElementSafeImpl on to a concurrent queue (right after construction) and the only way
 * another thread can get access to it is by pulling it off of the queue. The concurrent queue
 * stores the instance in a volatile field and so the coordination of the first thread writing to
 * that volatile field and the second thread reading from that volatile field ensures a
 * happens-before relationship which guarantees that the second thread sees everything that was done
 * to the objects in the parameters array prior to the first thread putting the trace element into
 * the concurrent queue.
 * 
 * hasPerformedNext and numRows are marked volatile to ensure visibility to other threads since they
 * are updated after putting the JdbcTraceElementSafeImpl on to the queue and so these updates
 * cannot piggyback on the happens-before relationship created by queuing / dequeuing.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class JdbcProbeExecution implements ProbeExecutionWithUpdate {

    public static final Object NULL_PARAMETER = new Object();

    private final String sql;

    // parameters and batchedParameters cannot both be non-null
    private final List<Object> parameters;
    private final Collection<List<Object>> batchedParameters;

    // this is only used for batching of non-PreparedStatements
    private final Collection<String> batchedSqls;

    private volatile boolean hasPerformedNext;
    private volatile int numRows;

    // TODO doc this
    private volatile Object probeExecutionHolder;

    public JdbcProbeExecution(String sql) {

        this.sql = sql;
        this.parameters = null;
        this.batchedParameters = null;
        this.batchedSqls = null;
    }

    public JdbcProbeExecution(String sql, List<Object> parameters) {

        this.sql = sql;
        this.parameters = parameters;
        this.batchedParameters = null;
        this.batchedSqls = null;
    }

    public JdbcProbeExecution(Collection<String> batchedSqls) {

        this.sql = null;
        this.parameters = null;
        this.batchedParameters = null;
        this.batchedSqls = batchedSqls;
    }

    // TODO clean this up, dummy is only to differentiate since it has the same
    public JdbcProbeExecution(String sql, Collection<List<Object>> batchedParameters,
            boolean dummy) {

        this.sql = sql;
        this.parameters = null;
        this.batchedParameters = batchedParameters;
        this.batchedSqls = null;
    }

    public void printDescription(PrintWriter out) {

        out.print("jdbc execution: ");

        if (batchedSqls != null) {
            printBatchedSqls(out);
            return;
        }

        if (isUsingBatchedParameters() && batchedParameters.size() > 1) {
            // print out number of batches to make it easy to identify
            out.print(batchedParameters.size());
            out.print(" x ");
        }

        out.print(sql);

        if (isUsingParameters() && parameters.size() != 0) {
            printParameters(out, parameters);
        } else if (isUsingBatchedParameters()) {
            for (List<Object> oneParameters : batchedParameters) {
                printParameters(out, oneParameters);
            }
        }

        if (hasPerformedNext) {
            out.print(" => ");
            out.print(numRows);
            if (numRows == 1) {
                out.print(" row");
            } else {
                out.print(" rows");
            }
        }
    }

    // TODO put row num and bind parameters in context map?
    public ProbeExecutionContext createContext() {
        return null;
    }

    public void setHasPerformedNext() {
        hasPerformedNext = true;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
    }

    // only called by tests
    public String getSql() {
        return sql;
    }

    // only called by tests
    // returns unmodifiable structure
    public List<Object> getParameters() {
        // even though this is only called by tests, copy the array just to be clear that this
        // object doesn't expose internal state for modification
        // (though technically we still aren't protecting the objects in the array from
        // modification)
        if (parameters == null) {
            return null;
        } else {
            return Collections.unmodifiableList(parameters);
        }
    }

    // only called by tests
    public Collection<List<Object>> getBatchedParameters() {
        // even though this is only called by tests, wrap the collection just to be clear that this
        // object doesn't expose internal state for modification
        // (though technically we still aren't protecting the objects in the collection from
        // modification)
        return Collections.unmodifiableCollection(Collections2.transform(batchedParameters,
                new Function<List<Object>, List<Object>>() {
                    public List<Object> apply(List<Object> from) {
                        return Collections.unmodifiableList(from);
                    }
                }));
    }

    // only called by tests
    public Collection<String> getBatchedSqls() {
        // even though this is only called by tests, wrap the collection just to be clear that this
        // object doesn't expose internal state for modification
        return Collections.unmodifiableCollection(batchedSqls);
    }

    // only called by tests
    public int getNumRows() {
        return numRows;
    }

    private boolean isUsingParameters() {
        return parameters != null;
    }

    private boolean isUsingBatchedParameters() {
        return batchedParameters != null;
    }

    private void printBatchedSqls(PrintWriter out) {
        boolean first = true;
        for (String batchedSql : batchedSqls) {
            if (!first) {
                out.print(", ");
            }
            out.print(batchedSql);
            first = false;
        }
    }

    private void printParameters(PrintWriter out, List<Object> parameters) {
        out.print(" [");
        boolean first = true;
        for (Object parameter : parameters) {
            if (!first) {
                out.print(", ");
            }
            if (parameter instanceof String) {
                out.print("'");
                out.print(parameter);
                out.print("'");
            } else if (parameter == NULL_PARAMETER) {
                out.print("NULL");
            } else {
                out.print(parameter);
            }
            first = false;
        }
        out.print("]");
    }

    public Object getProbeExecutionHolder() {
        return probeExecutionHolder;
    }

    public void setProbeExecutionHolder(Object probeExecutionHolder) {
        this.probeExecutionHolder = probeExecutionHolder;
    }
}
