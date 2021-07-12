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
package io.seata.spring.boot.autoconfigure.properties.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_REDIS_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = REGISTRY_REDIS_PREFIX)
public class RegistryRedisProperties {
    private String serverAddr = "127.0.0.1:6379";
    private int db = 0;
    private String password = "";
    private String cluster = "default";
    private int timeout = 0;


    public String getServerAddr() {
        return serverAddr;
    }

    public RegistryRedisProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

    public int getDb() {
        return db;
    }

    public RegistryRedisProperties setDb(int db) {
        this.db = db;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public RegistryRedisProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getCluster() {
        return cluster;
    }

    public RegistryRedisProperties setCluster(String cluster) {
        this.cluster = cluster;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public RegistryRedisProperties setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
}
