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

import io.netty.channel.ChannelHandlerContext;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.Version;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.RegisterCheckAuthHandler;
import io.seata.core.rpc.processor.RemotingProcessor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * process TM client registry message.
 * <p>
 * process message type:
 * {@link RegisterTMRequest}
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public class RegTmProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegTmProcessor.class);

    private RemotingServer remotingServer;

    private RegisterCheckAuthHandler checkAuthHandler;

    public RegTmProcessor(RemotingServer remotingServer) {
        this.remotingServer = remotingServer;
        this.checkAuthHandler = EnhancedServiceLoader.load(RegisterCheckAuthHandler.class);
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        onRegTmMessage(ctx, rpcMessage);
    }

    private void onRegTmMessage(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        RegisterTMRequest message = (RegisterTMRequest) rpcMessage.getBody();
        String ipAndPort = NetUtil.toStringAddress(ctx.channel().remoteAddress());
        Version.putChannelVersion(ctx.channel(), message.getVersion());
        boolean isSuccess = false;
        String errorInfo = StringUtils.EMPTY;
        try {
            if (null == checkAuthHandler || checkAuthHandler.regTransactionManagerCheckAuth(message)) {
                ChannelManager.registerTMChannel(message, ctx.channel());
                isSuccess = true;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("checkAuth for client:{},vgroup:{},applicationId:{}",
                        ipAndPort, message.getTransactionServiceGroup(), message.getApplicationId());
                }
            }
        } catch (Exception exx) {
            isSuccess = false;
            errorInfo = exx.getMessage();
            LOGGER.error("TM register fail, error message:{}", errorInfo);
        }
        RegisterTMResponse response = new RegisterTMResponse(isSuccess);
        if (StringUtils.isNotEmpty(errorInfo)) {
            response.setMsg(errorInfo);
        }
        remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), response);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("TM register success,message:{},channel:{},client version:{}", message, ctx.channel(),
                message.getVersion());
        }
    }

}
