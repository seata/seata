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
package io.seata.spring.tcc;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import io.seata.common.Constants;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.interceptor.ActionContextUtil;
import io.seata.spring.proxy.SeataProxyHandler;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Tcc Seata Proxy Handler
 *
 * @author wang.liang
 * @see io.seata.spring.proxy.SeataProxyInterceptor
 * @see TccSeataProxyAction
 * @see DefaultTccSeataProxyActionImpl
 */
public class TccSeataProxyHandler implements SeataProxyHandler, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private TccSeataProxyAction tccSeataProxyAction;

    @Override
    public Object doProxy(String targetBeanName, MethodInvocation invocation) throws Exception {
        Method method = invocation.getMethod();

        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = invocation.getArguments();

        // create actionContext
        BusinessActionContext actionContext = new BusinessActionContext();
        Map<String, Object> context = new HashMap<>();
        actionContext.setActionContext(context);

        // put the context of the invocation into actionContext
        // contains: target bean name, method name, parameter types, arguments
        context.put(Constants.TCC_PROXY_TARGET_BEAN_NAME, targetBeanName);
        context.put(Constants.TCC_PROXY_METHOD_NAME, methodName);
        if (parameterTypes.length > 0) {
            Object[] newArgs = ActionContextUtil.handleArgs(args);

            context.put(Constants.TCC_PROXY_METHOD_PARAMETER_TYPES, JSON.toJSONString(parameterTypes));
            context.put(Constants.TCC_PROXY_METHOD_ARGS, JSON.toJSONString(newArgs));
        }

        // do prepare
        this.getTccSeataProxyAction().prepare(actionContext);

        // no data to return, so null is always returned
        return null;
    }

    private TccSeataProxyAction getTccSeataProxyAction() {
        if (tccSeataProxyAction == null) {
            synchronized (TccSeataProxyHandler.class) {
                if (tccSeataProxyAction == null) {
                    tccSeataProxyAction = applicationContext.getBean(TccSeataProxyAction.class);
                }
            }
        }
        return tccSeataProxyAction;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
