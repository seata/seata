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
package io.seata.spring.schema;

import java.util.Arrays;
import java.util.Objects;

import io.seata.common.DefaultValues;
import io.seata.tm.api.transaction.Propagation;

/**
 * The type global transactional config
 *
 * @author xingfudeshi@gmail.com
 */
public class GlobalTransactionalConfig {
    private int timeoutMills = DefaultValues.DEFAULT_GLOBAL_TRANSACTION_TIMEOUT;
    private String name = "";
    private Class<? extends Throwable>[] rollbackFor = new Class[]{};
    private String[] rollbackForClassName = {};
    private Class<? extends Throwable>[] noRollbackFor = new Class[]{};
    private String[] noRollbackForClassName = {};
    private Propagation propagation = Propagation.REQUIRED;
    private int lockRetryInterval = 0;
    private int lockRetryTimes = -1;
    private String scanPackage;
    private String pattern;

    public int getTimeoutMills() {
        return timeoutMills;
    }

    public void setTimeoutMills(int timeoutMills) {
        this.timeoutMills = timeoutMills;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<? extends Throwable>[] getRollbackFor() {
        return rollbackFor;
    }

    public void setRollbackFor(Class<? extends Throwable>[] rollbackFor) {
        this.rollbackFor = rollbackFor;
    }

    public String[] getRollbackForClassName() {
        return rollbackForClassName;
    }

    public void setRollbackForClassName(String[] rollbackForClassName) {
        this.rollbackForClassName = rollbackForClassName;
    }

    public Class<? extends Throwable>[] getNoRollbackFor() {
        return noRollbackFor;
    }

    public void setNoRollbackFor(Class<? extends Throwable>[] noRollbackFor) {
        this.noRollbackFor = noRollbackFor;
    }

    public String[] getNoRollbackForClassName() {
        return noRollbackForClassName;
    }

    public void setNoRollbackForClassName(String[] noRollbackForClassName) {
        this.noRollbackForClassName = noRollbackForClassName;
    }

    public Propagation getPropagation() {
        return propagation;
    }

    public void setPropagation(Propagation propagation) {
        this.propagation = propagation;
    }

    public int getLockRetryInterval() {
        return lockRetryInterval;
    }

    public void setLockRetryInterval(int lockRetryInterval) {
        this.lockRetryInterval = lockRetryInterval;
    }

    public int getLockRetryTimes() {
        return lockRetryTimes;
    }

    public void setLockRetryTimes(int lockRetryTimes) {
        this.lockRetryTimes = lockRetryTimes;
    }

    public String getScanPackage() {
        return scanPackage;
    }

    public void setScanPackage(String scanPackage) {
        this.scanPackage = scanPackage;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return "GlobalTransactionalConfig{" +
            "timeoutMills=" + timeoutMills +
            ", name='" + name + '\'' +
            ", rollbackFor=" + Arrays.toString(rollbackFor) +
            ", rollbackForClassName=" + Arrays.toString(rollbackForClassName) +
            ", noRollbackFor=" + Arrays.toString(noRollbackFor) +
            ", noRollbackForClassName=" + Arrays.toString(noRollbackForClassName) +
            ", propagation=" + propagation +
            ", lockRetryInterval=" + lockRetryInterval +
            ", lockRetryTimes=" + lockRetryTimes +
            ", scanPackage='" + scanPackage + '\'' +
            ", pattern='" + pattern + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GlobalTransactionalConfig globalTransactionalConfig = (GlobalTransactionalConfig) o;
        return Objects.equals(scanPackage, globalTransactionalConfig.scanPackage) && Objects.equals(pattern, globalTransactionalConfig.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scanPackage, pattern);
    }
}
