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
package io.seata.server.storage.redis.store;

import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalCondition;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.core.store.LogStore;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.SessionStorable;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * @author funkye
 */
public class RedisTransactionStoreManager extends AbstractTransactionStoreManager<GlobalTransactionDO
        , BranchTransactionDO> {

    //region Instance

    private static volatile RedisTransactionStoreManager instance;

    /**
     * Get the instance.
     */
    public static RedisTransactionStoreManager getInstance() {
        if (instance == null) {
            synchronized (RedisTransactionStoreManager.class) {
                if (instance == null) {
                    instance = new RedisTransactionStoreManager();
                }
            }
        }
        return instance;
    }

    //endregion

    //region Constructor

    /**
     * Instantiates a new redis transaction store manager.
     */
    private RedisTransactionStoreManager() {
        //init logQueryLimit
        super.initLogQueryLimit(ConfigurationKeys.STORE_REDIS_QUERY_LIMIT);
    }

    /**
     * Instantiates a new redis transaction store manager.
     *
     * @param logStore      the log store
     * @param logQueryLimit the log query limit
     */
    private RedisTransactionStoreManager(LogStore logStore, int logQueryLimit) {
        super.logStore = logStore;
        super.logQueryLimit = logQueryLimit;
    }

    //endregion

    //region Override TransactionStoreManager

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        if (super.logStore != null) {
            return super.writeSession(logOperation, session);
        } else {
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                return this.create(jedis).writeSession(logOperation, session);
            }
        }
    }

    @Override
    public GlobalSession readSession(String xid, boolean withBranchSessions) {
        if (super.logStore != null) {
            return super.readSession(xid, withBranchSessions);
        } else {
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                return this.create(jedis).readSession(xid, withBranchSessions);
            }
        }
    }

    @Override
    public List<GlobalSession> readSession(GlobalCondition sessionCondition, boolean withBranchSessions) {
        if (super.logStore != null) {
            return super.readSession(sessionCondition, withBranchSessions);
        } else {
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                return this.create(jedis).readSession(sessionCondition, withBranchSessions);
            }
        }
    }

    //endregion

    //region Private

    /**
     * Create store manager.
     *
     * @param jedis the jedis
     * @return the store manager
     */
    private RedisTransactionStoreManager create(Jedis jedis) {
        LogStoreRedisDAO logStore = new LogStoreRedisDAO(jedis, super.logQueryLimit);
        return new RedisTransactionStoreManager(logStore, super.logQueryLimit);
    }

    //endregion
}
