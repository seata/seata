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
import io.seata.spring.boot.autoconfigure.properties.server.store.StoreProperties;
import io.seata.spring.boot.autoconfigure.properties.server.store.StoreRedisProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import static io.seata.spring.boot.autoconfigure.StarterConstants.METRICS_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVER_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVER_RECOVERY_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVER_UNDO_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_DB_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_FILE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_LOCK_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_REDIS_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_REDIS_SENTINEL_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_REDIS_SINGLE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_SESSION_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 * @author wang.liang
 */
public class SeataServerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        PROPERTY_BEAN_MAP.put(SERVER_PREFIX, ServerProperties.class);
        PROPERTY_BEAN_MAP.put(SERVER_UNDO_PREFIX, ServerUndoProperties.class);
        PROPERTY_BEAN_MAP.put(SERVER_RECOVERY_PREFIX, ServerRecoveryProperties.class);
        PROPERTY_BEAN_MAP.put(METRICS_PREFIX, MetricsProperties.class);
        PROPERTY_BEAN_MAP.put(STORE_PREFIX, StoreProperties.class);
        PROPERTY_BEAN_MAP.put(STORE_SESSION_PREFIX, StoreProperties.Session.class);
        PROPERTY_BEAN_MAP.put(STORE_LOCK_PREFIX, StoreProperties.Lock.class);
        PROPERTY_BEAN_MAP.put(STORE_FILE_PREFIX, StoreFileProperties.class);
        PROPERTY_BEAN_MAP.put(STORE_DB_PREFIX, StoreDBProperties.class);
        PROPERTY_BEAN_MAP.put(STORE_REDIS_PREFIX, StoreRedisProperties.class);
        PROPERTY_BEAN_MAP.put(STORE_REDIS_SINGLE_PREFIX, StoreRedisProperties.Single.class);
        PROPERTY_BEAN_MAP.put(STORE_REDIS_SENTINEL_PREFIX, StoreRedisProperties.Sentinel.class);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
