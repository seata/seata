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
package io.seata.rm.tcc.api;

/**
 * The enum ParamType
 *
 * @author wang.liang
 */
public enum ParamType {

    /**
     * The param
     */
    PARAM("param"),

    /**
     * The field
     */
    FIELD("field");

    private final String code;

    ParamType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
