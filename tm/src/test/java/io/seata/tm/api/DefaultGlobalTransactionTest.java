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
package io.seata.tm.api;


import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.tm.TransactionManagerHolder;
import io.seata.tm.api.transaction.MyRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author wangwei
 */
class DefaultGlobalTransactionTest {
    private static final String DEFAULT_XID = "1234567890";

    @BeforeAll
    public static void init() {

        TransactionManagerHolder.set(new TransactionManager() {
            @Override
            public String begin(String applicationId, String transactionServiceGroup, String name, int timeout)
                    throws TransactionException {
                return DEFAULT_XID;
            }

            @Override
            public GlobalStatus commit(String xid) throws TransactionException {
                throw new MyRuntimeException("");
            }

            @Override
            public GlobalStatus rollback(String xid) throws TransactionException {
                throw new MyRuntimeException("");
            }

            @Override
            public GlobalStatus getStatus(String xid) throws TransactionException {
              throw new MyRuntimeException("");
            }

            @Override
            public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
                return globalStatus;
            }
        });
    }


    @Test
    public void commitRetryExceptionTest() throws TransactionException {
        RootContext.unbind();
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        tx.begin();
        Assertions.assertThrows(TransactionException.class, tx::commit);
    }

    @Test
    public void commitNoXIDExceptionTest() throws TransactionException {
        RootContext.unbind();
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        Assertions.assertThrows(IllegalStateException.class, tx::commit);
    }


    @Test
    public void rollBackRetryExceptionTest() throws TransactionException {
        RootContext.unbind();
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        tx.begin();
        Assertions.assertThrows(TransactionException.class, tx::rollback);
    }

    @Test
    public void rollBackNoXIDExceptionTest() throws TransactionException {
        RootContext.unbind();
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        tx.begin();
        Assertions.assertThrows(TransactionException.class, tx::rollback);
    }
    
    @Test
    public void testExistingTransaction() {
        RootContext.bind(DEFAULT_XID);
        DefaultGlobalTransaction defaultGlobalTransaction = new DefaultGlobalTransaction();
        Assertions.assertTrue(defaultGlobalTransaction.existingTransaction(), "Existing transaction");

        RootContext.unbind();
        Assertions.assertFalse(defaultGlobalTransaction.existingTransaction(), "No existing transaction");
    }
}
