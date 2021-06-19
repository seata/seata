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
package io.seata.rm.tcc.config;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.rpc.Disposable;
import io.seata.rm.tcc.TCCFenceHandler;
import io.seata.rm.tcc.constant.TCCFenceCleanMode;
import io.seata.rm.tcc.constant.TCCFenceConstant;
import io.seata.rm.tcc.exception.TCCFenceException;
import io.seata.rm.tcc.store.db.TCCFenceStoreDataBaseDAO;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * TCC Fence Config
 *
 * @author kaka2code
 */
public class TCCFenceConfig implements InitializingBean, Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCCFenceConfig.class);
    /**
     * TCC fence datasource
     */
    private final DataSource dataSource;
    /**
     * TCC fence transactionManager
     */
    private final PlatformTransactionManager transactionManager;
    /**
     * TCC fence clean scheduled thread pool
     */
    private final ScheduledThreadPoolExecutor tccFenceClean = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("tccFenceClean", 1));
    /**
     * TCC fence clean mode
     */
    private TCCFenceCleanMode cleanMode;
    /**
     * TCC fence clean period
     */
    private int cleanPeriod;
    /**
     * TCC fence log table name
     */
    private String logTableName;

    public TCCFenceConfig(final DataSource dataSource, final PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setCleanMode(TCCFenceCleanMode cleanMode) {
        this.cleanMode = cleanMode;
    }

    public void setCleanPeriod(int cleanPeriod) {
        this.cleanPeriod = cleanPeriod;
    }

    public void setLogTableName(String logTableName) {
        this.logTableName = logTableName;
    }

    /**
     * init tcc fence clean task
     */
    private void initCleanTask() {
        if (!TCCFenceCleanMode.Close.equals(cleanMode)) {
            // Set timeUtil value and cleanPeriod field according to clean mode
            TimeUnit timeUnit;
            if (TCCFenceCleanMode.Hour.equals(cleanMode)) {
                timeUnit = TimeUnit.HOURS;
                if (cleanPeriod == 0) {
                    cleanPeriod = TCCFenceConstant.DEFAULT_CLEAN_HOUR;
                }
            } else if (TCCFenceCleanMode.Minute.equals(cleanMode)) {
                timeUnit = TimeUnit.MINUTES;
                if (cleanPeriod == 0) {
                    cleanPeriod = TCCFenceConstant.DEFAULT_CLEAN_MINUTE;
                }
            } else {
                // Default or clean mode equals TCCFenceCleanMode.Day
                timeUnit = TimeUnit.DAYS;
                if (cleanPeriod == 0) {
                    cleanPeriod = TCCFenceConstant.DEFAULT_CLEAN_DAY;
                }
            }
            tccFenceClean.scheduleWithFixedDelay(() -> {
                Date timeBefore;
                if (TCCFenceCleanMode.Hour.equals(cleanMode)) {
                    timeBefore = DateUtils.addHours(new Date(), -cleanPeriod);
                } else if (TCCFenceCleanMode.Minute.equals(cleanMode)) {
                    timeBefore = DateUtils.addMinutes(new Date(), -cleanPeriod);
                } else {
                    timeBefore = DateUtils.addDays(new Date(), -cleanPeriod);
                }
                try {
                    int deletedRowCount = TCCFenceHandler.deleteFenceByDate(timeBefore);
                    if (deletedRowCount > 0) {
                        LOGGER.info("TCC fence clean task executed success, timeBefore: {}, deleted row count: {}",
                                timeBefore, deletedRowCount);
                    }
                } catch (RuntimeException e) {
                    LOGGER.error("delete fence log failed, timeBefore: {}", timeBefore, e);
                }
            }, 0, cleanPeriod, timeUnit);
        }
    }

    @Override
    public void destroy() {
        // shutdown delete tcc fence log task
        tccFenceClean.shutdown();
    }

    @Override
    public void afterPropertiesSet() {
        // set log table name
        if (logTableName != null) {
            TCCFenceStoreDataBaseDAO.getInstance().setLogTableName(logTableName);
        }
        if (dataSource != null) {
            // set dataSource
            TCCFenceHandler.setDataSource(dataSource);
        } else {
            throw new TCCFenceException(FrameworkErrorCode.DateSourceNeedInjected);
        }
        if (transactionManager != null) {
            // set transaction template
            TCCFenceHandler.setTransactionTemplate(new TransactionTemplate(transactionManager));
        } else {
            throw new TCCFenceException(FrameworkErrorCode.TransactionManagerNeedInjected);
        }
        // init tcc fence clean task
        initCleanTask();
    }
}

