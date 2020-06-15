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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.CaseFormat;
import io.seata.common.holder.ObjectHolder;
import io.seata.config.Configuration;
import io.seata.config.ExtConfigurationProvider;
import org.apache.commons.lang.StringUtils;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.ApplicationContext;

import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_MAP;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_GROUPLIST;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_VGROUP_MAPPING;
import static io.seata.common.util.StringFormatUtils.DOT;

/**
 * @author xingfudeshi@gmail.com
 */
public class SpringBootConfigurationProvider implements ExtConfigurationProvider {
    private static final String INTERCEPT_METHOD_PREFIX = "get";

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
                    if (null != result) {
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

    private Object get(String dataId, Object defaultValue, long timeoutMills) throws IllegalAccessException {
        return get(dataId, defaultValue);

    }

    private Object get(String dataId, Object defaultValue) throws IllegalAccessException {
        Object result = get(dataId);
        if (null == result) {
            return defaultValue;
        }
        return result;
    }

    private Object get(String dataId) throws IllegalAccessException {
        String propertySuffix = getPropertySuffix(dataId);
        Class propertyClass = getPropertyClass(getPropertyPrefix(dataId));
        if (null != propertyClass) {
            Object propertyObject = ObjectHolder.INSTANCE.getObject(ApplicationContext.class).getBean(propertyClass);
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

        int dotCount = StringUtils.countMatches(dataId, String.valueOf(DOT));
        String posKey = dataId;

        while (dotCount-- > 0) {
            posKey = dataId.substring(0, StringUtils.lastIndexOf(posKey, String.valueOf(DOT)));
            if (PROPERTY_MAP.containsKey(posKey)) {
                return posKey;
            }
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

        int dotCount = StringUtils.countMatches(dataId, String.valueOf(DOT));

        String posKey = dataId;
        while (dotCount-- > 0) {
            posKey = dataId.substring(0, StringUtils.lastIndexOf(posKey, String.valueOf(DOT)));
            if (PROPERTY_MAP.containsKey(posKey)) {
                break;
            }
        }
        String result = StringUtils.substringAfterLast(dataId, posKey + DOT);
        if (result.contains(String.valueOf(DOT))) {
            return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, result.replace(String.valueOf(DOT), "-"));
        }

        return StringUtils.substringAfterLast(dataId, String.valueOf(DOT));
    }


    /**
     * Get property class
     *
     * @param propertyPrefix
     * @return propertyClass
     */
    private Class getPropertyClass(String propertyPrefix) {
        return PROPERTY_MAP.entrySet().stream()
            .filter(e -> propertyPrefix.equals(e.getKey()))
            .findAny()
            .map(Map.Entry::getValue)
            .orElse(null);
    }
}
