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
 * The enum Branch type.
 *
 * @author sharajava
 */
public enum BranchType {

    /**
     * The At.
     */
    // AT Branch
    AT,

    /**
     * The At.
     */
    // AT Branch
    SAGA,

    /**
     * The TCC.
     */
    TCC;

    /**
     * Get branch type.
     *
     * @param ordinal the ordinal
     * @return the branch type
     */
    public static BranchType get(byte ordinal) {
        return get((int)ordinal);
    }

    /**
     * Get branch type.
     *
     * @param ordinal the ordinal
     * @return the branch type
     */
    public static BranchType get(int ordinal) {
        for (BranchType branchType : BranchType.values()) {
            if (branchType.ordinal() == ordinal) {
                return branchType;
            }
        }
        throw new IllegalArgumentException("Unknown BranchType[" + ordinal + "]");
    }
}
