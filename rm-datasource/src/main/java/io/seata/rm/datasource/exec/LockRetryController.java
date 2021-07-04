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
package io.seata.rm.datasource.exec;

import io.seata.common.DefaultValues;
import io.seata.common.util.NumberUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationCache;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.GlobalLockConfigHolder;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalLockConfig;

/**
 * Lock retry controller
 *
 * @author sharajava
 */
public class LockRetryController {

    private static final GlobalConfig LISTENER = new GlobalConfig();

    static {
        ConfigurationCache.addConfigListener(ConfigurationKeys.CLIENT_LOCK_RETRY_INTERVAL, LISTENER);
        ConfigurationCache.addConfigListener(ConfigurationKeys.CLIENT_LOCK_RETRY_TIMES, LISTENER);
    }

    private int lockRetryInterval;

    private int lockRetryTimes;

    /**
     * Instantiates a new Lock retry controller.
     */
    public LockRetryController() {
        this.lockRetryInterval = getLockRetryInterval();
        this.lockRetryTimes = getLockRetryTimes();
    }

    /**
     * Sleep.
     *
     * @param e the e
     * @throws LockWaitTimeoutException the lock wait timeout exception
     */
    public void sleep(Exception e) throws LockWaitTimeoutException {
        // prioritize the rollback of other transactions
        if (--lockRetryTimes < 0 || (e instanceof LockConflictException
            && ((LockConflictException)e).getCode() == TransactionExceptionCode.LockKeyConflictFailFast)) {
            throw new LockWaitTimeoutException("Global lock wait timeout", e);
        }

        try {
            Thread.sleep(lockRetryInterval);
        } catch (InterruptedException ignore) {
        }
    }

    int getLockRetryInterval() {
        // get customized config first
        GlobalLockConfig config = GlobalLockConfigHolder.getCurrentGlobalLockConfig();
        if (config != null) {
            int configInterval = config.getLockRetryInterval();
            if (configInterval > 0) {
                return configInterval;
            }
        }
        // if there is no customized config, use global config instead
        return LISTENER.getGlobalLockRetryInterval();
    }

    int getLockRetryTimes() {
        // get customized config first
        GlobalLockConfig config = GlobalLockConfigHolder.getCurrentGlobalLockConfig();
        if (config != null) {
            int configTimes = config.getLockRetryTimes();
            if (configTimes >= 0) {
                return configTimes;
            }
        }
        // if there is no customized config, use global config instead
        return LISTENER.getGlobalLockRetryTimes();
    }

    static class GlobalConfig implements ConfigurationChangeListener {

        private volatile int globalLockRetryInterval;

        private volatile int globalLockRetryTimes;

        private final int defaultRetryInterval = DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_INTERVAL;
        private final int defaultRetryTimes = DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_TIMES;

        public GlobalConfig() {
            Configuration configuration = ConfigurationFactory.getInstance();
            globalLockRetryInterval = configuration.getInt(ConfigurationKeys.CLIENT_LOCK_RETRY_INTERVAL, defaultRetryInterval);
            globalLockRetryTimes = configuration.getInt(ConfigurationKeys.CLIENT_LOCK_RETRY_TIMES, defaultRetryTimes);
        }

        @Override
        public void onChangeEvent(ConfigurationChangeEvent event) {
            String dataId = event.getDataId();
            String newValue = event.getNewValue();
            if (ConfigurationKeys.CLIENT_LOCK_RETRY_INTERVAL.equals(dataId)) {
                globalLockRetryInterval = NumberUtils.toInt(newValue, defaultRetryInterval);
            }
            if (ConfigurationKeys.CLIENT_LOCK_RETRY_TIMES.equals(dataId)) {
                globalLockRetryTimes = NumberUtils.toInt(newValue, defaultRetryTimes);
            }
        }

        public int getGlobalLockRetryInterval() {
            return globalLockRetryInterval;
        }

        public int getGlobalLockRetryTimes() {
            return globalLockRetryTimes;
        }
    }
}
