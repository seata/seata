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
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/9 17:14
 */
public class Constants {
    /**
     * The constant IP_PORT_SPLIT_CHAR.
     */
    public static final String IP_PORT_SPLIT_CHAR = ":";
    /**
     * The constant CLIENT_ID_SPLIT_CHAR.
     */
    public static final String CLIENT_ID_SPLIT_CHAR = ":";
    /**
     * The constant ENDPOINT_BEGIN_CHAR.
     */
    public static final String ENDPOINT_BEGIN_CHAR = "/";
    /**
     * The constant DBKEYS_SPLIT_CHAR.
     */
    public static final String DBKEYS_SPLIT_CHAR = ",";

    /** the start time of transaction */
    public static final String START_TIME  = "start-time";

    /**
     * app name
     */
    public static final String APP_NAME = "appName";

    /**
     * TCC start time
     */
    public static final String ACTION_START_TIME = "action-start-time";

    /**
     * TCC name
     */
    public final static String ACTION_NAME = "actionName";

    /**
     * phase one method name
     */
    public final static String PREPARE_METHOD = "sys::prepare";

    /**
     * phase two commit method name
     */
    public final static String COMMIT_METHOD = "sys::commit";

    /**
     * phase two rollback method name
     */
    public final static String ROLLBACK_METHOD = "sys::rollback";

    /**
     * host ip
     */
    public final static String HOST_NAME = "host-name";

    /**
     * The constant TCC_METHOD_RESULT.
     */
    public final static String TCC_METHOD_RESULT = "result";

    /**
     * The constant SAGA_METHOD_RESULT.
     */
    public final static String  SAGA_METHOD_RESULT    = "result";
    
    /**
     * The constant TCC_METHOD_ARGUMENTS.
     */
    public final static String TCC_METHOD_ARGUMENTS = "arguments";

    /**
     */
    public final static String  SAGA_METHOD_ARGUMENTS = "arguments";
    
    /**
     * transaction context
     */
    public final static String  TCC_ACTIVITY_CONTEXT  = "activityContext";

    /**
     * transaction context
     */
    public final static String  SAGA_ACTIVITY_CONTEXT  = "activityContext";
    
    /**
     * branch context
     */
    public final static String TCC_ACTION_CONTEXT = "actionContext";

    /**
     * branch context
     */
    public final static String  SAGA_ACTION_CONTEXT   = "actionContext";
    
    /**
     * default charset name
     */
    public static final String DEFAULT_CHARSET_NAME = "UTF-8";
    
    /**
     * default charset is utf-8
     */
    public static final Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_CHARSET_NAME);
}
