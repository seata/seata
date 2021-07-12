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

import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_ETCD3_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = REGISTRY_ETCD3_PREFIX)
public class RegistryEtcd3Properties {
    private String cluster = "default";
    private String serverAddr = "http://127.0.0.1:2379";

    public String getCluster() {
        return cluster;
    }

    public RegistryEtcd3Properties setCluster(String cluster) {
        this.cluster = cluster;
        return this;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public RegistryEtcd3Properties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }
}
