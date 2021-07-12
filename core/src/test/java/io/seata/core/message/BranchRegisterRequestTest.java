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
package io.seata.core.message;

import io.seata.core.model.BranchType;
import io.seata.core.model.CommitType;
import io.seata.core.protocol.transaction.BranchRegisterRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Branch register request test.
 */
public class BranchRegisterRequestTest {
    /**
     * To string test.
     */
    @Test
    public void toStringTest() {
        BranchRegisterRequest branchRegisterRequest = new BranchRegisterRequest();
        branchRegisterRequest.setXid("127.0.0.1:8091:1249853");
        branchRegisterRequest.setBranchType(BranchType.AT);
        branchRegisterRequest.setCommitType(CommitType.AsyncCommit);
        branchRegisterRequest.setResourceId("resource1");
        branchRegisterRequest.setLockKey("lock_key_1");
        Assertions.assertEquals("xid=127.0.0.1:8091:1249853,branchType=AT,resourceId=resource1,lockKey=lock_key_1",
            branchRegisterRequest.toString());

    }

}
