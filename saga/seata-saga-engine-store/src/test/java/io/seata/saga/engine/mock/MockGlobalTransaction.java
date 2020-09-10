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
package io.seata.saga.engine.mock;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.transaction.SuspendedResourcesHolder;

/**
 * @author wang.liang
 */
public class MockGlobalTransaction implements GlobalTransaction {

    private final String xid;

    public MockGlobalTransaction(String xid) {
        this.xid = xid;
    }

    @Override
    public void begin() throws TransactionException {

    }

    @Override
    public void begin(int timeout) throws TransactionException {

    }

    @Override
    public void begin(int timeout, String name) throws TransactionException {

    }

    @Override
    public void commit() throws TransactionException {

    }

    @Override
    public void rollback() throws TransactionException {

    }

    @Override
    public SuspendedResourcesHolder suspend(boolean unbindXid) throws TransactionException {
        return null;
    }

    @Override
    public void resume(SuspendedResourcesHolder suspendedResourcesHolder) throws TransactionException {

    }

    @Override
    public GlobalStatus getStatus() throws TransactionException {
        return null;
    }

    @Override
    public String getXid() {
        return xid;
    }

    @Override
    public void globalReport(GlobalStatus globalStatus) throws TransactionException {

    }

    @Override
    public GlobalStatus getLocalStatus() {
        return null;
    }
}
