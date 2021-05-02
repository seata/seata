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
package io.seata.core.model;


/**
 * Status of global transaction.
 *
 * @author sharajava
 */
public enum GlobalStatus {

    /**
     * Un known global status.
     */
    // Unknown
    UnKnown(0),

    /**
     * The Begin.
     */
    // PHASE 1: can accept new branch registering.
    Begin(1),

    /**
     * PHASE 2: Running Status: may be changed any time.
     */
    // Committing.
    Committing(2),

    /**
     * The Commit retrying.
     */
    // Retrying commit after a recoverable failure.
    CommitRetrying(3),

    /**
     * Rollbacking global status.
     */
    // Rollbacking
    Rollbacking(4),

    /**
     * The Rollback retrying.
     */
    // Retrying rollback after a recoverable failure.
    RollbackRetrying(5),

    /**
     * The Timeout rollbacking.
     */
    // Rollbacking since timeout
    TimeoutRollbacking(6),

    /**
     * The Timeout rollback retrying.
     */
    // Retrying rollback (since timeout) after a recoverable failure.
    TimeoutRollbackRetrying(7),

    /**
     * All branches can be async committed. The committing is NOT done yet, but it can be seen as committed for TM/RM
     * client.
     */
    AsyncCommitting(8),

    /**
     * PHASE 2: Final Status: will NOT change any more.
     */
    // Finally: global transaction is successfully committed.
    Committed(9),

    /**
     * The Commit failed.
     */
    // Finally: failed to commit
    CommitFailed(10),

    /**
     * The Rollbacked.
     */
    // Finally: global transaction is successfully rollbacked.
    Rollbacked(11),

    /**
     * The Rollback failed.
     */
    // Finally: failed to rollback
    RollbackFailed(12),

    /**
     * The Timeout rollbacked.
     */
    // Finally: global transaction is successfully rollbacked since timeout.
    TimeoutRollbacked(13),

    /**
     * The Timeout rollback failed.
     */
    // Finally: failed to rollback since timeout
    TimeoutRollbackFailed(14),

    /**
     * The Finished.
     */
    // Not managed in session MAP any more
    Finished(15),
    
    /**
     * The Removed.
     */
    Removed(16);

    private int code;

    GlobalStatus(int code) {
        this.code = code;
    }


    /**
     * Gets code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }


    /**
     * Get global status.
     *
     * @param code the code
     * @return the global status
     */
    public static GlobalStatus get(byte code) {
        return get((int) code);
    }

    /**
     * Get global status.
     *
     * @param code the code
     * @return the global status
     */
    public static GlobalStatus get(int code) {
        GlobalStatus value = null;
        try {
            value = GlobalStatus.values()[code];
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown GlobalStatus[" + code + "]");
        }
        return value;
    }
}
