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
package io.seata.spring.boot.autoconfigure.provider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.seata.config.Configuration;
import io.seata.config.ExtConfigurationProvider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import static io.seata.common.util.StringFormatUtils.DOT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_RM_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_TM_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.COMPRESS_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_APOLLO_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_CONSUL_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_CUSTOM_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_ETCD3_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_FILE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_NACOS_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_ZK_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.LOCK_PREFIX;
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
import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SHUTDOWN_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_GROUPLIST;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_VGROUP_MAPPING;
import static io.seata.spring.boot.autoconfigure.StarterConstants.THREAD_FACTORY_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.TRANSPORT_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.UNDO_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.UNDO_REDIS_SENTINEL_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.UNDO_REDIS_SINGLE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.UNDO_CACHE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.UNDO_REDIS_PREFIX;
/**
 * @author xingfudeshi@gmail.com
 */
public class SpringBootConfigurationProvider implements ExtConfigurationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootConfigurationProvider.class);
    private static final String INTERCEPT_METHOD_PREFIX = "get";

    static {
        PROPERTY_BEAN_MAP.putIfAbsent(SEATA_PREFIX, new CompletableFuture<>());

        PROPERTY_BEAN_MAP.putIfAbsent(CLIENT_RM_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(CLIENT_TM_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(LOCK_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(SERVICE_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(SHUTDOWN_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(THREAD_FACTORY_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(UNDO_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(COMPRESS_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(UNDO_REDIS_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(UNDO_REDIS_SENTINEL_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(UNDO_REDIS_SINGLE_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(UNDO_CACHE_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(LOG_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(TRANSPORT_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(CONFIG_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(CONFIG_FILE_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(REGISTRY_PREFIX, new CompletableFuture<>());

        PROPERTY_BEAN_MAP.putIfAbsent(CONFIG_NACOS_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(CONFIG_CONSUL_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(CONFIG_ZK_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(CONFIG_APOLLO_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(CONFIG_ETCD3_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(CONFIG_CUSTOM_PREFIX, new CompletableFuture<>());

        PROPERTY_BEAN_MAP.putIfAbsent(REGISTRY_CONSUL_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(REGISTRY_ETCD3_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(REGISTRY_EUREKA_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(REGISTRY_NACOS_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(REGISTRY_REDIS_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(REGISTRY_SOFA_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(REGISTRY_ZK_PREFIX, new CompletableFuture<>());
        PROPERTY_BEAN_MAP.putIfAbsent(REGISTRY_CUSTOM_PREFIX, new CompletableFuture<>());
    }

    @Override
    public Configuration provide(Configuration originalConfiguration) {
        return (Configuration) Enhancer.create(originalConfiguration.getClass(), new MethodInterceptor() {
            @Override
            public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy)
                throws Throwable {
                if (method.getName().startsWith(INTERCEPT_METHOD_PREFIX) && args.length > 0) {
                    Object result = null;
                    String rawDataId = (String) args[0];
                    if (args.length == 1) {
                        result = get(convertDataId(rawDataId));
                    } else if (args.length == 2) {
                        result = get(convertDataId(rawDataId), args[1]);
                    } else if (args.length == 3) {
                        result = get(convertDataId(rawDataId), args[1], (Long) args[2]);
                    }
                    if (result != null) {
                        //If the return type is String,need to convert the object to string
                        if (method.getReturnType().equals(String.class)) {
                            return String.valueOf(result);
                        }
                        return result;
                    }
                }

                return method.invoke(originalConfiguration, args);
            }
        });
    }

    private Object get(String dataId, Object defaultValue, long timeoutMills) throws IllegalAccessException, ExecutionException, InterruptedException, TimeoutException {
        return get(dataId, defaultValue);

    }

    private Object get(String dataId, Object defaultValue) throws IllegalAccessException, ExecutionException, InterruptedException, TimeoutException {
        Object result = get(dataId);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    private Object get(String dataId) throws IllegalAccessException, ExecutionException, InterruptedException, TimeoutException {
        String propertyPrefix = getPropertyPrefix(dataId);
        CompletableFuture<Object> propertyFuture = PROPERTY_BEAN_MAP.get(propertyPrefix);
        if (propertyFuture != null) {
            Object propertyObject = propertyFuture.get(10000, TimeUnit.MILLISECONDS);
            String propertySuffix = getPropertySuffix(dataId);
            Optional<Field> fieldOptional = Stream.of(propertyObject.getClass().getDeclaredFields()).filter(
                f -> f.getName().equalsIgnoreCase(propertySuffix)).findAny();
            if (fieldOptional.isPresent()) {
                Field field = fieldOptional.get();
                field.setAccessible(true);
                Object valueObject = field.get(propertyObject);
                if (valueObject instanceof Map) {
                    String key = StringUtils.substringAfterLast(dataId, String.valueOf(DOT));
                    valueObject = ((Map) valueObject).get(key);
                }
                return valueObject;
            }
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Property bean with prefix '{}' was not found in `StarterConstants.PROPERTY_BEAN_MAP`."
                    + " Please inform the {} committer to fix this BUG.", propertyPrefix, SEATA_PREFIX);
            }
        }
        return null;
    }

    /**
     * convert data id
     *
     * @param rawDataId
     * @return dataId
     */
    private String convertDataId(String rawDataId) {
        if (rawDataId.endsWith(SPECIAL_KEY_GROUPLIST)) {
            String suffix = StringUtils.removeEnd(rawDataId, DOT + SPECIAL_KEY_GROUPLIST);
            //change the format of default.grouplist to grouplist.default
            return SERVICE_PREFIX + DOT + SPECIAL_KEY_GROUPLIST + DOT + suffix;
        }
        return SEATA_PREFIX + DOT + rawDataId;
    }

    /**
     * Get property prefix
     *
     * @param dataId
     * @return propertyPrefix
     */
    private String getPropertyPrefix(String dataId) {
        if (dataId.contains(SPECIAL_KEY_VGROUP_MAPPING)) {
            return SERVICE_PREFIX;
        }
        if (dataId.contains(SPECIAL_KEY_GROUPLIST)) {
            return SERVICE_PREFIX;
        }
        return StringUtils.substringBeforeLast(dataId, String.valueOf(DOT));
    }

    /**
     * Get property suffix
     *
     * @param dataId
     * @return propertySuffix
     */
    private String getPropertySuffix(String dataId) {
        if (dataId.contains(SPECIAL_KEY_VGROUP_MAPPING)) {
            return SPECIAL_KEY_VGROUP_MAPPING;
        }
        if (dataId.contains(SPECIAL_KEY_GROUPLIST)) {
            return SPECIAL_KEY_GROUPLIST;
        }
        return StringUtils.substringAfterLast(dataId, String.valueOf(DOT));
    }
}
