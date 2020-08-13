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
package io.seata.tc.transaction.saga;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import io.netty.channel.Channel;
import io.seata.core.exception.GlobalTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.core.rpc.RemotingServer;
import io.seata.tc.coordinator.AbstractCore;
import io.seata.tc.session.BranchSession;
import io.seata.tc.session.GlobalSession;
import io.seata.tc.session.SessionHelper;
import io.seata.tc.session.SessionHolder;

/**
 * The type saga core.
 *
 * @author ph3636
 */
public class SagaCore extends AbstractCore {

    public SagaCore(RemotingServer remotingServer) {
        super(remotingServer);
    }

    @Override
    public BranchType getHandleBranchType() {
        return BranchType.SAGA;
    }

    @Override
    public void globalSessionStatusCheck(GlobalSession globalSession) throws GlobalTransactionException {
        // SAGA type accept forward(retry) operation on timeout or commit fail, forward operation will register remaining branches
    }

    @Override
    public BranchStatus branchCommitSend(BranchCommitRequest request, GlobalSession globalSession,
                                         BranchSession branchSession) throws IOException, TimeoutException {
        Map<String, Channel> channels = ChannelManager.getRmChannels();
        if (channels == null || channels.size() == 0) {
            LOGGER.error("Failed to commit SAGA global[" + globalSession.getXid() + ", RM channels is empty.");
            return BranchStatus.PhaseTwo_CommitFailed_Retryable;
        }
        String sagaResourceId = getSagaResourceId(globalSession);
        Channel sagaChannel = channels.get(sagaResourceId);
        if (sagaChannel == null) {
            LOGGER.error("Failed to commit SAGA global[" + globalSession.getXid()
                    + ", cannot find channel by resourceId[" + sagaResourceId + "]");
            return BranchStatus.PhaseTwo_CommitFailed_Retryable;
        }
        BranchCommitResponse response = (BranchCommitResponse) remotingServer.sendSyncRequest(sagaChannel, request);
        return response.getBranchStatus();
    }

    @Override
    public BranchStatus branchRollbackSend(BranchRollbackRequest request, GlobalSession globalSession,
                                           BranchSession branchSession) throws IOException, TimeoutException {
        Map<String, Channel> channels = ChannelManager.getRmChannels();
        if (channels == null || channels.size() == 0) {
            LOGGER.error("Failed to rollback SAGA global[" + globalSession.getXid() + ", RM channels is empty.");
            return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        }
        String sagaResourceId = getSagaResourceId(globalSession);
        Channel sagaChannel = channels.get(sagaResourceId);
        if (sagaChannel == null) {
            LOGGER.error("Failed to rollback SAGA global[" + globalSession.getXid()
                    + ", cannot find channel by resourceId[" + sagaResourceId + "]");
            return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        }
        BranchRollbackResponse response = (BranchRollbackResponse) remotingServer.sendSyncRequest(sagaChannel, request);
        return response.getBranchStatus();
    }

    @Override
    public boolean doGlobalCommit(GlobalSession globalSession, boolean retrying) throws TransactionException {
        try {
            BranchStatus branchStatus = branchCommit(globalSession, SessionHelper.newBranch(BranchType.SAGA,
                    globalSession.getXid(), -1, getSagaResourceId(globalSession), globalSession.getStatus().name()));

            switch (branchStatus) {
                case PhaseTwo_Committed:
                    removeAllBranches(globalSession);
                    LOGGER.info("Successfully committed SAGA global[" + globalSession.getXid() + "]");
                    break;
                case PhaseTwo_Rollbacked:
                    LOGGER.info("Successfully rollbacked SAGA global[" + globalSession.getXid() + "]");
                    removeAllBranches(globalSession);
                    SessionHelper.endRollbacked(globalSession);
                    return false;
                case PhaseTwo_RollbackFailed_Retryable:
                    LOGGER.error("By [{}], failed to rollback SAGA global [{}], will retry later.", branchStatus,
                            globalSession.getXid());
                    SessionHolder.getRetryCommittingSessionManager().removeGlobalSession(globalSession);
                    globalSession.queueToRetryRollback();
                    return false;
                case PhaseOne_Failed:
                    LOGGER.error("By [{}], finish SAGA global [{}]", branchStatus, globalSession.getXid());
                    removeAllBranches(globalSession);
                    globalSession.changeStatus(GlobalStatus.Finished);
                    globalSession.end();
                    return false;
                case PhaseTwo_CommitFailed_Unretryable:
                    if (globalSession.canBeCommittedAsync()) {
                        LOGGER.error("By [{}], failed to commit SAGA global [{}]", branchStatus,
                                globalSession.getXid());
                        break;
                    } else {
                        SessionHelper.endCommitFailed(globalSession);
                        LOGGER.error("Finally, failed to commit SAGA global[{}]", globalSession.getXid());
                        return false;
                    }
                default:
                    if (!retrying) {
                        globalSession.queueToRetryCommit();
                        return false;
                    } else {
                        LOGGER.error("Failed to commit SAGA global[{}], will retry later.", globalSession.getXid());
                        return false;
                    }
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to commit global[" + globalSession.getXid() + "]", ex);

            if (!retrying) {
                globalSession.queueToRetryRollback();
            }
            throw new TransactionException(ex);
        }
        return true;
    }

    @Override
    public boolean doGlobalRollback(GlobalSession globalSession, boolean retrying) throws TransactionException {
        try {
            BranchStatus branchStatus = branchRollback(globalSession, SessionHelper.newBranch(BranchType.SAGA,
                    globalSession.getXid(), -1, getSagaResourceId(globalSession), globalSession.getStatus().name()));

            switch (branchStatus) {
                case PhaseTwo_Rollbacked:
                    removeAllBranches(globalSession);
                    LOGGER.info("Successfully rollbacked SAGA global[{}]",globalSession.getXid());
                    break;
                case PhaseTwo_RollbackFailed_Unretryable:
                    SessionHelper.endRollbackFailed(globalSession);
                    LOGGER.error("Failed to rollback SAGA global[{}]", globalSession.getXid());
                    return false;
                case PhaseTwo_CommitFailed_Retryable:
                    SessionHolder.getRetryRollbackingSessionManager().removeGlobalSession(globalSession);
                    globalSession.queueToRetryCommit();
                    LOGGER.warn("Retry by custom recover strategy [Forward] on timeout, SAGA global[{}]", globalSession.getXid());
                    return false;
                default:
                    LOGGER.error("Failed to rollback SAGA global[{}]", globalSession.getXid());
                    if (!retrying) {
                        globalSession.queueToRetryRollback();
                    }
                    return false;
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to rollback global[{}]", globalSession.getXid(), ex);
            if (!retrying) {
                globalSession.queueToRetryRollback();
            }
            throw new TransactionException(ex);
        }
        return true;
    }

    @Override
    public void doGlobalReport(GlobalSession globalSession, String xid, GlobalStatus globalStatus) throws TransactionException {
        if (GlobalStatus.Committed.equals(globalStatus)) {
            removeAllBranches(globalSession);
            SessionHelper.endCommitted(globalSession);
            LOGGER.info("Global[{}] committed", globalSession.getXid());
        } else if (GlobalStatus.Rollbacked.equals(globalStatus)
                || GlobalStatus.Finished.equals(globalStatus)) {
            removeAllBranches(globalSession);
            SessionHelper.endRollbacked(globalSession);
            LOGGER.info("Global[{}] rollbacked", globalSession.getXid());
        } else {
            globalSession.changeStatus(globalStatus);
            LOGGER.info("Global[{}] reporting is successfully done. status[{}]", globalSession.getXid(), globalSession.getStatus());

            if (GlobalStatus.RollbackRetrying.equals(globalStatus)
                    || GlobalStatus.TimeoutRollbackRetrying.equals(globalStatus)
                    || GlobalStatus.UnKnown.equals(globalStatus)) {
                globalSession.queueToRetryRollback();
                LOGGER.info("Global[{}] will retry rollback", globalSession.getXid());
            } else if (GlobalStatus.CommitRetrying.equals(globalStatus)) {
                globalSession.queueToRetryCommit();
                LOGGER.info("Global[{}] will retry commit", globalSession.getXid());
            }
        }
    }

    /**
     * remove all branches
     *
     * @param globalSession the globalSession
     * @throws TransactionException the TransactionException
     */
    private void removeAllBranches(GlobalSession globalSession) throws TransactionException {
        ArrayList<BranchSession> branchSessions = globalSession.getSortedBranches();
        for (BranchSession branchSession : branchSessions) {
            globalSession.removeBranch(branchSession);
        }
    }

    /**
     * get saga ResourceId
     *
     * @param globalSession the globalSession
     * @return sagaResourceId
     */
    private String getSagaResourceId(GlobalSession globalSession) {
        return globalSession.getApplicationId() + "#" + globalSession.getTransactionServiceGroup();
    }
}
