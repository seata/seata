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
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.concurrent.EventExecutorGroup;
import io.seata.common.exception.FrameworkException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.thread.RejectedPolicies;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * The type Rpc client.
 *
 * @author slievrly
 * @author zhaojun
 */
@Sharable
public final class TmRpcClient extends AbstractRpcRemotingClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TmRpcClient.class);
    private static volatile TmRpcClient instance;
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();
    private static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;
    private static final int MAX_QUEUE_SIZE = 2000;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private String applicationId;
    private String transactionServiceGroup;
    
    /**
     * The constant enableDegrade.
     */
    public static boolean enableDegrade = false;

    private TmRpcClient(NettyClientConfig nettyClientConfig,
                        EventExecutorGroup eventExecutorGroup,
                        ThreadPoolExecutor messageExecutor) {
        super(nettyClientConfig, eventExecutorGroup, messageExecutor, NettyPoolKey.TransactionRole.TMROLE);
    }

    /**
     * Gets instance.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @return the instance
     */
    public static TmRpcClient getInstance(String applicationId, String transactionServiceGroup) {
        TmRpcClient tmRpcClient = getInstance();
        tmRpcClient.setApplicationId(applicationId);
        tmRpcClient.setTransactionServiceGroup(transactionServiceGroup);
        return tmRpcClient;
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static TmRpcClient getInstance() {
        if (null == instance) {
            synchronized (TmRpcClient.class) {
                if (null == instance) {
                    NettyClientConfig nettyClientConfig = new NettyClientConfig();
                    final ThreadPoolExecutor messageExecutor = new ThreadPoolExecutor(
                        nettyClientConfig.getClientWorkerThreads(), nettyClientConfig.getClientWorkerThreads(),
                        KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
                        new NamedThreadFactory(nettyClientConfig.getTmDispatchThreadPrefix(),
                            nettyClientConfig.getClientWorkerThreads()),
                        RejectedPolicies.runsOldestTaskPolicy());
                    instance = new TmRpcClient(nettyClientConfig, null, messageExecutor);
                }
            }
        }
        return instance;
    }
    
    /**
     * Sets application id.
     *
     * @param applicationId the application id
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
    
    /**
     * Sets transaction service group.
     *
     * @param transactionServiceGroup the transaction service group
     */
    public void setTransactionServiceGroup(String transactionServiceGroup) {
        this.transactionServiceGroup = transactionServiceGroup;
    }
    
    @Override
    public void init() {
        if (initialized.compareAndSet(false, true)) {
            enableDegrade = CONFIG.getBoolean(ConfigurationKeys.SERVICE_PREFIX + ConfigurationKeys.ENABLE_DEGRADE_POSTFIX);
            super.init();
        }
    }
    
    @Override
    public void destroy() {
        super.destroy();
        initialized.getAndSet(false);
        instance = null;
    }
    
    @Override
    protected Function<String, NettyPoolKey> getPoolKeyFunction() {
        return (severAddress) -> {
            RegisterTMRequest message = new RegisterTMRequest(applicationId, transactionServiceGroup);
            return new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, severAddress, message);
        };
    }
    
    @Override
    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }
    
    @Override
    public void onRegisterMsgSuccess(String serverAddress, Channel channel, Object response,
                                     AbstractMessage requestMessage) {
    }

    @Override
    public void onRegisterMsgFail(String serverAddress, Channel channel, Object response,
                                  AbstractMessage requestMessage) {
        if (response instanceof RegisterTMResponse && LOGGER.isInfoEnabled()) {
            LOGGER.info("register client failed, server version:"
                + ((RegisterTMResponse)response).getVersion());
        }
        throw new FrameworkException("register client app failed.");
    }
}
