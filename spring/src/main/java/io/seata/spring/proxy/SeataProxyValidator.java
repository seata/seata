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

import io.seata.spring.proxy.desc.SeataProxyBeanDesc;
import org.aopalliance.intercept.MethodInvocation;

/**
 * The interface SeataProxyValidator
 *
 * @author wang.liang
 * @see SeataProxyInterceptor
 */
public interface SeataProxyValidator {

    /**
     * check need to skip
     *
     * @param targetBeanDesc the target bean desc
     * @param invocation     the invocation of the bean
     * @return the boolean
     */
    default boolean shouldSkip(SeataProxyBeanDesc targetBeanDesc, MethodInvocation invocation) {
        return false;
    }
}
