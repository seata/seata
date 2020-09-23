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

import com.alibaba.fastjson.JSON;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.store.AbstractLogStore;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalCondition;
import io.seata.core.store.GlobalTransactionDO;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The type Log store redis dao.
 *
 * @author funkye
 * @author wang.liang
 */
public class LogStoreRedisDAO extends AbstractLogStore {

    //region Constants

    // global transaction prefix
    private static final String DEFAULT_REDIS_SEATA_GLOBAL_PREFIX = "SEATA_GLOBAL_";

    // the prefix of the branchs transaction
    private static final String DEFAULT_REDIS_SEATA_XID_BRANCHS_PREFIX = "SEATA_XID_BRANCHS_";

    // the prefix of the branch transaction
    private static final String DEFAULT_REDIS_SEATA_BRANCH_PREFIX = "SEATA_BRANCH_";

    // global transaction id PREFIX
    private static final String DEFAULT_SEATA_TRANSACTION_ID_GLOBAL_PREFIX = "SEATA_TRANSACTION_ID_GLOBAL_";

    // initial cursor
    private static final String INITIAL_CURSOR = "0";

    //endregion

    //region Fields

    /**
     * The jedis.
     */
    private final Jedis jedis;

    /**
     * The log query limit.
     */
    private final int logQueryLimit;

    //endregion

    //region Constructor

    public LogStoreRedisDAO(Jedis jedis, int logQueryLimit) {
        this.jedis = jedis;
        this.logQueryLimit = logQueryLimit;
    }

    //endregion

    //region Override LogStore

    @Override
    public GlobalTransactionDO queryGlobalTransactionDO(String xid) {
        String globalSessionJson = jedis.get(getGlobalKeyByXid(xid));
        if (StringUtils.isBlank(globalSessionJson)) {
            return null;
        }
        return JSON.parseObject(globalSessionJson, GlobalTransactionDO.class);
    }

    @Override
    public GlobalTransactionDO queryGlobalTransactionDO(long transactionId) {
        String globalSessionJson = jedis.get(getGlobalKeyByTransactionId(transactionId));
        if (StringUtils.isBlank(globalSessionJson)) {
            return null;
        }
        return JSON.parseObject(globalSessionJson, GlobalTransactionDO.class);
    }

    @Override
    public List<GlobalTransactionDO> queryGlobalTransactionDO(GlobalCondition condition) {
        Set<String> keys = new HashSet<>();
        String cursor = INITIAL_CURSOR;
        ScanParams params = new ScanParams();
        params.count(condition.getPageSize()); // limit
        params.match(getGlobalKeyByXid("*"));
        ScanResult<String> scans;
        do {
            scans = jedis.scan(cursor, params);
            keys.addAll(scans.getResult());
            cursor = scans.getCursor();
        } while (!INITIAL_CURSOR.equals(cursor));

        if (CollectionUtils.isNotEmpty(keys)) {
            List<GlobalTransactionDO> globalTransactionDOs = new ArrayList<>();
            // get and match
            for (String globalKey : keys) {
                GlobalTransactionDO globalTransactionDO = JSON.parseObject(jedis.get(globalKey), GlobalTransactionDO.class);
                if (globalTransactionDO != null && condition.isMatch(globalTransactionDO)) {
                    globalTransactionDOs.add(globalTransactionDO);
                }
            }
            // order by
            globalTransactionDOs = condition.doSort(globalTransactionDOs);
            return globalTransactionDOs;
        } else {
            return null;
        }
    }

    @Override
    public int countGlobalTransactionDO(GlobalCondition condition) {
        // TODO:
        return 0;
    }

    @Override
    public boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String keys = getGlobalKeyByXid(globalTransactionDO.getXid());
        Pipeline pipeline = jedis.pipelined();
        String json = JSON.toJSONString(globalTransactionDO);
        pipeline.set(keys, json);
        keys = getGlobalKeyByTransactionId(globalTransactionDO.getTransactionId());
        pipeline.set(keys, json);
        pipeline.sync();
        return true;
    }

    @Override
    public boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        return insertGlobalTransactionDO(globalTransactionDO);
    }

    @Override
    public boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String[] keys = new String[3];
        keys[0] = getGlobalKeyByXid(globalTransactionDO.getXid());
        keys[1] = getGlobalKeyByTransactionId(globalTransactionDO.getTransactionId());
        keys[2] = getBranchListKeyByXid(globalTransactionDO.getXid());
        jedis.del(keys);
        return true;
    }

    @Override
    public List<BranchTransactionDO> queryBranchTransactionDO(String xid) {
        Set<String> keys = lRange(jedis, getBranchListKeyByXid(xid));
        if (CollectionUtils.isNotEmpty(keys)) {
            return getBranchJsons(jedis, keys);
        } else {
            return null;
        }
    }

    @Override
    public List<BranchTransactionDO> queryBranchTransactionDO(List<String> xids) {
        List<BranchTransactionDO> branchTransactionDOs = new ArrayList<>();
        for (String xid : xids) {
            Set<String> branches = lRange(jedis, getBranchListKeyByXid(xid));
            if (CollectionUtils.isNotEmpty(branches)) {
                branchTransactionDOs.addAll(getBranchJsons(jedis, branches));
            }
        }
        return branchTransactionDOs;
    }

    @Override
    public boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String key = getBranchKey(branchTransactionDO.getBranchId());
        Pipeline pipeline = jedis.pipelined();
        if (jedis.get(key) == null) {
            pipeline.lpush(getBranchListKeyByXid(branchTransactionDO.getXid()), key);
        }
        pipeline.set(key, JSON.toJSONString(branchTransactionDO));
        pipeline.sync();
        return true;
    }

    @Override
    public boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        return insertBranchTransactionDO(branchTransactionDO);
    }

    @Override
    public boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String key = getBranchKey(branchTransactionDO.getBranchId());
        jedis.del(key);
        return true;
    }

    //endregion

    //region Private

    private List<BranchTransactionDO> getBranchJsons(Jedis jedis, Set<String> keys) {
        List<BranchTransactionDO> branchTransactionDOs = new ArrayList<>();
        List<String> branchJsons = jedis.mget(keys.toArray(new String[0]));
        for (String branchJson : branchJsons) {
            if (!StringUtils.isEmpty(branchJson)) {
                branchTransactionDOs.add(JSON.parseObject(branchJson, BranchTransactionDO.class));
            }
        }
        return branchTransactionDOs;
    }

    private String getGlobalKeyByXid(String xid) {
        return DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + xid;
    }

    private String getBranchListKeyByXid(String xid) {
        return DEFAULT_REDIS_SEATA_XID_BRANCHS_PREFIX + xid;
    }

    private String getGlobalKeyByTransactionId(Long transactionId) {
        return DEFAULT_SEATA_TRANSACTION_ID_GLOBAL_PREFIX + transactionId;
    }

    private String getBranchKey(Long branchId) {
        return DEFAULT_REDIS_SEATA_BRANCH_PREFIX + branchId;
    }

    private Set<String> lRange(Jedis jedis, String key) {
        Set<String> keys = new HashSet<>();
        List<String> redisBranchJson;
        int start = 0;
        int stop = logQueryLimit;
        do {
            redisBranchJson = jedis.lrange(key, start, stop);
            keys.addAll(redisBranchJson);
            start = keys.size();
            stop = start + logQueryLimit;
        } while (CollectionUtils.isNotEmpty(redisBranchJson));
        return keys;
    }

    //endregion
}
