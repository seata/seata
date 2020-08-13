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
package io.seata.tc.storage.file.session;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.tc.session.AbstractSessionManager;
import io.seata.tc.session.BranchSession;
import io.seata.tc.session.GlobalSession;
import io.seata.tc.session.Reloadable;
import io.seata.tc.session.SessionCondition;
import io.seata.tc.storage.file.ReloadableStore;
import io.seata.tc.storage.file.TransactionWriteStore;
import io.seata.tc.storage.file.store.FileTransactionStoreManager;
import io.seata.tc.store.AbstractTransactionStoreManager;
import io.seata.tc.store.SessionStorable;
import io.seata.tc.store.TransactionStoreManager;
import io.seata.common.loader.Scope;


/**
 * The type File based session manager.
 *
 * @author slievrly
 */
@LoadLevel(name = "file", scope = Scope.PROTOTYPE)
public class FileSessionManager extends AbstractSessionManager implements Reloadable {

    private static final int READ_SIZE = ConfigurationFactory.getInstance().getInt(
        ConfigurationKeys.SERVICE_SESSION_RELOAD_READ_SIZE, 100);
    /**
     * The Session map.
     */
    private Map<String, GlobalSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * Instantiates a new File based session manager.
     *
     * @param name                 the name
     * @param sessionStoreFilePath the session store file path
     * @throws IOException the io exception
     */
    public FileSessionManager(String name, String sessionStoreFilePath) throws IOException {
        super(name);
        if (StringUtils.isNotBlank(sessionStoreFilePath)) {
            transactionStoreManager = new FileTransactionStoreManager(
                    sessionStoreFilePath + File.separator + name, this);
        } else {
            transactionStoreManager = new AbstractTransactionStoreManager() {
                @Override
                public boolean writeSession(LogOperation logOperation, SessionStorable session) {
                    return true;
                }
            };
        }
    }

    @Override
    public void reload() {
        restoreSessions();
        washSessions();
    }

    @Override
    public void addGlobalSession(GlobalSession session) throws TransactionException {
        super.addGlobalSession(session);
        sessionMap.put(session.getXid(), session);
    }

    @Override
    public GlobalSession findGlobalSession(String xid)  {
        return sessionMap.get(xid);
    }

    @Override
    public GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
        // withBranchSessions without process in memory
        return sessionMap.get(xid);
    }

    @Override
    public void removeGlobalSession(GlobalSession session) throws TransactionException {
        super.removeGlobalSession(session);
        sessionMap.remove(session.getXid());
    }

    @Override
    public Collection<GlobalSession> allSessions() {
        return sessionMap.values();
    }

    @Override
    public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
        List<GlobalSession> found = new ArrayList<>();
        for (GlobalSession globalSession : sessionMap.values()) {
            if (System.currentTimeMillis() - globalSession.getBeginTime() > condition.getOverTimeAliveMills()) {
                found.add(globalSession);
            }
        }
        return found;
    }

    @Override
    public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException {
        globalSession.lock();
        try {
            return lockCallable.call();
        } finally {
            globalSession.unlock();
        }
    }

    private void restoreSessions() {
        Map<Long, BranchSession> unhandledBranchBuffer = new HashMap<>();

        restoreSessions(true, unhandledBranchBuffer);
        restoreSessions(false, unhandledBranchBuffer);

        if (!unhandledBranchBuffer.isEmpty()) {
            unhandledBranchBuffer.values().forEach(branchSession -> {
                String xid = branchSession.getXid();
                long bid = branchSession.getBranchId();
                GlobalSession found = sessionMap.get(xid);
                if (found == null) {
                    // Ignore
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("GlobalSession Does Not Exists For BranchSession [" + bid + "/" + xid + "]");
                    }
                } else {
                    BranchSession existingBranch = found.getBranch(branchSession.getBranchId());
                    if (existingBranch == null) {
                        found.add(branchSession);
                    } else {
                        existingBranch.setStatus(branchSession.getStatus());
                    }
                }

            });
        }
    }

    private void washSessions() {
        if (sessionMap.size() > 0) {
            Iterator<Map.Entry<String, GlobalSession>> iterator = sessionMap.entrySet().iterator();
            while (iterator.hasNext()) {
                GlobalSession globalSession = iterator.next().getValue();

                GlobalStatus globalStatus = globalSession.getStatus();
                switch (globalStatus) {
                    case UnKnown:
                    case Committed:
                    case CommitFailed:
                    case Rollbacked:
                    case RollbackFailed:
                    case TimeoutRollbacked:
                    case TimeoutRollbackFailed:
                    case Finished:
                        // Remove all sessions finished
                        iterator.remove();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void restoreSessions(boolean isHistory, Map<Long, BranchSession> unhandledBranchBuffer) {
        if (!(transactionStoreManager instanceof ReloadableStore)) {
            return;
        }
        while (((ReloadableStore)transactionStoreManager).hasRemaining(isHistory)) {
            List<TransactionWriteStore> stores = ((ReloadableStore)transactionStoreManager).readWriteStore(READ_SIZE,
                isHistory);
            restore(stores, unhandledBranchBuffer);
        }
    }

    private void restore(List<TransactionWriteStore> stores, Map<Long, BranchSession> unhandledBranchSessions) {
        for (TransactionWriteStore store : stores) {
            TransactionStoreManager.LogOperation logOperation = store.getOperate();
            SessionStorable sessionStorable = store.getSessionRequest();
            switch (logOperation) {
                case GLOBAL_ADD:
                case GLOBAL_UPDATE: {
                    GlobalSession globalSession = (GlobalSession)sessionStorable;
                    if (globalSession.getTransactionId() == 0) {
                        LOGGER.error(
                            "Restore globalSession from file failed, the transactionId is zero , xid:" + globalSession
                                .getXid());
                        break;
                    }
                    GlobalSession foundGlobalSession = sessionMap.get(globalSession.getXid());
                    if (foundGlobalSession == null) {
                        sessionMap.put(globalSession.getXid(), globalSession);
                    } else {
                        foundGlobalSession.setStatus(globalSession.getStatus());
                    }
                    break;
                }
                case GLOBAL_REMOVE: {
                    GlobalSession globalSession = (GlobalSession)sessionStorable;
                    if (globalSession.getTransactionId() == 0) {
                        LOGGER.error(
                            "Restore globalSession from file failed, the transactionId is zero , xid:" + globalSession
                                .getXid());
                        break;
                    }
                    if (sessionMap.remove(globalSession.getXid()) == null) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("GlobalSession To Be Removed Does Not Exists [" + globalSession.getXid() + "]");
                        }
                    }
                    break;
                }
                case BRANCH_ADD:
                case BRANCH_UPDATE: {
                    BranchSession branchSession = (BranchSession)sessionStorable;
                    if (branchSession.getTransactionId() == 0) {
                        LOGGER.error(
                            "Restore branchSession from file failed, the transactionId is zero , xid:" + branchSession
                                .getXid());
                        break;
                    }
                    GlobalSession foundGlobalSession = sessionMap.get(branchSession.getXid());
                    if (foundGlobalSession == null) {
                        unhandledBranchSessions.put(branchSession.getBranchId(), branchSession);
                    } else {
                        BranchSession existingBranch = foundGlobalSession.getBranch(branchSession.getBranchId());
                        if (existingBranch == null) {
                            foundGlobalSession.add(branchSession);
                        } else {
                            existingBranch.setStatus(branchSession.getStatus());
                        }
                    }
                    break;
                }
                case BRANCH_REMOVE: {
                    BranchSession branchSession = (BranchSession)sessionStorable;
                    String xid = branchSession.getXid();
                    long bid = branchSession.getBranchId();
                    if (branchSession.getTransactionId() == 0) {
                        LOGGER.error(
                            "Restore branchSession from file failed, the transactionId is zero , xid:" + branchSession
                                .getXid());
                        break;
                    }
                    GlobalSession found = sessionMap.get(xid);
                    if (found == null) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(
                                "GlobalSession To Be Updated (Remove Branch) Does Not Exists [" + bid + "/" + xid
                                    + "]");
                        }
                    } else {
                        BranchSession theBranch = found.getBranch(bid);
                        if (theBranch == null) {
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("BranchSession To Be Updated Does Not Exists [" + bid + "/" + xid + "]");
                            }
                        } else {
                            found.remove(theBranch);
                        }
                    }
                    break;
                }

                default:
                    throw new ShouldNeverHappenException("Unknown Operation: " + logOperation);

            }
        }

    }

    @Override
    public void destroy() {
        transactionStoreManager.shutdown();
    }
}
