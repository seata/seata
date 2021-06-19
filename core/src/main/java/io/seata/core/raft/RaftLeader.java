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

import java.net.InetSocketAddress;

import static io.seata.common.DefaultValues.DEFAULT_RAFT_PORT_INTERVAL;

/**
 * @author funkye
 */
public class RaftLeader {

    private InetSocketAddress inetSocketAddress;

    private Long timestamp;

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    public void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isExpired() {
        return this.timestamp == null || ((System.currentTimeMillis() - this.timestamp) > DEFAULT_RAFT_PORT_INTERVAL);
    }

    public boolean isNotExpired() {
        return !isExpired();
    }

    @Override
    public String toString() {
        return "RaftLeader{" + "LEADER_ADDRESS=" + inetSocketAddress + ", timestamp=" + timestamp + '}';
    }

}
