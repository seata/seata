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
package io.seata.server.storage.db.session;

import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.server.session.AbstractSessionManager;
import io.seata.server.storage.db.store.DataBaseTransactionStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Data base session manager.
 *
 * @author zhangsen
 */
@LoadLevel(name = "db", scope = Scope.PROTOTYPE)
public class DataBaseSessionManager extends AbstractSessionManager {

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(DataBaseSessionManager.class);

    /**
     * Instantiates a new Data base session manager.
     */
    public DataBaseSessionManager() {
        super(DataBaseTransactionStoreManager.getInstance());
    }
}
