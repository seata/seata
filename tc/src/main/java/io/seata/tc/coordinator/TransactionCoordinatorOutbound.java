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
package io.seata.tc.coordinator;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.tc.session.BranchSession;
import io.seata.tc.session.GlobalSession;

/**
 * send outbound request to RM.
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.1.0
 */
public interface TransactionCoordinatorOutbound {

    /**
     * Commit a branch transaction.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @return Status of the branch after committing.
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     *                              out.
     */
    BranchStatus branchCommit(GlobalSession globalSession, BranchSession branchSession) throws TransactionException;

    /**
     * Rollback a branch transaction.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @return Status of the branch after rollbacking.
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     *                              out.
     */
    BranchStatus branchRollback(GlobalSession globalSession, BranchSession branchSession) throws TransactionException;


}
