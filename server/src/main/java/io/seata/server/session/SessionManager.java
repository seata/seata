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

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.rpc.Disposable;

import java.util.Collection;
import java.util.List;

/**
 * The interface Session manager.
 *
 * @author sharajava
 */
public interface SessionManager extends SessionLifecycleListener, Disposable {

    /**
     * Get global session global session.
     *
     * @param xid the xid
     * @return the global session
     */
    default GlobalSession getGlobalSession(String xid) {
        return getGlobalSession(xid, true);
    }

    /**
     * Get global session global session.
     *
     * @param xid                the xid
     * @param withBranchSessions the withBranchSessions
     * @return the global session
     */
    GlobalSession getGlobalSession(String xid, boolean withBranchSessions);

    /**
     * Get global session global session.
     *
     * @param transactionId the transaction id
     * @return the global session
     */
    default GlobalSession getGlobalSession(long transactionId) {
        return getGlobalSession(transactionId, true);
    }

    /**
     * Get global session global session.
     *
     * @param transactionId      the transaction id
     * @param withBranchSessions the withBranchSessions
     * @return the global session
     */
    GlobalSession getGlobalSession(long transactionId, boolean withBranchSessions);

    /**
     * All sessions collection.
     *
     * @return the collection
     */
    default Collection<GlobalSession> allSessions() {
        return allSessions(true);
    }

    /**
     * All sessions collection.
     *
     * @param withBranchSessions the with branch sessions
     * @return the collection
     */
    default Collection<GlobalSession> allSessions(boolean withBranchSessions) {
        SessionCondition condition = new SessionCondition();
        condition.setPageSize(0);
        return findGlobalSessions(condition, withBranchSessions);
    }

    /**
     * Find global sessions list.
     *
     * @param condition the condition
     * @return the list
     */
    default List<GlobalSession> findGlobalSessions(SessionCondition condition) {
        return findGlobalSessions(condition, true);
    }

    /**
     * Find global sessions list.
     *
     * @param condition          the condition
     * @param withBranchSessions the withBranchSessions
     * @return the list
     */
    List<GlobalSession> findGlobalSessions(SessionCondition condition, boolean withBranchSessions);

    /**
     * Find global sessions list.
     *
     * @param statuses the statuses
     * @return the list
     */
    default List<GlobalSession> findGlobalSessions(GlobalStatus... statuses) {
        return findGlobalSessions(statuses, true);
    }

    /**
     * Find global sessions list.
     *
     * @param statuses           the statuses
     * @param withBranchSessions the withBranchSessions
     * @return the list
     */
    default List<GlobalSession> findGlobalSessions(GlobalStatus[] statuses, boolean withBranchSessions) {
        return findGlobalSessions(new SessionCondition(statuses), withBranchSessions);
    }

    /**
     * Find global sessions list.
     *
     * @param overTimeAliveMills the over time alive mills
     * @return the list
     */
    default List<GlobalSession> findGlobalSessions(long overTimeAliveMills) {
        return findGlobalSessions(new SessionCondition(overTimeAliveMills));
    }

    /**
     * Add global session.
     *
     * @param session the session
     * @throws TransactionException the transaction exception
     */
    void addGlobalSession(GlobalSession session) throws TransactionException;

    /**
     * Update global session.
     *
     * @param session the session
     * @param status  the status
     * @throws TransactionException the transaction exception
     */
    void updateGlobalSession(GlobalSession session, GlobalStatus status) throws TransactionException;

    /**
     * Remove global session.
     *
     * @param session the session
     * @throws TransactionException the transaction exception
     */
    void removeGlobalSession(GlobalSession session) throws TransactionException;

    /**
     * Add branch session.
     *
     * @param globalSession the global session
     * @param session       the session
     * @throws TransactionException the transaction exception
     */
    void addBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException;

    /**
     * Update branch session.
     *
     * @param branchSession   the session
     * @param status          the status
     * @param applicationData the application data
     * @throws TransactionException the transaction exception
     */
    void updateBranchSession(BranchSession branchSession, BranchStatus status, String applicationData) throws TransactionException;

    /**
     * Remove branch session.
     *
     * @param globalSession the global session
     * @param session       the session
     * @throws TransactionException the transaction exception
     */
    void removeBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException;

    /**
     * lock and execute
     *
     * @param globalSession the global session
     * @param lockCallable  the lock Callable
     * @return the value
     */
    <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException;
}
