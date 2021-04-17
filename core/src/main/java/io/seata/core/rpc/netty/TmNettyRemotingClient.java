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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.netty.channel.Channel;
import io.seata.common.exception.FrameworkException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.thread.RejectedPolicies;
import io.seata.common.util.NetUtil;
import io.seata.core.auth.AuthSigner;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.rpc.processor.client.ClientHeartbeatProcessor;
import io.seata.core.rpc.processor.client.ClientOnResponseProcessor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.core.constants.ConfigurationKeys.EXTRA_DATA_KV_CHAR;
import static io.seata.core.constants.ConfigurationKeys.EXTRA_DATA_SPLIT_CHAR;
import static io.seata.core.constants.ConfigurationKeys.SEATA_ACCESS_KEY;
import static io.seata.core.constants.ConfigurationKeys.SEATA_SECRET_KEY;

/**
 * The rm netty client.
 *
 * @author slievrly
 * @author zhaojun
 * @author zhangchenghui.dev@gmail.com
 */

public final class TmNettyRemotingClient extends AbstractNettyRemotingClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TmNettyRemotingClient.class);
    private static volatile TmNettyRemotingClient instance;
    private static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;
    private static final int MAX_QUEUE_SIZE = 2000;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private String applicationId;
    private String transactionServiceGroup;
    private final AuthSigner signer;
    private String accessKey;
    private String secretKey;


    private TmNettyRemotingClient(NettyClientConfig nettyClientConfig,
                                  ThreadPoolExecutor messageExecutor) {
        super(nettyClientConfig, messageExecutor, NettyPoolKey.TransactionRole.TMROLE);
        this.signer = EnhancedServiceLoader.load(AuthSigner.class);
    }

    /**
     * Gets instance.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @return the instance
     */
    public static TmNettyRemotingClient getInstance(String applicationId, String transactionServiceGroup) {
        return getInstance(applicationId, transactionServiceGroup, null, null);
    }

    /**
     * Gets instance.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @param accessKey               the access key
     * @param secretKey               the secret key
     * @return the instance
     */
    public static TmNettyRemotingClient getInstance(String applicationId, String transactionServiceGroup, String accessKey, String secretKey) {
        TmNettyRemotingClient tmRpcClient = getInstance();
        tmRpcClient.setApplicationId(applicationId);
        tmRpcClient.setTransactionServiceGroup(transactionServiceGroup);
        tmRpcClient.setAccessKey(accessKey);
        tmRpcClient.setSecretKey(secretKey);
        return tmRpcClient;
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static TmNettyRemotingClient getInstance() {
        if (instance == null) {
            synchronized (TmNettyRemotingClient.class) {
                if (instance == null) {
                    NettyClientConfig nettyClientConfig = new NettyClientConfig();
                    final ThreadPoolExecutor messageExecutor = new ThreadPoolExecutor(
                            nettyClientConfig.getClientWorkerThreads(), nettyClientConfig.getClientWorkerThreads(),
                            KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
                            new NamedThreadFactory(nettyClientConfig.getTmDispatchThreadPrefix(),
                                    nettyClientConfig.getClientWorkerThreads()),
                            RejectedPolicies.runsOldestTaskPolicy());
                    instance = new TmNettyRemotingClient(nettyClientConfig, messageExecutor);
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

    /**
     * Sets access key.
     *
     * @param accessKey the access key
     */
    protected void setAccessKey(String accessKey) {
        if (null != accessKey) {
            this.accessKey = accessKey;
            return;
        }
        this.accessKey = System.getProperty(SEATA_ACCESS_KEY);
    }

    /**
     * Sets secret key.
     *
     * @param secretKey the secret key
     */
    protected void setSecretKey(String secretKey) {
        if (null != secretKey) {
            this.secretKey = secretKey;
            return;
        }
        this.secretKey = System.getProperty(SEATA_SECRET_KEY);
    }

    @Override
    public void init() {
        // registry processor
        registerProcessor();
        if (initialized.compareAndSet(false, true)) {
            super.init();
        }
    }

    @Override
    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    @Override
    public void onRegisterMsgSuccess(String serverAddress, Channel channel, Object response,
                                     AbstractMessage requestMessage) {
        RegisterTMRequest registerTMRequest = (RegisterTMRequest) requestMessage;
        RegisterTMResponse registerTMResponse = (RegisterTMResponse) response;
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("register TM success. client version:{}, server version:{},channel:{}", registerTMRequest.getVersion(), registerTMResponse.getVersion(), channel);
        }
        getClientChannelManager().registerChannel(serverAddress, channel);
    }

    @Override
    public void onRegisterMsgFail(String serverAddress, Channel channel, Object response,
                                  AbstractMessage requestMessage) {
        RegisterTMRequest registerTMRequest = (RegisterTMRequest) requestMessage;
        RegisterTMResponse registerTMResponse = (RegisterTMResponse) response;
        String errMsg = String.format(
                "register TM failed. client version: %s,server version: %s, errorMsg: %s, " + "channel: %s", registerTMRequest.getVersion(), registerTMResponse.getVersion(), registerTMResponse.getMsg(), channel);
        throw new FrameworkException(errMsg);
    }

    @Override
    public void destroy() {
        super.destroy();
        initialized.getAndSet(false);
        instance = null;
    }

    @Override
    protected Function<String, NettyPoolKey> getPoolKeyFunction() {
        return severAddress -> {
            RegisterTMRequest message = new RegisterTMRequest(applicationId, transactionServiceGroup, getExtraData());
            return new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, severAddress, message);
        };
    }

    private void registerProcessor() {
        // 1.registry TC response processor
        ClientOnResponseProcessor onResponseProcessor =
                new ClientOnResponseProcessor(mergeMsgMap, super.getFutures(), getTransactionMessageHandler());
        super.registerProcessor(MessageType.TYPE_SEATA_MERGE_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_GLOBAL_BEGIN_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_GLOBAL_COMMIT_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_GLOBAL_REPORT_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_GLOBAL_ROLLBACK_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_GLOBAL_STATUS_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_REG_CLT_RESULT, onResponseProcessor, null);
        // 2.registry heartbeat message processor
        ClientHeartbeatProcessor clientHeartbeatProcessor = new ClientHeartbeatProcessor();
        super.registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, clientHeartbeatProcessor, null);
    }

    private String getExtraData() {
        String ip = NetUtil.getLocalIp();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String digestSource;
        if (StringUtils.isEmpty(ip)) {
            digestSource = transactionServiceGroup + ",127.0.0.1," + timestamp;
        } else {
            digestSource = transactionServiceGroup + "," + ip + "," + timestamp;
        }
        String digest = signer.sign(digestSource, secretKey);
        StringBuilder sb = new StringBuilder();
        sb.append(RegisterTMRequest.UDATA_AK).append(EXTRA_DATA_KV_CHAR).append(accessKey).append(EXTRA_DATA_SPLIT_CHAR);
        sb.append(RegisterTMRequest.UDATA_DIGEST).append(EXTRA_DATA_KV_CHAR).append(digest).append(EXTRA_DATA_SPLIT_CHAR);
        sb.append(RegisterTMRequest.UDATA_TIMESTAMP).append(EXTRA_DATA_KV_CHAR).append(timestamp).append(EXTRA_DATA_SPLIT_CHAR);
        return sb.toString();
    }
}
