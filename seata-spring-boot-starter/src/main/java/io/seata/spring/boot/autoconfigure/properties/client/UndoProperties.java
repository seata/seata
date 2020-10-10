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
package io.seata.spring.boot.autoconfigure.properties.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_ONLY_CARE_UPDATE_COLUMNS;
import static io.seata.common.DefaultValues.DEFAULT_TRANSACTION_UNDO_DATA_VALIDATION;
import static io.seata.common.DefaultValues.DEFAULT_TRANSACTION_UNDO_LOG_SERIALIZATION;
import static io.seata.common.DefaultValues.DEFAULT_TRANSACTION_UNDO_LOG_TABLE;
import static io.seata.common.DefaultValues.DEFAULT_DO_NOT_CARE_ON_UPDATE_TIMESTAMP_WHEN_INSERT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.UNDO_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = UNDO_PREFIX)
public class UndoProperties {
    private boolean dataValidation = DEFAULT_TRANSACTION_UNDO_DATA_VALIDATION;
    private String logSerialization = DEFAULT_TRANSACTION_UNDO_LOG_SERIALIZATION;
    private String logTable = DEFAULT_TRANSACTION_UNDO_LOG_TABLE;
    private boolean onlyCareUpdateColumns = DEFAULT_ONLY_CARE_UPDATE_COLUMNS;
    private boolean doNotCareOnUpdateTimestampWhenInsert = DEFAULT_DO_NOT_CARE_ON_UPDATE_TIMESTAMP_WHEN_INSERT;

    public boolean isDataValidation() {
        return dataValidation;
    }

    public UndoProperties setDataValidation(boolean dataValidation) {
        this.dataValidation = dataValidation;
        return this;
    }

    public String getLogSerialization() {
        return logSerialization;
    }

    public UndoProperties setLogSerialization(String logSerialization) {
        this.logSerialization = logSerialization;
        return this;
    }

    public String getLogTable() {
        return logTable;
    }

    public UndoProperties setLogTable(String logTable) {
        this.logTable = logTable;
        return this;
    }

    public boolean isOnlyCareUpdateColumns() {
        return onlyCareUpdateColumns;
    }

    public UndoProperties setOnlyCareUpdateColumns(boolean onlyCareUpdateColumns) {
        this.onlyCareUpdateColumns = onlyCareUpdateColumns;
        return this;
    }

    public boolean isdoNotCareOnUpdateTimestampWhenInsert() {
        return doNotCareOnUpdateTimestampWhenInsert;
    }

    public UndoProperties setdoNotCareOnUpdateTimestampWhenInsert(boolean doNotCareOnUpdateTimestampWhenInsert) {
        this.doNotCareOnUpdateTimestampWhenInsert = doNotCareOnUpdateTimestampWhenInsert;
        return this;
    }
}
