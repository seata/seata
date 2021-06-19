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
package io.seata.server.storage.raft.lock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.alipay.sofa.jraft.Closure;
import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.core.store.StoreMode;
import io.seata.server.lock.AbstractLockManager;
import io.seata.server.lock.LockManager;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.file.lock.FileLockManager;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.RaftTaskUtil;


import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.ACQUIRE_LOCK;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.RELEASE_GLOBAL_SESSION_LOCK;

/**
 * @author funkye
 */
@LoadLevel(name = "raft")
public class RaftLockManager extends AbstractLockManager {

    private static final FileLockManager FILE_LOCK_MANAGER =
        (FileLockManager)EnhancedServiceLoader.load(LockManager.class, StoreMode.FILE.getName());

    public static LockManager getFileLockManager() {
        return FILE_LOCK_MANAGER;
    }

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        BranchTransactionDO branchTransactionDO = SessionConverter.convertBranchTransactionDO(branchSession);
        RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(ACQUIRE_LOCK, branchTransactionDO);
        Closure closure = status -> {
            if (status.isOk()) {
                try {
                    completableFuture.complete(super.acquireLock(branchSession));
                } catch (TransactionException e) {
                    completableFuture.completeExceptionally(e);
                }
            }
        };
        RaftTaskUtil.createTask(closure, raftSyncMsg);
        try {
            return completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public Locker getLocker(BranchSession branchSession) {
        return FILE_LOCK_MANAGER.getLocker(branchSession);
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);
        RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(RELEASE_GLOBAL_SESSION_LOCK, globalTransactionDO);
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Closure closure = status -> {
            if (status.isOk()) {
                try {
                    completableFuture.complete(FILE_LOCK_MANAGER.releaseGlobalSessionLock(globalSession));
                } catch (TransactionException e) {
                    completableFuture.completeExceptionally(e);
                }
            }
        };
        RaftTaskUtil.createTask(closure, raftSyncMsg);
        try {
            return completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new StoreException(e);
        }
    }

}
