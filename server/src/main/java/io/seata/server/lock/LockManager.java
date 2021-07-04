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
package io.seata.server.lock;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.LockStatus;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;

/**
 * The interface Lock manager.
 *
 * @author sharajava
 */
public interface LockManager {

    /**
     * Acquire lock boolean.
     *
     * @param branchSession the branch session
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean acquireLock(BranchSession branchSession) throws TransactionException;

    /**
     * Acquire lock boolean.
     *
     * @param branchSession the branch session
     * @param autoCommit the auto commit
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean acquireLock(BranchSession branchSession, boolean autoCommit) throws TransactionException;

    /**
     * Un lock boolean.
     *
     * @param branchSession the branch session
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean releaseLock(BranchSession branchSession) throws TransactionException;

    /**
     * Un lock boolean.
     *
     * @param globalSession the global session
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException;

    /**
     * Is lockable boolean.
     *
     * @param xid        the xid
     * @param resourceId the resource id
     * @param lockKey    the lock key
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean isLockable(String xid, String resourceId, String lockKey) throws TransactionException;

    /**
     * Clean all locks.
     *
     * @throws TransactionException the transaction exception
     */
    void cleanAllLocks() throws TransactionException;

    /**
     * update lock status.
     * @param xid the xid
     * @param lockStatus the lock status
     * @throws TransactionException the transaction exception
     * @return the boolean
     */
    boolean updateLockStatus(String xid, LockStatus lockStatus) throws TransactionException;

}
