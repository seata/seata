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

package io.seata.server.lock.mongo;

import com.github.fakemongo.Fongo;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.server.lock.LockManager;
import io.seata.server.session.BranchSession;
import io.seata.server.storage.file.lock.FileLockManager;
import io.seata.server.storage.mongo.MongoPooledFactory;
import io.seata.server.storage.mongo.lock.MongoLocker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author funkye
 */
public class MongoLockManagerTest {
    static LockManager lockManager = null;

    @BeforeAll
    public static void start() {
        Fongo fongo = new Fongo("mongo server 1");
        MongoPooledFactory.getMongoPoolInstance(fongo.getMongo());
        lockManager = new MongoLockManagerForTest();
    }

    @Test
    public void acquireLock() throws TransactionException {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:786756");
        branchSession.setTransactionId(123543465);
        branchSession.setBranchId(5756678);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:13,14;t2:11,12");
        Assertions.assertTrue(lockManager.acquireLock(branchSession));
    }

    @Test
    public void unLock() throws TransactionException {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:56867");
        branchSession.setTransactionId(1236765);
        branchSession.setBranchId(204565);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:3,4;t2:4,5");
        Assertions.assertTrue(lockManager.releaseLock(branchSession));
    }

    @Test
    public void isLockable() throws TransactionException {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:56877898");
        branchSession.setTransactionId(245686786);
        branchSession.setBranchId(467568);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:8,7;t2:1,2");
        Assertions.assertTrue(lockManager.acquireLock(branchSession));
        BranchSession branchSession2 = new BranchSession();
        branchSession2.setXid("abc-123:56877898");
        branchSession2.setTransactionId(245686786);
        branchSession2.setBranchId(1242354576);
        branchSession2.setResourceId("abcss");
        branchSession2.setLockKey("t1:8");
        Assertions.assertNotNull(lockManager.isLockable(branchSession2.getXid(), branchSession2.getResourceId(),
            branchSession2.getLockKey()));
    }


    public static class MongoLockManagerForTest extends FileLockManager {

        public MongoLockManagerForTest() {}

        @Override
        public Locker getLocker(BranchSession branchSession) {

            return new MongoLocker();
        }
    }
}
