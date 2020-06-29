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
package io.seata.core.constants;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author xingfudeshi@gmail.com
 */
public class DefaultValues {
    public static final int DEFAULT_CLIENT_LOCK_RETRY_INTERVAL = 10;
    public static final int DEFAULT_TM_DEGRADE_CHECK_ALLOW_TIMES = 10;
    public static final int DEFAULT_CLIENT_LOCK_RETRY_TIMES = 30;
    public static final boolean DEFAULT_CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT = true;
    public static final int DEFAULT_LOG_EXCEPTION_RATE = 100;
    public static final int DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT = 10000;
    public static final int DEFAULT_TM_DEGRADE_CHECK_PERIOD = 2000;
    public static final int DEFAULT_CLIENT_REPORT_RETRY_COUNT = 5;
    public static final boolean DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE = false;
    public static final boolean DEFAULT_CLIENT_TABLE_META_CHECK_ENABLE = false;
    public static final boolean DEFAULT_TM_DEGRADE_CHECK = false;
    public static final boolean DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE = false;
    /**
     * Shutdown timeout default 3s
     */
    public static final int DEFAULT_SHUTDOWN_TIMEOUT_SEC = 3;
    public static final int DEFAULT_SELECTOR_THREAD_SIZE = 1;
    public static final int DEFAULT_BOSS_THREAD_SIZE = 1;


    public static final String DEFAULT_SELECTOR_THREAD_PREFIX = "NettyClientSelector";
    public static final String DEFAULT_WORKER_THREAD_PREFIX = "NettyClientWorkerThread";
    public static final boolean DEFAULT_ENABLE_CLIENT_BATCH_SEND_REQUEST = true;


    public static final String DEFAULT_BOSS_THREAD_PREFIX = "NettyBoss";
    public static final String DEFAULT_NIO_WORKER_THREAD_PREFIX = "NettyServerNIOWorker";
    public static final String DEFAULT_EXECUTOR_THREAD_PREFIX = "NettyServerBizHandler";

    public static final boolean DEFAULT_TRANSPORT_HEARTBEAT = true;
    public static final boolean DEFAULT_TRANSACTION_UNDO_DATA_VALIDATION = true;
    public static final String DEFAULT_TRANSACTION_UNDO_LOG_SERIALIZATION = "jackson";
    public static final boolean DEFAULT_ONLY_CARE_UPDATE_COLUMNS = true;
    /**
     * The constant  DEFAULT_TRANSACTION_UNDO_LOG_TABLE.
     */
    public static final String DEFAULT_TRANSACTION_UNDO_LOG_TABLE = "undo_log";
    /**
     * The constant DEFAULT_STORE_DB_GLOBAL_TABLE.
     */
    public static final String DEFAULT_STORE_DB_GLOBAL_TABLE = "global_table";

    /**
     * The constant DEFAULT_STORE_DB_BRANCH_TABLE.
     */
    public static final String DEFAULT_STORE_DB_BRANCH_TABLE = "branch_table";

    /**
     * The constant DEFAULT_LOCK_DB_TABLE.
     */
    public static final String DEFAULT_LOCK_DB_TABLE = "lock_table";

    public static final int DEFAULT_TM_COMMIT_RETRY_COUNT = 5;
    public static final int DEFAULT_TM_ROLLBACK_RETRY_COUNT = 5;

    public static final String DEFAULT_TX_GROUP = "my_test_tx_group";
    public static final String DEFAULT_TC_CLUSTER = "default";
    public static final String DEFAULT_GROUPLIST = "127.0.0.1:8091";

    public static final boolean DEFAULT_DISABLE_GLOBAL_TRANSACTION = false;

    public static final String DEFAULT_CONTEXT_TYPE = "FastThreadLocalContextCore";

    public static final int SERVER_DEFAULT_PORT = 8091;
    public static final String SERVER_DEFAULT_STORE_MODE = "file";
    public static final long SERVER_DEFAULT_NODE = ThreadLocalRandom.current().nextLong(1024);
    public static final boolean DEFAULT_SERVER_ENABLE_CHECK_AUTH = true;


}
