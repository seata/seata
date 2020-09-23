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
package io.seata.core.store;

/**
 * The type Branch transaction model.
 *
 * @author wang.liang
 */
public interface BranchTransactionModel extends BaseModel {

    String getXid();

    void setXid(String xid);

    long getTransactionId();

    void setTransactionId(long transactionId);

    long getBranchId();

    void setBranchId(long branchId);

    String getResourceGroupId();

    void setResourceGroupId(String resourceGroupId);

    String getResourceId();

    void setResourceId(String resourceId);

    String getBranchType();

    void setBranchType(String branchType);

    int getStatus();

    void setStatus(int status);

    String getClientId();

    void setClientId(String clientId);

    String getApplicationData();

    void setApplicationData(String applicationData);
}
