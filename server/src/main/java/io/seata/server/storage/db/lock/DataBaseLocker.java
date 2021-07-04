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
package io.seata.server.storage.db.lock;

import java.util.List;
import javax.sql.DataSource;
import io.seata.common.exception.DataAccessException;
import io.seata.common.exception.StoreException;
import io.seata.common.util.CollectionUtils;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.model.LockStatus;
import io.seata.core.store.LockStore;

/**
 * The type Data base locker.
 *
 * @author zhangsen
 */
public class DataBaseLocker extends AbstractLocker {

    private LockStore lockStore;

    /**
     * Instantiates a new Data base locker.
     */
    public DataBaseLocker() {
    }

    /**
     * Instantiates a new Data base locker.
     *
     * @param logStoreDataSource the log store data source
     */
    public DataBaseLocker(DataSource logStoreDataSource) {
        lockStore = new LockStoreDataBaseDAO(logStoreDataSource);
    }

    @Override
    public boolean acquireLock(List<RowLock> locks) {
        return acquireLock(locks, true);
    }

    @Override
    public boolean acquireLock(List<RowLock> locks, boolean autoCommit) {
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
            return true;
        }
        try {
            return lockStore.acquireLock(convertToLockDO(locks), autoCommit);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("AcquireLock error, locks:{}", CollectionUtils.toString(locks), t);
            return false;
        }
    }

    @Override
    public boolean releaseLock(List<RowLock> locks) {
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
            return true;
        }
        try {
            return lockStore.unLock(convertToLockDO(locks));
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock error, locks:{}", CollectionUtils.toString(locks), t);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        try {
            return lockStore.unLock(xid, branchId);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock by branchId error, xid {}, branchId:{}", xid, branchId, t);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String xid, List<Long> branchIds) {
        if (CollectionUtils.isEmpty(branchIds)) {
            // no lock
            return true;
        }
        try {
            return lockStore.unLock(xid, branchIds);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock by branchIds error, xid {}, branchIds:{}", xid, CollectionUtils.toString(branchIds), t);
            return false;
        }
    }

    @Override
    public boolean isLockable(List<RowLock> locks) {
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
            return true;
        }
        try {
            return lockStore.isLockable(convertToLockDO(locks));
        } catch (DataAccessException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("isLockable error, locks:{}", CollectionUtils.toString(locks), t);
            return false;
        }
    }

    @Override
    public boolean updateLockStatus(String xid, LockStatus lockStatus) {
        return lockStore.updateLockStatus(xid, lockStatus);
    }

    /**
     * Sets lock store.
     *
     * @param lockStore the lock store
     */
    public void setLockStore(LockStore lockStore) {
        this.lockStore = lockStore;
    }

}
