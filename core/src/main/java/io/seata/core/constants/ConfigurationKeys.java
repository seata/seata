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

/**
 * The type Configuration keys.
 *
 * @author slievrly
 */
public class ConfigurationKeys {

    /**
     * The constant SERVICE_PREFIX.
     */
    public static final String SERVICE_PREFIX = "service.";

    /**
     * The constant STORE_PREFIX.
     */
    public static final String STORE_PREFIX = "store.";

    /**
     * The constant STORE_MODE.
     */
    public static final String STORE_MODE = STORE_PREFIX + "mode";

    /**
     * The constant STORE_FILE_PREFIX
     */
    public static final String STORE_FILE_PREFIX = STORE_PREFIX + "file.";

    /**
     * The constant STORE_FILE_DIR
     */
    public static final String STORE_FILE_DIR = STORE_FILE_PREFIX + "dir";

    /**
     * The constant SERVICE_GROUP_MAPPING_PREFIX.
     */
    public static final String SERVICE_GROUP_MAPPING_PREFIX = SERVICE_PREFIX + "vgroupMapping.";
    /**
     * The constant GROUPLIST_POSTFIX.
     */
    public static final String GROUPLIST_POSTFIX = ".grouplist";
    /**
     * The constant SERVER_NODE_SPLIT_CHAR.
     */
    public static final String SERVER_NODE_SPLIT_CHAR = System.getProperty("line.separator");

    /**
     * The constant ENABLE_DEGRADE_POSTFIX.
     */
    public static final String ENABLE_DEGRADE_POSTFIX = "enableDegrade";

    /**
     * The constant CLIENT_PREFIX.
     */
    public static final String CLIENT_PREFIX = "client.";

    /**
     * The constant SERVER_PREFIX.
     */
    public static final String SERVER_PREFIX = "server.";

    /**
     * The constant TRANSPORT_PREFIX.
     */
    public static final String TRANSPORT_PREFIX = "transport.";

    /**
     * The constant CLIENT_RM_PREFIX.
     */
    public static final String CLIENT_RM_PREFIX = CLIENT_PREFIX + "rm.";

    /**
     * The constant CLIENT_ASYNC_COMMIT_BUFFER_LIMIT.
     */
    public static final String CLIENT_ASYNC_COMMIT_BUFFER_LIMIT = CLIENT_RM_PREFIX + "asyncCommitBufferLimit";
    /**
     * The constant CLIENT_RM_LOCK_PREFIX.
     */
    public static final String CLIENT_RM_LOCK_PREFIX = CLIENT_RM_PREFIX + "lock.";

    /**
     * The constant CLIENT_LOCK_RETRY_TIMES.
     */
    public static final String CLIENT_LOCK_RETRY_TIMES = CLIENT_RM_LOCK_PREFIX + "retryTimes";
    /**
     * The constant CLIENT_LOCK_RETRY_INTERVAL.
     */
    public static final String CLIENT_LOCK_RETRY_INTERVAL = CLIENT_RM_LOCK_PREFIX + "retryInterval";
    /**
     * The constant CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT.
     */
    public static final String CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT = CLIENT_RM_LOCK_PREFIX + "retryPolicyBranchRollbackOnConflict";


    /**
     * The constant SERVICE_SESSION_RELOAD_READ_SIZE
     */
    public static final String SERVICE_SESSION_RELOAD_READ_SIZE = STORE_FILE_PREFIX + "sessionReloadReadSize";

    /**
     * The constant CLIENT_REPORT_SUCCESS_ENABLE.
     */
    public static final String CLIENT_REPORT_SUCCESS_ENABLE = CLIENT_RM_PREFIX + "reportSuccessEnable";

    /**
     * The constant CLIENT_SAGA_BRANCH_REGISTER_ENABLE.
     */
    public static final String CLIENT_SAGA_BRANCH_REGISTER_ENABLE = CLIENT_RM_PREFIX + "sagaBranchRegisterEnable";

    /**
     * The constant CLIENT_REPORT_RETRY_COUNT.
     */
    public static final String CLIENT_REPORT_RETRY_COUNT = CLIENT_RM_PREFIX + "reportRetryCount";

    /**
     * The constant CLIENT_TABLE_META_CHECK_ENABLE.
     */
    public static final String CLIENT_TABLE_META_CHECK_ENABLE = CLIENT_RM_PREFIX + "tableMetaCheckEnable";

    /**
     * The constant CLIENT_TM_PREFIX.
     */
    public static final String CLIENT_TM_PREFIX = CLIENT_PREFIX + "tm.";
    /**
     * The constant CLIENT_TM_COMMIT_RETRY_TIMES.
     */
    public static final String CLIENT_TM_COMMIT_RETRY_COUNT = CLIENT_TM_PREFIX + "commitRetryCount";

    /**
     * The constant CLIENT_TM_ROLLBACK_RETRY_TIMES.
     */
    public static final String CLIENT_TM_ROLLBACK_RETRY_COUNT = CLIENT_TM_PREFIX + "rollbackRetryCount";

    /**
     * The constant SERIALIZE_FOR_RPC.
     */
    public static final String SERIALIZE_FOR_RPC = TRANSPORT_PREFIX + "serialization";

    /**
     * The constant COMPRESSOR_FOR_RPC.
     *
     * @since 0.7.0
     */
    public static final String COMPRESSOR_FOR_RPC = TRANSPORT_PREFIX + "compressor";

    /**
     * The constant STORE_DB_PREFIX.
     */
    public static final String STORE_DB_PREFIX = "store.db.";

    /**
     * The constant STORE_REDIS_PREFIX.
     */
    public static final String STORE_REDIS_PREFIX = "store.redis.";

    /**
     * The constant STORE_DB_GLOBAL_TABLE.
     */
    public static final String STORE_DB_GLOBAL_TABLE = STORE_DB_PREFIX + "globalTable";

    /**
     * The constant STORE_DB_BRANCH_TABLE.
     */
    public static final String STORE_DB_BRANCH_TABLE = STORE_DB_PREFIX + "branchTable";

    /**
     * The constant STORE_DB_DATASOURCE_TYPE.
     */
    public static final String STORE_DB_DATASOURCE_TYPE = STORE_DB_PREFIX + "datasource";

    /**
     * The constant STORE_DB_TYPE.
     */
    public static final String STORE_DB_TYPE = STORE_DB_PREFIX + "dbType";

    /**
     * The constant STORE_DB_DRIVER_CLASS_NAME.
     */
    public static final String STORE_DB_DRIVER_CLASS_NAME = STORE_DB_PREFIX + "driverClassName";

    /**
     * The constant STORE_DB_MAX_WAIT.
     */
    public static final String STORE_DB_MAX_WAIT = STORE_DB_PREFIX + "maxWait";

    /**
     * The constant STORE_DB_URL.
     */
    public static final String STORE_DB_URL = STORE_DB_PREFIX + "url";

    /**
     * The constant STORE_DB_USER.
     */
    public static final String STORE_DB_USER = STORE_DB_PREFIX + "user";

    /**
     * The constant STORE_DB_PASSWORD.
     */
    public static final String STORE_DB_PASSWORD = STORE_DB_PREFIX + "password";

    /**
     * The constant STORE_DB_MIN_CONN.
     */
    public static final String STORE_DB_MIN_CONN = STORE_DB_PREFIX + "minConn";

    /**
     * The constant STORE_DB_MAX_CONN.
     */
    public static final String STORE_DB_MAX_CONN = STORE_DB_PREFIX + "maxConn";

    /**
     * The constant STORE_DB_LOG_QUERY_LIMIT.
     */
    public static final String STORE_DB_LOG_QUERY_LIMIT = STORE_DB_PREFIX + "queryLimit";

    /**
     * The constant LOCK_DB_TABLE.
     */
    public static final String LOCK_DB_TABLE = STORE_DB_PREFIX + "lockTable";

    /**
     * The constant SERVER_PORT.
     */
    public static final String SERVER_PORT = SERVER_PREFIX + "port";

    /**
     * The constant RECOVERY_PREFIX.
     */
    public static final String RECOVERY_PREFIX = SERVER_PREFIX + "recovery.";
    /**
     * The constant COMMITING_RETRY_PERIOD.
     */
    public static final String COMMITING_RETRY_PERIOD = RECOVERY_PREFIX + "committingRetryPeriod";

    /**
     * The constant ASYN_COMMITING_RETRY_PERIOD.
     */
    public static final String ASYN_COMMITING_RETRY_PERIOD = RECOVERY_PREFIX + "asynCommittingRetryPeriod";

    /**
     * The constant ROLLBACKING_RETRY_PERIOD.
     */
    public static final String ROLLBACKING_RETRY_PERIOD = RECOVERY_PREFIX + "rollbackingRetryPeriod";

    /**
     * The constant TIMEOUT_RETRY_PERIOD.
     */
    public static final String TIMEOUT_RETRY_PERIOD = RECOVERY_PREFIX + "timeoutRetryPeriod";

    /**
     * The constant CLIENT_UNDO_PREFIX.
     */
    public static final String CLIENT_UNDO_PREFIX = "client.undo.";

    /**
     * The constant TRANSACTION_UNDO_DATA_VALIDATION.
     */
    public static final String TRANSACTION_UNDO_DATA_VALIDATION = CLIENT_UNDO_PREFIX + "dataValidation";
    /**
     * The constant TRANSACTION_UNDO_LOG_SERIALIZATION.
     */
    public static final String TRANSACTION_UNDO_LOG_SERIALIZATION = CLIENT_UNDO_PREFIX + "logSerialization";

    /**
     * The constant TRANSACTION_UNDO_ONLY_CARE_UPDATE_COLUMNS.
     */
    public static final String TRANSACTION_UNDO_ONLY_CARE_UPDATE_COLUMNS = CLIENT_UNDO_PREFIX + "onlyCareUpdateColumns";

    /**
     * The constant METRICS_PREFIX.
     */
    public static final String METRICS_PREFIX = "metrics.";

    /**
     * The constant METRICS_ENABLED.
     */
    public static final String METRICS_ENABLED = "enabled";

    /**
     * The constant METRICS_REGISTRY_TYPE.
     */
    public static final String METRICS_REGISTRY_TYPE = "registryType";

    /**
     * The constant METRICS_EXPORTER_LIST.
     */
    public static final String METRICS_EXPORTER_LIST = "exporterList";
    /**
     * The constant METRICS_EXPORTER_PROMETHEUS_PORT
     */
    public static final String METRICS_EXPORTER_PROMETHEUS_PORT = "exporterPrometheusPort";

    /**
     * The constant SERVER_UNDO_PREFIX.
     */
    public static final String SERVER_UNDO_PREFIX = SERVER_PREFIX + "undo.";

    /**
     * The constant TRANSACTION_UNDO_LOG_SAVE_DAYS.
     */
    public static final String TRANSACTION_UNDO_LOG_SAVE_DAYS = SERVER_UNDO_PREFIX + "logSaveDays";

    /**
     * The constant TRANSACTION_UNDO_LOG_DELETE_PERIOD
     */
    public static final String TRANSACTION_UNDO_LOG_DELETE_PERIOD = SERVER_UNDO_PREFIX + "logDeletePeriod";

    /**
     * The constant TRANSACTION_UNDO_LOG_TABLE
     */
    public static final String TRANSACTION_UNDO_LOG_TABLE = CLIENT_UNDO_PREFIX + "logTable";
    /**
     * The constant CLIENT_LOG_PREFIX
     */
    public static final String CLIENT_LOG_PREFIX = CLIENT_PREFIX + "log.";

    /**
     * The constant TRANSACTION_UNDO_LOG_EXCEPTION_RATE
     */
    public static final String TRANSACTION_LOG_EXCEPTION_RATE = CLIENT_LOG_PREFIX + "exceptionRate";

    /**
     * The constant MAX_COMMIT_RETRY_TIMEOUT.
     */
    public static final String MAX_COMMIT_RETRY_TIMEOUT = SERVER_PREFIX + "maxCommitRetryTimeout";

    /**
     * The constant MAX_ROLLBACK_RETRY_TIMEOUT.
     */
    public static final String MAX_ROLLBACK_RETRY_TIMEOUT = SERVER_PREFIX + "maxRollbackRetryTimeout";

    /**
     * The constant ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE.
     */
    public static final String ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE = SERVER_PREFIX + "rollbackRetryTimeoutUnlockEnable";

    /**
     * The constant TRANSPORT_TYPE
     */
    public static final String TRANSPORT_TYPE = TRANSPORT_PREFIX + "type";

    /**
     * The constant TRANSPORT_SERVER
     */
    public static final String TRANSPORT_SERVER = TRANSPORT_PREFIX + "server";

    /**
     * The constant TRANSPORT_HEARTBEAT
     */
    public static final String TRANSPORT_HEARTBEAT = TRANSPORT_PREFIX + "heartbeat";

    /**
     * The constant THREAD_FACTORY_PREFIX
     */
    public static final String THREAD_FACTORY_PREFIX = TRANSPORT_PREFIX + "threadFactory.";

    /**
     * The constant BOSS_THREAD_PREFIX
     */
    public static final String BOSS_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "bossThreadPrefix";

    /**
     * The constant WORKER_THREAD_PREFIX
     */
    public static final String WORKER_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "workerThreadPrefix";

    /**
     * The constant SERVER_EXECUTOR_THREAD_PREFIX
     */
    public static final String SERVER_EXECUTOR_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "serverExecutorThreadPrefix";

    /**
     * The constant SHARE_BOSS_WORKER
     */
    public static final String SHARE_BOSS_WORKER = THREAD_FACTORY_PREFIX + "shareBossWorker";

    /**
     * The constant CLIENT_SELECTOR_THREAD_PREFIX
     */
    public static final String CLIENT_SELECTOR_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "clientSelectorThreadPrefix";

    /**
     * The constant CLIENT_SELECTOR_THREAD_SIZE
     */
    public static final String CLIENT_SELECTOR_THREAD_SIZE = THREAD_FACTORY_PREFIX + "clientSelectorThreadSize";

    /**
     * The constant CLIENT_WORKER_THREAD_PREFIX
     */
    public static final String CLIENT_WORKER_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "clientWorkerThreadPrefix";

    /**
     * The constant BOSS_THREAD_SIZE
     */
    public static final String BOSS_THREAD_SIZE = THREAD_FACTORY_PREFIX + "bossThreadSize";

    /**
     * The constant WORKER_THREAD_SIZE
     */
    public static final String WORKER_THREAD_SIZE = THREAD_FACTORY_PREFIX + "workerThreadSize";

    /**
     * The constant SHUTDOWN_PREFIX
     */
    public static final String SHUTDOWN_PREFIX = TRANSPORT_PREFIX + "shutdown.";

    /**
     * The constant SHUTDOWN_WAIT
     */
    public static final String SHUTDOWN_WAIT = SHUTDOWN_PREFIX + "wait";

    /**
     * The constant ENABLE_CLIENT_BATCH_SEND_REQUEST
     */
    public static final String ENABLE_CLIENT_BATCH_SEND_REQUEST = TRANSPORT_PREFIX + "enableClientBatchSendRequest";

    /**
     * The constant DISABLE_GLOBAL_TRANSACTION.
     */
    public static final String DISABLE_GLOBAL_TRANSACTION = SERVICE_PREFIX + "disableGlobalTransaction";

    /**
     * The constant CONTEXT_TYPE.
     */
    public static final String CONTEXT_TYPE = SERVICE_PREFIX + "contextType";

    /**
     * The constant SQL_PARSER_TYPE.
     */
    public static final String SQL_PARSER_TYPE = CLIENT_RM_PREFIX + "sqlParserType";

    /**
     * The constant STORE_REDIS_HOST.
     */
    public static final String STORE_REDIS_HOST = STORE_REDIS_PREFIX + "host";

    /**
     * The constant STORE_MIN_Conn.
     */
    public static final String STORE_REDIS_MIN_CONN = STORE_REDIS_PREFIX + "minConn";

    /**
     * The constant STORE_REDIS_PORT.
     */
    public static final String STORE_REDIS_PORT = STORE_REDIS_PREFIX + "port";

    /**
     * The constant STORE_REDIS_MAX_CONN.
     */
    public static final String STORE_REDIS_MAX_CONN = STORE_REDIS_PREFIX + "maxConn";

    /**
     * The constant STORE_REDIS_DATABASE.
     */
    public static final String STORE_REDIS_DATABASE = STORE_REDIS_PREFIX + "database";

    /**
     * The constant STORE_REDIS_PASSWORD.
     */
    public static final String STORE_REDIS_PASSWORD = STORE_REDIS_PREFIX + "password";

    /**
     * The constant STORE_REDIS_QUERY_LIMIT.
     */
    public static final String STORE_REDIS_QUERY_LIMIT = STORE_REDIS_PREFIX + "queryLimit";

    /**
     * The constant CLIENT_DEGRADE_CHECK_PERIOD.
     */
    public static final String CLIENT_DEGRADE_CHECK_PERIOD = CLIENT_TM_PREFIX + "degradeCheckPeriod";

    /**
     * The constant CLIENT_DEGRADE_CHECK.
     */
    public static final String CLIENT_DEGRADE_CHECK = CLIENT_TM_PREFIX + "degradeCheck";
    /**
     * The constant CLIENT_DEGRADE_CHECK_ALLOW_TIMES.
     */
    public static final String CLIENT_DEGRADE_CHECK_ALLOW_TIMES = CLIENT_TM_PREFIX + "degradeCheckAllowTimes";

    /**
     * The constant SERVER_ENABLE_CHECK_AUTH.
     */
    public static final String SERVER_ENABLE_CHECK_AUTH = SERVER_PREFIX + "enableCheckAuth";

}
