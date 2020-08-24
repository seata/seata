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
package io.seata.tc.storage.redis.lock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.LambdaUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.LockDO;
import io.seata.tc.storage.redis.JedisPooledFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * @author funkye
 */
public class RedisLocker extends AbstractLocker {

    private static final Integer DEFAULT_QUERY_LIMIT = 100;

    private static final String DEFAULT_REDIS_SEATA_LOCK_PREFIX = "SEATA_LOCK_";

    private static final String DEFAULT_REDIS_SEATA_LOCK_XID_PREFIX = "SEATA_LOCK_XID_";

    /**
     * The query limit.
     */
    private int logQueryLimit;

    /**
     * Instantiates a new Redis locker.
     */
    public RedisLocker() {
        logQueryLimit =
            ConfigurationFactory.getInstance().getInt(ConfigurationKeys.STORE_REDIS_QUERY_LIMIT, DEFAULT_QUERY_LIMIT);
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            // no lock
            return true;
        }
        Set<String> successList = new HashSet<>();
        long status = 1;
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<LockDO> locks = convertToLockDO(rowLocks);
            if (locks.size() > 1) {
                locks =
                    locks.stream().filter(LambdaUtils.distinctByKey(LockDO::getRowKey)).collect(Collectors.toList());
            }
            List<String> existedKeyList = new ArrayList<>();
            locks.forEach(lockDO -> {
                existedKeyList.add(getLockKey(lockDO.getRowKey()));
            });
            List<String> lockList = jedis.mget(existedKeyList.toArray(new String[0]));
            Map<String, String> map = new HashMap<>(existedKeyList.size(), 1);
            for (int i = 0; i < existedKeyList.size(); i++) {
                String existedValue = lockList.get(i);
                if (existedValue == null) {
                    String key = existedKeyList.get(i);
                    map.put(key, JSON.toJSONString(locks.get(i)));
                } else {
                    LockDO existed = JSON.parseObject(existedValue, LockDO.class);
                    if (!StringUtils.equals(existed.getXid(), locks.get(i).getXid())) {
                        return false;
                    }
                }
            }
            if (map.isEmpty()) {
                return true;
            }
            Pipeline pipeline = jedis.pipelined();
            List<String> readyKeys = new ArrayList<>();
            map.forEach((key, value) -> {
                pipeline.setnx(key, value);
                readyKeys.add(key);
            });
            List<Object> results = pipeline.syncAndReturnAll();
            for (int i = 0; i < results.size(); i++) {
                Long result = (long)results.get(i);
                String key = readyKeys.get(i);
                if (result != 1) {
                    status = result;
                } else {
                    successList.add(key);
                }
            }
            if (status != 1) {
                String[] rms = successList.toArray(new String[0]);
                if (rms.length > 0) {
                    jedis.del(rms);
                }
                return false;
            } else {
                try {
                    String xidLockKey = getXidLockKey(locks.get(0).getXid());
                    jedis.lpush(xidLockKey, readyKeys.toArray(new String[0]));
                } catch (Exception e) {
                    return false;
                }
                return true;
            }
        }
    }

    @Override
    public boolean releaseLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            // no lock
            return true;
        }
        String[] keys = new String[rowLocks.size()];
        List<LockDO> locks = convertToLockDO(rowLocks);
        for (int i = 0; i < locks.size(); i++) {
            String key = getLockKey(locks.get(i).getRowKey());
            keys[i] = key;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String xidLockKey = getXidLockKey(locks.get(0).getXid());
            Pipeline pipeline = jedis.pipelined();
            pipeline.del(keys);
            Arrays.stream(keys).forEach(key -> pipeline.lrem(xidLockKey, 0, key));
            pipeline.sync();
            return true;
        }
    }

    @Override
    public boolean releaseLock(String xid, List<Long> branchIds) {
        if (CollectionUtils.isEmpty(branchIds)) {
            // no lock
            return true;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String lockListKey = getXidLockKey(xid);
            Set<String> keys = lRange(jedis, lockListKey);
            if (CollectionUtils.isNotEmpty(keys)) {
                List<String> delKeys = new ArrayList<>();
                List<String> values = jedis.mget(keys.toArray(new String[0]));
                for (String value : values) {
                    Iterator<Long> it = branchIds.iterator();
                    LockDO lock = JSON.parseObject(value, LockDO.class);
                    while (it.hasNext()) {
                        Long branchId = it.next();
                        if (lock != null && Objects.equals(lock.getBranchId(), branchId)) {
                            delKeys.add(getLockKey(lock.getRowKey()));
                            break;
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(delKeys)) {
                    Pipeline pipeline = jedis.pipelined();
                    pipeline.del(delKeys.toArray(new String[0]));
                    for (String key : delKeys) {
                        pipeline.lrem(lockListKey, 0, key);
                    }
                    pipeline.sync();
                }
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
            // no lock
            return true;
        }
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<LockDO> locks = convertToLockDO(rowLocks);
            Set<String> lockKeys = new HashSet<>();
            for (LockDO rowlock : locks) {
                lockKeys.add(getLockKey(rowlock.getRowKey()));
            }
            List<String> rowlockJsons = jedis.mget(lockKeys.toArray(new String[0]));
            String xid = rowLocks.get(0).getXid();
            for (String rowlockJson : rowlockJsons) {
                if (!StringUtils.isEmpty(rowlockJson)) {
                    LockDO lock = JSON.parseObject(rowlockJson, LockDO.class);
                    if (lock != null && !Objects.equals(lock.getXid(), xid)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private Set<String> lRange(Jedis jedis, String key) {
        Set<String> keys = new HashSet<>();
        List<String> redisLockJson;
        int start = 0;
        int stop = logQueryLimit;
        do {
            redisLockJson = jedis.lrange(key, start, stop);
            keys.addAll(redisLockJson);
            start = keys.size();
            stop = start + logQueryLimit;
        } while (CollectionUtils.isNotEmpty(redisLockJson));
        return keys;
    }

    private String getXidLockKey(String xid) {
        return DEFAULT_REDIS_SEATA_LOCK_XID_PREFIX + xid;
    }

    private String getLockKey(String rowKey) {
        return DEFAULT_REDIS_SEATA_LOCK_PREFIX + rowKey;
    }

}
