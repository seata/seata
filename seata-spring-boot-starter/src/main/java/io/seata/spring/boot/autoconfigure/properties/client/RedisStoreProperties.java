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
package io.seata.spring.boot.autoconfigure.properties.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.core.constants.DefaultValues.DEFAULT_REDIS_DATABASE;
import static io.seata.core.constants.DefaultValues.DEFAULT_REDIS_HOST;
import static io.seata.core.constants.DefaultValues.DEFAULT_REDIS_MAXCONN;
import static io.seata.core.constants.DefaultValues.DEFAULT_REDIS_MINCONN;
import static io.seata.core.constants.DefaultValues.DEFAULT_REDIS_PORT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REDIS_STORE_PREFIX;

/**
 * @author wang.liang
 */
@Component
@ConfigurationProperties(prefix = REDIS_STORE_PREFIX)
public class RedisStoreProperties {

    private String host = DEFAULT_REDIS_HOST;
    private int port = DEFAULT_REDIS_PORT;

    private int minConn = DEFAULT_REDIS_MINCONN;
    private int maxConn = DEFAULT_REDIS_MAXCONN;

    private int database = DEFAULT_REDIS_DATABASE;
    private String password;

    public String getHost() {
        return host;
    }

    public RedisStoreProperties setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public RedisStoreProperties setPort(int port) {
        this.port = port;
        return this;
    }

    public int getDatabase() {
        return database;
    }

    public RedisStoreProperties setDatabase(int database) {
        this.database = database;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public RedisStoreProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public int getMaxConn() {
        return maxConn;
    }

    public RedisStoreProperties setMaxConn(int maxConn) {
        this.maxConn = maxConn;
        return this;
    }

    public int getMinConn() {
        return minConn;
    }

    public RedisStoreProperties setMinConn(int minConn) {
        this.minConn = minConn;
        return this;
    }
}
