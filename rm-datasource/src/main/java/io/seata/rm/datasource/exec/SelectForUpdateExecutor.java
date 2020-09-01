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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLSelectRecognizer;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Select for update executor.
 *
 * @param <S> the type parameter
 * @author sharajava
 */
public class SelectForUpdateExecutor<T, S extends Statement> extends BaseTransactionalExecutor<T, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelectForUpdateExecutor.class);

    /**
     * Instantiates a new Select for update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public SelectForUpdateExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
                                   SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    public T doExecute(Object... args) throws Throwable {
        Connection conn = statementProxy.getConnection();
        DatabaseMetaData dbmd = conn.getMetaData();
        T rs;
        Savepoint sp = null;
        LockRetryController lockRetryController = new LockRetryController();
        boolean originalAutoCommit = conn.getAutoCommit();
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String selectPKSQL = buildSelectSQL(paramAppenderList);
        try {
            if (originalAutoCommit) {
                /*
                 * In order to hold the local db lock during global lock checking
                 * set auto commit value to false first if original auto commit was true
                 */
                conn.setAutoCommit(false);
            } else if (dbmd.supportsSavepoints()) {
                /*
                 * In order to release the local db lock when global lock conflict
                 * create a save point if original auto commit was false, then use the save point here to release db
                 * lock during global lock checking if necessary
                 */
                sp = conn.setSavepoint();
            } else {
                throw new SQLException("not support savepoint. please check your db version");
            }

            while (true) {
                try {
                    // #870
                    // execute return Boolean
                    // executeQuery return ResultSet
                    rs = statementCallback.execute(statementProxy.getTargetStatement(), args);

                    // Try to get global lock of those rows selected
                    TableRecords selectPKRows = buildTableRecords(getTableMeta(), selectPKSQL, paramAppenderList);
                    String lockKeys = buildLockKey(selectPKRows);
                    if (StringUtils.isNullOrEmpty(lockKeys)) {
                        break;
                    }

                    if (RootContext.inGlobalTransaction() || RootContext.requireGlobalLock()) {
                        // do as usual
                        // in @GlobalLock env, don't need to append key eithor,
                        // but check lock hear, do the same thing as @GlobalTransactional env
                        statementProxy.getConnectionProxy().checkLock(lockKeys);
                    } else {
                        throw new RuntimeException("Unknown situation!");
                    }
                    break;
                } catch (LockConflictException lce) {
                    if (sp != null) {
                        conn.rollback(sp);
                    } else {
                        conn.rollback();
                    }

                    // retry hear only when the config is true
                    if (LockRetryPolicy.isLockRetryPolicyBranchRollbackOnConflict()) {
                        lockRetryController.sleep(lce);
                    } else {
                        throw new LockWaitTimeoutException("Global lock wait timeout", lce);
                    }
                }
            }
        } finally {
            if (sp != null) {
                try {
                    conn.releaseSavepoint(sp);
                } catch (SQLException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("{} does not support release save point, but this is not a error.", getDbType());
                    }
                }
            }
            if (originalAutoCommit) {
                conn.setAutoCommit(true);
            }
        }
        return rs;
    }

    private String buildSelectSQL(ArrayList<List<Object>> paramAppenderList) {
        SQLSelectRecognizer recognizer = (SQLSelectRecognizer)sqlRecognizer;
        StringBuilder selectSQLAppender = new StringBuilder("SELECT ");
        selectSQLAppender.append(getColumnNamesInSQL(getTableMeta().getEscapePkNameList(getDbType())));
        selectSQLAppender.append(" FROM ").append(getFromTableInSQL());
        String whereCondition = buildWhereCondition(recognizer, paramAppenderList);
        if (StringUtils.isNotBlank(whereCondition)) {
            selectSQLAppender.append(" WHERE ").append(whereCondition);
        }
        selectSQLAppender.append(" FOR UPDATE");
        return selectSQLAppender.toString();
    }
}
