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
package io.seata.rm.datasource.lcn;

import java.sql.SQLException;
import java.util.List;
import io.seata.common.util.CollectionUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.rm.datasource.AbstractDataSourceCacheResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RM for LCN mode.
 *
 * @author funkye
 */
public class ResourceManagerLcn extends AbstractDataSourceCacheResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerLcn.class);

    @Override
    public void init() {
        LOGGER.info("ResourceManagerLcn init ...");

    }

    @Override
    public BranchType getBranchType() {
        return BranchType.LCN;
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

    private BranchStatus finishBranch(boolean committed, BranchType branchType, String xid, long branchId,
        String resourceId, String applicationData) throws TransactionException {
        List<ConnectionProxyLcn> connectionProxyLcnList = LcnXidBuilder.getConnectionList(xid);
        if (CollectionUtils.isNotEmpty(connectionProxyLcnList)) {
            try {
                for (ConnectionProxyLcn conn : connectionProxyLcnList) {
                    conn.policy(committed);
                }
                LcnXidBuilder.remove(xid);
            } catch (SQLException e) {
                if (committed) {
                    LOGGER.info(branchId + " commit failed since " + e.getMessage(), e);
                    // FIXME: case of PhaseTwo_CommitFailed_Unretryable
                    return BranchStatus.PhaseTwo_CommitFailed_Retryable;
                } else {
                    LOGGER.info(branchId + " rollback failed since " + e.getMessage(), e);
                    // FIXME: case of PhaseTwo_RollbackFailed_Unretryable
                    return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
                }
            }
        }
        return committed ? BranchStatus.PhaseTwo_Committed : BranchStatus.PhaseTwo_Rollbacked;
    }
}
