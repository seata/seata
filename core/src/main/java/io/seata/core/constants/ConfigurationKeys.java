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
 * @author jimin.jm @alibaba-inc.com
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
    public static final String STORE_MODE =  STORE_PREFIX + "mode";

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
    public static final String SERVICE_GROUP_MAPPING_PREFIX = SERVICE_PREFIX + "vgroup_mapping.";
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
     * The constant TRANSPORT_PREFIX.
     */
    public static final String TRANSPORT_PREFIX = "transport.";

    /**
     * The constant CLIENT_ASYNC_COMMIT_BUFFER_LIMIT.
     */
    public static final String CLIENT_ASYNC_COMMIT_BUFFER_LIMIT = CLIENT_PREFIX + "async.commit.buffer.limit";
    /**
     * The constant CLIENT_LOCK_RETRY_TIMES.
     */
    public static final String CLIENT_LOCK_RETRY_TIMES = CLIENT_PREFIX + "lock.retry.times";
    /**
     * The constant CLIENT_LOCK_RETRY_INTERNAL.
     */
    public static final String CLIENT_LOCK_RETRY_INTERNAL = CLIENT_PREFIX + "lock.retry.internal";
    public static final String CLIENT_THREAD_PROPAGATE = CLIENT_PREFIX + "thread.propagate";

    /**
     * The constant SERVICE_SESSION_RELOAD_READ_SIZE
     */
    public static final String SERVICE_SESSION_RELOAD_READ_SIZE = STORE_FILE_PREFIX + "session.reload.read_size";

    /**
     * The constant CLIENT_REPORT_RETRY_COUNT.
     */
    public static final String CLIENT_REPORT_RETRY_COUNT = CLIENT_PREFIX + "report.retry.count";

    /**
     * The constant CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT.
     */
    public static final String CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT = CLIENT_PREFIX + "lock.retry.policy.branch-rollback-on-conflict";

    /**
     * The constant CLIENT_TABLE_META_CHECK_ENABLE.
     */
    public static final String CLIENT_TABLE_META_CHECK_ENABLE  = CLIENT_PREFIX + "table.meta.check.enable";

    /**
     * The constant CLIENT_TM_COMMIT_RETRY_TIMES.
     */
    public static final String CLIENT_TM_COMMIT_RETRY_COUNT = CLIENT_PREFIX + "tm.commit.retry.count";

    /**
     * The constant CLIENT_TM_ROLLBACK_RETRY_TIMES.
     */
    public static final String CLIENT_TM_ROLLBACK_RETRY_COUNT = CLIENT_PREFIX + "tm.rollback.retry.count";

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
     * The constant STORE_DB_GLOBAL_TABLE.
     */
    public static final String STORE_DB_GLOBAL_TABLE  = "store.db.global.table";

    /**
     * The constant STORE_DB_BRANCH_TABLE.
     */
    public static final String STORE_DB_BRANCH_TABLE  = "store.db.branch.table";

    /**
     * The constant STORE_DB_GLOBAL_DEFAULT_TABLE.
     */
    public static final String STORE_DB_GLOBAL_DEFAULT_TABLE  = "global_table";

    /**
     * The constant STORE_DB_BRANCH_DEFAULT_TABLE.
     */
    public static final String STORE_DB_BRANCH_DEFAULT_TABLE  = "branch_table";

    /**
     * The constant STORE_DB_DATASOURCE_TYPE.
     */
    public static final String STORE_DB_DATASOURCE_TYPE  = "store.db.datasource";


    /**
     * The constant STORE_DB_TYPE.
     */
    public static final String STORE_DB_TYPE  = "store.db.db-type";
    /**
     * The constant STORE_DB_DRIVER_CLASS_NAME.
     */
    public static final String STORE_DB_DRIVER_CLASS_NAME  =  "store.db.driver-class-name";

    /**
     * The constant STORE_DB_URL.
     */
    public static final String STORE_DB_URL  = "store.db.url";

    /**
     * The constant STORE_DB_USER.
     */
    public static final String STORE_DB_USER  = "store.db.user";

    /**
     * The constant STORE_DB_PASSWORD.
     */
    public static final String STORE_DB_PASSWORD  = "store.db.password";

    /**
     * The constant STORE_DB_MIN_CONN.
     */
    public static final String STORE_DB_MIN_CONN = "store.db.min-conn";

    /**
     * The constant STORE_DB_MAX_CONN.
     */
    public static final String STORE_DB_MAX_CONN  = "store.db.max-conn";

    /**
     * The constant STORE_DB_LOG_QUERY_LIMIT.
     */
    public static final String STORE_DB_LOG_QUERY_LIMIT  = "store.db.query-limit";

    /**
     * The constant LOCK_MODE.
     */
    public static final String LOCK_MODE =  "lock.mode";

    /**
     * The constant LOCK_DB_TABLE.
     */
    public static final String LOCK_DB_TABLE  = "store.db.lock-table";

    /**
     * The constant LOCK_DB_DEFAULT_TABLE.
     */
    public static final String LOCK_DB_DEFAULT_TABLE  = "lock_table";

    /**
     * The constant COMMITING_RETRY_PERIOD.
     */
    public static final String COMMITING_RETRY_PERIOD = "recovery.committing-retry-period";

    /**
     * The constant ASYN_COMMITING_RETRY_PERIOD.
     */
    public static final String ASYN_COMMITING_RETRY_PERIOD = "recovery.asyn-committing-retry-period";

    /**
     * The constant ROLLBACKING_RETRY_PERIOD.
     */
    public static final String ROLLBACKING_RETRY_PERIOD = "recovery.rollbacking-retry-period";

    /**
     * The constant TIMEOUT_RETRY_PERIOD.
     */
    public static final String TIMEOUT_RETRY_PERIOD = "recovery.timeout-retry-period";

    /**
     * The constant TRANSACTION_PREFIX.
     */
    public static final String TRANSACTION_PREFIX = "transaction.";

    /**
     * The constant TRANSACTION_UNDO_DATA_VALIDATION.
     */
    public static final String TRANSACTION_UNDO_DATA_VALIDATION = TRANSACTION_PREFIX + "undo.data.validation";
    /**
     * The constant TRANSACTION_UNDO_LOG_SERIALIZATION.
     */
    public static final String TRANSACTION_UNDO_LOG_SERIALIZATION = TRANSACTION_PREFIX + "undo.log.serialization";

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
    public static final String METRICS_REGISTRY_TYPE = "registry-type";

    /**
     * The constant METRICS_EXPORTER_LIST.
     */
    public static final String METRICS_EXPORTER_LIST = "exporter-list";

    /**
     * The constant TRANSACTION_UNDO_LOG_SAVE_DAYS.
     */
    public static final String TRANSACTION_UNDO_LOG_SAVE_DAYS = TRANSACTION_PREFIX + "undo.log.save.days";

    /**
     * The constant TRANSACTION_UNDO_LOG_DELETE_PERIOD
     */
    public static final String TRANSACTION_UNDO_LOG_DELETE_PERIOD = TRANSACTION_PREFIX + "undo.log.delete.period";

    /**
     * The constant TRANSACTION_UNDO_LOG_TABLE
     */
    public static final String TRANSACTION_UNDO_LOG_TABLE = TRANSACTION_PREFIX + "undo.log.table";

    /**
     * The constant TRANSACTION_UNDO_LOG_DEFAULT_TABLE.
     */
    public static final String TRANSACTION_UNDO_LOG_DEFAULT_TABLE  = "undo_log";

    /**
     * The constant SUPPORT_PREFIX.
     */
    public static final String SUPPORT_PREFIX = "support.";
    /**
     * The constant SPRING_PREFIX.
     */
    public static final String SPRING_PREFIX = "spring.";
    /**
     * The constant DATASOURCE_PREFIX.
     */
    public static final String DATASOURCE_PREFIX = "datasource.";
    /**
     * The constant DATASOURCE_AUTOPROXY.
     */
    public static final String DATASOURCE_AUTOPROXY = SUPPORT_PREFIX + SPRING_PREFIX + DATASOURCE_PREFIX + "autoproxy";
}
