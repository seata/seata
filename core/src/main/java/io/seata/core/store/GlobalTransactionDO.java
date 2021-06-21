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
package io.seata.core.store;

import io.seata.common.util.StringUtils;
import io.seata.core.store.standard.GlobalTransactionModel;

import java.util.Date;

/**
 * Global Transaction data object
 *
 * @author zhangsen
 */
public class GlobalTransactionDO implements GlobalTransactionModel {

    private String xid;

    private Long transactionId;

    private Integer status;

    private String applicationId;

    private String transactionServiceGroup;

    private String transactionName;

    private Integer timeout;

    private Long beginTime;

    private String applicationData;

    private Date gmtCreate;

    private Date gmtModified;

    /**
     * Gets xid.
     *
     * @return the xid
     */
    @Override
    public String getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    @Override
    public void setXid(String xid) {
        this.xid = xid;
    }

    /**
     * Gets status code.
     *
     * @return the status code
     */
    @Override
    public int getStatusCode() {
        return status;
    }

    /**
     * Sets status code.
     *
     * @param statusCode the status code
     */
    @Override
    public void setStatusCode(int statusCode) {
        this.status = statusCode;
    }

    /**
     * Gets application id.
     *
     * @return the application id
     */
    @Override
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Sets application id.
     *
     * @param applicationId the application id
     */
    @Override
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Gets transaction service group.
     *
     * @return the transaction service group
     */
    @Override
    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    /**
     * Sets transaction service group.
     *
     * @param transactionServiceGroup the transaction service group
     */
    @Override
    public void setTransactionServiceGroup(String transactionServiceGroup) {
        this.transactionServiceGroup = transactionServiceGroup;
    }

    /**
     * Gets transaction name.
     *
     * @return the transaction name
     */
    @Override
    public String getTransactionName() {
        return transactionName;
    }

    /**
     * Sets transaction name.
     *
     * @param transactionName the transaction name
     */
    @Override
    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    @Override
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     */
    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Gets begin time.
     *
     * @return the begin time
     */
    @Override
    public long getBeginTime() {
        return beginTime;
    }

    /**
     * Sets begin time.
     *
     * @param beginTime the begin time
     */
    @Override
    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    /**
     * Gets transaction id.
     *
     * @return the transaction id
     */
    @Override
    public long getTransactionId() {
        return transactionId;
    }

    /**
     * Sets transaction id.
     *
     * @param transactionId the transaction id
     */
    @Override
    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets application data.
     *
     * @return the application data
     */
    @Override
    public String getApplicationData() {
        return applicationData;
    }

    /**
     * Sets application data.
     *
     * @param applicationData the application data
     */
    @Override
    public void setApplicationData(String applicationData) {
        this.applicationData = applicationData;
    }

    /**
     * Gets gmt create.
     *
     * @return the gmt create
     */
    @Override
    public Date getGmtCreate() {
        return gmtCreate;
    }

    /**
     * Sets gmt create.
     *
     * @param gmtCreate the gmt create
     */
    @Override
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /**
     * Gets gmt modified.
     *
     * @return the gmt modified
     */
    @Override
    public Date getGmtModified() {
        return gmtModified;
    }

    /**
     * Sets gmt modified.
     *
     * @param gmtModified the gmt modified
     */
    @Override
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    /**
     * Sets transactionId
     * @param transactionId the transactionId
     */
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Sets timeout
     * @param timeout the timeout
     */
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    /**
     * Sets begin time
     * @param beginTime the begin time
     */
    public void setBeginTime(Long beginTime) {
        this.beginTime = beginTime;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
