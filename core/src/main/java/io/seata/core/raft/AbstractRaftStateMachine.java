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

import java.util.concurrent.atomic.AtomicLong;
import com.alipay.sofa.jraft.core.StateMachineAdapter;

/**
 * @author funkye
 */
public abstract class AbstractRaftStateMachine extends StateMachineAdapter {

    /**
     * Leader term
     */
    protected final AtomicLong leaderTerm = new AtomicLong(-1);

    protected String mode;

    protected boolean isLeader() {
        return this.leaderTerm.get() > 0;
    }

}
