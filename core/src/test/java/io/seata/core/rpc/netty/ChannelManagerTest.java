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
package io.seata.core.rpc.netty;

import io.netty.channel.Channel;
import io.seata.common.Constants;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.rpc.RpcContext;
import org.junit.jupiter.api.Test;

import java.net.SocketAddress;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author selfishlover
 */
class ChannelManagerTest {

    private final String appId = "test";

    private final String resourceId = "mysql";

    private final String clientIp = "192.168.1.1";

    private final Integer clientPort = 8080;

    @Test
    void registerTMChannel() {
        RegisterTMRequest request = new RegisterTMRequest();
        request.setApplicationId(appId);

        Channel channel = buildChannel();

        assertDoesNotThrow(() -> ChannelManager.registerTMChannel(request, channel));
        assertTrue(ChannelManager.isRegistered(channel), "fail to register TM");
        Map<Integer, RpcContext> portMap = ChannelManager.getTmPortMap(appId, clientIp);
        assertTrue(portMap.containsKey(clientPort), "fail to save TM channel");

        ChannelManager.releaseRpcContext(channel);
        assertFalse(ChannelManager.isRegistered(channel), "fail to unregister TM");
        assertFalse(portMap.containsKey(clientPort), "fail to clean TM channel");
    }

    @Test
    void registerRMChannel() {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId(appId);
        request.setResourceIds(resourceId);

        Channel channel = buildChannel();

        assertDoesNotThrow(() -> ChannelManager.registerRMChannel(request, channel));
        assertTrue(ChannelManager.isRegistered(channel), "fail to register RM");
        Map<Integer, RpcContext> portMap = ChannelManager.getRmPortMap(resourceId, appId, clientIp);
        assertTrue(portMap.containsKey(clientPort), "fail to save RM channel");

        ChannelManager.releaseRpcContext(channel);
        assertFalse(ChannelManager.isRegistered(channel), "fail to unregister RM");
        assertFalse(portMap.containsKey(clientPort), "fail to clean RM channel");
    }

    private Channel buildChannel() {
        SocketAddress address = mock(SocketAddress.class);
        when(address.toString()).thenReturn(getAddress());
        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(address);
        return channel;
    }

    private String getAddress() {
        return clientIp + Constants.IP_PORT_SPLIT_CHAR + clientPort;
    }
}