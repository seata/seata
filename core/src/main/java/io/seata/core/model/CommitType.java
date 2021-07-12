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
 * The enum of two phase commit type.
 *
 * @author wang.liang
 */
public enum CommitType {

    /**
     * The sync commit.
     */
    SyncCommit(0),

    /**
     * The async commit.
     */
    AsyncCommit(1),

    /**
     * The no commit.
     */
    NoCommit(2),
    ;

    private int value;

    CommitType(int value) {
        this.value = value;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public int value() {
        return value;
    }

    /**
     * Get commit type
     *
     * @param value the value
     * @return the commit type
     */
    public static CommitType get(byte value) {
        return get((int) value);
    }

    /**
     * Get commit type.
     *
     * @param value the value
     * @return the commit type
     */
    public static CommitType get(int value) {
        for (CommitType t : CommitType.values()) {
            if (t.value() == value) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown CommitType[" + value + "]");
    }

    /**
     * Get the default by branch type
     *
     * @param branchType the branch type
     * @return the default commit type
     */
    public static CommitType getDefault(BranchType branchType) {
        if (branchType == BranchType.AT) {
            return AsyncCommit;
        } else if (branchType == BranchType.SAGA) {
            return NoCommit;
        } else {
            return SyncCommit;
        }
    }
}
