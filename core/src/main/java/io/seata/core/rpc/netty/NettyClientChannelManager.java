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
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.discovery.registry.RegistryFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Netty client pool manager.
 *
 * @author slievrly
 * @author zhaojun
 */
class NettyClientChannelManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClientChannelManager.class);
    
    private final ConcurrentMap<String, Object> channelLocks = new ConcurrentHashMap<>();
    
    private final ConcurrentMap<String, NettyPoolKey> poolKeyMap = new ConcurrentHashMap<>();
    
    private final ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<>();
    
    private final GenericKeyedObjectPool<NettyPoolKey, Channel> nettyClientKeyPool;
    
    private final Function<String, NettyPoolKey> poolKeyFunction;

    private final AbstractRpcRemotingClient rpcRemotingClient;
    
    NettyClientChannelManager(final NettyPoolableFactory keyPoolableFactory,
                              final Function<String, NettyPoolKey> poolKeyFunction,
                              final AbstractRpcRemotingClient rpcRemotingClient,
                              final NettyClientConfig clientConfig) {
        nettyClientKeyPool = new GenericKeyedObjectPool<>(keyPoolableFactory);
        nettyClientKeyPool.setConfig(getNettyPoolConfig(clientConfig));
        this.poolKeyFunction = poolKeyFunction;
        this.rpcRemotingClient = rpcRemotingClient;
    }
    
    private GenericKeyedObjectPool.Config getNettyPoolConfig(final NettyClientConfig clientConfig) {
        GenericKeyedObjectPool.Config poolConfig = new GenericKeyedObjectPool.Config();
        poolConfig.maxActive = clientConfig.getMaxPoolActive();
        poolConfig.minIdle = clientConfig.getMinPoolIdle();
        poolConfig.maxWait = clientConfig.getMaxAcquireConnMills();
        poolConfig.testOnBorrow = clientConfig.isPoolTestBorrow();
        poolConfig.testOnReturn = clientConfig.isPoolTestReturn();
        poolConfig.lifo = clientConfig.isPoolLifo();
        return poolConfig;
    }
    
    /**
     * Get all channels registered on current Rpc Client.
     *
     * @return channels
     */
    Map<String, Channel> getChannels() {
        return new HashMap<>(channels);
    }

    /**
     * Get all registered channels' count
     *
     * @return channels' count
     */
    int channelCount() {
        return channels.size();
    }
    
    /**
     * Acquire netty client channel connected to remote server.
     *
     * @param serverAddress server address
     * @return netty channel
     */
    Channel acquireChannel(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (channelToServer != null) {
            channelToServer = getExistAliveChannel(channelToServer, serverAddress);
            if (null != channelToServer) {
                return channelToServer;
            }
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("will connect to " + serverAddress);
        }
        channelLocks.putIfAbsent(serverAddress, new Object());
        synchronized (channelLocks.get(serverAddress)) {
            return doConnect(serverAddress);
        }
    }
    
    /**
     * Release channel if necessary.
     *
     * @param channel channel
     * @param serverAddress server address
     */
    void releaseChannel(Channel channel, String serverAddress) {
        if (null == channel || null == serverAddress) { return; }
        try {
            synchronized (channelLocks.get(serverAddress)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("release channel {}", channel);
                }
                NettyPoolKey poolKey = poolKeyMap.get(serverAddress);
                boolean channelRemoved = channels.remove(serverAddress, channel);
                nettyClientKeyPool.invalidateObject(poolKey, channel);
                if (channelRemoved) {
                    poolKeyMap.remove(serverAddress, poolKey);
                    channelLocks.remove(serverAddress);
                }
            }
        } catch (Exception exx) {
            LOGGER.error("release channel {} error:{}", channel, exx.getMessage(), exx);
        }
    }
    
    /**
     * Reconnect to remote server of current transaction service group.
     *
     * @param transactionServiceGroup transaction service group
     */
    void reconnect(String transactionServiceGroup) {
        List<String> availList = null;
        try {
            availList = getAvailServerList(transactionServiceGroup);
        } catch (Exception e) {
            LOGGER.error("Failed to get available servers: {}", e.getMessage(), e);
            return;
        }
        if (CollectionUtils.isEmpty(availList)) {
            String serviceGroup = RegistryFactory.getInstance()
                                                 .getServiceGroup(transactionServiceGroup);
            LOGGER.error("no available service '{}' found, please make sure registry config correct", serviceGroup);
            return;
        }
        for (String serverAddress : availList) {
            try {
                acquireChannel(serverAddress);
            } catch (Exception e) {
                LOGGER.error("{} can not connect to {} cause:{}",FrameworkErrorCode.NetConnect.getErrCode(), serverAddress, e.getMessage(), e);
            }
        }
    }
    
    private Channel doConnect(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (channelToServer != null && channelToServer.isActive()) {
            return channelToServer;
        }
        Channel channelFromPool;
        NettyPoolKey poolKey;
        try {
            NettyPoolKey currentPoolKey = poolKeyFunction.apply(serverAddress);
            NettyPoolKey previousPoolKey = poolKeyMap.putIfAbsent(serverAddress, currentPoolKey);
            if (null != previousPoolKey && previousPoolKey.getMessage() instanceof RegisterRMRequest) {
                RegisterRMRequest registerRMRequest = (RegisterRMRequest) currentPoolKey.getMessage();
                ((RegisterRMRequest) previousPoolKey.getMessage()).setResourceIds(registerRMRequest.getResourceIds());
            }
            poolKey = previousPoolKey != null ? previousPoolKey : currentPoolKey;
            channelFromPool = nettyClientKeyPool.borrowObject(poolKey);
            channels.put(serverAddress, channelFromPool);
        } catch (Exception exx) {
            LOGGER.error("{} register RM failed.",FrameworkErrorCode.RegisterRM.getErrCode(), exx);
            throw new FrameworkException("can not register RM,err:" + exx.getMessage());
        }
        try {
            doRegister(poolKey, channelFromPool);
        } catch (Exception exx) {
            releaseChannel(channelFromPool, serverAddress);
            if (exx instanceof FrameworkException) {
                throw exx;
            } else {
                throw new FrameworkException("can not register RM,err: " + exx.getMessage());
            }
        }
        return channelFromPool;
    }

    private void doRegister(NettyPoolKey poolKey, Channel channelToServer) {
        if (rpcRemotingClient == null) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            if (null == poolKey.getMessage()) {
                throw new FrameworkException("register msg is null, role:" + poolKey.getTransactionRole().name());
            }
            Object response = rpcRemotingClient.sendAsyncRequestWithResponse(channelToServer, poolKey.getMessage());
            if (!isResponseSuccess(response, poolKey.getTransactionRole())) {
                rpcRemotingClient.onRegisterMsgFail(poolKey.getAddress(), channelToServer, response,
                        poolKey.getMessage());
            } else {
                rpcRemotingClient.onRegisterMsgSuccess(poolKey.getAddress(), channelToServer, response,
                        poolKey.getMessage());
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "register success, cost " + (System.currentTimeMillis() - start) + " ms, version:"
                                + getVersion(response, poolKey.getTransactionRole()) + ",role:" + poolKey.getTransactionRole().name()
                                + ",channel:" + channelToServer);
            }
        } catch (Exception exx) {
            LOGGER.error("{} register RM failed.", FrameworkErrorCode.RegisterRM.getErrCode(), exx);
            throw new FrameworkException(
                    "register error,role:" + poolKey.getTransactionRole().name() + ",err:" + exx.getMessage());
        }
    }

    private boolean isResponseSuccess(Object response, NettyPoolKey.TransactionRole transactionRole) {
        if (null == response) {
            return false;
        }
        if (transactionRole.equals(NettyPoolKey.TransactionRole.TMROLE)) {
            if (!(response instanceof RegisterTMResponse)) {
                return false;
            }
            return ((RegisterTMResponse) response).isIdentified();
        } else if (transactionRole.equals(NettyPoolKey.TransactionRole.RMROLE)) {
            if (!(response instanceof RegisterRMResponse)) {
                return false;
            }
            return ((RegisterRMResponse) response).isIdentified();
        }
        return false;
    }

    private String getVersion(Object response, NettyPoolKey.TransactionRole transactionRole) {
        if (transactionRole.equals(NettyPoolKey.TransactionRole.TMROLE)) {
            return ((RegisterTMResponse) response).getVersion();
        } else {
            return ((RegisterRMResponse) response).getVersion();
        }
    }
    
    private List<String> getAvailServerList(String transactionServiceGroup) throws Exception {
        List<InetSocketAddress> availInetSocketAddressList = RegistryFactory.getInstance()
                                                                            .lookup(transactionServiceGroup);
        if (CollectionUtils.isEmpty(availInetSocketAddressList)) {
            return Collections.emptyList();
        }

        return availInetSocketAddressList.stream()
                                         .map(NetUtil::toStringAddress)
                                         .collect(Collectors.toList());
    }
    
    private Channel getExistAliveChannel(Channel rmChannel, String serverAddress) {
        if (rmChannel.isActive()) {
            return rmChannel;
        } else {
            int i = 0;
            for (; i < NettyClientConfig.getMaxCheckAliveRetry(); i++) {
                try {
                    Thread.sleep(NettyClientConfig.getCheckAliveInternal());
                } catch (InterruptedException exx) {
                    LOGGER.error(exx.getMessage());
                }
                rmChannel = channels.get(serverAddress);
                if (null != rmChannel && rmChannel.isActive()) {
                    return rmChannel;
                }
            }
            if (i == NettyClientConfig.getMaxCheckAliveRetry()) {
                LOGGER.warn("channel {} is not active after long wait, close it.", rmChannel);
                releaseChannel(rmChannel, serverAddress);
                return null;
            }
        }
        return null;
    }
}

