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
package io.seata.server.session;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.GlobalStoppedReason;

/**
 * The interface Session lifecycle listener.
 *
 * @author sharajava
 */
public interface SessionLifecycleListener {

    /**
     * On begin.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    void onBegin(GlobalSession globalSession) throws TransactionException;

    /**
     * On update.
     *
     * @param globalSession    the global session
     * @param status           the status
     * @param suspendedEndTime the suspended end time
     * @param stoppedReason    the stopped reason
     * @throws TransactionException the transaction exception
     */
    void onUpdate(GlobalSession globalSession, GlobalStatus status,
                  long suspendedEndTime, GlobalStoppedReason stoppedReason) throws TransactionException;

    /**
     * On branch update.
     *
     * @param globalSession   the global session
     * @param branchSession   the branch session
     * @param status          the status
     * @param applicationData the application data
     * @param retryCount      the retry count
     * @throws TransactionException the transaction exception
     */
    void onBranchUpdate(GlobalSession globalSession, BranchSession branchSession, BranchStatus status,
                        String applicationData, int retryCount) throws TransactionException;

    /**
     * On add branch.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws TransactionException the transaction exception
     */
    void onAddBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException;

    /**
     * On remove branch.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws TransactionException the transaction exception
     */
    void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException;

    /**
     * On close.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    void onClose(GlobalSession globalSession) throws TransactionException;

    /**
     * On end.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    void onEnd(GlobalSession globalSession) throws TransactionException;
}
