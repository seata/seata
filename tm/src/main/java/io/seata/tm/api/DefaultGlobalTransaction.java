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
package io.seata.tm.api;

import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.tm.TransactionManagerHolder;
import io.seata.tm.api.transaction.Propagation;
import io.seata.tm.api.transaction.SuspendedResourcesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.DefaultValues.DEFAULT_TM_COMMIT_RETRY_COUNT;
import static io.seata.common.DefaultValues.DEFAULT_TM_ROLLBACK_RETRY_COUNT;

/**
 * The type Default global transaction.
 *
 * @author sharajava
 */
public class DefaultGlobalTransaction implements GlobalTransaction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGlobalTransaction.class);

    private static final int DEFAULT_GLOBAL_TX_TIMEOUT = 60000;

    private static final String DEFAULT_GLOBAL_TX_NAME = "default";

    private TransactionManager transactionManager;

    private String xid;

    private GlobalStatus status;

    private GlobalTransactionRole role;

    private Propagation propagation;

    private SuspendedResourcesHolder suspendedResourcesHolder;

    private volatile boolean used;

    private static final int COMMIT_RETRY_COUNT = ConfigurationFactory.getInstance()
        .getInt(ConfigurationKeys.CLIENT_TM_COMMIT_RETRY_COUNT, DEFAULT_TM_COMMIT_RETRY_COUNT);

    private static final int ROLLBACK_RETRY_COUNT = ConfigurationFactory.getInstance()
        .getInt(ConfigurationKeys.CLIENT_TM_ROLLBACK_RETRY_COUNT, DEFAULT_TM_ROLLBACK_RETRY_COUNT);

    /**
     * Instantiates a new Default global transaction.
     */
    DefaultGlobalTransaction() {
        this(null, GlobalStatus.UnKnown, GlobalTransactionRole.Launcher);
    }

    /**
     * Instantiates a new Default global transaction.
     *
     * @param xid
     *            the xid
     * @param status
     *            the status
     * @param role
     *            the role
     */
    DefaultGlobalTransaction(String xid, GlobalStatus status, GlobalTransactionRole role) {
        this.transactionManager = TransactionManagerHolder.get();
        this.xid = xid;
        this.status = status;
        this.role = role;
    }

    @Override
    public void begin() throws TransactionException {
        begin(DEFAULT_GLOBAL_TX_TIMEOUT);
    }

    @Override
    public void begin(int timeout) throws TransactionException {
        begin(timeout, DEFAULT_GLOBAL_TX_NAME);
    }

    @Override
    public void begin(int timeout, String name) throws TransactionException {
        begin(timeout, name, Propagation.REQUIRED);
    }

    @Override
    public void begin(int timeout, String name, Propagation propagation) throws TransactionException {
        assertInUsed();
        this.propagation = propagation;
        switch (propagation) {
            case NOT_SUPPORTED:
                if (existingTransaction()) {
                    suspendedResourcesHolder = suspend();
                }
                return;
            case REQUIRES_NEW:
                suspendedResourcesHolder = suspend();
                break;
            case SUPPORTS:
                if (!existingTransaction()) {
                    return;
                }
                break;
            case REQUIRED:
                break;
            case NEVER:
                if (existingTransaction()) {
                    throw new TransactionException(String.format(
                        "Existing transaction found for transaction marked with propagation 'never',xid = %s",
                        RootContext.getXID()));
                } else {
                    return;
                }
            case MANDATORY:
                if (!existingTransaction()) {
                    throw new TransactionException(
                        "No existing transaction found for transaction marked with propagation 'mandatory'");
                }
                break;
            default:
                throw new TransactionException("Not Supported Propagation:" + propagation);
        }
        if (role != GlobalTransactionRole.Launcher) {
            assertXIDNotNull();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore Begin(): just involved in global transaction [{}]", xid);
            }
            return;
        }
        assertXIDNull();
        if (RootContext.getXID() != null) {
            throw new IllegalStateException();
        }
        xid = transactionManager.begin(null, null, name, timeout);
        status = GlobalStatus.Begin;
        RootContext.bind(xid);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Begin new global transaction [{}]", xid);
        }
    }

    @Override
    public void commit() throws TransactionException {
        if (role == GlobalTransactionRole.Participant) {
            // Participant has no responsibility of committing
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore Commit(): just involved in global transaction [{}]", xid);
            }
            return;
        }
        try {
            if (StringUtils.isBlank(xid)) {
                if (Propagation.NOT_SUPPORTED.equals(this.propagation) || Propagation.SUPPORTS.equals(this.propagation)
                    || Propagation.NEVER.equals(this.propagation)) {
                    return;
                }
            }
            assertXIDNotNull();
            int retry = COMMIT_RETRY_COUNT <= 0 ? DEFAULT_TM_COMMIT_RETRY_COUNT : COMMIT_RETRY_COUNT;
            while (retry > 0) {
                try {
                    status = transactionManager.commit(xid);
                    break;
                } catch (Throwable ex) {
                    LOGGER.error("Failed to report global commit [{}],Retry Countdown: {}, reason: {}", this.getXid(),
                        retry, ex.getMessage());
                    retry--;
                    if (retry == 0) {
                        throw new TransactionException("Failed to report global commit", ex);
                    }
                }
            }
        } finally {
            if (RootContext.getXID() != null && xid.equals(RootContext.getXID())) {
                suspend();
            }
            resume(suspendedResourcesHolder);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}] commit status: {}", xid, status);
        }
    }

    @Override
    public void rollback() throws TransactionException {
        if (role == GlobalTransactionRole.Participant) {
            // Participant has no responsibility of rollback
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore Rollback(): just involved in global transaction [{}]", xid);
            }
            return;
        }
        try {
            if (StringUtils.isBlank(xid)) {
                if (Propagation.NOT_SUPPORTED.equals(this.propagation) || Propagation.SUPPORTS.equals(this.propagation)
                    || Propagation.NEVER.equals(this.propagation)) {
                    return;
                }
            }
            assertXIDNotNull();
            int retry = ROLLBACK_RETRY_COUNT <= 0 ? DEFAULT_TM_ROLLBACK_RETRY_COUNT : ROLLBACK_RETRY_COUNT;
            while (retry > 0) {
                try {
                    status = transactionManager.rollback(xid);
                    break;
                } catch (Throwable ex) {
                    LOGGER.error("Failed to report global rollback [{}],Retry Countdown: {}, reason: {}", this.getXid(),
                        retry, ex.getMessage());
                    retry--;
                    if (retry == 0) {
                        throw new TransactionException("Failed to report global rollback", ex);
                    }
                }
            }
        } finally {
            if (RootContext.getXID() != null && xid.equals(RootContext.getXID())) {
                suspend();
            }
            resume(suspendedResourcesHolder);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}] rollback status: {}", xid, status);
        }
    }

    @Override
    public SuspendedResourcesHolder suspend() throws TransactionException {
        String xid = RootContext.unbind();
        if (xid != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Suspending current transaction, xid = {}", xid);
            }
            return new SuspendedResourcesHolder(xid);
        } else {
            return null;
        }
    }

    @Override
    public void resume(SuspendedResourcesHolder suspendedResourcesHolder) throws TransactionException {
        if (suspendedResourcesHolder == null) {
            return;
        }
        String xid = suspendedResourcesHolder.getXid();
        RootContext.bind(xid);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Resumimg the transaction,xid = {}", xid);
        }
    }

    @Override
    public GlobalStatus getStatus() throws TransactionException {
        if (xid == null) {
            return GlobalStatus.UnKnown;
        }
        status = transactionManager.getStatus(xid);
        return status;
    }

    @Override
    public String getXid() {
        return xid;
    }

    @Override
    public void globalReport(GlobalStatus globalStatus) throws TransactionException {
        assertXIDNotNull();

        if (globalStatus == null) {
            throw new IllegalStateException();
        }

        status = transactionManager.globalReport(xid, globalStatus);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}] report status: {}", xid, status);
        }

        if (RootContext.getXID() != null && xid.equals(RootContext.getXID())) {
            suspend();
        }
    }

    @Override
    public GlobalStatus getLocalStatus() {
        return status;
    }

    private void assertXIDNotNull() {
        if (xid == null) {
            throw new IllegalStateException();
        }
    }

    private void assertXIDNull() {
        if (xid != null) {
            throw new IllegalStateException();
        }
    }

    public boolean existingTransaction() {
        return StringUtils.isNotEmpty(RootContext.getXID());
    }

    private void assertInUsed() {
        if (used) {
            throw new IllegalStateException("the same instance is allowed to be used only once");
        } else {
            used = true;
        }
    }

}
