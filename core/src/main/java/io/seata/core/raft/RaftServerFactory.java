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
package io.seata.core.raft;

import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.StoreMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.seata.common.DefaultValues.DEFAULT_RAFT_PORT_INTERVAL;
import static io.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;
import static io.seata.common.DefaultValues.SEATA_RAFT_GROUP;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_APPLY_BATCH;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_DISRUPTOR_BUFFER_SIZE;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_ELECTION_TIMEOUT_MS;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_MAX_APPEND_BUFFER_SIZE;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_MAX_REPLICATOR_INFLIGHT_MSGS;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_SNAPSHOT_INTERVAL;
import static io.seata.core.raft.AbstractRaftServer.RAFT_TAG;
import static java.io.File.separator;

/**
 * @author funkye
 */
public class RaftServerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftServerFactory.class);

    private AbstractRaftServer raftServer;

    private AbstractRaftStateMachine stateMachine;

    private Boolean raftMode = false;

    private io.seata.config.Configuration config = ConfigurationFactory.getInstance();

    public static RaftServerFactory getInstance() {
        return SingletonHandler.instance;
    }

    public void init(String host, int port) {
        String initConfStr = config.getConfig(ConfigurationKeys.SERVER_RAFT_CLUSTER);
        if (StringUtils.isBlank(initConfStr)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("initialize SofaJRaft fail , server.raft.cluster is null");
            }
            return;
        }
        String mode = config.getConfig(ConfigurationKeys.STORE_MODE);
        StoreMode storeMode = StoreMode.get(mode);
        if (storeMode.equals(StoreMode.RAFT)) {
            raftMode = true;
        }
        String colon = ":";
        String serverIdStr = new StringBuilder(host).append(colon).append(port - DEFAULT_RAFT_PORT_INTERVAL).toString();
        final String dataPath = config.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR)
            + separator + serverIdStr.split(colon)[1];
        final NodeOptions nodeOptions = new NodeOptions();
        // set the election timeout to 1 second
        nodeOptions.setElectionTimeoutMs(DEFAULT_RAFT_PORT_INTERVAL);
        // enable the CLI service.
        nodeOptions.setDisableCli(false);
        // snapshot should be made every 30 seconds
        Integer snapshotInterval = config.getInt(SERVER_RAFT_SNAPSHOT_INTERVAL, 60 * 10);
        nodeOptions.setSnapshotIntervalSecs(snapshotInterval);
        RaftOptions raftOptions = new RaftOptions();
        raftOptions
            .setApplyBatch(config.getInt(SERVER_RAFT_APPLY_BATCH, raftOptions.getApplyBatch()));
        raftOptions.setMaxAppendBufferSize(
            config.getInt(SERVER_RAFT_MAX_APPEND_BUFFER_SIZE, raftOptions.getMaxAppendBufferSize()));
        raftOptions.setDisruptorBufferSize(
            config.getInt(SERVER_RAFT_DISRUPTOR_BUFFER_SIZE, raftOptions.getDisruptorBufferSize()));
        raftOptions.setMaxReplicatorInflightMsgs(config.getInt(
            SERVER_RAFT_MAX_REPLICATOR_INFLIGHT_MSGS, raftOptions.getMaxReplicatorInflightMsgs()));
        nodeOptions.setRaftOptions(raftOptions);
        nodeOptions.setElectionTimeoutMs(
            config.getInt(SERVER_RAFT_ELECTION_TIMEOUT_MS, nodeOptions.getElectionTimeoutMs()));
        // analytic parameter
        final PeerId serverId = new PeerId();
        if (!serverId.parse(serverIdStr)) {
            throw new IllegalArgumentException("fail to parse serverId:" + serverIdStr);
        }
        final Configuration initConf = new Configuration();
        if (!initConf.parse(initConfStr)) {
            throw new IllegalArgumentException("fail to parse initConf:" + initConfStr);
        }
        // set up the initial cluster configuration
        nodeOptions.setInitialConf(initConf);
        raftServer = EnhancedServiceLoader.load(AbstractRaftServer.class, RAFT_TAG,
            new Object[] {dataPath, SEATA_RAFT_GROUP, serverId, nodeOptions});
        stateMachine = raftServer.getAbstractRaftStateMachine();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("initialize SofaJRaft");
        }
        LOGGER.info("started counter server at port:{}", raftServer.node.getNodeId().getPeerId().getPort());
    }

    public AbstractRaftServer getRaftServer() {
        return raftServer;
    }

    public void setRaftServer(AbstractRaftServer raftServer) {
        this.raftServer = raftServer;
    }

    public AbstractRaftStateMachine getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(AbstractRaftStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    public Boolean isLeader() {
        return !isRaftMode() && raftServer == null ? true
            : stateMachine != null && stateMachine.isLeader() ? true : false;
    }

    public Boolean isRaftMode() {
        return raftMode;
    }

    public Boolean isNotRaftModeLeader() {
        return !isLeader() && isRaftMode();
    }

    private static class SingletonHandler {
        private static RaftServerFactory instance = new RaftServerFactory();
    }
}
