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
package io.seata.spring.interceptor;

import java.lang.reflect.Method;

import io.seata.spring.annotation.AspectTransactional;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.ClassUtils;

import io.seata.spring.annotation.GlobalTransactional;
import io.seata.spring.util.GlobalTransactionalCheck;
import io.seata.tm.api.FailureHandler;

/**
 * @author funkye
 */
public class AspectTransactionalInterceptor extends GlobalInterceptor implements MethodInterceptor {
    private final FailureHandler failureHandler;
    private final AspectTransactional aspectTransactional;
    private static final AspectTransactional DEFAULT_AT_TRANSACTIONAL = new AspectTransactional();

    public AspectTransactionalInterceptor(FailureHandler failureHandler, AspectTransactional aspectTransactional) {
        this.failureHandler = null == failureHandler ? DEFAULT_FAIL_HANDLER : failureHandler;
        this.aspectTransactional = null == aspectTransactional ? DEFAULT_AT_TRANSACTIONAL : aspectTransactional;
    }

    public AspectTransactionalInterceptor() {
        this.failureHandler = DEFAULT_FAIL_HANDLER;
        this.aspectTransactional = DEFAULT_AT_TRANSACTIONAL;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Class<?> targetClass = invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null;
        Method specificMethod = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        if (null != specificMethod) {
            final Method method = BridgeMethodResolver.findBridgedMethod(specificMethod);
            final GlobalTransactional globalTransactionalAnnotation =
                getAnnotation(method, targetClass, GlobalTransactional.class);
            if (null == globalTransactionalAnnotation && !GlobalTransactionalCheck.localDisable()) {
                return handleGlobalTransaction.runTransaction(invocation, aspectTransactional, failureHandler,
                    transactionalTemplate);
            }
        }
        return invocation.proceed();
    }

}
