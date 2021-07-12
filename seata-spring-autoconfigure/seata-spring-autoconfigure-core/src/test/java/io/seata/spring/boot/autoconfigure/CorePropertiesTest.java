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

import io.seata.spring.boot.autoconfigure.properties.ThreadFactoryProperties;
import io.seata.spring.boot.autoconfigure.properties.TransportProperties;
import io.seata.spring.boot.autoconfigure.properties.ShutdownProperties;
import io.seata.spring.boot.autoconfigure.properties.LogProperties;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author xingfudeshi@gmail.com
 */
public class CorePropertiesTest {
    private static AnnotationConfigApplicationContext context;

    @BeforeAll
    public static void initContext() {
        context = new AnnotationConfigApplicationContext("io.seata.spring.boot.autoconfigure.properties");
    }


    @Test
    public void testThreadFactoryProperties() {
        assertEquals("NettyBoss", context.getBean(ThreadFactoryProperties.class).getBossThreadPrefix());
        assertEquals("NettyServerNIOWorker", context.getBean(ThreadFactoryProperties.class).getWorkerThreadPrefix());
        assertEquals("NettyServerBizHandler", context.getBean(ThreadFactoryProperties.class).getServerExecutorThreadPrefix());
        assertFalse(context.getBean(ThreadFactoryProperties.class).isShareBossWorker());
        assertEquals("NettyClientSelector", context.getBean(ThreadFactoryProperties.class).getClientSelectorThreadPrefix());
        assertEquals(1, context.getBean(ThreadFactoryProperties.class).getClientSelectorThreadSize());
        assertEquals("NettyClientWorkerThread", context.getBean(ThreadFactoryProperties.class).getClientWorkerThreadPrefix());
        assertEquals(1, context.getBean(ThreadFactoryProperties.class).getBossThreadSize());
        assertEquals("Default", context.getBean(ThreadFactoryProperties.class).getWorkerThreadSize());
    }

    @Test
    public void testTransportProperties() {
        assertEquals("TCP", context.getBean(TransportProperties.class).getType());
        assertEquals("NIO", context.getBean(TransportProperties.class).getServer());
        assertTrue(context.getBean(TransportProperties.class).isHeartbeat());
        assertEquals("seata", context.getBean(TransportProperties.class).getSerialization());
        assertEquals("none", context.getBean(TransportProperties.class).getCompressor());
        assertTrue(context.getBean(TransportProperties.class).isEnableClientBatchSendRequest());
    }

    @Test
    public void testShutdownProperties() {
        assertEquals(3L, context.getBean(ShutdownProperties.class).getWait());
    }

    @Test
    public void testLogProperties() {
        assertEquals(100, context.getBean(LogProperties.class).getExceptionRate());
    }

    @Test
    public void testConfigApolloProperties() {
        assertEquals("seata-server", context.getBean(ConfigApolloProperties.class).getAppId());
        assertEquals("http://127.0.0.1:8801", context.getBean(ConfigApolloProperties.class).getApolloMeta());
    }

    @Test
    public void testConfigConsulProperties() {
        assertEquals("127.0.0.1:8500", context.getBean(ConfigConsulProperties.class).getServerAddr());
    }

    @Test
    public void testConfigEtcd3Properties() {
        assertEquals("http://127.0.0.1:2379", context.getBean(ConfigEtcd3Properties.class).getServerAddr());
    }

    @Test
    public void testConfigFileProperties() {
        assertEquals("file.conf", context.getBean(ConfigFileProperties.class).getName());
    }

    @Test
    public void testConfigNacosProperties() {
        assertEquals("127.0.0.1:8848", context.getBean(ConfigNacosProperties.class).getServerAddr());
        assertEquals("", context.getBean(ConfigNacosProperties.class).getNamespace());
    }

    @Test
    public void testConfigProperties() {
        assertEquals("file", context.getBean(ConfigProperties.class).getType());
    }

    @Test
    public void testConfigZooKeeperProperties() {
        assertEquals("127.0.0.1:2181", context.getBean(ConfigZooKeeperProperties.class).getServerAddr());
        assertEquals(6000L, context.getBean(ConfigZooKeeperProperties.class).getSessionTimeout());
        assertEquals(2000L, context.getBean(ConfigZooKeeperProperties.class).getConnectTimeout());
    }

    @Test
    public void testConfigCustomProperties() {
        assertEquals("", context.getBean(ConfigCustomProperties.class).getName());
    }

    @Test
    public void testRegistryConsulProperties() {
        assertEquals("default", context.getBean(RegistryConsulProperties.class).getCluster());
        assertEquals("127.0.0.1:8500", context.getBean(RegistryConsulProperties.class).getServerAddr());
    }

    @Test
    public void testRegistryEtcd3Properties() {
        assertEquals("default", context.getBean(RegistryEtcd3Properties.class).getCluster());
        assertEquals("http://127.0.0.1:2379", context.getBean(RegistryEtcd3Properties.class).getServerAddr());
    }

    @Test
    public void testRegistryEurekaProperties() {
        assertEquals("seata-server", context.getBean(RegistryEurekaProperties.class).getApplication());
        assertEquals("http://127.0.0.1:8761/eureka", context.getBean(RegistryEurekaProperties.class).getServiceUrl());
        assertEquals("1", context.getBean(RegistryEurekaProperties.class).getWeight());
    }

    @Test
    public void testRegistryNacosProperties() {
        assertEquals("127.0.0.1:8848", context.getBean(RegistryNacosProperties.class).getServerAddr());
        assertEquals("", context.getBean(RegistryNacosProperties.class).getNamespace());
        assertEquals("SEATA_GROUP", context.getBean(RegistryNacosProperties.class).getGroup());
        assertEquals("default", context.getBean(RegistryNacosProperties.class).getCluster());
        assertEquals("", context.getBean(RegistryNacosProperties.class).getUsername());
        assertEquals("", context.getBean(RegistryNacosProperties.class).getPassword());
        assertEquals("seata-server", context.getBean(RegistryNacosProperties.class).getApplication());
    }

    @Test
    public void testRegistryProperties() {
        assertEquals("file", context.getBean(RegistryProperties.class).getType());
    }


    @Test
    public void testRegistryRedisProperties() {
        assertEquals("127.0.0.1:6379", context.getBean(RegistryRedisProperties.class).getServerAddr());
        assertEquals(0, context.getBean(RegistryRedisProperties.class).getDb());
        assertEquals("", context.getBean(RegistryRedisProperties.class).getPassword());
        assertEquals("default", context.getBean(RegistryRedisProperties.class).getCluster());
        assertEquals(0, context.getBean(RegistryRedisProperties.class).getTimeout());
    }

    @Test
    public void testRegistrySofaProperties() {
        assertEquals("127.0.0.1:9603", context.getBean(RegistrySofaProperties.class).getServerAddr());
        assertEquals("seata-server", context.getBean(RegistrySofaProperties.class).getApplication());
        assertEquals("DEFAULT_ZONE", context.getBean(RegistrySofaProperties.class).getRegion());
        assertEquals("DefaultDataCenter", context.getBean(RegistrySofaProperties.class).getDatacenter());
        assertEquals("default", context.getBean(RegistrySofaProperties.class).getCluster());
        assertEquals("SEATA_GROUP", context.getBean(RegistrySofaProperties.class).getGroup());
        assertEquals("3000", context.getBean(RegistrySofaProperties.class).getAddressWaitTime());
    }

    @Test
    public void testRegistryZooKeeperProperties() {
        assertEquals("default", context.getBean(RegistryZooKeeperProperties.class).getCluster());
        assertEquals("127.0.0.1:2181", context.getBean(RegistryZooKeeperProperties.class).getServerAddr());
        assertEquals(6000L, context.getBean(RegistryZooKeeperProperties.class).getSessionTimeout());
        assertEquals(2000L, context.getBean(RegistryZooKeeperProperties.class).getConnectTimeout());
    }

    @Test
    public void testRegistryCustomProperties() {
        assertEquals("", context.getBean(RegistryCustomProperties.class).getName());
    }


    @AfterAll
    public static void closeContext() {
        context.close();
    }
}
