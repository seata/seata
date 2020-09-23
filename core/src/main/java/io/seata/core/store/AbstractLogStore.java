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
package io.seata.core.store;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;

/**
 * The type Abstract log store.
 *
 * @author wang.liang
 */
public abstract class AbstractLogStore<G extends GlobalTransactionModel, B extends BranchTransactionModel>
        implements LogStore<G, B> {

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Override
    public long getCurrentMaxSessionId(long high, long low) {
        return 0;
    }
}
