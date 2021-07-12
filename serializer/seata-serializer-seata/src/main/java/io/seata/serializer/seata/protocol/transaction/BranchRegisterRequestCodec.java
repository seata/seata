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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.seata.core.model.BranchType;
import io.seata.core.model.CommitType;
import io.seata.core.protocol.transaction.BranchRegisterRequest;

/**
 * The type Branch register request codec.
 *
 * @author zhangsen
 */
public class BranchRegisterRequestCodec extends AbstractTransactionRequestToTCCodec {

    @Override
    public Class<?> getMessageClassType() {
        return BranchRegisterRequest.class;
    }

    @Override
    public <T> void encode(T t, ByteBuf out) {
        BranchRegisterRequest branchRegisterRequest = (BranchRegisterRequest)t;

        String xid = branchRegisterRequest.getXid();
        BranchType branchType = branchRegisterRequest.getBranchType();
        CommitType commitType = branchRegisterRequest.getCommitType();
        String resourceId = branchRegisterRequest.getResourceId();
        String lockKey = branchRegisterRequest.getLockKey();
        String applicationData = branchRegisterRequest.getApplicationData();

        // 1. xid
        if (xid != null) {
            byte[] bs = xid.getBytes(UTF8);
            out.writeShort((short)bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short)0);
        }
        // 2. Branch Type
        out.writeByte(branchType.ordinal());

        // 3. Resource Id
        if (resourceId != null) {
            byte[] bs = resourceId.getBytes(UTF8);
            out.writeShort((short)bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short)0);
        }

        // 4. Lock Key
        if (lockKey != null) {
            byte[] lockKeyBytes = lockKey.getBytes(UTF8);
            out.writeInt(lockKeyBytes.length);
            if (lockKeyBytes.length > 0) {
                out.writeBytes(lockKeyBytes);
            }
        } else {
            out.writeInt(0);
        }

        //5. applicationData
        if (applicationData != null) {
            byte[] applicationDataBytes = applicationData.getBytes(UTF8);
            out.writeInt(applicationDataBytes.length);
            if (applicationDataBytes.length > 0) {
                out.writeBytes(applicationDataBytes);
            }
        } else {
            out.writeInt(0);
        }

        // 6. Commit Type
        out.writeByte((byte)commitType.value());
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        BranchRegisterRequest branchRegisterRequest = (BranchRegisterRequest)t;

        short xidLen = in.getShort();
        if (xidLen > 0) {
            byte[] bs = new byte[xidLen];
            in.get(bs);
            branchRegisterRequest.setXid(new String(bs, UTF8));
        }
        branchRegisterRequest.setBranchType(BranchType.get(in.get()));
        short len = in.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            in.get(bs);
            branchRegisterRequest.setResourceId(new String(bs, UTF8));
        }

        int iLen = in.getInt();
        if (iLen > 0) {
            byte[] bs = new byte[iLen];
            in.get(bs);
            branchRegisterRequest.setLockKey(new String(bs, UTF8));
        }

        int applicationDataLen = in.getInt();
        if (applicationDataLen > 0) {
            byte[] bs = new byte[applicationDataLen];
            in.get(bs);
            branchRegisterRequest.setApplicationData(new String(bs, UTF8));
        }

        try {
            branchRegisterRequest.setCommitType(CommitType.get(in.get()));
        } catch (BufferUnderflowException e) {
            // If current request is from an older version of the client, set the default commitType.
            CommitType commitType = CommitType.getDefault(branchRegisterRequest.getBranchType());
            branchRegisterRequest.setCommitType(commitType);
        }
    }

}
