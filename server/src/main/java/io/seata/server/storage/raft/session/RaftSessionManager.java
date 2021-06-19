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
package io.seata.server.storage.raft.session;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.alipay.sofa.jraft.Closure;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.raft.RaftServerFactory;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.AbstractSessionManager;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.file.session.FileSessionManager;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.storage.raft.RaftTaskUtil;


import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.ADD_GLOBAL_SESSION;
import static io.seata.core.raft.msg.RaftSyncMsg.MsgType.REMOVE_GLOBAL_SESSION;

/**
 * @author funkye
 */
@LoadLevel(name = "raft", scope = Scope.PROTOTYPE)
public class RaftSessionManager extends AbstractSessionManager {

    private FileSessionManager fileSessionManager;

    private String name;

    public RaftSessionManager(String name, FileSessionManager fileSessionManager) {
        this.fileSessionManager = fileSessionManager;
        this.name = name;
    }

    public void addGlobalSession(GlobalSession globalSession) throws TransactionException {
        if (RaftServerFactory.getInstance().isLeader()) {
            Closure closure = status -> {
                if (status.isOk()) {
                    try {
                        LOGGER.info("addGlobalSession,raftSessionManager:{}", name);
                        this.fileSessionManager.addGlobalSession(globalSession);
                    } catch (TransactionException e) {
                        e.printStackTrace();
                    }
                }
            };
            GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);
            RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(ADD_GLOBAL_SESSION, globalTransactionDO);
            RaftTaskUtil.createTask(closure, raftSyncMsg);
        } else {
            this.fileSessionManager.addGlobalSession(globalSession);
        }
    }

    @Override
    public GlobalSession findGlobalSession(String xid) {
        return this.fileSessionManager.findGlobalSession(xid);
    }

    @Override
    public GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
        return this.fileSessionManager.findGlobalSession(xid, withBranchSessions);
    }

    @Override
    public Collection<GlobalSession> allSessions() {
        return this.fileSessionManager.allSessions();
    }

    @Override
    public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
        return this.fileSessionManager.findGlobalSessions(condition);
    }

    @Override
    public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
        throws TransactionException {
        return this.fileSessionManager.lockAndExecute(globalSession, lockCallable);
    }

    @Override
    public void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status) throws TransactionException {
        this.fileSessionManager.updateGlobalSessionStatus(session, status);
    }

    @Override
    public void removeGlobalSession(GlobalSession session) throws TransactionException {
        this.fileSessionManager.removeGlobalSession(session);
    }

    @Override
    public void addBranchSession(GlobalSession session, BranchSession branchSession) throws TransactionException {
        this.fileSessionManager.addBranchSession(session, branchSession);
    }

    @Override
    public void onBegin(GlobalSession globalSession) throws TransactionException {
        this.addGlobalSession(globalSession);
    }

    @Override
    public void onStatusChange(GlobalSession globalSession, GlobalStatus globalStatus) throws TransactionException {
        this.updateGlobalSessionStatus(globalSession, globalStatus);
    }

    @Override
    public void onBranchStatusChange(GlobalSession globalSession, BranchSession branchSession,
        BranchStatus branchStatus) throws TransactionException {
        this.updateBranchSessionStatus(branchSession, branchStatus);
    }

    @Override
    public void onAddBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        this.addBranchSession(globalSession, branchSession);
    }

    @Override
    public void removeBranchSession(GlobalSession globalSession, BranchSession branchSession)
        throws TransactionException {
        this.fileSessionManager.removeBranchSession(globalSession, branchSession);
    }

    @Override
    public void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        this.removeBranchSession(globalSession, branchSession);
    }

    @Override
    public void onClose(GlobalSession globalSession) throws TransactionException {
        this.fileSessionManager.onClose(globalSession);
    }

    @Override
    public void onEnd(GlobalSession globalSession) throws TransactionException {
        if (RaftServerFactory.getInstance().isLeader()) {
            Closure closure = status -> {
                if (status.isOk()) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("onEnd,raftSessionManager:{}", name);
                        }
                        this.fileSessionManager.removeGlobalSession(globalSession);
                    } catch (TransactionException e) {
                        e.printStackTrace();
                    }
                }
            };
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO(globalSession.getXid());
            RaftSessionSyncMsg raftSyncMsg = new RaftSessionSyncMsg(REMOVE_GLOBAL_SESSION, globalTransactionDO);
            RaftTaskUtil.createTask(closure, raftSyncMsg);
        } else {
            this.removeGlobalSession(globalSession);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileSessionManager getFileSessionManager() {
        return fileSessionManager;
    }

    public void setFileSessionManager(FileSessionManager fileSessionManager) {
        this.fileSessionManager = fileSessionManager;
    }

    public Map<String, GlobalSession> getSessionMap() {
        return this.fileSessionManager.getSessionMap();
    }

}
