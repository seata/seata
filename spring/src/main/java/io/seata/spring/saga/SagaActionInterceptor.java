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
package io.seata.spring.saga;

import io.seata.common.Constants;
import io.seata.common.executor.Callback;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.rm.saga.api.SagaCompensiable;
import io.seata.rm.saga.interceptor.ActionInterceptorHandler;
import io.seata.rm.saga.remoting.RemotingDesc;
import io.seata.rm.saga.remoting.parser.DubboUtil;
import io.seata.spring.util.SpringProxyUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * SAGA Interceptor
 *
 * @author zhangsen
 */
public class SagaActionInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaActionInterceptor.class);

    private static final String DUBBO_PROXY_NAME_PREFIX="com.alibaba.dubbo.common.bytecode.proxy";


    private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    /**
     * remoting bean info
     */
    protected RemotingDesc remotingDesc;

    /**
     * Instantiates a new SAGA action interceptor.
     */
    public SagaActionInterceptor() {
    }

    /**
     * Instantiates a new SAGA action interceptor.
     *
     * @param remotingDesc the remoting desc
     */
    public SagaActionInterceptor(RemotingDesc remotingDesc) {
        this.remotingDesc = remotingDesc;
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        if(!RootContext.inGlobalTransaction()){
            //not in transaction
            return invocation.proceed();
        }
        Method method = getActionInterfaceMethod(invocation);
        SagaCompensiable businessAction = method.getAnnotation(SagaCompensiable.class);
        //try method
        if (businessAction != null) {
            //save the xid
            String xid = RootContext.getXID();
            String xidType = String.format("%s_%s", xid, BranchType.SAGA.name());
            //clear the context
            RootContext.unbind();
            RootContext.bindType(xidType);
            try {
                Object[] methodArgs = invocation.getArguments();
                //Handler the SAGA Aspect
                Map<String, Object> ret = actionInterceptorHandler.proceed(method, methodArgs, xid, businessAction,
                        new Callback<Object>() {
                            @Override
                            public Object execute() throws Throwable {
                                return invocation.proceed();
                            }
                        });
                //return the final result
                return ret.get(Constants.SAGA_METHOD_RESULT);
            } finally {
                //recovery the context
                RootContext.bind(xid);
                RootContext.unbindType();
            }
        }
        return invocation.proceed();
    }

    /**
     * get the method from interface
     *
     * @param invocation the invocation
     * @return the action interface method
     */
    protected Method getActionInterfaceMethod(MethodInvocation invocation) {
        try {
            Class<?> interfaceType = null;
            if (remotingDesc == null) {
                interfaceType = getProxyInterface(invocation.getThis());
            } else {
                interfaceType = remotingDesc.getInterfaceClass();
            }
            if (interfaceType == null && remotingDesc.getInterfaceClassName() != null) {
                interfaceType = Class.forName(remotingDesc.getInterfaceClassName(), true,
                    Thread.currentThread().getContextClassLoader());
            }
            if (interfaceType == null) {
                return invocation.getMethod();
            }
            Method method = interfaceType.getMethod(invocation.getMethod().getName(),
                invocation.getMethod().getParameterTypes());
            return method;
        } catch (Exception e) {
            LOGGER.warn("get Method from interface failed", e);
            return invocation.getMethod();
        }
    }

    /**
     * get the interface of proxy
     *
     * @param proxyBean the proxy bean
     * @return proxy interface
     * @throws Exception the exception
     */
    protected Class<?> getProxyInterface(Object proxyBean) throws Exception {
        if (proxyBean.getClass().getName().startsWith(DUBBO_PROXY_NAME_PREFIX)) {
            //dubbo javaassist proxy
            return DubboUtil.getAssistInterface(proxyBean);
        } else {
            //jdk/cglib proxy
            return SpringProxyUtils.getTargetInterface(proxyBean);
        }
    }
}
