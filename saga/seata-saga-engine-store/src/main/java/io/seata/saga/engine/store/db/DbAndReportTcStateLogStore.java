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
package io.seata.saga.engine.store.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.seata.common.Constants;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.config.DbStateMachineConfig;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.engine.sequence.SeqGenerator;
import io.seata.saga.engine.serializer.Serializer;
import io.seata.saga.engine.serializer.impl.ExceptionSerializer;
import io.seata.saga.engine.serializer.impl.ParamsSerializer;
import io.seata.saga.engine.store.StateLogStore;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.impl.StateInstanceImpl;
import io.seata.saga.statelang.domain.impl.StateMachineInstanceImpl;
import io.seata.saga.tm.SagaTransactionalTemplate;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.TransactionalExecutor.ExecutionException;
import io.seata.tm.api.transaction.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * State machine logs and definitions persist to database and report status to TC (Transaction Coordinator)
 *
 * @author lorne.cl
 */
public class DbAndReportTcStateLogStore extends AbstractStore implements StateLogStore {

    private static final Logger                                   LOGGER                       = LoggerFactory.getLogger(
            DbAndReportTcStateLogStore.class);
    private static final StateMachineInstanceToStatementForInsert STATE_MACHINE_INSTANCE_TO_STATEMENT_FOR_INSERT
                                                                                               = new StateMachineInstanceToStatementForInsert();
    private static final StateMachineInstanceToStatementForUpdate STATE_MACHINE_INSTANCE_TO_STATEMENT_FOR_UPDATE
                                                                                               = new StateMachineInstanceToStatementForUpdate();
    private static final ResultSetToStateMachineInstance          RESULT_SET_TO_STATE_MACHINE_INSTANCE
                                                                                               = new ResultSetToStateMachineInstance();
    private static final StateInstanceToStatementForInsert        STATE_INSTANCE_TO_STATEMENT_FOR_INSERT
                                                                                               = new StateInstanceToStatementForInsert();
    private static final StateInstanceToStatementForUpdate        STATE_INSTANCE_TO_STATEMENT_FOR_UPDATE
                                                                                               = new StateInstanceToStatementForUpdate();
    private static final ResultSetToStateInstance                 RESULT_SET_TO_STATE_INSTANCE = new ResultSetToStateInstance();
    private SagaTransactionalTemplate sagaTransactionalTemplate;
    private Serializer<Object, String>    paramsSerializer    = new ParamsSerializer();
    private Serializer<Exception, byte[]> exceptionSerializer = new ExceptionSerializer();
    private StateLogStoreSqls stateLogStoreSqls;
    private String            defaultTenantId;
    private SeqGenerator      seqGenerator;

    @Override
    public void recordStateMachineStarted(StateMachineInstance machineInstance, ProcessContext context) {

        if (machineInstance != null) {
            //if parentId is not null, machineInstance is a SubStateMachine, do not start a new global transaction,
            //use parent transaction instead.
            String parentId = machineInstance.getParentId();
            if (StringUtils.hasLength(parentId)) {
                if (StringUtils.isEmpty(machineInstance.getId())) {
                    machineInstance.setId(parentId);
                }
            } else {
                beginTransaction(machineInstance, context);
            }


            if (StringUtils.isEmpty(machineInstance.getId()) && seqGenerator != null) {
                machineInstance.setId(seqGenerator.generate(DomainConstants.SEQ_ENTITY_STATE_MACHINE_INST));
            }

            // save to db
            machineInstance.setSerializedStartParams(paramsSerializer.serialize(machineInstance.getStartParams()));
            executeUpdate(stateLogStoreSqls.getRecordStateMachineStartedSql(dbType),
                    STATE_MACHINE_INSTANCE_TO_STATEMENT_FOR_INSERT, machineInstance);
        }
    }

    protected void beginTransaction(StateMachineInstance machineInstance, ProcessContext context) {

        if (sagaTransactionalTemplate != null) {

            StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(
                    DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
            TransactionInfo transactionInfo = new TransactionInfo();
            transactionInfo.setTimeOut(stateMachineConfig.getTransOperationTimeout());
            transactionInfo.setName(Constants.SAGA_TRANS_NAME_PREFIX + machineInstance.getStateMachine().getName());
            try {
                GlobalTransaction globalTransaction = sagaTransactionalTemplate.beginTransaction(transactionInfo);
                machineInstance.setId(globalTransaction.getXid());

                context.setVariable(DomainConstants.VAR_NAME_GLOBAL_TX, globalTransaction);
                Map<String, Object> machineContext = machineInstance.getContext();
                if (machineContext != null) {
                    machineContext.put(DomainConstants.VAR_NAME_GLOBAL_TX, globalTransaction);
                }

            } catch (ExecutionException e) {

                String xid = null;
                if (e.getTransaction() != null) {
                    xid = e.getTransaction().getXid();
                }
                throw new EngineExecutionException(e,
                        e.getCode() + ", TransName:" + transactionInfo.getName() + ", XID: " + xid + ", Reason: " + e
                                .getMessage(), FrameworkErrorCode.TransactionManagerError);
            }
            finally {
                if (Boolean.TRUE.equals(context.getVariable(DomainConstants.VAR_NAME_IS_ASYNC_EXECUTION))) {
                    RootContext.unbind();
                }
            }
        }
    }

    @Override
    public void recordStateMachineFinished(StateMachineInstance machineInstance, ProcessContext context) {

        if (machineInstance != null) {
            // save to db
            Map<String, Object> endParams = machineInstance.getEndParams();
            if (endParams != null) {
                endParams.remove(DomainConstants.VAR_NAME_GLOBAL_TX);
            }

            // if success, clear exception
            if (ExecutionStatus.SU.equals(machineInstance.getStatus()) && machineInstance.getException() != null) {
                machineInstance.setException(null);
            }

            machineInstance.setSerializedEndParams(paramsSerializer.serialize(machineInstance.getEndParams()));
            machineInstance.setSerializedException(exceptionSerializer.serialize(machineInstance.getException()));
            int effect = executeUpdate(stateLogStoreSqls.getRecordStateMachineFinishedSql(dbType),
                    STATE_MACHINE_INSTANCE_TO_STATEMENT_FOR_UPDATE, machineInstance);
            if (effect < 1) {
                LOGGER.warn("StateMachineInstance[{}] is recovery by server, skip recordStateMachineFinished.", machineInstance.getId());
            } else {
                StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(
                        DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
                if (EngineUtils.isTimeout(machineInstance.getGmtUpdated(), stateMachineConfig.getTransOperationTimeout())) {
                    LOGGER.warn("StateMachineInstance[{}] is execution timeout, skip report transaction finished to server.", machineInstance.getId());
                } else if (StringUtils.isEmpty(machineInstance.getParentId())) {
                    //if parentId is not null, machineInstance is a SubStateMachine, do not report global transaction.
                    reportTransactionFinished(machineInstance, context);
                }
            }
            RootContext.unbind();
        }
    }

    protected void reportTransactionFinished(StateMachineInstance machineInstance, ProcessContext context) {

        if (sagaTransactionalTemplate != null) {

            GlobalTransaction globalTransaction = null;
            try {
                globalTransaction = getGlobalTransaction(machineInstance, context);
                if (globalTransaction == null) {

                    throw new EngineExecutionException("Global transaction is not exists",
                            FrameworkErrorCode.ObjectNotExists);
                }

                GlobalStatus globalStatus;
                if (ExecutionStatus.SU.equals(machineInstance.getStatus())
                        && machineInstance.getCompensationStatus() == null) {
                    globalStatus = GlobalStatus.Committed;
                } else if (ExecutionStatus.SU.equals(machineInstance.getCompensationStatus())) {
                    globalStatus = GlobalStatus.Rollbacked;
                } else if (ExecutionStatus.FA.equals(machineInstance.getCompensationStatus()) || ExecutionStatus.UN
                        .equals(machineInstance.getCompensationStatus())) {
                    globalStatus = GlobalStatus.RollbackRetrying;
                } else if (ExecutionStatus.FA.equals(machineInstance.getStatus())
                        && machineInstance.getCompensationStatus() == null) {
                    globalStatus = GlobalStatus.Finished;
                } else if (ExecutionStatus.UN.equals(machineInstance.getStatus())
                        && machineInstance.getCompensationStatus() == null) {
                    globalStatus = GlobalStatus.CommitRetrying;
                } else {
                    globalStatus = GlobalStatus.UnKnown;
                }
                sagaTransactionalTemplate.reportTransaction(globalTransaction, globalStatus);
            } catch (ExecutionException e) {
                LOGGER.error("Report transaction finish to server error: ",
                        e.getCode() + ", StateMachine:" + machineInstance.getStateMachine().getName() + ", XID: "
                                + machineInstance.getId() + ", Reason: " + e.getMessage(), e);
            } catch (TransactionException e) {
                LOGGER.error(
                        "Report transaction finish to server error: " + e.getCode() + ", StateMachine:" + machineInstance
                                .getStateMachine().getName() + ", XID: " + machineInstance.getId() + ", Reason: " + e
                                .getMessage(), e);
            } finally {
                // clear
                RootContext.unbind();
                if (globalTransaction != null) {
                    sagaTransactionalTemplate.triggerAfterCompletion(globalTransaction);
                }
                sagaTransactionalTemplate.cleanUp();
            }
        }
    }

    @Override
    public void recordStateMachineRestarted(StateMachineInstance machineInstance, ProcessContext context) {

        if (machineInstance != null) {
            //save to db
            Date gmtUpdated = new Date();
            int effect = executeUpdate(stateLogStoreSqls.getUpdateStateMachineRunningStatusSql(dbType), machineInstance.isRunning(), new Timestamp(gmtUpdated.getTime()),
                    machineInstance.getId(), new Timestamp(machineInstance.getGmtUpdated().getTime()));
            if (effect < 1) {
                throw new EngineExecutionException(
                        "StateMachineInstance [id:" + machineInstance.getId() + "] is recovered by an other execution, restart denied", FrameworkErrorCode.OperationDenied);
            }
            machineInstance.setGmtUpdated(gmtUpdated);
        }
    }

    @Override
    public void recordStateStarted(StateInstance stateInstance, ProcessContext context) {

        if (stateInstance != null) {

            //if this state is for retry, do not register branch, but generate id
            if (StringUtils.hasLength(stateInstance.getStateIdRetriedFor())) {

                stateInstance.setId(generateRetryStateInstanceId(stateInstance));
            }
            //if this state is for compensation, do not register branch, but generate id
            else if (StringUtils.hasLength(stateInstance.getStateIdCompensatedFor())) {

                stateInstance.setId(generateCompensateStateInstanceId(stateInstance));
            }
            else {
                branchRegister(stateInstance, context);
            }


            if (StringUtils.isEmpty(stateInstance.getId()) && seqGenerator != null) {
                stateInstance.setId(seqGenerator.generate(DomainConstants.SEQ_ENTITY_STATE_INST));
            }

            stateInstance.setSerializedInputParams(paramsSerializer.serialize(stateInstance.getInputParams()));
            executeUpdate(stateLogStoreSqls.getRecordStateStartedSql(dbType), STATE_INSTANCE_TO_STATEMENT_FOR_INSERT,
                    stateInstance);
        }
    }

    protected void branchRegister(StateInstance stateInstance, ProcessContext context) {

        if (sagaTransactionalTemplate != null) {

            StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(
                    DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

            if (stateMachineConfig instanceof DbStateMachineConfig
                    && !((DbStateMachineConfig)stateMachineConfig).isSagaBranchRegisterEnable()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("sagaBranchRegisterEnable = false, skip register branch. state[" + stateInstance.getName() + "]");
                }
                return;
            }

            //Register branch
            try {
                StateMachineInstance machineInstance = stateInstance.getStateMachineInstance();
                GlobalTransaction globalTransaction = getGlobalTransaction(machineInstance, context);
                if (globalTransaction == null) {
                    throw new EngineExecutionException("Global transaction is not exists", FrameworkErrorCode.ObjectNotExists);
                }

                String resourceId = stateInstance.getStateMachineInstance().getStateMachine().getName() + "#" + stateInstance.getName();
                long branchId = sagaTransactionalTemplate.branchRegister(resourceId, null, globalTransaction.getXid(), null, null);
                stateInstance.setId(String.valueOf(branchId));
            } catch (TransactionException e) {
                throw new EngineExecutionException(e,
                        "Branch transaction error: " + e.getCode() + ", StateMachine:" + stateInstance.getStateMachineInstance()
                                .getStateMachine().getName() + ", XID: " + stateInstance.getStateMachineInstance().getId() + ", State:"
                                + stateInstance.getName() + ", stateId: " + stateInstance.getId() + ", Reason: " + e.getMessage(),
                        FrameworkErrorCode.TransactionManagerError);
            } catch (ExecutionException e) {
                throw new EngineExecutionException(e,
                        "Branch transaction error: " + e.getCode() + ", StateMachine:" + stateInstance.getStateMachineInstance()
                                .getStateMachine().getName() + ", XID: " + stateInstance.getStateMachineInstance().getId() + ", State:"
                                + stateInstance.getName() + ", stateId: " + stateInstance.getId() + ", Reason: " + e.getMessage(),
                        FrameworkErrorCode.TransactionManagerError);
            }
        }
    }

    protected GlobalTransaction getGlobalTransaction(StateMachineInstance machineInstance, ProcessContext context)
            throws ExecutionException, TransactionException {

        GlobalTransaction globalTransaction = (GlobalTransaction) context.getVariable(DomainConstants.VAR_NAME_GLOBAL_TX);
        if (globalTransaction == null) {
            String xid;
            String parentId = machineInstance.getParentId();
            if (StringUtils.isEmpty(parentId)) {
                xid = machineInstance.getId();
            } else {
                xid = parentId.substring(0, parentId.lastIndexOf(DomainConstants.SEPERATOR_PARENT_ID));
            }
            globalTransaction = sagaTransactionalTemplate.reloadTransaction(xid);
            if (globalTransaction != null) {
                context.setVariable(DomainConstants.VAR_NAME_GLOBAL_TX, globalTransaction);
            }
        }
        return globalTransaction;
    }

    /**
     * generate retry state instance id based on original state instance id
     * ${originalStateInstanceId}.${retryCount}
     * @param stateInstance
     * @return
     */
    private String generateRetryStateInstanceId(StateInstance stateInstance) {

        String originalStateInstId = stateInstance.getStateIdRetriedFor();
        int maxIndex = 1;
        Map<String, StateInstance> stateInstanceMap = stateInstance.getStateMachineInstance().getStateMap();
        StateInstance originalStateInst = stateInstanceMap.get(stateInstance.getStateIdRetriedFor());
        while (StringUtils.hasLength(originalStateInst.getStateIdRetriedFor())) {
            originalStateInst = stateInstanceMap.get(originalStateInst.getStateIdRetriedFor());
            int idIndex = getIdIndex(originalStateInst.getId(), ".");
            maxIndex = idIndex > maxIndex ? idIndex : maxIndex;
            maxIndex++;
        }
        if (originalStateInst != null) {
            originalStateInstId = originalStateInst.getId();
        }
        return originalStateInstId + "." + maxIndex;
    }

    /**
     * generate compensate state instance id based on original state instance id
     * ${originalStateInstanceId}-${retryCount}
     * @param stateInstance
     * @return
     */
    private String generateCompensateStateInstanceId(StateInstance stateInstance) {

        String originalCompensateStateInstId = stateInstance.getStateIdCompensatedFor();
        int maxIndex = 1;
        for (StateInstance aStateInstance : stateInstance.getStateMachineInstance().getStateList()) {
            if (aStateInstance != stateInstance
                    && originalCompensateStateInstId.equals(aStateInstance.getStateIdCompensatedFor())) {
                int idIndex = getIdIndex(aStateInstance.getId(), "-");
                maxIndex = idIndex > maxIndex ? idIndex : maxIndex;
                maxIndex++;
            }
        }
        return originalCompensateStateInstId + "-" + maxIndex;
    }

    private int getIdIndex(String stateInstanceId, String separator) {

        if (StringUtils.hasLength(stateInstanceId)) {
            int start = stateInstanceId.lastIndexOf(separator);
            if (start > 0) {
                String indexStr = stateInstanceId.substring(start + 1, stateInstanceId.length());
                try {
                    return Integer.parseInt(indexStr);
                } catch (NumberFormatException e) {
                    LOGGER.warn("get stateInstance id index failed", e);
                }
            }
        }
        return -1;
    }

    @Override
    public void recordStateFinished(StateInstance stateInstance, ProcessContext context) {

        if (stateInstance != null) {

            stateInstance.setSerializedOutputParams(paramsSerializer.serialize(stateInstance.getOutputParams()));
            stateInstance.setSerializedException(exceptionSerializer.serialize(stateInstance.getException()));
            executeUpdate(stateLogStoreSqls.getRecordStateFinishedSql(dbType), STATE_INSTANCE_TO_STATEMENT_FOR_UPDATE,
                    stateInstance);

            //A switch to skip branch report on branch success, in order to optimize performance
            StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(
                    DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
            if (!(stateMachineConfig instanceof DbStateMachineConfig
                    && !((DbStateMachineConfig)stateMachineConfig).isRmReportSuccessEnable()
                    && ExecutionStatus.SU.equals(stateInstance.getStatus()))) {
                branchReport(stateInstance, context);
            }
        }
    }

    protected void branchReport(StateInstance stateInstance, ProcessContext context) {

        if (sagaTransactionalTemplate != null) {

            StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(
                    DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

            if (stateMachineConfig instanceof DbStateMachineConfig
                    && !((DbStateMachineConfig)stateMachineConfig).isSagaBranchRegisterEnable()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("sagaBranchRegisterEnable = false, skip branch report. state[" + stateInstance.getName() + "]");
                }
                return;
            }

            BranchStatus branchStatus = null;
            //find out the original state instance, only the original state instance is registered on the server, and its status should
            // be reported.
            StateInstance originalStateInst = null;
            if (StringUtils.hasLength(stateInstance.getStateIdRetriedFor())) {

                originalStateInst = findOutOriginalStateInstanceOfRetryState(stateInstance);

                if (ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                    branchStatus = BranchStatus.PhaseTwo_Committed;
                } else if (ExecutionStatus.FA.equals(stateInstance.getStatus()) || ExecutionStatus.UN.equals(
                        stateInstance.getStatus())) {
                    branchStatus = BranchStatus.PhaseOne_Failed;
                } else {
                    branchStatus = BranchStatus.Unknown;
                }

            } else if (StringUtils.hasLength(stateInstance.getStateIdCompensatedFor())) {

                originalStateInst = findOutOriginalStateInstanceOfCompensateState(stateInstance);
            }

            if (originalStateInst == null) {
                originalStateInst = stateInstance;
            }

            if (branchStatus == null) {
                if (ExecutionStatus.SU.equals(originalStateInst.getStatus()) && originalStateInst.getCompensationStatus() == null) {
                    branchStatus = BranchStatus.PhaseTwo_Committed;
                } else if (ExecutionStatus.SU.equals(originalStateInst.getCompensationStatus())) {
                    branchStatus = BranchStatus.PhaseTwo_Rollbacked;
                } else if (ExecutionStatus.FA.equals(originalStateInst.getCompensationStatus())
                        || ExecutionStatus.UN.equals(originalStateInst.getCompensationStatus())) {
                    branchStatus = BranchStatus.PhaseTwo_RollbackFailed_Retryable;
                } else if ((ExecutionStatus.FA.equals(originalStateInst.getStatus()) || ExecutionStatus.UN.equals(
                        originalStateInst.getStatus()))
                        && originalStateInst.getCompensationStatus() == null) {
                    branchStatus = BranchStatus.PhaseOne_Failed;
                } else {
                    branchStatus = BranchStatus.Unknown;
                }
            }

            try {
                StateMachineInstance machineInstance = stateInstance.getStateMachineInstance();
                GlobalTransaction globalTransaction = getGlobalTransaction(machineInstance, context);

                if (globalTransaction == null) {
                    throw new EngineExecutionException("Global transaction is not exists", FrameworkErrorCode.ObjectNotExists);
                }

                sagaTransactionalTemplate.branchReport(globalTransaction.getXid(), Long.parseLong(originalStateInst.getId()), branchStatus,
                        null);
            } catch (TransactionException e) {
                LOGGER.error(
                        "Report branch status to server error: {}, StateMachine:{}, StateName:{}, XID: {}, branchId: {}, branchStatus:{},"
                                + " Reason:{} "
                        , e.getCode()
                        , originalStateInst.getStateMachineInstance().getStateMachine().getName()
                        , originalStateInst.getName()
                        , originalStateInst.getStateMachineInstance().getId()
                        , originalStateInst.getId()
                        , branchStatus
                        , e.getMessage()
                        , e);
            } catch (ExecutionException e) {
                LOGGER.error(
                        "Report branch status to server error: {}, StateMachine:{}, StateName:{}, XID: {}, branchId: {}, branchStatus:{},"
                                + " Reason:{} "
                        , e.getCode()
                        , originalStateInst.getStateMachineInstance().getStateMachine().getName()
                        , originalStateInst.getName()
                        , originalStateInst.getStateMachineInstance().getId()
                        , originalStateInst.getId()
                        , branchStatus
                        , e.getMessage()
                        , e);
            }
        }
    }

    private StateInstance findOutOriginalStateInstanceOfRetryState(StateInstance stateInstance) {

        StateInstance originalStateInst;
        Map<String, StateInstance> stateInstanceMap = stateInstance.getStateMachineInstance().getStateMap();
        originalStateInst = stateInstanceMap.get(stateInstance.getStateIdRetriedFor());
        while (StringUtils.hasLength(originalStateInst.getStateIdRetriedFor())) {
            originalStateInst = stateInstanceMap.get(originalStateInst.getStateIdRetriedFor());
        }
        return originalStateInst;
    }

    private StateInstance findOutOriginalStateInstanceOfCompensateState(StateInstance stateInstance) {

        StateInstance originalStateInst;
        Map<String, StateInstance> stateInstanceMap = stateInstance.getStateMachineInstance().getStateMap();
        originalStateInst = stateInstance.getStateMachineInstance().getStateMap().get(stateInstance.getStateIdCompensatedFor());
        while (StringUtils.hasLength(originalStateInst.getStateIdRetriedFor())) {
            originalStateInst = stateInstanceMap.get(originalStateInst.getStateIdRetriedFor());
        }
        return originalStateInst;
    }

    @Override
    public StateMachineInstance getStateMachineInstance(String stateMachineInstanceId) {

        StateMachineInstance stateMachineInstance = selectOne(stateLogStoreSqls.getGetStateMachineInstanceByIdSql(dbType),
                RESULT_SET_TO_STATE_MACHINE_INSTANCE, stateMachineInstanceId);
        if (stateMachineInstance == null) {
            return null;
        }
        List<StateInstance> stateInstanceList = queryStateInstanceListByMachineInstanceId(stateMachineInstanceId);
        for (StateInstance stateInstance : stateInstanceList) {
            stateMachineInstance.putStateInstance(stateInstance.getId(), stateInstance);
        }
        deserializeParamsAndException(stateMachineInstance);

        return stateMachineInstance;
    }

    @Override
    public StateMachineInstance getStateMachineInstanceByBusinessKey(String businessKey, String tenantId) {

        if (StringUtils.isEmpty(tenantId)) {
            tenantId = defaultTenantId;
        }
        StateMachineInstance stateMachineInstance = selectOne(
                stateLogStoreSqls.getGetStateMachineInstanceByBusinessKeySql(dbType), RESULT_SET_TO_STATE_MACHINE_INSTANCE,
                businessKey, tenantId);
        if (stateMachineInstance == null) {
            return null;
        }
        List<StateInstance> stateInstanceList = queryStateInstanceListByMachineInstanceId(stateMachineInstance.getId());
        for (StateInstance stateInstance : stateInstanceList) {
            stateMachineInstance.putStateInstance(stateInstance.getId(), stateInstance);
        }
        deserializeParamsAndException(stateMachineInstance);

        return stateMachineInstance;
    }

    private void deserializeParamsAndException(StateMachineInstance stateMachineInstance) {
        byte[] serializedException = (byte[]) stateMachineInstance.getSerializedException();
        if (serializedException != null) {
            stateMachineInstance.setException((Exception) exceptionSerializer.deserialize(serializedException));
        }

        String serializedStartParams = (String) stateMachineInstance.getSerializedStartParams();
        if (StringUtils.hasLength(serializedStartParams)) {
            stateMachineInstance.setStartParams(
                    (Map<String, Object>) paramsSerializer.deserialize(serializedStartParams));
        }

        String serializedEndParams = (String) stateMachineInstance.getSerializedEndParams();
        if (StringUtils.hasLength(serializedEndParams)) {
            stateMachineInstance.setEndParams((Map<String, Object>) paramsSerializer.deserialize(serializedEndParams));
        }
    }

    @Override
    public List<StateMachineInstance> queryStateMachineInstanceByParentId(String parentId) {
        return selectList(stateLogStoreSqls.getQueryStateMachineInstancesByParentIdSql(dbType),
                RESULT_SET_TO_STATE_MACHINE_INSTANCE, parentId);
    }

    @Override
    public StateInstance getStateInstance(String stateInstanceId, String machineInstId) {

        StateInstance stateInstance = selectOne(
                stateLogStoreSqls.getGetStateInstanceByIdAndMachineInstanceIdSql(dbType), RESULT_SET_TO_STATE_INSTANCE,
                machineInstId, stateInstanceId);
        deserializeParamsAndException(stateInstance);
        return stateInstance;
    }

    private void deserializeParamsAndException(StateInstance stateInstance) {
        if (stateInstance != null) {
            String inputParams = (String) stateInstance.getSerializedInputParams();
            if (StringUtils.hasLength(inputParams)) {
                stateInstance.setInputParams(paramsSerializer.deserialize(inputParams));
            }
            String outputParams = (String) stateInstance.getSerializedOutputParams();
            if (StringUtils.hasLength(outputParams)) {
                stateInstance.setOutputParams(paramsSerializer.deserialize(outputParams));
            }
            byte[] serializedException = (byte[]) stateInstance.getSerializedException();
            if (serializedException != null) {
                stateInstance.setException((Exception) exceptionSerializer.deserialize(serializedException));
            }
        }
    }

    @Override
    public List<StateInstance> queryStateInstanceListByMachineInstanceId(String stateMachineInstanceId) {

        List<StateInstance> stateInstanceList = selectList(
                stateLogStoreSqls.getQueryStateInstancesByMachineInstanceIdSql(dbType), RESULT_SET_TO_STATE_INSTANCE,
                stateMachineInstanceId);

        if (stateInstanceList == null || stateInstanceList.size() == 0) {
            return stateInstanceList;
        }
        StateInstance lastStateInstance = stateInstanceList.get(stateInstanceList.size() - 1);
        if (lastStateInstance.getGmtEnd() == null) {
            lastStateInstance.setStatus(ExecutionStatus.RU);
        }
        Map<String, StateInstance> originStateMap = new HashMap<>();
        Map<String/* originStateId */, StateInstance/* compensatedState */> compensatedStateMap = new HashMap<>();
        Map<String/* originStateId */, StateInstance/* retriedState */> retriedStateMap = new HashMap<>();
        for (int i = 0; i < stateInstanceList.size(); i++) {
            StateInstance tempStateInstance = stateInstanceList.get(i);

            deserializeParamsAndException(tempStateInstance);

            if (StringUtils.hasText(tempStateInstance.getStateIdCompensatedFor())) {
                putLastStateToMap(compensatedStateMap, tempStateInstance, tempStateInstance.getStateIdCompensatedFor());
            } else {
                if (StringUtils.hasText(tempStateInstance.getStateIdRetriedFor())) {
                    putLastStateToMap(retriedStateMap, tempStateInstance, tempStateInstance.getStateIdRetriedFor());
                }
                originStateMap.put(tempStateInstance.getId(), tempStateInstance);
            }
        }

        if (compensatedStateMap.size() != 0) {
            for (StateInstance origState : originStateMap.values()) {
                origState.setCompensationState(compensatedStateMap.get(origState.getId()));
            }
        }

        if (retriedStateMap.size() != 0) {
            for (StateInstance origState : originStateMap.values()) {
                if (retriedStateMap.containsKey(origState.getId())) {
                    origState.setIgnoreStatus(true);
                }
            }
        }
        return stateInstanceList;
    }

    private void putLastStateToMap(Map<String, StateInstance> resultMap, StateInstance newState, String key) {

        if (!resultMap.containsKey(key)) {

            resultMap.put(key, newState);
        } else if (newState.getGmtEnd().after(resultMap.get(key).getGmtEnd())) {

            StateInstance oldState = resultMap.remove(key);
            oldState.setIgnoreStatus(true);

            resultMap.put(key, newState);
        } else {

            newState.setIgnoreStatus(true);
        }
    }

    public void setExceptionSerializer(Serializer<Exception, byte[]> exceptionSerializer) {
        this.exceptionSerializer = exceptionSerializer;
    }

    public SagaTransactionalTemplate getSagaTransactionalTemplate() {
        return sagaTransactionalTemplate;
    }

    public void setSagaTransactionalTemplate(SagaTransactionalTemplate sagaTransactionalTemplate) {
        this.sagaTransactionalTemplate = sagaTransactionalTemplate;
    }

    public Serializer<Object, String> getParamsSerializer() {
        return paramsSerializer;
    }

    public void setParamsSerializer(Serializer<Object, String> paramsSerializer) {
        this.paramsSerializer = paramsSerializer;
    }

    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    public void setSeqGenerator(SeqGenerator seqGenerator) {
        this.seqGenerator = seqGenerator;
    }

    @Override
    public void setTablePrefix(String tablePrefix) {
        super.setTablePrefix(tablePrefix);
        this.stateLogStoreSqls = new StateLogStoreSqls(tablePrefix);
    }

    private static class StateMachineInstanceToStatementForInsert implements ObjectToStatement<StateMachineInstance> {
        @Override
        public void toStatement(StateMachineInstance stateMachineInstance, PreparedStatement statement)
                throws SQLException {
            statement.setString(1, stateMachineInstance.getId());
            statement.setString(2, stateMachineInstance.getMachineId());
            statement.setString(3, stateMachineInstance.getTenantId());
            statement.setString(4, stateMachineInstance.getParentId());
            statement.setTimestamp(5, new Timestamp(stateMachineInstance.getGmtStarted().getTime()));
            statement.setString(6, stateMachineInstance.getBusinessKey());
            statement.setObject(7, stateMachineInstance.getSerializedStartParams());
            statement.setBoolean(8, stateMachineInstance.isRunning());
            statement.setString(9, stateMachineInstance.getStatus().name());
            statement.setTimestamp(10, new Timestamp(stateMachineInstance.getGmtUpdated().getTime()));
        }
    }

    private static class StateMachineInstanceToStatementForUpdate implements ObjectToStatement<StateMachineInstance> {
        @Override
        public void toStatement(StateMachineInstance stateMachineInstance, PreparedStatement statement)
                throws SQLException {
            statement.setTimestamp(1, new Timestamp(stateMachineInstance.getGmtEnd().getTime()));
            statement.setBytes(2, stateMachineInstance.getSerializedException() != null ? (byte[]) stateMachineInstance
                    .getSerializedException() : null);
            statement.setObject(3, stateMachineInstance.getSerializedEndParams());
            statement.setString(4, stateMachineInstance.getStatus().name());
            statement.setString(5,
                    stateMachineInstance.getCompensationStatus() != null ? stateMachineInstance.getCompensationStatus()
                            .name() : null);
            statement.setBoolean(6, stateMachineInstance.isRunning());
            statement.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            statement.setString(8, stateMachineInstance.getId());
            statement.setTimestamp(9, new Timestamp(stateMachineInstance.getGmtUpdated().getTime()));
        }
    }

    private static class StateInstanceToStatementForInsert implements ObjectToStatement<StateInstance> {
        @Override
        public void toStatement(StateInstance stateInstance, PreparedStatement statement) throws SQLException {
            statement.setString(1, stateInstance.getId());
            statement.setString(2, stateInstance.getMachineInstanceId());
            statement.setString(3, stateInstance.getName());
            statement.setString(4, stateInstance.getType());
            statement.setTimestamp(5, new Timestamp(stateInstance.getGmtStarted().getTime()));
            statement.setString(6, stateInstance.getServiceName());
            statement.setString(7, stateInstance.getServiceMethod());
            statement.setString(8, stateInstance.getServiceType());
            statement.setBoolean(9, stateInstance.isForUpdate());
            statement.setObject(10, stateInstance.getSerializedInputParams());
            statement.setString(11, stateInstance.getStatus().name());
            statement.setString(12, stateInstance.getBusinessKey());
            statement.setString(13, stateInstance.getStateIdCompensatedFor());
            statement.setString(14, stateInstance.getStateIdRetriedFor());
        }
    }

    private static class StateInstanceToStatementForUpdate implements ObjectToStatement<StateInstance> {
        @Override
        public void toStatement(StateInstance stateInstance, PreparedStatement statement) throws SQLException {
            statement.setTimestamp(1, new Timestamp(stateInstance.getGmtEnd().getTime()));
            statement.setBytes(2,
                    stateInstance.getException() != null ? (byte[]) stateInstance.getSerializedException() : null);
            statement.setString(3, stateInstance.getStatus().name());
            statement.setObject(4, stateInstance.getSerializedOutputParams());
            statement.setString(5, stateInstance.getId());
            statement.setString(6, stateInstance.getMachineInstanceId());
        }
    }

    private static class ResultSetToStateMachineInstance implements ResultSetToObject<StateMachineInstance> {
        @Override
        public StateMachineInstance toObject(ResultSet resultSet) throws SQLException {

            StateMachineInstanceImpl stateMachineInstance = new StateMachineInstanceImpl();
            stateMachineInstance.setId(resultSet.getString("id"));
            stateMachineInstance.setMachineId(resultSet.getString("machine_id"));
            stateMachineInstance.setTenantId(resultSet.getString("tenant_id"));
            stateMachineInstance.setParentId(resultSet.getString("parent_id"));
            stateMachineInstance.setBusinessKey(resultSet.getString("business_key"));
            stateMachineInstance.setGmtStarted(resultSet.getTimestamp("gmt_started"));
            stateMachineInstance.setGmtEnd(resultSet.getTimestamp("gmt_end"));
            stateMachineInstance.setStatus(ExecutionStatus.valueOf(resultSet.getString("status")));

            String compensationStatusName = resultSet.getString("compensation_status");
            if (StringUtils.hasLength(compensationStatusName)) {
                stateMachineInstance.setCompensationStatus(ExecutionStatus.valueOf(compensationStatusName));
            }
            stateMachineInstance.setRunning(resultSet.getBoolean("is_running"));
            stateMachineInstance.setGmtUpdated(resultSet.getTimestamp("gmt_updated"));

            if (resultSet.getMetaData().getColumnCount() > 11) {
                stateMachineInstance.setSerializedStartParams(resultSet.getString("start_params"));
                stateMachineInstance.setSerializedEndParams(resultSet.getString("end_params"));
                stateMachineInstance.setSerializedException(resultSet.getBytes("excep"));
            }
            return stateMachineInstance;
        }
    }

    private static class ResultSetToStateInstance implements ResultSetToObject<StateInstance> {
        @Override
        public StateInstance toObject(ResultSet resultSet) throws SQLException {

            StateInstanceImpl stateInstance = new StateInstanceImpl();
            stateInstance.setId(resultSet.getString("id"));
            stateInstance.setMachineInstanceId(resultSet.getString("machine_inst_id"));
            stateInstance.setName(resultSet.getString("name"));
            stateInstance.setType(resultSet.getString("type"));
            stateInstance.setBusinessKey(resultSet.getString("business_key"));
            stateInstance.setStatus(ExecutionStatus.valueOf(resultSet.getString("status")));
            stateInstance.setGmtStarted(resultSet.getTimestamp("gmt_started"));
            stateInstance.setGmtEnd(resultSet.getTimestamp("gmt_end"));
            stateInstance.setServiceName(resultSet.getString("service_name"));
            stateInstance.setServiceMethod(resultSet.getString("service_method"));
            stateInstance.setServiceType(resultSet.getString("service_type"));
            stateInstance.setForUpdate(resultSet.getBoolean("is_for_update"));
            stateInstance.setStateIdCompensatedFor(resultSet.getString("state_id_compensated_for"));
            stateInstance.setStateIdRetriedFor(resultSet.getString("state_id_retried_for"));
            stateInstance.setSerializedInputParams(resultSet.getString("input_params"));
            stateInstance.setSerializedOutputParams(resultSet.getString("output_params"));
            stateInstance.setSerializedException(resultSet.getBytes("excep"));

            return stateInstance;
        }
    }
}
