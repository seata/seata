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

import java.io.Serializable;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.common.exception.FrameworkException;
import io.seata.rm.tcc.interceptor.ActionContextUtil;

/**
 * The type Business action context.
 */
public class BusinessActionContext implements Serializable {

    private static final long serialVersionUID = 6539226288677737991L;

    private String xid;

    private String branchId;

    private String actionName;

    /**
     * delay branch report while sharing params to tcc phase 2 to enhance performance
     *
     * @see io.seata.rm.tcc.api.BusinessActionContextUtil
     * @see io.seata.rm.tcc.api.TwoPhaseBusinessAction
     */
    private Boolean isDelayReport;

    /**
     * mark that actionContext has been updated by business
     *
     * @see io.seata.rm.tcc.api.BusinessActionContextUtil
     */
    private Boolean isUpdated;

    /**
     * action context
     */
    private Map<String, Object> actionContext;

    /**
     * Instantiates a new Business action context.
     */
    public BusinessActionContext() {
    }

    /**
     * Instantiates a new Business action context.
     *
     * @param xid           the xid
     * @param branchId      the branch id
     * @param actionContext the action context
     */
    public BusinessActionContext(String xid, String branchId, Map<String, Object> actionContext) {
        this.xid = xid;
        this.branchId = branchId;
        this.actionContext = actionContext;
    }

    /**
     * Gets action context.
     *
     * @param key the key
     * @return the action context
     */
    @Nullable
    public Object getActionContext(String key) {
        return actionContext.get(key);
    }

    /**
     * Gets action context.
     *
     * @param key         the key
     * @param targetClazz the target class
     * @param <T>         the target type
     * @return the action context of the target type
     */
    @Nullable
    public <T> T getActionContext(String key, @Nonnull Class<T> targetClazz) {
        Object value = actionContext.get(key);
        try {
            return ActionContextUtil.convertActionContext(value, targetClazz);
        } catch (RuntimeException e) {
            String errorMsg = String.format("Failed to convert the action context with key '%s' from '%s' to '%s'.",
                    key, value.getClass().getName(), targetClazz.getName());
            throw new FrameworkException(e, errorMsg);
        }
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public long getBranchId() {
        return branchId != null ? Long.parseLong(branchId) : -1;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(long branchId) {
        this.branchId = String.valueOf(branchId);
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    /**
     * Gets action context.
     *
     * @return the action context
     */
    public Map<String, Object> getActionContext() {
        return actionContext;
    }

    /**
     * Sets action context.
     *
     * @param actionContext the action context
     */
    public void setActionContext(Map<String, Object> actionContext) {
        this.actionContext = actionContext;
    }

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public String getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(String xid) {
        this.xid = xid;
    }

    /**
     * Gets action name.
     *
     * @return the action name
     */
    public String getActionName() {
        return actionName;
    }

    /**
     * Sets action name.
     *
     * @param actionName the action name
     */
    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    /**
     * add actionContext
     *
     * @param key   the action context's key
     * @param value biz value
     * @return the action context is changed
     * @see BusinessActionContextUtil // the TCC API utils
     * @deprecated Don't use this method in the `Try` method. Please use {@link BusinessActionContextUtil#addContext}
     */
    @Deprecated
    public boolean addActionContext(String key, Object value) {
        if (value == null) {
            return false;
        }

        Object previousValue = this.actionContext.put(key, value);
        boolean isChanged = !value.equals(previousValue);
        if (isChanged) {
            this.setUpdated(true);
        }
        return isChanged;
    }

    public Boolean getDelayReport() {
        return isDelayReport;
    }

    public void setDelayReport(Boolean delayReport) {
        isDelayReport = delayReport;
    }

    public Boolean getUpdated() {
        return isUpdated;
    }

    public void setUpdated(Boolean updated) {
        isUpdated = updated;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[xid:").append(xid)
                .append(",branch_Id:").append(branchId)
                .append(",action_name:").append(actionName)
                .append(",is_delay_report:").append(isDelayReport)
                .append(",is_updated:").append(isDelayReport)
                .append(",action_context:")
                .append(actionContext).append("]");
        return sb.toString();
    }
}
