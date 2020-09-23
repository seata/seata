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

import io.seata.core.model.GlobalStatus;

/**
 * The type Global transaction model.
 *
 * @author wang.liang
 */
public interface GlobalTransactionModel extends BaseModel {

    String getXid();

    void setXid(String xid);

    long getTransactionId();

    void setTransactionId(long transactionId);

    GlobalStatus getStatus();

    void setStatus(GlobalStatus status);

    String getApplicationId();

    void setApplicationId(String applicationId);

    String getTransactionServiceGroup();

    void setTransactionServiceGroup(String transactionServiceGroup);

    String getTransactionName();

    void setTransactionName(String transactionName);

    int getTimeout();

    void setTimeout(int timeout);

    long getBeginTime();

    void setBeginTime(long beginTime);

    String getApplicationData();

    void setApplicationData(String applicationData);
}
