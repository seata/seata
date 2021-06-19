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
package io.seata.core.raft;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.RpcMessage;

/**
 * @author funkye
 */
public class RaftClosure implements Closure {

    ChannelHandlerContext ctx;

    RpcMessage rpcMessage;

    MergeResultMessage resultMessage;

    AbstractResultMessage result;

    public void setChannelHandlerContext(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public void setRpcMessage(RpcMessage rpcMessage) {
        this.rpcMessage = rpcMessage;
    }

    public void setMergeResultMessage(MergeResultMessage resultMessage) {
        this.resultMessage = resultMessage;
    }

    public void setAbstractResultMessage(AbstractResultMessage result) {
        this.result = result;
    }

    @Override
    public void run(Status status) {
    }

}
