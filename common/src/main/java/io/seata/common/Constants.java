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

import java.nio.charset.Charset;

/**
 * The type Constants.
 *
 * @author slievrly
 */
public interface Constants {

    /**
     * The constant IP_PORT_SPLIT_CHAR.
     */
    String IP_PORT_SPLIT_CHAR = ":";
    /**
     * The constant CLIENT_ID_SPLIT_CHAR.
     */
    String CLIENT_ID_SPLIT_CHAR = ":";
    /**
     * The constant ENDPOINT_BEGIN_CHAR.
     */
    String ENDPOINT_BEGIN_CHAR = "/";
    /**
     * The constant DBKEYS_SPLIT_CHAR.
     */
    String DBKEYS_SPLIT_CHAR = ",";

    /**
     * The constant ROW_LOCK_KEY_SPLIT_CHAR.
     */
    String ROW_LOCK_KEY_SPLIT_CHAR = ";";

    /**
     * the start time of transaction
     */
    String START_TIME = "start-time";

    /**
     * app name
     */
    String APP_NAME = "appName";

    /**
     * TCC start time
     */
    String ACTION_START_TIME = "action-start-time";

    /**
     * TCC name
     */
    String ACTION_NAME = "actionName";

    /**
     * Use TCC fence
     */
    String USE_TCC_FENCE = "useTCCFence";

    /**
     * phase one method name
     */
    String PREPARE_METHOD = "sys::prepare";

    /**
     * phase two commit method name
     */
    String COMMIT_METHOD = "sys::commit";

    /**
     * phase two rollback method name
     */
    String ROLLBACK_METHOD = "sys::rollback";

    /**
     * host ip
     */
    String HOST_NAME = "host-name";

    /**
     * branch context
     */
    String TCC_ACTION_CONTEXT = "actionContext";

    /**
     * default charset name
     */
    String DEFAULT_CHARSET_NAME = "UTF-8";

    /**
     * default charset is utf-8
     */
    Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_CHARSET_NAME);
    /**
     * The constant OBJECT_KEY_SPRING_APPLICATION_CONTEXT
     */
    String OBJECT_KEY_SPRING_APPLICATION_CONTEXT = "springApplicationContext";
    /**
     * The constant BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER
     */
    String BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER = "springApplicationContextProvider";
    /**
     * The constant BEAN_NAME_FAILURE_HANDLER
     */
    String BEAN_NAME_FAILURE_HANDLER = "failureHandler";
    /**
     * The constant SAGA_TRANS_NAME_PREFIX
     */
    String SAGA_TRANS_NAME_PREFIX = "$Saga_";

    /**
     * The constant RETRY_ROLLBACKING
     */
    String RETRY_ROLLBACKING = "RetryRollbacking";

    /**
     * The constant RETRY_COMMITTING
     */
    String RETRY_COMMITTING = "RetryCommitting";

    /**
     * The constant ASYNC_COMMITTING
     */
    String ASYNC_COMMITTING = "AsyncCommitting";

    /**
     * The constant TX_TIMEOUT_CHECK
     */
    String TX_TIMEOUT_CHECK = "TxTimeoutCheck";

    /**
     * The constant UNDOLOG_DELETE
     */
    String UNDOLOG_DELETE = "UndologDelete";

    /**
     * The constant AUTO_COMMIT
     */
    String AUTO_COMMIT = "autoCommit";

}
