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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import io.seata.core.domain.GlobalSessionStatusStateMachine;
import io.seata.core.domain.StateMachineHelper;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalOperation;
import io.seata.core.model.GlobalStatus;
import io.seata.server.UUIDGenerator;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.StoreConfig;

/**
 * The type Global session.
 *
 * @author sharajava
 */
public class GlobalSession implements SessionLifecycle, SessionStorable {

    private static final int MAX_GLOBAL_SESSION_SIZE = StoreConfig.getMaxGlobalSessionSize();

    private static ThreadLocal<ByteBuffer> byteBufferThreadLocal = ThreadLocal.withInitial(() -> ByteBuffer.allocate(
        MAX_GLOBAL_SESSION_SIZE));

    private long transactionId;

    private volatile GlobalStatus status;

    private String applicationId;

    private String transactionServiceGroup;

    private String transactionName;

    private int timeout;

    private long beginTime;

    private boolean active;

    private ArrayList<BranchSession> branchSessions = new ArrayList<>();

    private GlobalSessionStatusStateMachine sessionStatusStateMachine = StateMachineHelper.buildGlobalStatusMachine(
        GlobalStatus.UnKnown);

    private GlobalSessionSpinLock globalSessionSpinLock = new GlobalSessionSpinLock();

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

    private ArrayList<SessionLifecycleListener> lifecycleListeners = new ArrayList<>();

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
     * Is timeout boolean.
     *
     * @return the boolean
     */
    public boolean isTimeout() {
        return (System.currentTimeMillis() - beginTime) > timeout;
    }

    @Override
    public void begin() throws TransactionException {
        this.beginTime = System.currentTimeMillis();
        this.active = true;
        sessionStatusStateMachine.fire(GlobalOperation.BEGIN);
        this.status = GlobalStatus.Begin;
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onBegin(this);
        }
    }

    @Override
    public void changeStatus(GlobalOperation operation, GlobalStatus status) throws TransactionException {
        sessionStatusStateMachine.fire(operation);
        if (sessionStatusStateMachine.getCurrentState() != status) {
            throw new TransactionException(
                TransactionExceptionCode.FailedToChangeGlobalStatus,
                "status not can be changed,current status is " + this.status + " ,operation=" + operation
                    + " , target status="
                    + status);
        }
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

    private void clean() throws TransactionException {
        for (BranchSession branchSession : branchSessions) {
            branchSession.unlock();
        }

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
        ArrayList<BranchSession> sorted = new ArrayList();
        sorted.addAll(branchSessions);
        return sorted;
    }

    /**
     * Gets reverse sorted branches.
     *
     * @return the reverse sorted branches
     */
    public ArrayList<BranchSession> getReverseSortedBranches() {
        ArrayList<BranchSession> reversed = new ArrayList();
        reversed.addAll(branchSessions);
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
        this.sessionStatusStateMachine.fire(GlobalOperation.BEGIN);
        this.status = GlobalStatus.Begin;
        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
        this.transactionName = transactionName;
        this.timeout = timeout;
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
    void setStatus(GlobalStatus status) {
        this.status = status;
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

        int size = calGlobalSessionSize(byApplicationIdBytes, byServiceGroupBytes, byTxNameBytes);

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
        byteBuffer.putLong(beginTime);
        byteBuffer.put((byte)status.getCode());
        byteBuffer.flip();
        byte[] result = new byte[byteBuffer.limit()];
        byteBuffer.get(result);
        return result;
    }

    private int calGlobalSessionSize(byte[] byApplicationIdBytes, byte[] byServiceGroupBytes, byte[] byTxNameBytes) {
        final int size = 8 // trascationId
            + 4 // timeout
            + 2 // byApplicationIdBytes.length
            + 2 // byServiceGroupBytes.length
            + 2 // byTxNameBytes.length
            + 8 // beginTime
            + 1 // statusCode
            + (byApplicationIdBytes == null ? 0 : byApplicationIdBytes.length)
            + (byServiceGroupBytes == null ? 0 : byServiceGroupBytes.length)
            + (byTxNameBytes == null ? 0 : byTxNameBytes.length);
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
        this.beginTime = byteBuffer.getLong();
        this.status = GlobalStatus.get(byteBuffer.get());

        this.sessionStatusStateMachine = StateMachineHelper.buildGlobalStatusMachine(this.status);
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
        globalSessionSpinLock.lock();
    }

    public void unlock() {
        globalSessionSpinLock.unlock();
    }

    public void lockAndExcute(LockRunnable excuteRunnable) throws TransactionException {
        this.lock();
        try {
            excuteRunnable.run();
        } finally {
            this.unlock();
        }
    }

    public <T> T lockAndExcute(LockCallable<T> lockCallable) throws TransactionException {
        this.lock();
        try {
            return lockCallable.call();
        } finally {
            this.unlock();
        }
    }

    /**
     * globalsession is low race conditions, so use spinlock
     */
    private class GlobalSessionSpinLock {
        /**
         * true: Can lock, false : in lock.
         */
        private AtomicBoolean globalSessionSpinLock = new AtomicBoolean(true);

        private static final int GLOBAL_SESSOION_LOCK_TIME_OUT_MILLS = 2 * 1000;

        private static final int PARK_TIMES_BASE = 10;

        private static final int PARK_TIMES_BASE_MILLS = 1;

        public void lock() throws TransactionException {
            boolean flag;
            int times = 1;
            long beginTime = System.currentTimeMillis();
            do {
                long restTime = GLOBAL_SESSOION_LOCK_TIME_OUT_MILLS - (System.currentTimeMillis() - beginTime);
                if (restTime <= 0) {
                    throw new TransactionException(TransactionExceptionCode.FailedLockGlobalTranscation);
                }
                // Pause every PARK_TIMES_BASE times,yield the CPU
                if (times % PARK_TIMES_BASE == 0) {
                    // Exponential Backoff
                    long backOffTime = PARK_TIMES_BASE_MILLS << (times / PARK_TIMES_BASE);
                    long parkTime = backOffTime < restTime ? backOffTime : restTime;
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(parkTime));
                }
                flag = this.globalSessionSpinLock.compareAndSet(true, false);
                times++;
            }
            while (!flag);
        }

        public void unlock() {
            this.globalSessionSpinLock.compareAndSet(false, true);
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

    public GlobalSessionStatusStateMachine getSessionStatusStateMachine() {
        return sessionStatusStateMachine;
    }
}
