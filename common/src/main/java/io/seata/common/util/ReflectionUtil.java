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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection tools
 *
 * @author zhangsen
 * @author wang.liang
 */
public final class ReflectionUtil {

    private ReflectionUtil() {
    }


    //region Constants

    /**
     * The constant EMPTY_FIELD_ARRAY
     */
    public static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

    /**
     * The constant EMPTY_CLASS_ARRAY
     */
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    /**
     * The constant EMPTY_ARGS
     */
    public static final Object[] EMPTY_ARGS = new Object[0];

    /**
     * The cache CLASS_FIELDS_CACHE
     */
    private static final Map<Class<?>, Field[]> CLASS_FIELDS_CACHE = new ConcurrentHashMap<>();

    /**
     * The cache SINGLETON_CACHE
     */
    private static final Map<Class<?>, Object> SINGLETON_CACHE = new ConcurrentHashMap<>();

    //endregion


    //region Class

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
     * class name set to class set
     *
     * @param classNames the class names
     * @return the class set
     */
    public static Set<Class<?>> classNamesToClassSet(Collection<String> classNames) {
        Set<Class<?>> classSet = new HashSet<>();
        if (classNames != null) {
            Class<?> clazz;
            for (String className : classNames) {
                try {
                    clazz = getClassByName(className);
                    classSet.add(clazz);
                } catch (ClassNotFoundException ignore) {
                    // do nothing
                }
            }
        }
        return classSet;
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

    //endregion


    //region Field

    /**
     * Gets all fields.
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
        final LinkedList<Field> fieldList = new LinkedList<>(Arrays.asList(fields));

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

    /**
     * get Field
     *
     * @param clazz     the class
     * @param fieldName the field name
     * @return the field
     * @throws NoSuchFieldException if the field named {@code fieldName} does not exist
     * @throws SecurityException    the security exception
     */
    public static Field getField(final Class<?> clazz, final String fieldName) throws NoSuchFieldException, SecurityException {
        Class<?> cl = clazz;
        while (cl != null && cl != Object.class && !cl.isInterface()) {
            try {
                return cl.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                cl = cl.getSuperclass();
            }
        }

        throw new NoSuchFieldException("field not found: " + clazz.getName() + ", field: " + fieldName);
    }

    /**
     * get field value
     *
     * @param target the target
     * @param field  the field of the target
     * @return field value
     * @throws IllegalArgumentException if {@code target} is {@code null}
     * @throws SecurityException        the security exception
     */
    public static Object getFieldValue(Object target, Field field)
            throws IllegalArgumentException, SecurityException {
        if (target == null) {
            throw new IllegalArgumentException("target must be not null");
        }

        while (true) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                return field.get(target);
            } catch (IllegalAccessException ignore) {
                // avoid other threads executing `field.setAccessible(false)`
            }
        }
    }

    /**
     * get field value
     *
     * @param target    the target
     * @param fieldName the field name
     * @return field value
     * @throws IllegalArgumentException if {@code target} is {@code null}
     * @throws NoSuchFieldException     if the field named {@code fieldName} does not exist
     * @throws SecurityException        the security exception
     */
    public static Object getFieldValue(Object target, String fieldName)
            throws IllegalArgumentException, NoSuchFieldException, SecurityException {
        if (target == null) {
            throw new IllegalArgumentException("target must be not null");
        }

        // get field
        Field field = getField(target.getClass(), fieldName);

        // get field value
        return getFieldValue(target, field);
    }

    /**
     * set field value
     *
     * @param target     the target
     * @param field      the field of the target
     * @param fieldValue the field value
     * @throws IllegalArgumentException if {@code target} is {@code null}
     * @throws SecurityException        the security exception
     */
    public static void setFieldValue(Object target, Field field, Object fieldValue)
            throws IllegalArgumentException, SecurityException {
        if (target == null) {
            throw new IllegalArgumentException("target must be not null");
        }

        while (true) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                field.set(target, fieldValue);
                return;
            } catch (IllegalAccessException ignore) {
                // avoid other threads executing `field.setAccessible(false)`
            }
        }
    }

    /**
     * get field value
     *
     * @param target     the target
     * @param fieldName  the field name
     * @param fieldValue the field value
     * @throws IllegalArgumentException if {@code target} is {@code null}
     * @throws NoSuchFieldException     if the field named {@code fieldName} does not exist
     * @throws SecurityException        the security exception
     */
    public static void setFieldValue(Object target, String fieldName, Object fieldValue)
            throws IllegalArgumentException, NoSuchFieldException, SecurityException {
        if (target == null) {
            throw new IllegalArgumentException("target must be not null");
        }

        // get field
        Field field = getField(target.getClass(), fieldName);

        // set new value
        setFieldValue(target, field, fieldValue);
    }

    /**
     * modify `static` or `static final` field value
     *
     * @param staticField the static field
     * @param newValue    the new value
     * @throws IllegalArgumentException if {@code staticField} is {@code null} or not a static field
     * @throws NoSuchFieldException     if the class of the staticField has no `modifiers` field
     * @throws IllegalAccessException   the illegal access exception
     */
    public static void modifyStaticFinalField(Field staticField, Object newValue)
            throws NoSuchFieldException, IllegalAccessException {
        if (staticField == null) {
            throw new IllegalArgumentException("staticField must be not null");
        }

        // check is static field
        if (!Modifier.isStatic(staticField.getModifiers())) {
            throw new IllegalArgumentException("the `" + fieldToString(staticField) + "` is not a static field, cannot modify value.");
        }

        // remove the `final` keyword from the field
        if (Modifier.isFinal(staticField.getModifiers())) {
            Field modifiers = staticField.getClass().getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(staticField, staticField.getModifiers() & ~Modifier.FINAL);
        }

        // set new value
        staticField.setAccessible(true);
        staticField.set(staticField.getDeclaringClass(), newValue);
    }

    /**
     * modify `static` or `static final` field value
     *
     * @param targetClass     the target class
     * @param staticFieldName the static field name
     * @param newValue        the new value
     * @throws IllegalArgumentException if {@code targetClass} is {@code null}
     * @throws NullPointerException     if {@code staticFieldName} is {@code null}
     * @throws NoSuchFieldException     if the field named {@code modifyFieldName} does not exist
     * @throws IllegalAccessException   the illegal access exception
     */
    public static void modifyStaticFinalField(Class<?> targetClass, String staticFieldName, Object newValue)
            throws NoSuchFieldException, IllegalAccessException {
        if (targetClass == null) {
            throw new IllegalArgumentException("targetClass must be not null");
        }

        // get field
        Field field = targetClass.getDeclaredField(staticFieldName);

        // modify static final field value
        modifyStaticFinalField(field, newValue);
    }

    //endregion


    //region Method

    /**
     * get method
     *
     * @param clazz          the class
     * @param methodName     the method name
     * @param parameterTypes the parameter types
     * @return the method
     * @throws IllegalArgumentException if {@code clazz} is {@code null}
     * @throws NullPointerException     if {@code methodName} is {@code null}
     * @throws NoSuchMethodException    if the method named {@code methodName} does not exist
     * @throws SecurityException        the security exception
     */
    public static Method getMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes)
            throws NoSuchMethodException, SecurityException {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must be not null");
        }

        Class<?> cl = clazz;
        while (cl != null) {
            try {
                return cl.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                cl = cl.getSuperclass();
            }
        }

        throw new NoSuchMethodException("method not found: " + methodToString(clazz, methodName, parameterTypes));
    }

    /**
     * get method
     *
     * @param clazz      the class
     * @param methodName the method name
     * @return the method
     * @throws IllegalArgumentException if {@code clazz} is {@code null}
     * @throws NullPointerException     if {@code methodName} is {@code null}
     * @throws NoSuchMethodException    if the method named {@code methodName} does not exist
     * @throws SecurityException        the security exception
     */
    public static Method getMethod(final Class<?> clazz, final String methodName)
            throws NoSuchMethodException, SecurityException {
        return getMethod(clazz, methodName, EMPTY_CLASS_ARRAY);
    }

    /**
     * invoke Method
     *
     * @param target the target
     * @param method the method
     * @param args   the args
     * @return the result of the underlying method
     * @throws InvocationTargetException if the underlying method throws an exception.
     * @throws IllegalArgumentException  the illegal argument exception
     * @throws SecurityException         the security exception
     */
    public static Object invokeMethod(Object target, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, SecurityException {
        while (true) {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            try {
                return method.invoke(target, args);
            } catch (IllegalAccessException ignore) {
                // avoid other threads executing `method.setAccessible(false)`
            }
        }
    }

    /**
     * invoke Method
     *
     * @param target the target
     * @param method the method
     * @return the result of the underlying method
     * @throws InvocationTargetException if the underlying method throws an exception.
     * @throws IllegalArgumentException  the illegal argument exception
     * @throws SecurityException         the security exception
     */
    public static Object invokeMethod(Object target, Method method)
            throws InvocationTargetException, IllegalArgumentException, SecurityException {
        return invokeMethod(target, method, EMPTY_ARGS);
    }

    /**
     * invoke Method
     *
     * @param target         the target
     * @param methodName     the method name
     * @param parameterTypes the parameter types
     * @param args           the args
     * @return the result of the underlying method
     * @throws IllegalArgumentException  if {@code target} is {@code null}
     * @throws NullPointerException      if {@code methodName} is {@code null}
     * @throws NoSuchMethodException     if the method named {@code methodName} does not exist
     * @throws InvocationTargetException if the underlying method throws an exception.
     * @throws SecurityException         the security exception
     */
    public static Object invokeMethod(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException, SecurityException {
        if (target == null) {
            throw new IllegalArgumentException("target must be not null");
        }

        // get method
        Method method = getMethod(target.getClass(), methodName, parameterTypes);

        // invoke method
        return invokeMethod(target, method, args);
    }

    /**
     * invoke Method
     *
     * @param target     the target
     * @param methodName the method name
     * @return the result of the underlying method
     * @throws NoSuchMethodException     the no such method exception
     * @throws InvocationTargetException if the underlying method throws an exception.
     * @throws IllegalArgumentException  the illegal argument exception
     * @throws SecurityException         the security exception
     */
    public static Object invokeMethod(Object target, String methodName)
            throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException, SecurityException {
        return invokeMethod(target, methodName, EMPTY_CLASS_ARRAY, EMPTY_ARGS);
    }

    /**
     * invoke static Method
     *
     * @param staticMethod the static method
     * @param args         the args
     * @return the result of the static method
     * @throws IllegalArgumentException  if {@code staticMethod} is {@code null} or not a static method
     * @throws InvocationTargetException if the underlying method throws an exception.
     * @throws SecurityException         the security exception
     */
    public static Object invokeStaticMethod(Method staticMethod, Object... args)
            throws IllegalArgumentException, InvocationTargetException, SecurityException {
        if (staticMethod == null) {
            throw new IllegalArgumentException("staticMethod must be not null");
        }

        if (!Modifier.isStatic(staticMethod.getModifiers())) {
            throw new IllegalArgumentException("`" + methodToString(staticMethod) + "` is not a static method");
        }

        return invokeMethod(null, staticMethod, args);
    }

    /**
     * invoke static Method
     *
     * @param staticMethod the static method
     * @return the result of the static method
     * @throws InvocationTargetException if the underlying method throws an exception.
     * @throws IllegalArgumentException  the illegal argument exception
     * @throws SecurityException         the security exception
     */
    public static Object invokeStaticMethod(Method staticMethod)
            throws InvocationTargetException, IllegalArgumentException, SecurityException {
        return invokeStaticMethod(staticMethod, EMPTY_ARGS);
    }

    /**
     * invoke static Method
     *
     * @param targetClass      the target class
     * @param staticMethodName the static method name
     * @param parameterTypes   the parameter types
     * @param args             the args
     * @return the result of the static method
     * @throws IllegalArgumentException  if {@code targetClass} is {@code null}
     * @throws NullPointerException      if {@code methodName} is {@code null}
     * @throws NoSuchMethodException     the no such method exception
     * @throws InvocationTargetException if the underlying method throws an exception.
     * @throws SecurityException         the security exception
     */
    public static Object invokeStaticMethod(Class<?> targetClass, String staticMethodName,
                                            Class<?>[] parameterTypes, Object... args)
            throws IllegalArgumentException, NoSuchMethodException, InvocationTargetException, SecurityException {
        if (targetClass == null) {
            throw new IllegalArgumentException("targetClass must be not null");
        }

        // get method
        Method staticMethod = getMethod(targetClass, staticMethodName, parameterTypes);
        if (!Modifier.isStatic(staticMethod.getModifiers())) {
            throw new NoSuchMethodException("static method not found: "
                    + methodToString(targetClass, staticMethodName, parameterTypes));
        }

        return invokeStaticMethod(staticMethod, args);
    }

    /**
     * invoke static Method
     *
     * @param targetClass      the target class
     * @param staticMethodName the static method name
     * @return the result of the static method
     * @throws IllegalArgumentException  if {@code targetClass} is {@code null}
     * @throws NullPointerException      if {@code methodName} is {@code null}
     * @throws NoSuchMethodException     the no such method exception
     * @throws InvocationTargetException if the underlying method throws an exception.
     * @throws SecurityException         the security exception
     */
    public static Object invokeStaticMethod(Class<?> targetClass, String staticMethodName)
            throws IllegalArgumentException, NoSuchMethodException, SecurityException, InvocationTargetException {
        return invokeStaticMethod(targetClass, staticMethodName, EMPTY_CLASS_ARRAY, EMPTY_ARGS);
    }

    /**
     * Equals method
     *
     * @param m1 the method 1
     * @param m2 the method 2
     * @return the boolean
     */
    public static boolean equalsMethod(Method m1, Method m2) {
        if (m1.equals(m2)) {
            return true;
        }

        if (!m1.getName().equals(m2.getName()) || m1.getParameterCount() != m2.getParameterCount()) {
            return false;
        }

        for (int i = 0; i < m1.getParameterCount(); ++i) {
            if (!m1.getParameters()[i].equals(m2.getParameterTypes()[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Contains method
     *
     * @param methods the methods
     * @param method  the method
     * @return the boolean
     */
    public static boolean containsMethod(Collection<Method> methods, Method method) {
        if (methods.contains(method)) {
            return true;
        }

        for (Method m : methods) {
            if (equalsMethod(m, method)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Contains method
     *
     * @param methodMap the method map
     * @param method    the method
     * @return the boolean
     */
    public static boolean containsMethod(Map<Method, ?> methodMap, Method method) {
        if (methodMap.isEmpty()) {
            return false;
        }

        return containsMethod(methodMap.keySet(), method);
    }

    /**
     * Has method
     *
     * @param clazz  the class
     * @param method the method
     * @return the boolean
     */
    public static boolean hasMethod(Class<?> clazz, Method method) {
        if (method.getDeclaringClass() == clazz) {
            return true;
        }

        try {
            clazz.getMethod(method.getName(), method.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    //endregion


    //region Annotation

    /**
     * get annotation from the method or super class method
     *
     * @param method          the method
     * @param annotationClass the annotation class
     * @param <T>             the annotation type
     * @return the annotation
     */
    public static <T extends Annotation> T getAnnotation(final Method method, final Class<T> annotationClass) {
        T annotation = method.getAnnotation(annotationClass);
        if (annotation == null) {
            Class<?> superClass = method.getDeclaringClass().getSuperclass();
            Method m;
            while (superClass != null && superClass != Object.class) {
                try {
                    m = superClass.getMethod(method.getName(), method.getParameterTypes());
                    annotation = m.getAnnotation(annotationClass);
                    if (annotation != null) {
                        return annotation;
                    }
                } catch (NoSuchMethodException ignore) {
                    // do nothing
                }
                superClass = superClass.getSuperclass();
            }
        }
        return annotation;
    }

    //endregion


    //region Instance


    /**
     * get singleton for the class
     *
     * @param clazz the clazz
     * @param <T>   the type
     * @return the singleton
     * @throws IllegalArgumentException
     */
    public static <T> T getSingleton(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must be not null");
        }

        if (clazz.isInterface()) {
            throw new IllegalArgumentException("clazz must be not an interface: " + clazz);
        }

        return (T)CollectionUtils.computeIfAbsent(SINGLETON_CACHE, clazz, key -> {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException("new instance failed, class: " + clazz, e);
            }
        });
    }

    //endregion


    //region toString

    /**
     * method to string
     *
     * @param clazz          the clazz
     * @param methodName     the method name
     * @param parameterTypes the parameter types
     * @return the string
     */
    public static String methodToString(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return clazz.getName() + "." + methodName + parameterTypesToString(parameterTypes);
    }

    /**
     * method to string
     *
     * @param method the method
     * @return the string
     */
    public static String methodToString(Method method) {
        String methodStr = methodToString(method.getDeclaringClass(), method.getName(), method.getParameterTypes());
        if (Modifier.isStatic(method.getModifiers())) {
            methodStr = "static " + methodStr;
        }
        return methodStr;
    }

    /**
     * field to string
     *
     * @param clazz     the clazz
     * @param fieldName the field name
     * @param fieldType the field type
     * @return the string
     */
    public static String fieldToString(Class<?> clazz, String fieldName, Class<?> fieldType) {
        return fieldType.getName() + " field " + clazz.getName() + "." + fieldName;
    }

    /**
     * field to string
     *
     * @param field the field
     * @return the string
     */
    public static String fieldToString(Field field) {
        return fieldToString(field.getDeclaringClass(), field.getName(), field.getType());
    }

    /**
     * parameter types to string
     *
     * @param parameterTypes the parameter types
     * @return the string
     */
    public static String parameterTypesToString(Class<?>[] parameterTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Class<?> c = parameterTypes[i];
                sb.append((c == null) ? "null" : c.getName());
            }
        }
        sb.append(")");
        return sb.toString();
    }

    //endregion
}
