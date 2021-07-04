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

import io.seata.core.exception.BranchTransactionException;
import io.seata.core.exception.GlobalTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.LockStatus;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import io.seata.server.store.TransactionStoreManager.LogOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract session manager.
 */
public abstract class AbstractSessionManager implements SessionManager, SessionLifecycleListener {

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractSessionManager.class);

    /**
     * The Transaction store manager.
     */
    protected TransactionStoreManager transactionStoreManager;

    /**
     * The Name.
     */
    protected String name;

    /**
     * Instantiates a new Abstract session manager.
     */
    public AbstractSessionManager() {
    }

    /**
     * Instantiates a new Abstract session manager.
     *
     * @param name the name
     */
    public AbstractSessionManager(String name) {
        this.name = name;
    }

    @Override
    public void addGlobalSession(GlobalSession session) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + session + "] " + LogOperation.GLOBAL_ADD);
        }
        writeSession(LogOperation.GLOBAL_ADD, session);
    }

    @Override
    public void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + session + "] " + LogOperation.GLOBAL_UPDATE);
        }
        if (GlobalStatus.Rollbacking == status) {
            session.getBranchSessions().forEach(i -> i.setLockStatus(LockStatus.Rollbacking));
        }
        writeSession(LogOperation.GLOBAL_UPDATE, session);
    }

    @Override
    public void removeGlobalSession(GlobalSession session) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + session + "] " + LogOperation.GLOBAL_REMOVE);
        }
        writeSession(LogOperation.GLOBAL_REMOVE, session);
    }

    @Override
    public void addBranchSession(GlobalSession session, BranchSession branchSession) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + branchSession + "] " + LogOperation.BRANCH_ADD);
        }
        writeSession(LogOperation.BRANCH_ADD, branchSession);
    }

    @Override
    public void updateBranchSessionStatus(BranchSession branchSession, BranchStatus status)
        throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + branchSession + "] " + LogOperation.BRANCH_UPDATE);
        }
        writeSession(LogOperation.BRANCH_UPDATE, branchSession);
    }

    @Override
    public void removeBranchSession(GlobalSession globalSession, BranchSession branchSession)
        throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + branchSession + "] " + LogOperation.BRANCH_REMOVE);
        }
        writeSession(LogOperation.BRANCH_REMOVE, branchSession);
    }

    @Override
    public void onBegin(GlobalSession globalSession) throws TransactionException {
        addGlobalSession(globalSession);
    }

    @Override
    public void onStatusChange(GlobalSession globalSession, GlobalStatus status) throws TransactionException {
        updateGlobalSessionStatus(globalSession, status);
    }

    @Override
    public void onBranchStatusChange(GlobalSession globalSession, BranchSession branchSession, BranchStatus status)
        throws TransactionException {
        updateBranchSessionStatus(branchSession, status);
    }

    @Override
    public void onAddBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        addBranchSession(globalSession, branchSession);
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

    @Override
    public void destroy() {
    }

    /**
     * Sets transaction store manager.
     *
     * @param transactionStoreManager the transaction store manager
     */
    public void setTransactionStoreManager(TransactionStoreManager transactionStoreManager) {
        this.transactionStoreManager = transactionStoreManager;
    }
}
