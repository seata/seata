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
package io.seata.tm;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.core.model.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Default transaction manager.
 *
 * @author sharajava
 */
public class TransactionManagerHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManagerHolder.class);

    private static class SingletonHolder {

        private static TransactionManager INSTANCE = null;

        static {
            try {
                INSTANCE = EnhancedServiceLoader.load(TransactionManager.class, "defaultTM");
                LOGGER.info("TransactionManager Singleton {}", INSTANCE);
            } catch (Throwable anyEx) {
                LOGGER.error("Failed to load TransactionManager Singleton! ", anyEx);
            }
        }
    }

    /**
     * Get transaction manager.
     *
     * @return the transaction manager
     */
    public static TransactionManager get() {
        if (SingletonHolder.INSTANCE == null) {
            throw new ShouldNeverHappenException("TransactionManager is NOT ready!");
        }
        return SingletonHolder.INSTANCE;
    }

    /**
     * Set a TM instance.
     *
     * @param mock commonly used for test mocking
     */
    public static void set(TransactionManager mock) {
        SingletonHolder.INSTANCE = mock;
    }

    private TransactionManagerHolder() {

    }
}
