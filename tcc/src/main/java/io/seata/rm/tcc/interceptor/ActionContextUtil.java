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

import java.lang.reflect.Field;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

                // load param by the config of annotation, and then put to the context
                String fieldName = f.getName();
                loadParamByAnnotationAndPutToContext("field", fieldName, fieldValue, annotation, context);
            }
            return context;
        } catch (Throwable t) {
            throw new FrameworkException(t, "fetchContextFromObject failover");
        }
    }

    /**
     * load param by the config of annotation, and then put to the context
     *
     * @param objType    the object type, 'param' or 'field'
     * @param objName    the object key
     * @param objValue   the object value
     * @param annotation the annotation on the param or field
     * @param context    the action context
     */
    public static void loadParamByAnnotationAndPutToContext(@Nonnull final String objType, @Nonnull String objName, Object objValue,
            @Nonnull final BusinessActionContextParameter annotation, @Nonnull final Map<String, Object> context) {
        if (objValue == null) {
            return;
        }

        // If {@code index >= 0}, get by index from the list param or field
        int index = annotation.index();
        if (index >= 0) {
            objValue = getByIndex(objType, objName, objValue, index);
            if (objValue == null) {
                return;
            }
        }

        // if {@code isParamInProperty == true}, fetch context from objValue
        if (annotation.isParamInProperty()) {
            Map<String, Object> paramContext = fetchContextFromObject(objValue);
            if (CollectionUtils.isNotEmpty(paramContext)) {
                context.putAll(paramContext);
            }
        } else {
            String paramName = getParamName(annotation);
            if (StringUtils.isNotBlank(paramName)) {
                objName = paramName;
            }
            context.put(objName, objValue);
        }
    }

    @Nullable
    private static Object getByIndex(String objType, String objName, Object objValue, int index) {
        if (objValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>)objValue;
            if (list.isEmpty()) {
                return null;
            }
            if (list.size() <= index) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("The index '{}' is out of bounds for the list {} named '{}'," +
                            " whose size is '{}', so pass this {}", index, objType, objName, list.size(), objType);
                }
                return null;
            }
            objValue = list.get(index);
        } else {
            LOGGER.warn("the {} named '{}' is not a `List`, so the 'index' field of '@{}' cannot be used on it",
                    objType, objName, BusinessActionContextParameter.class.getSimpleName());
        }

        return objValue;
    }

    public static String getParamName(@Nonnull BusinessActionContextParameter annotation) {
        String paramName = annotation.paramName();
        if (StringUtils.isBlank(paramName)) {
            paramName = annotation.value();
        }
        return paramName;
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
        if (actionContext instanceof CharSequence || actionContext instanceof Number || actionContext instanceof Boolean) {
            return actionContext;
        } else {
            return JSON.toJSONString(actionContext);
        }
    }

    /**
     * Handle the args
     *
     * @param args the args
     * @return new args
     */
    public static Object[] handleArgs(Object[] args) {
        Object[] newArgs = new Object[args.length];

        Object arg;
        for (int i = 0; i < args.length; ++i) {
            arg = args[i];
            if (arg == null) {
                continue;
            }
            newArgs[i] = handleActionContext(arg);
        }

        return newArgs;
    }

    /**
     * Convert action context
     *
     * @param value       the actionContext's value
     * @param targetClazz the target class
     * @param <T>         the target type
     * @return the action context of the target type
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertActionContext(@Nullable Object value, @Nonnull Class<T> targetClazz) {
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
        if (value instanceof CharSequence) {
            return JSON.parseObject(value.toString(), targetClazz);
        } else {
            return JSON.parseObject(JSON.toJSONString(value), targetClazz);
        }
    }
}
