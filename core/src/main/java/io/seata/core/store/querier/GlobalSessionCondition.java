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
package io.seata.core.store.querier;

import java.util.Date;

import io.seata.common.util.ComparableUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.standard.GlobalTableField;
import io.seata.core.store.standard.GlobalTransactionModel;

import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_DATA;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_ID;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_BEGIN_TIME;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_GMT_CREATE;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_STATUS;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_TIMEOUT;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_NAME;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_SERVICE_GROUP;
import static io.seata.core.constants.ServerTableColumnsName.GLOBAL_TABLE_XID;

/**
 * The type GlobalTransaction condition.
 *
 * @author wang.liang
 */
public class GlobalSessionCondition extends AbstractQuerier<GlobalTransactionModel> {

    //region Condition fields

    /**
     * condition: xid = ?
     */
    protected String xid;

    /**
     * condition: status in (?, ?, ..., ?)
     */
    protected GlobalStatus[] statuses;

    /**
     * filter is timeout or not timeout data
     * -  null: all..............   // pass this condition
     * -  true: timeout data.....   condition: begin_time  < System.currentTimeMillis() - timeout
     * - false: not timeout data.   condition: begin_time >= System.currentTimeMillis() - timeout
     */
    protected Boolean isTimeoutData;

    /**
     * condition: begin_time < System.currentTimeMillis() - :overTimeAliveMills
     */
    protected long overTimeAliveMills = 0L;

    /**
     * condition: gmt_modified >= :minGmtModified
     */
    protected Date minGmtModified;

    //endregion

    //region Constructor

    /**
     * Instantiates a new condition.
     */
    public GlobalSessionCondition() {
    }

    /**
     * Instantiates a new condition.
     *
     * @param statuses the statuses
     */
    public GlobalSessionCondition(GlobalStatus... statuses) {
        this.statuses = statuses;
    }

    /**
     * Instantiates a new condition.
     *
     * @param status   the status
     * @param pageSize the page size
     */
    public GlobalSessionCondition(GlobalStatus status, int pageSize) {
        this(new GlobalStatus[]{status}, pageSize);
    }

    /**
     * Instantiates a new condition.
     *
     * @param status     the status
     * @param pageNumber the page number
     * @param pageSize   the page size
     */
    public GlobalSessionCondition(GlobalStatus status, int pageNumber, int pageSize) {
        this(new GlobalStatus[]{status}, pageNumber, pageSize);
    }

    /**
     * Instantiates a new condition.
     *
     * @param statuses the statuses
     * @param pageSize the page size
     */
    public GlobalSessionCondition(GlobalStatus[] statuses, int pageSize) {
        this.statuses = statuses;
        this.pageSize = pageSize;
    }

    /**
     * Instantiates a new condition.
     *
     * @param statuses   the statuses
     * @param pageNumber the page number
     * @param pageSize   the page size
     */
    public GlobalSessionCondition(GlobalStatus[] statuses, int pageNumber, int pageSize) {
        this.statuses = statuses;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    /**
     * Instantiates a new condition.
     *
     * @param overTimeAliveMills the over time alive mills
     */
    public GlobalSessionCondition(long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
    }

    //endregion

    //region Override Querier.isMatch, AbstractQuerier.compareByFieldName

    /**
     * Match data.
     *
     * @param globalTransaction the global transaction
     * @return the boolean
     */
    @Override
    public <D extends GlobalTransactionModel> boolean isMatch(D globalTransaction) {
        if (globalTransaction == null) {
            return false;
        }

        // where
        // xid = ?
        if (StringUtils.isNotBlank(xid)) {
            if (!xid.equals(globalTransaction.getXid())) {
                return false; // un match
            }
        }
        // status in (?, ?, ..., ?)
        if (statuses != null && statuses.length > 0) {
            if (!this.hasStatus(globalTransaction.getStatusCode())) {
                return false; // un match
            }
        }
        //  true: begin_time  < System.currentTimeMillis() - timeout
        // false: begin_time >= System.currentTimeMillis() - timeout
        if (isTimeoutData != null) {
            boolean isTimeout = globalTransaction.getBeginTime() < System.currentTimeMillis() - globalTransaction.getTimeout();
            if (isTimeoutData != isTimeout) {
                return false; // un match
            }
        }
        // begin_time < System.currentTimeMillis() - ?
        if (overTimeAliveMills > 0) {
            if (globalTransaction.getBeginTime() >= System.currentTimeMillis() - overTimeAliveMills) {
                return false; // un match
            }
        }
        // gmt_modified >= :minGmtModified
        if (minGmtModified != null) {
            if (globalTransaction.getGmtModified() == null
                    || globalTransaction.getGmtModified().getTime() < minGmtModified.getTime()) {
                return false; // un match
            }
        }

        // is match
        return true;
    }

    /**
     * Compare by field name.
     *
     * @param a             the object a
     * @param b             the object b
     * @param sortFieldName the sort field name
     * @return the compare result
     */
    @Override
    public <D extends GlobalTransactionModel> int compareByFieldName(D a, D b, String sortFieldName) {
        switch (sortFieldName) {
            case GLOBAL_TABLE_XID:
                return ComparableUtils.compare(a.getXid(), b.getXid());
            case GLOBAL_TABLE_TRANSACTION_ID:
                return ComparableUtils.compare(a.getTransactionId(), b.getTransactionId());
            case GLOBAL_TABLE_STATUS:
                return ComparableUtils.compare(a.getStatusCode(), b.getStatusCode());
            case GLOBAL_TABLE_APPLICATION_ID:
                return ComparableUtils.compare(a.getApplicationId(), b.getApplicationId());
            case GLOBAL_TABLE_TRANSACTION_SERVICE_GROUP:
                return ComparableUtils.compare(a.getTransactionServiceGroup(), b.getTransactionServiceGroup());
            case GLOBAL_TABLE_TRANSACTION_NAME:
                return ComparableUtils.compare(a.getTransactionName(), b.getTransactionName());
            case GLOBAL_TABLE_TIMEOUT:
                return ComparableUtils.compare(a.getTimeout(), b.getTimeout());
            case GLOBAL_TABLE_BEGIN_TIME:
                return ComparableUtils.compare(a.getBeginTime(), b.getBeginTime());
            case GLOBAL_TABLE_APPLICATION_DATA:
                return ComparableUtils.compare(a.getApplicationData(), b.getApplicationData());
            case GLOBAL_TABLE_GMT_CREATE:
                return ComparableUtils.compare(a.getGmtCreate(), b.getGmtCreate());
            case GLOBAL_TABLE_GMT_MODIFIED:
                return ComparableUtils.compare(a.getGmtModified(), b.getGmtModified());
            default:
                throw new RuntimeException("Unknown or not support sort field name: " + sortFieldName);
        }
    }

    //endregion

    //region Private methods

    private boolean hasStatus(int statusCode) {
        for (GlobalStatus status : statuses) {
            if (status.getCode() == statusCode) {
                return true;
            }
        }
        return false;
    }

    //endregion

    //region Gets and Sets methods

    public String getXid() {
        return xid;
    }

    public GlobalSessionCondition setXid(String xid) {
        this.xid = xid;
        return this;
    }

    public GlobalStatus[] getStatuses() {
        return statuses;
    }

    public GlobalSessionCondition setStatuses(GlobalStatus... statuses) {
        this.statuses = statuses;
        return this;
    }

    public Boolean getTimeoutData() {
        return isTimeoutData;
    }

    public GlobalSessionCondition setTimeoutData(Boolean timeoutData) {
        isTimeoutData = timeoutData;
        return this;
    }

    public long getOverTimeAliveMills() {
        return overTimeAliveMills;
    }

    public GlobalSessionCondition setOverTimeAliveMills(long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
        return this;
    }

    public Date getMinGmtModified() {
        return minGmtModified;
    }

    public GlobalSessionCondition setMinGmtModified(Date minGmtModified) {
        this.minGmtModified = minGmtModified;
        return this;
    }

    //region Special Gets and Sets

    /**
     * Sets sort fields, and all fields use SortOrder.ASC
     *
     * @param sortFields the sort fields
     */
    public GlobalSessionCondition setSortFields(GlobalTableField... sortFields) {
        if (sortFields.length == 0) {
            return this;
        }
        SortParam[] sortParams = new SortParam[sortFields.length];
        for (int i = 0, l = sortFields.length; i < l; ++i) {
            sortParams[i] = new SortParam(sortFields[i].getFieldName());
        }
        super.setSortParams(sortParams);
        return this;
    }

    //endregion

    //endregion
}
