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
package io.seata.server.raft.execute.global;

import io.seata.server.raft.execute.AbstractRaftMsgExecute;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.raft.RaftSessionSyncMsg;

/**
 * @author jianbin.chen
 */
public class AddGlobalSessionExecute extends AbstractRaftMsgExecute {

    public AddGlobalSessionExecute(RaftSessionSyncMsg sessionSyncMsg) {
        super(sessionSyncMsg);
    }

    @Override
    public Boolean execute(Object... objects) throws Throwable {
        GlobalSession globalSession = SessionConverter.convertGlobalSession(sessionSyncMsg.getGlobalSession());
        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        raftSessionManager.addGlobalSession(globalSession);
        if (logger.isDebugEnabled()) {
            logger.debug("addGlobalSession xid: {},status: {}", globalSession.getXid(), globalSession.getStatus());
        }
        return true;
    }

}
