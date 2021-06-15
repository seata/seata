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
 * The interface SeataProxyResultHandler
 *
 * @author wang.liang
 * @see SeataProxyInterceptor
 * @see SeataProxyHandler
 * @see io.seata.spring.proxy.resulthandler.impl.DefaultSeataProxyResultHandlerImpl
 */
public interface SeataProxyResultHandler {

    /**
     * handle the result of the proxy handler
     *
     * @param proxyHandlerResult the result of the proxy handler
     * @param beanDesc           the bean desc
     * @param invocation         the invocation
     * @param proxyHandler       the proxy handler
     * @return the final result
     * @throws Exception the Exception
     */
    Object handle(Object proxyHandlerResult, SeataProxyBeanDesc beanDesc, MethodInvocation invocation,
                  SeataProxyHandler proxyHandler) throws Exception;
}
