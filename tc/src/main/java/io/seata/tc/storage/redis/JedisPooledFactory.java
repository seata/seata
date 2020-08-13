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
package io.seata.tc.storage.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import static io.seata.core.constants.DefaultValues.DEFAULT_REDIS_DATABASE;
import static io.seata.core.constants.DefaultValues.DEFAULT_REDIS_HOST;
import static io.seata.core.constants.DefaultValues.DEFAULT_REDIS_MAXCONN;
import static io.seata.core.constants.DefaultValues.DEFAULT_REDIS_MINCONN;
import static io.seata.core.constants.DefaultValues.DEFAULT_REDIS_PORT;

/**
 * @author funkye
 */
public class JedisPooledFactory {
    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(JedisPooledFactory.class);

    private static volatile JedisPool jedisPool = null;

    private static final String HOST = DEFAULT_REDIS_HOST;

    private static final int PORT = DEFAULT_REDIS_PORT;

    private static final int MINCONN = DEFAULT_REDIS_MINCONN;

    private static final int MAXCONN = DEFAULT_REDIS_MAXCONN;

    private static final int DATABASE = DEFAULT_REDIS_DATABASE;

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    /**
     * get the RedisPool instance (singleton)
     * 
     * @return redisPool
     */
    public static JedisPool getJedisPoolInstance(JedisPool... jedisPools) {
        if (jedisPool == null) {
            synchronized (JedisPooledFactory.class) {
                if (jedisPool == null) {
                    if (jedisPools != null && jedisPools.length > 0) {
                        jedisPool = jedisPools[0];
                    } else {
                        String password = CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_PASSWORD);
                        if (StringUtils.isBlank(password)) {
                            password = null;
                        }
                        JedisPoolConfig poolConfig = new JedisPoolConfig();
                        poolConfig.setMinIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MIN_CONN, MINCONN));
                        poolConfig.setMaxIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MAX_CONN, MAXCONN));
                        jedisPool =
                            new JedisPool(poolConfig, CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_HOST, HOST),
                                CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_PORT, PORT), 60000, password,
                                CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_DATABASE, DATABASE));
                    }
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("initialization of the build redis connection pool is complete");
                    }
                }
            }
        }
        return jedisPool;
    }

    /**
     * get an instance of Jedis (connection) from the connection pool
     * 
     * @return jedis
     */
    public static Jedis getJedisInstance() {
        return getJedisPoolInstance().getResource();
    }

}
