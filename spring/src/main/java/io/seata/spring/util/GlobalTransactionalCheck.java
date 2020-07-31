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
package io.seata.spring.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.google.common.eventbus.Subscribe;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.config.ConfigurationCache;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.event.EventBus;
import io.seata.core.event.GuavaEventBus;
import io.seata.spring.event.DegradeCheckEvent;
import io.seata.tm.TransactionManagerHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.seata.core.constants.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;
import static io.seata.core.constants.DefaultValues.DEFAULT_TM_DEGRADE_CHECK;
import static io.seata.core.constants.DefaultValues.DEFAULT_TM_DEGRADE_CHECK_ALLOW_TIMES;
import static io.seata.core.constants.DefaultValues.DEFAULT_TM_DEGRADE_CHECK_PERIOD;

/**
 * service dynamic processing center
 *
 * @author funkye
 */
public class GlobalTransactionalCheck implements ConfigurationChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionalCheck.class);
    private static int degradeCheckPeriod;
    private static int degradeCheckAllowTimes;
    private static volatile Integer degradeNum = 0;
    private static volatile Integer reachNum = 0;
    private static volatile boolean disable;
    private static ScheduledThreadPoolExecutor executor =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("degradeCheckWorker", 1, true));
    public static volatile boolean degradeCheck;
    public static final EventBus EVENT_BUS = new GuavaEventBus("degradeCheckEventBus", true);

    public GlobalTransactionalCheck() {
        disable = ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
            DEFAULT_DISABLE_GLOBAL_TRANSACTION);
        degradeCheck = ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.CLIENT_DEGRADE_CHECK,
            DEFAULT_TM_DEGRADE_CHECK);
        if (degradeCheck) {
            ConfigurationCache.addConfigListener(ConfigurationKeys.CLIENT_DEGRADE_CHECK, this);
            degradeCheckPeriod = ConfigurationFactory.getInstance()
                .getInt(ConfigurationKeys.CLIENT_DEGRADE_CHECK_PERIOD, DEFAULT_TM_DEGRADE_CHECK_PERIOD);
            degradeCheckAllowTimes = ConfigurationFactory.getInstance()
                .getInt(ConfigurationKeys.CLIENT_DEGRADE_CHECK_ALLOW_TIMES, DEFAULT_TM_DEGRADE_CHECK_ALLOW_TIMES);
            EVENT_BUS.register(this);
            if (degradeCheckPeriod > 0 && degradeCheckAllowTimes > 0) {
                startDegradeCheck();
            }
        }
    }

    /**
     * auto upgrade service detection
     */
    private static void startDegradeCheck() {
        executor.scheduleAtFixedRate(() -> {
            if (degradeCheck) {
                try {
                    String xid = TransactionManagerHolder.get().begin(null, null, "degradeCheck", 60000);
                    TransactionManagerHolder.get().commit(xid);
                    EVENT_BUS.post(new DegradeCheckEvent(true));
                } catch (Exception e) {
                    EVENT_BUS.post(new DegradeCheckEvent(false));
                }
            }
        }, degradeCheckPeriod, degradeCheckPeriod, TimeUnit.MILLISECONDS);
    }

    @Subscribe
    public static void onDegradeCheck(DegradeCheckEvent event) {
        if (event.isRequestSuccess()) {
            if (degradeNum >= degradeCheckAllowTimes) {
                reachNum++;
                if (reachNum >= degradeCheckAllowTimes) {
                    reachNum = 0;
                    degradeNum = 0;
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("the current global transaction has been restored");
                    }
                }
            } else if (degradeNum != 0) {
                degradeNum = 0;
            }
        } else {
            if (degradeNum < degradeCheckAllowTimes) {
                degradeNum++;
                if (degradeNum >= degradeCheckAllowTimes) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("the current global transaction has been automatically downgraded");
                    }
                }
            } else if (reachNum != 0) {
                reachNum = 0;
            }
        }
    }

    public static boolean localDisable() {
        if (!disable) {
            return degradeCheck && degradeNum >= degradeCheckAllowTimes;
        }
        return disable;
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        if (ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION.equals(event.getDataId())) {
            LOGGER.info("{} config changed, old value:{}, new value:{}", ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                disable, event.getNewValue());
            disable = Boolean.parseBoolean(event.getNewValue().trim());
        } else if (ConfigurationKeys.CLIENT_DEGRADE_CHECK.equals(event.getDataId())) {
            degradeCheck = Boolean.parseBoolean(event.getNewValue());
            if (!degradeCheck) {
                degradeNum = 0;
            }
        }
    }

}
