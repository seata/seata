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

import io.seata.spring.boot.autoconfigure.properties.LogProperties;
import io.seata.spring.boot.autoconfigure.properties.ShutdownProperties;
import io.seata.spring.boot.autoconfigure.properties.ThreadFactoryProperties;
import io.seata.spring.boot.autoconfigure.properties.TransportProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigApolloProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigConsulProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigCustomProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigEtcd3Properties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigFileProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigNacosProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigZooKeeperProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryConsulProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryCustomProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryEtcd3Properties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryEurekaProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryNacosProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryRedisProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistrySofaProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryZooKeeperProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_APOLLO_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_CONSUL_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_CUSTOM_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_ETCD3_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_FILE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_NACOS_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_ZK_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.LOG_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_CONSUL_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_CUSTOM_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_ETCD3_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_EUREKA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_NACOS_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_REDIS_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_SOFA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_ZK_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SHUTDOWN_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.THREAD_FACTORY_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.TRANSPORT_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 * @author wang.liang
 */
public class SeataCoreEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        PROPERTY_BEAN_MAP.put(CONFIG_PREFIX, ConfigProperties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_FILE_PREFIX, ConfigFileProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_PREFIX, RegistryProperties.class);

        PROPERTY_BEAN_MAP.put(CONFIG_NACOS_PREFIX, ConfigNacosProperties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_CONSUL_PREFIX, ConfigConsulProperties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_ZK_PREFIX, ConfigZooKeeperProperties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_APOLLO_PREFIX, ConfigApolloProperties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_ETCD3_PREFIX, ConfigEtcd3Properties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_CUSTOM_PREFIX, ConfigCustomProperties.class);

        PROPERTY_BEAN_MAP.put(REGISTRY_CONSUL_PREFIX, RegistryConsulProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_ETCD3_PREFIX, RegistryEtcd3Properties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_EUREKA_PREFIX, RegistryEurekaProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_NACOS_PREFIX, RegistryNacosProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_REDIS_PREFIX, RegistryRedisProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_SOFA_PREFIX, RegistrySofaProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_ZK_PREFIX, RegistryZooKeeperProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_CUSTOM_PREFIX, RegistryCustomProperties.class);

        PROPERTY_BEAN_MAP.put(THREAD_FACTORY_PREFIX, ThreadFactoryProperties.class);
        PROPERTY_BEAN_MAP.put(TRANSPORT_PREFIX, TransportProperties.class);
        PROPERTY_BEAN_MAP.put(SHUTDOWN_PREFIX, ShutdownProperties.class);
        PROPERTY_BEAN_MAP.put(LOG_PREFIX, LogProperties.class);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
