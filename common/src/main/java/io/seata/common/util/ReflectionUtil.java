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
package io.seata.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection tools
 *
 * @author zhangsen
 */
public final class ReflectionUtil {

    /**
     * The EMPTY_FIELD_ARRAY
     */
    public static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

    /**
     * The constant MAX_NEST_DEPTH.
     */
    public static final int MAX_NEST_DEPTH = 20;
    /**
     * The cache CLASS_FIELDS_CACHE
     */
    private static final Map<Class<?>, Field[]> CLASS_FIELDS_CACHE = new ConcurrentHashMap<>();

    private ReflectionUtil() {
    }

    /**
     * Gets class by name.
     *
     * @param className the class name
     * @return the class by name
     * @throws ClassNotFoundException the class not found exception
     */
    public static Class<?> getClassByName(String className) throws ClassNotFoundException {
        return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
    }

    /**
     * get Field Value
     *
     * @param target    the target
     * @param fieldName the field name
     * @return field value
     * @throws NoSuchFieldException the no such field exception
     * @throws SecurityException the security exception
     * @throws IllegalArgumentException the illegal argument exception
     */
    public static Object getFieldValue(Object target, String fieldName)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException {
        Class<?> cl = target.getClass();
        int i = 0;
        while ((i++) < MAX_NEST_DEPTH && cl != null) {
            try {
                Field field = cl.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (Exception e) {
                cl = cl.getSuperclass();
            }
        }
        throw new NoSuchFieldException("class:" + target.getClass() + ", field:" + fieldName);
    }

    /**
     * invoke Method
     *
     * @param target     the target
     * @param methodName the method name
     * @return object
     * @throws NoSuchMethodException the no such method exception
     * @throws SecurityException the security exception
     * @throws IllegalArgumentException the illegal argument exception
     */
    public static Object invokeMethod(Object target, String methodName)
            throws NoSuchMethodException, SecurityException, IllegalArgumentException {
        Class<?> cl = target.getClass();
        int i = 0;
        while ((i++) < MAX_NEST_DEPTH && cl != null) {
            try {
                Method m = cl.getDeclaredMethod(methodName);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (Exception e) {
                cl = cl.getSuperclass();
            }
        }
        throw new NoSuchMethodException("class:" + target.getClass() + ", methodName:" + methodName);
    }

    /**
     * invoke Method
     *
     * @param target         the target
     * @param methodName     the method name
     * @param parameterTypes the parameter types
     * @param args           the args
     * @return object
     * @throws NoSuchMethodException the no such method exception
     * @throws SecurityException the security exception
     * @throws IllegalArgumentException the illegal argument exception
     */
    public static Object invokeMethod(Object target, String methodName, Class<?>[] parameterTypes, Object[] args)
            throws NoSuchMethodException, SecurityException, IllegalArgumentException {
        Class<?> cl = target.getClass();
        int i = 0;
        while ((i++) < MAX_NEST_DEPTH && cl != null) {
            try {
                Method m = cl.getDeclaredMethod(methodName, parameterTypes);
                m.setAccessible(true);
                return m.invoke(target, args);
            } catch (Exception e) {
                cl = cl.getSuperclass();
            }
        }
        throw new NoSuchMethodException("class:" + target.getClass() + ", methodName:" + methodName);
    }

    /**
     * invoke static Method
     *
     * @param targetClass     the target class
     * @param methodName      the method name
     * @param parameterTypes  the parameter types
     * @param parameterValues the parameter values
     * @return object
     * @throws NoSuchMethodException the no such method exception
     * @throws SecurityException the security exception
     * @throws IllegalArgumentException the illegal argument exception
     */
    public static Object invokeStaticMethod(Class<?> targetClass, String methodName, Class<?>[] parameterTypes,
                                            Object[] parameterValues)
            throws NoSuchMethodException, SecurityException, IllegalArgumentException {
        int i = 0;
        while ((i++) < MAX_NEST_DEPTH && targetClass != null) {
            try {
                Method m = targetClass.getMethod(methodName, parameterTypes);
                return m.invoke(null, parameterValues);
            } catch (Exception e) {
                targetClass = targetClass.getSuperclass();
            }
        }
        throw new NoSuchMethodException("class:" + targetClass + ", methodName:" + methodName);
    }

    /**
     * get all interface of the clazz
     *
     * @param clazz the clazz
     * @return set
     */
    public static Set<Class<?>> getInterfaces(Class<?> clazz) {
        if (clazz.isInterface()) {
            return Collections.singleton(clazz);
        }
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        while (clazz != null) {
            Class<?>[] ifcs = clazz.getInterfaces();
            for (Class<?> ifc : ifcs) {
                interfaces.addAll(getInterfaces(ifc));
            }
            clazz = clazz.getSuperclass();
        }
        return interfaces;
    }

    public static void modifyStaticFinalField(Class<?> cla, String modifyFieldName, Object newValue)
        throws NoSuchFieldException, IllegalAccessException {
        Field field = cla.getDeclaredField(modifyFieldName);
        field.setAccessible(true);
        Field modifiers = field.getClass().getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(cla, newValue);
    }

    /**
     * Gets all fields, excluding static or synthetic fields
     *
     * @param targetClazz the target class
     */
    public static Field[] getAllFields(final Class<?> targetClazz) {
        if (targetClazz == Object.class || targetClazz.isInterface()) {
            return EMPTY_FIELD_ARRAY;
        }

        // get from the cache
        Field[] fields = CLASS_FIELDS_CACHE.get(targetClazz);
        if (fields != null) {
            return fields;
        }

        // load current class declared fields
        fields = targetClazz.getDeclaredFields();
        LinkedList<Field> fieldList = new LinkedList<>(Arrays.asList(fields));

        // remove the static or synthetic fields
        fieldList.removeIf(f -> Modifier.isStatic(f.getModifiers()) || f.isSynthetic());

        // load super class all fields, and add to the field list
        Field[] superFields = getAllFields(targetClazz.getSuperclass());
        if (CollectionUtils.isNotEmpty(superFields)) {
            fieldList.addAll(Arrays.asList(superFields));
        }

        // list to array
        Field[] resultFields;
        if (!fieldList.isEmpty()) {
            resultFields = fieldList.toArray(new Field[0]);
        } else {
            // reuse the EMPTY_FIELD_ARRAY
            resultFields = EMPTY_FIELD_ARRAY;
        }

        // set cache
        CLASS_FIELDS_CACHE.put(targetClazz, resultFields);

        return resultFields;
    }
}
