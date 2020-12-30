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
package io.seata.rm.datasource.undo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.redis.JedisPooledFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

/**
 * @author funkye
 */
public class UndoLogCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoLogCache.class);

    private String undoLogCacheKeyXid = "UNDO_LOG_CACHE_KEY_XID_";

    private String branchIdPrefix = "BRANCH_ID_";

    private String match = "*";

    public static final String XID = "xid";

    public static final String BRANCH_ID = "branch_id";

    private String initialCursor = "0";

    public static final String CONTEXT = "context";

    public static final String ROLL_BACK_INFO = "roll_back_info";

    public static final String STATE = "state";

    private ObjectMapper mapper;

    /**
     * The query limit.
     */
    private int logQueryLimit;

    public UndoLogCache() {
        logQueryLimit = ConfigurationFactory.getInstance().getInt(ConfigurationKeys.CLIENT_UNDO_REDIS_QUERY_LIMIT, 100);
        mapper = new ObjectMapper();
        JedisPooledFactory.ACTIVATE_NAME = "undo";
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        mapper.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER);

    }

    public Map<String, Object> get(String xid, Long branchId) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String value = jedis.get(getCacheKey(xid, branchId));
            if (StringUtils.isNotBlank(value)) {
                Map<String, Object> objects = null;
                try {
                    objects = (Map<String, Object>)mapper.readValue(value, Map.class);
                } catch (IOException e) {
                    LOGGER.error("get undolog cache error :{}", e.getMessage(), e);
                }
                if (objects != null && objects.size() > 0) {
                    return objects;
                }
            }
        }
        return null;
    }

    public void remove(String xid, Long branchId) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.del(getCacheKey(xid, branchId));
        }
    }

    public void batchDeleteUndoLog(Set<String> xids, Set<Long> branchIds) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Set<String> keys = new HashSet<>();
            xids.forEach(xid -> {
                String cursor = initialCursor;
                ScanParams params = new ScanParams();
                params.count(logQueryLimit);
                params.match(getCacheKey(xid,match));
                ScanResult<String> scans;
                do {
                    scans = jedis.scan(cursor, params);
                    keys.addAll(scans.getResult());
                    cursor = scans.getCursor();
                } while (!initialCursor.equals(cursor));
            });
            if (CollectionUtils.isNotEmpty(keys)) {
                Set<String> delKeys = new HashSet<>();
                keys.forEach(key -> {
                    branchIds.forEach(branchId -> {
                        if (branchId != null && key.contains(branchId.toString())) {
                            delKeys.add(key);
                        }
                    });
                });
                jedis.del(delKeys.toArray(new String[0]));
            }
        }
    }

    public void remove(Set<String> xids) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Set<String> keys = new HashSet<>();
            xids.forEach(xid -> {
                String cursor = initialCursor;
                ScanParams params = new ScanParams();
                params.count(logQueryLimit);
                params.match(getCacheKey(xid,match));
                ScanResult<String> scans;
                do {
                    scans = jedis.scan(cursor, params);
                    keys.addAll(scans.getResult());
                    cursor = scans.getCursor();
                } while (!initialCursor.equals(cursor));
            });
            if (CollectionUtils.isNotEmpty(keys)) {
                jedis.del(keys.toArray(new String[0]));
            }
        }
    }

    public void put(Map<String, Object> objects) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String key = getCacheKey((String)objects.get(XID), (Long)objects.get(BRANCH_ID));
            Pipeline pipeline = jedis.pipelined();
            pipeline.set(key, mapper.writeValueAsString(objects));
            pipeline.expire(key, 60);
            pipeline.sync();
        } catch (JsonProcessingException e) {
            LOGGER.error("put cache fail error msg :{}", e.getMessage(), e);
        }
    }

    private String getCacheKey(String xid, Long branchId) {
        StringBuilder sb = new StringBuilder();
        sb.append(undoLogCacheKeyXid).append(xid).append(branchIdPrefix).append(branchId);
        return sb.toString();
    }

    private String getCacheKey(String xid, String match) {
        StringBuilder sb = new StringBuilder();
        sb.append(undoLogCacheKeyXid).append(xid).append(match);
        return sb.toString();
    }

    public static UndoLogCache getInstance() {
        return UndoLogCacheHolder.INSTANCE;
    }

    private static class UndoLogCacheHolder {
        private static final UndoLogCache INSTANCE = new UndoLogCache();
    }

}
