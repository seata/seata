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

import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.server.raft.execute.AbstractRaftMsgExecute;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.raft.RaftSessionSyncMsg;


import static io.seata.core.model.GlobalStatus.AsyncCommitting;
import static io.seata.core.model.GlobalStatus.CommitRetrying;
import static io.seata.core.model.GlobalStatus.Committing;

/**
 * @author jianbin.chen
 */
public class RemoveBranchSessionExecute extends AbstractRaftMsgExecute {

    public RemoveBranchSessionExecute(RaftSessionSyncMsg sessionSyncMsg) {
        super(sessionSyncMsg);
    }

    @Override
    public Boolean execute(Object... args) throws Throwable {
        GlobalSession globalSession = raftSessionManager.findGlobalSession(sessionSyncMsg.getBranchSession().getXid());
        if (globalSession != null) {
            GlobalStatus status = globalSession.getStatus();
            BranchSession branchSession = globalSession.getBranch(sessionSyncMsg.getBranchSession().getBranchId());
            if (status != Committing && status != CommitRetrying && status != AsyncCommitting) {
                if (!branchSession.unlock()) {
                    throw new TransactionException("Unlock branch lock failed, xid = " + branchSession.getXid()
                        + ", branchId = " + branchSession.getBranchId());
                }
            }
            globalSession.remove(branchSession);
            if (logger.isDebugEnabled()) {
                logger.debug("removeBranch xid: {},branchId: {}", globalSession.getXid(),
                    sessionSyncMsg.getBranchSession().getBranchId());
            }
        }
        return true;
    }
}
