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
package io.seata.saga.tm;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.rpc.netty.RmNettyRemotingClient;
import io.seata.core.rpc.ShutdownHook;
import io.seata.core.rpc.netty.TmNettyRemotingClient;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.RMClient;
import io.seata.saga.rm.SagaResource;
import io.seata.tm.TMClient;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import io.seata.tm.api.TransactionalExecutor;
import io.seata.tm.api.TransactionalExecutor.ExecutionException;
import io.seata.tm.api.transaction.TransactionHook;
import io.seata.tm.api.transaction.TransactionHookManager;
import io.seata.tm.api.transaction.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Template of executing business logic with a global transaction for SAGA mode
 *
 * @author lorne.cl
 */
public class DefaultSagaTransactionalTemplate
    implements SagaTransactionalTemplate, ApplicationContextAware, DisposableBean, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSagaTransactionalTemplate.class);

    private String applicationId;
    private String txServiceGroup;
    private ApplicationContext applicationContext;

    @Override
    public void commitTransaction(GlobalTransaction tx) throws TransactionalExecutor.ExecutionException {
        try {
            triggerBeforeCommit(tx);
            tx.commit();
            triggerAfterCommit(tx);
        } catch (TransactionException txe) {
            // 4.1 Failed to commit
            throw new TransactionalExecutor.ExecutionException(tx, txe, TransactionalExecutor.Code.CommitFailure);
        }
    }

    @Override
    public void rollbackTransaction(GlobalTransaction tx, Throwable ex)
        throws TransactionException, TransactionalExecutor.ExecutionException {
        triggerBeforeRollback(tx);
        tx.rollback();
        triggerAfterRollback(tx);
        // Successfully rolled back
    }

    @Override
    public GlobalTransaction beginTransaction(TransactionInfo txInfo) throws TransactionalExecutor.ExecutionException {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        try {
            triggerBeforeBegin(tx);
            tx.begin(txInfo.getTimeOut(), txInfo.getName());
            triggerAfterBegin(tx);
        } catch (TransactionException txe) {
            throw new TransactionalExecutor.ExecutionException(tx, txe, TransactionalExecutor.Code.BeginFailure);

        }
        return tx;
    }

    @Override
    public GlobalTransaction reloadTransaction(String xid) throws ExecutionException, TransactionException {
        return GlobalTransactionContext.reload(xid);
    }

    @Override
    public void reportTransaction(GlobalTransaction tx, GlobalStatus globalStatus)
        throws TransactionalExecutor.ExecutionException {
        try {
            tx.globalReport(globalStatus);
            triggerAfterCompletion(tx);
        } catch (TransactionException txe) {

            throw new TransactionalExecutor.ExecutionException(tx, txe, TransactionalExecutor.Code.ReportFailure);
        }
    }

    @Override
    public long branchRegister(String resourceId, String clientId, String xid, String applicationData, String lockKeys)
        throws TransactionException {
        return DefaultResourceManager.get().branchRegister(BranchType.SAGA, resourceId, clientId, xid, applicationData,
            lockKeys);
    }

    @Override
    public void branchReport(String xid, long branchId, BranchStatus status, String applicationData)
        throws TransactionException {
        DefaultResourceManager.get().branchReport(BranchType.SAGA, xid, branchId, status, applicationData);
    }

    protected void triggerBeforeBegin(GlobalTransaction tx) {
        TransactionHookManager.triggerHooks(tx.getXid(), hooks -> {
            for (TransactionHook hook : hooks) {
                try {
                    hook.beforeBegin();
                } catch (Exception e) {
                    LOGGER.error("Failed execute beforeBegin in hook {}", e.getMessage(), e);
                }
            }
        });
    }

    protected void triggerAfterBegin(GlobalTransaction tx) {
        TransactionHookManager.triggerHooks(tx.getXid(), hooks -> {
            for (TransactionHook hook : hooks) {
                try {
                    hook.afterBegin();
                } catch (Exception e) {
                    LOGGER.error("Failed execute afterBegin in hook {} ", e.getMessage(), e);
                }
            }
        });
    }

    protected void triggerBeforeRollback(GlobalTransaction tx) {
        TransactionHookManager.triggerHooks(tx.getXid(), hooks -> {
            for (TransactionHook hook : hooks) {
                try {
                    hook.beforeRollback();
                } catch (Exception e) {
                    LOGGER.error("Failed execute beforeRollback in hook {} ", e.getMessage(), e);
                }
            }
        });
    }

    protected void triggerAfterRollback(GlobalTransaction tx) {
        TransactionHookManager.triggerHooks(tx.getXid(), hooks -> {
            for (TransactionHook hook : hooks) {
                try {
                    hook.afterRollback();
                } catch (Exception e) {
                    LOGGER.error("Failed execute afterRollback in hook {}", e.getMessage(), e);
                }
            }
        });
    }

    protected void triggerBeforeCommit(GlobalTransaction tx) {
        TransactionHookManager.triggerHooks(tx.getXid(), hooks -> {
            for (TransactionHook hook : hooks) {
                try {
                    hook.beforeCommit();
                } catch (Exception e) {
                    LOGGER.error("Failed execute beforeCommit in hook {}", e.getMessage(), e);
                }
            }
        });
    }

    protected void triggerAfterCommit(GlobalTransaction tx) {
        TransactionHookManager.triggerHooks(tx.getXid(), hooks -> {
            for (TransactionHook hook : hooks) {
                try {
                    hook.afterCommit();
                } catch (Exception e) {
                    LOGGER.error("Failed execute afterCommit in hook {}", e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void triggerAfterCompletion(GlobalTransaction tx) {
        TransactionHookManager.triggerHooks(tx.getXid(), hooks -> {
            for (TransactionHook hook : hooks) {
                try {
                    hook.afterCompletion();
                } catch (Exception e) {
                    LOGGER.error("Failed execute afterCompletion in hook {}", e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initSeataClient();
    }

    @Override
    public void destroy() {
        ShutdownHook.getInstance().destroyAll();
    }

    private void initSeataClient() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Initializing Global Transaction Clients ... ");
        }
        if (io.seata.common.util.StringUtils.isNullOrEmpty(applicationId) || io.seata.common.util.StringUtils
            .isNullOrEmpty(txServiceGroup)) {
            throw new IllegalArgumentException(
                "applicationId: " + applicationId + ", txServiceGroup: " + txServiceGroup);
        }
        //init TM
        TMClient.init(applicationId, txServiceGroup);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "Transaction Manager Client is initialized. applicationId[" + applicationId + "] txServiceGroup["
                    + txServiceGroup + "]");
        }
        //init RM
        RMClient.init(applicationId, txServiceGroup);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "Resource Manager is initialized. applicationId[" + applicationId + "] txServiceGroup[" + txServiceGroup
                    + "]");
        }

        // Only register application as a saga resource
        SagaResource sagaResource = new SagaResource();
        sagaResource.setResourceGroupId(getTxServiceGroup());
        sagaResource.setApplicationId(getApplicationId());
        DefaultResourceManager.get().registerResource(sagaResource);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Global Transaction Clients are initialized. ");
        }
        registerSpringShutdownHook();

    }

    private void registerSpringShutdownHook() {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext)applicationContext).registerShutdownHook();
            ShutdownHook.removeRuntimeShutdownHook();
        }
        ShutdownHook.getInstance().addDisposable(TmNettyRemotingClient.getInstance(applicationId, txServiceGroup));
        ShutdownHook.getInstance().addDisposable(RmNettyRemotingClient.getInstance(applicationId, txServiceGroup));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void cleanUp() {
        TransactionHookManager.clear();
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTxServiceGroup() {
        return txServiceGroup;
    }

    public void setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
    }
}
