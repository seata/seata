/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.sqlparser.druid;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGSelectQueryBlock;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * DruidSQLRecognizerFactoryImpl
 *
 * @author sharajava
 * @author ggndnn
 */
class DruidSQLRecognizerFactoryImpl implements SQLRecognizerFactory {
    @Override
    public List<SQLRecognizer> create(String sql, String dbType) {
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, dbType);
        if (CollectionUtils.isEmpty(asts)) {
            throw new UnsupportedOperationException("Unsupported SQL: " + sql);
        }
        if (asts.size() > 1 && !(asts.stream().allMatch(statement -> statement instanceof SQLUpdateStatement)
                || asts.stream().allMatch(statement -> statement instanceof SQLDeleteStatement))) {
            throw new UnsupportedOperationException("ONLY SUPPORT SAME TYPE (UPDATE OR DELETE) MULTI SQL -" + sql);
        }
        List<SQLRecognizer> recognizers = null;
        SQLRecognizer recognizer = null;
        for (SQLStatement ast : asts) {
            SQLOperateRecognizerHolder recognizerHolder =
                    SQLOperateRecognizerHolderFactory.getSQLRecognizerHolder(dbType.toLowerCase());
            if (ast instanceof SQLInsertStatement) {
                recognizer = recognizerHolder.getInsertRecognizer(sql, ast);
            } else if (ast instanceof SQLUpdateStatement) {
                recognizer = recognizerHolder.getUpdateRecognizer(sql, ast);
            } else if (ast instanceof SQLDeleteStatement) {
                recognizer = recognizerHolder.getDeleteRecognizer(sql, ast);
            } else if (ast instanceof SQLSelectStatement) {
                if (isForUpdate(ast, dbType)) {
                    recognizer = recognizerHolder.getSelectForUpdateRecognizer(sql, ast);
                } else {
                    recognizer = recognizerHolder.getSelectRecognizer(sql, ast);
                }
            }
            if (recognizer != null) {
                if (recognizers == null) {
                    recognizers = new ArrayList<>();
                }
                recognizers.add(recognizer);
            }
        }
        return recognizers;
    }

    private boolean isForUpdate(SQLStatement ast, String dbType) {
        switch (dbType) {
            case JdbcConstants.MYSQL:
            case JdbcConstants.ORACLE:
                return ((SQLSelectStatement) ast).getSelect().getFirstQueryBlock().isForUpdate();
            case JdbcConstants.POSTGRESQL:
                PGSelectQueryBlock selectQueryBlock = (PGSelectQueryBlock) ((SQLSelectStatement) ast).getSelect().getFirstQueryBlock();
                return selectQueryBlock.getForClause() != null
                        && selectQueryBlock.getForClause().getOption().equals(PGSelectQueryBlock.ForClause.Option.UPDATE);
            default:
                throw new ShouldNeverHappenException("Unsupported DB type " + dbType);
        }
    }
}
