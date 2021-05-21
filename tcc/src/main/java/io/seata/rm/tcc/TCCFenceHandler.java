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
package io.seata.rm.tcc;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.executor.Callback;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.constant.TCCFenceConstant;
import io.seata.rm.tcc.exception.TCCFenceException;
import io.seata.rm.tcc.store.TCCFenceDO;
import io.seata.rm.tcc.store.TCCFenceStore;
import io.seata.rm.tcc.store.db.TCCFenceStoreDataBaseDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Date;

/**
 * TCC Fence Handler(idempotent, non_rollback, suspend)
 *
 * @author kaka2code
 */
public class TCCFenceHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TCCFenceHandler.class);

    private static final TCCFenceStore TCC_FENCE_DAO = TCCFenceStoreDataBaseDAO.getInstance();

    private static DataSource dataSource;

    private static TransactionTemplate transactionTemplate;

    public static void setDataSource(DataSource dataSource) {
        TCCFenceHandler.dataSource = dataSource;
    }

    public static void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        TCCFenceHandler.transactionTemplate = transactionTemplate;
    }

    /**
     * tcc prepare method enhanced
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    public static Object prepareFence(String xid, Long branchId, Callback<Object> targetCallback) {
        return transactionTemplate.execute(status -> {
            try {
                Connection conn = DataSourceUtils.getConnection(dataSource);
                TCCFenceDO tccFenceDO = new TCCFenceDO();
                tccFenceDO.setXid(xid);
                tccFenceDO.setBranchId(branchId);
                tccFenceDO.setStatus(TCCFenceConstant.STATUS_TRIED);
                boolean result = TCC_FENCE_DAO.insertTCCFenceDO(conn, tccFenceDO);
                LOGGER.info("TCC fence prepare result: {}. xid: {}, branchId: {}", result, xid, branchId);
                if (result) {
                    return targetCallback.execute();
                } else {
                    throw new TCCFenceException(String.format("Insert tcc fence record error, prepare fence failed. xid= %s, branchId= %s", xid, branchId),
                            FrameworkErrorCode.InsertRecordError);
                }
            } catch (Throwable t) {
                status.setRollbackOnly();
                throw new FrameworkException(t);
            }
        });
    }

    /**
     * tcc commit method enhanced
     * @param commitMethod commit method
     * @param targetTCCBean target tcc bean
     * @param businessActionContext businessActionContext
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    public static boolean commitFence(Method commitMethod, Object targetTCCBean, BusinessActionContext businessActionContext, String xid, Long branchId) {
        return transactionTemplate.execute(status -> {
            try {
                Connection conn = DataSourceUtils.getConnection(dataSource);
                TCCFenceDO tccFenceDO = TCC_FENCE_DAO.queryTCCFenceDO(conn, xid, branchId);
                if (tccFenceDO == null) {
                    throw new TCCFenceException(String.format("TCC fence record not exists, commit fence method failed. xid= %s, branchId= %s", xid, branchId),
                            FrameworkErrorCode.RecordAlreadyExists);
                }
                if (TCCFenceConstant.STATUS_COMMITTED == tccFenceDO.getStatus()) {
                    LOGGER.info("Branch transaction has already committed before. idempotency rejected. xid: {}, branchId: {}, status: {}", xid, branchId, tccFenceDO.getStatus());
                    return true;
                }
                if (TCCFenceConstant.STATUS_ROLLBACKED == tccFenceDO.getStatus() || TCCFenceConstant.STATUS_SUSPENDED == tccFenceDO.getStatus()) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Branch transaction status is unexpected. xid: {}, branchId: {}, status: {}", xid, branchId, tccFenceDO.getStatus());
                    }
                    return false;
                }
                return updateStatusAndInvokeTargetMethod(conn, commitMethod, targetTCCBean, businessActionContext, xid, branchId, TCCFenceConstant.STATUS_COMMITTED);
            } catch (Throwable t) {
                status.setRollbackOnly();
                throw new FrameworkException(t);
            }
        });
    }

    /**
     * tcc rollback method enhanced
     * @param rollbackMethod rollback method
     * @param targetTCCBean target tcc bean
     * @param businessActionContext businessActionContext
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    public static boolean rollbackFence(Method rollbackMethod, Object targetTCCBean, BusinessActionContext businessActionContext, String xid, Long branchId) {
        return transactionTemplate.execute(status -> {
            try {
                Connection conn = DataSourceUtils.getConnection(dataSource);
                TCCFenceDO tccFenceDO = TCC_FENCE_DAO.queryTCCFenceDO(conn, xid, branchId);
                // non_rollback
                if (tccFenceDO == null) {
                    tccFenceDO = new TCCFenceDO();
                    tccFenceDO.setXid(xid);
                    tccFenceDO.setBranchId(branchId);
                    tccFenceDO.setStatus(TCCFenceConstant.STATUS_SUSPENDED);
                    boolean result = TCC_FENCE_DAO.insertTCCFenceDO(conn, tccFenceDO);
                    LOGGER.info("Insert tcc fence record result: {}. xid: {}, branchId: {}", result, xid, branchId);
                    if (!result) {
                        throw new TCCFenceException(String.format("Insert tcc fence record error, rollback fence method failed. xid= %s, branchId= %s", xid, branchId),
                                FrameworkErrorCode.InsertRecordError);
                    }
                    return true;
                } else {
                    if (TCCFenceConstant.STATUS_ROLLBACKED == tccFenceDO.getStatus() || TCCFenceConstant.STATUS_SUSPENDED == tccFenceDO.getStatus()) {
                        LOGGER.info("Branch transaction had already rollbacked before, idempotency rejected. xid: {}, branchId: {}, status: {}", xid, branchId, tccFenceDO.getStatus());
                        return true;
                    }
                    if (TCCFenceConstant.STATUS_COMMITTED == tccFenceDO.getStatus()) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Branch transaction status is unexpected. xid: {}, branchId: {}, status: {}", xid, branchId, tccFenceDO.getStatus());
                        }
                        return false;
                    }
                }
                return updateStatusAndInvokeTargetMethod(conn, rollbackMethod, targetTCCBean, businessActionContext, xid, branchId, TCCFenceConstant.STATUS_ROLLBACKED);
            } catch (Throwable t) {
                status.setRollbackOnly();
                throw new FrameworkException(t);
            }
        });
    }

    /**
     * Update TCC Fence status and invoke target method
     * @param method target method
     * @param targetTCCBean target bean
     * @param businessActionContext businessActionContext
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @param status the tcc fence status
     * @return the boolean
     */
    private static boolean updateStatusAndInvokeTargetMethod(Connection conn, Method method, Object targetTCCBean, BusinessActionContext businessActionContext, String xid, Long branchId, int status) throws Exception {
        boolean result = TCC_FENCE_DAO.updateTCCFenceDO(conn, xid, branchId, status, TCCFenceConstant.STATUS_TRIED);
        if (result) {
            // invoke two phase method
            Object ret = method.invoke(targetTCCBean, businessActionContext);
            if (null != ret) {
                if (ret instanceof TwoPhaseResult) {
                    result = ((TwoPhaseResult) ret).isSuccess();
                } else {
                    result = (boolean) ret;
                }
            }
        }
        return result;
    }

    /**
     * Delete TCC Fence
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    public static boolean deleteFence(String xid, Long branchId) {
        return transactionTemplate.execute(status -> {
            boolean ret = false;
            try {
                Connection conn = DataSourceUtils.getConnection(dataSource);
                ret = TCC_FENCE_DAO.deleteTCCFenceDO(conn, xid, branchId);
            } catch (Exception e) {
                status.setRollbackOnly();
            }
            return ret;
        });
    }

    /**
     * Delete TCC Fence By Datetime
     * @param datetime datetime
     * @return the boolean
     */
    public static boolean deleteFenceByDate(Date datetime) {
        return transactionTemplate.execute(status -> {
            boolean ret = false;
            try {
                Connection conn = DataSourceUtils.getConnection(dataSource);
                ret = TCC_FENCE_DAO.deleteTCCFenceDOByDate(conn, datetime);
            } catch (Exception e) {
                status.setRollbackOnly();
            }
            return ret;
        });
    }
}