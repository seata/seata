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
package io.seata.core.protocol.transaction;

import io.seata.core.model.BranchType;
import io.seata.core.model.CommitType;
import io.seata.core.protocol.MessageType;
import io.seata.core.rpc.RpcContext;

/**
 * The type Branch register request.
 *
 * @author sharajava
 */
public class BranchRegisterRequest extends AbstractTransactionRequestToTC  {

    private String xid;

    private BranchType branchType = BranchType.AT;

    private CommitType commitType = CommitType.SyncCommit;

    private String resourceId;

    private String lockKey;

    private String applicationData;

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public String getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(String xid) {
        this.xid = xid;
    }

    /**
     * Gets branch type.
     *
     * @return the branch type
     */
    public BranchType getBranchType() {
        return branchType;
    }

    /**
     * Sets branch type.
     *
     * @param branchType the branch type
     */
    public void setBranchType(BranchType branchType) {
        this.branchType = branchType;
    }

    /**
     * Gets commit type.
     *
     * @return the commit type
     */
    public CommitType getCommitType() {
        return commitType;
    }

    /**
     * Sets commit type.
     *
     * @param commitType the commit type
     */
    public BranchRegisterRequest setCommitType(CommitType commitType) {
        this.commitType = commitType;
        return this;
    }

    /**
     * Gets lock key.
     *
     * @return the lock key
     */
    public String getLockKey() {
        return lockKey;
    }

    /**
     * Sets lock key.
     *
     * @param lockKey the lock key
     */
    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    /**
     * Gets resource id.
     *
     * @return the resource id
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Sets resource id.
     *
     * @param resourceId the resource id
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_BRANCH_REGISTER;
    }

    /**
     * Gets application data.
     *
     * @return the application data
     */
    public String getApplicationData() {
        return applicationData;
    }

    /**
     * Sets application data.
     *
     * @param applicationData the application data
     */
    public void setApplicationData(String applicationData) {
        this.applicationData = applicationData;
    }

    @Override
    public AbstractTransactionResponse handle(RpcContext rpcContext) {
        return handler.handle(this, rpcContext);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("xid=");
        result.append(xid);
        result.append(",");
        result.append("branchType=");
        result.append(branchType);
        result.append(",");
        result.append("resourceId=");
        result.append(resourceId);
        result.append(",");
        result.append("lockKey=");
        result.append(lockKey);

        return result.toString();
    }
}
