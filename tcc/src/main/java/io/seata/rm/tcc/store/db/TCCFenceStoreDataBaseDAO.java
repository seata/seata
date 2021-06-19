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
package io.seata.rm.tcc.store.db;

import io.seata.common.exception.DataAccessException;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.StoreException;
import io.seata.common.util.IOUtil;
import io.seata.rm.tcc.constant.TCCFenceConstant;
import io.seata.rm.tcc.exception.TCCFenceException;
import io.seata.rm.tcc.store.TCCFenceDO;
import io.seata.rm.tcc.store.TCCFenceStore;
import io.seata.rm.tcc.store.db.sql.TCCFenceStoreSqls;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * The type TCC Fence store data base dao
 *
 * @author kaka2code
 */
public class TCCFenceStoreDataBaseDAO implements TCCFenceStore {

    private static volatile TCCFenceStoreDataBaseDAO instance = null;
    /**
     * TCC fence log table name
     */
    private String logTableName = TCCFenceConstant.DEFAULT_LOG_TABLE_NAME;

    private TCCFenceStoreDataBaseDAO() {}

    public static TCCFenceStore getInstance() {
        if (instance == null) {
            synchronized (TCCFenceStore.class) {
                if (instance == null) {
                    instance = new TCCFenceStoreDataBaseDAO();
                }
            }
        }
        return instance;
    }

    @Override
    public TCCFenceDO queryTCCFenceDO(Connection conn, String xid, Long branchId) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = TCCFenceStoreSqls.getQuerySQLByBranchIdAndXid(logTableName);
            ps = conn.prepareStatement(sql);
            ps.setString(1, xid);
            ps.setLong(2, branchId);
            rs = ps.executeQuery();
            if (rs.next()) {
                TCCFenceDO tccFenceDO = new TCCFenceDO();
                tccFenceDO.setXid(rs.getString("xid"));
                tccFenceDO.setBranchId(rs.getLong("branch_id"));
                tccFenceDO.setStatus(rs.getInt("status"));
                return tccFenceDO;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps);
        }
    }

    @Override
    public boolean insertTCCFenceDO(Connection conn, TCCFenceDO tccFenceDO) {
        PreparedStatement ps = null;
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());

            String sql = TCCFenceStoreSqls.getInsertLocalTCCLogSQL(logTableName);
            ps = conn.prepareStatement(sql);
            ps.setString(1, tccFenceDO.getXid());
            ps.setLong(2, tccFenceDO.getBranchId());
            ps.setInt(3, tccFenceDO.getStatus());
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            return ps.executeUpdate() > 0;
        } catch (DuplicateKeyException e) {
            throw new TCCFenceException(String.format("Insert tcc fence record duplicate key exception. xid= %s, branchId= %s", tccFenceDO.getXid(), tccFenceDO.getBranchId()),
                    FrameworkErrorCode.DuplicateKeyException);
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps);
        }
    }

    @Override
    public boolean updateTCCFenceDO(Connection conn, String xid, Long branchId, int newStatus, int oldStatus) {
        PreparedStatement ps = null;
        try {
            String sql = TCCFenceStoreSqls.getUpdateStatusSQLByBranchIdAndXid(logTableName);
            ps = conn.prepareStatement(sql);
            ps.setInt(1, newStatus);
            // gmt_modified
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setString(3, xid);
            ps.setLong(4, branchId);
            ps.setInt(5, oldStatus);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps);
        }
    }

    @Override
    public boolean deleteTCCFenceDO(Connection conn, String xid, Long branchId) {
        PreparedStatement ps = null;
        try {
            String sql = TCCFenceStoreSqls.getDeleteSQLByBranchIdAndXid(logTableName);
            ps = conn.prepareStatement(sql);
            ps.setString(1, xid);
            ps.setLong(2, branchId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps);
        }
    }

    @Override
    public int deleteTCCFenceDOByDate(Connection conn, Date datetime) {
        PreparedStatement ps = null;
        try {
            String sql = TCCFenceStoreSqls.getDeleteSQLByDateAndStatus(logTableName);
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, new Timestamp(datetime.getTime()));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps);
        }
    }

    @Override
    public void setLogTableName(String logTableName) {
        this.logTableName = logTableName;
    }
}
