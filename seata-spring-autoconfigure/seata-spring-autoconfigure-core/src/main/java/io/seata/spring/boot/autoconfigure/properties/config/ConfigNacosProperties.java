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

import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_NACOS_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = CONFIG_NACOS_PREFIX)
public class ConfigNacosProperties {
    private String serverAddr = "127.0.0.1:8848";
    private String namespace = "";
    private String group = "SEATA_GROUP";
    private String username = "";
    private String password = "";
    private String dataId = "seata.properties";

    public String getServerAddr() {
        return serverAddr;
    }

    public ConfigNacosProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public ConfigNacosProperties setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public ConfigNacosProperties setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public ConfigNacosProperties setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public ConfigNacosProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getDataId() {
        return dataId;
    }

    public ConfigNacosProperties setDataId(String dataId) {
        this.dataId = dataId;
        return this;
    }
}
