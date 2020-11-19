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
package io.seata.core.raft.msg;

import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.RpcContext;

/**
 * @author funkye
 */
public class RaftOnRequestMsg extends RaftSyncMsg {

    RpcMessage rpcMessage;
    boolean leader;
    RpcContext rpcContext;

    public RaftOnRequestMsg(MsgType msgType, RpcMessage rpcMessage, boolean leader, RpcContext rpcContext) {
        this.rpcMessage = rpcMessage;
        this.leader = leader;
        this.rpcContext = rpcContext;
        this.msgType = msgType;
    }

    @Override
    public MsgType getMsgType() {
        return this.msgType;
    }

    public RpcMessage getRpcMessage() {
        return rpcMessage;
    }

    public void setRpcMessage(RpcMessage rpcMessage) {
        this.rpcMessage = rpcMessage;
    }

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }

    public RpcContext getRpcContext() {
        return rpcContext;
    }

    public void setRpcContext(RpcContext rpcContext) {
        this.rpcContext = rpcContext;
    }
}
