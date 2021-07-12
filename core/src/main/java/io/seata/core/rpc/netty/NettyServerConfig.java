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

import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.seata.core.constants.ConfigurationKeys;

import static io.seata.common.DefaultValues.DEFAULT_BOSS_THREAD_PREFIX;
import static io.seata.common.DefaultValues.DEFAULT_BOSS_THREAD_SIZE;
import static io.seata.common.DefaultValues.DEFAULT_EXECUTOR_THREAD_PREFIX;
import static io.seata.common.DefaultValues.DEFAULT_NIO_WORKER_THREAD_PREFIX;
import static io.seata.common.DefaultValues.DEFAULT_SHUTDOWN_TIMEOUT_SEC;

/**
 * The type Netty server config.
 *
 * @author slievrly
 */
public class NettyServerConfig extends NettyBaseConfig {

    private int serverSelectorThreads = Integer.parseInt(System.getProperty(
            ConfigurationKeys.TRANSPORT_PREFIX + "serverSelectorThreads", String.valueOf(WORKER_THREAD_SIZE)));
    private int serverSocketSendBufSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.TRANSPORT_PREFIX + "serverSocketSendBufSize", String.valueOf(153600)));
    private int serverSocketResvBufSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.TRANSPORT_PREFIX + "serverSocketResvBufSize", String.valueOf(153600)));
    private int serverWorkerThreads = Integer.parseInt(System.getProperty(
            ConfigurationKeys.TRANSPORT_PREFIX + "serverWorkerThreads", String.valueOf(WORKER_THREAD_SIZE)));
    private int soBackLogSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.TRANSPORT_PREFIX + "soBackLogSize", String.valueOf(1024)));
    private int writeBufferHighWaterMark = Integer.parseInt(System.getProperty(
            ConfigurationKeys.TRANSPORT_PREFIX + "writeBufferHighWaterMark", String.valueOf(67108864)));
    private int writeBufferLowWaterMark = Integer.parseInt(System.getProperty(
            ConfigurationKeys.TRANSPORT_PREFIX + "writeBufferLowWaterMark", String.valueOf(1048576)));
    private static final int DEFAULT_LISTEN_PORT = 8091;
    private static final int RPC_REQUEST_TIMEOUT = 30 * 1000;
    private int serverChannelMaxIdleTimeSeconds = Integer.parseInt(System.getProperty(
            ConfigurationKeys.TRANSPORT_PREFIX + "serverChannelMaxIdleTimeSeconds", String.valueOf(30)));
    private static final String EPOLL_WORKER_THREAD_PREFIX = "NettyServerEPollWorker";
    private static int minServerPoolSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.MIN_SERVER_POOL_SIZE, "50"));
    private static int maxServerPoolSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.MAX_SERVER_POOL_SIZE, "500"));
    private static int maxTaskQueueSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.MAX_TASK_QUEUE_SIZE, "20000"));
    private static int keepAliveTime = Integer.parseInt(System.getProperty(
            ConfigurationKeys.KEEP_ALIVE_TIME, "500"));
    private static int minBranchResultPoolSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.MIN_BRANCH_RESULT_POOL_SIZE, "50"));
    private static int maxBranchResultPoolSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.MAX_BRANCH_RESULT_POOL_SIZE, "500"));
    /**
     * The Server channel clazz.
     */
    public static final Class<? extends ServerChannel> SERVER_CHANNEL_CLAZZ = NettyBaseConfig.SERVER_CHANNEL_CLAZZ;


    /**
     * Gets server selector threads.
     *
     * @return the server selector threads
     */
    public int getServerSelectorThreads() {
        return serverSelectorThreads;
    }

    /**
     * Sets server selector threads.
     *
     * @param serverSelectorThreads the server selector threads
     */
    public void setServerSelectorThreads(int serverSelectorThreads) {
        this.serverSelectorThreads = serverSelectorThreads;
    }

    /**
     * Enable epoll boolean.
     *
     * @return the boolean
     */
    public static boolean enableEpoll() {
        return NettyBaseConfig.SERVER_CHANNEL_CLAZZ.equals(EpollServerSocketChannel.class)
            && Epoll.isAvailable();

    }

    /**
     * Gets server socket send buf size.
     *
     * @return the server socket send buf size
     */
    public int getServerSocketSendBufSize() {
        return serverSocketSendBufSize;
    }

    /**
     * Sets server socket send buf size.
     *
     * @param serverSocketSendBufSize the server socket send buf size
     */
    public void setServerSocketSendBufSize(int serverSocketSendBufSize) {
        this.serverSocketSendBufSize = serverSocketSendBufSize;
    }

    /**
     * Gets server socket resv buf size.
     *
     * @return the server socket resv buf size
     */
    public int getServerSocketResvBufSize() {
        return serverSocketResvBufSize;
    }

    /**
     * Sets server socket resv buf size.
     *
     * @param serverSocketResvBufSize the server socket resv buf size
     */
    public void setServerSocketResvBufSize(int serverSocketResvBufSize) {
        this.serverSocketResvBufSize = serverSocketResvBufSize;
    }

    /**
     * Gets server worker threads.
     *
     * @return the server worker threads
     */
    public int getServerWorkerThreads() {
        return serverWorkerThreads;
    }

    /**
     * Sets server worker threads.
     *
     * @param serverWorkerThreads the server worker threads
     */
    public void setServerWorkerThreads(int serverWorkerThreads) {
        this.serverWorkerThreads = serverWorkerThreads;
    }

    /**
     * Gets so back log size.
     *
     * @return the so back log size
     */
    public int getSoBackLogSize() {
        return soBackLogSize;
    }

    /**
     * Sets so back log size.
     *
     * @param soBackLogSize the so back log size
     */
    public void setSoBackLogSize(int soBackLogSize) {
        this.soBackLogSize = soBackLogSize;
    }

    /**
     * Gets write buffer high water mark.
     *
     * @return the write buffer high water mark
     */
    public int getWriteBufferHighWaterMark() {
        return writeBufferHighWaterMark;
    }

    /**
     * Sets write buffer high water mark.
     *
     * @param writeBufferHighWaterMark the write buffer high water mark
     */
    public void setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        this.writeBufferHighWaterMark = writeBufferHighWaterMark;
    }

    /**
     * Gets write buffer low water mark.
     *
     * @return the write buffer low water mark
     */
    public int getWriteBufferLowWaterMark() {
        return writeBufferLowWaterMark;
    }

    /**
     * Sets write buffer low water mark.
     *
     * @param writeBufferLowWaterMark the write buffer low water mark
     */
    public void setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        this.writeBufferLowWaterMark = writeBufferLowWaterMark;
    }

    /**
     * Gets listen port.
     *
     * @return the listen port
     */
    public int getDefaultListenPort() {
        return DEFAULT_LISTEN_PORT;
    }

    /**
     * Gets channel max read idle seconds.
     *
     * @return the channel max read idle seconds
     */
    public int getChannelMaxReadIdleSeconds() {
        return MAX_READ_IDLE_SECONDS;
    }

    /**
     * Gets server channel max idle time seconds.
     *
     * @return the server channel max idle time seconds
     */
    public int getServerChannelMaxIdleTimeSeconds() {
        return serverChannelMaxIdleTimeSeconds;
    }

    /**
     * Gets rpc request timeout.
     *
     * @return the rpc request timeout
     */
    public static int getRpcRequestTimeout() {
        return RPC_REQUEST_TIMEOUT;
    }

    /**
     * Get boss thread prefix string.
     *
     * @return the string
     */
    public String getBossThreadPrefix() {
        return CONFIG.getConfig(ConfigurationKeys.BOSS_THREAD_PREFIX, DEFAULT_BOSS_THREAD_PREFIX);
    }

    /**
     * Get worker thread prefix string.
     *
     * @return the string
     */
    public String getWorkerThreadPrefix() {
        return CONFIG.getConfig(ConfigurationKeys.WORKER_THREAD_PREFIX,
            enableEpoll() ? EPOLL_WORKER_THREAD_PREFIX : DEFAULT_NIO_WORKER_THREAD_PREFIX);
    }

    /**
     * Get executor thread prefix string.
     *
     * @return the string
     */
    public String getExecutorThreadPrefix() {
        return CONFIG.getConfig(ConfigurationKeys.SERVER_EXECUTOR_THREAD_PREFIX,
            DEFAULT_EXECUTOR_THREAD_PREFIX);
    }

    /**
     * Get boss thread size int.
     *
     * @return the int
     */
    public int getBossThreadSize() {
        return CONFIG.getInt(ConfigurationKeys.BOSS_THREAD_SIZE, DEFAULT_BOSS_THREAD_SIZE);
    }

    /**
     * Get the timeout seconds of shutdown.
     *
     * @return the int
     */
    public int getServerShutdownWaitTime() {
        return CONFIG.getInt(ConfigurationKeys.SHUTDOWN_WAIT, DEFAULT_SHUTDOWN_TIMEOUT_SEC);
    }

    public static int getMinServerPoolSize() {
        return minServerPoolSize;
    }

    public static int getMaxServerPoolSize() {
        return maxServerPoolSize;
    }

    public static int getMaxTaskQueueSize() {
        return maxTaskQueueSize;
    }

    public static int getKeepAliveTime() {
        return keepAliveTime;
    }

    /**
     * Get the min size for branch result thread pool
     *
     * @return the int
     */
    public static int getMinBranchResultPoolSize() {
        return minBranchResultPoolSize;
    }
    /**
     * Get the max size for branch result thread pool
     *
     * @return the int
     */
    public static int getMaxBranchResultPoolSize() {
        return maxBranchResultPoolSize;
    }
}
