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
package io.seata.spring.proxy.desc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.common.util.ReflectionUtil;
import io.seata.spring.proxy.SeataProxy;
import io.seata.spring.proxy.SeataProxyBeanRegister;

/**
 * The SeataProxy Parser
 *
 * @author wang.liang
 * @see SeataProxyBeanDesc
 * @see SeataProxyMethodDesc
 * @see SeataProxyBeanRegister
 */
public final class SeataProxyParser {

    private SeataProxyParser() {
    }


    /**
     * Phase bean desc of the target bean
     *
     * @param totalRegister the total register
     * @param bean          the bean
     * @param beanName      the bean name
     * @return the proxy bean desc
     */
    public static SeataProxyBeanDesc parserBeanDesc(SeataProxyBeanRegister totalRegister, @Nonnull Object bean, String beanName) {
        // get by bean name
        SeataProxyBeanDesc registerBeanDesc = totalRegister.getBeanNameBeanDescMap().get(beanName);
        if (registerBeanDesc == null) {
            // get by bean class
            registerBeanDesc = totalRegister.getBeanClassBeanDescMap().get(bean.getClass());
        }

        if (registerBeanDesc == null) {
            // create new
            registerBeanDesc = new SeataProxyBeanDesc(beanName, bean.getClass());
        }

        // reset bean name and bean
        registerBeanDesc.setTargetBeanName(beanName);
        registerBeanDesc.setTargetBean(bean);

        // return
        return registerBeanDesc;
    }

    /**
     * Parser impl desc of the target bean class
     *
     * @param targetBeanClass the target bean class
     * @return the impl desc
     */
    @Nullable
    public static SeataProxyImplementationDesc parserImplDesc(Class<?> targetBeanClass) {
        SeataProxy annotation = targetBeanClass.getAnnotation(SeataProxy.class);
        if (annotation == null) {
            return null;
        }
        return new SeataProxyImplementationDesc(annotation);
    }

    /**
     * Parser method desc map of the target bean class
     *
     * @param targetBeanClass the target bean class
     * @param methodFilter    the method filter
     * @return the method desc map
     */
    public static Map<Method, SeataProxyMethodDesc> parserMethodDescMap(Class<?> targetBeanClass, Predicate<Method> methodFilter) {
        Map<Method, SeataProxyMethodDesc> methodDescMap = new HashMap<>();

        boolean onlyScanAnnotatedMethods = false;
        SeataProxy seataProxyAnnoOnClass = targetBeanClass.getAnnotation(SeataProxy.class);
        if (seataProxyAnnoOnClass != null) {
            onlyScanAnnotatedMethods = seataProxyAnnoOnClass.onlyScanAnnotatedMethods();
        }

        SeataProxyMethodDesc methodDesc;
        Method[] methods = targetBeanClass.getMethods();
        for (Method method : methods) {
            // ignore the static methods
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            // ignore the synthetic methods
            if (method.isSynthetic()) {
                continue;
            }

            // ignore the methods of the Object class
            if (ReflectionUtil.hasMethod(Object.class, method)) {
                continue;
            }

            // ignore the method override in the child class
            if (ReflectionUtil.containsMethod(methodDescMap, method)) {
                continue;
            }

            // ignore the method is not matched
            if (methodFilter != null && !methodFilter.test(method)) {
                continue;
            }

            // create the methodDesc and put to the map
            methodDesc = new SeataProxyMethodDesc(method);
            if (onlyScanAnnotatedMethods && methodDesc.getImplDesc() == null) {
                continue;
            }

            methodDescMap.put(method, methodDesc);
        }

        return methodDescMap;
    }
}
