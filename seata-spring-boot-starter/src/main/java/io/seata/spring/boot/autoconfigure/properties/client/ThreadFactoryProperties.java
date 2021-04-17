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
package io.seata.spring.boot.autoconfigure.properties.client;

import io.seata.core.rpc.netty.NettyBaseConfig.WorkThreadMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_BOSS_THREAD_PREFIX;
import static io.seata.common.DefaultValues.DEFAULT_BOSS_THREAD_SIZE;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_PIPELINE_THREAD_PREFIX;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_PIPELINE_THREAD_SIZE;
import static io.seata.common.DefaultValues.DEFAULT_EXECUTOR_THREAD_PREFIX;
import static io.seata.common.DefaultValues.DEFAULT_NIO_WORKER_THREAD_PREFIX;
import static io.seata.common.DefaultValues.DEFAULT_SELECTOR_THREAD_PREFIX;
import static io.seata.common.DefaultValues.DEFAULT_SELECTOR_THREAD_SIZE;
import static io.seata.common.DefaultValues.DEFAULT_SERVER_PIPELINE_THREAD_PREFIX;
import static io.seata.common.DefaultValues.DEFAULT_SERVER_PIPELINE_THREAD_SIZE;
import static io.seata.common.DefaultValues.DEFAULT_WORKER_THREAD_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.THREAD_FACTORY_PREFIX_KEBAB_STYLE;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = THREAD_FACTORY_PREFIX_KEBAB_STYLE)
public class ThreadFactoryProperties {
    private String bossThreadPrefix = DEFAULT_BOSS_THREAD_PREFIX;
    private String workerThreadPrefix = DEFAULT_NIO_WORKER_THREAD_PREFIX;
    private String serverExecutorThreadPrefix = DEFAULT_EXECUTOR_THREAD_PREFIX;
    private boolean shareBossWorker = false;
    private String clientSelectorThreadPrefix = DEFAULT_SELECTOR_THREAD_PREFIX;
    private int clientSelectorThreadSize = DEFAULT_SELECTOR_THREAD_SIZE;
    private String clientWorkerThreadPrefix = DEFAULT_WORKER_THREAD_PREFIX;
    /**
     * netty boss thread size
     */
    private int bossThreadSize = DEFAULT_BOSS_THREAD_SIZE;
    /**
     * auto default pin or 8
     */
    private String workerThreadSize = WorkThreadMode.Default.name();
    private String serverPipelineThreadPrefix = DEFAULT_SERVER_PIPELINE_THREAD_PREFIX;
    /**
     * server pipeline thread size, mainly deal with non-business logic such as codec etc
     */
    private int serverPipelineThreadSize = DEFAULT_SERVER_PIPELINE_THREAD_SIZE;
    private String clientPipelineThreadPrefix = DEFAULT_CLIENT_PIPELINE_THREAD_PREFIX;
    /**
     * client pipeline thread size, mainly deal with non-business logic such as codec etc
     */
    private int clientPipelineThreadSize = DEFAULT_CLIENT_PIPELINE_THREAD_SIZE;

    public String getBossThreadPrefix() {
        return bossThreadPrefix;
    }

    public ThreadFactoryProperties setBossThreadPrefix(String bossThreadPrefix) {
        this.bossThreadPrefix = bossThreadPrefix;
        return this;
    }

    public String getWorkerThreadPrefix() {
        return workerThreadPrefix;
    }

    public ThreadFactoryProperties setWorkerThreadPrefix(String workerThreadPrefix) {
        this.workerThreadPrefix = workerThreadPrefix;
        return this;
    }

    public String getServerExecutorThreadPrefix() {
        return serverExecutorThreadPrefix;
    }

    public ThreadFactoryProperties setServerExecutorThreadPrefix(String serverExecutorThreadPrefix) {
        this.serverExecutorThreadPrefix = serverExecutorThreadPrefix;
        return this;
    }

    public boolean isShareBossWorker() {
        return shareBossWorker;
    }

    public ThreadFactoryProperties setShareBossWorker(boolean shareBossWorker) {
        this.shareBossWorker = shareBossWorker;
        return this;
    }

    public String getClientSelectorThreadPrefix() {
        return clientSelectorThreadPrefix;
    }

    public ThreadFactoryProperties setClientSelectorThreadPrefix(String clientSelectorThreadPrefix) {
        this.clientSelectorThreadPrefix = clientSelectorThreadPrefix;
        return this;
    }

    public String getClientWorkerThreadPrefix() {
        return clientWorkerThreadPrefix;
    }

    public ThreadFactoryProperties setClientWorkerThreadPrefix(String clientWorkerThreadPrefix) {
        this.clientWorkerThreadPrefix = clientWorkerThreadPrefix;
        return this;
    }

    public int getClientSelectorThreadSize() {
        return clientSelectorThreadSize;
    }

    public ThreadFactoryProperties setClientSelectorThreadSize(int clientSelectorThreadSize) {
        this.clientSelectorThreadSize = clientSelectorThreadSize;
        return this;
    }

    public int getBossThreadSize() {
        return bossThreadSize;
    }

    public ThreadFactoryProperties setBossThreadSize(int bossThreadSize) {
        this.bossThreadSize = bossThreadSize;
        return this;
    }

    public String getWorkerThreadSize() {
        return workerThreadSize;
    }

    public ThreadFactoryProperties setWorkerThreadSize(String workerThreadSize) {
        this.workerThreadSize = workerThreadSize;
        return this;
    }

    public String getServerPipelineThreadPrefix() {
        return serverPipelineThreadPrefix;
    }

    public ThreadFactoryProperties setServerPipelineThreadPrefix(String serverPipelineThreadPrefix) {
        this.serverPipelineThreadPrefix = serverPipelineThreadPrefix;
        return this;
    }

    public int getServerPipelineThreadSize() {
        return serverPipelineThreadSize;
    }

    public ThreadFactoryProperties setServerPipelineThreadSize(int serverPipelineThreadSize) {
        this.serverPipelineThreadSize = serverPipelineThreadSize;
        return this;
    }

    public String getClientPipelineThreadPrefix() {
        return clientPipelineThreadPrefix;
    }

    public ThreadFactoryProperties setClientPipelineThreadPrefix(String clientPipelineThreadPrefix) {
        this.clientPipelineThreadPrefix = clientPipelineThreadPrefix;
        return this;
    }

    public int getClientPipelineThreadSize() {
        return clientPipelineThreadSize;
    }

    public ThreadFactoryProperties setClientPipelineThreadSize(int clientPipelineThreadSize) {
        this.clientPipelineThreadSize = clientPipelineThreadSize;
        return this;
    }
}
