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
package io.seata.tm.api.transaction;

import java.io.Serializable;
import java.util.Set;
import io.seata.common.util.CollectionUtils;

/**
 * @author guoyao
 */
public final class TransactionInfo implements Serializable {

    private int timeOut;

    private String name;

    private Set<RollbackRule> rollbackRules;

    private Propagation propagation;

    private Set<TransactionHook> transactionHooks;

    private int lockRetryInternal;

    private int lockRetryTimes;

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<RollbackRule> getRollbackRules() {
        return rollbackRules;
    }

    public void setRollbackRules(Set<RollbackRule> rollbackRules) {
        this.rollbackRules = rollbackRules;
    }

    public boolean rollbackOn(Throwable ex) {

        RollbackRule winner = null;
        int deepest = Integer.MAX_VALUE;

        if (CollectionUtils.isNotEmpty(rollbackRules)) {
            winner = NoRollbackRule.DEFAULT_NO_ROLLBACK_RULE;
            for (RollbackRule rule : this.rollbackRules) {
                int depth = rule.getDepth(ex);
                if (depth >= 0 && depth < deepest) {
                    deepest = depth;
                    winner = rule;
                }
            }
        }

        return !(winner instanceof NoRollbackRule);
    }

    public Propagation getPropagation() {
        if (this.propagation != null) {
            return this.propagation;
        }
        //default propagation
        return Propagation.REQUIRED;
    }

    public void setPropagation(Propagation propagation) {
        this.propagation = propagation;
    }

    public Set<TransactionHook> getTransactionHooks() {
        return transactionHooks;
    }

    public void setTransactionHooks(Set<TransactionHook> transactionHooks) {
        this.transactionHooks = transactionHooks;
    }

    public int getLockRetryInternal() {
        return lockRetryInternal;
    }

    public void setLockRetryInternal(int lockRetryInternal) {
        this.lockRetryInternal = lockRetryInternal;
    }

    public int getLockRetryTimes() {
        return lockRetryTimes;
    }

    public void setLockRetryTimes(int lockRetryTimes) {
        this.lockRetryTimes = lockRetryTimes;
    }

}
