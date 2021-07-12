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
package io.seata.rm.tcc.api;

import io.seata.core.model.CommitType;

/**
 * The enum of tcc two phase commit type.
 *
 * @author wang.liang
 */
public enum TCCCommitType {

    /**
     * The sync commit.
     */
    SyncCommit,

    /**
     * The async commit.
     */
    AsyncCommit,

    ;

    /**
     * Gets the corresponding CommitType
     *
     * @return the commit type
     */
    public CommitType getCommitType() {
        for (CommitType commitType : CommitType.values()) {
            if (commitType.name().equals(this.name())) {
                return commitType;
            }
        }
        throw new RuntimeException("The " + TCCCommitType.class.getSimpleName() + " name '" + this.name() + "' is incorrect");
    }
}
