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
import java.sql.Statement;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

/**
 * Used by JdbcAspect to associate a {@link StatementInfo} with every {@link Statement}.
 * 
 * {@link StatementInfo} is used to capture and mirror the state of statements since we cannot
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
public class StatementInfoCache {

    private final Cache<Statement, StatementInfo> statementInfoMap = CacheBuilder.newBuilder()
            .weakKeys().build(new StatementInfoLazyMapValueFunction());

    private final Cache<PreparedStatement, PreparedStatementInfo> preparedStatementInfoMap =
            CacheBuilder.newBuilder().weakKeys()
                    .build(new PreparedStatementInfoLazyMapValueFunction());

    public StatementInfo getStatementInfo(Statement statement) {
        if (statement instanceof PreparedStatement) {
            return preparedStatementInfoMap.getUnchecked((PreparedStatement) statement);
        } else {
            return statementInfoMap.getUnchecked(statement);
        }
    }

    public PreparedStatementInfo getPreparedStatementInfo(PreparedStatement preparedStatement) {
        return preparedStatementInfoMap.getUnchecked(preparedStatement);
    }

    private static class StatementInfoLazyMapValueFunction extends
            CacheLoader<Statement, StatementInfo> {

        public StatementInfo load(Statement from) {
            return new StatementInfo();
        }
    }

    private static class PreparedStatementInfoLazyMapValueFunction extends
            CacheLoader<PreparedStatement, PreparedStatementInfo> {

        public PreparedStatementInfo load(PreparedStatement from) {
            return new PreparedStatementInfo();
        }
    }
}
