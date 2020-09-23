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
package io.seata.server.storage.db.store;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import io.seata.common.exception.DataAccessException;
import io.seata.common.exception.StoreException;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.store.AbstractLogStore;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalCondition;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.core.store.SortOrder;
import io.seata.core.store.SortParam;
import io.seata.core.store.db.sql.log.LogStoreSqlsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.DefaultValues.DEFAULT_STORE_DB_BRANCH_TABLE;
import static io.seata.common.DefaultValues.DEFAULT_STORE_DB_GLOBAL_TABLE;

/**
 * The type Log store data base dao.
 *
 * @author zhangsen
 */
public class LogStoreDataBaseDAO extends AbstractLogStore {

    //region Constants

    private static final Logger LOGGER = LoggerFactory.getLogger(LogStoreDataBaseDAO.class);

    /**
     * The transaction name key
     */
    private static final String TRANSACTION_NAME_KEY = "TRANSACTION_NAME";
    /**
     * The transaction name default size is 128
     */
    private static final int TRANSACTION_NAME_DEFAULT_SIZE = 128;

    //endregion

    //region Fields

    /**
     * The Log store data source.
     */
    protected DataSource logStoreDataSource;

    /**
     * The Global table.
     */
    protected String globalTable;

    /**
     * The Branch table.
     */
    protected String branchTable;

    /**
     * The db type.
     */
    private String dbType;

    /**
     * The transaction name column size.
     */
    private int transactionNameColumnSize = TRANSACTION_NAME_DEFAULT_SIZE;

    //endregion

    //region Constructor

    /**
     * Instantiates a new Log store data base dao.
     *
     * @param logStoreDataSource the log store data source
     */
    public LogStoreDataBaseDAO(DataSource logStoreDataSource) {
        this.logStoreDataSource = logStoreDataSource;
        globalTable = CONFIG.getConfig(ConfigurationKeys.STORE_DB_GLOBAL_TABLE,
            DEFAULT_STORE_DB_GLOBAL_TABLE);
        branchTable = CONFIG.getConfig(ConfigurationKeys.STORE_DB_BRANCH_TABLE,
            DEFAULT_STORE_DB_BRANCH_TABLE);
        dbType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_TYPE);
        if (StringUtils.isBlank(dbType)) {
            throw new StoreException("there must be db type.");
        }
        if (logStoreDataSource == null) {
            throw new StoreException("there must be logStoreDataSource.");
        }
        // init transaction_name size
        initTransactionNameSize();
    }

    //endregion

    //region Override LogStore

    @Override
    public GlobalTransactionDO queryGlobalTransactionDO(String xid) {
        String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getQueryGlobalTransactionSQL(globalTable);
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, xid);
            rs = ps.executeQuery();
            if (rs.next()) {
                return convertGlobalTransactionDO(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps, conn);
        }
    }

    @Override
    public GlobalTransactionDO queryGlobalTransactionDO(long transactionId) {
        String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setLong(1, transactionId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return convertGlobalTransactionDO(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps, conn);
        }
    }

    @Override
    public List<GlobalTransactionDO> queryGlobalTransactionDO(GlobalCondition condition) {
        List<GlobalTransactionDO> ret = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);

            // where place holder
            String wherePlaceHolder = this.buildWherePlaceHolder(condition);
            // order by place holder
            String orderByPlaceHolder = this.buildOrderByPlaceHolder(condition);

            // build sql
            String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getQueryGlobalTransactionSQLByCondition(globalTable,
                    wherePlaceHolder, orderByPlaceHolder, condition);

            ps = conn.prepareStatement(sql);

            // set condition parameters
            int i = this.setConditionParameters(ps, condition);
            // set paging parameters
            LogStoreSqlsFactory.getLogStoreSqls(dbType).setQueryGlobalTransactionSQLPagingParameters(ps, condition, i);

            rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(convertGlobalTransactionDO(rs));
            }
            return ret;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps, conn);
        }
    }

    @Override
    public int countGlobalTransactionDO(GlobalCondition condition) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);

            // where place holder
            String wherePlaceHolder = this.buildWherePlaceHolder(condition);

            // build sql
            String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getCountGlobalTransactionSQLByCondition(globalTable,
                    wherePlaceHolder);

            ps = conn.prepareStatement(sql);

            // set condition parameters
            this.setConditionParameters(ps, condition);

            rs = ps.executeQuery();
            int ret = 0;
            if (rs.next()) {
                ret = rs.getInt(1);
            }
            return ret;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps, conn);
        }
    }

    @Override
    public boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getInsertGlobalTransactionSQL(globalTable);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, globalTransactionDO.getXid());
            ps.setLong(2, globalTransactionDO.getTransactionId());
            ps.setInt(3, globalTransactionDO.getStatus());
            ps.setString(4, globalTransactionDO.getApplicationId());
            ps.setString(5, globalTransactionDO.getTransactionServiceGroup());
            String transactionName = globalTransactionDO.getTransactionName();
            transactionName = transactionName.length() > transactionNameColumnSize ? transactionName.substring(0,
                transactionNameColumnSize) : transactionName;
            ps.setString(6, transactionName);
            ps.setInt(7, globalTransactionDO.getTimeout());
            ps.setLong(8, globalTransactionDO.getBeginTime());
            ps.setString(9, globalTransactionDO.getApplicationData());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
    }

    @Override
    public boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        // sets place holder
        StringBuilder setsPlaceHolder = new StringBuilder();
        setsPlaceHolder.append(ServerTableColumnsName.GLOBAL_TABLE_STATUS).append(" = ?, ");

        // build update sql
        String updateSql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getUpdateGlobalTransactionSQL(globalTable, setsPlaceHolder.toString());

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);

            //sets
            ps = conn.prepareStatement(updateSql);
            ps.setInt(1, globalTransactionDO.getStatus());
            ps.setString(2, globalTransactionDO.getXid());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
    }

    @Override
    public boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getDeleteGlobalTransactionSQL(globalTable);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, globalTransactionDO.getXid());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
        return true;
    }

    @Override
    public List<BranchTransactionDO> queryBranchTransactionDO(String xid) {
        List<BranchTransactionDO> rets = new ArrayList<>();
        String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getQueryBranchTransaction(branchTable);
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);

            ps = conn.prepareStatement(sql);
            ps.setString(1, xid);

            rs = ps.executeQuery();
            while (rs.next()) {
                rets.add(convertBranchTransactionDO(rs));
            }
            return rets;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps, conn);
        }
    }

    @Override
    public List<BranchTransactionDO> queryBranchTransactionDO(List<String> xids) {
        int length = xids.size();
        List<BranchTransactionDO> rets = new ArrayList<>(length * 3);
        String paramsPlaceHolder = org.apache.commons.lang.StringUtils.repeat("?", ",", length);
        String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getQueryBranchTransaction(branchTable, paramsPlaceHolder);
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < length; i++) {
                ps.setString(i + 1, xids.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                rets.add(convertBranchTransactionDO(rs));
            }
            return rets;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps, conn);
        }
    }

    @Override
    public boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getInsertBranchTransactionSQL(branchTable);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, branchTransactionDO.getXid());
            ps.setLong(2, branchTransactionDO.getTransactionId());
            ps.setLong(3, branchTransactionDO.getBranchId());
            ps.setString(4, branchTransactionDO.getResourceGroupId());
            ps.setString(5, branchTransactionDO.getResourceId());
            ps.setString(6, branchTransactionDO.getBranchType());
            ps.setInt(7, branchTransactionDO.getStatus());
            ps.setString(8, branchTransactionDO.getClientId());
            ps.setString(9, branchTransactionDO.getApplicationData());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
    }

    @Override
    public boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        // sets place holder
        StringBuilder setsPlaceHolder = new StringBuilder();
        setsPlaceHolder.append(ServerTableColumnsName.BRANCH_TABLE_STATUS).append(" = ?, ");
        setsPlaceHolder.append(ServerTableColumnsName.BRANCH_TABLE_APPLICATION_DATA).append(" = ?, ");

        // build update branch sql
        String updateSql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getUpdateBranchTransactionSQL(branchTable, setsPlaceHolder.toString());

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);

            //sets
            ps = conn.prepareStatement(updateSql);
            ps.setInt(1, branchTransactionDO.getStatus());
            ps.setString(2, branchTransactionDO.getApplicationData());
            ps.setString(3, branchTransactionDO.getXid());
            ps.setLong(4, branchTransactionDO.getBranchId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
    }

    @Override
    public boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, branchTransactionDO.getXid());
            ps.setLong(2, branchTransactionDO.getBranchId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
        return true;
    }

    @Override
    public long getCurrentMaxSessionId(long high, long low) {
        String transMaxSql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getQueryGlobalMax(globalTable);
        String branchMaxSql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getQueryBranchMax(branchTable);
        long maxTransId = getCurrentMaxSessionId(transMaxSql, high, low);
        long maxBranchId = getCurrentMaxSessionId(branchMaxSql, high, low);
        return maxBranchId > maxTransId ? maxBranchId : maxTransId;
    }

    //endregion

    //region Private

    private long getCurrentMaxSessionId(String sql, long high, long low) {
        long max = 0;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setLong(1, high);
            ps.setLong(2, low);

            rs = ps.executeQuery();
            while (rs.next()) {
                max = rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps, conn);
        }
        return max;
    }

    private String buildWherePlaceHolder(GlobalCondition condition) {
        // where
        StringBuilder wherePlaceHolder = new StringBuilder();
        // status in (?, ?, ?)
        if (condition.getStatuses() != null && condition.getStatuses().length > 0) {
            wherePlaceHolder.append(wherePlaceHolder.length() == 0 ? " where " : " and ")
                    .append(ServerTableColumnsName.GLOBAL_TABLE_STATUS);
            if (condition.getStatuses().length > 1) {
                wherePlaceHolder.append(" in (");
                String paramsPlaceHolder = org.apache.commons.lang.StringUtils.repeat("?", ",", condition.getStatuses().length);
                wherePlaceHolder.append(paramsPlaceHolder);
                wherePlaceHolder.append(")");
            } else {
                wherePlaceHolder.append(" = ?");
            }
        }
        //  true: begin_time  < System.currentTimeMillis() - timeout
        // false: begin_time >= System.currentTimeMillis() - timeout
        if (condition.getIsTimeoutData() != null) {
            wherePlaceHolder.append(wherePlaceHolder.length() == 0 ? " where " : " and ")
                    .append(ServerTableColumnsName.GLOBAL_TABLE_BEGIN_TIME);
            if (condition.getIsTimeoutData()) {
                wherePlaceHolder.append(" < ? - timeout");
            } else {
                wherePlaceHolder.append(" >= ? - timeout");
            }
        }
        // begin_time < System.currentTimeMillis() - :overTimeAliveMills
        if (condition.getOverTimeAliveMills() > 0) {
            wherePlaceHolder.append(wherePlaceHolder.length() == 0 ? " where " : " and ")
                    .append(ServerTableColumnsName.GLOBAL_TABLE_BEGIN_TIME).append(" < ?");
        }
        // gmt_modified >= :minGmtModified
        if (condition.getMinGmtModified() != null) {
            wherePlaceHolder.append(wherePlaceHolder.length() == 0 ? " where " : " and ")
                    .append(ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED).append(" >= ?");
        }

        return wherePlaceHolder.toString();
    }

    private String buildOrderByPlaceHolder(GlobalCondition condition) {
        if (condition.hasSortParams()) {
            StringBuilder orderByPlaceHolder = new StringBuilder(" order by ");
            SortParam[] sortParams = condition.getSortParams();
            SortParam sortParam;
            for (int i = 0, l = sortParams.length; i < l; ++i) {
                sortParam = sortParams[i];
                if (i > 0) {
                    orderByPlaceHolder.append(", ");
                }
                orderByPlaceHolder.append(sortParam.getSortFieldName());
                if (SortOrder.DESC == sortParam.getSortOrder()) {
                    orderByPlaceHolder.append(" desc");
                }
            }
            return orderByPlaceHolder.toString();
        } else {
            // db mode default sort is: order by gmt_modified asc
            return " order by " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED;
        }
    }

    private int setConditionParameters(PreparedStatement ps, GlobalCondition condition) throws SQLException {
        int i = 0;
        long now = System.currentTimeMillis();
        // status in (?, ?, ?)
        if (condition.getStatuses() != null && condition.getStatuses().length > 0) {
            for (int j = 0, l = condition.getStatuses().length; j < l; j++) {
                ps.setInt(++i, condition.getStatuses()[j].getCode());
            }
        }
        //  true: begin_time  < System.currentTimeMillis() - timeout
        // false: begin_time >= System.currentTimeMillis() - timeout
        if (condition.getIsTimeoutData() != null) {
            ps.setLong(++i, now);
        }
        // begin_time < System.currentTimeMillis() - :overTimeAliveMills
        if (condition.getOverTimeAliveMills() > 0) {
            ps.setLong(++i, now - condition.getOverTimeAliveMills());
        }
        // gmt_modified >= :minGmtModified
        if (condition.getMinGmtModified() != null) {
            ps.setDate(++i, new Date(condition.getMinGmtModified().getTime()));
        }

        return i;
    }

    private GlobalTransactionDO convertGlobalTransactionDO(ResultSet rs) throws SQLException {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_XID));
        globalTransactionDO.setStatus(rs.getInt(ServerTableColumnsName.GLOBAL_TABLE_STATUS));
        globalTransactionDO.setApplicationId(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_ID));
        globalTransactionDO.setBeginTime(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_BEGIN_TIME));
        globalTransactionDO.setTimeout(rs.getInt(ServerTableColumnsName.GLOBAL_TABLE_TIMEOUT));
        globalTransactionDO.setTransactionId(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID));
        globalTransactionDO.setTransactionName(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_NAME));
        globalTransactionDO.setTransactionServiceGroup(
            rs.getString(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_SERVICE_GROUP));
        globalTransactionDO.setApplicationData(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_DATA));
        globalTransactionDO.setGmtCreate(rs.getTimestamp(ServerTableColumnsName.GLOBAL_TABLE_GMT_CREATE));
        globalTransactionDO.setGmtModified(rs.getTimestamp(ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED));
        return globalTransactionDO;
    }

    private BranchTransactionDO convertBranchTransactionDO(ResultSet rs) throws SQLException {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setResourceGroupId(rs.getString(ServerTableColumnsName.BRANCH_TABLE_RESOURCE_GROUP_ID));
        branchTransactionDO.setStatus(rs.getInt(ServerTableColumnsName.BRANCH_TABLE_STATUS));
        branchTransactionDO.setApplicationData(rs.getString(ServerTableColumnsName.BRANCH_TABLE_APPLICATION_DATA));
        branchTransactionDO.setClientId(rs.getString(ServerTableColumnsName.BRANCH_TABLE_CLIENT_ID));
        branchTransactionDO.setXid(rs.getString(ServerTableColumnsName.BRANCH_TABLE_XID));
        branchTransactionDO.setResourceId(rs.getString(ServerTableColumnsName.BRANCH_TABLE_RESOURCE_ID));
        branchTransactionDO.setBranchId(rs.getLong(ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID));
        branchTransactionDO.setBranchType(rs.getString(ServerTableColumnsName.BRANCH_TABLE_BRANCH_TYPE));
        branchTransactionDO.setTransactionId(rs.getLong(ServerTableColumnsName.BRANCH_TABLE_TRANSACTION_ID));
        branchTransactionDO.setGmtCreate(rs.getTimestamp(ServerTableColumnsName.BRANCH_TABLE_GMT_CREATE));
        branchTransactionDO.setGmtModified(rs.getTimestamp(ServerTableColumnsName.BRANCH_TABLE_GMT_MODIFIED));
        return branchTransactionDO;
    }

    /**
     * the public modifier only for test
     */
    public void initTransactionNameSize() {
        ColumnInfo columnInfo = queryTableStructure(globalTable, TRANSACTION_NAME_KEY);
        if (columnInfo == null) {
            LOGGER.warn("{} table or {} column not found", globalTable, TRANSACTION_NAME_KEY);
            return;
        }
        this.transactionNameColumnSize = columnInfo.getColumnSize();
    }

    /**
     * query column info from table
     *
     * @param tableName the table name
     * @param colName   the column name
     * @return the column info
     */
    private ColumnInfo queryTableStructure(final String tableName, String colName) {
        try (Connection conn = logStoreDataSource.getConnection()) {
            DatabaseMetaData dbmd = conn.getMetaData();
            String schema = getSchema(conn);
            ResultSet tableRs = dbmd.getTables(null, schema, "%", new String[]{"TABLE"});
            while (tableRs.next()) {
                String table = tableRs.getString("TABLE_NAME");
                if (StringUtils.equalsIgnoreCase(table, tableName)) {
                    ResultSet columnRs = conn.getMetaData().getColumns(null, schema, tableName, null);
                    while (columnRs.next()) {
                        ColumnInfo info = new ColumnInfo();
                        String columnName = columnRs.getString("COLUMN_NAME");
                        info.setColumnName(columnName);
                        String typeName = columnRs.getString("TYPE_NAME");
                        info.setTypeName(typeName);
                        int columnSize = columnRs.getInt("COLUMN_SIZE");
                        info.setColumnSize(columnSize);
                        String remarks = columnRs.getString("REMARKS");
                        info.setRemarks(remarks);
                        if (StringUtils.equalsIgnoreCase(columnName, colName)) {
                            return info;
                        }
                    }
                    break;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("query transaction_name size fail, {}", e.getMessage(), e);
        }
        return null;
    }

    private String getSchema(Connection conn) throws SQLException {
        if ("h2".equalsIgnoreCase(dbType)) {
            return null;
        }
        return conn.getMetaData().getUserName();
    }

    //endregion

    //region Gets and Sets

    /**
     * Sets log store data source.
     *
     * @param logStoreDataSource the log store data source
     */
    public void setLogStoreDataSource(DataSource logStoreDataSource) {
        this.logStoreDataSource = logStoreDataSource;
    }

    /**
     * Sets global table.
     *
     * @param globalTable the global table
     */
    public void setGlobalTable(String globalTable) {
        this.globalTable = globalTable;
    }

    /**
     * Sets branch table.
     *
     * @param branchTable the branch table
     */
    public void setBranchTable(String branchTable) {
        this.branchTable = branchTable;
    }

    /**
     * Sets db type.
     *
     * @param dbType the db type
     */
    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    /**
     * Gets transaction name column size.
     *
     * @return the transaction name column size
     */
    public int getTransactionNameColumnSize() {
        return transactionNameColumnSize;
    }

    //endregion


    /**
     * column info
     */
    private static class ColumnInfo {
        private String columnName;
        private String typeName;
        private int columnSize;
        private String remarks;

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public int getColumnSize() {
            return columnSize;
        }

        public void setColumnSize(int columnSize) {
            this.columnSize = columnSize;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }
}
