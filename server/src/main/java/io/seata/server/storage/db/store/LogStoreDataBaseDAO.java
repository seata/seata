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
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.core.store.LogStore;
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
public class LogStoreDataBaseDAO implements LogStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogStoreDataBaseDAO.class);

    /**
     * The transaction name key
     */
    private static final String TRANSACTION_NAME_KEY = "TRANSACTION_NAME";
    /**
     * The transaction name default size is 128
     */
    private static final int TRANSACTION_NAME_DEFAULT_SIZE = 128;

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The Log store data source.
     */
    protected DataSource logStoreDataSource = null;

    /**
     * The Global table.
     */
    protected String globalTable;

    /**
     * The Branch table.
     */
    protected String branchTable;

    private String dbType;

    private int transactionNameColumnSize = TRANSACTION_NAME_DEFAULT_SIZE;

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
    public List<GlobalTransactionDO> queryGlobalTransactionDO(int[] statuses, int limit) {
        List<GlobalTransactionDO> ret = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);

            String paramsPlaceHolder = org.apache.commons.lang.StringUtils.repeat("?", ",", statuses.length);

            String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getQueryGlobalTransactionSQLByStatus(globalTable, paramsPlaceHolder);
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < statuses.length; i++) {
                int status = statuses[i];
                ps.setInt(i + 1, status);
            }
            ps.setInt(statuses.length + 1, limit);
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
    public boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getInsertGlobalTransactionSQL(globalTable);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            int index = 1;
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(index++, globalTransactionDO.getXid());
            ps.setLong(index++, globalTransactionDO.getTransactionId());
            ps.setInt(index++, globalTransactionDO.getStatus());
            ps.setString(index++, globalTransactionDO.getApplicationId());
            ps.setString(index++, globalTransactionDO.getTransactionServiceGroup());
            String transactionName = globalTransactionDO.getTransactionName();
            transactionName = transactionName.length() > transactionNameColumnSize ?
                transactionName.substring(0, transactionNameColumnSize) :
                transactionName;
            ps.setString(index++, transactionName);
            ps.setInt(index++, globalTransactionDO.getTimeout());
            ps.setLong(index++, globalTransactionDO.getBeginTime());
            ps.setString(index++, globalTransactionDO.getApplicationData());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
    }

    @Override
    public boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String sql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getUpdateGlobalTransactionStatusSQL(globalTable);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            int index = 1;
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setInt(index++, globalTransactionDO.getStatus());
            ps.setString(index++, globalTransactionDO.getXid());
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
            int index = 1;
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(index++, branchTransactionDO.getXid());
            ps.setLong(index++, branchTransactionDO.getTransactionId());
            ps.setLong(index++, branchTransactionDO.getBranchId());
            ps.setString(index++, branchTransactionDO.getResourceGroupId());
            ps.setString(index++, branchTransactionDO.getResourceId());
            ps.setString(index++, branchTransactionDO.getBranchType());
            ps.setInt(index++, branchTransactionDO.getStatus());
            ps.setString(index++, branchTransactionDO.getClientId());
            ps.setString(index++, branchTransactionDO.getApplicationData());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
    }

    @Override
    public boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        boolean shouldUpdateAppData = StringUtils.isNotBlank(branchTransactionDO.getApplicationData());
        String sql = shouldUpdateAppData ?
            LogStoreSqlsFactory.getLogStoreSqls(dbType).getUpdateBranchTransactionStatusAppDataSQL(branchTable) :
            LogStoreSqlsFactory.getLogStoreSqls(dbType).getUpdateBranchTransactionStatusSQL(branchTable);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            int index = 1;
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setInt(index++, branchTransactionDO.getStatus());
            if (shouldUpdateAppData) {
                ps.setString(index++, branchTransactionDO.getApplicationData());
            }
            ps.setString(index++, branchTransactionDO.getXid());
            ps.setLong(index++, branchTransactionDO.getBranchId());
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
        return Math.max(maxBranchId, maxTransId);
    }

    private long getCurrentMaxSessionId(String sql, long high, long low) {
        long max = 0;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            int index = 1;
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setLong(index++, high);
            ps.setLong(index++, low);

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
                    ResultSet columnRs = conn.getMetaData().getColumns(null, schema, table, null);
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
        } else if ("postgresql".equalsIgnoreCase(dbType)) {
            String sql = "select current_schema";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
                String schema = null;
                if (rs.next()) {
                    schema = rs.getString(1);
                }
                return schema;
            } catch (SQLException e) {
                throw new StoreException(e);
            }
        } else {
            return conn.getMetaData().getUserName();
        }
    }

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

    public int getTransactionNameColumnSize() {
        return transactionNameColumnSize;
    }

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
