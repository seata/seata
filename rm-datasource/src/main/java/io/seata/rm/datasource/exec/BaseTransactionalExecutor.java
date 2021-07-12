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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.SqlGenerateUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
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
        String xid = RootContext.getXID();
        if (xid != null) {
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
        StringBuilder columnNamesStr = new StringBuilder();
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
     * get standard pk column name from user sql column name
     *
     * @param userColumnName the user column name
     * @return standard pk column name
     */
    protected String getStandardPkColumnName(String userColumnName) {
        String newUserColumnName = ColumnUtils.delEscape(userColumnName, getDbType());
        for (String cn : getTableMeta().getPrimaryKeyOnlyName()) {
            if (cn.equalsIgnoreCase(newUserColumnName)) {
                return cn;
            }
        }
        return null;
    }

    /**
     * Assert we can undo this operation precisely and safely by given beforeImage and afterImage.
     * Otherwise, throws an exception.
     *
     * @param beforeImage the beforeImage.
     * @param afterImage the afterImage.
     */
    protected final void checkBeforeAfterImages(TableRecords beforeImage, TableRecords afterImage) throws SQLException {
        if (SQLType.UPDATE == sqlRecognizer.getSQLType()) {
            if (beforeImage.getRows().size() != afterImage.getRows().size()) {
                // Forbid update primary keys, otherwise we can not rollback by seata undoLog.
                // Why dont check update primary keys before execution? see:
                // https://github.com/seata/seata/pull/3287
                // https://github.com/seata/seata/issues/3465
                throw new ShouldNeverHappenException(
                        "Before image size is not equaled to after image size, probably because you updated the "
                                + "primary keys.");
            }
            Set<List<Field>> beforeImagePks = beforeImage.getRows().stream().map(Row::primaryKeys)
                    .collect(Collectors.toSet());
            Set<List<Field>> afterImagePks = afterImage.getRows().stream().map(Row::primaryKeys)
                    .collect(Collectors.toSet());
            if (!Sets.difference(beforeImagePks, afterImagePks).isEmpty()) {
                throw new SQLException(
                        "Before image and after image has different primary keys, we can't safely rollback this "
                                + "operation by undoLog. Possible reason: https://github.com/seata/seata/issues/3611");
            }
        }
        // More SqlTypes to be check. eg. INSERT_IGNORE, INSERT_ON_DUPLICATE_UPDATE
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
        checkBeforeAfterImages(beforeImage, afterImage);
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();

        TableRecords lockKeyRecords = sqlRecognizer.getSQLType() == SQLType.DELETE ? beforeImage : afterImage;
        String lockKeys = buildLockKey(lockKeyRecords);
        if (null != lockKeys) {
            connectionProxy.appendLockKey(lockKeys);

            SQLUndoLog sqlUndoLog = buildUndoItem(beforeImage, afterImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
        }
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
        List<String> primaryKeysOnlyName = getTableMeta().getPrimaryKeyOnlyName();
        for (Map<String, Field> rowMap : pksRows) {
            int pkSplitIndex = 0;
            for (String pkName : primaryKeysOnlyName) {
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
     * @throws SQLException the sql exception
     */
    protected TableRecords buildTableRecords(Map<String, List<Object>> pkValuesMap) throws SQLException {
        List<String> pkColumnNameList = getTableMeta().getPrimaryKeyOnlyName();
        StringBuilder sql = new StringBuilder()
            .append("SELECT * FROM ")
            .append(getFromTableInSQL())
            .append(" WHERE ");
        // build check sql
        String firstKey = pkValuesMap.keySet().stream().findFirst().get();
        int rowSize = pkValuesMap.get(firstKey).size();
        sql.append(SqlGenerateUtils.buildWhereConditionByPKs(pkColumnNameList, rowSize, getDbType()));

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = statementProxy.getConnection().prepareStatement(sql.toString());

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

    /**
     * get db type
     *
     * @return db type
     */
    protected String getDbType() {
        return statementProxy.getConnectionProxy().getDbType();
    }

}
