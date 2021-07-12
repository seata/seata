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

import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_SOFA_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = REGISTRY_SOFA_PREFIX)
public class RegistrySofaProperties {
    private String serverAddr = "127.0.0.1:9603";
    private String application = "seata-server";
    private String region = "DEFAULT_ZONE";
    private String datacenter = "DefaultDataCenter";
    private String cluster = "default";
    private String group = "SEATA_GROUP";
    private String addressWaitTime = "3000";

    public String getServerAddr() {
        return serverAddr;
    }

    public RegistrySofaProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

    public String getApplication() {
        return application;
    }

    public RegistrySofaProperties setApplication(String application) {
        this.application = application;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public RegistrySofaProperties setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public RegistrySofaProperties setDatacenter(String datacenter) {
        this.datacenter = datacenter;
        return this;
    }

    public String getCluster() {
        return cluster;
    }

    public RegistrySofaProperties setCluster(String cluster) {
        this.cluster = cluster;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public RegistrySofaProperties setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getAddressWaitTime() {
        return addressWaitTime;
    }

    public RegistrySofaProperties setAddressWaitTime(String addressWaitTime) {
        this.addressWaitTime = addressWaitTime;
        return this;
    }
}
