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

import io.seata.spring.boot.autoconfigure.properties.server.MetricsProperties;
import io.seata.spring.boot.autoconfigure.properties.server.ServerProperties;
import io.seata.spring.boot.autoconfigure.properties.server.ServerRecoveryProperties;
import io.seata.spring.boot.autoconfigure.properties.server.ServerUndoProperties;
import io.seata.spring.boot.autoconfigure.properties.server.store.StoreDBProperties;
import io.seata.spring.boot.autoconfigure.properties.server.store.StoreFileProperties;
import io.seata.spring.boot.autoconfigure.properties.StoreProperties;
import io.seata.spring.boot.autoconfigure.properties.server.store.StoreRedisProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * @author xingfudeshi@gmail.com
 */
public class ServerPropertiesTest {
    private static AnnotationConfigApplicationContext context;

    @BeforeAll
    public static void initContext() {
        context = new AnnotationConfigApplicationContext("io.seata.spring.boot.autoconfigure.properties");
    }

    @Test
    public void testServerProperties() {
        assertFalse(context.getBean(ServerProperties.class).getRollbackRetryTimeoutUnlockEnable());
    }

    @Test
    public void testServerRecoveryProperties() {
        assertEquals(context.getBean(ServerRecoveryProperties.class).getAsynCommittingRetryPeriod(), 1000);
    }

    @Test
    public void testServerUndoProperties() {
        assertEquals(context.getBean(ServerUndoProperties.class).getLogDeletePeriod(), 86400000);
    }

    @Test
    public void testMetricsProperties() {
        assertEquals(context.getBean(MetricsProperties.class).getExporterList(), "prometheus");
    }

    @Test
    public void testStoreProperties() {
        assertEquals(context.getBean(StoreProperties.class).getMode(), "file");
    }

    @Test
    public void testStoreFileProperties() {
        assertEquals(context.getBean(StoreFileProperties.class).getDir(), "sessionStore");
    }

    @Test
    public void testStoreDBProperties() {
        assertEquals(context.getBean(StoreDBProperties.class).getDatasource(), "druid");
    }

    @Test
    public void testStoreRedisProperties() {
        assertEquals(context.getBean(StoreRedisProperties.class).getMode(), "single");
    }

    @Test
    public void testStoreRedisPropertiesSingle() {
        assertEquals(context.getBean(StoreRedisProperties.Single.class).getHost(), "127.0.0.1");
    }

    @Test
    public void testStoreRedisPropertiesSentinel() {
        assertEquals(context.getBean(StoreRedisProperties.Sentinel.class).getSentinelHosts(), "");
    }

    @AfterAll
    public static void closeContext() {
        context.close();
    }
}
