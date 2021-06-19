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
package io.seata.server.raft;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.RaftGroupService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.codahale.metrics.Slf4jReporter;
import io.seata.common.loader.LoadLevel;
import io.seata.config.ConfigurationCache;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.core.raft.AbstractRaftServer;
import io.seata.core.raft.AbstractRaftStateMachine;
import io.seata.core.raft.RaftServer;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;


import static io.seata.common.DefaultValues.SEATA_RAFT_GROUP;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_CLUSTER;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_REPORTER_ENABLED;
import static io.seata.core.constants.ConfigurationKeys.SERVER_RAFT_REPORTER_INITIAL_DELAY;
import static io.seata.core.raft.AbstractRaftServer.RAFT_TAG;

/**
 * @author funkye
 */
@LoadLevel(name = RAFT_TAG)
public class RaftServerImpl extends AbstractRaftServer implements ConfigurationChangeListener {

    public RaftServerImpl(final String dataPath, final String groupId, final PeerId serverId,
        final NodeOptions nodeOptions) throws IOException {
        // Initialization path
        FileUtils.forceMkdir(new File(dataPath));

        // Here you have raft RPC and business RPC using the same RPC server, and you can usually do this separately
        final RpcServer rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
        // Initialize the state machine
        this.raftStateMachine = new RaftStateMachine();
        // Set the state machine to startup parameters
        nodeOptions.setFsm(this.raftStateMachine);
        // Set the storage path
        // Log, must
        nodeOptions.setLogUri(dataPath + File.separator + "log");
        // Meta information, must
        nodeOptions.setRaftMetaUri(dataPath + File.separator + "raft_meta");
        // Snapshot, optional, is generally recommended
        nodeOptions.setSnapshotUri(dataPath + File.separator + "snapshot");
        boolean reporterEnabled = ConfigurationFactory.getInstance().getBoolean(SERVER_RAFT_REPORTER_ENABLED, false);
        nodeOptions.setEnableMetrics(reporterEnabled);
        // Initialize the raft Group service framework
        this.raftGroupService = new RaftGroupService(groupId, serverId, nodeOptions, rpcServer);
        this.cliService = RaftServiceFactory.createAndInitCliService(new CliOptions());
        ConfigurationCache.addConfigListener(SERVER_RAFT_CLUSTER, this);
        this.node = this.raftGroupService.start();
        if (reporterEnabled) {
            final Slf4jReporter reporter = Slf4jReporter.forRegistry(node.getNodeMetrics().getMetricRegistry())
                .outputTo(LoggerFactory.getLogger(getClass())).convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS).build();
            reporter.start(ConfigurationFactory.getInstance().getInt(SERVER_RAFT_REPORTER_INITIAL_DELAY, 30),
                TimeUnit.MINUTES);
        }
    }

    public RaftServerImpl() {}

    @Override
    public Node getNode() {
        return this.node;
    }

    @Override
    public AbstractRaftStateMachine getAbstractRaftStateMachine() {
        return raftStateMachine;
    }

    public StateMachineAdapter getRaftStateMachine() {
        return raftStateMachine;
    }

    public void setRaftStateMachine(RaftStateMachine raftStateMachine) {
        this.raftStateMachine = raftStateMachine;
    }

    public CliService getCliService() {
        return cliService;
    }

    public void setCliService(CliService cliService) {
        this.cliService = cliService;
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        if (SERVER_RAFT_CLUSTER.equals(event.getDataId())) {
            final Configuration newConf = new Configuration();
            if (newConf.parse(event.getNewValue())) {
                Node node = getNode();
                if (node != null && node.isLeader()) {
                    CliService cliService = getCliService();
                    cliService.changePeers(SEATA_RAFT_GROUP, getNode().getOptions().getInitialConf(), newConf);
                }
            }
        }
    }

    @Override
    public RaftServer init(String dataPath, String groupId, PeerId serverId, NodeOptions nodeOptions)
        throws IOException {
        return new RaftServerImpl(dataPath, groupId, serverId, nodeOptions);
    }

    @Override
    public void close() throws Exception {
        if (this.raftGroupService != null) {
            this.raftGroupService.shutdown();
        }
    }

}
