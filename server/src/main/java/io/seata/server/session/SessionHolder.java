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
package io.seata.server.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import io.seata.common.XID;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.raft.RaftServerFactory;
import io.seata.core.store.StoreMode;
import io.seata.server.lock.LockManager;
import io.seata.server.storage.file.lock.FileLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.seata.common.Constants.ASYNC_COMMITTING;
import static io.seata.common.Constants.RETRY_COMMITTING;
import static io.seata.common.Constants.RETRY_ROLLBACKING;
import static io.seata.common.Constants.TX_TIMEOUT_CHECK;
import static io.seata.common.Constants.UNDOLOG_DELETE;
import static io.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;
import static io.seata.common.DefaultValues.SERVER_DEFAULT_STORE_MODE;
import static java.io.File.separator;

/**
 * The type Session holder.
 *
 * @author sharajava
 */
public class SessionHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionHolder.class);

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The constant ROOT_SESSION_MANAGER_NAME.
     */
    public static final String ROOT_SESSION_MANAGER_NAME = "root.data";
    /**
     * The constant ASYNC_COMMITTING_SESSION_MANAGER_NAME.
     */
    public static final String ASYNC_COMMITTING_SESSION_MANAGER_NAME = "async.commit.data";
    /**
     * The constant RETRY_COMMITTING_SESSION_MANAGER_NAME.
     */
    public static final String RETRY_COMMITTING_SESSION_MANAGER_NAME = "retry.commit.data";
    /**
     * The constant RETRY_ROLLBACKING_SESSION_MANAGER_NAME.
     */
    public static final String RETRY_ROLLBACKING_SESSION_MANAGER_NAME = "retry.rollback.data";

    private static SessionManager ROOT_SESSION_MANAGER;
    private static SessionManager ASYNC_COMMITTING_SESSION_MANAGER;
    private static SessionManager RETRY_COMMITTING_SESSION_MANAGER;
    private static SessionManager RETRY_ROLLBACKING_SESSION_MANAGER;

    /**
     * Init.
     *
     * @param mode the store mode: file, db, redis
     * @throws IOException the io exception
     */
    public static void init(String mode) {
        if (StringUtils.isBlank(mode)) {
            mode = CONFIG.getConfig(ConfigurationKeys.STORE_SESSION_MODE,
                CONFIG.getConfig(ConfigurationKeys.STORE_MODE, SERVER_DEFAULT_STORE_MODE));
        }
        StoreMode storeMode = StoreMode.get(mode);
        if (StoreMode.DB.equals(storeMode)) {
            ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.DB.getName());
            ASYNC_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.DB.getName(),
                new Object[] {ASYNC_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.DB.getName(),
                new Object[] {RETRY_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_ROLLBACKING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.DB.getName(),
                new Object[] {RETRY_ROLLBACKING_SESSION_MANAGER_NAME});
        } else if (StoreMode.RAFT.equals(storeMode) || StoreMode.FILE.equals(storeMode)) {
            String sessionStorePath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR)
                + separator + XID.getPort();
            if (StringUtils.isBlank(sessionStorePath)) {
                throw new StoreException("the {store.file.dir} is empty.");
            }
            ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.FILE.getName(),
                new Object[] {ROOT_SESSION_MANAGER_NAME, sessionStorePath});
            ASYNC_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.FILE.getName(),
                new Class[] {String.class, String.class}, new Object[] {ASYNC_COMMITTING_SESSION_MANAGER_NAME, null});
            RETRY_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.FILE.getName(),
                new Class[] {String.class, String.class}, new Object[] {RETRY_COMMITTING_SESSION_MANAGER_NAME, null});
            RETRY_ROLLBACKING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.FILE.getName(),
                new Class[] {String.class, String.class}, new Object[] {RETRY_ROLLBACKING_SESSION_MANAGER_NAME, null});
            if (StoreMode.RAFT.equals(storeMode) || StoreMode.RAFT.getName()
                .equalsIgnoreCase(CONFIG.getConfig(ConfigurationKeys.STORE_MODE, SERVER_DEFAULT_STORE_MODE))) {
                ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.RAFT.getName(),
                    new Object[] {ROOT_SESSION_MANAGER_NAME, ROOT_SESSION_MANAGER});
            }
        } else if (StoreMode.REDIS.equals(storeMode)) {
            ROOT_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.REDIS.getName());
            ASYNC_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class,
                StoreMode.REDIS.getName(), new Object[] {ASYNC_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_COMMITTING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class,
                StoreMode.REDIS.getName(), new Object[] {RETRY_COMMITTING_SESSION_MANAGER_NAME});
            RETRY_ROLLBACKING_SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class,
                StoreMode.REDIS.getName(), new Object[] {RETRY_ROLLBACKING_SESSION_MANAGER_NAME});
        } else {
            // unknown store
            throw new IllegalArgumentException("unknown store mode:" + mode);
        }
        RaftServerFactory.getInstance().init(XID.getIpAddress(), XID.getPort());
        if (RaftServerFactory.getInstance().isRaftMode()) {
            return;
        }
        reload(storeMode);
    }

    /**
     * Reload.
     */
    protected static void reload(StoreMode storeMode) {
        if (ROOT_SESSION_MANAGER instanceof Reloadable) {
            ((Reloadable)ROOT_SESSION_MANAGER).reload();
        }
        reload(ROOT_SESSION_MANAGER.allSessions(), storeMode);
    }

    public static void reload(Collection<GlobalSession> allSessions, StoreMode storeMode) {
        reload(allSessions, storeMode, true);
    }

    public static void reload(Collection<GlobalSession> allSessions, StoreMode storeMode, boolean acquireLock) {
        if (CollectionUtils.isNotEmpty(allSessions)) {
            List<GlobalSession> removeGlobalSessions = new ArrayList<>();
            Iterator<GlobalSession> iterator = allSessions.iterator();
            while (iterator.hasNext()) {
                GlobalSession globalSession = iterator.next();
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
                        removeGlobalSessions.add(globalSession);
                        break;
                    case AsyncCommitting:
                        if (storeMode == StoreMode.FILE) {
                            queueToAsyncCommitting(globalSession);
                        }
                        break;
                    default: {
                        if (storeMode == StoreMode.FILE) {
                            if (acquireLock) {
                                lockBranchSessions(globalSession.getSortedBranches());
                            }
                            switch (globalStatus) {
                                case Committing:
                                case CommitRetrying:
                                    queueToRetryCommit(globalSession);
                                    break;
                                case Rollbacking:
                                case RollbackRetrying:
                                case TimeoutRollbacking:
                                case TimeoutRollbackRetrying:
                                    queueToRetryRollback(globalSession);
                                    break;
                                case Begin:
                                    globalSession.setActive(true);
                                    break;
                                default:
                                    throw new ShouldNeverHappenException("NOT properly handled " + globalStatus);
                            }
                        }
                        break;
                    }
                }
            }
            for (GlobalSession globalSession : removeGlobalSessions) {
                removeInErrorState(globalSession);
            }
        }
    }

    private static void removeInErrorState(GlobalSession globalSession) {
        try {
            LOGGER.warn("The global session should NOT be {}, remove it. xid = {}", globalSession.getStatus(), globalSession.getXid());
            ROOT_SESSION_MANAGER.removeGlobalSession(globalSession);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Remove global session succeed, xid = {}, status = {}", globalSession.getXid(), globalSession.getStatus());
            }
        } catch (Exception e) {
            LOGGER.error("Remove global session failed, xid = {}, status = {}", globalSession.getXid(), globalSession.getStatus(), e);
        }
    }

    private static void queueToAsyncCommitting(GlobalSession globalSession) {
        try {
            globalSession.addSessionLifecycleListener(getAsyncCommittingSessionManager());
            getAsyncCommittingSessionManager().addGlobalSession(globalSession);
        } catch (TransactionException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    private static void lockBranchSessions(ArrayList<BranchSession> branchSessions) {
        FileLockManager fileLockManager =
            (FileLockManager)EnhancedServiceLoader.load(LockManager.class, StoreMode.FILE.getName());
        branchSessions.forEach(branchSession -> {
            try {
                if (StringUtils.isNotBlank(branchSession.getLockKey())) {
                    fileLockManager.acquireLock(branchSession);
                }
            } catch (TransactionException e) {
                throw new ShouldNeverHappenException(e);
            }
        });
    }

    private static void queueToRetryCommit(GlobalSession globalSession) {
        try {
            globalSession.addSessionLifecycleListener(getRetryCommittingSessionManager());
            getRetryCommittingSessionManager().addGlobalSession(globalSession);
        } catch (TransactionException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    private static void queueToRetryRollback(GlobalSession globalSession) {
        try {
            globalSession.addSessionLifecycleListener(getRetryRollbackingSessionManager());
            getRetryRollbackingSessionManager().addGlobalSession(globalSession);
        } catch (TransactionException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    //endregion

    //region get session manager

    /**
     * Gets root session manager.
     *
     * @return the root session manager
     */
    public static SessionManager getRootSessionManager() {
        if (ROOT_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return ROOT_SESSION_MANAGER;
    }

    /**
     * Gets async committing session manager.
     *
     * @return the async committing session manager
     */
    public static SessionManager getAsyncCommittingSessionManager() {
        if (ASYNC_COMMITTING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return ASYNC_COMMITTING_SESSION_MANAGER;
    }

    /**
     * Gets retry committing session manager.
     *
     * @return the retry committing session manager
     */
    public static SessionManager getRetryCommittingSessionManager() {
        if (RETRY_COMMITTING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return RETRY_COMMITTING_SESSION_MANAGER;
    }

    /**
     * Gets retry rollbacking session manager.
     *
     * @return the retry rollbacking session manager
     */
    public static SessionManager getRetryRollbackingSessionManager() {
        if (RETRY_ROLLBACKING_SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return RETRY_ROLLBACKING_SESSION_MANAGER;
    }

    //endregion

    /**
     * Find global session.
     *
     * @param xid the xid
     * @return the global session
     */
    public static GlobalSession findGlobalSession(String xid) {
        return findGlobalSession(xid, true);
    }

    /**
     * Find global session.
     *
     * @param xid                the xid
     * @param withBranchSessions the withBranchSessions
     * @return the global session
     */
    public static GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
        return getRootSessionManager().findGlobalSession(xid, withBranchSessions);
    }

    /**
     * lock and execute
     *
     * @param globalSession the global session
     * @param lockCallable the lock Callable
     * @return the value
     */
    public static <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException {
        return getRootSessionManager().lockAndExecute(globalSession, lockCallable);
    }

    /**
     * retry rollbacking lock
     *
     * @return the boolean
     */
    public static boolean retryRollbackingLock() {
        return getRootSessionManager().scheduledLock(RETRY_ROLLBACKING);
    }

    /**
     * retry committing lock
     *
     * @return the boolean
     */
    public static boolean retryCommittingLock() {
        return getRootSessionManager().scheduledLock(RETRY_COMMITTING);
    }

    /**
     * async committing lock
     *
     * @return the boolean
     */
    public static boolean asyncCommittingLock() {
        return getRootSessionManager().scheduledLock(ASYNC_COMMITTING);
    }

    /**
     * tx timeout check lOck
     *
     * @return the boolean
     */
    public static boolean txTimeoutCheckLock() {
        return getRootSessionManager().scheduledLock(TX_TIMEOUT_CHECK);
    }

    /**
     * undolog delete lock
     *
     * @return the boolean
     */
    public static boolean undoLogDeleteLock() {
        return getRootSessionManager().scheduledLock(UNDOLOG_DELETE);
    }

    /**
     * un retry rollbacking lock
     *
     * @return the boolean
     */
    public static boolean unRetryRollbackingLock() {
        return getRootSessionManager().unScheduledLock(RETRY_ROLLBACKING);
    }

    /**
     * un retry committing lock
     *
     * @return the boolean
     */
    public static boolean unRetryCommittingLock() {
        return getRootSessionManager().unScheduledLock(RETRY_COMMITTING);
    }

    /**
     * un async committing lock
     *
     * @return the boolean
     */
    public static boolean unAsyncCommittingLock() {
        return getRootSessionManager().unScheduledLock(ASYNC_COMMITTING);
    }

    /**
     * un tx timeout check lOck
     *
     * @return the boolean
     */
    public static boolean unTxTimeoutCheckLock() {
        return getRootSessionManager().unScheduledLock(TX_TIMEOUT_CHECK);
    }

    /**
     * un undolog delete lock
     *
     * @return the boolean
     */
    public static boolean unUndoLogDeleteLock() {
        return getRootSessionManager().unScheduledLock(UNDOLOG_DELETE);
    }

    public static void destroy() {
        if (ROOT_SESSION_MANAGER != null) {
            ROOT_SESSION_MANAGER.destroy();
        }
        if (ASYNC_COMMITTING_SESSION_MANAGER != null) {
            ASYNC_COMMITTING_SESSION_MANAGER.destroy();
        }
        if (RETRY_COMMITTING_SESSION_MANAGER != null) {
            RETRY_COMMITTING_SESSION_MANAGER.destroy();
        }
        if (RETRY_ROLLBACKING_SESSION_MANAGER != null) {
            RETRY_ROLLBACKING_SESSION_MANAGER.destroy();
        }
    }
}
