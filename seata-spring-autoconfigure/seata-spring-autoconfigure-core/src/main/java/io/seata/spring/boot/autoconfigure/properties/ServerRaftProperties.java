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
package io.seata.spring.boot.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVER_RAFT_PREFIX;

/**
 * @author funkye
 */
@Component
@ConfigurationProperties(prefix = SERVER_RAFT_PREFIX)
public class ServerRaftProperties {

    private String cluster;

    private Integer snapshotInterval = 600;

    private Integer applyBatch = 32;

    private Integer maxAppendBufferSize = 256 * 1024;

    private Integer maxReplicatorInflightMsgs = 256;

    private Integer disruptorBufferSize = 16384;

    private Integer electionTimeoutMs = 2000;

    private boolean reporterEnabled = false;

    private Integer reporterInitialDelay = 30;

    public String getCluster() {
        return cluster;
    }

    public ServerRaftProperties setCluster(String cluster) {
        this.cluster = cluster;
        return this;
    }

    public Integer getSnapshotInterval() {
        return snapshotInterval;
    }

    public ServerRaftProperties setSnapshotInterval(Integer snapshotInterval) {
        this.snapshotInterval = snapshotInterval;
        return this;
    }

    public Integer getApplyBatch() {
        return applyBatch;
    }

    public ServerRaftProperties setApplyBatch(Integer applyBatch) {
        this.applyBatch = applyBatch;
        return this;
    }

    public Integer getMaxAppendBufferSize() {
        return maxAppendBufferSize;
    }

    public ServerRaftProperties setMaxAppendBufferSize(Integer maxAppendBufferSize) {
        this.maxAppendBufferSize = maxAppendBufferSize;
        return this;
    }

    public Integer getMaxReplicatorInflightMsgs() {
        return maxReplicatorInflightMsgs;
    }

    public ServerRaftProperties setMaxReplicatorInflightMsgs(Integer maxReplicatorInflightMsgs) {
        this.maxReplicatorInflightMsgs = maxReplicatorInflightMsgs;
        return this;
    }

    public Integer getDisruptorBufferSize() {
        return disruptorBufferSize;
    }

    public ServerRaftProperties setDisruptorBufferSize(Integer disruptorBufferSize) {
        this.disruptorBufferSize = disruptorBufferSize;
        return this;
    }

    public Integer getElectionTimeoutMs() {
        return electionTimeoutMs;
    }

    public ServerRaftProperties setElectionTimeoutMs(Integer electionTimeoutMs) {
        this.electionTimeoutMs = electionTimeoutMs;
        return this;
    }

    public boolean isReporterEnabled() {
        return reporterEnabled;
    }

    public ServerRaftProperties setReporterEnabled(boolean reporterEnabled) {
        this.reporterEnabled = reporterEnabled;
        return this;
    }

    public Integer getReporterInitialDelay() {
        return reporterInitialDelay;
    }

    public ServerRaftProperties setReporterInitialDelay(Integer reporterInitialDelay) {
        this.reporterInitialDelay = reporterInitialDelay;
        return this;
    }

}
