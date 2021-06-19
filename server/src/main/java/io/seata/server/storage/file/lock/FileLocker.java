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
package io.seata.server.storage.file.lock;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.util.internal.ConcurrentSet;
import io.seata.common.exception.FrameworkException;
import io.seata.common.util.CollectionUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.server.session.BranchSession;

/**
 * The type Memory locker.
 *
 * @author zhangsen
 */
public class FileLocker extends AbstractLocker {

    private static final int BUCKET_PER_TABLE = 128;

    private static final ConcurrentMap<String/* resourceId */, ConcurrentMap<String/* tableName */,
        ConcurrentMap<Integer/* bucketId */, BucketLockMap>>>
        LOCK_MAP = new ConcurrentHashMap<>();

    /**
     * The Branch session.
     */
    protected BranchSession branchSession = null;

    /**
     * Instantiates a new Memory locker.
     *
     * @param branchSession the branch session
     */
    public FileLocker(BranchSession branchSession) {
        this.branchSession = branchSession;
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            //no lock
            return true;
        }
        String resourceId = branchSession.getResourceId();
        long transactionId = branchSession.getTransactionId();

        ConcurrentMap<BucketLockMap, Set<String>> bucketHolder = branchSession.getLockHolder();
        ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>> dbLockMap = CollectionUtils.computeIfAbsent(
            LOCK_MAP, resourceId, key -> new ConcurrentHashMap<>());

        for (RowLock lock : rowLocks) {
            String tableName = lock.getTableName();
            String pk = lock.getPk();
            ConcurrentMap<Integer, BucketLockMap> tableLockMap = CollectionUtils.computeIfAbsent(dbLockMap, tableName,
                key -> new ConcurrentHashMap<>());

            int bucketId = pk.hashCode() % BUCKET_PER_TABLE;
            BucketLockMap bucketLockMap = CollectionUtils.computeIfAbsent(tableLockMap, bucketId,
                key -> new BucketLockMap());
            Long previousLockTransactionId = bucketLockMap.get().putIfAbsent(pk, transactionId);
            if (previousLockTransactionId == null) {
                //No existing lock, and now locked by myself
                Set<String> keysInHolder = CollectionUtils.computeIfAbsent(bucketHolder, bucketLockMap,
                    key -> new ConcurrentSet<>());
                keysInHolder.add(pk);
            } else if (previousLockTransactionId == transactionId) {
                // Locked by me before
                continue;
            } else {
                LOGGER.info("Global lock on [" + tableName + ":" + pk + "] is holding by " + previousLockTransactionId);
                try {
                    // Release all acquired locks.
                    branchSession.unlock();
                } catch (TransactionException e) {
                    throw new FrameworkException(e);
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean releaseLock(List<RowLock> rowLock) {
        if (CollectionUtils.isEmpty(rowLock)) {
            //no lock
            return true;
        }
        ConcurrentMap<BucketLockMap, Set<String>> lockHolder = branchSession.getLockHolder();
        if (CollectionUtils.isEmpty(lockHolder)) {
            return true;
        }
        for (Map.Entry<BucketLockMap, Set<String>> entry : lockHolder.entrySet()) {
            BucketLockMap bucket = entry.getKey();
            Set<String> keys = entry.getValue();
            for (String key : keys) {
                // remove lock only if it locked by myself
                bucket.get().remove(key, branchSession.getTransactionId());
            }
        }
        lockHolder.clear();
        return true;
    }

    @Override
    public boolean isLockable(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            //no lock
            return true;
        }
        Long transactionId = rowLocks.get(0).getTransactionId();
        String resourceId = rowLocks.get(0).getResourceId();
        ConcurrentMap<String, ConcurrentMap<Integer, BucketLockMap>> dbLockMap = LOCK_MAP.get(resourceId);
        if (dbLockMap == null) {
            return true;
        }
        for (RowLock rowLock : rowLocks) {
            String tableName = rowLock.getTableName();
            String pk = rowLock.getPk();

            ConcurrentMap<Integer, BucketLockMap> tableLockMap = dbLockMap.get(tableName);
            if (tableLockMap == null) {
                continue;
            }
            int bucketId = pk.hashCode() % BUCKET_PER_TABLE;
            BucketLockMap bucketLockMap = tableLockMap.get(bucketId);
            if (bucketLockMap == null) {
                continue;
            }
            Long lockingTransactionId = bucketLockMap.get().get(pk);
            if (lockingTransactionId == null || lockingTransactionId.longValue() == transactionId) {
                // Locked by me
                continue;
            } else {
                LOGGER.info("Global lock on [" + tableName + ":" + pk + "] is holding by " + lockingTransactionId);
                return false;
            }
        }
        return true;
    }

    @Override
    public void cleanAllLocks() {
        LOCK_MAP.clear();
    }

    /**
     * Because bucket lock map will be key of HashMap(lockHolder), however {@link ConcurrentHashMap} overwrites
     * {@link Object##hashCode()} and {@link Object##equals(Object)}, that leads to hash key conflict in lockHolder.
     * We define a {@link BucketLockMap} to hold the ConcurrentHashMap(bucketLockMap) and replace it as key of
     * HashMap(lockHolder).
     */
    public static class BucketLockMap {
        private final ConcurrentHashMap<String/* pk */, Long/* transactionId */> bucketLockMap
            = new ConcurrentHashMap<>();

        public ConcurrentHashMap<String, Long> get() {
            return bucketLockMap;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }
    }
}
