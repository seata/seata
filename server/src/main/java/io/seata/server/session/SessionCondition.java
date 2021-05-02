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
package io.seata.server.session;

import io.seata.core.model.GlobalStatus;

/**
 * The type Session condition.
 *
 * @author slievrly
 */
public class SessionCondition {
    private Long transactionId;
    private String xid;
    private GlobalStatus status;
    private GlobalStatus[] statuses;
    private long overTimeAliveMills;
    private Long expiredTime;

    /**
     * Instantiates a new Session condition.
     */
    public SessionCondition() {
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param xid the xid
     */
    public SessionCondition(String xid) {
        this.xid = xid;
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param status the status
     */
    public SessionCondition(GlobalStatus status) {
        this.status = status;
        statuses = new GlobalStatus[] {status};
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param statuses the statuses
     */
    public SessionCondition(GlobalStatus[] statuses) {
        this.statuses = statuses;
    }

    /**
     * Instantiates a new Session condition.
     *
     * @param overTimeAliveMills the over time alive mills
     */
    public SessionCondition(long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public GlobalStatus getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(GlobalStatus status) {
        this.status = status;
    }

    /**
     * Gets over time alive mills.
     *
     * @return the over time alive mills
     */
    public long getOverTimeAliveMills() {
        return overTimeAliveMills;
    }

    /**
     * Sets over time alive mills.
     *
     * @param overTimeAliveMills the over time alive mills
     */
    public void setOverTimeAliveMills(long overTimeAliveMills) {
        this.overTimeAliveMills = overTimeAliveMills;
    }

    public Long getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(Long expiredTime) {
        this.expiredTime = expiredTime;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public GlobalStatus[] getStatuses() {
        return statuses;
    }

    public void setStatuses(GlobalStatus[] statuses) {
        this.statuses = statuses;
    }
}
