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
package io.seata.rm.saga.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.seata.common.exception.FrameworkException;
import io.seata.common.util.StringUtils;
import io.seata.rm.saga.api.BusinessActionContextParameter;

/**
 * Extracting TCC Context from Method
 *
 * @author zhangsen
 */
public class ActionContextUtil {

    /**
     * Extracting context data from parameters
     *
     * @param targetParam the target param
     * @return map map
     */
    public static Map<String, Object> fetchContextFromObject(Object targetParam) {
        try {
            Map<String, Object> context = new HashMap<String, Object>(8);
            List<Field> fields = new ArrayList<Field>();
            getAllField(targetParam.getClass(), fields);
            for (Field f : fields) {
                String fieldName = f.getName();
                Annotation annotation = f.getAnnotation(BusinessActionContextParameter.class);

                if (annotation != null) {
                    BusinessActionContextParameter param = (BusinessActionContextParameter)annotation;
                    f.setAccessible(true);
                    Object paramObject = f.get(targetParam);
                    int index = param.index();
                    if (index >= 0) {
                        @SuppressWarnings("unchecked")
                        Object targetObject = ((List<Object>)paramObject).get(index);
                        if (param.isParamInProperty()) {
                            context.putAll(fetchContextFromObject(targetObject));
                        } else {
                            if (StringUtils.isBlank(param.paramName())) {
                                context.put(fieldName, paramObject);
                            } else {
                                context.put(param.paramName(), paramObject);
                            }
                        }
                    } else {
                        if (param.isParamInProperty()) {
                            context.putAll(fetchContextFromObject(paramObject));
                        } else {
                            if (StringUtils.isBlank(param.paramName())) {
                                context.put(fieldName, paramObject);
                            } else {
                                context.put(param.paramName(), paramObject);
                            }
                        }
                    }
                }
            }
            return context;
        } catch (Throwable t) {
            throw new FrameworkException(t, "fetchContextFromObject failover");
        }
    }

    /**
     * Gets all field.
     *
     * @param interFace the inter face
     * @param fields    the fields
     */
    public static void getAllField(Class<?> interFace, List<Field> fields) {
        if (interFace == Object.class || interFace.isInterface()) {
            return;
        }
        Field[] field = interFace.getDeclaredFields();
        if (field != null) {
            fields.addAll(Arrays.asList(field));
        }
        getAllField(interFace.getSuperclass(), fields);
    }

}
