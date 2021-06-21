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
package io.seata.core.store.standard;

import io.seata.core.constants.ServerTableColumnsName;

/**
 * @author wang.liang
 */
public enum GlobalTableField {

    /**
     * xid
     */
    XID(ServerTableColumnsName.GLOBAL_TABLE_XID),

    /**
     * transaction_id
     */
    TRANSACTION_ID(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID),

    /**
     * status
     */
    STATUS(ServerTableColumnsName.GLOBAL_TABLE_STATUS),

    /**
     * application_id
     */
    APPLICATION_ID(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_ID),

    /**
     * transaction_service_group
     */
    TRANSACTION_SERVICE_GROUP(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_SERVICE_GROUP),

    /**
     * transaction_name
     */
    TRANSACTION_NAME(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_NAME),

    /**
     * timeout
     */
    TIMEOUT(ServerTableColumnsName.GLOBAL_TABLE_TIMEOUT),

    /**
     * begin_time
     */
    BEGIN_TIME(ServerTableColumnsName.GLOBAL_TABLE_BEGIN_TIME),

    /**
     * application_data
     */
    APPLICATION_DATA(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_DATA),

    /**
     * gmt_create
     */
    GMT_CREATE(ServerTableColumnsName.GLOBAL_TABLE_GMT_CREATE),

    /**
     * gmt_modified
     */
    GMT_MODIFIED(ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED);

    private String fieldName;

    GlobalTableField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return this.fieldName;
    }
}
