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
package io.seata.tm;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TmTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.AbstractTransactionRequest;
import io.seata.core.protocol.transaction.AbstractTransactionResponse;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalReportResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.netty.TmNettyRemotingClient;

import java.util.concurrent.TimeoutException;

import static io.seata.core.constants.ConfigurationKeys.STORE_MODE;
import static io.seata.core.constants.ConfigurationKeys.TX_SERVICE_GROUP;

/**
 * The type Default transaction manager.
 *
 * @author sharajava
 */
@LoadLevel(name = "defaultTM")
public class DefaultTransactionManager implements TransactionManager {

    private static final TransactionManager DIRECT_CONNECT_TC_STORE_TM;
    private static final String APPLICATION_ID;
    private static final String TRANSACTION_SERVICE_GROUP;

    static {
        Configuration config = ConfigurationFactory.getInstance();
        String storeMode = config.getConfig(STORE_MODE);
        if (StringUtils.isNotBlank(storeMode) && !"none".equalsIgnoreCase(storeMode) && !"file".equalsIgnoreCase(storeMode)) {
            DIRECT_CONNECT_TC_STORE_TM = EnhancedServiceLoader.load(TransactionManager.class, "defaultCore",
                    new Class[]{RemotingServer.class}, new Object[]{null});
            APPLICATION_ID = config.getConfig(ConfigurationKeys.APPLICATION_ID);
            TRANSACTION_SERVICE_GROUP = config.getConfig(TX_SERVICE_GROUP);
        } else {
            DIRECT_CONNECT_TC_STORE_TM = null;
            APPLICATION_ID = null;
            TRANSACTION_SERVICE_GROUP = null;
        }
    }


    @Override
    public String begin(String applicationId, String transactionServiceGroup, String name, int timeout)
        throws TransactionException {
        if (DIRECT_CONNECT_TC_STORE_TM != null) {
            if (StringUtils.isBlank(applicationId)) {
                applicationId = APPLICATION_ID;
            }
            if (StringUtils.isBlank(transactionServiceGroup)) {
                transactionServiceGroup = TRANSACTION_SERVICE_GROUP;
            }
            return DIRECT_CONNECT_TC_STORE_TM.begin(applicationId, transactionServiceGroup, name, timeout);
        }

        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName(name);
        request.setTimeout(timeout);
        GlobalBeginResponse response = (GlobalBeginResponse) syncCall(request);
        if (response.getResultCode() == ResultCode.Failed) {
            throw new TmTransactionException(TransactionExceptionCode.BeginFailed, response.getMsg());
        }
        return response.getXid();
    }

    @Override
    public GlobalStatus commit(String xid) throws TransactionException {
        GlobalCommitRequest globalCommit = new GlobalCommitRequest();
        globalCommit.setXid(xid);
        GlobalCommitResponse response = (GlobalCommitResponse) syncCall(globalCommit);
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus rollback(String xid) throws TransactionException {
        GlobalRollbackRequest globalRollback = new GlobalRollbackRequest();
        globalRollback.setXid(xid);
        GlobalRollbackResponse response = (GlobalRollbackResponse) syncCall(globalRollback);
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus getStatus(String xid) throws TransactionException {
        if (DIRECT_CONNECT_TC_STORE_TM != null) {
            return DIRECT_CONNECT_TC_STORE_TM.getStatus(xid);
        }

        GlobalStatusRequest queryGlobalStatus = new GlobalStatusRequest();
        queryGlobalStatus.setXid(xid);
        GlobalStatusResponse response = (GlobalStatusResponse) syncCall(queryGlobalStatus);
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
        if (DIRECT_CONNECT_TC_STORE_TM != null) {
            return DIRECT_CONNECT_TC_STORE_TM.globalReport(xid, globalStatus);
        }

        GlobalReportRequest globalReport = new GlobalReportRequest();
        globalReport.setXid(xid);
        globalReport.setGlobalStatus(globalStatus);
        GlobalReportResponse response = (GlobalReportResponse) syncCall(globalReport);
        return response.getGlobalStatus();
    }

    private AbstractTransactionResponse syncCall(AbstractTransactionRequest request) throws TransactionException {
        try {
            return (AbstractTransactionResponse) TmNettyRemotingClient.getInstance().sendSyncRequest(request);
        } catch (TimeoutException toe) {
            throw new TmTransactionException(TransactionExceptionCode.IO, "RPC timeout", toe);
        }
    }
}
