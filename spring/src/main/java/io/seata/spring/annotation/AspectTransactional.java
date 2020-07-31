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
package io.seata.spring.annotation;

import io.seata.tm.api.transaction.Propagation;
import io.seata.tm.api.transaction.TransactionInfo;

/**
 * @author funkye
 */
public class AspectTransactional {
    /**
     * Global transaction timeoutMills in MILLISECONDS.
     *
     * @return timeoutMills in MILLISECONDS.
     */
    private int timeoutMills = TransactionInfo.DEFAULT_TIME_OUT;

    /**
     * Given name of the global transaction instance.
     *
     * @return Given name.
     */
    private String name = "";

    /**
     * roll back for the Class
     * 
     * @return
     */
    private Class<? extends Throwable>[] rollbackFor = new Class[] {};

    /**
     * roll back for the class name
     * 
     * @return
     */
    private String[] rollbackForClassName = {};

    /**
     * not roll back for the Class
     * 
     * @return
     */
    private Class<? extends Throwable>[] noRollbackFor = new Class[] {};

    /**
     * not roll back for the class name
     * 
     * @return
     */
    private String[] noRollbackForClassName = {};

    /**
     * the propagation of the global transaction
     * 
     * @return
     */
    private Propagation propagation = Propagation.REQUIRED;

    public AspectTransactional() {}

    public AspectTransactional(int timeoutMills, String name, Class<? extends Throwable>[] rollbackFor,
        String[] rollbackForClassName, Class<? extends Throwable>[] noRollbackFor, String[] noRollbackForClassName,
        Propagation propagation) {
        this.timeoutMills = timeoutMills;
        this.name = name;
        this.rollbackFor = rollbackFor;
        this.rollbackForClassName = rollbackForClassName;
        this.noRollbackFor = noRollbackFor;
        this.noRollbackForClassName = noRollbackForClassName;
        this.propagation = propagation;
    }

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

}
