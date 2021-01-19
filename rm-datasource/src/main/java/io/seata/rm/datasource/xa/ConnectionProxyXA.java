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
package io.seata.rm.datasource.xa;

import com.alibaba.druid.util.JdbcUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.rm.BaseDataSourceResource;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.transaction.RMTransactionHookManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection proxy for XA mode.
 *
 * @author sharajava
 */
public class ConnectionProxyXA extends AbstractConnectionProxyXA implements Holdable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionProxyXA.class);

    private boolean currentAutoCommitStatus = true;

    private XAXid xaBranchXid;

    private boolean xaActive = false;

    private boolean kept = false;

    /**
     * Constructor of Connection Proxy for XA mode.
     *
     * @param originalConnection Normal Connection from the original DataSource.
     * @param xaConnection XA Connection based on physical connection of the normal Connection above.
     * @param resource The corresponding Resource(DataSource proxy) from which the connections was created.
     * @param xid Seata global transaction xid.
     */
    public ConnectionProxyXA(Connection originalConnection, XAConnection xaConnection, BaseDataSourceResource resource, String xid) {
        super(originalConnection, xaConnection, resource, xid);
    }

    public void init() {
        try {
            this.xaResource = xaConnection.getXAResource();
            this.currentAutoCommitStatus = this.originalConnection.getAutoCommit();
            if (!currentAutoCommitStatus) {
                throw new IllegalStateException("Connection[autocommit=false] as default is NOT supported");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private void keepIfNecessary() {
        if (shouldBeHeld()) {
            resource.hold(xaBranchXid.toString(), this);
        }
    }

    private void releaseIfNecessary() {
        if (isHeld()) {
            resource.release(xaBranchXid.toString(), this);
        }
    }

    /**
     * XA commit
     * @param xid global transaction xid
     * @param branchId transaction branch id
     * @throws SQLException
     */
    public void xaCommit(String xid, long branchId, String applicationData) throws XAException {
        XAXid xaXid = XAXidBuilder.build(xid, branchId);
        xaResource.commit(xaXid, false);
        releaseIfNecessary();

    }

    /**
     * XA rollback
     * @param xid global transaction xid
     * @param branchId transaction branch id
     * @throws SQLException
     */
    public void xaRollback(String xid, long branchId, String applicationData) throws XAException {
        XAXid xaXid = XAXidBuilder.build(xid, branchId);
        xaResource.rollback(xaXid);
        releaseIfNecessary();

    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (currentAutoCommitStatus == autoCommit) {
            return;
        }
        if (autoCommit) {
            // According to JDBC spec:
            // If this method is called during a transaction and the
            // auto-commit mode is changed, the transaction is committed.
            if (xaActive) {
                commit();
            }
        } else {
            if (xaActive) {
                throw new SQLException("should NEVER happen: setAutoCommit from true to false while xa branch is active");
            }
            // Start a XA branch
            long branchId = 0L;
            try {
                // 1. register branch to TC then get the branchId
                branchId = DefaultResourceManager.get().branchRegister(BranchType.XA, resource.getResourceId(), null, xid, null,
                    null);
            } catch (TransactionException te) {
                cleanXABranchContext();
                throw new SQLException("failed to register xa branch " + xid + " since " + te.getCode() + ":" + te.getMessage(), te);
            }
            RMTransactionHookManager.setLocalBranchId(branchId);

            // 2. build XA-Xid with xid and branchId
            this.xaBranchXid = XAXidBuilder.build(xid, branchId);
            try {
                // 3. XA Start
                xaResource.start(this.xaBranchXid, XAResource.TMNOFLAGS);
            } catch (XAException e) {
                cleanXABranchContext();
                throw new SQLException("failed to start xa branch " + xid + " since " + e.getMessage(), e);
            }
            // 4. XA is active
            this.xaActive = true;

        }

        currentAutoCommitStatus = autoCommit;
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return currentAutoCommitStatus;
    }

    @Override
    public void commit() throws SQLException {
        if (currentAutoCommitStatus) {
            // Ignore the committing on an autocommit session.
            return;
        }
        if (!xaActive || this.xaBranchXid == null) {
            throw new SQLException("should NOT commit on an inactive session", SQLSTATE_XA_NOT_END);
        }
        try {
            // XA End: Success
            xaResource.end(xaBranchXid, XAResource.TMSUCCESS);
            // XA Prepare
            xaResource.prepare(xaBranchXid);
            // Keep the Connection if necessary
            keepIfNecessary();
        } catch (XAException xe) {
            try {
                // Branch Report to TC: Failed
                DefaultResourceManager.get().branchReport(BranchType.XA, xid, xaBranchXid.getBranchId(),
                    BranchStatus.PhaseOne_Failed, null);
            } catch (TransactionException te) {
                LOGGER.warn("Failed to report XA branch commit-failure on " + xid + "-" + xaBranchXid.getBranchId()
                    + " since " + te.getCode() + ":" + te.getMessage() + " and XAException:" + xe.getMessage());

            }
            throw new SQLException(
                "Failed to end(TMSUCCESS)/prepare xa branch on " + xid + "-" + xaBranchXid.getBranchId() + " since " + xe
                    .getMessage(), xe);
        } finally {
            cleanXABranchContext();
        }
    }

    @Override
    public void rollback() throws SQLException {
        if (currentAutoCommitStatus) {
            // Ignore the committing on an autocommit session.
            return;
        }
        if (!xaActive || this.xaBranchXid == null) {
            throw new SQLException("should NOT rollback on an inactive session");
        }
        try {
            // Branch Report to TC
            DefaultResourceManager.get().branchReport(BranchType.XA, xid, xaBranchXid.getBranchId(), BranchStatus.PhaseOne_Failed, null);

        } catch (TransactionException te) {
            // log and ignore the report failure
            LOGGER.warn("Failed to report XA branch rollback on " + xid + "-" + xaBranchXid.getBranchId()
                + " since " + te.getCode() + ":" + te.getMessage());
        }
        try {
            // XA End: Fail
            xaResource.end(xaBranchXid, XAResource.TMFAIL);
        } catch (XAException xe) {
            throw new SQLException(
                "Failed to end(TMFAIL) xa branch on " + xid + "-" + xaBranchXid.getBranchId() + " since " + xe
                    .getMessage(), xe);
        } finally {
            cleanXABranchContext();
        }
    }

    private void cleanXABranchContext() {
        xaActive = false;
        if (!isHeld()) {
            xaBranchXid = null;
        }
    }

    @Override
    public void close() throws SQLException {
        if (isHeld()) {
            // if kept by a keeper, just hold the connection.
            return;
        }
        cleanXABranchContext();
        originalConnection.close();
    }

    @Override
    public void setHeld(boolean kept) {
        this.kept = kept;
    }

    @Override
    public boolean isHeld() {
        return kept;
    }

    @Override
    public boolean shouldBeHeld() {
        return JdbcUtils.MYSQL.equals(resource.getDbType()) || JdbcUtils.MARIADB.equals(resource.getDbType());
    }

}
