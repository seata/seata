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
package io.seata.rm;

import io.seata.core.exception.AbstractExceptionHandler;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.ResourceManager;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.transaction.AbstractTransactionRequestToRM;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.protocol.transaction.RMInboundHandler;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.rm.transaction.RMTransactionHookManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Abstract RM event handler
 *
 * @author sharajava
 */
public abstract class AbstractRMHandler extends AbstractExceptionHandler
    implements RMInboundHandler, TransactionMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRMHandler.class);

    @Override
    public BranchCommitResponse handle(BranchCommitRequest request) {
        BranchCommitResponse response = new BranchCommitResponse();
        exceptionHandleTemplate(new AbstractCallback<BranchCommitRequest, BranchCommitResponse>() {
            @Override
            public void execute(BranchCommitRequest request, BranchCommitResponse response)
                throws TransactionException {
                doBranchCommit(request, response);
            }
        }, request, response);
        return response;
    }

    @Override
    public BranchRollbackResponse handle(BranchRollbackRequest request) {
        BranchRollbackResponse response = new BranchRollbackResponse();
        exceptionHandleTemplate(new AbstractCallback<BranchRollbackRequest, BranchRollbackResponse>() {
            @Override
            public void execute(BranchRollbackRequest request, BranchRollbackResponse response)
                throws TransactionException {
                doBranchRollback(request, response);
            }
        }, request, response);
        return response;
    }

    /**
     * delete undo log
     * @param request the request
     */
    @Override
    public void handle(UndoLogDeleteRequest request) {
        // https://github.com/seata/seata/issues/2226
    }

    /**
     * Do branch commit.
     *
     * @param request  the request
     * @param response the response
     * @throws TransactionException the transaction exception
     */
    protected void doBranchCommit(BranchCommitRequest request, BranchCommitResponse response)
        throws TransactionException {
        BranchType branchType = request.getBranchType();
        String xid = request.getXid();
        long branchId = request.getBranchId();
        String resourceId = request.getResourceId();
        String applicationData = request.getApplicationData();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Branch Committing: {} {} {} {}", xid, branchId, resourceId, applicationData);
        }

        try {
            //trigger before branch commit hooks
            RMTransactionHookManager.triggerHooks(LOGGER, branchId, hook -> {
                hook.beforeBranchCommit(branchType, xid, branchId);
            });

            //do branch commit
            BranchStatus status = getResourceManager().branchCommit(branchType, xid, branchId, resourceId,
                applicationData);
            response.setXid(xid);
            response.setBranchId(branchId);
            response.setBranchStatus(status);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Branch Committed result: {}", status);
            }

            //trigger after branch commit hooks
            if (status == BranchStatus.PhaseTwo_Committed) {
                RMTransactionHookManager.triggerHooks(LOGGER, branchId, hook -> {
                    hook.afterBranchCommitted(branchType, xid, branchId);
                });
            } else {
                RMTransactionHookManager.triggerHooks(LOGGER, branchId, hook -> {
                    hook.afterBranchCommitFailed(branchType, xid, branchId, status);
                });
            }

            //if committed or unretryable, remove branch hooks
            if (status == BranchStatus.PhaseTwo_Committed || status == BranchStatus.PhaseTwo_CommitFailed_Unretryable) {
                RMTransactionHookManager.removeBranchHooks(branchId);
            }
        } catch (Exception e) {
            //trigger after branch commit failed hooks
            RMTransactionHookManager.triggerHooks(LOGGER, branchId, hook -> {
                hook.afterBranchCommitException(branchType, xid, branchId, e);
            });

            // throw the exception after finished hooks
            throw e;
        }
    }

    /**
     * Do branch rollback.
     *
     * @param request  the request
     * @param response the response
     * @throws TransactionException the transaction exception
     */
    protected void doBranchRollback(BranchRollbackRequest request, BranchRollbackResponse response)
        throws TransactionException {
        BranchType branchType = request.getBranchType();
        String xid = request.getXid();
        long branchId = request.getBranchId();
        String resourceId = request.getResourceId();
        String applicationData = request.getApplicationData();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Branch Rollbacking: {} {} {} {}", xid, branchId, resourceId, applicationData);
        }

        try {
            //trigger before branch rollback hooks
            RMTransactionHookManager.triggerHooks(LOGGER, branchId, hook -> {
                hook.beforeBranchRollback(branchType, xid, branchId);
            });

            //do branch rollback
            BranchStatus status = getResourceManager().branchRollback(branchType, xid, branchId, resourceId,
                applicationData);
            response.setXid(xid);
            response.setBranchId(branchId);
            response.setBranchStatus(status);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Branch Rollbacked result: {}", status);
            }

            //trigger after branch rollback hooks
            if (status == BranchStatus.PhaseTwo_Rollbacked) {
                RMTransactionHookManager.triggerHooks(LOGGER, branchId, hook -> {
                    hook.afterBranchRollbacked(branchType, xid, branchId);
                });
            } else {
                RMTransactionHookManager.triggerHooks(LOGGER, branchId, hook -> {
                    hook.afterBranchRollbackFailed(branchType, xid, branchId, status);
                });
            }

            //if rollbacked or unretryable, remove branch hooks
            if (status == BranchStatus.PhaseTwo_Rollbacked || status == BranchStatus.PhaseTwo_RollbackFailed_Unretryable) {
                RMTransactionHookManager.removeBranchHooks(branchId);
            }
        } catch (Exception e) {
            //trigger after branch rollback failed hooks
            RMTransactionHookManager.triggerHooks(LOGGER, branchId, hook -> {
                hook.afterBranchRollbackException(branchType, xid, branchId, e);
            });

            // throw the exception after finished hooks
            throw e;
        }
    }

    /**
     * get resource manager implement
     *
     * @return
     */
    protected abstract ResourceManager getResourceManager();

    @Override
    public AbstractResultMessage onRequest(AbstractMessage request, RpcContext context) {
        if (!(request instanceof AbstractTransactionRequestToRM)) {
            throw new IllegalArgumentException();
        }
        AbstractTransactionRequestToRM transactionRequest = (AbstractTransactionRequestToRM)request;
        transactionRequest.setRMInboundMessageHandler(this);

        return transactionRequest.handle(context);
    }

    @Override
    public void onResponse(AbstractResultMessage response, RpcContext context) {
        LOGGER.info("the rm client received response msg [{}] from tc server.", response.toString());
    }

    public abstract BranchType getBranchType();
}
