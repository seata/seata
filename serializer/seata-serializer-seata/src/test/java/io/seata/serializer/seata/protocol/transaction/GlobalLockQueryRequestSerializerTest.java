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
package io.seata.serializer.seata.protocol.transaction;

import io.seata.core.model.CommitType;
import io.seata.serializer.seata.SeataSerializer;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * The type Global lock query request codec test.
 *
 * @author zhangsen
 */
public class GlobalLockQueryRequestSerializerTest {

    /**
     * The Seata codec.
     */
    SeataSerializer seataSerializer = new SeataSerializer();

    /**
     * Test codec.
     */
    @Test
    public void test_codec(){
        GlobalLockQueryRequest globalLockQueryRequest = new GlobalLockQueryRequest();
        globalLockQueryRequest.setApplicationData("aaaa");
        globalLockQueryRequest.setBranchType(BranchType.TCC);
        globalLockQueryRequest.setCommitType(CommitType.SyncCommit);
        globalLockQueryRequest.setLockKey("a:1,b,2");
        globalLockQueryRequest.setXid("aaa");
        globalLockQueryRequest.setResourceId("1s");

        byte[] bytes = seataSerializer.serialize(globalLockQueryRequest);

        GlobalLockQueryRequest globalLockQueryRequest2 = seataSerializer.deserialize(bytes);

        assertThat(globalLockQueryRequest2.getApplicationData()).isEqualTo(globalLockQueryRequest.getApplicationData());
        assertThat(globalLockQueryRequest2.getBranchType()).isEqualTo(globalLockQueryRequest.getBranchType());
        assertThat(globalLockQueryRequest2.getCommitType()).isEqualTo(globalLockQueryRequest.getCommitType());
        assertThat(globalLockQueryRequest2.getLockKey()).isEqualTo(globalLockQueryRequest.getLockKey());
        assertThat(globalLockQueryRequest2.getResourceId()).isEqualTo(globalLockQueryRequest.getResourceId());
        assertThat(globalLockQueryRequest2.getXid()).isEqualTo(globalLockQueryRequest.getXid());
    }

}
