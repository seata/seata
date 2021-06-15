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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.common.exception.FrameworkException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import io.seata.core.context.RootContext;
import io.seata.core.logger.StackTraceLogger;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import io.seata.spring.annotation.SeataInterceptor;
import io.seata.spring.annotation.SeataInterceptorPosition;
import io.seata.spring.proxy.desc.SeataProxyBeanDesc;
import io.seata.spring.proxy.desc.SeataProxyMethodDesc;
import io.seata.spring.proxy.util.SeataProxyInterceptorUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seata Proxy Interceptor
 *
 * @author wang.liang
 * @see SeataProxyHandler
 */
public class SeataProxyInterceptor implements MethodInterceptor, SeataInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataProxyInterceptor.class);

    private final SeataProxyBeanDesc targetBeanDesc;

    private final String targetBeanName;
    private int orderNum;
    private final SeataInterceptorPosition position;

    public SeataProxyInterceptor(SeataProxyBeanDesc targetBeanDesc) {
        if (targetBeanDesc == null) {
            throw new IllegalArgumentException("targetBeanDesc must be not null");
        }

        this.targetBeanDesc = targetBeanDesc;

        this.targetBeanName = targetBeanDesc.getTargetBeanName();
        this.orderNum = targetBeanDesc.getInterceptorOrderNum();
        this.position = targetBeanDesc.getInterceptorPosition();
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        try {
            if (!RootContext.inGlobalTransaction() || RootContext.inSagaBranch() || !SeataProxyInterceptorUtil.isNeedProxy() ||
                    this.shouldSkip(invocation)) {
                return invocation.proceed();
            }

            // get method
            Method method = invocation.getMethod();

            // if in the try method of the TCC branch, offer a suggestion
            if (RootContext.inTccBranch() && LOGGER.isWarnEnabled() && StackTraceLogger.needToPrintLog()) {
                LOGGER.warn("Currently in the try method of the TCC branch, it's recommended to " +
                                "transfer the `{}` to the commit method of the TCC action '{}'.",
                        ReflectionUtil.methodToString(method), BusinessActionContextUtil.getContext().getActionName());
            }

            // get the proxy handler
            SeataProxyHandler proxyHandler = this.getHandler(method);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("the method `{}` is proxied by the proxy handler '{}' in the '{}'.", method.getName(),
                        proxyHandler.getClass().getName(), SeataProxyInterceptor.class.getName());
            }
            // do proxy
            Object result;
            try {
                result = proxyHandler.doProxy(this.targetBeanName, invocation);
            } catch (Exception e) {
                LOGGER.error("do proxy failed, bean: {}, handler: {}, error: {}", this.targetBeanName,
                        proxyHandler.getClass().getName(), e.getMessage());
                throw new FrameworkException(e, "do proxy failed: " + e.getMessage());
            }

            // get the proxy result handler
            SeataProxyResultHandler resultHandler = this.getResultHandler(method);
            // handle the result of the proxy handler
            try {
                return resultHandler.handle(result, targetBeanDesc, invocation, proxyHandler);
            } catch (Exception e) {
                LOGGER.error("handle proxy result failed, bean: {}, resultHandler: {}, error: {}", this.targetBeanName,
                        resultHandler.getClass().getName(), e.getMessage());
                throw new FrameworkException(e, "handle proxy result failed: " + e.getMessage());
            }
        } finally {
            SeataProxyInterceptorUtil.enableProxy();
        }
    }

    private boolean shouldSkip(MethodInvocation invocation) {
        Method method = invocation.getMethod();

        // skip `Object.toString()`
        String methodName = method.getName();
        if ("toString".equals(methodName) && CollectionUtils.isEmpty(method.getParameterTypes())) {
            return true;
        }

        // get method desc and validate
        SeataProxyMethodDesc methodDesc = targetBeanDesc.getMethodDesc(method);
        if (methodDesc == null || methodDesc.isShouldSkip()) {
            return true;
        }

        // check by the validator
        SeataProxyValidator validator = this.getValidator(method);
        if (validator != null && validator.shouldSkip(this.targetBeanDesc, invocation)) {
            return true;
        }

        return false;
    }

    //region get the implementation

    @Nullable
    public SeataProxyValidator getValidator(Method method) {
        SeataProxyMethodDesc methodDesc = SeataProxyInterceptorUtil.getMethodDesc(targetBeanDesc.getMethodDescMap(), method);
        if (methodDesc != null) {
            SeataProxyValidator validator = methodDesc.getValidator();
            if (validator != null) {
                return validator;
            }
        }

        return SeataProxyInterceptorUtil.getDefaultValidator();
    }

    @Nonnull
    public SeataProxyHandler getHandler(Method method) {
        SeataProxyMethodDesc methodDesc = SeataProxyInterceptorUtil.getMethodDesc(targetBeanDesc.getMethodDescMap(), method);
        if (methodDesc != null) {
            SeataProxyHandler handler = methodDesc.getHandler();
            if (handler != null) {
                return handler;
            }
        }

        return SeataProxyInterceptorUtil.getDefaultHandler();
    }

    @Nonnull
    public SeataProxyResultHandler getResultHandler(Method method) {
        SeataProxyMethodDesc methodDesc = SeataProxyInterceptorUtil.getMethodDesc(targetBeanDesc.getMethodDescMap(), method);
        if (methodDesc != null) {
            SeataProxyResultHandler resultHandler = methodDesc.getResultHandler();
            if (resultHandler != null) {
                return resultHandler;
            }
        }

        return SeataProxyInterceptorUtil.getDefaultResultHandler();
    }

    //endregion


    @Override
    public int getOrder() {
        return orderNum;
    }

    @Override
    public void setOrder(int order) {
        this.orderNum = order;
    }

    @Override
    public SeataInterceptorPosition getPosition() {
        return position;
    }
}
