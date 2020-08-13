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
package io.seata.tc.store;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.tc.storage.file.FlushDiskMode;

import static io.seata.core.constants.ConfigurationKeys.STORE_FILE_PREFIX;


/**
 * @author lizhao
 */
public class StoreConfig {

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();


    /**
     * Default 16kb.
     */
    private static final int DEFAULT_MAX_BRANCH_SESSION_SIZE = 1024 * 16;

    /**
     * Default 512b.
     */
    private static final int DEFAULT_MAX_GLOBAL_SESSION_SIZE = 512;

    /**
     * Default 16kb.
     */
    private static final int DEFAULT_WRITE_BUFFER_SIZE = 1024 * 16;

    public static int getMaxBranchSessionSize() {
        return CONFIGURATION.getInt(STORE_FILE_PREFIX + "maxBranchSessionSize", DEFAULT_MAX_BRANCH_SESSION_SIZE);
    }

    public static int getMaxGlobalSessionSize() {
        return CONFIGURATION.getInt(STORE_FILE_PREFIX + "maxGlobalSessionSize", DEFAULT_MAX_GLOBAL_SESSION_SIZE);
    }

    public static int getFileWriteBufferCacheSize() {
        return CONFIGURATION.getInt(STORE_FILE_PREFIX + "fileWriteBufferCacheSize", DEFAULT_WRITE_BUFFER_SIZE);
    }

    public static FlushDiskMode getFlushDiskMode() {
        return FlushDiskMode.findDiskMode(CONFIGURATION.getConfig(STORE_FILE_PREFIX + "flushDiskMode"));
    }
}
