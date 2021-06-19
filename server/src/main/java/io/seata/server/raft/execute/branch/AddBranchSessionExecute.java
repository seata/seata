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
package io.seata.server.raft.execute.branch;

import io.seata.common.util.StringUtils;
import io.seata.core.model.BranchType;
import io.seata.core.store.BranchTransactionDO;
import io.seata.server.raft.execute.AbstractRaftMsgExecute;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.lock.RaftLockManager;

/**
 * @author jianbin.chen
 */
public class AddBranchSessionExecute extends AbstractRaftMsgExecute {

    public AddBranchSessionExecute(RaftSessionSyncMsg sessionSyncMsg) {
        super(sessionSyncMsg);
    }

    @Override
    public Boolean execute(Object... args) throws Throwable {
        BranchTransactionDO branchTransactionDO = sessionSyncMsg.getBranchSession();
        GlobalSession globalSession = raftSessionManager.findGlobalSession(branchTransactionDO.getXid());
        BranchSession branchSession = globalSession.getBranch(branchTransactionDO.getBranchId());
        if (branchSession == null) {
            branchSession = SessionConverter.convertBranchSession(branchTransactionDO);
            if (branchSession.getBranchType() == BranchType.AT && StringUtils.isNotBlank(branchSession.getLockKey())) {
                RaftLockManager.getFileLockManager().acquireLock(branchSession);
            }
            globalSession.add(branchSession);
            if (logger.isDebugEnabled()) {
                logger.debug("addBranch xid: {},branchId: {}", branchTransactionDO.getXid(),
                    branchTransactionDO.getBranchId());
            }
        }
        return true;
    }

}
