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
package io.seata.serializer.protobuf.convertor;

import io.seata.core.model.CommitType;
import io.seata.serializer.protobuf.generated.GlobalLockQueryRequestProto;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author leizhiyuan
 */
public class GlobalLockQueryRequestConvertorTest {

    @Test
    public void convert2Proto() {

        GlobalLockQueryRequest globalLockQueryRequest = new GlobalLockQueryRequest();
        globalLockQueryRequest.setApplicationData("data");
        globalLockQueryRequest.setBranchType(BranchType.AT);
        globalLockQueryRequest.setCommitType(CommitType.AsyncCommit);
        globalLockQueryRequest.setLockKey("localKey");
        globalLockQueryRequest.setResourceId("resourceId");
        globalLockQueryRequest.setXid("xid");

        GlobalLockQueryRequestConvertor convertor = new GlobalLockQueryRequestConvertor();
        GlobalLockQueryRequestProto proto = convertor.convert2Proto(
            globalLockQueryRequest);
        GlobalLockQueryRequest real = convertor.convert2Model(proto);

        assertThat(real.getTypeCode()).isEqualTo(globalLockQueryRequest.getTypeCode());
        assertThat(real.getApplicationData()).isEqualTo(globalLockQueryRequest.getApplicationData());
        assertThat(real.getXid()).isEqualTo(globalLockQueryRequest.getXid());
        assertThat(real.getBranchType()).isEqualTo(globalLockQueryRequest.getBranchType());
        assertThat(real.getCommitType()).isEqualTo(globalLockQueryRequest.getCommitType());
        assertThat(real.getLockKey()).isEqualTo(globalLockQueryRequest.getLockKey());
        assertThat(real.getResourceId()).isEqualTo(globalLockQueryRequest.getResourceId());
    }
}