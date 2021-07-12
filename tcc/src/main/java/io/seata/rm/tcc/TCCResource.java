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
package io.seata.rm.tcc;

import java.lang.reflect.Method;

import io.seata.core.model.BranchType;
import io.seata.core.model.CommitType;
import io.seata.core.model.Resource;

/**
 * The type Tcc resource.
 *
 * @author zhangsen
 */
public class TCCResource implements Resource {

    private String resourceGroupId = "DEFAULT";

    private String appName;

    private String actionName;

    private Object targetBean;

    private Method prepareMethod;

    private String commitMethodName;

    private Method commitMethod;

    private CommitType commitType;

    private String rollbackMethodName;

    private Method rollbackMethod;

    @Override
    public String getResourceGroupId() {
        return resourceGroupId;
    }

    /**
     * Sets resource group id.
     *
     * @param resourceGroupId the resource group id
     */
    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    @Override
    public String getResourceId() {
        return actionName;
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.TCC;
    }

    /**
     * Gets app name.
     *
     * @return the app name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Sets app name.
     *
     * @param appName the app name
     */
    public void setAppName(String appName) {
        this.appName = appName;
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
     * Gets target bean.
     *
     * @return the target bean
     */
    public Object getTargetBean() {
        return targetBean;
    }

    /**
     * Sets target bean.
     *
     * @param targetBean the target bean
     */
    public void setTargetBean(Object targetBean) {
        this.targetBean = targetBean;
    }

    /**
     * Gets prepare method.
     *
     * @return the prepare method
     */
    public Method getPrepareMethod() {
        return prepareMethod;
    }

    /**
     * Sets prepare method.
     *
     * @param prepareMethod the prepare method
     */
    public void setPrepareMethod(Method prepareMethod) {
        this.prepareMethod = prepareMethod;
    }

    /**
     * Gets commit method.
     *
     * @return the commit method
     */
    public Method getCommitMethod() {
        return commitMethod;
    }

    /**
     * Sets commit method.
     *
     * @param commitMethod the commit method
     */
    public void setCommitMethod(Method commitMethod) {
        this.commitMethod = commitMethod;
    }

    /**
     * Gets commit type.
     *
     * @return the commit type
     */
    public CommitType getCommitType() {
        return commitType;
    }

    /**
     * Sets commit type.
     *
     * @param commitType the commit type
     */
    public void setCommitType(CommitType commitType) {
        this.commitType = commitType;
    }

    /**
     * Gets rollback method.
     *
     * @return the rollback method
     */
    public Method getRollbackMethod() {
        return rollbackMethod;
    }

    /**
     * Sets rollback method.
     *
     * @param rollbackMethod the rollback method
     */
    public void setRollbackMethod(Method rollbackMethod) {
        this.rollbackMethod = rollbackMethod;
    }

    /**
     * Gets commit method name.
     *
     * @return the commit method name
     */
    public String getCommitMethodName() {
        return commitMethodName;
    }

    /**
     * Sets commit method name.
     *
     * @param commitMethodName the commit method name
     */
    public void setCommitMethodName(String commitMethodName) {
        this.commitMethodName = commitMethodName;
    }

    /**
     * Gets rollback method name.
     *
     * @return the rollback method name
     */
    public String getRollbackMethodName() {
        return rollbackMethodName;
    }

    /**
     * Sets rollback method name.
     *
     * @param rollbackMethodName the rollback method name
     */
    public void setRollbackMethodName(String rollbackMethodName) {
        this.rollbackMethodName = rollbackMethodName;
    }

    @Override
    public int hashCode() {
        return actionName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TCCResource)) {
            return false;
        }
        return this.actionName.equals(((TCCResource)obj).actionName);
    }
}
