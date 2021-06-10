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
package io.seata.server.lock.distributed;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.core.store.DistributedLocker;

/**
 * @description Distributed locker factory
 * @author  zhongxiang.wang
 */
public class DistributedLockerFactory {

    private static final Map<String, DistributedLocker> DISTRIBUTED_LOCKER_MAP = new ConcurrentHashMap<>();

    /**
     * Get the distributed locker by lockerType
     *
     * @param lockerType the locker type
     * @return the distributed locker
     */
    public static DistributedLocker getDistributedLocker(String lockerType) {
        return CollectionUtils.computeIfAbsent(DISTRIBUTED_LOCKER_MAP, lockerType, key -> EnhancedServiceLoader.load(DistributedLocker.class, lockerType));
    }

}
