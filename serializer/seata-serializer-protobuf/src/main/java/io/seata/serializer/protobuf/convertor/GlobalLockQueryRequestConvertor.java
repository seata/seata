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
import io.seata.serializer.protobuf.generated.AbstractMessageProto;
import io.seata.serializer.protobuf.generated.AbstractTransactionRequestProto;
import io.seata.serializer.protobuf.generated.BranchRegisterRequestProto;
import io.seata.serializer.protobuf.generated.BranchTypeProto;
import io.seata.serializer.protobuf.generated.GlobalLockQueryRequestProto;
import io.seata.serializer.protobuf.generated.MessageTypeProto;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;

/**
 * @author leizhiyuan
 */
public class GlobalLockQueryRequestConvertor
    implements PbConvertor<GlobalLockQueryRequest, GlobalLockQueryRequestProto> {
    @Override
    public GlobalLockQueryRequestProto convert2Proto(GlobalLockQueryRequest globalLockQueryRequest) {
        final short typeCode = globalLockQueryRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(abstractMessage).build();

        final String applicationData = globalLockQueryRequest.getApplicationData();
        final String lockKey = globalLockQueryRequest.getLockKey();
        BranchRegisterRequestProto branchRegisterRequestProto = BranchRegisterRequestProto.newBuilder()
            .setAbstractTransactionRequest(abstractTransactionRequestProto)
            .setApplicationData(applicationData == null ? "" : applicationData)
            .setBranchType(BranchTypeProto.valueOf(globalLockQueryRequest.getBranchType().name()))
            .setCommitType(globalLockQueryRequest.getCommitType().value())
            .setLockKey(lockKey == null ? "" : lockKey)
            .setResourceId(globalLockQueryRequest.getResourceId())
            .setXid(globalLockQueryRequest.getXid())
            .build();

        GlobalLockQueryRequestProto result = GlobalLockQueryRequestProto.newBuilder().setBranchRegisterRequest(
            branchRegisterRequestProto).build();

        return result;
    }

    @Override
    public GlobalLockQueryRequest convert2Model(GlobalLockQueryRequestProto globalLockQueryRequestProto) {
        GlobalLockQueryRequest branchRegisterRequest = new GlobalLockQueryRequest();
        BranchRegisterRequestProto branchRegisterRequestProto = globalLockQueryRequestProto.getBranchRegisterRequest();
        branchRegisterRequest.setApplicationData(branchRegisterRequestProto.getApplicationData());
        branchRegisterRequest.setBranchType(BranchType.valueOf(branchRegisterRequestProto.getBranchType().name()));
        branchRegisterRequest.setCommitType(CommitType.get(branchRegisterRequestProto.getCommitType()));
        branchRegisterRequest.setLockKey(branchRegisterRequestProto.getLockKey());
        branchRegisterRequest.setResourceId(branchRegisterRequestProto.getResourceId());
        branchRegisterRequest.setXid(branchRegisterRequestProto.getXid());
        return branchRegisterRequest;
    }
}