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

import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Transaction propagation filter.
 *
 * @author sharajava
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER}, order = 100)
public class TransactionPropagationInterceptorFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionPropagationInterceptorFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String xidInterceptorType = RootContext.getXIDInterceptorType();
        String rpcXidInterceptorType = RpcContext.getContext().getAttachment(RootContext.KEY_XID_INTERCEPTOR_TYPE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xidInterceptorType in RootContext[{}] rpcXidInterceptorType in RpcContext[{}]",
                    xidInterceptorType, rpcXidInterceptorType);
        }
        boolean bind = false;
        if (StringUtils.isNotBlank(xidInterceptorType)) {
            RpcContext.getContext().setAttachment(RootContext.KEY_XID_INTERCEPTOR_TYPE, xidInterceptorType);
        } else {
            if (StringUtils.isNotBlank(rpcXidInterceptorType)) {
                RootContext.bindInterceptorType(rpcXidInterceptorType);
                bind = true;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("bind interceptorType[{}] to RootContext", rpcXidInterceptorType);
                }
            }
        }

        try {
            return invoker.invoke(invocation);
        } finally {
            if (bind) {
                String unbindInterceptorType = RootContext.unbindInterceptorType();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("unbind interceptorType[{}] from RootContext", unbindInterceptorType);
                }
                if (!rpcXidInterceptorType.equalsIgnoreCase(unbindInterceptorType)) {
                    LOGGER.warn("xidInterceptorType in change during RPC from {} to {}", rpcXidInterceptorType, unbindInterceptorType);
                    if (unbindInterceptorType != null) {
                        RootContext.bindInterceptorType(unbindInterceptorType);
                        LOGGER.warn("bind interceptorType [{}] back to RootContext", unbindInterceptorType);
                    }
                }
            }
        }
    }
}
