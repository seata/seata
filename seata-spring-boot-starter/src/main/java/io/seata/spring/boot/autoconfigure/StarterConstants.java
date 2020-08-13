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
package io.seata.spring.boot.autoconfigure;

import java.util.HashMap;

import io.seata.spring.boot.autoconfigure.properties.client.DbStoreProperties;
import io.seata.spring.boot.autoconfigure.properties.client.FileStoreProperties;
import io.seata.spring.boot.autoconfigure.properties.client.LockProperties;
import io.seata.spring.boot.autoconfigure.properties.client.LogProperties;
import io.seata.spring.boot.autoconfigure.properties.client.RedisStoreProperties;
import io.seata.spring.boot.autoconfigure.properties.client.RmProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ServiceProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ShutdownProperties;
import io.seata.spring.boot.autoconfigure.properties.client.StoreProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ThreadFactoryProperties;
import io.seata.spring.boot.autoconfigure.properties.client.TmProperties;
import io.seata.spring.boot.autoconfigure.properties.client.TransportProperties;
import io.seata.spring.boot.autoconfigure.properties.client.UndoProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigApolloProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigConsulProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigEtcd3Properties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigFileProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigNacosProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigZooKeeperProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryConsulProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryEtcd3Properties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryEurekaProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryNacosProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryRedisProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistrySofaProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryZooKeeperProperties;

/**
 * @author xingfudeshi@gmail.com
 */
public class StarterConstants {
    private static final int MAP_CAPACITY = 64;
    public static final String SEATA_PREFIX = "seata";
    public static final String SEATA_SPRING_CLOUD_ALIBABA_PREFIX = "spring.cloud.alibaba.seata";
    public static final String TRANSPORT_PREFIX = SEATA_PREFIX + ".transport";
    public static final String THREAD_FACTORY_PREFIX_KEBAB_STYLE = TRANSPORT_PREFIX + ".thread-factory";
    public static final String THREAD_FACTORY_PREFIX = TRANSPORT_PREFIX + ".threadFactory";
    public static final String SHUTDOWN_PREFIX = TRANSPORT_PREFIX + ".shutdown";
    public static final String SERVICE_PREFIX = SEATA_PREFIX + ".service";
    public static final String CLIENT_PREFIX = SEATA_PREFIX + ".client";
    public static final String CLIENT_RM_PREFIX = CLIENT_PREFIX + ".rm";
    public static final String CLIENT_TM_PREFIX = CLIENT_PREFIX + ".tm";
    public static final String LOCK_PREFIX = CLIENT_RM_PREFIX + ".lock";
    public static final String UNDO_PREFIX = CLIENT_PREFIX + ".undo";
    public static final String LOG_PREFIX = CLIENT_PREFIX + ".log";

    public static final String REGISTRY_PREFIX = SEATA_PREFIX + ".registry";
    public static final String REGISTRY_NACOS_PREFIX = REGISTRY_PREFIX + ".nacos";
    public static final String REGISTRY_EUREKA_PREFIX = REGISTRY_PREFIX + ".eureka";
    public static final String REGISTRY_REDIS_PREFIX = REGISTRY_PREFIX + ".redis";
    public static final String REGISTRY_ZK_PREFIX = REGISTRY_PREFIX + ".zk";
    public static final String REGISTRY_CONSUL_PREFIX = REGISTRY_PREFIX + ".consul";
    public static final String REGISTRY_ETCD3_PREFIX = REGISTRY_PREFIX + ".etcd3";
    public static final String REGISTRY_SOFA_PREFIX = REGISTRY_PREFIX + ".sofa";

    public static final String CONFIG_PREFIX = SEATA_PREFIX + ".config";
    public static final String CONFIG_NACOS_PREFIX = CONFIG_PREFIX + ".nacos";
    public static final String CONFIG_CONSUL_PREFIX = CONFIG_PREFIX + ".consul";
    public static final String CONFIG_ETCD3_PREFIX = CONFIG_PREFIX + ".etcd3";
    public static final String CONFIG_APOLLO_PREFIX = CONFIG_PREFIX + ".apollo";
    public static final String CONFIG_ZK_PREFIX = CONFIG_PREFIX + ".zk";
    public static final String CONFIG_FILE_PREFIX = CONFIG_PREFIX + ".file";

    public static final String STORE_PREFIX = SEATA_PREFIX + ".store";
    public static final String FILE_STORE_PREFIX = STORE_PREFIX + ".file";
    public static final String DB_STORE_PREFIX = STORE_PREFIX + ".db";
    public static final String REDIS_STORE_PREFIX = STORE_PREFIX + ".redis";

    public static final HashMap<String, Class> PROPERTY_MAP = new HashMap<String, Class>(MAP_CAPACITY) {
        private static final long serialVersionUID = -8902807645596274597L;

        {
            put(CLIENT_RM_PREFIX, RmProperties.class);
            put(CLIENT_TM_PREFIX, TmProperties.class);
            put(LOCK_PREFIX, LockProperties.class);
            put(SERVICE_PREFIX, ServiceProperties.class);
            put(SHUTDOWN_PREFIX, ShutdownProperties.class);
            put(THREAD_FACTORY_PREFIX, ThreadFactoryProperties.class);
            put(UNDO_PREFIX, UndoProperties.class);
            put(LOG_PREFIX, LogProperties.class);
            put(TRANSPORT_PREFIX, TransportProperties.class);
            put(CONFIG_PREFIX, ConfigProperties.class);
            put(CONFIG_FILE_PREFIX, ConfigFileProperties.class);
            put(REGISTRY_PREFIX, RegistryProperties.class);

            put(CONFIG_NACOS_PREFIX, ConfigNacosProperties.class);
            put(CONFIG_CONSUL_PREFIX, ConfigConsulProperties.class);
            put(CONFIG_ZK_PREFIX, ConfigZooKeeperProperties.class);
            put(CONFIG_APOLLO_PREFIX, ConfigApolloProperties.class);
            put(CONFIG_ETCD3_PREFIX, ConfigEtcd3Properties.class);

            put(REGISTRY_CONSUL_PREFIX, RegistryConsulProperties.class);
            put(REGISTRY_ETCD3_PREFIX, RegistryEtcd3Properties.class);
            put(REGISTRY_EUREKA_PREFIX, RegistryEurekaProperties.class);
            put(REGISTRY_NACOS_PREFIX, RegistryNacosProperties.class);
            put(REGISTRY_REDIS_PREFIX, RegistryRedisProperties.class);
            put(REGISTRY_SOFA_PREFIX, RegistrySofaProperties.class);
            put(REGISTRY_ZK_PREFIX, RegistryZooKeeperProperties.class);

            put(STORE_PREFIX, StoreProperties.class);
            put(FILE_STORE_PREFIX, FileStoreProperties.class);
            put(DB_STORE_PREFIX, DbStoreProperties.class);
            put(REDIS_STORE_PREFIX, RedisStoreProperties.class);
        }

    };


    /**
     * The following special keys need to be normalized.
     */
    public static final String SPECIAL_KEY_GROUPLIST = "grouplist";
    public static final String SPECIAL_KEY_VGROUP_MAPPING = "vgroupMapping";

}
