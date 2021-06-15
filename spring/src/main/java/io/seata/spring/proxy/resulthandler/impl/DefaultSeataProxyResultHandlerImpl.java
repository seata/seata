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
package io.seata.spring.proxy.resulthandler.impl;

import java.lang.reflect.Method;

import io.seata.common.util.ReflectionUtil;
import io.seata.core.logger.StackTraceLogger;
import io.seata.spring.proxy.SeataProxy;
import io.seata.spring.proxy.SeataProxyHandler;
import io.seata.spring.proxy.SeataProxyResultHandler;
import io.seata.spring.proxy.desc.SeataProxyBeanDesc;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Implementation of the {@link io.seata.spring.proxy.SeataProxyResultHandler}
 *
 * @author wang.liang
 */
public class DefaultSeataProxyResultHandlerImpl implements SeataProxyResultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSeataProxyResultHandlerImpl.class);

    @Override
    public Object handle(Object proxyHandlerResult, SeataProxyBeanDesc beanDesc, MethodInvocation invocation,
                         SeataProxyHandler proxyHandler) throws Exception {
        // if the result is null, validate the return type
        if (proxyHandlerResult == null) {
            Method method = invocation.getMethod();
            Class<?> returnType = method.getReturnType();

            // if the return type is boolean
            if (returnType == boolean.class || returnType == Boolean.class) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("`null` is returned from `{}.doProxy(...)`, but the return type is `{}.class`, " +
                                    "so return `true` to replace the `null`.",
                            proxyHandler.getClass().getName(), returnType.getSimpleName());
                }
                // return `true` to replace the `null`
                return true;
            }

            // if the return type is not void
            if (returnType != void.class && returnType != Void.class
                    && LOGGER.isWarnEnabled() && StackTraceLogger.needToPrintLog()) {
                LOGGER.warn("`null` is returned from `{}.doProxy(...)`, but the return type of `{}` is not `void.class`. " +
                                "If you do not want the method to be proxied, please use the `SeataProxyUtil.disableProxy()` before calling the method, " +
                                "or add the `@{}(skip = true)` on the method.",
                        proxyHandler.getClass().getName(), ReflectionUtil.methodToString(method), SeataProxy.class.getSimpleName());
            }
        }

        return proxyHandlerResult;
    }
}
