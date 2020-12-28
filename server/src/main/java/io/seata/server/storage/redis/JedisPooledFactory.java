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
package io.seata.server.storage.redis;

import io.seata.common.Constants;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author funkye
 */
public class JedisPooledFactory {
    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(JedisPooledFactory.class);

    private static volatile JedisPool jedisPool = null;

    private static final String HOST = "127.0.0.1";

    private static final int PORT = 6379;

    private static final int MINCONN = 1;

    private static final int MAXCONN = 10;

    private static final int MAXTOTAL = 100;

    private static final int TIMEOUT = 60000;

    private static final int CONNECTION_TIMEOUT = 60000;

    private static final int SOTIMEOUT = 60000;

    private static final int MAXREDIRECTIONS = 20;

    private static final int DATABASE = 0;

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
                        poolConfig.setMaxTotal(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MAX_TOTAL, MAXTOTAL));
                        jedisPool =
                            new JedisPool(poolConfig, CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_SINGLE_HOST, HOST),
                                CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_SINGLE_PORT, PORT),
                                CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_TIMEOUT, TIMEOUT) , password,
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

    /**
     * parse the nodes string like:127.0.0.1:6379;127.0.0.1:6380;127.0.0.1:6381;127.0.0.1:6382;127.0.0.1:6383;127.0.0.1:6384
     *
     * @param nodes the nodes String
     * @return Set<HostAndPort>
     */
    public static Set<HostAndPort> getHostAndPorts(String nodes) {
        if (StringUtils.isEmpty(nodes)) {
            return null;
        }
        Set<HostAndPort> set = new HashSet<>();
        String[] nodesArr = nodes.split(Constants.REDIS_CLUSTER_NODES_SPLIT_CHAR);
        if (CollectionUtils.isNotEmpty(nodesArr)) {
            for (String hostAndPortStr : nodesArr) {
                if (StringUtils.isNotEmpty(hostAndPortStr)) {
                    String[] hostAndPortArr = hostAndPortStr.split(Constants.IP_PORT_SPLIT_CHAR);
                    HostAndPort hostAndPort = new HostAndPort(hostAndPortArr[0], Integer.valueOf(hostAndPortArr[1]));
                    set.add(hostAndPort);
                }
            }
        }
        return set;
    }

    /**
     * get jediscluster instance
     * @return JedisCluster
     */
    public static JedisCluster getJedisClusterInstance() {
        String nodes = CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_CLUSTER_NODES);
        if (StringUtils.isEmpty(nodes)) {
            throw new ShouldNeverHappenException("Redis cluster nodes must config,Please check the properties of redis cluster!");
        }
        String password = StringUtils.isEmpty(CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_PASSWORD)) ?
                null : CONFIGURATION.getConfig(ConfigurationKeys.STORE_REDIS_PASSWORD);
        Integer maxRedirections = CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_CLUSTER_MAXREDIRECTIONS,MAXREDIRECTIONS);
        Integer soTimeout = CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_SOTIMEOUT,SOTIMEOUT);
        Integer connectionTimeout = CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_CONNECTION_TIMEOUT,CONNECTION_TIMEOUT);
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MIN_CONN, MINCONN));
        poolConfig.setMaxIdle(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MAX_CONN, MAXCONN));
        poolConfig.setMaxTotal(CONFIGURATION.getInt(ConfigurationKeys.STORE_REDIS_MAX_TOTAL, MAXTOTAL));
        Set<HostAndPort> hostAndPorts = getHostAndPorts(nodes);
        JedisCluster jedisCluster = new JedisCluster(hostAndPorts,connectionTimeout,soTimeout,maxRedirections,password,poolConfig);
        return jedisCluster;

    }
}
