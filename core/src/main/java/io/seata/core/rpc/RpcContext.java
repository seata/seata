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
package io.seata.core.rpc;

import io.netty.channel.Channel;
import io.seata.common.util.StringUtils;
import io.seata.core.rpc.netty.NettyPoolKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The type rpc context.
 *
 * @author slievrly
 */
public class RpcContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcContext.class);

    private NettyPoolKey.TransactionRole clientRole;

    private String version;

    private String applicationId;

    private String transactionServiceGroup;

    private String clientId;

    private Channel channel;

    private Set<String> resourceSets;

    /**
     * id
     */
    private ConcurrentMap<Channel, RpcContext> clientIDHolderMap;

    /**
     * tm
     */
    private ConcurrentMap<Integer, RpcContext> clientTMHolderMap;

    /**
     * dbkeyRm
     */
    private ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>> clientRMHolderMap;

    /**
     * Release.
     */
    public void release() {
        Integer clientPort = ChannelUtil.getClientPortFromChannel(channel);
        if (clientIDHolderMap != null) {
            clientIDHolderMap = null;
        }
        if (clientRole == NettyPoolKey.TransactionRole.TMROLE && clientTMHolderMap != null) {
            clientTMHolderMap.remove(clientPort);
            clientTMHolderMap = null;
        }
        if (clientRole == NettyPoolKey.TransactionRole.RMROLE && clientRMHolderMap != null) {
            for (Map<Integer, RpcContext> portMap : clientRMHolderMap.values()) {
                portMap.remove(clientPort);
            }
            clientRMHolderMap = null;
        }
        if (null != resourceSets) {
            resourceSets.clear();
        }
    }

    /**
     * Hold in client channels.
     *
     * @param clientTMHolderMap the client tm holder map
     */
    public void holdInClientChannels(ConcurrentMap<Integer, RpcContext> clientTMHolderMap) {
        if (this.clientTMHolderMap != null) {
            throw new IllegalStateException();
        }
        this.clientTMHolderMap = clientTMHolderMap;
        Integer clientPort = ChannelUtil.getClientPortFromChannel(channel);
        this.clientTMHolderMap.put(clientPort, this);
    }

    /**
     * Hold in identified channels.
     *
     * @param clientIDHolderMap the client id holder map
     */
    public void holdInIdentifiedChannels(ConcurrentMap<Channel, RpcContext> clientIDHolderMap) {
        if (this.clientIDHolderMap != null) {
            throw new IllegalStateException();
        }
        this.clientIDHolderMap = clientIDHolderMap;
        this.clientIDHolderMap.put(channel, this);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The number of identified channel is: {}", clientIDHolderMap.size());
        }
    }

    /**
     * Hold in resource manager channels.
     *
     * @param resourceId the resource id
     * @param portMap    the client rm holder map
     */
    public void holdInResourceManagerChannels(String resourceId, ConcurrentMap<Integer, RpcContext> portMap) {
        if (null == this.clientRMHolderMap) {
            this.clientRMHolderMap = new ConcurrentHashMap<String, ConcurrentMap<Integer, RpcContext>>();
        }
        Integer clientPort = ChannelUtil.getClientPortFromChannel(channel);
        portMap.put(clientPort, this);
        this.clientRMHolderMap.put(resourceId, portMap);
    }

    /**
     * Hold in resource manager channels.
     *
     * @param resourceId the resource id
     * @param clientPort the client port
     */
    public void holdInResourceManagerChannels(String resourceId, Integer clientPort) {
        if (null == this.clientRMHolderMap) {
            this.clientRMHolderMap = new ConcurrentHashMap<String, ConcurrentMap<Integer, RpcContext>>();
        }
        clientRMHolderMap.putIfAbsent(resourceId, new ConcurrentHashMap<Integer, RpcContext>());
        ConcurrentMap<Integer, RpcContext> portMap = clientRMHolderMap.get(resourceId);
        portMap.put(clientPort, this);
    }

    /**
     * Gets get client rm holder map.
     *
     * @return the get client rm holder map
     */
    public ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>> getClientRMHolderMap() {
        return clientRMHolderMap;
    }

    /**
     * Gets port map.
     *
     * @param resourceId the resource id
     * @return the port map
     */
    public Map<Integer, RpcContext> getPortMap(String resourceId) {
        return clientRMHolderMap.get(resourceId);
    }

    /**
     * Gets get client id.
     *
     * @return the get client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets get channel.
     *
     * @return the get channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Sets set channel.
     *
     * @param channel the channel
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * Gets get application id.
     *
     * @return the get application id
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Sets set application id.
     *
     * @param applicationId the application id
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Gets get transaction service group.
     *
     * @return the get transaction service group
     */
    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    /**
     * Sets set transaction service group.
     *
     * @param transactionServiceGroup the transaction service group
     */
    public void setTransactionServiceGroup(String transactionServiceGroup) {
        this.transactionServiceGroup = transactionServiceGroup;
    }

    /**
     * Gets get client role.
     *
     * @return the get client role
     */
    public NettyPoolKey.TransactionRole getClientRole() {
        return clientRole;
    }

    /**
     * Sets set client role.
     *
     * @param clientRole the client role
     */
    public void setClientRole(NettyPoolKey.TransactionRole clientRole) {
        this.clientRole = clientRole;
    }

    /**
     * Gets get version.
     *
     * @return the get version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets set version.
     *
     * @param version the version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets get resource sets.
     *
     * @return the get resource sets
     */
    public Set<String> getResourceSets() {
        return resourceSets;
    }

    /**
     * Sets set resource sets.
     *
     * @param resourceSets the resource sets
     */
    public void setResourceSets(Set<String> resourceSets) {
        this.resourceSets = resourceSets;
    }

    /**
     * Add resource.
     *
     * @param resource the resource
     */
    public void addResource(String resource) {
        if (StringUtils.isBlank(resource)) {
            return;
        }
        if (null == resourceSets) {
            this.resourceSets = new HashSet<String>();
        }
        this.resourceSets.add(resource);
    }

    /**
     * Add resources.
     *
     * @param resources the resources
     */
    public void addResources(Set<String> resources) {
        if (null == resources) { return; }
        if (null == resourceSets) {
            this.resourceSets = new HashSet<String>();
        }
        this.resourceSets.addAll(resources);
    }

    /**
     * Sets client id.
     *
     * @param clientId the client id
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String toString() {
        return "RpcContext{" +
            "applicationId='" + applicationId + '\'' +
            ", transactionServiceGroup='" + transactionServiceGroup + '\'' +
            ", clientId='" + clientId + '\'' +
            ", channel=" + channel +
            ", resourceSets=" + resourceSets +
            '}';
    }
}
