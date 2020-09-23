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

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.StoreMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * The session manager
     */
    private static SessionManager SESSION_MANAGER;

    /**
     * Init.
     *
     * @param mode the store mode: file, db
     * @throws IOException the io exception
     */
    public static void init(String mode) {
        if (StringUtils.isBlank(mode)) {
            mode = CONFIG.getConfig(ConfigurationKeys.STORE_MODE);
        }
        StoreMode storeMode = StoreMode.get(mode);
        if (StoreMode.DB.equals(storeMode)) {
            SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.DB.getName());
        } else if (StoreMode.FILE.equals(storeMode)) {
            SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.FILE.getName());
        } else if (StoreMode.REDIS.equals(storeMode)) {
            SESSION_MANAGER = EnhancedServiceLoader.load(SessionManager.class, StoreMode.REDIS.getName());
        } else {
            // unknown store
            throw new IllegalArgumentException("unknown store mode:" + mode);
        }
        reload();
    }

    /**
     * Reload.
     */
    protected static void reload() {
        if (SESSION_MANAGER instanceof Reloadable) {
            ((Reloadable) SESSION_MANAGER).reload();

            Collection<GlobalSession> reloadedSessions = SESSION_MANAGER.allSessions();
            if (reloadedSessions != null && !reloadedSessions.isEmpty()) {
                reloadedSessions.forEach(globalSession -> {
                    GlobalStatus globalStatus = globalSession.getStatus();
                    switch (globalStatus) {
                        case UnKnown: // 0
                        case Committed: // 9
                        case CommitFailed: // 10
                        case Rollbacked: // 11
                        case RollbackFailed: // 12
                        case TimeoutRollbacked: // 13
                        case TimeoutRollbackFailed: // 14
                        case Finished: // 15
                            try {
                                LOGGER.warn("Reloaded Session should NOT be " + globalStatus + ", xid = " + globalSession.getXid());
                                SESSION_MANAGER.removeGlobalSession(globalSession);
                                LOGGER.info("Remove global session succeeded, xid = {}, status = {}", globalSession.getXid(), globalSession.getStatus());
                            } catch (Exception e) {
                                LOGGER.error("Remove the global session failed, xid = " + globalSession.getXid() + ", status = " + globalSession.getStatus(), e);
                            }
                            break;
                        case AsyncCommitting: // 8
                            break;
                        default: {
                            switch (globalStatus) {
                                case Committing: // 2
                                case CommitRetrying: // 3
                                case Rollbacking: // 4
                                case RollbackRetrying: // 5
                                case TimeoutRollbacking: // 6
                                case TimeoutRollbackRetrying: // 7
                                    break;
                                case Begin: // 1
                                    globalSession.setActive(true);
                                    break;
                                default:
                                    throw new ShouldNeverHappenException("NOT properly handled " + globalStatus);
                            }

                            ArrayList<BranchSession> branchSessions = globalSession.getSortedBranches();
                            branchSessions.forEach(branchSession -> {
                                try {
                                    branchSession.lock();
                                } catch (TransactionException e) {
                                    throw new ShouldNeverHappenException(e);
                                }
                            });
                            break;
                        }
                    }
                });
            }
        }
    }

    /**
     * Gets session manager.
     *
     * @return the session manager
     */
    public static SessionManager getSessionManager() {
        if (SESSION_MANAGER == null) {
            throw new ShouldNeverHappenException("SessionManager is NOT init!");
        }
        return SESSION_MANAGER;
    }

    /**
     * Get global session.
     *
     * @param xid the xid
     * @return the global session
     */
    public static GlobalSession getGlobalSession(String xid) {
        return getGlobalSession(xid, true);
    }

    /**
     * Get global session.
     *
     * @param xid                the xid
     * @param withBranchSessions the withBranchSessions
     * @return the global session
     */
    public static GlobalSession getGlobalSession(String xid, boolean withBranchSessions) {
        return SESSION_MANAGER.getGlobalSession(xid, withBranchSessions);
    }

    /**
     * lock and execute
     *
     * @param globalSession the global session
     * @param lockCallable  the lock Callable
     * @return the value
     */
    public static <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException {
        return SESSION_MANAGER.lockAndExecute(globalSession, lockCallable);
    }

    /**
     * Destroy session manager
     */
    public static void destroy() {
        if (SESSION_MANAGER != null) {
            SESSION_MANAGER.destroy();
        }
    }
}
