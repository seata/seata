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
package io.seata.core.constants;

/**
 * @author chd
 */
public enum Isolation {
    /**
     * the isolation read uncommitted
     */
    READ_UNCOMMITTED,
    /**
     * the isolation read commit
     */
    READ_COMMITTED,
    /**
     * the isolation repeatable read
     */
    REPEATABLE_READ,
    /**
     * the isolation serializable
     */
    SERIALIZABLE;

    public static boolean isSupport(Isolation isolation) {
        return isolation == READ_UNCOMMITTED || isolation == READ_COMMITTED;
    }
}
