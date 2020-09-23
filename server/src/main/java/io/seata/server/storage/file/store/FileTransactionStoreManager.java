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
package io.seata.server.storage.file.store;

import java.io.IOException;
import java.util.List;

import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.GlobalCondition;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.Reloadable;
import io.seata.server.store.AbstractTransactionStoreManager;

/**
 * The type File transaction store manager.
 *
 * @author slievrly
 */
public class FileTransactionStoreManager extends AbstractTransactionStoreManager<GlobalSession, BranchSession>
    implements Reloadable {

    //region Constructor

    /**
     * Instantiates a new File transaction store manager.
     *
     * @param fullFileName the dir path
     * @throws IOException the io exception
     */
    public FileTransactionStoreManager(String fullFileName) throws IOException {
        //init logStore
        super.logStore = new LogStoreFileDAO(fullFileName);

        // init logQueryLimit
        int logQueryLimit = ConfigurationFactory.getInstance().getInt(
                ConfigurationKeys.SERVICE_SESSION_RELOAD_READ_SIZE,
                AbstractTransactionStoreManager.DEFAULT_LOG_QUERY_LIMIT);
        super.logQueryLimit = logQueryLimit;
    }

    //endregion

    //region Override TransactionStoreManager

    @Override
    public GlobalSession readSession(String xid, boolean withBranchSessions) {
        //global transaction
        GlobalSession globalTransactionDO = logStore.getGlobalTransactionDO(xid);
        //withBranchSessions without process in memory
        return globalTransactionDO;
    }

    @Override
    public List<GlobalSession> readSession(GlobalCondition sessionCondition, boolean withBranchSessions) {
        if (sessionCondition.getPageSize() <= 0) {
            sessionCondition.setPageSize(logQueryLimit <= 0 ? DEFAULT_LOG_QUERY_LIMIT : logQueryLimit);
        }

        //global transactions
        List<GlobalSession> globalTransactionDOs = logStore.queryGlobalTransactionDO(sessionCondition);
        //withBranchSessions without process in memory
        return globalTransactionDOs;
    }

    @Override
    public void shutdown() {
        ((LogStoreFileDAO) super.logStore).shutdown();
    }

    //endregion

    //region Override Reloadable

    @Override
    public void reload() {
        ((Reloadable) super.logStore).reload();
    }

    //endregion
}
