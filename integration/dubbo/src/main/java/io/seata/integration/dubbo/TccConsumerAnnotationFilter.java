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
package io.seata.integration.dubbo;

import io.seata.common.executor.Callback;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.interceptor.ActionInterceptorHandler;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

@Activate(group = {Constants.CONSUMER}, order = 95)
public class TccConsumerAnnotationFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TccConsumerAnnotationFilter.class);
    private final static String DUBBO_GENERIC_SERVICE_INVOKE = "$invoke";
    private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            if (!RootContext.inGlobalTransaction() && !RootContext.inGlobalTransactionSagaTcc()) {
                return invoker.invoke(invocation);
            }

            RpcInvocation rpcInvocation = (RpcInvocation) invocation;
            String methodName = rpcInvocation.getMethodName();
            Class<?>[] parameterTypes = rpcInvocation.getParameterTypes();
            Object[] arguments = rpcInvocation.getArguments();
            Class interfaceClass = invoker.getInterface();
            if (DUBBO_GENERIC_SERVICE_INVOKE.equals(methodName)) {
                return invoker.invoke(invocation);
            }

            Method method = interfaceClass.getMethod(methodName, parameterTypes);

            TwoPhaseBusinessAction businessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
            //try method
            if (businessAction != null) {
                //save the xid
                String xid = RootContext.getXID();
                //clear the context
                RootContext.unbind();
                RootContext.bindAnnotationType(xid, BranchType.TCC);
                try {
                    Map<String, Object> ret = actionInterceptorHandler.proceed(method, arguments, xid, businessAction, new Callback<Object>() {
                        @Override
                        public Object execute() throws Throwable {
                            return invoker.invoke(invocation);
                        }
                    });
                    return (Result) ret.get(io.seata.common.Constants.TCC_METHOD_RESULT);
                } finally {
                    //recovery the context
                    RootContext.unbindAnnotationType();
                    RootContext.bind(xid);
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Tcc dubbo invokes service to register branch transaction exception:{}", e.getMessage(), e);
            RpcResult result = new RpcResult();
            result.setValue(false);
            result.setException(e);
            return result;
        }
        return invoker.invoke(invocation);
    }
}
