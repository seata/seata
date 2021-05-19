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
package io.seata.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Reason for the global transaction stopped.
 *
 * @author wang.liang
 */
public enum GlobalStoppedReason {

    /**
     * empty
     */
    Empty(0),

    /**
     * Branch phase two commit failed un retryable
     */
    Branch_CommitFailed_Unretryable(10),

    /**
     * Branch phase two rollback failed un retryable
     */
    Branch_RollbackFailed_Unretryable(11),

    /**
     * Triggered the max count of the retry strategy
     */
    Triggered_Retry_Strategy_MaxCount(20),

    /**
     * Triggered the expiration time of the retry strategy
     */
    //This value is reserved.
    Triggered_Retry_Strategy_Expire(21);

    private int code;

    GlobalStoppedReason(int code) {
        this.code = code;
    }


    /**
     * Gets code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }


    private static final Map<Integer, GlobalStoppedReason> MAP = new HashMap<>(values().length);

    static {
        for (GlobalStoppedReason status : values()) {
            MAP.put(status.code, status);
        }
    }

    /**
     * Get stopped reason.
     *
     * @param code the code
     * @return the stopped reason
     */
    public static GlobalStoppedReason get(byte code) {
        return get((int)code);
    }

    /**
     * Get stopped reason.
     *
     * @param code the code
     * @return the stopped reason
     */
    public static GlobalStoppedReason get(int code) {
        GlobalStoppedReason status = MAP.get(code);

        if (null == status) {
            throw new IllegalArgumentException("Unknown StoppedReason[" + code + "]");
        }

        return status;
    }
}
