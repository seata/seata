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
package io.seata.rm.tcc.interceptor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import io.seata.common.exception.FrameworkException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import io.seata.common.util.StringUtils;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.ParamType;
import io.seata.rm.tcc.api.ParameterFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;

/**
 * Extracting TCC Context from Method
 *
 * @author zhangsen
 * @author wang.liang
 */
public final class ActionContextUtil {

    private ActionContextUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionContextUtil.class);

    public static final DefaultParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    /**
     * Get parameter names of the method
     *
     * @param method the method
     * @return the parameter names
     */
    @Nullable
    public static String[] getParameterNames(Method method) {
        return PARAMETER_NAME_DISCOVERER.getParameterNames(method);
    }

    /**
     * Extracting context data from parameters
     *
     * @param targetParam the target param
     * @return map the context
     */
    public static Map<String, Object> fetchContextFromObject(@Nonnull Object targetParam) {
        try {
            // gets the fields from the class of the target parameter
            Field[] fields = ReflectionUtil.getAllFields(targetParam.getClass());
            if (CollectionUtils.isEmpty(fields)) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("The param of type `{}` has no field, please don't use `@{}(isParamInProperty = true)` on it",
                            targetParam.getClass().getName(), BusinessActionContextParameter.class.getSimpleName());
                }
                return Collections.emptyMap();
            }

            // fetch context from the fields
            Map<String, Object> context = new HashMap<>(8);
            for (Field f : fields) {
                // get annotation
                BusinessActionContextParameter annotation = f.getAnnotation(BusinessActionContextParameter.class);
                if (annotation == null) {
                    continue;
                }

                // get the field value
                f.setAccessible(true);
                Object fieldValue = f.get(targetParam);

                // load param by the config of annotation, and then put into the context
                String fieldName = f.getName();
                loadParamByAnnotationAndPutToContext(ParamType.FIELD, fieldName, fieldValue, annotation, context);
            }
            return context;
        } catch (Exception e) {
            throw new FrameworkException(e, "fetchContextFromObject failover");
        }
    }

    /**
     * load param by the config of annotation, and then put into the action context
     *
     * @param paramType     the param type, 'param' or 'field'
     * @param paramName     the param name
     * @param paramValue    the param value
     * @param annotation    the annotation on the param or field
     * @param actionContext the action context
     */
    public static void loadParamByAnnotationAndPutToContext(@Nonnull final ParamType paramType, @Nonnull String paramName, Object paramValue,
            @Nonnull final BusinessActionContextParameter annotation, @Nonnull final Map<String, Object> actionContext) {
        if (paramValue == null) {
            return;
        }

        // If {@code index >= 0}, get by index from the list/array param or field
        int index = annotation.index();
        if (index >= 0) {
            paramValue = getByIndex(paramType, paramName, paramValue, index);
            if (paramValue == null) {
                return;
            }
        }

        // if {@code isParamInProperty == true}, fetch context from paramValue
        if (annotation.isParamInProperty()) {
            // get the class of the parameter fetcher
            // @since above 1.4.2
            Class<? extends ParameterFetcher> fetcherClazz = annotation.fetcher();
            if (fetcherClazz.isInterface()) {
                throw new FrameworkException("the parameter fetcher must be not an interface, fetcher = " + fetcherClazz.getName());
            }

            // get the singleton of the parameter fetcher
            ParameterFetcher fetcher = ReflectionUtil.getSingleton(fetcherClazz);

            // fetch context from the param or field, and put into the action context
            fetcher.fetchContext(paramType, paramName, paramValue, annotation, actionContext);
        } else {
            putObjectByParamName(paramName, paramValue, annotation, actionContext);
        }
    }

    @Nullable
    private static Object getByIndex(@Nonnull ParamType paramType, @Nonnull String paramName, @Nonnull Object paramValue, int index) {
        if (paramValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>)paramValue;
            if (list.isEmpty()) {
                return null;
            }
            if (list.size() <= index) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("The index '{}' is out of bounds for the list {} named '{}'," +
                            " whose size is '{}', so pass this {}", index, paramType.getCode(), paramName, list.size(), paramType.getCode());
                }
                return null;
            }
            paramValue = list.get(index);
        } else if (paramValue.getClass().isArray()) {
            // The `index` field supports `Array`
            // @since above 1.4.2
            Object array = paramValue;
            int length = Array.getLength(array);
            if (length == 0) {
                return null;
            }
            if (length <= index) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("The index '{}' is out of bounds for the array {} named '{}'," +
                        " whose size is '{}', so pass this {}", index, paramType.getCode(), paramName, length, paramType.getCode());
                }
                return null;
            }
            paramValue = Array.get(array, index);
        } else {
            LOGGER.warn("the {} named '{}' is not a `List` or `Array`, so the 'index' field of '@{}' cannot be used on it",
                paramType.getCode(), paramName, BusinessActionContextParameter.class.getSimpleName());
        }

        return paramValue;
    }

    public static String getParamNameFromAnnotation(@Nonnull BusinessActionContextParameter annotation) {
        String paramName = annotation.paramName();
        if (StringUtils.isBlank(paramName)) {
            paramName = annotation.value();
        }
        return paramName;
    }

    public static void putObjectByParamName(String paramName, Object paramValue, @Nonnull BusinessActionContextParameter annotation,
            @Nonnull final Map<String, Object> actionContext) {
        if (paramValue == null) {
            return;
        }

        // get param name
        String paramNameFromAnnotation = getParamNameFromAnnotation(annotation);
        if (StringUtils.isNotBlank(paramNameFromAnnotation)) {
            paramName = paramNameFromAnnotation;
        }

        // put into the context
        actionContext.put(paramName, paramValue);
    }

    public static void putContextByParamName(Map<String, Object> paramContext, @Nonnull BusinessActionContextParameter annotation,
            @Nonnull final Map<String, Object> actionContext) {
        if (CollectionUtils.isEmpty(paramContext)) {
            return;
        }

        String paramNameFromAnnotation = ActionContextUtil.getParamNameFromAnnotation(annotation);
        if (StringUtils.isNotBlank(paramNameFromAnnotation)) {
            // If the `paramName` of "@BusinessActionContextParameter" is not blank, put the param context in it
            // @since: above 1.4.2
            actionContext.put(paramNameFromAnnotation, paramContext);
        } else {
            // Merge the param context into context
            // Warn: This may cause values with the same name to be overridden
            actionContext.putAll(paramContext);
        }
    }

    /**
     * put the action context after handle
     *
     * @param actionContext the action context
     * @param key           the actionContext's key
     * @param value         the actionContext's key
     * @return the action context is changed
     */
    public static boolean putActionContext(Map<String, Object> actionContext, String key, Object value) {
        if (value == null) {
            return false;
        }

        value = handleActionContext(value);
        Object previousValue = actionContext.put(key, value);
        return !value.equals(previousValue);
    }

    /**
     * put the action context after handle
     *
     * @param actionContext    the action context
     * @param actionContextMap the actionContextMap
     * @return the action context is changed
     */
    public static boolean putActionContext(Map<String, Object> actionContext, @Nonnull Map<String, Object> actionContextMap) {
        boolean isChanged = false;
        for (Map.Entry<String, Object> entry : actionContextMap.entrySet()) {
            if (putActionContext(actionContext, entry.getKey(), entry.getValue())) {
                isChanged = true;
            }
        }
        return isChanged;
    }

    /**
     * Handle the action context.
     * It is convenient to convert type in phase 2.
     *
     * @param actionContext the action context
     * @return the action context or JSON string
     * @see #convertActionContext(String, Object, Class)
     * @see BusinessActionContext#getActionContext(String, Class)
     */
    public static Object handleActionContext(@Nonnull Object actionContext) {
        if (actionContext instanceof CharSequence || actionContext instanceof Number || actionContext instanceof Boolean
                || actionContext instanceof Character) {
            return actionContext;
        } else {
            return JSON.toJSONString(actionContext);
        }
    }

    /**
     * Convert action context
     *
     * @param key         the actionContext's key
     * @param value       the actionContext's value
     * @param targetClazz the target class
     * @param <T>         the target type
     * @return the action context of the target type
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertActionContext(String key, @Nullable Object value, @Nonnull Class<T> targetClazz) {
        if (targetClazz.isPrimitive()) {
            throw new IllegalArgumentException("The targetClazz cannot be a primitive type, because the value may be null. Please use the wrapped type.");
        }

        if (value == null) {
            return null;
        }

        // Same class or super class, can cast directly
        if (targetClazz.isAssignableFrom(value.getClass())) {
            return (T)value;
        }

        // String class
        if (String.class.equals(targetClazz)) {
            return (T)value.toString();
        }

        // JSON to Object
        try {
            if (value instanceof CharSequence || value instanceof Character) {
                return JSON.parseObject(value.toString(), targetClazz);
            } else {
                return JSON.parseObject(JSON.toJSONString(value), targetClazz);
            }
        } catch (RuntimeException e) {
            String errorMsg = String.format("Failed to convert the action context with key '%s' from '%s' to '%s'.",
                    key, value.getClass().getName(), targetClazz.getName());
            throw new FrameworkException(e, errorMsg);
        }
    }
}
