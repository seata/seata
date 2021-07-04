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
package io.seata.core.lock;

import java.util.List;
import io.seata.core.model.LockStatus;

/**
 * The interface Locker.
 */
public interface Locker {

    /**
     * Acquire lock boolean.
     *
     * @param rowLock the row lock
     * @return the boolean
     */
    boolean acquireLock(List<RowLock> rowLock) ;

    /**
     * Acquire lock boolean.
     *
     * @param rowLock the row lock
     * @param autoCommit the auto commit
     * @return the boolean
     */
    boolean acquireLock(List<RowLock> rowLock, boolean autoCommit);

    /**
     * Release lock boolean.
     *
     * @param rowLock the row lock
     * @return the boolean
     */
    boolean releaseLock(List<RowLock> rowLock);

    /**
     * Release lock boolean.
     *
     * @param xid      the xid
     * @param branchId the branch id
     * @return the boolean
     */
    boolean releaseLock(String xid, Long branchId);

    /**
     * Release lock boolean.
     *
     * @param xid       the xid
     * @param branchIds the branch ids
     * @return the boolean
     */
    boolean releaseLock(String xid, List<Long> branchIds);

    /**
     * Is lockable boolean.
     *
     * @param rowLock the row lock
     * @return the boolean
     */
    boolean isLockable(List<RowLock> rowLock);

    /**
     * Clean all locks.
     */
    void cleanAllLocks();

    /**
     * update lock status .
     *
     * @param xid the xid
     * @param lockStatus the lock status
     * @return the lock owners
     */
    boolean updateLockStatus(String xid, LockStatus lockStatus);

}

