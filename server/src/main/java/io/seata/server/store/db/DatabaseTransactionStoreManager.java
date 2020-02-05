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
package io.seata.server.store.db;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import io.seata.common.exception.StoreException;
import io.seata.common.executor.Initialize;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.core.store.LogStore;
import io.seata.core.store.StoreMode;
import io.seata.core.store.db.DataSourceGenerator;
import io.seata.server.UUIDGenerator;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;

/**
 * The type Database transaction store manager.
 *
 * @author zhangsen
 */
@LoadLevel(name = "db")
public class DatabaseTransactionStoreManager extends AbstractTransactionStoreManager
    implements TransactionStoreManager, Initialize {

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The constant DEFAULT_LOG_QUERY_LIMIT.
     */
    protected static final int DEFAULT_LOG_QUERY_LIMIT = 100;

    /**
     * is inited
     */
    protected AtomicBoolean inited = new AtomicBoolean(false);

    /**
     * The Log store.
     */
    protected LogStore logStore;

    /**
     * The Log query limit.
     */
    protected int logQueryLimit;

    /**
     * Instantiates a new Database transaction store manager.
     */
    public DatabaseTransactionStoreManager() {
    }

    @Override
    public synchronized void init() {
        if (inited.get()) {
            return;
        }
        logQueryLimit = CONFIG.getInt(ConfigurationKeys.STORE_DB_LOG_QUERY_LIMIT, DEFAULT_LOG_QUERY_LIMIT);
        String datasourceType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        //init dataSource
        DataSourceGenerator dataSourceGenerator = EnhancedServiceLoader.load(DataSourceGenerator.class, datasourceType);
        DataSource logStoreDataSource = dataSourceGenerator.generateDataSource();
        logStore = EnhancedServiceLoader.load(LogStore.class, StoreMode.DB.name(), new Class[] {DataSource.class},
            new Object[] {logStoreDataSource});
        inited.set(true);
    }

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        if (LogOperation.GLOBAL_ADD.equals(logOperation)) {
            return logStore.insertGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_UPDATE.equals(logOperation)) {
            return logStore.updateGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_REMOVE.equals(logOperation)) {
            return logStore.deleteGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.BRANCH_ADD.equals(logOperation)) {
            return logStore.insertBranchTransactionDO(convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_UPDATE.equals(logOperation)) {
            return logStore.updateBranchTransactionDO(convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_REMOVE.equals(logOperation)) {
            return logStore.deleteBranchTransactionDO(convertBranchTransactionDO(session));
        } else {
            throw new StoreException("Unknown LogOperation:" + logOperation.name());
        }
    }

    /**
     * Read session global session.
     *
     * @param transactionId the transaction id
     * @return the global session
     */
    public GlobalSession readSession(Long transactionId) {
        //global transaction
        GlobalTransactionDO globalTransactionDO = logStore.queryGlobalTransactionDO(transactionId);
        if (globalTransactionDO == null) {
            return null;
        }
        //branch transactions
        List<BranchTransactionDO> branchTransactionDOs = logStore.queryBranchTransactionDO(
            globalTransactionDO.getXid());
        return getGlobalSession(globalTransactionDO, branchTransactionDOs);
    }

    /**
     * Read session global session.
     *
     * @param xid the xid
     * @return the global session
     */
    @Override
    public GlobalSession readSession(String xid) {
        return this.readSession(xid, true);
    }

    /**
     * Read session global session.
     *
     * @param xid the xid
     * @param withBranchSessions the withBranchSessions
     * @return the global session
     */
    @Override
    public GlobalSession readSession(String xid, boolean withBranchSessions) {
        //global transaction
        GlobalTransactionDO globalTransactionDO = logStore.queryGlobalTransactionDO(xid);
        if (globalTransactionDO == null) {
            return null;
        }
        //branch transactions
        List<BranchTransactionDO> branchTransactionDOs = null;
        //reduce rpc with db when branchRegister and getGlobalStatus
        if (withBranchSessions) {
            branchTransactionDOs = logStore.queryBranchTransactionDO(globalTransactionDO.getXid());
        }
        return getGlobalSession(globalTransactionDO, branchTransactionDOs);
    }

    /**
     * Read session list.
     *
     * @param statuses the statuses
     * @return the list
     */
    public List<GlobalSession> readSession(GlobalStatus[] statuses) {
        int[] states = new int[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            states[i] = statuses[i].getCode();
        }
        //global transaction
        List<GlobalTransactionDO> globalTransactionDOs = logStore.queryGlobalTransactionDO(states, logQueryLimit);
        if (CollectionUtils.isEmpty(globalTransactionDOs)) {
            return null;
        }
        List<String> xids = globalTransactionDOs.stream().map(GlobalTransactionDO::getXid).collect(Collectors.toList());
        List<BranchTransactionDO> branchTransactionDOs = logStore.queryBranchTransactionDO(xids);
        Map<String, List<BranchTransactionDO>> branchTransactionDOsMap = branchTransactionDOs.stream()
            .collect(Collectors.groupingBy(BranchTransactionDO::getXid, LinkedHashMap::new, Collectors.toList()));
        return globalTransactionDOs.stream().map(globalTransactionDO ->
            getGlobalSession(globalTransactionDO, branchTransactionDOsMap.get(globalTransactionDO.getXid())))
            .collect(Collectors.toList());
    }

    @Override
    public List<GlobalSession> readSession(SessionCondition sessionCondition) {
        if (StringUtils.isNotBlank(sessionCondition.getXid())) {
            GlobalSession globalSession = readSession(sessionCondition.getXid());
            if (globalSession != null) {
                List<GlobalSession> globalSessions = new ArrayList<>();
                globalSessions.add(globalSession);
                return globalSessions;
            }
        } else if (sessionCondition.getTransactionId() != null) {
            GlobalSession globalSession = readSession(sessionCondition.getTransactionId());
            if (globalSession != null) {
                List<GlobalSession> globalSessions = new ArrayList<>();
                globalSessions.add(globalSession);
                return globalSessions;
            }
        } else if (CollectionUtils.isNotEmpty(sessionCondition.getStatuses())) {
            return readSession(sessionCondition.getStatuses());
        }
        return null;
    }

    @Override
    public long getCurrentMaxSessionId() {
        //check max transId or branchId
        return logStore.getCurrentMaxSessionId(UUIDGenerator.getMaxUUID(), UUIDGenerator.getInitUUID());
    }

    private GlobalSession getGlobalSession(GlobalTransactionDO globalTransactionDO,
                                           List<BranchTransactionDO> branchTransactionDOs) {
        GlobalSession globalSession = convertGlobalSession(globalTransactionDO);
        //branch transactions
        if (branchTransactionDOs != null && branchTransactionDOs.size() > 0) {
            for (BranchTransactionDO branchTransactionDO : branchTransactionDOs) {
                globalSession.add(convertBranchSession(branchTransactionDO));
            }
        }
        return globalSession;
    }

    private GlobalSession convertGlobalSession(GlobalTransactionDO globalTransactionDO) {
        GlobalSession session = new GlobalSession(globalTransactionDO.getApplicationId(),
            globalTransactionDO.getTransactionServiceGroup(),
            globalTransactionDO.getTransactionName(),
            globalTransactionDO.getTimeout());
        session.setTransactionId(globalTransactionDO.getTransactionId());
        session.setXid(globalTransactionDO.getXid());
        session.setStatus(GlobalStatus.get(globalTransactionDO.getStatus()));
        session.setApplicationData(globalTransactionDO.getApplicationData());
        session.setBeginTime(globalTransactionDO.getBeginTime());
        session.setVersion(globalTransactionDO.getVersion());
        return session;
    }

    private BranchSession convertBranchSession(BranchTransactionDO branchTransactionDO) {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(branchTransactionDO.getXid());
        branchSession.setTransactionId(branchTransactionDO.getTransactionId());
        branchSession.setApplicationData(branchTransactionDO.getApplicationData());
        branchSession.setBranchId(branchTransactionDO.getBranchId());
        branchSession.setBranchType(BranchType.valueOf(branchTransactionDO.getBranchType()));
        branchSession.setResourceId(branchTransactionDO.getResourceId());
        branchSession.setClientId(branchTransactionDO.getClientId());
        branchSession.setResourceGroupId(branchTransactionDO.getResourceGroupId());
        branchSession.setStatus(BranchStatus.get(branchTransactionDO.getStatus()));
        return branchSession;
    }

    private GlobalTransactionDO convertGlobalTransactionDO(SessionStorable session) {
        if (session == null || !(session instanceof GlobalSession)) {
            throw new IllegalArgumentException(
                "the parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        GlobalSession globalSession = (GlobalSession)session;

        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid(globalSession.getXid());
        globalTransactionDO.setStatus(globalSession.getStatus().getCode());
        globalTransactionDO.setApplicationId(globalSession.getApplicationId());
        globalTransactionDO.setBeginTime(globalSession.getBeginTime());
        globalTransactionDO.setTimeout(globalSession.getTimeout());
        globalTransactionDO.setTransactionId(globalSession.getTransactionId());
        globalTransactionDO.setTransactionName(globalSession.getTransactionName());
        globalTransactionDO.setTransactionServiceGroup(globalSession.getTransactionServiceGroup());
        globalTransactionDO.setApplicationData(globalSession.getApplicationData());
        globalTransactionDO.setVersion(globalSession.getVersion());
        return globalTransactionDO;
    }

    private BranchTransactionDO convertBranchTransactionDO(SessionStorable session) {
        if (session == null || !(session instanceof BranchSession)) {
            throw new IllegalArgumentException(
                "the parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        BranchSession branchSession = (BranchSession)session;

        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setXid(branchSession.getXid());
        branchTransactionDO.setBranchId(branchSession.getBranchId());
        branchTransactionDO.setBranchType(branchSession.getBranchType().name());
        branchTransactionDO.setClientId(branchSession.getClientId());
        branchTransactionDO.setResourceGroupId(branchSession.getResourceGroupId());
        branchTransactionDO.setTransactionId(branchSession.getTransactionId());
        branchTransactionDO.setApplicationData(branchSession.getApplicationData());
        branchTransactionDO.setResourceId(branchSession.getResourceId());
        branchTransactionDO.setStatus(branchSession.getStatus().getCode());
        return branchTransactionDO;
    }

    /**
     * Sets log store.
     *
     * @param logStore the log store
     */
    public void setLogStore(LogStore logStore) {
        this.logStore = logStore;
    }

    /**
     * Sets log query limit.
     *
     * @param logQueryLimit the log query limit
     */
    public void setLogQueryLimit(int logQueryLimit) {
        this.logQueryLimit = logQueryLimit;
    }
}
