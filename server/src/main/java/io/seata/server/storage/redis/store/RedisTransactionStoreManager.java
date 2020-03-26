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
package io.seata.server.storage.redis.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.alibaba.fastjson.JSON;
import io.seata.common.exception.StoreException;
import io.seata.common.util.StringUtils;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import redis.clients.jedis.Jedis;

/**
 * @author funkye
 * @date 2020/3/26
 */
public class RedisTransactionStoreManager  extends AbstractTransactionStoreManager
    implements TransactionStoreManager {

    private static String DEFAULT_REDIS_SEATA_GLOBAL_PREFIX = "SEATA_GLOBAL_";

    private static String DEFAULT_REDIS_SEATA_BRANCH_PREFIX = "SEATA_BRANCH_";

    private static volatile RedisTransactionStoreManager instance;

    /**
     * Get the instance.
     */
    public static RedisTransactionStoreManager getInstance() {
        if (null == instance) {
            synchronized (RedisTransactionStoreManager.class) {
                if (null == instance) {
                    instance = new RedisTransactionStoreManager();
                }
            }
        }
        return instance;
    }

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        if (LogOperation.GLOBAL_ADD.equals(logOperation)) {
            return insertOrUpdateGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_UPDATE.equals(logOperation)) {
            return insertOrUpdateGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_REMOVE.equals(logOperation)) {
            return deleteGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.BRANCH_ADD.equals(logOperation)) {
            return insertOrUpdateBranchTransactionDO(convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_UPDATE.equals(logOperation)) {
            return insertOrUpdateBranchTransactionDO(convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_REMOVE.equals(logOperation)) {
            return deleteBranchTransactionDO(convertBranchTransactionDO(session));
        } else {
            throw new StoreException("Unknown LogOperation:" + logOperation.name());
        }
    }

    private boolean deleteBranchTransactionDO(BranchTransactionDO convertBranchTransactionDO) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.del(DEFAULT_REDIS_SEATA_BRANCH_PREFIX + convertBranchTransactionDO.getXid()
                + convertBranchTransactionDO.getBranchId());
            return true;
        }
    }

    private boolean insertOrUpdateBranchTransactionDO(BranchTransactionDO convertBranchTransactionDO) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.set(DEFAULT_REDIS_SEATA_BRANCH_PREFIX + convertBranchTransactionDO.getXid()
                + "_"+convertBranchTransactionDO.getBranchId(), JSON.toJSONString(convertBranchTransactionDO));
            return true;
        }
    }

    private boolean deleteGlobalTransactionDO(GlobalTransactionDO convertGlobalTransactionDO) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.del(DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + convertGlobalTransactionDO.getXid());
            return true;
        }
    }

    private boolean insertOrUpdateGlobalTransactionDO(GlobalTransactionDO convertGlobalTransactionDO) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.set(DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + convertGlobalTransactionDO.getXid(),
                JSON.toJSONString(convertGlobalTransactionDO));
            return true;
        }
    }

    @Override
    public long getCurrentMaxSessionId() {
        return 0;
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
        String globalSessionJson;
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            globalSessionJson = jedis.get(DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + xid);
            if(StringUtils.isBlank(globalSessionJson)){
                return null;
            }
            //global transaction
            GlobalTransactionDO globalTransactionDO = JSON.parseObject(globalSessionJson,GlobalTransactionDO.class);
            if (globalTransactionDO == null) {
                return null;
            }
            //branch transactions
            List<BranchTransactionDO> branchTransactionDOs = null;
            //reduce rpc with db when branchRegister and getGlobalStatus
            if (withBranchSessions) {
                Set<String> branchJson=jedis.keys("*"+globalTransactionDO.getXid()+"*");
                if (null != branchJson && branchJson.size() > 0) {
                    branchTransactionDOs = new ArrayList<>();
                    for (String s : branchJson) {
                        branchTransactionDOs.add(JSON.parseObject(jedis.get(s), BranchTransactionDO.class));
                    }
                }
            }
            return getGlobalSession(globalTransactionDO, branchTransactionDOs);
        }
    }

    @Override
    public List<GlobalSession> readSession(SessionCondition sessionCondition) {
        String globalSessionJson;
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            if (StringUtils.isNotBlank(sessionCondition.getXid())) {
                globalSessionJson = jedis.get(DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + sessionCondition.getXid());
                if (StringUtils.isNotBlank(globalSessionJson)) {
                    GlobalSession session =
                        convertGlobalSession(JSON.parseObject(globalSessionJson, GlobalTransactionDO.class));
                    List<GlobalSession> globalSessions = new ArrayList<>();
                    globalSessions.add(session);
                    return globalSessions;
                }
            } else if (sessionCondition.getTransactionId() != null) {
                String BranchTransactionDOJson =
                    jedis.get(DEFAULT_REDIS_SEATA_BRANCH_PREFIX + sessionCondition.getTransactionId());
                if (StringUtils.isNotBlank(BranchTransactionDOJson)) {
                    BranchTransactionDO branch = JSON.parseObject(BranchTransactionDOJson, BranchTransactionDO.class);
                    globalSessionJson = jedis.get(DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + sessionCondition.getXid());
                    if (StringUtils.isNotBlank(globalSessionJson)) {
                        GlobalSession session =
                            convertGlobalSession(JSON.parseObject(globalSessionJson, GlobalTransactionDO.class));
                        List<GlobalSession> globalSessions = new ArrayList<>();
                        globalSessions.add(session);
                        return globalSessions;
                    }
                }
            }
        }
        return null;
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
}
