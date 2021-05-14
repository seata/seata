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

import java.sql.SQLException;
import javax.transaction.xa.XAException;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.datasource.AbstractDataSourceCacheResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RM for XA mode.
 *
 * @author sharajava
 */
public class ResourceManagerXA extends AbstractDataSourceCacheResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerXA.class);

    @Override
    public void init() {
        LOGGER.info("ResourceManagerXA init ...");

    }

    @Override
    public BranchType getBranchType() {
        return BranchType.XA;
    }

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                     String applicationData) throws TransactionException {
        return finishBranch(true, branchType, xid, branchId, resourceId, applicationData);
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) throws TransactionException {
        return finishBranch(false, branchType, xid, branchId, resourceId, applicationData);
    }

    private BranchStatus finishBranch(boolean committed, BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) throws TransactionException {
        XAXid xaBranchXid = XAXidBuilder.build(xid, branchId);
        Resource resource = dataSourceCache.get(resourceId);
        if (resource instanceof AbstractDataSourceProxyXA) {
            try (ConnectionProxyXA connectionProxyXA = ((AbstractDataSourceProxyXA)resource).getConnectionForXAFinish(xaBranchXid)) {
                if (committed) {
                    connectionProxyXA.xaCommit(xid, branchId, applicationData);
                    LOGGER.info(xaBranchXid + " was committed.");
                    return BranchStatus.PhaseTwo_Committed;
                } else {
                    connectionProxyXA.xaRollback(xid, branchId, applicationData);
                    LOGGER.info(xaBranchXid + " was rollbacked");
                    return BranchStatus.PhaseTwo_Rollbacked;
                }
            } catch (XAException | SQLException sqle) {
                if (sqle instanceof XAException) {
                    int errorCode = ((XAException)sqle).errorCode;
                    // -3 An XA control connection could not be created; -4 The XA transaction has ended
                    boolean rationalFail =
                        errorCode == XAException.XAER_NOTA || errorCode == XAException.XAER_RMERR;
                    if (rationalFail) {
                        if (committed) {
                            return BranchStatus.PhaseTwo_Committed;
                        } else {
                            return BranchStatus.PhaseTwo_Rollbacked;
                        }
                    }
                }
                if (committed) {
                    LOGGER.info(xaBranchXid + " commit failed since " + sqle.getMessage(), sqle);
                    // FIXME: case of PhaseTwo_CommitFailed_Unretryable
                    return BranchStatus.PhaseTwo_CommitFailed_Retryable;
                } else {
                    LOGGER.info(xaBranchXid + " rollback failed since " + sqle.getMessage(), sqle);
                    // FIXME: case of PhaseTwo_RollbackFailed_Unretryable
                    return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
                }
            }
        } else {
            LOGGER.error("Unknown Resource for XA resource " + resourceId + " " + resource);
            if (committed) {
                return BranchStatus.PhaseTwo_CommitFailed_Unretryable;
            } else {
                return BranchStatus.PhaseTwo_RollbackFailed_Unretryable;
            }
        }
    }
}
