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

import io.seata.common.util.CompressUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.RetryStrategy;
import io.seata.core.model.RetryStrategyInfo;
import io.seata.server.lock.LockerManagerFactory;
import io.seata.server.storage.file.lock.FileLocker;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.DefaultValues.DEFAULT_RETRY_STRATEGY;

/**
 * The type Branch session.
 *
 * @author sharajava
 */
public class BranchSession implements Lockable, Comparable<BranchSession>, SessionStorable, RetryStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(BranchSession.class);

    private static final int MAX_BRANCH_SESSION_SIZE = StoreConfig.getMaxBranchSessionSize();

    private static ThreadLocal<ByteBuffer> byteBufferThreadLocal = ThreadLocal.withInitial(() -> ByteBuffer.allocate(
        MAX_BRANCH_SESSION_SIZE));

    public static final String RETRY_STRATEGY = ConfigurationFactory.getInstance().getConfig(
            ConfigurationKeys.SERVER_RETRY_STRATEGY, DEFAULT_RETRY_STRATEGY);

    private String xid;

    private long transactionId;

    private long branchId;

    private String resourceGroupId;

    private String resourceId;

    private String lockKey;

    private BranchType branchType;

    private BranchStatus status = BranchStatus.Unknown;

    private String retryStrategy;

    private RetryStrategyInfo retryStrategyInfo;

    private int retryCount;

    private String clientId;

    private String applicationData;

    private ConcurrentMap<FileLocker.BucketLockMap, Set<String>> lockHolder
        = new ConcurrentHashMap<>();

    /**
     * Gets application data.
     *
     * @return the application data
     */
    public String getApplicationData() {
        return applicationData;
    }

    /**
     * Sets application data.
     *
     * @param applicationData the application data
     */
    public void setApplicationData(String applicationData) {
        this.applicationData = applicationData;
    }

    /**
     * Gets resource group id.
     *
     * @return the resource group id
     */
    public String getResourceGroupId() {
        return resourceGroupId;
    }

    /**
     * Sets resource group id.
     *
     * @param resourceGroupId the resource group id
     */
    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    /**
     * Gets client id.
     *
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets client id.
     *
     * @param clientId the client id
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets resource id.
     *
     * @return the resource id
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Sets resource id.
     *
     * @param resourceId the resource id
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

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

    /**
     * Gets branch type.
     *
     * @return the branch type
     */
    public BranchType getBranchType() {
        return branchType;
    }

    /**
     * Sets branch type.
     *
     * @param branchType the branch type
     */
    public void setBranchType(BranchType branchType) {
        this.branchType = branchType;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public BranchStatus getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(BranchStatus status) {
        this.status = status;
    }

    /**
     * Gets retry strategy.
     *
     * @return the retry strategy
     */
    public String getRetryStrategy() {
        return retryStrategy;
    }

    /**
     * Sets retry strategy.
     *
     * @param retryStrategy the retry strategy
     */
    public void setRetryStrategy(String retryStrategy) {
        if (StringUtils.equals(this.retryStrategy, retryStrategy)) {
            return;
        }
        this.retryStrategy = retryStrategy;
        this.retryStrategyInfo = null;
    }

    /**
     * Gets retry strategy info.
     *
     * @return the retry strategy info
     */
    public RetryStrategyInfo getRetryStrategyInfo() {
        if (retryStrategyInfo == null) {
            String retryStrategy = this.retryStrategy;
            if (StringUtils.isBlank(retryStrategy)) {
                retryStrategy = RETRY_STRATEGY;
            }
            retryStrategyInfo = new RetryStrategyInfo(retryStrategy);
        }
        return retryStrategyInfo;
    }

    /**
     * Sets retry strategy info.
     *
     * @param retryStrategyInfo the retry strategy info
     */
    public void setRetryStrategyInfo(RetryStrategyInfo retryStrategyInfo) {
        this.retryStrategyInfo = retryStrategyInfo;
        if (retryStrategyInfo != null) {
            this.retryStrategy = retryStrategyInfo.toString();
        }
    }

    /**
     * Gets retry count.
     *
     * @return the retry count
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * Sets retry count.
     *
     * @param retryCount the retry count
     */
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * Gets transaction id.
     *
     * @return the transaction id
     */
    public long getTransactionId() {
        return transactionId;
    }

    /**
     * Sets transaction id.
     *
     * @param transactionId the transaction id
     */
    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public long getBranchId() {
        return branchId;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public String getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(String xid) {
        this.xid = xid;
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

    /**
     * is expired
     *
     * @param globalTransactionBeginTime the global transaction begin time
     * @return the boolean
     */
    @Override
    public boolean isExpired(long globalTransactionBeginTime) {
        try {
            RetryStrategyInfo retryStrategy = getRetryStrategyInfo();
            return retryStrategy.isExpired(globalTransactionBeginTime);
        } catch (Exception e) {
            LOGGER.error("isExpired error: xid={} branchId={}", xid, branchId, e);
            return false;
        }
    }

    /**
     * is reached max retry count
     *
     * @param branchRetryCount
     * @return the boolean
     */
    @Override
    public boolean isReachedMaxRetryCount(int branchRetryCount) {
        try {
            RetryStrategyInfo retryStrategy = getRetryStrategyInfo();
            return retryStrategy.isReachedMaxRetryCount(branchRetryCount);
        } catch (Exception e) {
            LOGGER.error("isReachedMaxRetryCount error: xid={} branchId={}", xid, branchId, e);
            return false;
        }
    }

    /**
     * next retry interval
     *
     * @param branchRetryCount the count of the branch retries so far
     * @return the retry interval, the unit is milliseconds
     */
    @Override
    public long nextRetryInterval(int branchRetryCount) {
        try {
            RetryStrategyInfo retryStrategy = getRetryStrategyInfo();
            return retryStrategy != null ? retryStrategy.nextRetryInterval(branchRetryCount) : 0L;
        } catch (Exception e) {
            LOGGER.error("nextRetryInterval error: xid={} branchId={}", xid, branchId, e);
            return 0L;
        }
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

        byte[] retryStrategyBytes = retryStrategy != null ? retryStrategy.getBytes() : null;

        int size = calBranchSessionSize(resourceIdBytes, lockKeyBytes, clientIdBytes,
                applicationDataBytes, xidBytes, retryStrategyBytes);

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

        if (retryStrategyBytes != null) {
            byteBuffer.putShort((short)retryStrategyBytes.length);
            byteBuffer.put(retryStrategyBytes);
        } else {
            byteBuffer.putShort((short)0);
        }

        byteBuffer.putInt(retryCount);

        byteBuffer.flip();
        byte[] result = new byte[byteBuffer.limit()];
        byteBuffer.get(result);
        return result;
    }

    private int calBranchSessionSize(byte[] resourceIdBytes, byte[] lockKeyBytes, byte[] clientIdBytes,
                                     byte[] applicationDataBytes, byte[] xidBytes, byte[] retryStrategyBytes) {
        final int size = 8 // trascationId
            + 8 // branchId
            + 2 // resourceIdBytes.length
            + 2 // lockKeyBytes.length
            + 2 // clientIdBytes.length
            + 4 // applicationDataBytes.length
            + 2 // xidBytes.length
            + 2 // retryStrategyBytes.length
            + 1 // statusCode
            + (resourceIdBytes == null ? 0 : resourceIdBytes.length)
            + (lockKeyBytes == null ? 0 : lockKeyBytes.length)
            + (clientIdBytes == null ? 0 : clientIdBytes.length)
            + (applicationDataBytes == null ? 0 : applicationDataBytes.length)
            + (xidBytes == null ? 0 : xidBytes.length)
            + 1 // branchType
            + (retryStrategyBytes == null ? 0 : retryStrategyBytes.length)
            + 4;// retryCount
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

        short retryStrategyLen = byteBuffer.getShort();
        if (retryStrategyLen > 0) {
            byte[] retryStrategyBytes = new byte[retryStrategyLen];
            byteBuffer.get(retryStrategyBytes);
            this.retryStrategy = new String(retryStrategyBytes);
        }
        this.retryCount = byteBuffer.getInt();
    }

}
