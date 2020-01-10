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
package io.seata.rm.datasource.sql;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLRecognizerFactory;

/**
 * @author ggndnn
 */
public class SQLVisitorFactory {
    /**
     * SQLRecognizerFactory.
     */
    private final static SQLRecognizerFactory SQL_RECOGNIZER_FACTORY;

    static {
        String sqlparserType = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SQL_PARSER_TYPE, "druid");
        SQL_RECOGNIZER_FACTORY = EnhancedServiceLoader.load(SQLRecognizerFactory.class, sqlparserType);
    }

    /**
     * Get sql recognizer.
     *
     * @param sql    the sql
     * @param dbType the db type
     * @return the sql recognizer
     */
    public static SQLRecognizer get(String sql, String dbType) {
        return SQL_RECOGNIZER_FACTORY.create(sql, dbType);
    }

    /**
     * Get  multi sql recognizer. not support multi insert or select sql
     *
     * @param sql    the sql
     * @param dbType the db type
     * @return the sql recognizer
     */
    public static List<SQLRecognizer> getMulti(String sql, String dbType) {
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, dbType);
        if (asts == null || asts.size() == 0) {
            throw new UnsupportedOperationException("Unsupported SQL: " + sql);
        }
        if (asts.size() > 1 && asts.stream().anyMatch(statement -> (statement instanceof SQLInsertStatement) || (statement instanceof SQLSelectStatement))) {
            throw new UnsupportedOperationException("Unsupported INSERT OR SELECT MULTI SQL: " + sql);
        }
        final List<SQLRecognizer> recognizers = new ArrayList<>();
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
                recognizer = recognizerHolder.getSelectForUpdateRecognizer(sql, ast);
            }
            if (recognizer != null) {
                recognizers.add(recognizer);
            }
        }
        return recognizers;
    }
}
