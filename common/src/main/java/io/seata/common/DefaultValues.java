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
package io.seata.common;

/**
 * @author xingfudeshi@gmail.com
 */
public interface DefaultValues {
    int DEFAULT_CLIENT_LOCK_RETRY_INTERVAL = 10;
    int DEFAULT_TM_DEGRADE_CHECK_ALLOW_TIMES = 10;
    int DEFAULT_CLIENT_LOCK_RETRY_TIMES = 30;
    boolean DEFAULT_CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT = true;
    int DEFAULT_LOG_EXCEPTION_RATE = 100;
    int DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT = 10000;
    int DEFAULT_TM_DEGRADE_CHECK_PERIOD = 2000;
    int DEFAULT_CLIENT_REPORT_RETRY_COUNT = 5;
    boolean DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE = false;
    boolean DEFAULT_CLIENT_TABLE_META_CHECK_ENABLE = false;
    long DEFAULT_TABLE_META_CHECKER_INTERVAL = 60000L;
    boolean DEFAULT_TM_DEGRADE_CHECK = false;
    boolean DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE = false;
    boolean DEFAULT_CLIENT_SAGA_RETRY_PERSIST_MODE_UPDATE = false;
    boolean DEFAULT_CLIENT_SAGA_COMPENSATE_PERSIST_MODE_UPDATE = false;

    /**
     * Shutdown timeout default 3s
     */
    int DEFAULT_SHUTDOWN_TIMEOUT_SEC = 3;
    int DEFAULT_SELECTOR_THREAD_SIZE = 1;
    int DEFAULT_BOSS_THREAD_SIZE = 1;


    String DEFAULT_SELECTOR_THREAD_PREFIX = "NettyClientSelector";
    String DEFAULT_WORKER_THREAD_PREFIX = "NettyClientWorkerThread";
    boolean DEFAULT_ENABLE_CLIENT_BATCH_SEND_REQUEST = true;


    String DEFAULT_BOSS_THREAD_PREFIX = "NettyBoss";
    String DEFAULT_NIO_WORKER_THREAD_PREFIX = "NettyServerNIOWorker";
    String DEFAULT_EXECUTOR_THREAD_PREFIX = "NettyServerBizHandler";

    boolean DEFAULT_TRANSPORT_HEARTBEAT = true;
    boolean DEFAULT_TRANSACTION_UNDO_DATA_VALIDATION = true;
    String DEFAULT_TRANSACTION_UNDO_LOG_SERIALIZATION = "jackson";
    boolean DEFAULT_ONLY_CARE_UPDATE_COLUMNS = true;
    /**
     * The constant  DEFAULT_TRANSACTION_UNDO_LOG_TABLE.
     */
    String DEFAULT_TRANSACTION_UNDO_LOG_TABLE = "undo_log";
    /**
     * The constant DEFAULT_STORE_DB_GLOBAL_TABLE.
     */
    String DEFAULT_STORE_DB_GLOBAL_TABLE = "global_table";

    /**
     * The constant DEFAULT_STORE_DB_BRANCH_TABLE.
     */
    String DEFAULT_STORE_DB_BRANCH_TABLE = "branch_table";

    /**
     * The constant DEFAULT_LOCK_DB_TABLE.
     */
    String DEFAULT_LOCK_DB_TABLE = "lock_table";

    int DEFAULT_TM_COMMIT_RETRY_COUNT = 5;
    int DEFAULT_TM_ROLLBACK_RETRY_COUNT = 5;
    int DEFAULT_GLOBAL_TRANSACTION_TIMEOUT = 60000;

    String DEFAULT_TX_GROUP = "my_test_tx_group";
    String DEFAULT_TC_CLUSTER = "default";
    String DEFAULT_GROUPLIST = "127.0.0.1:8091";

    String DEFAULT_DATA_SOURCE_PROXY_MODE = "AT";

    boolean DEFAULT_DISABLE_GLOBAL_TRANSACTION = false;

    String DEFAULT_RETRY_STRATEGY = "I,5s,5m|-,7d";

    int SERVER_DEFAULT_PORT = 8091;
    String SERVER_DEFAULT_STORE_MODE = "file";

    String DEFAULT_SAGA_JSON_PARSER = "fastjson";

    boolean DEFAULT_SERVER_ENABLE_CHECK_AUTH = true;

    String DEFAULT_LOAD_BALANCE = "RandomLoadBalance";
    int VIRTUAL_NODES_DEFAULT = 10;

    /**
     * the constant DEFAULT_CLIENT_UNDO_COMPRESS_ENABLE
     */
    boolean DEFAULT_CLIENT_UNDO_COMPRESS_ENABLE = true;

    /**
     * the constant DEFAULT_CLIENT_UNDO_COMPRESS_TYPE
     */
    String DEFAULT_CLIENT_UNDO_COMPRESS_TYPE = "zip";

    /**
     * the constant DEFAULT_CLIENT_UNDO_COMPRESS_THRESHOLD
     */
    String DEFAULT_CLIENT_UNDO_COMPRESS_THRESHOLD = "64k";

    /**
     * the constant DEFAULT_RETRY_DEAD_THRESHOLD
     */
    int DEFAULT_RETRY_DEAD_THRESHOLD = 2 * 60 * 1000 + 10 * 1000;

    /**
     * the constant TM_INTERCEPTOR_ORDER
     */
    int TM_INTERCEPTOR_ORDER = Integer.MIN_VALUE + 1000;
  
    /**
     * the constant TCC_ACTION_INTERCEPTOR_ORDER
     */
    int TCC_ACTION_INTERCEPTOR_ORDER = Integer.MIN_VALUE + 1000;
}
