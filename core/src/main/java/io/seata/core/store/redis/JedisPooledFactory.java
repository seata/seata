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
package io.seata.core.store.redis;

import io.seata.common.loader.EnhancedServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolAbstract;

/**
 * @author funkye
 */
public class JedisPooledFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(JedisPooledFactory.class);

    public static String ACTIVATE_NAME = "store";

    private static volatile JedisPoolAbstract instance = null;

    public static JedisPoolAbstract getJedisPoolInstance(JedisPool... jedisPool) {
        if (instance == null) {
            synchronized (JedisPooledFactory.class) {
                if (instance == null) {
                    if (jedisPool != null && jedisPool.length > 0) {
                        instance = jedisPool[0];
                    } else {
                        instance = buildJedisPool();
                    }
                }
            }
        }
        return instance;
    }

    public static Jedis getJedisInstance() {
        return getJedisPoolInstance().getResource();
    }

    private static JedisPoolAbstract buildJedisPool() {
        JedisPooledProvider jedisPooledProvider = EnhancedServiceLoader.load(JedisPooledProvider.class, ACTIVATE_NAME);
        if (jedisPooledProvider == null) {
            throw new RuntimeException("jedisPooledProvider is null");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("initialization of the build redis connection pool is complete");
        }
        return jedisPooledProvider.generate();
    }

}
