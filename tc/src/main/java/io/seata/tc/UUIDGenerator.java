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
package io.seata.tc;

import io.seata.common.util.IdWorker;

/**
 * The type Uuid generator.
 *
 * @author sharajava
 */
public class UUIDGenerator {

    /**
     * Generate uuid long.
     *
     * @return the long
     */
    public static long generateUUID() {
        return IdWorker.getInstance().nextId();
    }

    /**
     * Init.
     *
     * @param serverNode the server node id
     */
    public static void init(Long serverNode) {
        IdWorker.init(serverNode);
    }

}
