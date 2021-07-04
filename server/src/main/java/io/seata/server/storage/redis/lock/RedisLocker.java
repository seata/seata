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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import com.google.common.collect.Lists;
import io.seata.common.exception.StoreException;
import io.seata.common.io.FileLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.LambdaUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.BranchTransactionException;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.model.LockStatus;
import io.seata.core.store.LockDO;
import io.seata.server.storage.redis.JedisPooledFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;


import static io.seata.common.Constants.ROW_LOCK_KEY_SPLIT_CHAR;
import static io.seata.core.exception.TransactionExceptionCode.LockKeyConflictFailFast;

/**
 * The redis lock store operation
 *
 * @author funkye
 * @author wangzhongxiang
 */
public class RedisLocker extends AbstractLocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLocker.class);

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

    private static final String STATUS = "status";

    private static final String ROW_KEY = "rowKey";

    private static final String REDIS_LUA_FILE_NAME = "lua/redislocker/redislock.lua";

    private static String ACQUIRE_LOCK_SHA;

    private static final String WHITE_SPACE = " ";

    private static final String ANNOTATION_LUA = "--";

    /**
     * Instantiates a new Redis locker.
     */
    public RedisLocker() {
        if (ACQUIRE_LOCK_SHA == null) {
            File luaFile = FileLoader.load(REDIS_LUA_FILE_NAME);
            if (luaFile != null) {
                StringBuilder acquireLockLuaByFile = new StringBuilder();
                try (FileInputStream fis = new FileInputStream(luaFile)) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().startsWith(ANNOTATION_LUA)) {
                            continue;
                        }
                        acquireLockLuaByFile.append(line);
                        acquireLockLuaByFile.append(WHITE_SPACE);
                    }
                // if it fails to read the file, pipeline mode is used
                } catch (IOException e) {
                    LOGGER.info("redis locker use pipeline mode");
                    return;
                }
                try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                    ACQUIRE_LOCK_SHA = jedis.scriptLoad(acquireLockLuaByFile.toString());
                    LOGGER.info("redis locker use lua mode");
                }
            } else {
                LOGGER.info("redis locker use pipeline mode");
            }
        }
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks) {
        return acquireLock(rowLocks, true);
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks, boolean autoCommit) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            return true;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            if (ACQUIRE_LOCK_SHA != null && autoCommit) {
                return acquireLockByLua(jedis, rowLocks);
            } else {
                return acquireLockByPipeline(jedis, rowLocks, autoCommit);
            }
        }
    }

    private boolean acquireLockByPipeline(Jedis jedis, List<RowLock> rowLocks, boolean autoCommit) {
        String needLockXid = rowLocks.get(0).getXid();
        Long branchId = rowLocks.get(0).getBranchId();
        List<LockDO> needLockDOS = convertToLockDO(rowLocks);
        if (needLockDOS.size() > 1) {
            needLockDOS = needLockDOS.stream().
                filter(LambdaUtils.distinctByKey(LockDO::getRowKey))
                .collect(Collectors.toList());
        }
        List<String> needLockKeys = new ArrayList<>();
        needLockDOS.forEach(lockDO -> needLockKeys.add(buildLockKey(lockDO.getRowKey())));

        Pipeline pipeline1 = jedis.pipelined();
        needLockKeys.stream().forEachOrdered(needLockKey -> {
            pipeline1.hget(needLockKey, XID);
            if (!autoCommit) {
                pipeline1.hget(needLockKey, STATUS);
            }
        });
        List<List<String>> existedLockInfos =
            Lists.partition((List<String>)(List)pipeline1.syncAndReturnAll(), autoCommit ? 1 : 2);
        Map<String, LockDO> needAddLock = new HashMap<>(needLockKeys.size(), 1);
        boolean failFast = false;
        boolean canLock = true;
        for (int i = 0; i < needLockKeys.size(); i++) {
            List<String> results = existedLockInfos.get(i);
            String existedLockXid = CollectionUtils.isEmpty(results) ? null : existedLockInfos.get(i).get(0);
            if (StringUtils.isEmpty(existedLockXid)) {
                // If empty,we need to lock this row
                needAddLock.put(needLockKeys.get(i), needLockDOS.get(i));
            } else {
                if (!StringUtils.equals(existedLockXid, needLockXid)) {
                    if (!autoCommit) {
                        String status = existedLockInfos.get(i).get(1);
                        if (StringUtils.equals(status, String.valueOf(LockStatus.Rollbacking.getCode()))) {
                            failFast = true;
                            break;
                        }
                    }
                    // If not equals,means the rowkey is holding by another global transaction
                    if (canLock) {
                        canLock = false;
                        if (autoCommit) {
                            break;
                        }
                    }
                }
            }
        }
        if (failFast) {
            throw new StoreException(new BranchTransactionException(LockKeyConflictFailFast));
        }
        if (!canLock) {
            return canLock;
        }
        if (needAddLock.isEmpty()) {
            return true;
        }
        Pipeline pipeline = jedis.pipelined();
        List<String> readyKeys = new ArrayList<>(needAddLock.keySet());
        needAddLock.forEach((key, value) -> {
            pipeline.hsetnx(key, XID, value.getXid());
            pipeline.hsetnx(key, TRANSACTION_ID, value.getTransactionId().toString());
            pipeline.hsetnx(key, BRANCH_ID, value.getBranchId().toString());
            pipeline.hsetnx(key, STATUS, String.valueOf(LockStatus.Locked.getCode()));
            pipeline.hset(key, ROW_KEY, value.getRowKey());
            pipeline.hset(key, RESOURCE_ID, value.getResourceId());
            pipeline.hset(key, TABLE_NAME, value.getTableName());
            pipeline.hset(key, PK, value.getPk());
        });
        List<Integer> results = (List<Integer>) (List) pipeline.syncAndReturnAll();
        List<List<Integer>> partitions = Lists.partition(results, 8);

        ArrayList<String> success = new ArrayList<>(partitions.size());
        Integer status = SUCCEED;
        for (int i = 0; i < partitions.size(); i++) {
            if (Objects.equals(partitions.get(i).get(0),FAILED)) {
                status = FAILED;
            } else {
                success.add(readyKeys.get(i));
            }
        }

        //If someone has failed,all the lockkey which has been added need to be delete.
        if (FAILED.equals(status)) {
            if (success.size() > 0) {
                jedis.del(success.toArray(new String[0]));
            }
            return false;
        }
        String xidLockKey = buildXidLockKey(needLockXid);
        StringJoiner lockKeysString = new StringJoiner(ROW_LOCK_KEY_SPLIT_CHAR);
        needLockKeys.forEach(lockKeysString::add);
        jedis.hset(xidLockKey, branchId.toString(), lockKeysString.toString());
        return true;
    }

    private boolean acquireLockByLua(Jedis jedis, List<RowLock> rowLocks) {
        String needLockXid = rowLocks.get(0).getXid();
        Long branchId = rowLocks.get(0).getBranchId();
        List<LockDO> needLockDOs = rowLocks.stream()
                .map(this::convertToLockDO)
                .filter(LambdaUtils.distinctByKey(LockDO::getRowKey))
                .collect(Collectors.toList());
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> args = new ArrayList<>();
        int size = needLockDOs.size();
        args.add(String.valueOf(size));
        // args index 2 placeholder
        args.add(null);
        args.add(needLockXid);
        for (LockDO lockDO : needLockDOs) {
            keys.add(buildLockKey(lockDO.getRowKey()));
            args.add(lockDO.getTransactionId().toString());
            args.add(lockDO.getBranchId().toString());
            args.add(lockDO.getResourceId());
            args.add(lockDO.getTableName());
            args.add(lockDO.getRowKey());
            args.add(lockDO.getPk());
        }
        String xidLockKey = buildXidLockKey(needLockXid);
        StringJoiner lockKeysString = new StringJoiner(ROW_LOCK_KEY_SPLIT_CHAR);
        needLockDOs.stream().map(lockDO -> buildLockKey(lockDO.getRowKey())).forEach(lockKeysString::add);
        keys.add(xidLockKey);
        keys.add(branchId.toString());
        args.add(lockKeysString.toString());
        // reset args index 2
        args.set(1, String.valueOf(args.size()));
        long result = (long)jedis.evalsha(ACQUIRE_LOCK_SHA, keys, args);
        return SUCCEED == result;
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

    @Override
    public boolean updateLockStatus(String xid, LockStatus lockStatus) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = buildXidLockKey(xid);
            Map<String, String> branchAndLockKeys = jedis.hgetAll(xidLockKey);
            if (CollectionUtils.isNotEmpty(branchAndLockKeys)) {
                try (Pipeline pipeline = jedis.pipelined()) {
                    branchAndLockKeys.values()
                        .forEach(k -> pipeline.hset(k, STATUS, String.valueOf(lockStatus.getCode())));
                    pipeline.sync();
                }
            }
            return true;
        }
    }

    private String buildXidLockKey(String xid) {
        return DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX + xid;
    }

    private String buildLockKey(String rowKey) {
        return DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX + rowKey;
    }

}
