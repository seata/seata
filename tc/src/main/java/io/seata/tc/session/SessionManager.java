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
package io.seata.tc.session;

import java.util.Collection;
import java.util.List;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.rpc.Disposable;

/**
 * The interface Session manager.
 *
 * @author sharajava
 */
public interface SessionManager extends SessionLifecycleListener, Disposable {

    /**
     * Add global session.
     *
     * @param session the session
     * @throws TransactionException the transaction exception
     */
    void addGlobalSession(GlobalSession session) throws TransactionException;

    /**
     * Find global session global session.
     *
     * @param xid the xid
     * @return the global session
     */
    GlobalSession findGlobalSession(String xid) ;

    /**
     * Find global session global session.
     *
     * @param xid the xid
     * @param withBranchSessions the withBranchSessions
     * @return the global session
     */
    GlobalSession findGlobalSession(String xid, boolean withBranchSessions);

    /**
     * Update global session status.
     *
     * @param session the session
     * @param status  the status
     * @throws TransactionException the transaction exception
     */
    void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status) throws TransactionException;

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
     * Update branch session status.
     *
     * @param session the session
     * @param status  the status
     * @throws TransactionException the transaction exception
     */
    void updateBranchSessionStatus(BranchSession session, BranchStatus status) throws TransactionException;

    /**
     * Remove branch session.
     *
     * @param globalSession the global session
     * @param session       the session
     * @throws TransactionException the transaction exception
     */
    void removeBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException;

    /**
     * All sessions collection.
     *
     * @return the collection
     */
    Collection<GlobalSession> allSessions();

    /**
     * Find global sessions list.
     *
     * @param condition the condition
     * @return the list
     */
    List<GlobalSession> findGlobalSessions(SessionCondition condition);

    /**
     * lock and execute
     *
     * @param globalSession the global session
     * @param lockCallable the lock Callable
     * @return the value
     */
    <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException;
}
