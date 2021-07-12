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
package io.seata.core.model;

import io.seata.core.exception.TransactionException;

/**
 * Resource Manager: send outbound request to TC.
 *
 * @author sharajava
 */
public interface ResourceManagerOutbound {

    /**
     * Branch register long.
     *
     * @param branchType      the branch type
     * @param commitType      the commit type, @since above 1.4.2
     * @param resourceId      the resource id
     * @param clientId        the client id
     * @param xid             the xid
     * @param applicationData the context
     * @param lockKeys        the lock keys
     * @return the long
     * @throws TransactionException the transaction exception
     */
    Long branchRegister(BranchType branchType, CommitType commitType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws
        TransactionException;

    /**
     * Branch register long.
     *
     * @param branchType      the branch type
     * @param resourceId      the resource id
     * @param clientId        the client id
     * @param xid             the xid
     * @param applicationData the context
     * @param lockKeys        the lock keys
     * @return the long
     * @throws TransactionException the transaction exception
     */
    default Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws
            TransactionException {
        CommitType commitType = CommitType.getDefault(branchType);
        return branchRegister(branchType, commitType, resourceId, clientId, xid, applicationData, lockKeys);
    }

    /**
     * Branch report.
     *
     * @param branchType      the branch type
     * @param xid             the xid
     * @param branchId        the branch id
     * @param status          the status
     * @param applicationData the application data
     * @throws TransactionException the transaction exception
     */
    void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException;

    /**
     * Lock query boolean.
     *
     * @param branchType the branch type
     * @param resourceId the resource id
     * @param xid        the xid
     * @param lockKeys   the lock keys
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
        throws TransactionException;
}
