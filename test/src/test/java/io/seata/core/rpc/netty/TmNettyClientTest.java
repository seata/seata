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
import io.seata.common.XID;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.saga.engine.db.AbstractServerTest;
import io.seata.server.UUIDGenerator;
import io.seata.server.coordinator.DefaultCoordinator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.management.ManagementFactory;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author slievrly
 */
public class TmNettyClientTest extends AbstractServerTest {

    public static ThreadPoolExecutor initMessageExecutor() {
        return new ThreadPoolExecutor(100, 500, 500, TimeUnit.SECONDS,
                new LinkedBlockingQueue(20000), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testDoConnect() throws Exception {
        ThreadPoolExecutor workingThreads = initMessageExecutor();
        NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads);
        //start services server first
        Thread thread = new Thread(() -> {
            nettyRemotingServer.setHandler(new DefaultCoordinator(nettyRemotingServer));
            nettyRemotingServer.setListenPort(8091);
            // set registry
            XID.setIpAddress(NetUtil.getLocalIp());
            XID.setPort(8091);
            // init snowflake for transactionId, branchId
            UUIDGenerator.init(1L);
            System.out.println("pid info: "+ ManagementFactory.getRuntimeMXBean().getName());
            nettyRemotingServer.init();
        });
        thread.start();

        //then test client
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

        tmNettyRemotingClient.init();
        String serverAddress = "0.0.0.0:8091";
        Channel channel = TmNettyRemotingClient.getInstance().getClientChannelManager().acquireChannel(serverAddress);
        Assertions.assertNotNull(channel);
        nettyRemotingServer.destroy();
        tmNettyRemotingClient.destroy();
    }

    /**
     * Client rely on server's starting first
     *
     * @throws Exception
     */
    @Test
    public void testReconnect() throws Exception {
        ThreadPoolExecutor workingThreads = initMessageExecutor();
        NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads);
        //start services server first
        Thread thread = new Thread(() -> {
            nettyRemotingServer.setHandler(new DefaultCoordinator(nettyRemotingServer));
            nettyRemotingServer.setListenPort(8091);
            // set registry
            XID.setIpAddress(NetUtil.getLocalIp());
            XID.setPort(8091);
            // init snowflake for transactionId, branchId
            UUIDGenerator.init(1L);
            nettyRemotingServer.init();
        });
        thread.start();

        //then test client
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "my_test_tx_group";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

        tmNettyRemotingClient.init();

        TmNettyRemotingClient.getInstance().getClientChannelManager().reconnect(transactionServiceGroup);
        nettyRemotingServer.destroy();
        tmNettyRemotingClient.destroy();
    }

    @Test
    public void testSendMsgWithResponse() throws Exception {
        ThreadPoolExecutor workingThreads = initMessageExecutor();
        NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads);
        new Thread(() -> {
            nettyRemotingServer.setHandler(new DefaultCoordinator(nettyRemotingServer));
            nettyRemotingServer.setListenPort(8091);
            // set registry
            XID.setIpAddress(NetUtil.getLocalIp());
            XID.setPort(8091);
            // init snowflake for transactionId, branchId
            UUIDGenerator.init(1L);
            nettyRemotingServer.init();
        }).start();
        Thread.sleep(3000);

        String applicationId = "app 1";
        String transactionServiceGroup = "my_test_tx_group";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        tmNettyRemotingClient.init();

        String serverAddress = "0.0.0.0:8091";
        Channel channel = TmNettyRemotingClient.getInstance().getClientChannelManager().acquireChannel(serverAddress);
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
        nettyRemotingServer.destroy();
        tmNettyRemotingClient.destroy();
    }
}
