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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.seata.common.XID;
import io.seata.core.exception.GlobalTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.server.UUIDGenerator;
import io.seata.server.lock.LockerFactory;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Global session.
 *
 * @author sharajava
 */
public class GlobalSession implements SessionLifecycle, SessionStorable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSession.class);

    private static final int MAX_GLOBAL_SESSION_SIZE = StoreConfig.getMaxGlobalSessionSize();

    private static ThreadLocal<ByteBuffer> byteBufferThreadLocal = ThreadLocal.withInitial(() -> ByteBuffer.allocate(
        MAX_GLOBAL_SESSION_SIZE));

    private String xid;

    private long transactionId;

    private volatile GlobalStatus status;

    private String applicationId;

    private String transactionServiceGroup;

    private String transactionName;

    private int timeout;

    private long beginTime;

    private String applicationData;

    private int version;

    private boolean active = true;

    private final ArrayList<BranchSession> branchSessions = new ArrayList<>();

    private GlobalSessionLock globalSessionLock = new GlobalSessionLock();


    /**
     * Add boolean.
     *
     * @param branchSession the branch session
     * @return the boolean
     */
    public boolean add(BranchSession branchSession) {
        return branchSessions.add(branchSession);
    }

    /**
     * Remove boolean.
     *
     * @param branchSession the branch session
     * @return the boolean
     */
    public boolean remove(BranchSession branchSession) {
        return branchSessions.remove(branchSession);
    }

    private Set<SessionLifecycleListener> lifecycleListeners = new HashSet<>();

    /**
     * Can be committed async boolean.
     *
     * @return the boolean
     */
    public boolean canBeCommittedAsync() {
        for (BranchSession branchSession : branchSessions) {
            if (branchSession.getBranchType() == BranchType.TCC) {
                return false;
            }
        }
        return true;
    }

    /**
     * Is saga type transaction
     *
     * @return is saga
     */
    public boolean isSaga() {
        if (branchSessions.size() > 0) {
            return BranchType.SAGA == branchSessions.get(0).getBranchType();
        }
        return false;
    }

    /**
     * Is timeout boolean.
     *
     * @return the boolean
     */
    public boolean isTimeout() {
        return (System.currentTimeMillis() - beginTime) > timeout;
    }

    /**
     * prevent could not handle rollbacking transaction
     * @return if true force roll back
     */
    public boolean isRollbackingDead() {
        return (System.currentTimeMillis() - beginTime) > (2 * 6000);
    }

    @Override
    public void begin() throws TransactionException {
        this.status = GlobalStatus.Begin;
        this.beginTime = System.currentTimeMillis();
        this.active = true;
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onBegin(this);
        }
    }

    @Override
    public void changeStatus(GlobalStatus status) throws TransactionException {
        this.status = status;
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onStatusChange(this, status);
        }

    }

    @Override
    public void changeBranchStatus(BranchSession branchSession, BranchStatus status)
        throws TransactionException {
        branchSession.setStatus(status);
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onBranchStatusChange(this, branchSession, status);
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void close() throws TransactionException {
        if (active) {
            for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onClose(this);
            }
        }
    }

    @Override
    public void end() throws TransactionException {
        // Clean locks first
        clean();

        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onEnd(this);
        }

    }

    public void clean() throws TransactionException {
        LockerFactory.getLockManager().releaseGlobalSessionLock(this);

    }

    /**
     * Close and clean.
     *
     * @throws TransactionException the transaction exception
     */
    public void closeAndClean() throws TransactionException {
        close();
        clean();

    }

    /**
     * Add session lifecycle listener.
     *
     * @param sessionLifecycleListener the session lifecycle listener
     */
    public void addSessionLifecycleListener(SessionLifecycleListener sessionLifecycleListener) {
        lifecycleListeners.add(sessionLifecycleListener);
    }

    /**
     * Remove session lifecycle listener.
     *
     * @param sessionLifecycleListener the session lifecycle listener
     */
    public void removeSessionLifecycleListener(SessionLifecycleListener sessionLifecycleListener) {
        lifecycleListeners.remove(sessionLifecycleListener);
    }

    @Override
    public void addBranch(BranchSession branchSession) throws TransactionException {
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onAddBranch(this, branchSession);
        }
        branchSession.setStatus(BranchStatus.Registered);
        add(branchSession);
    }

    @Override
    public void removeBranch(BranchSession branchSession) throws TransactionException {
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onRemoveBranch(this, branchSession);
        }
        branchSession.unlock();
        remove(branchSession);
    }

    /**
     * Gets branch.
     *
     * @param branchId the branch id
     * @return the branch
     */
    public BranchSession getBranch(long branchId) {
        synchronized (branchSessions) {
            for (BranchSession branchSession : branchSessions) {
                if (branchSession.getBranchId() == branchId) {
                    return branchSession;
                }
            }

            return null;
        }

    }

    /**
     * Gets sorted branches.
     *
     * @return the sorted branches
     */
    public ArrayList<BranchSession> getSortedBranches() {
        return new ArrayList<>(branchSessions);
    }

    /**
     * Gets reverse sorted branches.
     *
     * @return the reverse sorted branches
     */
    public ArrayList<BranchSession> getReverseSortedBranches() {
        ArrayList<BranchSession> reversed = new ArrayList<>(branchSessions);
        Collections.reverse(reversed);
        return reversed;
    }

    /**
     * Instantiates a new Global session.
     */
    public GlobalSession() {}

    /**
     * Instantiates a new Global session.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @param transactionName         the transaction name
     * @param timeout                 the timeout
     */
    public GlobalSession(String applicationId, String transactionServiceGroup, String transactionName, int timeout) {
        this.transactionId = UUIDGenerator.generateUUID();
        this.status = GlobalStatus.Begin;

        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
        this.transactionName = transactionName;
        this.timeout = timeout;
        this.xid = XID.generateXID(transactionId);
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
     * Gets status.
     *
     * @return the status
     */
    public GlobalStatus getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(GlobalStatus status) {
        this.status = status;
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

    /**
     * Gets application id.
     *
     * @return the application id
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Gets transaction service group.
     *
     * @return the transaction service group
     */
    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    /**
     * Gets transaction name.
     *
     * @return the transaction name
     */
    public String getTransactionName() {
        return transactionName;
    }

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Gets begin time.
     *
     * @return the begin time
     */
    public long getBeginTime() {
        return beginTime;
    }

    /**
     * Sets begin time.
     *
     * @param beginTime the begin time
     */
    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

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
     * Gets session version
     *
     * @return session version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Sets session version
     *
     * @param version the session version
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Create global session global session.
     *
     * @param applicationId  the application id
     * @param txServiceGroup the tx service group
     * @param txName         the tx name
     * @param timeout        the timeout
     * @return the global session
     */
    public static GlobalSession createGlobalSession(String applicationId, String txServiceGroup, String txName,
                                                    int timeout) {
        GlobalSession session = new GlobalSession(applicationId, txServiceGroup, txName, timeout);
        return session;
    }

    /**
     * Sets active.
     *
     * @param active the active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public byte[] encode() {

        byte[] byApplicationIdBytes = applicationId != null ? applicationId.getBytes() : null;

        byte[] byServiceGroupBytes = transactionServiceGroup != null ? transactionServiceGroup.getBytes() : null;

        byte[] byTxNameBytes = transactionName != null ? transactionName.getBytes() : null;

        byte[] xidBytes = xid != null ? xid.getBytes() : null;

        byte[] applicationDataBytes = applicationData != null ? applicationData.getBytes() : null;

        int size = calGlobalSessionSize(byApplicationIdBytes, byServiceGroupBytes, byTxNameBytes, xidBytes,
            applicationDataBytes);

        if (size > MAX_GLOBAL_SESSION_SIZE) {
            throw new RuntimeException("global session size exceeded, size : " + size + " maxBranchSessionSize : " +
                MAX_GLOBAL_SESSION_SIZE);
        }
        ByteBuffer byteBuffer = byteBufferThreadLocal.get();
        //recycle
        byteBuffer.clear();

        byteBuffer.putLong(transactionId);
        byteBuffer.putInt(timeout);
        if (null != byApplicationIdBytes) {
            byteBuffer.putShort((short)byApplicationIdBytes.length);
            byteBuffer.put(byApplicationIdBytes);
        } else {
            byteBuffer.putShort((short)0);
        }
        if (null != byServiceGroupBytes) {
            byteBuffer.putShort((short)byServiceGroupBytes.length);
            byteBuffer.put(byServiceGroupBytes);
        } else {
            byteBuffer.putShort((short)0);
        }
        if (null != byTxNameBytes) {
            byteBuffer.putShort((short)byTxNameBytes.length);
            byteBuffer.put(byTxNameBytes);
        } else {
            byteBuffer.putShort((short)0);
        }
        if (xidBytes != null) {
            byteBuffer.putInt(xidBytes.length);
            byteBuffer.put(xidBytes);
        } else {
            byteBuffer.putInt(0);
        }
        if (applicationDataBytes != null) {
            byteBuffer.putInt(applicationDataBytes.length);
            byteBuffer.put(applicationDataBytes);
        } else {
            byteBuffer.putInt(0);
        }

        byteBuffer.putLong(beginTime);
        byteBuffer.put((byte)status.getCode());
        byteBuffer.putInt(version);
        byteBuffer.flip();
        byte[] result = new byte[byteBuffer.limit()];
        byteBuffer.get(result);
        return result;
    }

    private int calGlobalSessionSize(byte[] byApplicationIdBytes, byte[] byServiceGroupBytes, byte[] byTxNameBytes,
                                     byte[] xidBytes, byte[] applicationDataBytes) {
        final int size = 8 // transactionId
            + 4 // timeout
            + 2 // byApplicationIdBytes.length
            + 2 // byServiceGroupBytes.length
            + 2 // byTxNameBytes.length
            + 4 // xidBytes.length
            + 4 // applicationDataBytes.length
            + 8 // beginTime
            + 1 // statusCode
            + 4 // version
            + (byApplicationIdBytes == null ? 0 : byApplicationIdBytes.length)
            + (byServiceGroupBytes == null ? 0 : byServiceGroupBytes.length)
            + (byTxNameBytes == null ? 0 : byTxNameBytes.length)
            + (xidBytes == null ? 0 : xidBytes.length)
            + (applicationDataBytes == null ? 0 : applicationDataBytes.length);
        return size;
    }

    @Override
    public void decode(byte[] a) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(a);
        this.transactionId = byteBuffer.getLong();
        this.timeout = byteBuffer.getInt();
        short applicationIdLen = byteBuffer.getShort();
        if (applicationIdLen > 0) {
            byte[] byApplicationId = new byte[applicationIdLen];
            byteBuffer.get(byApplicationId);
            this.applicationId = new String(byApplicationId);
        }
        short serviceGroupLen = byteBuffer.getShort();
        if (serviceGroupLen > 0) {
            byte[] byServiceGroup = new byte[serviceGroupLen];
            byteBuffer.get(byServiceGroup);
            this.transactionServiceGroup = new String(byServiceGroup);
        }
        short txNameLen = byteBuffer.getShort();
        if (txNameLen > 0) {
            byte[] byTxName = new byte[txNameLen];
            byteBuffer.get(byTxName);
            this.transactionName = new String(byTxName);
        }
        int xidLen = byteBuffer.getInt();
        if (xidLen > 0) {
            byte[] xidBytes = new byte[xidLen];
            byteBuffer.get(xidBytes);
            this.xid = new String(xidBytes);
        }
        int applicationDataLen = byteBuffer.getInt();
        if (applicationDataLen > 0) {
            byte[] applicationDataLenBytes = new byte[applicationDataLen];
            byteBuffer.get(applicationDataLenBytes);
            this.applicationData = new String(applicationDataLenBytes);
        }

        this.beginTime = byteBuffer.getLong();
        this.status = GlobalStatus.get(byteBuffer.get());
        this.version = byteBuffer.getInt();
    }

    /**
     * Has branch boolean.
     *
     * @return the boolean
     */
    public boolean hasBranch() {
        return branchSessions.size() > 0;
    }

    public void lock() throws TransactionException {
        globalSessionLock.lock();
    }

    public void unlock() {
        globalSessionLock.unlock();
    }

    public void lockAndExecute(LockRunnable excuteRunnable) throws TransactionException {
        this.lock();
        try {
            excuteRunnable.run();
        } finally {
            this.unlock();
        }
    }

    public <T> T lockAndExecute(LockCallable<T> lockCallable) throws TransactionException {
        this.lock();
        try {
            return lockCallable.call();
        } finally {
            this.unlock();
        }
    }

    private static class GlobalSessionLock {

        private Lock globalSessionLock = new ReentrantLock();

        private static final int GLOBAL_SESSION_LOCK_TIME_OUT_MILLS = 2 * 1000;

        public void lock() throws TransactionException {
            try {
                if (globalSessionLock.tryLock(GLOBAL_SESSION_LOCK_TIME_OUT_MILLS, TimeUnit.MILLISECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted error", e);
            }
            throw new GlobalTransactionException(TransactionExceptionCode.FailedLockGlobalTranscation, "Lock global session failed");
        }

        public void unlock() {
            globalSessionLock.unlock();
        }

    }

    @FunctionalInterface
    public interface LockRunnable {

        void run() throws TransactionException;
    }

    @FunctionalInterface
    public interface LockCallable<V> {

        V call() throws TransactionException;
    }

    public ArrayList<BranchSession> getBranchSessions() {
        return branchSessions;
    }

    public void asyncCommit() throws TransactionException {
        this.addSessionLifecycleListener(SessionHolder.getAsyncCommittingSessionManager());
        SessionHolder.getAsyncCommittingSessionManager().addGlobalSession(this);
        this.changeStatus(GlobalStatus.AsyncCommitting);
    }

    public void queueToRetryCommit() throws TransactionException {
        this.addSessionLifecycleListener(SessionHolder.getRetryCommittingSessionManager());
        SessionHolder.getRetryCommittingSessionManager().addGlobalSession(this);
        this.changeStatus(GlobalStatus.CommitRetrying);
    }

    public void queueToRetryRollback() throws TransactionException {
        this.addSessionLifecycleListener(SessionHolder.getRetryRollbackingSessionManager());
        SessionHolder.getRetryRollbackingSessionManager().addGlobalSession(this);
        GlobalStatus currentStatus = this.getStatus();
        if (SessionHelper.isTimeoutGlobalStatus(currentStatus)) {
            this.changeStatus(GlobalStatus.TimeoutRollbackRetrying);
        } else {
            this.changeStatus(GlobalStatus.RollbackRetrying);
        }
    }
}
