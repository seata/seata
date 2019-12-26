package io.seata.server.transaction.at;

import io.netty.channel.Channel;
import io.seata.core.event.EventBus;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.exception.BranchTransactionException;
import io.seata.core.exception.GlobalTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.ResourceManagerInbound;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.rpc.ChannelManager;
import io.seata.server.coordinator.AbstractCore;
import io.seata.server.coordinator.Core;
import io.seata.server.coordinator.DefaultCore;
import io.seata.server.event.EventBusManager;
import io.seata.server.lock.LockManager;
import io.seata.server.lock.LockerManagerFactory;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHelper;
import io.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static io.seata.core.exception.TransactionExceptionCode.*;

/**
 * Created by txg on 2019-12-24.
 */
public class ATCore extends AbstractCore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCore.class);

    private EventBus eventBus = EventBusManager.get();

    @Override
    public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid,
                               String applicationData, String lockKeys) throws TransactionException {
        GlobalSession globalSession = assertGlobalSessionNotNull(xid, false);
        return globalSession.lockAndExcute(() -> {
            if (!globalSession.isActive()) {
                throw new GlobalTransactionException(GlobalTransactionNotActive, String
                        .format("Could not register branch into global session xid = %s status = %s",
                                globalSession.getXid(), globalSession.getStatus()));
            }
            //SAGA type accept forward(retry) operation, forward operation will register remaining branches
            if (globalSession.getStatus() != GlobalStatus.Begin) {
                throw new GlobalTransactionException(GlobalTransactionStatusInvalid, String
                        .format("Could not register branch into global session xid = %s status = %s while expecting %s",
                                globalSession.getXid(), globalSession.getStatus(), GlobalStatus.Begin));
            }
            globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
            BranchSession branchSession = SessionHelper.newBranchByGlobal(globalSession, branchType, resourceId,
                    applicationData, lockKeys, clientId);
            if (!branchSession.lock()) {
                throw new BranchTransactionException(LockKeyConflict, String
                        .format("Global lock acquire failed xid = %s branchId = %s", globalSession.getXid(),
                                branchSession.getBranchId()));
            }
            try {
                globalSession.addBranch(branchSession);
            } catch (RuntimeException ex) {
                branchSession.unlock();
                throw new BranchTransactionException(FailedToAddBranch, String
                        .format("Failed to store branch xid = %s branchId = %s", globalSession.getXid(),
                                branchSession.getBranchId()), ex);
            }
            LOGGER.info("Successfully register branch xid = {}, branchId = {}", globalSession.getXid(),
                    branchSession.getBranchId());
            return branchSession.getBranchId();
        });
    }

    private GlobalSession assertGlobalSessionNotNull(String xid, boolean withBranchSessions)
            throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid, withBranchSessions);
        if (globalSession == null) {
            throw new GlobalTransactionException(TransactionExceptionCode.GlobalTransactionNotExist,
                    String.format("Could not found global transaction xid = %s", xid));
        }
        return globalSession;
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
            throws TransactionException {
        return lockManager.isLockable(xid, resourceId, lockKeys);
    }

    @Override
    public void doGlobalCommit(GlobalSession globalSession, boolean retrying) throws TransactionException {
        //start committing event
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                globalSession.getTransactionName(), globalSession.getBeginTime(), null, globalSession.getStatus()));
        for (BranchSession branchSession : globalSession.getSortedBranches()) {
            BranchStatus currentStatus = branchSession.getStatus();
            if (currentStatus == BranchStatus.PhaseOne_Failed) {
                globalSession.removeBranch(branchSession);
                continue;
            }
            try {
                BranchStatus branchStatus = branchCommit(branchSession.getBranchType(),
                        branchSession.getXid(), branchSession.getBranchId(), branchSession.getResourceId(),
                        branchSession.getApplicationData());

                switch (branchStatus) {
                    case PhaseTwo_Committed:
                        globalSession.removeBranch(branchSession);
                        continue;
                    case PhaseTwo_CommitFailed_Unretryable:
                        if (globalSession.canBeCommittedAsync()) {
                            LOGGER.error("By [{}], failed to commit branch {}", branchStatus, branchSession);
                            continue;
                        } else {
                            SessionHelper.endCommitFailed(globalSession);
                            LOGGER.error("Finally, failed to commit global[{}] since branch[{}] commit failed",
                                    globalSession.getXid(), branchSession.getBranchId());
                            return;
                        }
                    default:
                        if (!retrying) {
                            queueToRetryCommit(globalSession);
                            return;
                        }
                        if (globalSession.canBeCommittedAsync()) {
                            LOGGER.error("By [{}], failed to commit branch {}", branchStatus, branchSession);
                            continue;
                        } else {
                            LOGGER.error(
                                    "Failed to commit global[{}] since branch[{}] commit failed, will retry later.",
                                    globalSession.getXid(), branchSession.getBranchId());
                            return;
                        }

                }

            } catch (Exception ex) {
                LOGGER.error("Exception committing branch {}", branchSession, ex);
                if (!retrying) {
                    queueToRetryCommit(globalSession);
                    throw new TransactionException(ex);
                }

            }

        }
        if (globalSession.hasBranch()) {
            LOGGER.info("Global[{}] committing is NOT done.", globalSession.getXid());
            return;
        }

        SessionHelper.endCommitted(globalSession);

        //committed event
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                globalSession.getTransactionName(), globalSession.getBeginTime(), System.currentTimeMillis(),
                globalSession.getStatus()));

        LOGGER.info("Global[{}] committing is successfully done.", globalSession.getXid());

    }

    private void asyncCommit(GlobalSession globalSession) throws TransactionException {
        globalSession.addSessionLifecycleListener(SessionHolder.getAsyncCommittingSessionManager());
        SessionHolder.getAsyncCommittingSessionManager().addGlobalSession(globalSession);
        globalSession.changeStatus(GlobalStatus.AsyncCommitting);
    }

    private void queueToRetryCommit(GlobalSession globalSession) throws TransactionException {
        globalSession.addSessionLifecycleListener(SessionHolder.getRetryCommittingSessionManager());
        SessionHolder.getRetryCommittingSessionManager().addGlobalSession(globalSession);
        globalSession.changeStatus(GlobalStatus.CommitRetrying);
    }

    private void queueToRetryRollback(GlobalSession globalSession) throws TransactionException {
        globalSession.addSessionLifecycleListener(SessionHolder.getRetryRollbackingSessionManager());
        SessionHolder.getRetryRollbackingSessionManager().addGlobalSession(globalSession);
        GlobalStatus currentStatus = globalSession.getStatus();
        if (SessionHelper.isTimeoutGlobalStatus(currentStatus)) {
            globalSession.changeStatus(GlobalStatus.TimeoutRollbackRetrying);
        } else {
            globalSession.changeStatus(GlobalStatus.RollbackRetrying);
        }
    }

    @Override
    public void doGlobalRollback(GlobalSession globalSession, boolean retrying) throws TransactionException {
        //start rollback event
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                globalSession.getTransactionName(), globalSession.getBeginTime(), null, globalSession.getStatus()));

        for (BranchSession branchSession : globalSession.getReverseSortedBranches()) {
            BranchStatus currentBranchStatus = branchSession.getStatus();
            if (currentBranchStatus == BranchStatus.PhaseOne_Failed) {
                globalSession.removeBranch(branchSession);
                continue;
            }
            try {
                BranchStatus branchStatus = branchRollback(branchSession.getBranchType(),
                        branchSession.getXid(), branchSession.getBranchId(), branchSession.getResourceId(),
                        branchSession.getApplicationData());

                switch (branchStatus) {
                    case PhaseTwo_Rollbacked:
                        globalSession.removeBranch(branchSession);
                        LOGGER.info("Successfully rollback branch xid={} branchId={}", globalSession.getXid(),
                                branchSession.getBranchId());
                        continue;
                    case PhaseTwo_RollbackFailed_Unretryable:
                        SessionHelper.endRollbackFailed(globalSession);
                        LOGGER.info("Failed to rollback branch and stop retry xid={} branchId={}",
                                globalSession.getXid(), branchSession.getBranchId());
                        return;
                    default:
                        LOGGER.info("Failed to rollback branch xid={} branchId={}", globalSession.getXid(),
                                branchSession.getBranchId());
                        if (!retrying) {
                            queueToRetryRollback(globalSession);
                        }
                        return;

                }
            } catch (Exception ex) {
                LOGGER.error("Exception rollbacking branch xid={} branchId={}", globalSession.getXid(),
                        branchSession.getBranchId(), ex);
                if (!retrying) {
                    queueToRetryRollback(globalSession);
                }
                throw new TransactionException(ex);
            }
        }

        //In db mode, there is a problem of inconsistent data in multiple copies, resulting in new branch
        // transaction registration when rolling back.
        //1. New branch transaction and rollback branch transaction have no data association
        //2. New branch transaction has data association with rollback branch transaction
        //The second query can solve the first problem, and if it is the second problem, it may cause a rollback
        // failure due to data changes.
        GlobalSession globalSessionTwice = SessionHolder.findGlobalSession(globalSession.getXid());
        if (globalSessionTwice != null && globalSessionTwice.hasBranch()) {
            LOGGER.info("Global[{}] rollbacking is NOT done.", globalSession.getXid());
            return;
        }

        SessionHelper.endRollbacked(globalSession);

        //rollbacked event
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                globalSession.getTransactionName(), globalSession.getBeginTime(), System.currentTimeMillis(),
                globalSession.getStatus()));

        LOGGER.info("Successfully rollback global, xid = {}", globalSession.getXid());
    }

    /**
     * remove all branches
     *
     * @param globalSession
     * @throws TransactionException
     */
    protected void removeAllBranches(GlobalSession globalSession) throws TransactionException {
        ArrayList<BranchSession> branchSessions = globalSession.getSortedBranches();
        for (BranchSession branchSession : branchSessions) {
            globalSession.removeBranch(branchSession);
        }
    }

    @Override
    public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        if (globalSession == null) {
            return globalStatus;
        }

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        return globalSession.getStatus();
    }

    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                     String applicationData) throws TransactionException {
        try {
            BranchCommitRequest request = new BranchCommitRequest();
            request.setXid(xid);
            request.setBranchId(branchId);
            request.setResourceId(resourceId);
            request.setApplicationData(applicationData);
            request.setBranchType(branchType);

            GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
            if (globalSession == null) {
                return BranchStatus.PhaseTwo_Committed;
            }
            BranchSession branchSession = globalSession.getBranch(branchId);
            if (null != branchSession) {
                BranchCommitResponse response = (BranchCommitResponse)messageSender.sendSyncRequest(resourceId,
                        branchSession.getClientId(), request);
                return response.getBranchStatus();
            } else {
                return BranchStatus.PhaseTwo_Committed;
            }
        } catch (IOException | TimeoutException e) {
            throw new BranchTransactionException(FailedToSendBranchCommitRequest,
                    String.format("Send branch commit failed, xid = %s branchId = %s", xid, branchId), e);
        }
    }

    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) throws TransactionException {
        try {
            BranchRollbackRequest request = new BranchRollbackRequest();
            request.setXid(xid);
            request.setBranchId(branchId);
            request.setResourceId(resourceId);
            request.setApplicationData(applicationData);
            request.setBranchType(branchType);

            GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
            if (globalSession == null) {
                return BranchStatus.PhaseTwo_Rollbacked;
            }

            if (BranchType.SAGA.equals(branchType)) {

                Map<String, Channel> channels = ChannelManager.getRmChannels();
                if (channels == null || channels.size() == 0) {
                    LOGGER.error(
                            "Failed to rollback SAGA global[" + globalSession.getXid() + ", RM channels is empty.");
                    return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
                }
                String sagaResourceId = globalSession.getApplicationId() + "#" + globalSession
                        .getTransactionServiceGroup();
                Channel sagaChannel = channels.get(sagaResourceId);
                if (sagaChannel == null) {
                    LOGGER.error("Failed to rollback SAGA global[" + globalSession.getXid()
                            + ", cannot find channel by resourceId[" + sagaResourceId + "]");
                    return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
                }
                BranchRollbackResponse response = (BranchRollbackResponse)messageSender.sendSyncRequest(sagaChannel,
                        request);
                return response.getBranchStatus();
            } else {

                BranchSession branchSession = globalSession.getBranch(branchId);

                BranchRollbackResponse response = (BranchRollbackResponse)messageSender.sendSyncRequest(resourceId,
                        branchSession.getClientId(), request);
                return response.getBranchStatus();
            }

        } catch (IOException | TimeoutException e) {
            throw new BranchTransactionException(FailedToSendBranchRollbackRequest,
                    String.format("Send branch rollback failed, xid = %s branchId = %s", xid, branchId), e);
        }
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.AT;
    }
}
