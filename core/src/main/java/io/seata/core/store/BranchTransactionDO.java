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

import java.util.Date;

import io.seata.common.util.StringUtils;
import io.seata.core.model.BranchStatus;
import io.seata.core.store.standard.BranchTransactionModel;

/**
 * branch transaction data object
 *
 * @author zhangsen
 */
public class BranchTransactionDO implements Comparable<BranchTransactionDO>, BranchTransactionModel {

    private String xid;

    private Long transactionId;

    private Long branchId;

    private String resourceGroupId;

    private String resourceId;

    private String branchType;

    private Integer status = BranchStatus.Unknown.getCode();

    private String clientId;

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
     * Gets branch id.
     *
     * @return the branch id
     */
    @Override
    public long getBranchId() {
        return branchId;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    @Override
    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    /**
     * Gets resource group id.
     *
     * @return the resource group id
     */
    @Override
    public String getResourceGroupId() {
        return resourceGroupId;
    }

    /**
     * Sets resource group id.
     *
     * @param resourceGroupId the resource group id
     */
    @Override
    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    /**
     * Gets resource id.
     *
     * @return the resource id
     */
    @Override
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Sets resource id.
     *
     * @param resourceId the resource id
     */
    @Override
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Gets branch type.
     *
     * @return the branch type
     */
    @Override
    public String getBranchType() {
        return branchType;
    }

    /**
     * Sets branch type.
     *
     * @param branchType the branch type
     */
    @Override
    public void setBranchType(String branchType) {
        this.branchType = branchType;
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
     * Gets client id.
     *
     * @return the client id
     */
    @Override
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets client id.
     *
     * @param clientId the client id
     */
    @Override
    public void setClientId(String clientId) {
        this.clientId = clientId;
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

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

    @Override
    public int compareTo(BranchTransactionDO branchTransactionDO) {
        return this.getGmtCreate().compareTo(branchTransactionDO.getGmtCreate());
    }

}
