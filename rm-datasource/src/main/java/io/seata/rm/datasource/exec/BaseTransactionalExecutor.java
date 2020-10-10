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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.StringJoiner;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.common.DefaultValues;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.SqlGenerateUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.WhereRecognizer;

/**
 * The type Base transactional executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author sharajava
 */
public abstract class BaseTransactionalExecutor<T, S extends Statement> implements Executor<T> {

    /**
     * The Statement proxy.
     */
    protected StatementProxy<S> statementProxy;

    /**
     * The Statement callback.
     */
    protected StatementCallback<T, S> statementCallback;

    /**
     * The Sql recognizer.
     */
    protected SQLRecognizer sqlRecognizer;

    /**
     * The Sql recognizer.
     */
    protected List<SQLRecognizer> sqlRecognizers;

    private TableMeta tableMeta;

    private static final String NEXT_LINE = "\n";

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static final boolean DO_NOT_CARE_ON_UPDATE_TIMESTAMP_WHEN_INSERT = CONFIG.getBoolean(
            ConfigurationKeys.TRANSACTION_UNDO_DO_NOT_CARE_ON_UPDATE_TIMESTAMP_WHEN_INSERT, DefaultValues.DEFAULT_DO_NOT_CARE_ON_UPDATE_TIMESTAMP_WHEN_INSERT);

    /**
     * Instantiates a new Base transactional executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public BaseTransactionalExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
                                     SQLRecognizer sqlRecognizer) {
        this.statementProxy = statementProxy;
        this.statementCallback = statementCallback;
        this.sqlRecognizer = sqlRecognizer;
    }

    /**
     * Instantiates a new Base transactional executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizers    the multi sql recognizer
     */
    public BaseTransactionalExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
                                     List<SQLRecognizer> sqlRecognizers) {
        this.statementProxy = statementProxy;
        this.statementCallback = statementCallback;
        this.sqlRecognizers = sqlRecognizers;
    }

    @Override
    public T execute(Object... args) throws Throwable {
        if (RootContext.inGlobalTransaction()) {
            String xid = RootContext.getXID();
            statementProxy.getConnectionProxy().bind(xid);
        }

        statementProxy.getConnectionProxy().setGlobalLockRequire(RootContext.requireGlobalLock());
        return doExecute(args);
    }

    /**
     * Do execute object.
     *
     * @param args the args
     * @return the object
     * @throws Throwable the throwable
     */
    protected abstract T doExecute(Object... args) throws Throwable;


    /**
     * build buildWhereCondition
     *
     * @param recognizer        the recognizer
     * @param paramAppenderList the param paramAppender list
     * @return the string
     */
    protected String buildWhereCondition(WhereRecognizer recognizer, ArrayList<List<Object>> paramAppenderList) {
        String whereCondition = null;
        if (statementProxy instanceof ParametersHolder) {
            whereCondition = recognizer.getWhereCondition((ParametersHolder) statementProxy, paramAppenderList);
        } else {
            whereCondition = recognizer.getWhereCondition();
        }
        //process batch operation
        if (StringUtils.isNotBlank(whereCondition) && CollectionUtils.isNotEmpty(paramAppenderList) && paramAppenderList.size() > 1) {
            StringBuilder whereConditionSb = new StringBuilder();
            whereConditionSb.append(" ( ").append(whereCondition).append(" ) ");
            for (int i = 1; i < paramAppenderList.size(); i++) {
                whereConditionSb.append(" or ( ").append(whereCondition).append(" ) ");
            }
            whereCondition = whereConditionSb.toString();
        }
        // fix druid bug parse where contain \n
        if (StringUtils.isNotBlank(whereCondition) && whereCondition.contains(NEXT_LINE)) {
            whereCondition = whereCondition.replace(NEXT_LINE, " ");
        }
        return whereCondition;
    }

    /**
     * Gets column name in sql.
     *
     * @param columnName the column name
     * @return the column name in sql
     */
    protected String getColumnNameInSQL(String columnName) {
        String tableAlias = sqlRecognizer.getTableAlias();
        return tableAlias == null ? columnName : tableAlias + "." + columnName;
    }

    /**
     * Gets several column name in sql.
     *
     * @param columnNameList the column name
     * @return the column name in sql
     */
    protected String getColumnNamesInSQL(List<String> columnNameList) {
        if (Objects.isNull(columnNameList) || columnNameList.isEmpty()) {
            return null;
        }
        StringBuffer columnNamesStr = new StringBuffer();
        for (int i = 0; i < columnNameList.size(); i++) {
            if (i > 0) {
                columnNamesStr.append(" , ");
            }
            columnNamesStr.append(getColumnNameInSQL(columnNameList.get(i)));
        }
        return columnNamesStr.toString();
    }

    /**
     * Gets from table in sql.
     *
     * @return the from table in sql
     */
    protected String getFromTableInSQL() {
        String tableName = sqlRecognizer.getTableName();
        String tableAlias = sqlRecognizer.getTableAlias();
        return tableAlias == null ? tableName : tableName + " " + tableAlias;
    }

    /**
     * Gets table meta.
     *
     * @return the table meta
     */
    protected TableMeta getTableMeta() {
        return getTableMeta(sqlRecognizer.getTableName());
    }

    /**
     * Gets table meta.
     *
     * @param tableName the table name
     * @return the table meta
     */
    protected TableMeta getTableMeta(String tableName) {
        if (tableMeta != null) {
            return tableMeta;
        }
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        tableMeta = TableMetaCacheFactory.getTableMetaCache(connectionProxy.getDbType())
                .getTableMeta(connectionProxy.getTargetConnection(), tableName, connectionProxy.getDataSourceProxy().getResourceId());
        return tableMeta;
    }

    /**
     * the columns contains table meta pk
     *
     * @param columns the column name list
     * @return true: contains pk false: not contains pk
     */
    protected boolean containsPK(List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            return false;
        }
        List<String> newColumns = ColumnUtils.delEscape(columns, getDbType());
        return getTableMeta().containsPK(newColumns);
    }


    /**
     * compare column name and primary key name
     *
     * @param columnName the primary key column name
     * @return true: contain false: not contain
     */
    protected boolean containPK(String columnName) {
        String newColumnName = ColumnUtils.delEscape(columnName, getDbType());
        return CollectionUtils.toUpperList(getTableMeta().getPrimaryKeyOnlyName()).contains(newColumnName.toUpperCase());
    }


    /**
     * get standard column name from user sql column name
     *
     * @return
     */
    protected String getStandardColumnName(String userColumnName) {
        String newUserColumnName = ColumnUtils.delEscape(userColumnName, getDbType());
        for (String cn : getTableMeta().getPrimaryKeyOnlyName()) {
            if (cn.toUpperCase().equals(newUserColumnName.toUpperCase())) {
                return cn;
            }
        }
        return null;
    }

    /**
     * prepare undo log.
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     * @throws SQLException the sql exception
     */
    protected void prepareUndoLog(TableRecords beforeImage, TableRecords afterImage) throws SQLException {
        if (beforeImage.getRows().isEmpty() && afterImage.getRows().isEmpty()) {
            return;
        }

        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();

        TableRecords lockKeyRecords = sqlRecognizer.getSQLType() == SQLType.DELETE ? beforeImage : afterImage;
        String lockKeys = buildLockKey(lockKeyRecords);
        connectionProxy.appendLockKey(lockKeys);

        SQLUndoLog sqlUndoLog = buildUndoItem(beforeImage, afterImage);
        connectionProxy.appendUndoLog(sqlUndoLog);
    }

    /**
     * build lockKey
     *
     * @param rowsIncludingPK the records
     * @return the string as local key. the local key example(multi pk): "t_user:1_a,2_b"
     */
    protected String buildLockKey(TableRecords rowsIncludingPK) {
        if (rowsIncludingPK.size() == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(rowsIncludingPK.getTableMeta().getTableName());
        sb.append(":");
        int filedSequence = 0;
        List<Map<String, Field>> pksRows = rowsIncludingPK.pkRows();
        for (Map<String, Field> rowMap : pksRows) {
            int pkSplitIndex = 0;
            for (String pkName : getTableMeta().getPrimaryKeyOnlyName()) {
                if (pkSplitIndex > 0) {
                    sb.append("_");
                }
                sb.append(rowMap.get(pkName).getValue());
                pkSplitIndex++;
            }
            filedSequence++;
            if (filedSequence < pksRows.size()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * build a SQLUndoLog
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     * @return sql undo log
     */
    protected SQLUndoLog buildUndoItem(TableRecords beforeImage, TableRecords afterImage) {
        SQLType sqlType = sqlRecognizer.getSQLType();
        String tableName = sqlRecognizer.getTableName();

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(sqlType);
        sqlUndoLog.setTableName(tableName);
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);
        return sqlUndoLog;
    }


    /**
     * build a BeforeImage
     *
     * @param tableMeta         the tableMeta
     * @param selectSQL         the selectSQL
     * @param paramAppenderList the paramAppender list
     * @return a tableRecords
     * @throws SQLException the sql exception
     */
    protected TableRecords buildTableRecords(TableMeta tableMeta, String selectSQL, ArrayList<List<Object>> paramAppenderList) throws SQLException {
        ResultSet rs = null;
        try (PreparedStatement ps = statementProxy.getConnection().prepareStatement(selectSQL)) {
            if (CollectionUtils.isNotEmpty(paramAppenderList)) {
                for (int i = 0, ts = paramAppenderList.size(); i < ts; i++) {
                    List<Object> paramAppender = paramAppenderList.get(i);
                    for (int j = 0, ds = paramAppender.size(); j < ds; j++) {
                        ps.setObject(i * ds + j + 1, paramAppender.get(j));
                    }
                }
            }
            rs = ps.executeQuery();
            return TableRecords.buildRecords(tableMeta, rs);
        } finally {
            IOUtil.close(rs);
        }
    }

    /**
     * build TableRecords
     *
     * @param pkValuesMap the pkValuesMap
     * @return return TableRecords;
     * @throws SQLException
     */
    protected TableRecords buildTableRecords(Map<String, List<Object>> pkValuesMap) throws SQLException {
        List<String> pkColumnNameList = getTableMeta().getPrimaryKeyOnlyName();

        String firstKey = pkValuesMap.keySet().stream().findFirst().get();
        int rowSize = pkValuesMap.get(firstKey).size();
        String whereValueSql = SqlGenerateUtils.buildWhereConditionByPKs(pkColumnNameList, rowSize, getDbType());

        StringBuilder prefix = new StringBuilder("SELECT ");
        String suffix = " FROM " + getFromTableInSQL() + " WHERE " + whereValueSql;
        StringJoiner selectSQLJoiner = new StringJoiner(", ", prefix.toString(), suffix);

        for (String columnName : getTableMeta().getAllColumns().keySet()) {
            if (canIgnoreOnUpdateTimestamp(tableMeta, columnName)) {
                continue;
            }
            selectSQLJoiner.add(ColumnUtils.addEscape(columnName, getDbType()));
        }

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = statementProxy.getConnection().prepareStatement(selectSQLJoiner.toString());

            int paramIndex = 1;
            for (int r = 0; r < rowSize; r++) {
                for (int c = 0; c < pkColumnNameList.size(); c++) {
                    List<Object> pkColumnValueList = pkValuesMap.get(pkColumnNameList.get(c));
                    int dataType = tableMeta.getColumnMeta(pkColumnNameList.get(c)).getDataType();
                    ps.setObject(paramIndex, pkColumnValueList.get(r), dataType);
                    paramIndex++;
                }
            }
            rs = ps.executeQuery();
            return TableRecords.buildRecords(getTableMeta(), rs);
        } finally {
            IOUtil.close(rs);
        }
    }

    private boolean canIgnoreOnUpdateTimestamp(TableMeta tableMeta, String columnName) {
        if (DO_NOT_CARE_ON_UPDATE_TIMESTAMP_WHEN_INSERT) {
            ColumnMeta currentColumn = tableMeta.getColumnMeta(ColumnUtils.delEscape(columnName, getDbType()));
            return "Yes".equals(currentColumn.getIsOnUpdateTimestamp());
        }
        return false;
    }

    /**
     * get db type
     *
     * @return
     */
    protected String getDbType() {
        return statementProxy.getConnectionProxy().getDbType();
    }

}
