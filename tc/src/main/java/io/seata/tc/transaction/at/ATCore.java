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
package io.seata.tc.transaction.at;

import io.seata.core.exception.BranchTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.core.rpc.RemotingServer;
import io.seata.tc.coordinator.AbstractCore;
import io.seata.tc.session.BranchSession;
import io.seata.tc.session.GlobalSession;

import static io.seata.core.exception.TransactionExceptionCode.LockKeyConflict;

/**
 * The type at core.
 *
 * @author ph3636
 */
public class ATCore extends AbstractCore {

    public ATCore(RemotingServer remotingServer) {
        super(remotingServer);
    }

    @Override
    public BranchType getHandleBranchType() {
        return BranchType.AT;
    }

    @Override
    protected void branchSessionLock(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        if (!branchSession.lock()) {
            throw new BranchTransactionException(LockKeyConflict, String
                    .format("Global lock acquire failed xid = %s branchId = %s", globalSession.getXid(),
                            branchSession.getBranchId()));
        }
    }

    @Override
    protected void branchSessionUnlock(BranchSession branchSession) throws TransactionException {
        branchSession.unlock();
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
            throws TransactionException {
        return lockManager.isLockable(xid, resourceId, lockKeys);
    }
}
