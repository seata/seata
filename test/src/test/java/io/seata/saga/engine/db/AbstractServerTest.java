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
package io.seata.saga.engine.db;

import io.seata.common.XID;
import io.seata.common.util.NetUtil;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.rpc.ShutdownHook;
import io.seata.core.rpc.netty.NettyRemotingServer;
import io.seata.server.ParameterParser;
import io.seata.tc.UUIDGenerator;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.metrics.MetricsManager;
import io.seata.tc.session.SessionHolder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Abstract Server Test
 *
 * @author lorne.cl
 */
public abstract class AbstractServerTest {


    private static NettyRemotingServer nettyServer;
    private static final ThreadPoolExecutor workingThreads = new ThreadPoolExecutor(100, 500, 500, TimeUnit.SECONDS,
            new LinkedBlockingQueue(20000), new ThreadPoolExecutor.CallerRunsPolicy());

    protected static void startSeataServer() throws InterruptedException {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File("sessionStore/root.data");
                    if(file.exists()){
                        file.delete();
                    }

                    ParameterParser parameterParser = new ParameterParser(new String[]{});

                    //initialize the metrics
                    MetricsManager.get().init();

                    System.setProperty(ConfigurationKeys.STORE_MODE, parameterParser.getStoreMode());

                    nettyServer = new NettyRemotingServer(workingThreads);
                    //server port
                    nettyServer.setListenPort(parameterParser.getPort());
                    UUIDGenerator.init(parameterParser.getServerNode());
                    //log store mode : file、db
                    SessionHolder.init(parameterParser.getStoreMode());

                    DefaultCoordinator coordinator = new DefaultCoordinator(nettyServer);
                    coordinator.init();
                    nettyServer.setHandler(coordinator);
                    // register ShutdownHook
                    ShutdownHook.getInstance().addDisposable(coordinator);

                    //127.0.0.1 and 0.0.0.0 are not valid here.
                    if (NetUtil.isValidIp(parameterParser.getHost(), false)) {
                        XID.setIpAddress(parameterParser.getHost());
                    } else {
                        XID.setIpAddress(NetUtil.getLocalIp());
                    }
                    XID.setPort(nettyServer.getListenPort());

                    nettyServer.init();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        })).start();
        Thread.sleep(5000);
    }

    protected static final void stopSeataServer() throws InterruptedException {
        if(nettyServer != null){
            nettyServer.destroy();
            Thread.sleep(5000);
        }
    }

}