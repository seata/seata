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
package io.seata.rm.datasource.exec;


import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLRecognizer;

/**
 * The type MultiSql executor. now just support same type
 * ex.
 * <pre>
 *  jdbcTemplate.update("update account_tbl set money = money - ? where user_id = ?;update account_tbl set money = money - ? where user_id = ?", new Object[] {money, userId,"U10000",money,"U1000"});
 *  </pre>
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author wangwei.ying
 */
public class MultiExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    private Map<String, List<SQLRecognizer>> multiSqlGroup = new HashMap<>(4);
    private Map<SQLRecognizer, TableRecords> beforeImagesMap = new HashMap<>(4);
    private Map<SQLRecognizer, TableRecords> afterImagesMap = new HashMap<>(4);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizers    the sql recognizers
     */
    public MultiExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, List<SQLRecognizer> sqlRecognizers) {
        super(statementProxy, statementCallback, sqlRecognizers);
    }

    /**
     * Before image table records.  only support update or deleted
     *
     * @return the table records
     * @throws SQLException the sql exception
     * @see io.seata.rm.datasource.sql.SQLVisitorFactory#get(String, String) validate sqlType
     */
    @Override
    protected TableRecords beforeImage() throws SQLException {
        //group by sqlType
        multiSqlGroup = sqlRecognizers.stream().collect(Collectors.groupingBy(t -> t.getTableName()));
        AbstractDMLBaseExecutor<T, S> executor = null;
        for (List<SQLRecognizer> value : multiSqlGroup.values()) {
            switch (value.get(0).getSQLType()) {
                case UPDATE:
                    executor = new MultiUpdateExecutor<T, S>(statementProxy, statementCallback, value);
                    break;
                case DELETE:
                    executor = new MultiDeleteExecutor<T, S>(statementProxy, statementCallback, value);
                    break;
                default:
                    throw new UnsupportedOperationException("not support sql" + value.get(0).getOriginalSQL());
            }
            TableRecords beforeImage = executor.beforeImage();
            beforeImagesMap.put(value.get(0), beforeImage);
        }
        return null;
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        AbstractDMLBaseExecutor<T, S> executor = null;
        for (List<SQLRecognizer> value : multiSqlGroup.values()) {
            switch (value.get(0).getSQLType()) {
                case UPDATE:
                    executor = new MultiUpdateExecutor<T, S>(statementProxy, statementCallback, value);
                    break;
                case DELETE:
                    executor = new MultiDeleteExecutor<T, S>(statementProxy, statementCallback, value);
                    break;
                default:
                    throw new UnsupportedOperationException("not support sql" + value.get(0).getOriginalSQL());
            }
            beforeImage = beforeImagesMap.get(value.get(0));
            TableRecords afterImage = executor.afterImage(beforeImage);
            afterImagesMap.put(value.get(0), afterImage);
        }
        return null;
    }


    @Override
    protected void prepareUndoLog(TableRecords beforeImage, TableRecords afterImage) throws SQLException {
        if (beforeImagesMap == null || afterImagesMap == null) {
            throw new IllegalStateException("images can not be null");
        }
        SQLRecognizer recognizer;
        for (Map.Entry<SQLRecognizer, TableRecords> entry : beforeImagesMap.entrySet()) {
            sqlRecognizer = recognizer = entry.getKey();
            beforeImage = entry.getValue();
            afterImage = afterImagesMap.get(recognizer);
            checkBeforeAfterImages(beforeImage, afterImage);
            super.prepareUndoLog(beforeImage, afterImage);
        }
    }

    public Map<String, List<SQLRecognizer>> getMultiSqlGroup() {
        return multiSqlGroup;
    }

    public Map<SQLRecognizer, TableRecords> getBeforeImagesMap() {
        return beforeImagesMap;
    }

    public Map<SQLRecognizer, TableRecords> getAfterImagesMap() {
        return afterImagesMap;
    }
}
