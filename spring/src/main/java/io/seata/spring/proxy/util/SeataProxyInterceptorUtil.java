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
package io.seata.spring.proxy.util;

import java.lang.reflect.Method;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.holder.ObjectHolder;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import io.seata.common.util.StringUtils;
import io.seata.spring.proxy.SeataProxy;
import io.seata.spring.proxy.SeataProxyConstants;
import io.seata.spring.proxy.SeataProxyHandler;
import io.seata.spring.proxy.SeataProxyInterceptor;
import io.seata.spring.proxy.SeataProxyResultHandler;
import io.seata.spring.proxy.SeataProxyValidator;
import io.seata.spring.proxy.desc.SeataProxyMethodDesc;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;

/**
 * The Seata Proxy Interceptor Util
 *
 * @author wang.liang
 * @see SeataProxyInterceptor
 */
public final class SeataProxyInterceptorUtil {

    private SeataProxyInterceptorUtil() {
    }


    //region the switch of the SeataProxy function for the current Thread

    private static final ThreadLocal<Boolean> NEED_PROXY = new ThreadLocal<>();

    public static void enableProxy() {
        NEED_PROXY.remove();
    }

    public static void disableProxy() {
        NEED_PROXY.set(false);
    }

    public static boolean isNeedProxy() {
        return !Boolean.FALSE.equals(NEED_PROXY.get());
    }

    //endregion


    //region get the default implementation

    private static SeataProxyValidator defaultProxyValidator;
    private static SeataProxyHandler defaultProxyHandler;
    private static SeataProxyResultHandler defaultProxyResultHandler;

    @Nullable
    public static SeataProxyValidator getDefaultValidator() {
        if (defaultProxyValidator == null) {
            synchronized (SeataProxyInterceptor.class) {
                if (defaultProxyValidator == null) {
                    defaultProxyValidator = tryGetBean(SeataProxyConstants.DEFAULT_VALIDATOR_BEAN_NAME,
                            SeataProxyConstants.DEFAULT_VALIDATOR_CLASS, true);
                }
            }
        }

        return defaultProxyValidator;
    }

    @Nonnull
    public static SeataProxyHandler getDefaultHandler() {
        if (defaultProxyHandler == null) {
            synchronized (SeataProxyInterceptor.class) {
                if (defaultProxyHandler == null) {
                    defaultProxyHandler = tryGetBean(SeataProxyConstants.DEFAULT_HANDLER_BEAN_NAME,
                            SeataProxyConstants.DEFAULT_HANDLER_CLASS, true);
                }
            }
        }

        return defaultProxyHandler;
    }

    @Nonnull
    public static SeataProxyResultHandler getDefaultResultHandler() {
        if (defaultProxyResultHandler == null) {
            synchronized (SeataProxyInterceptor.class) {
                if (defaultProxyResultHandler == null) {
                    defaultProxyResultHandler = tryGetBean(SeataProxyConstants.DEFAULT_RESULT_HANDLER_BEAN_NAME,
                            SeataProxyConstants.DEFAULT_RESULT_HANDLER_CLASS, true);
                }
            }
        }

        return defaultProxyResultHandler;
    }

    //endregion


    //region get bean

    /**
     * get bean
     *
     * @param beanName the bean name
     * @param <T>      the bean type
     * @return the bean
     */
    public static <T> T getBean(String beanName) {
        ApplicationContext applicationContext = (ApplicationContext)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT);
        if (applicationContext == null) {
            throw new ShouldNeverHappenException("the application context should never set to the ObjectHolder");
        }

        return (T)applicationContext.getBean(beanName);
    }

    /**
     * Try get the bean
     *
     * @param beanName  the bean name
     * @param beanClass the bean class
     * @param <T>       the bean type
     * @return the bean
     */
    public static <T> T tryGetBean(String beanName, Class<T> beanClass, boolean autoCreateIfTheBeanNotExists) {
        ApplicationContext applicationContext = (ApplicationContext)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT);
        if (applicationContext == null) {
            throw new ShouldNeverHappenException("the application context should never set to the ObjectHolder");
        }

        T bean = null;

        if (StringUtils.isNotBlank(beanName)) {
            try {
                // get by bean name
                bean = (T)applicationContext.getBean(beanName);
            } catch (NoSuchBeanDefinitionException ignore) {
                // do nothing
            }
        }

        if (bean == null && beanClass != null) {
            try {
                // get by bean class
                bean = applicationContext.getBean(beanClass);
            } catch (NoSuchBeanDefinitionException ignore) {
                if (autoCreateIfTheBeanNotExists) {
                    bean = ReflectionUtil.getSingleton(beanClass);
                }
            }
        }

        return bean;
    }

    //endregion


    /**
     * get method desc
     *
     * @param methodDescMap the method desc map
     * @param method        the method
     * @return the method desc
     */
    @Nullable
    public static SeataProxyMethodDesc getMethodDesc(Map<Method, SeataProxyMethodDesc> methodDescMap, Method method) {
        if (CollectionUtils.isEmpty(methodDescMap)) {
            return null;
        }

        SeataProxyMethodDesc methodDesc = methodDescMap.get(method);
        if (methodDesc != null) {
            return methodDesc;
        }

        for (Map.Entry<Method, SeataProxyMethodDesc> entry : methodDescMap.entrySet()) {
            if (ReflectionUtil.equalsMethod(entry.getKey(), method)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public static boolean isShouldSkip(@Nonnull SeataProxy seataProxyAnno) {
        if (seataProxyAnno.skip()) {
            return true;
        }

        return seataProxyAnno.value();
    }

}
