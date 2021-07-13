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
package io.seata.integration.hsf;

import com.taobao.hsf.context.RPCContext;
import com.taobao.hsf.invocation.Invocation;
import com.taobao.hsf.invocation.InvocationHandler;
import com.taobao.hsf.invocation.RPCResult;
import com.taobao.hsf.invocation.filter.ClientFilter;
import com.taobao.hsf.invocation.filter.ServerFilter;
import com.taobao.hsf.util.concurrent.ListenableFuture;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Transaction propagation filter.
 *
 * @author slievrly
 */
public class HsfTransactionFilter implements ClientFilter, ServerFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HsfTransactionFilter.class);

    @Override
    public ListenableFuture<RPCResult> invoke(InvocationHandler invocationHandler, Invocation invocation)
        throws Throwable {
        String xid = RootContext.getXID();
        BranchType branchType = RootContext.getBranchType();

        Object rpcXid = RPCContext.getServerContext().getAttachment(RootContext.KEY_XID);
        Object rpcBranchType = RPCContext.getServerContext().getAttachment(RootContext.KEY_BRANCH_TYPE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in RootContext[{}] xid in RpcContext[{}]", xid, rpcXid);
        }
        boolean bind = false;
        if (xid != null) {
            RPCContext.getClientContext().putAttachment(RootContext.KEY_XID, xid);
            RPCContext.getClientContext().putAttachment(RootContext.KEY_BRANCH_TYPE, branchType.name());
        } else {
            if (rpcXid != null) {
                RootContext.bind(rpcXid.toString());
                if (StringUtils.equals(BranchType.TCC.name(), rpcBranchType.toString())) {
                    RootContext.bindBranchType(BranchType.TCC);
                }
                bind = true;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("bind xid [{}] branchType [{}] to RootContext", rpcXid, rpcBranchType);
                }
            }
        }
        try {
            return invocationHandler.invoke(invocation);
        } finally {
            if (bind) {
                BranchType previousBranchType = RootContext.getBranchType();
                String unbindXid = RootContext.unbind();
                if (BranchType.TCC == previousBranchType) {
                    RootContext.unbindBranchType();
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("unbind xid [{}] branchType [{}] from RootContext", unbindXid, previousBranchType);
                }
                if (!rpcXid.toString().equalsIgnoreCase(unbindXid)) {
                    LOGGER.warn("xid in change during RPC from {} to {},branchType from {} to {}", rpcXid, unbindXid,
                        rpcBranchType != null ? rpcBranchType : "AT", previousBranchType);
                    if (unbindXid != null) {
                        RootContext.bind(unbindXid);
                        LOGGER.warn("bind xid [{}] back to RootContext", unbindXid);
                        if (BranchType.TCC == previousBranchType) {
                            RootContext.bindBranchType(BranchType.TCC);
                            LOGGER.warn("bind branchType [{}] back to RootContext", previousBranchType);
                        }
                    }
                }
            }
            RPCContext.getClientContext().removeAttachment(RootContext.KEY_XID);
            RPCContext.getClientContext().removeAttachment(RootContext.KEY_BRANCH_TYPE);
            RPCContext.getServerContext().removeAttachment(RootContext.KEY_XID);
            RPCContext.getServerContext().removeAttachment(RootContext.KEY_BRANCH_TYPE);
        }
    }

    @Override
    public void onResponse(Invocation invocation, RPCResult rpcResult) {

    }
}
