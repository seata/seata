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
package io.seata.server;

import io.seata.common.exception.StoreException;
import io.seata.core.exception.AbstractExceptionHandler;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.transaction.AbstractGlobalEndRequest;
import io.seata.core.protocol.transaction.AbstractGlobalEndResponse;
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
import io.seata.core.protocol.transaction.TCInboundHandler;
import io.seata.core.rpc.RpcContext;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract tc inbound handler.
 *
 * @author sharajava
 */
public abstract class AbstractTCInboundHandler extends AbstractExceptionHandler implements TCInboundHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTCInboundHandler.class);

    @Override
    public GlobalBeginResponse handle(GlobalBeginRequest request, final RpcContext rpcContext) {
        GlobalBeginResponse response = new GlobalBeginResponse();
        exceptionHandleTemplate(new AbstractCallback<GlobalBeginRequest, GlobalBeginResponse>() {
            @Override
            public void execute(GlobalBeginRequest request, GlobalBeginResponse response) throws TransactionException {
                try {
                    doGlobalBegin(request, response, rpcContext);
                } catch (StoreException e) {
                    throw new TransactionException(TransactionExceptionCode.FailedStore,
                        String.format("begin global request failed. xid=%s, msg=%s", response.getXid(), e.getMessage()),
                        e);
                }
            }
        }, request, response);
        return response;
    }

    /**
     * Do global begin.
     *
     * @param request    the request
     * @param response   the response
     * @param rpcContext the rpc context
     * @throws TransactionException the transaction exception
     */
    protected abstract void doGlobalBegin(GlobalBeginRequest request, GlobalBeginResponse response,
                                          RpcContext rpcContext) throws TransactionException;

    @Override
    public GlobalCommitResponse handle(GlobalCommitRequest request, final RpcContext rpcContext) {
        GlobalCommitResponse response = new GlobalCommitResponse();
        response.setGlobalStatus(GlobalStatus.Committing);
        exceptionHandleTemplate(new AbstractCallback<GlobalCommitRequest, GlobalCommitResponse>() {
            @Override
            public void execute(GlobalCommitRequest request, GlobalCommitResponse response)
                throws TransactionException {
                try {
                    doGlobalCommit(request, response, rpcContext);
                } catch (StoreException e) {
                    throw new TransactionException(TransactionExceptionCode.FailedStore,
                        String.format("global commit request failed. xid=%s, msg=%s", request.getXid(), e.getMessage()),
                        e);
                }
            }
            @Override
            public void onTransactionException(GlobalCommitRequest request, GlobalCommitResponse response,
                                               TransactionException tex) {
                super.onTransactionException(request, response, tex);
                checkTransactionStatus(request, response);
            }

            @Override
            public void onException(GlobalCommitRequest request, GlobalCommitResponse response, Exception rex) {
                super.onException(request, response, rex);
                checkTransactionStatus(request, response);
            }


        }, request, response);
        return response;
    }

    /**
     * Do global commit.
     *
     * @param request    the request
     * @param response   the response
     * @param rpcContext the rpc context
     * @throws TransactionException the transaction exception
     */
    protected abstract void doGlobalCommit(GlobalCommitRequest request, GlobalCommitResponse response,
                                           RpcContext rpcContext) throws TransactionException;

    @Override
    public GlobalRollbackResponse handle(GlobalRollbackRequest request, final RpcContext rpcContext) {
        GlobalRollbackResponse response = new GlobalRollbackResponse();
        response.setGlobalStatus(GlobalStatus.Rollbacking);
        exceptionHandleTemplate(new AbstractCallback<GlobalRollbackRequest, GlobalRollbackResponse>() {
            @Override
            public void execute(GlobalRollbackRequest request, GlobalRollbackResponse response)
                throws TransactionException {
                try {
                    doGlobalRollback(request, response, rpcContext);
                } catch (StoreException e) {
                    throw new TransactionException(TransactionExceptionCode.FailedStore, String
                        .format("global rollback request failed. xid=%s, msg=%s", request.getXid(), e.getMessage()), e);
                }
            }

            @Override
            public void onTransactionException(GlobalRollbackRequest request, GlobalRollbackResponse response,
                                               TransactionException tex) {
                super.onTransactionException(request, response, tex);
                // may be appears StoreException outer layer method catch
                checkTransactionStatus(request, response);
            }

            @Override
            public void onException(GlobalRollbackRequest request, GlobalRollbackResponse response, Exception rex) {
                super.onException(request, response, rex);
                // may be appears StoreException outer layer method catch
                checkTransactionStatus(request, response);
            }
        }, request, response);
        return response;
    }

    /**
     * Do global rollback.
     *
     * @param request    the request
     * @param response   the response
     * @param rpcContext the rpc context
     * @throws TransactionException the transaction exception
     */
    protected abstract void doGlobalRollback(GlobalRollbackRequest request, GlobalRollbackResponse response,
                                             RpcContext rpcContext) throws TransactionException;

    @Override
    public BranchRegisterResponse handle(BranchRegisterRequest request, final RpcContext rpcContext) {
        BranchRegisterResponse response = new BranchRegisterResponse();
        exceptionHandleTemplate(new AbstractCallback<BranchRegisterRequest, BranchRegisterResponse>() {
            @Override
            public void execute(BranchRegisterRequest request, BranchRegisterResponse response)
                throws TransactionException {
                try {
                    doBranchRegister(request, response, rpcContext);
                } catch (StoreException e) {
                    throw new TransactionException(TransactionExceptionCode.FailedStore, String
                        .format("branch register request failed. xid=%s, msg=%s", request.getXid(), e.getMessage()), e);
                }
            }
        }, request, response);
        return response;
    }

    /**
     * Do branch register.
     *
     * @param request    the request
     * @param response   the response
     * @param rpcContext the rpc context
     * @throws TransactionException the transaction exception
     */
    protected abstract void doBranchRegister(BranchRegisterRequest request, BranchRegisterResponse response,
                                             RpcContext rpcContext) throws TransactionException;

    @Override
    public BranchReportResponse handle(BranchReportRequest request, final RpcContext rpcContext) {
        BranchReportResponse response = new BranchReportResponse();
        exceptionHandleTemplate(new AbstractCallback<BranchReportRequest, BranchReportResponse>() {
            @Override
            public void execute(BranchReportRequest request, BranchReportResponse response)
                throws TransactionException {
                try {
                    doBranchReport(request, response, rpcContext);
                } catch (StoreException e) {
                    throw new TransactionException(TransactionExceptionCode.FailedStore, String
                        .format("branch report request failed. xid=%s, branchId=%s, msg=%s", request.getXid(),
                            request.getBranchId(), e.getMessage()), e);
                }
            }
        }, request, response);
        return response;
    }

    /**
     * Do branch report.
     *
     * @param request    the request
     * @param rpcContext the rpc context
     * @throws TransactionException the transaction exception
     */
    protected abstract void doBranchReport(BranchReportRequest request, BranchReportResponse response,
                                           RpcContext rpcContext) throws TransactionException;

    @Override
    public GlobalLockQueryResponse handle(GlobalLockQueryRequest request, final RpcContext rpcContext) {
        GlobalLockQueryResponse response = new GlobalLockQueryResponse();
        exceptionHandleTemplate(new AbstractCallback<GlobalLockQueryRequest, GlobalLockQueryResponse>() {
            @Override
            public void execute(GlobalLockQueryRequest request, GlobalLockQueryResponse response)
                throws TransactionException {
                try {
                    doLockCheck(request, response, rpcContext);
                } catch (StoreException e) {
                    throw new TransactionException(TransactionExceptionCode.FailedStore, String
                        .format("global lock query request failed. xid=%s, msg=%s", request.getXid(), e.getMessage()),
                        e);
                }
            }
        }, request, response);
        return response;
    }

    /**
     * Do lock check.
     *
     * @param request    the request
     * @param response   the response
     * @param rpcContext the rpc context
     * @throws TransactionException the transaction exception
     */
    protected abstract void doLockCheck(GlobalLockQueryRequest request, GlobalLockQueryResponse response,
                                        RpcContext rpcContext) throws TransactionException;

    @Override
    public GlobalStatusResponse handle(GlobalStatusRequest request, final RpcContext rpcContext) {
        GlobalStatusResponse response = new GlobalStatusResponse();
        response.setGlobalStatus(GlobalStatus.UnKnown);
        exceptionHandleTemplate(new AbstractCallback<GlobalStatusRequest, GlobalStatusResponse>() {
            @Override
            public void execute(GlobalStatusRequest request, GlobalStatusResponse response)
                throws TransactionException {
                try {
                    doGlobalStatus(request, response, rpcContext);
                } catch (StoreException e) {
                    throw new TransactionException(TransactionExceptionCode.FailedStore,
                        String.format("global status request failed. xid=%s, msg=%s", request.getXid(), e.getMessage()),
                        e);
                }
            }

            @Override
            public void onTransactionException(GlobalStatusRequest request, GlobalStatusResponse response,
                                               TransactionException tex) {
                super.onTransactionException(request, response, tex);
                checkTransactionStatus(request, response);
            }

            @Override
            public void onException(GlobalStatusRequest request, GlobalStatusResponse response, Exception rex) {
                super.onException(request, response, rex);
                checkTransactionStatus(request, response);
            }
        }, request, response);
        return response;
    }

    /**
     * Do global status.
     *
     * @param request    the request
     * @param response   the response
     * @param rpcContext the rpc context
     * @throws TransactionException the transaction exception
     */
    protected abstract void doGlobalStatus(GlobalStatusRequest request, GlobalStatusResponse response,
                                           RpcContext rpcContext) throws TransactionException;

    @Override
    public GlobalReportResponse handle(GlobalReportRequest request, final RpcContext rpcContext) {
        GlobalReportResponse response = new GlobalReportResponse();
        response.setGlobalStatus(request.getGlobalStatus());
        exceptionHandleTemplate(new AbstractCallback<GlobalReportRequest, GlobalReportResponse>() {
            @Override
            public void execute(GlobalReportRequest request, GlobalReportResponse response)
                throws TransactionException {
                doGlobalReport(request, response, rpcContext);
            }
        }, request, response);
        return response;
    }

    /**
     * Do global report.
     *
     * @param request    the request
     * @param response   the response
     * @param rpcContext the rpc context
     * @throws TransactionException the transaction exception
     */
    protected abstract void doGlobalReport(GlobalReportRequest request, GlobalReportResponse response,
                                           RpcContext rpcContext) throws TransactionException;

    private void checkTransactionStatus(AbstractGlobalEndRequest request, AbstractGlobalEndResponse response) {
        try {
            GlobalSession globalSession = SessionHolder.getGlobalSession(request.getXid(), false);
            if (globalSession != null) {
                response.setGlobalStatus(globalSession.getStatus());
            } else {
                response.setGlobalStatus(GlobalStatus.Finished);
            }
        } catch (Exception exx) {
            LOGGER.error("check transaction status error,{}]", exx.getMessage());
        }
    }
}
