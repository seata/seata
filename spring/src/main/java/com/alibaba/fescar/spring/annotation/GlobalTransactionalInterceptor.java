/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.spring.annotation;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.spring.schema.TransationConfigBean;
import com.alibaba.fescar.spring.schema.TransationConfigManager;
import com.alibaba.fescar.tm.api.DefaultFailureHandlerImpl;
import com.alibaba.fescar.tm.api.FailureHandler;
import com.alibaba.fescar.tm.api.TransactionalExecutor;
import com.alibaba.fescar.tm.api.TransactionalTemplate;

/**
 * The type Global transactional interceptor.
 */
public class GlobalTransactionalInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionalInterceptor.class);
    private static final FailureHandler DEFAULT_FAIL_HANDLER = new DefaultFailureHandlerImpl();

    private final TransactionalTemplate transactionalTemplate = new TransactionalTemplate();
    private final FailureHandler failureHandler;

    /**
     * Instantiates a new Global transactional interceptor.
     *
     * @param failureHandler the failure handler
     */
    public GlobalTransactionalInterceptor(FailureHandler failureHandler) {
        if (null == failureHandler) {
            failureHandler = DEFAULT_FAIL_HANDLER;
        }
        this.failureHandler = failureHandler;
    }

    @Override
    public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
    	
    	Method method = methodInvocation.getMethod();
    	String key = org.apache.commons.lang3.StringUtils.removeStart(method.getDeclaringClass()+ "#" + method.getName(), "class");
    	
    	TransationConfigBean configBean = TransationConfigManager.getInstance().getConfig(key);
    	
        if (configBean != null) {
            try {
                return transactionalTemplate.execute(new TransactionalExecutor() {
                    @Override
                    public Object execute() throws Throwable {
                        return methodInvocation.proceed();
                    }

                    @Override
                    public int timeout() {
                        return configBean.getTimeout();
                    }

                    @Override
                    public String name() {
                        String name = configBean.getName();
                        if (!StringUtils.isEmpty(name)) {
                            return name;
                        }
                        return formatMethod(methodInvocation.getMethod());
                    }
                });
            } catch (TransactionalExecutor.ExecutionException e) {
                TransactionalExecutor.Code code = e.getCode();
                switch (code) {
                    case RollbackDone:
                        throw e.getOriginalException();
                    case BeginFailure:
                        failureHandler.onBeginFailure(e.getTransaction(), e.getCause());
                        throw e.getCause();
                    case CommitFailure:
                        failureHandler.onCommitFailure(e.getTransaction(), e.getCause());
                        throw e.getCause();
                    case RollbackFailure:
                        failureHandler.onRollbackFailure(e.getTransaction(), e.getCause());
                        throw e.getCause();
                    default:
                        throw new ShouldNeverHappenException("Unknown TransactionalExecutor.Code: " + code);

                }
            }

        }
        return methodInvocation.proceed();
    }

    private String formatMethod(Method method) {
        StringBuilder sb = new StringBuilder();

        String methodName = method.getName();
        Class<?>[] params = method.getParameterTypes();
        sb.append(methodName);
        sb.append("(");

        int paramPos = 0;
        for (Class<?> clazz : params) {
            sb.append(clazz.getName());
            if (++paramPos < params.length) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
