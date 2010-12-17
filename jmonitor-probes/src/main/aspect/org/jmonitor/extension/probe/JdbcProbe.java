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

package org.jmonitor.extension.probe;

import java.io.InputStream;
import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.jmonitor.api.probe.ProbeExecution;
import org.jmonitor.api.probe.ProbeExecutionCreator;
import org.jmonitor.api.probe.ProbeExecutionManagerFactory;
import org.jmonitor.extension.probe.JdbcProbeExecution;
import org.jmonitor.extension.probe.PreparedStatementMirror;
import org.jmonitor.extension.probe.StatementMirror;
import org.jmonitor.extension.probe.StatementMirrorCache;
import org.slf4j.Logger;

/**
 * Defines pointcuts to capture data on {@link Statement}, {@link PreparedStatement},
 * {@link CallableStatement} and {@link ResultSet} calls.
 * 
 * All pointcuts use !cflowbelow() constructs in order to pick out only top-level executions since
 * often jdbc drivers are exposed by application servers via wrappers (although this is primarily
 * useful in case runtime weaving is supported in the future which would expose these application
 * server proxies to the weaving process).
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
@Aspect
@SuppressAjWarnings("adviceDidNotMatch")
public class JdbcProbe { // NOPMD for too many methods, many are just AspectJ pointcuts

    private static final Logger LOGGER =
            ProbeExecutionManagerFactory.getManager().getProbeLogger(JdbcProbe.class);

    private static final String JDBC_PREPARE_SUMMARY_KEY = "jdbc prepare";
    private static final String JDBC_EXECUTE_SUMMARY_KEY = "jdbc execute";
    private static final String JDBC_NEXT_SUMMARY_KEY = "jdbc next";

    private static StatementMirrorCache statementMirrorCache = new StatementMirrorCache();

    @Pointcut("if()")
    public static boolean inOperation() {
        return ProbeExecutionManagerFactory.getManager().getRootProbeExecution() != null;
    }

    @Pointcut("if()")
    public static boolean isProbeEnabled() {
        return ProbeExecutionManagerFactory.getManager().isEnabled();
    }

    /*
     * ===================== Statement Preparation =====================
     */

    // capture the sql used to create the PreparedStatement
    @Pointcut("call(java.sql.PreparedStatement+ java.sql.Connection.prepare*(String, ..))")
    void connectionPreparePointcut() {
    }

    // we don't restrict this pointcut to isProbeEnabled() or inOperation()
    // because we need to track PreparedStatements for their entire life
    @Around("connectionPreparePointcut() && !cflowbelow(connectionPreparePointcut()) && args(sql)")
    public Object connectionPrepareAdvice(ProceedingJoinPoint joinPoint, String sql)
            throws Throwable {

        PreparedStatement preparedStatement =
                (PreparedStatement) ProbeExecutionManagerFactory.getManager().proceedAndRecordMetricData(
                        joinPoint, JDBC_PREPARE_SUMMARY_KEY);
        statementMirrorCache.getOrCreatePreparedStatementMirror(preparedStatement, sql);
        return preparedStatement;
    }

    /*
     * ================= Parameter Binding =================
     */

    // capture the parameters that are bound to the PreparedStatement except
    // parameters bound via setNull(..)
    // see special case to handle setNull(..)
    @Pointcut("call(void java.sql.PreparedStatement.set*(int, ..)) && !preparedStatementSetNullPointcut()")
    void preparedStatementSetXPointcut() {
    }

    @Pointcut("call(void java.sql.PreparedStatement.setNull(int, ..))")
    void preparedStatementSetNullPointcut() {
    }

    @AfterReturning("isProbeEnabled() && inOperation()"
            + " && preparedStatementSetXPointcut() && !cflowbelow(preparedStatementSetXPointcut())"
            + " && target(preparedStatement) && args(parameterIndex, x, ..)")
    public void preparedStatementSetXAdvice(PreparedStatement preparedStatement,
            int parameterIndex, Object x) { // NOPMD for short name "x"

        if (x instanceof InputStream || x instanceof Reader) {
            statementMirrorCache.getPreparedStatementMirror(preparedStatement).setParameterValue(
                    parameterIndex, x.getClass().getName() + "@" + x.hashCode());
        } else {
            statementMirrorCache.getPreparedStatementMirror(preparedStatement).setParameterValue(
                    parameterIndex, x);
        }
    }

    @AfterReturning("isProbeEnabled() && inOperation()"
            + " && preparedStatementSetNullPointcut() && !cflowbelow(preparedStatementSetNullPointcut())"
            + " && target(preparedStatement) && args(parameterIndex, ..)")
    public void preparedStatementSetNullAdvice(PreparedStatement preparedStatement,
            int parameterIndex) {

        statementMirrorCache.getPreparedStatementMirror(preparedStatement).setParameterValue(
                parameterIndex, JdbcProbeExecution.NULL_PARAMETER);
    }

    /*
     * ================== Statement Batching ==================
     */

    // handle Statement.addBatch(String)
    @Pointcut("call(void java.sql.Statement.addBatch(String))")
    void statementAddBatchPointcut() {
    }

    @AfterReturning("isProbeEnabled() && inOperation()"
            + " && statementAddBatchPointcut() && !cflowbelow(statementAddBatchPointcut())"
            + " && target(statement) && args(sql)")
    public void statementAddBatchAdvice(Statement statement, String sql) {

        statementMirrorCache.getStatementMirror(statement).addBatch(sql);
    }

    // handle PreparedStatement.addBatch()
    @Pointcut("call(void java.sql.PreparedStatement.addBatch())")
    void preparedStatementAddBatchPointcut() {
    }

    @AfterReturning("isProbeEnabled() && inOperation()"
            + " && preparedStatementAddBatchPointcut() && !cflowbelow(preparedStatementAddBatchPointcut())"
            + " && target(preparedStatement)")
    public void preparedStatementAddBatchAdvice(PreparedStatement preparedStatement) {

        statementMirrorCache.getPreparedStatementMirror(preparedStatement).addBatch();
    }

    /*
     * =================== Statement Execution ===================
     */

    // pointcut for executing Statement
    @Pointcut("call(* java.sql.Statement.execute*(String, ..))")
    void statementExecutePointcut() {
    }

    // pointcut for executing PreparedStatement
    @Pointcut("call(* java.sql.PreparedStatement.execute*())")
    void preparedStatementExecutePointcut() {
    }

    // record call and summary data for Statement.execute()
    // we don't restrict this pointcut to inOperation() because we support an
    // option to log a warning if jdbc calls occur outside of an operation
    // 
    // so we handle the inOperation() restriction inside the method implementation
    @Around("isProbeEnabled()"
            + " && statementExecutePointcut() && !cflowbelow(statementExecutePointcut())"
            + " && target(statement) && args(sql)")
    public Object statementExecuteAdvice(ProceedingJoinPoint joinPoint, final Statement statement,
            final String sql) throws Throwable {

        final StatementMirror statementMirror = statementMirrorCache.getStatementMirror(statement);

        // clear it out since it may not be populated below if the probe execution is
        // short-circuited for some reason
        // TODO remove this (and line above) once we clear it on close()
        statementMirror.setLastProbeExecution(null);

        ProbeExecutionCreator probeExecution = new ProbeExecutionCreator() {
            public ProbeExecution createProbeExecution() {
                JdbcProbeExecution probeExecution = new JdbcProbeExecution(sql);
                statementMirror.setLastProbeExecution(probeExecution);
                return probeExecution;
            }
        };

        return ProbeExecutionManagerFactory.getManager().execute(probeExecution, joinPoint,
                JDBC_EXECUTE_SUMMARY_KEY, true);
    }

    // record trace element and summary data for Statement.execute()
    @Around("isProbeEnabled() && inOperation()"
            + " && preparedStatementExecutePointcut() && !cflowbelow(preparedStatementExecutePointcut())"
            + " && target(preparedStatement)")
    public Object preparedStatementExecuteAdvice(ProceedingJoinPoint joinPoint,
            final PreparedStatement preparedStatement) throws Throwable {

        final PreparedStatementMirror info =
                statementMirrorCache.getPreparedStatementMirror(preparedStatement);

        // clear it out since it may not be populated below if the probe execution is
        // short-circuited for some reason
        // TODO remove this (and line above) once we clear it on close()
        info.setLastProbeExecution(null);

        ProbeExecutionCreator probeExecution = new ProbeExecutionCreator() {
            public ProbeExecution createProbeExecution() {
                JdbcProbeExecution probeExecution =
                        new JdbcProbeExecution(info.getSql(), info.getParametersCopy());
                info.setLastProbeExecution(probeExecution);
                return probeExecution;
            }
        };

        return ProbeExecutionManagerFactory.getManager().execute(probeExecution, joinPoint,
                JDBC_EXECUTE_SUMMARY_KEY, true);
    }

    // handle Statement.executeBatch()
    @Pointcut("call(int[] java.sql.Statement.executeBatch()) && !target(java.sql.PreparedStatement)")
    void statementExecuteBatchPointcut() {
    }

    @Pointcut("call(int[] java.sql.Statement.executeBatch()) && target(java.sql.PreparedStatement)")
    void preparedStatementExecuteBatchPointcut() {
    }

    @Around("isProbeEnabled() && inOperation()"
            + " && statementExecuteBatchPointcut() && !cflowbelow(statementExecuteBatchPointcut())"
            + " && target(statement)")
    public Object statementExecuteBatchAdvice(ProceedingJoinPoint joinPoint, Statement statement)
            throws Throwable {

        final StatementMirror statementMirror = statementMirrorCache.getStatementMirror(statement);

        // clear it out since it may not be populated below if the probe execution is
        // short-circuited for some reason
        // TODO remove this (and line above) once we clear it on close()
        statementMirror.setLastProbeExecution(null);

        ProbeExecutionCreator probeExecution = new ProbeExecutionCreator() {
            public ProbeExecution createProbeExecution() {
                JdbcProbeExecution probeExecution =
                        new JdbcProbeExecution(statementMirror.getBatchedSqlCopy());
                statementMirror.setLastProbeExecution(probeExecution);
                return probeExecution;
            }
        };

        return ProbeExecutionManagerFactory.getManager().execute(probeExecution, joinPoint,
                JDBC_EXECUTE_SUMMARY_KEY, true);
    }

    @Around("isProbeEnabled() && inOperation()"
            + " && preparedStatementExecuteBatchPointcut() && !cflowbelow(preparedStatementExecuteBatchPointcut())"
            + " && target(preparedStatement)")
    public Object preparedStatementExecuteBatchAdvice(ProceedingJoinPoint joinPoint,
            PreparedStatement preparedStatement) throws Throwable {

        final PreparedStatementMirror info =
                statementMirrorCache.getPreparedStatementMirror(preparedStatement);

        // clear it out since it may not be populated below if the probe execution is
        // short-circuited for some reason
        // TODO remove this (and line above) once we clear it on close()
        info.setLastProbeExecution(null);

        ProbeExecutionCreator probeExecution = new ProbeExecutionCreator() {
            public ProbeExecution createProbeExecution() {
                JdbcProbeExecution probeExecution;
                if (info.isUsingBatchedParameters()) {
                    // make a copy of batchedArrays
                    probeExecution =
                            new JdbcProbeExecution(info.getSql(), info.getBatchedParametersCopy());
                } else {
                    // TODO is this branch necessary? are you allowed to call
                    // executeBatch() if you haven't called addBatch() at least once?
                    LOGGER.warn("executeBatch() was called on a PreparedStatement"
                            + " without calling addBatch() first");
                    probeExecution =
                            new JdbcProbeExecution(info.getSql(), info.getParametersCopy());
                }
                info.setLastProbeExecution(probeExecution);
                return probeExecution;
            }
        };

        return ProbeExecutionManagerFactory.getManager().execute(probeExecution, joinPoint,
                JDBC_EXECUTE_SUMMARY_KEY, true);
    }

    /*
     * ========= ResultSet =========
     */

    // It doesn't currently support ResultSet.relative(), absolute(), last().

    // capture the row number any time the cursor is moved through the result set
    @Pointcut("call(boolean java.sql.ResultSet.next())")
    void resultNextPointcut() {
    }

    // capture aggregate timing data around calls to ResultSet.next()
    @Around("isProbeEnabled() && inOperation()"
            + " && resultNextPointcut() && !cflowbelow(resultNextPointcut())"
            + " && target(resultSet)")
    public boolean resultNextAdvice(ProceedingJoinPoint joinPoint, final ResultSet resultSet)
            throws Throwable {

        JdbcProbeExecution lastProbeExecution =
                statementMirrorCache.getStatementMirror(
                        resultSet.getStatement()).getLastProbeExecution();

        if (lastProbeExecution == null) {
            // tracing must be disabled (e.g. exceeded trace limit per operation),
            // but we still gather metric data
            return (Boolean) ProbeExecutionManagerFactory.getManager().proceedAndRecordMetricData(
                    joinPoint, JDBC_NEXT_SUMMARY_KEY);
        }

        boolean currentRowValid =
                (Boolean) ProbeExecutionManagerFactory.getManager().proceedAndRecordMetricData(
                        joinPoint, JDBC_NEXT_SUMMARY_KEY);

        lastProbeExecution.setHasPerformedNext();
        if (currentRowValid) {
            lastProbeExecution.setNumRows(resultSet.getRow());
            // TODO also record time spent in next into JdbcProbeExecution
        }
        ProbeExecutionManagerFactory.getManager().handleProbeExecutionUpdate(lastProbeExecution);

        return currentRowValid;
    }

    /*
     * ================== Statement Clearing ==================
     */

    // Statement.clearBatch() can be used to re-initiate a prepared statement
    // that has been cached from a previous usage
    @Pointcut("call(void java.sql.Statement.clearBatch())")
    void statementClearBatchPointcut() {
    }

    // we don't restrict this pointcut to inOperation() because we need to track
    // PreparedStatements for their entire life
    @AfterReturning("isProbeEnabled()"
            + " && statementClearBatchPointcut() && !cflowbelow(statementClearBatchPointcut())"
            + " && target(statement)")
    public void statementClearBatchAdvice(Statement statement) {

        StatementMirror statementMirror = statementMirrorCache.getStatementMirror(statement);
        statementMirror.clearBatch();
        statementMirror.setLastProbeExecution(null);
    }
}
