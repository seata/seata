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
package io.seata.spring.boot.autoconfigure.properties.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_CONSUL_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = CONFIG_CONSUL_PREFIX)
public class ConfigConsulProperties {
    private String serverAddr = "127.0.0.1:8500";
    private String key = "seata.properties";
    private String aclToken = "";

    public String getServerAddr() {
        return serverAddr;
    }

    public ConfigConsulProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

    public String getKey() {
        return key;
    }

    public ConfigConsulProperties setKey(String key) {
        this.key = key;
        return this;
    }
    public String getAclToken() {
        return aclToken;
    }

    public ConfigConsulProperties setAclToken(String aclToken) {
        this.aclToken = aclToken;
        return this;
    }
}
