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
package io.seata.server.coordinator;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.DurationUtil;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.event.EventBus;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.transaction.AbstractTransactionRequestToTC;
import io.seata.core.protocol.transaction.AbstractTransactionResponse;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalReportResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;
import io.seata.core.rpc.ChannelManager;
import io.seata.core.rpc.Disposable;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.ServerMessageSender;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.netty.RpcServer;
import io.seata.server.AbstractTCInboundHandler;
import io.seata.server.event.EventBusManager;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Default coordinator.
 */
public class DefaultCoordinator extends AbstractTCInboundHandler implements TransactionMessageHandler, Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCoordinator.class);

    private static final int TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS = 5000;

    /**
     * The constant COMMITTING_RETRY_PERIOD.
     */
    protected static final long COMMITTING_RETRY_PERIOD = CONFIG.getLong(ConfigurationKeys.COMMITING_RETRY_PERIOD,
        1000L);

    /**
     * The constant ASYNC_COMMITTING_RETRY_PERIOD.
     */
    protected static final long ASYNC_COMMITTING_RETRY_PERIOD = CONFIG.getLong(
        ConfigurationKeys.ASYN_COMMITING_RETRY_PERIOD, 1000L);

    /**
     * The constant ROLLBACKING_RETRY_PERIOD.
     */
    protected static final long ROLLBACKING_RETRY_PERIOD = CONFIG.getLong(ConfigurationKeys.ROLLBACKING_RETRY_PERIOD,
        1000L);

    /**
     * The constant TIMEOUT_RETRY_PERIOD.
     */
    protected static final long TIMEOUT_RETRY_PERIOD = CONFIG.getLong(ConfigurationKeys.TIMEOUT_RETRY_PERIOD, 1000L);

    /**
     * The Transaction undo log delete period.
     */
    protected static final long UNDO_LOG_DELETE_PERIOD = CONFIG.getLong(
        ConfigurationKeys.TRANSACTION_UNDO_LOG_DELETE_PERIOD, 24 * 60 * 60 * 1000);

    /**
     * The Transaction undo log delay delete period
     */
    protected static final long UNDO_LOG_DELAY_DELETE_PERIOD = 3 * 60 * 1000;

    private static final int ALWAYS_RETRY_BOUNDARY = 0;

    private static final Duration MAX_COMMIT_RETRY_TIMEOUT = ConfigurationFactory.getInstance().getDuration(
        ConfigurationKeys.MAX_COMMIT_RETRY_TIMEOUT, DurationUtil.DEFAULT_DURATION, 100);

    private static final Duration MAX_ROLLBACK_RETRY_TIMEOUT = ConfigurationFactory.getInstance().getDuration(
        ConfigurationKeys.MAX_ROLLBACK_RETRY_TIMEOUT, DurationUtil.DEFAULT_DURATION, 100);

    private static final boolean ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE = ConfigurationFactory.getInstance().getBoolean(
        ConfigurationKeys.ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE, false);

    private HashedWheelTimer retryRollbackingWheelTimer = new HashedWheelTimer(
        new NamedThreadFactory("RetryRollbacking", 1), ROLLBACKING_RETRY_PERIOD, TimeUnit.MILLISECONDS, 512);

    private HashedWheelTimer retryCommittingWheelTimer = new HashedWheelTimer(
        new NamedThreadFactory("RetryCommitting", 1), COMMITTING_RETRY_PERIOD, TimeUnit.MILLISECONDS, 512);

    private HashedWheelTimer asyncCommittingWheelTimer = new HashedWheelTimer(
        new NamedThreadFactory("AsyncCommitting", 1), ASYNC_COMMITTING_RETRY_PERIOD, TimeUnit.MILLISECONDS, 512);

    private HashedWheelTimer timeoutCheckWheelTimer = new HashedWheelTimer(new NamedThreadFactory("TxTimeoutCheck", 1),
        TIMEOUT_RETRY_PERIOD, TimeUnit.MILLISECONDS, 512);

    private HashedWheelTimer undoLogDeleteWheelTimer = new HashedWheelTimer(new NamedThreadFactory("UndoLogDelete", 1),
        UNDO_LOG_DELAY_DELETE_PERIOD, TimeUnit.MILLISECONDS, 512);

    private ServerMessageSender messageSender;

    private DefaultCore core;

    private EventBus eventBus = EventBusManager.get();

    /**
     * Instantiates a new Default coordinator.
     *
     * @param messageSender the message sender
     */
    public DefaultCoordinator(ServerMessageSender messageSender) {
        this.messageSender = messageSender;
        this.core = new DefaultCore(messageSender);
    }

    @Override
    protected void doGlobalBegin(GlobalBeginRequest request, GlobalBeginResponse response, RpcContext rpcContext)
        throws TransactionException {
        response.setXid(core.begin(rpcContext.getApplicationId(), rpcContext.getTransactionServiceGroup(),
            request.getTransactionName(), request.getTimeout()));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Begin new global transaction applicationId: {},transactionServiceGroup: {}, transactionName: {},timeout:{},xid:{}",
                rpcContext.getApplicationId(), rpcContext.getTransactionServiceGroup(), request.getTransactionName(), request.getTimeout(), response.getXid());
        }
    }

    @Override
    protected void doGlobalCommit(GlobalCommitRequest request, GlobalCommitResponse response, RpcContext rpcContext)
        throws TransactionException {
        response.setGlobalStatus(core.commit(request.getXid()));
    }

    @Override
    protected void doGlobalRollback(GlobalRollbackRequest request, GlobalRollbackResponse response,
                                    RpcContext rpcContext) throws TransactionException {
        response.setGlobalStatus(core.rollback(request.getXid()));
    }

    @Override
    protected void doGlobalStatus(GlobalStatusRequest request, GlobalStatusResponse response, RpcContext rpcContext)
        throws TransactionException {
        response.setGlobalStatus(core.getStatus(request.getXid()));
    }

    @Override
    protected void doGlobalReport(GlobalReportRequest request, GlobalReportResponse response, RpcContext rpcContext)
        throws TransactionException {
        response.setGlobalStatus(core.globalReport(request.getXid(), request.getGlobalStatus()));
    }

    @Override
    protected void doBranchRegister(BranchRegisterRequest request, BranchRegisterResponse response,
                                    RpcContext rpcContext) throws TransactionException {
        response.setBranchId(
            core.branchRegister(request.getBranchType(), request.getResourceId(), rpcContext.getClientId(),
                request.getXid(), request.getApplicationData(), request.getLockKey()));
    }

    @Override
    protected void doBranchReport(BranchReportRequest request, BranchReportResponse response, RpcContext rpcContext)
        throws TransactionException {
        core.branchReport(request.getBranchType(), request.getXid(), request.getBranchId(), request.getStatus(),
            request.getApplicationData());
    }

    @Override
    protected void doLockCheck(GlobalLockQueryRequest request, GlobalLockQueryResponse response, RpcContext rpcContext)
        throws TransactionException {
        response.setLockable(
            core.lockQuery(request.getBranchType(), request.getResourceId(), request.getXid(), request.getLockKey()));
    }

    /**
     * Timeout check.
     *
     * @throws TransactionException the transaction exception
     */
    protected void timeoutCheck() throws TransactionException {
        Collection<GlobalSession> allSessions = SessionHolder.getRootSessionManager().allSessions();
        if (CollectionUtils.isEmpty(allSessions)) {
            return;
        }
        if (!allSessions.isEmpty() && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Global transaction timeout check begin, size: {}", allSessions.size());
        }
        for (GlobalSession globalSession : allSessions) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    globalSession.getXid() + " " + globalSession.getStatus() + " " + globalSession.getBeginTime() + " "
                        + globalSession.getTimeout());
            }
            boolean shouldTimeout = SessionHolder.lockAndExecute(globalSession, () -> {
                if (globalSession.getStatus() != GlobalStatus.Begin || !globalSession.isTimeout()) {
                    return false;
                }
                globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                globalSession.close();
                globalSession.changeStatus(GlobalStatus.TimeoutRollbacking);

                // transaction timeout and start rollbacking event
                eventBus.post(
                    new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                        globalSession.getTransactionName(), globalSession.getBeginTime(), null,
                        globalSession.getStatus()));

                return true;
            });
            if (!shouldTimeout) {
                continue;
            }
            LOGGER.info("Global transaction[{}] is timeout and will be rollback.", globalSession.getXid());

            globalSession.addSessionLifecycleListener(SessionHolder.getRetryRollbackingSessionManager());
            SessionHolder.getRetryRollbackingSessionManager().addGlobalSession(globalSession);

        }
        if (!allSessions.isEmpty() && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Global transaction timeout check end. ");
        }

    }

    /**
     * Handle retry rollbacking.
     */
    protected void handleRetryRollbacking() {
        Collection<GlobalSession> rollbackingSessions = SessionHolder.getRetryRollbackingSessionManager().allSessions();
        if (CollectionUtils.isEmpty(rollbackingSessions)) {
            return;
        }
        long now = System.currentTimeMillis();
        for (GlobalSession rollbackingSession : rollbackingSessions) {
            try {
                // prevent repeated rollback
                if (rollbackingSession.getStatus().equals(GlobalStatus.Rollbacking) && !rollbackingSession.isRollbackingDead()) {
                    continue;
                }
                if (isRetryTimeout(now, MAX_ROLLBACK_RETRY_TIMEOUT.toMillis(), rollbackingSession.getBeginTime())) {
                    if (ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE) {
                        rollbackingSession.clean();
                    }
                    /**
                     * Prevent thread safety issues
                     */
                    SessionHolder.getRetryRollbackingSessionManager().removeGlobalSession(rollbackingSession);
                    LOGGER.info("Global transaction rollback retry timeout and has removed [{}]", rollbackingSession.getXid());
                    continue;
                }
                rollbackingSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                core.doGlobalRollback(rollbackingSession, true);
            } catch (TransactionException ex) {
                LOGGER.info("Failed to retry rollbacking [{}] {} {}", rollbackingSession.getXid(), ex.getCode(), ex.getMessage());
            }
        }
    }

    /**
     * Handle retry committing.
     */
    protected void handleRetryCommitting() {
        Collection<GlobalSession> committingSessions = SessionHolder.getRetryCommittingSessionManager().allSessions();
        if (CollectionUtils.isEmpty(committingSessions)) {
            return;
        }
        long now = System.currentTimeMillis();
        for (GlobalSession committingSession : committingSessions) {
            try {
                if (isRetryTimeout(now, MAX_COMMIT_RETRY_TIMEOUT.toMillis(), committingSession.getBeginTime())) {
                    /**
                     * Prevent thread safety issues
                     */
                    SessionHolder.getRetryCommittingSessionManager().removeGlobalSession(committingSession);
                    LOGGER.error("Global transaction commit retry timeout and has removed [{}]", committingSession.getXid());
                    continue;
                }
                committingSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                core.doGlobalCommit(committingSession, true);
            } catch (TransactionException ex) {
                LOGGER.info("Failed to retry committing [{}] {} {}", committingSession.getXid(), ex.getCode(), ex.getMessage());
            }
        }
    }

    private boolean isRetryTimeout(long now, long timeout, long beginTime) {
        return timeout >= ALWAYS_RETRY_BOUNDARY && now - beginTime > timeout;
    }

    /**
     * Handle async committing.
     */
    protected void handleAsyncCommitting() {
        Collection<GlobalSession> asyncCommittingSessions = SessionHolder.getAsyncCommittingSessionManager()
            .allSessions();
        if (CollectionUtils.isEmpty(asyncCommittingSessions)) {
            return;
        }
        for (GlobalSession asyncCommittingSession : asyncCommittingSessions) {
            try {
                // Instruction reordering in DefaultCore#asyncCommit may cause this situation
                if (GlobalStatus.AsyncCommitting != asyncCommittingSession.getStatus()) {
                    continue;
                }
                asyncCommittingSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                core.doGlobalCommit(asyncCommittingSession, true);
            } catch (TransactionException ex) {
                LOGGER.error("Failed to async committing [{}] {} {}", asyncCommittingSession.getXid(), ex.getCode(), ex.getMessage(), ex);
            }
        }
    }

    /**
     * Undo log delete.
     */
    protected void undoLogDelete() {
        Map<String, Channel> rmChannels = ChannelManager.getRmChannels();
        if (rmChannels == null || rmChannels.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("no active rm channels to delete undo log");
            }
            return;
        }
        short saveDays = CONFIG.getShort(ConfigurationKeys.TRANSACTION_UNDO_LOG_SAVE_DAYS,
            UndoLogDeleteRequest.DEFAULT_SAVE_DAYS);
        for (Map.Entry<String, Channel> channelEntry : rmChannels.entrySet()) {
            String resourceId = channelEntry.getKey();
            UndoLogDeleteRequest deleteRequest = new UndoLogDeleteRequest();
            deleteRequest.setResourceId(resourceId);
            deleteRequest.setSaveDays(saveDays > 0 ? saveDays : UndoLogDeleteRequest.DEFAULT_SAVE_DAYS);
            try {
                messageSender.sendASyncRequest(channelEntry.getValue(), deleteRequest);
            } catch (Exception e) {
                LOGGER.error("Failed to async delete undo log resourceId = {}, exception: {}", resourceId, e.getMessage());
            }
        }
    }

    /**
     * Init.
     */
    public void init() {
        RetryRollbackingTimeTask retryRollbackingTimeTask = new RetryRollbackingTimeTask();
        retryRollbackingWheelTimer.newTimeout(retryRollbackingTimeTask, 0, TimeUnit.MILLISECONDS);

        RetryCommittingTimeTask retryCommittingTimeTask = new RetryCommittingTimeTask();
        retryCommittingWheelTimer.newTimeout(retryCommittingTimeTask, 0, TimeUnit.MILLISECONDS);

        AsyncCommittingTimeTask asyncCommittingTimeTask = new AsyncCommittingTimeTask();
        asyncCommittingWheelTimer.newTimeout(asyncCommittingTimeTask, 0, TimeUnit.MILLISECONDS);

        TimeoutCheckTimeTask timeoutCheckTimeTask = new TimeoutCheckTimeTask();
        timeoutCheckWheelTimer.newTimeout(timeoutCheckTimeTask, 0, TimeUnit.MILLISECONDS);

        UndoLogDeleteTimeTask undoLogDeleteTimeTask = new UndoLogDeleteTimeTask();
        undoLogDeleteWheelTimer.newTimeout(undoLogDeleteTimeTask, 0, TimeUnit.MILLISECONDS);
    }
    private class RetryRollbackingTimeTask implements TimerTask {

        @Override
        public void run(Timeout timeout) {
            try {
                handleRetryRollbacking();
            } catch (Exception e) {
                LOGGER.info("Exception retry rollbacking ... ", e);
            } finally {
                retryRollbackingWheelTimer.newTimeout(this, ROLLBACKING_RETRY_PERIOD >> 1, TimeUnit.MILLISECONDS);
            }
        }
    }


    private class RetryCommittingTimeTask implements TimerTask {

        @Override
        public void run(Timeout timeout) {
            try {
                handleRetryCommitting();
            } catch (Exception e) {
                LOGGER.info("Exception retry committing ... ", e);
            } finally {
                retryCommittingWheelTimer.newTimeout(this, 0, TimeUnit.MILLISECONDS);
            }
        }
    }


    private class AsyncCommittingTimeTask implements TimerTask {

        @Override
        public void run(Timeout timeout) {
            try {
                handleAsyncCommitting();
            } catch (Exception e) {
                LOGGER.info("Exception async committing ... ", e);
            } finally {
                retryCommittingWheelTimer.newTimeout(this, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    private class TimeoutCheckTimeTask implements TimerTask {

        @Override
        public void run(Timeout timeout) {
            try {
                timeoutCheck();
            } catch (Exception e) {
                LOGGER.info("Exception timeout checking ... ", e);
            } finally {
                retryCommittingWheelTimer.newTimeout(this, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    private class UndoLogDeleteTimeTask implements TimerTask {

        @Override
        public void run(Timeout timeout) {
            try {
                undoLogDelete();
            } catch (Exception e) {
                LOGGER.info("Exception undoLog deleting ... ", e);
            } finally {
                retryCommittingWheelTimer.newTimeout(this, 0, TimeUnit.MILLISECONDS);
            }
        }
    }



    @Override
    public AbstractResultMessage onRequest(AbstractMessage request, RpcContext context) {
        if (!(request instanceof AbstractTransactionRequestToTC)) {
            throw new IllegalArgumentException();
        }
        AbstractTransactionRequestToTC transactionRequest = (AbstractTransactionRequestToTC) request;
        transactionRequest.setTCInboundHandler(this);

        return transactionRequest.handle(context);
    }

    @Override
    public void onResponse(AbstractResultMessage response, RpcContext context) {
        if (!(response instanceof AbstractTransactionResponse)) {
            throw new IllegalArgumentException();
        }

    }

    @Override
    public void destroy() {
        // 1. first shutdown timed task
        retryRollbackingWheelTimer.stop();
        retryCommittingWheelTimer.stop();
        asyncCommittingWheelTimer.stop();
        timeoutCheckWheelTimer.stop();
        // 2. second close netty flow
        if (messageSender instanceof RpcServer) {
            ((RpcServer) messageSender).destroy();
        }
        // 3. last destroy SessionHolder
        SessionHolder.destroy();
    }
}
