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
package io.seata.core.rpc.processor.server;

import io.seata.core.exception.TransactionExceptionCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.entity.Task;

import io.netty.channel.ChannelHandlerContext;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.AbstractTransactionResponse;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.raft.AbstractRaftServer;
import io.seata.core.raft.RaftClosure;
import io.seata.core.raft.RaftServerFactory;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.core.rpc.processor.RemotingProcessor;

/**
 * process RM/TM client request message.
 * <p>
 * message type:
 * RM:
 * 1) {@link MergedWarpMessage}
 * 2) {@link BranchRegisterRequest}
 * 3) {@link BranchReportRequest}
 * 4) {@link GlobalLockQueryRequest}
 * TM:
 * 1) {@link MergedWarpMessage}
 * 2) {@link GlobalBeginRequest}
 * 3) {@link GlobalCommitRequest}
 * 4) {@link GlobalReportRequest}
 * 5) {@link GlobalRollbackRequest}
 * 6) {@link GlobalStatusRequest}
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public class ServerOnRequestProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerOnRequestProcessor.class);

    private RemotingServer remotingServer;

    private TransactionMessageHandler transactionMessageHandler;

    private Boolean raftMode = false;

    public ServerOnRequestProcessor(RemotingServer remotingServer, TransactionMessageHandler transactionMessageHandler) {
        this.remotingServer = remotingServer;
        this.transactionMessageHandler = transactionMessageHandler;
        AbstractRaftServer raftServer = RaftServerFactory.getInstance().getRaftServer();
        if (raftServer != null) {
            this.raftMode = RaftServerFactory.getInstance().isRaftMode();
        }
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        if (ChannelManager.isRegistered(ctx.channel())) {
            onRequestMessage(ctx, rpcMessage);
        } else {
            try {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("closeChannelHandlerContext channel:" + ctx.channel());
                }
                ctx.disconnect();
                ctx.close();
            } catch (Exception exx) {
                LOGGER.error(exx.getMessage());
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("close a unhandled connection! [%s]", ctx.channel().toString()));
            }
        }
    }

    private void onRequestMessage(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        Object message = rpcMessage.getBody();
        RpcContext rpcContext = ChannelManager.getContextFromIdentified(ctx.channel());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("server received:{},clientIp:{},vgroup:{}", message,
                NetUtil.toIpAddress(ctx.channel().remoteAddress()), rpcContext.getTransactionServiceGroup());
        } else {
            try {
                BatchLogHandler.INSTANCE.getLogQueue()
                    .put(message + ",clientIp:" + NetUtil.toIpAddress(ctx.channel().remoteAddress()) + ",vgroup:"
                        + rpcContext.getTransactionServiceGroup());
            } catch (InterruptedException e) {
                LOGGER.error("put message to logQueue error: {}", e.getMessage(), e);
            }
        }
        if (!(message instanceof AbstractMessage)) {
            return;
        }
        if (message instanceof MergedWarpMessage) {
            AbstractResultMessage[] results = new AbstractResultMessage[((MergedWarpMessage) message).msgs.size()];
            boolean notLeaderError = false;
            for (int i = 0; i < results.length; i++) {
                final AbstractMessage subMessage = ((MergedWarpMessage)message).msgs.get(i);
                AbstractResultMessage result = transactionMessageHandler.onRequest(subMessage, rpcContext);
                results[i] = result;
                if (result instanceof AbstractTransactionResponse) {
                    notLeaderError = ((AbstractTransactionResponse)result).getTransactionExceptionCode()
                        .equals(TransactionExceptionCode.NotRaftLeader) ? true : false;
                    if (notLeaderError) {
                        break;
                    }
                }
            }
            MergeResultMessage resultMessage = new MergeResultMessage();
            resultMessage.setMsgs(results);
            if (!raftMode || notLeaderError) {
                remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), resultMessage);
            } else {
                RaftClosure closure = new RaftClosure() {
                    @Override
                    public void run(Status status) {
                        if (status.isOk()) {
                            remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), resultMessage);
                        }
                    }
                };
                closure.setChannelHandlerContext(ctx);
                closure.setRpcMessage(rpcMessage);
                closure.setMergeResultMessage(resultMessage);
                final Task task = new Task();
                task.setDone(closure);
                RaftServerFactory.getInstance().getRaftServer().getNode().apply(task);
            }
        } else {
            // the single send request message
            final AbstractMessage msg = (AbstractMessage)message;
            AbstractResultMessage result = transactionMessageHandler.onRequest(msg, rpcContext);
            if (!raftMode) {
                remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), result);
            } else {
                RaftClosure closure = new RaftClosure() {
                    @Override
                    public void run(Status status) {
                        if (status.isOk()) {
                            remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), result);
                        }
                    }
                };
                closure.setChannelHandlerContext(ctx);
                closure.setRpcMessage(rpcMessage);
                closure.setAbstractResultMessage(result);
                final Task task = new Task();
                task.setDone(closure);
                RaftServerFactory.getInstance().getRaftServer().getNode().apply(task);
            }
        }
    }

}
