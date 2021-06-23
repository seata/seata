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
package io.seata.server.storage.db.distributed.lock;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;

import com.alibaba.druid.util.JdbcUtils;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLockStore;
import io.seata.core.store.db.sql.distribute.lock.DistributeLockSqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.DefaultValues.DEFAULT_DISTRIBUTE_LOCK_DB_TABLE;
import static io.seata.common.DefaultValues.DEFAULT_DISTRIBUTE_LOCK_EXPIRE;
import static io.seata.core.constants.ConfigurationKeys.DISTRIBUTED_LOCK_EXPIRE_TIME;
import static io.seata.core.constants.ConfigurationKeys.DISTRIBUTE_LOCK_DB_TABLE;

/**
 * @author chd
 */
public class DistributedLockStoreDAO implements DistributedLockStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockStoreDAO.class);

    private final DataSource distributedLockDataSource;

    private final String distributeLockTable;

    private final Long expireTime;

    private final String dbType;

    /**
     * Instantiates a new Log store data base dao.
     *
     * @param distributedLockDataSource the distribute lock store data source
     */
    public DistributedLockStoreDAO(DataSource distributedLockDataSource) {
        Configuration configuration = ConfigurationFactory.getInstance();
        distributeLockTable = configuration.getConfig(DISTRIBUTE_LOCK_DB_TABLE, DEFAULT_DISTRIBUTE_LOCK_DB_TABLE);
        expireTime = configuration.getLong(DISTRIBUTED_LOCK_EXPIRE_TIME, DEFAULT_DISTRIBUTE_LOCK_EXPIRE);
        dbType = configuration.getConfig(ConfigurationKeys.STORE_DB_TYPE);

        this.distributedLockDataSource = distributedLockDataSource;
    }


    @Override
    public boolean acquireLock(DistributedLockDO distributedLockDO) {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = distributedLockDataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            DistributedLockDO distributedLockDOFromDB = getDistributeLockDO(connection, distributedLockDO.getLockKey());
            if (null == distributedLockDOFromDB) {
                boolean ret = insertDistribute(connection, distributedLockDO);
                connection.commit();
                return ret;
            }

            if (distributedLockDOFromDB.getExpireTime() >= System.currentTimeMillis()) {
                LOGGER.info("the distribute lock for key :{} is holding by :{}, acquire lock failure.",
                        distributedLockDO.getLockKey(), distributedLockDOFromDB.getLockValue());
                connection.commit();
                return false;
            }

            boolean ret = updateDistributeLock(connection, distributedLockDO);
            connection.commit();

            return ret;
        } catch (SQLException ex) {
            LOGGER.error("execute acquire lock failure, key is: {}", distributedLockDO.getLockKey(), ex);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e) {
                LOGGER.warn("rollback fail because of {}", e.getMessage(), e);
            }
            return false;
        } finally {
            try {
                if (originalAutoCommit) {
                    connection.setAutoCommit(true);
                }
                JdbcUtils.close(connection);
            } catch (SQLException ignore) { }
        }
    }

    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = distributedLockDataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            DistributedLockDO distributedLockDOFromDB = getDistributeLockDO(connection, distributedLockDO.getLockKey());
            if (null == distributedLockDOFromDB) {
                throw new ShouldNeverHappenException("distributeLockDO would not be null when release distribute lock");
            }

            if (distributedLockDOFromDB.getExpireTime() >= System.currentTimeMillis()
                    && !Objects.equals(distributedLockDOFromDB.getLockValue(), distributedLockDO.getLockValue())) {
                LOGGER.warn("the distribute lock for key :{} is holding by :{}, skip the release lock.",
                        distributedLockDO.getLockKey(), distributedLockDOFromDB.getLockValue());
                connection.commit();
                return true;
            }
            distributedLockDO.setLockValue(StringUtils.SPACE);
            boolean ret = updateDistributeLock(connection, distributedLockDO);

            connection.commit();
            return ret;
        } catch (SQLException ex) {
            LOGGER.error("execute release lock failure, key is: {}", distributedLockDO.getLockKey(), ex);

            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e) {
                LOGGER.warn("rollback fail because of {}", e.getMessage(), e);
            }
            return false;
        } finally {
            try {
                if (originalAutoCommit) {
                    connection.setAutoCommit(true);
                }
                JdbcUtils.close(connection);
            } catch (SQLException ignore) { }
        }
    }

    protected DistributedLockDO getDistributeLockDO(Connection connection, String key) throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(DistributeLockSqlFactory.getDistributeLogStoreSql(dbType)
                .getSelectDistributeForUpdateSql(distributeLockTable))) {

            pst.setString(1, key);
            ResultSet resultSet = pst.executeQuery();

            while (resultSet.next()) {
                DistributedLockDO distributedLock = new DistributedLockDO();
                distributedLock.setExpireTime(resultSet.getLong(ServerTableColumnsName.DISTRIBUTE_LOCK_EXPIRE));
                distributedLock.setLockValue(resultSet.getString(ServerTableColumnsName.DISTRIBUTE_LOCK_VALUE));
                distributedLock.setLockKey(key);
                return distributedLock;
            }
            return null;
        }
    }

    protected boolean insertDistribute(Connection connection, DistributedLockDO distributedLockDO) throws SQLException {
        try (PreparedStatement insertPst = connection.prepareStatement(DistributeLockSqlFactory.getDistributeLogStoreSql(dbType)
                .getInsertSql(distributeLockTable))) {
            insertPst.setString(1, distributedLockDO.getLockKey());
            insertPst.setString(2, distributedLockDO.getLockValue());
            insertPst.setLong(3, distributedLockDO.getExpireTime());
            return insertPst.executeUpdate() > 0;
        }
    }

    protected boolean updateDistributeLock(Connection connection, DistributedLockDO distributedLockDO) throws SQLException {
        try (PreparedStatement updatePst = connection.prepareStatement(DistributeLockSqlFactory.getDistributeLogStoreSql(dbType)
                .getUpdateSql(distributeLockTable))) {
            updatePst.setString(1, distributedLockDO.getLockValue());
            updatePst.setLong(2, distributedLockDO.getExpireTime());
            updatePst.setString(3, distributedLockDO.getLockKey());
            return updatePst.executeUpdate() > 0;
        }
    }
}
