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
package io.seata.console.constant;

/**
 * The Code for the response of message
 *
 * @author jameslcj
 */
public enum Code {
    /**
     * response success
     */
    SUCCESS(200, "ok"),
    /**
     * server error
     */
    ERROR(500, "Server error"),
    /**
     * the custom error
     */
    LOGIN_FAILED(401, "Login failed");

    public int code;

    public String msg;

    private Code(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static String getErrorMsg(int code) {
        Code[] errorCodes = values();
        for (Code errCode : errorCodes) {
            if (errCode.getCode() == code) {
                return errCode.getMsg();
            }
        }
        return null;
    }
}

