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

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

/**
 * Used by JdbcAspect to associate a {@link StatementMirror} with every {@link Statement}.
 * 
 * {@link StatementMirror} is used to capture and mirror the state of statements since we cannot
 * inspect the underlying {@link Statement} values after they have been set.
 * 
 * Weak references are used to retain this association for only as long as the underlying
 * {@link Statement} is retained.
 * 
 * Note: {@link PreparedStatement}s are often retained by the application server to be reused later
 * so this association can (and needs to) last for a long time in this case.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class StatementMirrorCache {

    private final ConcurrentMap<Statement, StatementMirror> statementMirrorMap =
            new MapMaker().weakKeys().makeComputingMap(new StatementMirrorLazyMapValueFunction());

    private final ConcurrentMap<PreparedStatement, PreparedStatementMirror> preparedStatementMirrorMap =
            new MapMaker().weakKeys().makeMap();

    public StatementMirror getStatementMirror(Statement statement) {
        if (statement instanceof PreparedStatement) {
            return preparedStatementMirrorMap.get(statement);
        } else {
            return statementMirrorMap.get(statement);
        }
    }

    public PreparedStatementMirror getOrCreatePreparedStatementMirror(
            PreparedStatement preparedStatement, String sql) {

        PreparedStatementMirror info = preparedStatementMirrorMap.get(preparedStatement);
        if (info == null) {
            // shouldn't need to worry about multiple threads putting the same prepared statement
            // into the map at the same time since prepared statements are typically checked out
            // from a pool and owned by a thread until it the prepared statement is closed.
            // however, we handle this case anyways just to be safe since I guess(?) it's possible
            // that a pool could hand out read-only versions of a prepared statement that has no
            // parameters
            preparedStatementMirrorMap.putIfAbsent(preparedStatement, new PreparedStatementMirror(sql));
            info = preparedStatementMirrorMap.get(preparedStatement);
        } else {
            // make sure sql is still the same, in theory pool could reuse previous
            // PreparedStatement instance for new SQL
            if (!info.getSql().equals(sql)) {
                preparedStatementMirrorMap.replace(preparedStatement, info,
                        new PreparedStatementMirror(sql));
                info = preparedStatementMirrorMap.get(preparedStatement);
            }
        }
        return info;
    }

    public PreparedStatementMirror getPreparedStatementMirror(PreparedStatement preparedStatement) {
        PreparedStatementMirror info = preparedStatementMirrorMap.get(preparedStatement);
        if (info == null) {
            return new PreparedStatementMirror(
                    "SQL TEXT WAS NOT CAPTURED BY JMONITOR.  PLEASE REPORT THIS.");
        }
        return info;
    }

    private static class StatementMirrorLazyMapValueFunction implements
            Function<Statement, StatementMirror> {
        public StatementMirror apply(Statement from) {
            return new StatementMirror();
        }
    }
}
