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
package io.seata.server.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.seata.core.store.BranchTransactionDO;
import io.seata.server.storage.file.lock.FileLocker;
import io.seata.common.util.CompressUtil;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.server.lock.LockerManagerFactory;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Branch session.
 *
 * @author sharajava
 */
public class BranchSession extends BranchTransactionDO
        implements Lockable, Comparable<BranchSession>, SessionStorable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BranchSession.class);

    private static final int MAX_BRANCH_SESSION_SIZE = StoreConfig.getMaxBranchSessionSize();

    private static ThreadLocal<ByteBuffer> byteBufferThreadLocal = ThreadLocal.withInitial(() -> ByteBuffer.allocate(
        MAX_BRANCH_SESSION_SIZE));

    private String lockKey;

    private ConcurrentMap<FileLocker.BucketLockMap, Set<String>> lockHolder
        = new ConcurrentHashMap<>();

    /**
     * Gets lock key.
     *
     * @return the lock key
     */
    public String getLockKey() {
        return lockKey;
    }

    /**
     * Sets lock key.
     *
     * @param lockKey the lock key
     */
    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    @Override
    public String toString() {
        return "BR:" + branchId + "/" + transactionId;
    }

    @Override
    public int compareTo(BranchSession o) {
        return Long.compare(this.branchId, o.branchId);
    }

    public boolean canBeCommittedAsync() {
        return branchType == BranchType.AT || status == BranchStatus.PhaseOne_Failed;
    }

    /**
     * Gets lock holder.
     *
     * @return the lock holder
     */
    public ConcurrentMap<FileLocker.BucketLockMap, Set<String>> getLockHolder() {
        return lockHolder;
    }

    @Override
    public boolean lock() throws TransactionException {
        if (this.getBranchType().equals(BranchType.AT)) {
            return LockerManagerFactory.getLockManager().acquireLock(this);
        }
        return true;
    }

    @Override
    public boolean unlock() throws TransactionException {
        if (this.getBranchType() == BranchType.AT) {
            return LockerManagerFactory.getLockManager().releaseLock(this);
        }
        return true;
    }

    @Override
    public byte[] encode() {

        byte[] resourceIdBytes = resourceId != null ? resourceId.getBytes() : null;

        byte[] lockKeyBytes = lockKey != null ? lockKey.getBytes() : null;

        byte[] clientIdBytes = clientId != null ? clientId.getBytes() : null;

        byte[] applicationDataBytes = applicationData != null ? applicationData.getBytes() : null;

        byte[] xidBytes = xid != null ? xid.getBytes() : null;

        byte branchTypeByte = branchType != null ? (byte) branchType.ordinal() : -1;

        int size = calBranchSessionSize(resourceIdBytes, lockKeyBytes, clientIdBytes, applicationDataBytes, xidBytes);

        if (size > MAX_BRANCH_SESSION_SIZE) {
            if (lockKeyBytes == null) {
                throw new RuntimeException("branch session size exceeded, size : " + size + " maxBranchSessionSize : "
                    + MAX_BRANCH_SESSION_SIZE);
            }
            // try compress lockkey
            try {
                size -= lockKeyBytes.length;
                lockKeyBytes = CompressUtil.compress(lockKeyBytes);
            } catch (IOException e) {
                LOGGER.error("compress lockKey error", e);
            } finally {
                size += lockKeyBytes.length;
            }

            if (size > MAX_BRANCH_SESSION_SIZE) {
                throw new RuntimeException(
                    "compress branch session size exceeded, compressSize : " + size + " maxBranchSessionSize : "
                        + MAX_BRANCH_SESSION_SIZE);
            }
        }

        ByteBuffer byteBuffer = byteBufferThreadLocal.get();
        //recycle
        byteBuffer.clear();

        byteBuffer.putLong(transactionId);
        byteBuffer.putLong(branchId);

        if (resourceIdBytes != null) {
            byteBuffer.putShort((short)resourceIdBytes.length);
            byteBuffer.put(resourceIdBytes);
        } else {
            byteBuffer.putShort((short)0);
        }

        if (lockKeyBytes != null) {
            byteBuffer.putShort((short)lockKeyBytes.length);
            byteBuffer.put(lockKeyBytes);
        } else {
            byteBuffer.putShort((short)0);
        }

        if (clientIdBytes != null) {
            byteBuffer.putShort((short)clientIdBytes.length);
            byteBuffer.put(clientIdBytes);
        } else {
            byteBuffer.putShort((short)0);
        }

        if (applicationDataBytes != null) {
            byteBuffer.putInt(applicationDataBytes.length);
            byteBuffer.put(applicationDataBytes);
        } else {
            byteBuffer.putInt(0);
        }

        if (xidBytes != null) {
            byteBuffer.putShort((short)xidBytes.length);
            byteBuffer.put(xidBytes);
        } else {
            byteBuffer.putShort((short)0);
        }

        byteBuffer.put(branchTypeByte);

        byteBuffer.put((byte)status.getCode());
        byteBuffer.flip();
        byte[] result = new byte[byteBuffer.limit()];
        byteBuffer.get(result);
        return result;
    }

    private int calBranchSessionSize(byte[] resourceIdBytes, byte[] lockKeyBytes, byte[] clientIdBytes,
                                     byte[] applicationDataBytes, byte[] xidBytes) {
        final int size = 8 // trascationId
            + 8 // branchId
            + 2 // resourceIdBytes.length
            + 2 // lockKeyBytes.length
            + 2 // clientIdBytes.length
            + 4 // applicationDataBytes.length
            + 2 // xidBytes.length
            + 1 // statusCode
            + (resourceIdBytes == null ? 0 : resourceIdBytes.length)
            + (lockKeyBytes == null ? 0 : lockKeyBytes.length)
            + (clientIdBytes == null ? 0 : clientIdBytes.length)
            + (applicationDataBytes == null ? 0 : applicationDataBytes.length)
            + (xidBytes == null ? 0 : xidBytes.length)
            + 1; //branchType
        return size;
    }

    @Override
    public void decode(byte[] a) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(a);
        this.transactionId = byteBuffer.getLong();
        this.branchId = byteBuffer.getLong();
        short resourceLen = byteBuffer.getShort();
        if (resourceLen > 0) {
            byte[] byResource = new byte[resourceLen];
            byteBuffer.get(byResource);
            this.resourceId = new String(byResource);
        }
        short lockKeyLen = byteBuffer.getShort();
        if (lockKeyLen > 0) {
            byte[] byLockKey = new byte[lockKeyLen];
            byteBuffer.get(byLockKey);
            if (CompressUtil.isCompressData(byLockKey)) {
                try {
                    this.lockKey = new String(CompressUtil.uncompress(byLockKey));
                } catch (IOException e) {
                    throw new RuntimeException("decompress lockKey error", e);
                }
            } else {
                this.lockKey = new String(byLockKey);
            }

        }
        short clientIdLen = byteBuffer.getShort();
        if (clientIdLen > 0) {
            byte[] byClientId = new byte[clientIdLen];
            byteBuffer.get(byClientId);
            this.clientId = new String(byClientId);
        }
        int applicationDataLen = byteBuffer.getInt();
        if (applicationDataLen > 0) {
            byte[] byApplicationData = new byte[applicationDataLen];
            byteBuffer.get(byApplicationData);
            this.applicationData = new String(byApplicationData);
        }
        short xidLen = byteBuffer.getShort();
        if (xidLen > 0) {
            byte[] xidBytes = new byte[xidLen];
            byteBuffer.get(xidBytes);
            this.xid = new String(xidBytes);
        }
        int branchTypeId = byteBuffer.get();
        if (branchTypeId >= 0) {
            this.branchType = BranchType.values()[branchTypeId];
        }
        this.status = BranchStatus.get(byteBuffer.get());

    }

}
