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

import io.seata.common.util.StringUtils;
import io.seata.core.exception.BranchTransactionException;
import io.seata.core.exception.GlobalTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.GlobalCondition;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import io.seata.server.store.TransactionStoreManager.LogOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * The type Abstract session manager.
 */
public abstract class AbstractSessionManager implements SessionManager, SessionLifecycleListener {

    //region Logger and Fields

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractSessionManager.class);

    /**
     * The Transaction store manager.
     */
    protected TransactionStoreManager transactionStoreManager;

    //endregion

    //region Constructor

    /**
     * Instantiates a new Session manager.
     */
    public AbstractSessionManager() {
    }

    /**
     * Instantiates a new Session manager.
     *
     * @param transactionStoreManager the transaction store manager
     */
    public AbstractSessionManager(TransactionStoreManager transactionStoreManager) {
        this.transactionStoreManager = transactionStoreManager;
    }

    //endregion

    //region Override SessionManager

    @Override
    public GlobalSession getGlobalSession(String xid, boolean withBranchSessions) {
        return this.transactionStoreManager.readSession(xid, withBranchSessions);
    }

    @Override
    public List<GlobalSession> findGlobalSessions(GlobalCondition condition, boolean withBranchSessions) {
        return this.transactionStoreManager.readSession(condition, withBranchSessions);
    }

    @Override
    public void addGlobalSession(GlobalSession session) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + this.getClass().getSimpleName() + "] SESSION[" + session + "] " + LogOperation.GLOBAL_ADD);
        }
        writeSession(LogOperation.GLOBAL_ADD, session);
    }

    @Override
    public void updateGlobalSession(GlobalSession session, GlobalStatus status) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + this.getClass().getSimpleName() + "] SESSION[" + session + "] " + LogOperation.GLOBAL_UPDATE);
        }
        if (status != null) {
            session.setStatus(status);
        }
        writeSession(LogOperation.GLOBAL_UPDATE, session);
    }

    @Override
    public void removeGlobalSession(GlobalSession session) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + this.getClass().getSimpleName() + "] SESSION[" + session + "] " + LogOperation.GLOBAL_REMOVE);
        }
        writeSession(LogOperation.GLOBAL_REMOVE, session);
    }

    @Override
    public void addBranchSession(GlobalSession session, BranchSession branchSession) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + this.getClass().getSimpleName() + "] SESSION[" + branchSession + "] " + LogOperation.BRANCH_ADD);
        }
        writeSession(LogOperation.BRANCH_ADD, branchSession);
    }

    @Override
    public void updateBranchSession(BranchSession branchSession, BranchStatus status, String applicationData)
        throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + this.getClass().getSimpleName() + "] SESSION[" + branchSession + "] " + LogOperation.BRANCH_UPDATE);
        }
        if (status != null) {
            branchSession.setStatus(status);
        }
        if (StringUtils.isNotBlank(applicationData)) {
            branchSession.setApplicationData(applicationData);
        }
        writeSession(LogOperation.BRANCH_UPDATE, branchSession);
    }

    @Override
    public void removeBranchSession(GlobalSession globalSession, BranchSession branchSession)
        throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + this.getClass().getSimpleName() + "] SESSION[" + branchSession + "] " + LogOperation.BRANCH_REMOVE);
        }
        writeSession(LogOperation.BRANCH_REMOVE, branchSession);
    }

    @Override
    public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException {
        return lockCallable.call();
    }

    //endregion

    //region Override SessionLifecycleListener

    @Override
    public void onBegin(GlobalSession globalSession) throws TransactionException {
        addGlobalSession(globalSession);
    }

    @Override
    public void onUpdate(GlobalSession globalSession, GlobalStatus status) throws TransactionException {
        updateGlobalSession(globalSession, status);
    }

    @Override
    public void onAddBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        addBranchSession(globalSession, branchSession);
    }

    @Override
    public void onUpdateBranch(GlobalSession globalSession, BranchSession branchSession, BranchStatus status,
                               String applicationData) throws TransactionException {
        updateBranchSession(branchSession, status, applicationData);
    }

    @Override
    public void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        removeBranchSession(globalSession, branchSession);
    }

    @Override
    public void onClose(GlobalSession globalSession) throws TransactionException {
        globalSession.setActive(false);
    }

    @Override
    public void onEnd(GlobalSession globalSession) throws TransactionException {
        removeGlobalSession(globalSession);
    }

    //endregion

    //region Private

    private void writeSession(LogOperation logOperation, SessionStorable sessionStorable) throws TransactionException {
        if (!transactionStoreManager.writeSession(logOperation, sessionStorable)) {
            if (LogOperation.GLOBAL_ADD.equals(logOperation)) {
                throw new GlobalTransactionException(TransactionExceptionCode.FailedWriteSession,
                    "Fail to store global session");
            } else if (LogOperation.GLOBAL_UPDATE.equals(logOperation)) {
                throw new GlobalTransactionException(TransactionExceptionCode.FailedWriteSession,
                    "Fail to update global session");
            } else if (LogOperation.GLOBAL_REMOVE.equals(logOperation)) {
                throw new GlobalTransactionException(TransactionExceptionCode.FailedWriteSession,
                    "Fail to remove global session");
            } else if (LogOperation.BRANCH_ADD.equals(logOperation)) {
                throw new BranchTransactionException(TransactionExceptionCode.FailedWriteSession,
                    "Fail to store branch session");
            } else if (LogOperation.BRANCH_UPDATE.equals(logOperation)) {
                throw new BranchTransactionException(TransactionExceptionCode.FailedWriteSession,
                    "Fail to update branch session");
            } else if (LogOperation.BRANCH_REMOVE.equals(logOperation)) {
                throw new BranchTransactionException(TransactionExceptionCode.FailedWriteSession,
                    "Fail to remove branch session");
            } else {
                throw new BranchTransactionException(TransactionExceptionCode.FailedWriteSession,
                    "Unknown LogOperation:" + logOperation.name());
            }
        }
    }

    //endregion

    //region Override Disposable

    @Override
    public void destroy() {
    }

    //endregion

    //region Gets and Sets

    /**
     * Sets transaction store manager.
     *
     * @param transactionStoreManager the transaction store manager
     */
    public void setTransactionStoreManager(TransactionStoreManager transactionStoreManager) {
        this.transactionStoreManager = transactionStoreManager;
    }

    //endregion
}
