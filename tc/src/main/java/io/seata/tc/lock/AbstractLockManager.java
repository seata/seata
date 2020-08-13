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
package io.seata.tc.lock;

import java.util.ArrayList;
import java.util.List;

import io.seata.common.XID;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.core.lock.RowLock;
import io.seata.tc.session.BranchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract lock manager.
 *
 * @author zhangsen
 */
public abstract class AbstractLockManager implements LockManager {

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractLockManager.class);

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        if (branchSession == null) {
            throw new IllegalArgumentException("branchSession can't be null for memory/file locker.");
        }
        String lockKey = branchSession.getLockKey();
        if (StringUtils.isNullOrEmpty(lockKey)) {
            // no lock
            return true;
        }
        // get locks of branch
        List<RowLock> locks = collectRowLocks(branchSession);
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
            return true;
        }
        return getLocker(branchSession).acquireLock(locks);
    }

    @Override
    public boolean releaseLock(BranchSession branchSession) throws TransactionException {
        if (branchSession == null) {
            throw new IllegalArgumentException("branchSession can't be null for memory/file locker.");
        }
        List<RowLock> locks = collectRowLocks(branchSession);
        try {
            return getLocker(branchSession).releaseLock(locks);
        } catch (Exception t) {
            LOGGER.error("unLock error, branchSession:{}", branchSession, t);
            return false;
        }
    }

    @Override
    public boolean isLockable(String xid, String resourceId, String lockKey) throws TransactionException {
        if (StringUtils.isBlank(lockKey)) {
            // no lock
            return true;
        }
        List<RowLock> locks = collectRowLocks(lockKey, resourceId, xid);
        try {
            return getLocker().isLockable(locks);
        } catch (Exception t) {
            LOGGER.error("isLockable error, xid:{} resourceId:{}, lockKey:{}", xid, resourceId, lockKey, t);
            return false;
        }
    }


    @Override
    public void cleanAllLocks() throws TransactionException {
        getLocker().cleanAllLocks();
    }

    /**
     * Gets locker.
     *
     * @return the locker
     */
    protected Locker getLocker() {
        return getLocker(null);
    }

    /**
     * Gets locker.
     *
     * @param branchSession the branch session
     * @return the locker
     */
    protected abstract Locker getLocker(BranchSession branchSession);

    /**
     * Collect row locks list.`
     *
     * @param branchSession the branch session
     * @return the list
     */
    protected List<RowLock> collectRowLocks(BranchSession branchSession) {
        List<RowLock> locks = new ArrayList<>();
        if (branchSession == null || StringUtils.isBlank(branchSession.getLockKey())) {
            return locks;
        }
        String xid = branchSession.getXid();
        String resourceId = branchSession.getResourceId();
        long transactionId = branchSession.getTransactionId();

        String lockKey = branchSession.getLockKey();

        return collectRowLocks(lockKey, resourceId, xid, transactionId, branchSession.getBranchId());
    }

    /**
     * Collect row locks list.
     *
     * @param lockKey    the lock key
     * @param resourceId the resource id
     * @param xid        the xid
     * @return the list
     */
    protected List<RowLock> collectRowLocks(String lockKey, String resourceId, String xid) {
        return collectRowLocks(lockKey, resourceId, xid, XID.getTransactionId(xid), null);
    }

    /**
     * Collect row locks list.
     *
     * @param lockKey       the lock key
     * @param resourceId    the resource id
     * @param xid           the xid
     * @param transactionId the transaction id
     * @param branchID      the branch id
     * @return the list
     */
    protected List<RowLock> collectRowLocks(String lockKey, String resourceId, String xid, Long transactionId,
                                            Long branchID) {
        List<RowLock> locks = new ArrayList<RowLock>();

        String[] tableGroupedLockKeys = lockKey.split(";");
        for (String tableGroupedLockKey : tableGroupedLockKeys) {
            int idx = tableGroupedLockKey.indexOf(":");
            if (idx < 0) {
                return locks;
            }
            String tableName = tableGroupedLockKey.substring(0, idx);
            String mergedPKs = tableGroupedLockKey.substring(idx + 1);
            if (StringUtils.isBlank(mergedPKs)) {
                return locks;
            }
            String[] pks = mergedPKs.split(",");
            if (pks == null || pks.length == 0) {
                return locks;
            }
            for (String pk : pks) {
                if (StringUtils.isNotBlank(pk)) {
                    RowLock rowLock = new RowLock();
                    rowLock.setXid(xid);
                    rowLock.setTransactionId(transactionId);
                    rowLock.setBranchId(branchID);
                    rowLock.setTableName(tableName);
                    rowLock.setPk(pk);
                    rowLock.setResourceId(resourceId);
                    locks.add(rowLock);
                }
            }
        }
        return locks;
    }

}
