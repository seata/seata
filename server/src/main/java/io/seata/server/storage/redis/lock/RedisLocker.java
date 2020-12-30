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
package io.seata.server.storage.redis.lock;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.LambdaUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.LockDO;
import io.seata.core.store.redis.JedisPooledFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import static io.seata.common.Constants.ROW_LOCK_KEY_SPLIT_CHAR;

/**
 * The redis lock store operation
 *
 * @author funkye
 * @author wangzhongxiang
 */
public class RedisLocker extends AbstractLocker {

    private static final Integer SUCCEED = 1;

    private static final Integer FAILED = 0;

    private static final String DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX = "SEATA_ROW_LOCK_";

    private static final String DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX = "SEATA_GLOBAL_LOCK";

    private static final String XID = "xid";

    private static final String TRANSACTION_ID = "transactionId";

    private static final String BRANCH_ID = "branchId";

    private static final String RESOURCE_ID = "resourceId";

    private static final String TABLE_NAME = "tableName";

    private static final String PK = "pk";

    private static final String ROW_KEY = "rowKey";

    /**
     * Instantiates a new Redis locker.
     */
    public RedisLocker() {
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return true;
        }
        Integer status = SUCCEED;
        String needLockXid = rowLocks.get(0).getXid();
        Long branchId = rowLocks.get(0).getBranchId();

        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<LockDO> needLockDOS = convertToLockDO(rowLocks);
            if (needLockDOS.size() > 1) {
                needLockDOS = needLockDOS.stream().
                        filter(LambdaUtils.distinctByKey(LockDO::getRowKey))
                        .collect(Collectors.toList());
            }
            List<String> needLockKeys = new ArrayList<>();
            needLockDOS.forEach(lockDO -> needLockKeys.add(buildLockKey(lockDO.getRowKey())));

            Pipeline pipeline1 = jedis.pipelined();
            needLockKeys.stream().forEachOrdered(needLockKey -> pipeline1.hget(needLockKey, XID));
            List<String> existedLockInfos = (List<String>) (List) pipeline1.syncAndReturnAll();
            Map<String, LockDO> needAddLock = new HashMap<>(needLockKeys.size(), 1);

            for (int i = 0; i < needLockKeys.size(); i++) {
                String existedLockXid = existedLockInfos.get(i);
                if (StringUtils.isEmpty(existedLockXid)) {
                    //If empty,we need to lock this row
                    needAddLock.put(needLockKeys.get(i), needLockDOS.get(i));
                } else {
                    if (!StringUtils.equals(existedLockXid, needLockXid)) {
                        //If not equals,means the rowkey is holding by another global transaction
                        return false;
                    }
                }
            }

            if (needAddLock.isEmpty()) {
                return true;
            }
            Pipeline pipeline = jedis.pipelined();
            List<String> readyKeys = new ArrayList<>();
            needAddLock.forEach((key, value) -> {
                pipeline.hsetnx(key, XID, value.getXid());
                pipeline.hsetnx(key, TRANSACTION_ID, value.getTransactionId().toString());
                pipeline.hsetnx(key, BRANCH_ID, value.getBranchId().toString());
                pipeline.hsetnx(key, RESOURCE_ID, value.getResourceId());
                pipeline.hsetnx(key, TABLE_NAME, value.getTableName());
                pipeline.hsetnx(key, ROW_KEY, value.getRowKey());
                pipeline.hsetnx(key, PK, value.getPk());
                readyKeys.add(key);
            });
            List<Integer> results = (List<Integer>) (List) pipeline.syncAndReturnAll();
            List<List<Integer>> partitions = Lists.partition(results, 7);

            String[] success = new String[partitions.size()];
            for (int i = 0; i < partitions.size(); i++) {
                String key = readyKeys.get(i);
                if (partitions.get(i).contains(FAILED)) {
                    status = FAILED;
                } else {
                    success[0] = key;
                }
            }

            //If someone has failed,all the lockkey which has been added need to be delete.
            if (FAILED.equals(status)) {
                if (success.length > 0) {
                    jedis.del(success);
                }
                return false;
            }
            String xidLockKey = buildXidLockKey(needLockXid);
            StringJoiner lockKeysString = new StringJoiner(ROW_LOCK_KEY_SPLIT_CHAR);
            needLockKeys.forEach(lockKeysString::add);
            jedis.hset(xidLockKey, branchId.toString(), lockKeysString.toString());
            return true;
        }
    }

    @Override
    public boolean releaseLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return true;
        }
        String currentXid = rowLocks.get(0).getXid();
        Long branchId = rowLocks.get(0).getBranchId();
        List<LockDO> needReleaseLocks = convertToLockDO(rowLocks);
        String[] needReleaseKeys = new String[needReleaseLocks.size()];
        for (int i = 0; i < needReleaseLocks.size(); i ++) {
            needReleaseKeys[i] = buildLockKey(needReleaseLocks.get(i).getRowKey());
        }

        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Pipeline pipelined = jedis.pipelined();
            pipelined.del(needReleaseKeys);
            pipelined.hdel(buildXidLockKey(currentXid), branchId.toString());
            pipelined.sync();
            return true;
        }
    }

    @Override
    public boolean releaseLock(String xid, List<Long> branchIds) {
        if (CollectionUtils.isEmpty(branchIds)) {
            return true;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(xid);
            String[] branchIdsArray = new String[branchIds.size()];
            for (int i = 0; i < branchIds.size(); i++) {
                branchIdsArray[i] = branchIds.get(i).toString();
            }
            List<String> rowKeys = jedis.hmget(xidLockKey, branchIdsArray);

            if (CollectionUtils.isNotEmpty(rowKeys)) {
                Pipeline pipelined = jedis.pipelined();
                pipelined.hdel(xidLockKey, branchIdsArray);
                rowKeys.forEach(rowKeyStr -> {
                    if (StringUtils.isNotEmpty(rowKeyStr)) {
                        if (rowKeyStr.contains(ROW_LOCK_KEY_SPLIT_CHAR)) {
                            String[] keys = rowKeyStr.split(ROW_LOCK_KEY_SPLIT_CHAR);
                            pipelined.del(keys);
                        } else {
                            pipelined.del(rowKeyStr);
                        }
                    }
                });
                pipelined.sync();
            }
            return true;
        }
    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        List<Long> branchIds = new ArrayList<>();
        branchIds.add(branchId);
        return releaseLock(xid, branchIds);
    }

    @Override
    public boolean isLockable(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return true;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<LockDO> locks = convertToLockDO(rowLocks);
            Set<String> lockKeys = new HashSet<>();
            for (LockDO rowlock : locks) {
                lockKeys.add(buildLockKey(rowlock.getRowKey()));
            }

            String xid = rowLocks.get(0).getXid();
            Pipeline pipeline = jedis.pipelined();
            lockKeys.forEach(key -> pipeline.hget(key, XID));
            List<String> existedXids = (List<String>) (List) pipeline.syncAndReturnAll();
            return existedXids.stream().allMatch(existedXid -> existedXid == null || xid.equals(existedXid));
        }
    }

    private String buildXidLockKey(String xid) {
        return DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX + xid;
    }

    private String buildLockKey(String rowKey) {
        return DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX + rowKey;
    }

}
