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

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.tc.UUIDGenerator;
import io.seata.server.coordinator.DefaultCoordinator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author slievrly
 */
@Disabled
public class TmNettyClientTest {

    private static final ThreadPoolExecutor
        workingThreads = new ThreadPoolExecutor(100, 500, 500, TimeUnit.SECONDS,
                                                new LinkedBlockingQueue(20000), new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testDoConnect() throws Exception {

        //start services server first
        workingThreads.submit(new Runnable() {
            @Override
            public void run() {
                NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads);
                nettyRemotingServer.setHandler(new DefaultCoordinator(nettyRemotingServer));
                UUIDGenerator.init(1L);
                nettyRemotingServer.init();
            }
        });

        //then test client
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

        tmNettyRemotingClient.init();

        Method doConnectMethod = TmNettyRemotingClient.class.getDeclaredMethod("doConnect", String.class);
        doConnectMethod.setAccessible(true);
        String serverAddress = "0.0.0.0:8091";
        Channel channel = (Channel) doConnectMethod.invoke(tmNettyRemotingClient, serverAddress);
        Assertions.assertNotNull(channel);
    }

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testReconnect() throws Exception {

        //start services server first
        workingThreads.submit(new Runnable() {
            @Override
            public void run() {
                NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads);
                nettyRemotingServer.setHandler(new DefaultCoordinator(nettyRemotingServer));
                UUIDGenerator.init(1L);
                nettyRemotingServer.init();
            }
        });

        //then test client
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "my_test_tx_group";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

        tmNettyRemotingClient.init();

        Method doConnectMethod = TmNettyRemotingClient.class.getDeclaredMethod("reconnect");
        doConnectMethod.setAccessible(true);
        doConnectMethod.invoke(tmNettyRemotingClient);
    }

    @Test
    public void testSendMsgWithResponse() throws Exception {
        workingThreads.submit(new Runnable() {
            @Override
            public void run() {
                NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads);
                nettyRemotingServer.setHandler(new DefaultCoordinator(nettyRemotingServer));
                UUIDGenerator.init(1L);
                nettyRemotingServer.init();
            }
        });
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "my_test_tx_group";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        tmNettyRemotingClient.init();

        Method doConnectMethod = TmNettyRemotingClient.class.getDeclaredMethod("doConnect", String.class);
        doConnectMethod.setAccessible(true);
        String serverAddress = "0.0.0.0:8091";
        Channel channel = (Channel) doConnectMethod.invoke(tmNettyRemotingClient, serverAddress);
        Assertions.assertNotNull(channel);

        BranchRegisterRequest request = new BranchRegisterRequest();
        request.setXid("127.0.0.1:8091:1249853");
        request.setLockKey("lock key testSendMsgWithResponse");
        request.setResourceId("resoutceId1");
        BranchRegisterResponse branchRegisterResponse = (BranchRegisterResponse) tmNettyRemotingClient.sendSyncRequest(request);
        Assertions.assertNotNull(branchRegisterResponse);
        Assertions.assertEquals(ResultCode.Failed, branchRegisterResponse.getResultCode());
        Assertions.assertEquals("RuntimeException[SessionManager is NOT init!]",
                            branchRegisterResponse.getMsg());
    }
}
