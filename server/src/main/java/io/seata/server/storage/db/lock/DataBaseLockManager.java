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
package io.seata.server.storage.db.lock;

import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import io.seata.common.executor.Initialize;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.server.lock.AbstractLockManager;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;

/**
 * The type db lock manager.
 *
 * @author zjinlei
 */
@LoadLevel(name = "db")
public class DataBaseLockManager extends AbstractLockManager implements Initialize {

    /**
     * The locker.
     */
    private Locker locker;

    @Override
    public void init() {
        // init dataSource
        String datasourceType = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        DataSource lockStoreDataSource = EnhancedServiceLoader.load(DataSourceProvider.class, datasourceType).provide();
        locker = new DataBaseLocker(lockStoreDataSource);
    }

    @Override
    public boolean releaseLock(BranchSession branchSession) throws TransactionException {
        try {
            return getLocker().releaseLock(branchSession.getXid(), branchSession.getBranchId());
        } catch (Exception t) {
            LOGGER.error("unLock error, xid {}, branchId:{}", branchSession.getXid(), branchSession.getBranchId(), t);
            return false;
        }
    }

    @Override
    public Locker getLocker(BranchSession branchSession) {
        return locker;
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        List<BranchSession> branchSessions = globalSession.getBranchSessions();
        if (CollectionUtils.isEmpty(branchSessions)) {
            return true;
        }
        List<Long> branchIds = branchSessions.stream().map(BranchSession::getBranchId).collect(Collectors.toList());
        try {
            return getLocker().releaseLock(globalSession.getXid(), branchIds);
        } catch (Exception t) {
            LOGGER.error("unLock globalSession error, xid:{} branchIds:{}", globalSession.getXid(),
                CollectionUtils.toString(branchIds), t);
            return false;
        }
    }

}
