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
package io.seata.spring.proxy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import io.seata.spring.proxy.desc.SeataProxyBeanDesc;
import org.springframework.context.annotation.Lazy;

/**
 * Seata Proxy Bean Register
 *
 * @author wang.liang
 * @see io.seata.spring.annotation.GlobalTransactionScanner
 */
@Lazy(false)
public class SeataProxyBeanRegister {

    private Map<Class<?>, SeataProxyBeanDesc> beanClassBeanDescMap = new HashMap<>();
    private Map<String, SeataProxyBeanDesc> beanNameBeanDescMap = new HashMap<>();

    public Map<Class<?>, SeataProxyBeanDesc> getBeanClassBeanDescMap() {
        return beanClassBeanDescMap;
    }

    public Map<String, SeataProxyBeanDesc> getBeanNameBeanDescMap() {
        return beanNameBeanDescMap;
    }

    /**
     * merge the other register
     *
     * @param otherRegister the other register
     */
    public void merge(SeataProxyBeanRegister otherRegister) {
        if (otherRegister != null) {
            beanClassBeanDescMap.putAll(otherRegister.getBeanClassBeanDescMap());
            beanNameBeanDescMap.putAll(otherRegister.getBeanNameBeanDescMap());
        }
    }


    //region the methods for register proxy bean

    public void registerProxyBean(Class<?> targetBeanClass, SeataProxyBeanDesc proxyBeanDesc) {
        if (proxyBeanDesc == null) {
            throw new IllegalArgumentException("the proxyBeanDesc must be not null");
        }
        beanClassBeanDescMap.put(targetBeanClass, proxyBeanDesc);
    }

    public void registerProxyBean(String targetBeanName, SeataProxyBeanDesc proxyBeanDesc) {
        if (proxyBeanDesc == null) {
            throw new IllegalArgumentException("the proxyBeanDesc must be not null");
        }
        beanNameBeanDescMap.put(targetBeanName, proxyBeanDesc);
    }

    public void registerProxyBean(String targetBeanName, Class<?> targetBeanClass, Predicate<Method> methodMatcher) {
        SeataProxyBeanDesc proxyBeanDesc = new SeataProxyBeanDesc(targetBeanName, targetBeanClass, methodMatcher);
        registerProxyBean(targetBeanName, proxyBeanDesc);
        registerProxyBean(targetBeanClass, proxyBeanDesc);
    }

    public void registerProxyBean(String targetBeanName, Class<?> targetBeanClass) {
        registerProxyBean(targetBeanName, targetBeanClass, null);
    }

    //endregion
}
