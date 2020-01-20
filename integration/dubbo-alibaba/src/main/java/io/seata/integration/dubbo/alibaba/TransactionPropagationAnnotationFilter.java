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
package io.seata.integration.dubbo.alibaba;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Transaction propagation filter.
 *
 * @author sharajava
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER}, order = 100)
public class TransactionPropagationAnnotationFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionPropagationAnnotationFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String xidAnnotationType = RootContext.getXIDAnnotationType();
        String rpcXidAnnotationType = RpcContext.getContext().getAttachment(RootContext.KEY_XID_ANNOTATION_TYPE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xidAnnotationType in RootContext[{}] rpcXidAnnotationType in RpcContext[{}]",
                    xidAnnotationType, rpcXidAnnotationType);
        }
        boolean bind = false;
        if (StringUtils.isNotBlank(xidAnnotationType)) {
            RpcContext.getContext().setAttachment(RootContext.KEY_XID_ANNOTATION_TYPE, xidAnnotationType);
        } else {
            if (StringUtils.isNotBlank(rpcXidAnnotationType)) {
                RootContext.bindAnnotationType(rpcXidAnnotationType);
                bind = true;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("bind annotationType[{}] to RootContext", rpcXidAnnotationType);
                }
            }
        }
        try {
            return invoker.invoke(invocation);
        } finally {
            if (bind) {
                String unbindAnnotationType = RootContext.unbindAnnotationType();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("unbind annotationType[{}] from RootContext", unbindAnnotationType);
                }
                if (!rpcXidAnnotationType.equalsIgnoreCase(unbindAnnotationType)) {
                    LOGGER.warn("xidAnnotationType in change during RPC from {} to {}", rpcXidAnnotationType, unbindAnnotationType);
                    if (unbindAnnotationType != null) {
                        RootContext.bindAnnotationType(unbindAnnotationType);
                        LOGGER.warn("bind annotationType [{}] back to RootContext", unbindAnnotationType);
                    }
                }
            }
        }
    }
}
