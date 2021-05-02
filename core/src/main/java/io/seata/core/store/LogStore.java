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


import java.util.List;

/**
 * the transaction log store
 *
 * @author zhangsen
 */
public interface LogStore {

    /**
     * Query global transaction do global transaction do.
     *
     * @param xid the xid
     * @return the global transaction do
     */
    GlobalTransactionDO queryGlobalTransactionDO(String xid);

    /**
     * Query global transaction do global transaction do.
     *
     * @param transactionId the transaction id
     * @return the global transaction do
     */
    GlobalTransactionDO queryGlobalTransactionDO(long transactionId);

    /**
     * Query global transaction do list.
     *
     * @param status the status
     * @param limit  the limit
     * @return the list
     */
    List<GlobalTransactionDO> queryGlobalTransactionDO(int[] status, int limit);
    
    /**
     * Query expired removed global transaction do list.
     *
     * @param expiredTime
     * @param limit
     * @return global transaction do list.
     */
    List<GlobalTransactionDO> queryExpiredGlobalTransactionDO(long expiredTime, int limit);

    /**
     * Insert global transaction do boolean.
     *
     * @param globalTransactionDO the global transaction do
     * @return the boolean
     */
    boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO);

    /**
     * Update global transaction do boolean.
     *
     * @param globalTransactionDO the global transaction do
     * @return the boolean
     */
    boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO);

    /**
     * Delete global transaction do boolean.
     *
     * @param globalTransactionDO the global transaction do
     * @return the boolean
     */
    boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO);
    
    /**
     * Batch delete global transaction do boolean.
     *
     * @param globalTransactionDO the global transaction do
     * @return the boolean
     */
    boolean deleteGlobalTransactionDO(List<GlobalTransactionDO> globalTransactionDOs);

    /**
     * Query branch transaction do list.
     *
     * @param xid the xid
     * @return the BranchTransactionDO list
     */
    List<BranchTransactionDO> queryBranchTransactionDO(String xid);

    /**
     * Query branch transaction do list.
     *
     * @param xids the xid list
     * @return the BranchTransactionDO list
     */
    List<BranchTransactionDO> queryBranchTransactionDO(List<String> xids);

    /**
     * Insert branch transaction do boolean.
     *
     * @param branchTransactionDO the branch transaction do
     * @return the boolean
     */
    boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO);

    /**
     * Update branch transaction do boolean.
     *
     * @param branchTransactionDO the branch transaction do
     * @return the boolean
     */
    boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO);

    /**
     * Delete branch transaction do boolean.
     *
     * @param branchTransactionDO the branch transaction do
     * @return the boolean
     */
    boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO);
    
    /**
     * Batch delete branch transaction do boolean.
     *
     * @param branchTransactionDOs the branch transaction do list
     * @return the boolean
     */
    boolean deleteBranchTransactionDO(List<BranchTransactionDO> branchTransactionDOs);

    /**
     * Gets current max session id.
     *
     * @param high the high
     * @param low  the low
     * @return the current max session id
     */
    long getCurrentMaxSessionId(long high, long low);

}
