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
import io.seata.tm.api.transaction.RollbackRule;
import io.seata.tm.api.transaction.TransactionInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The type Api test.
 */
public class APITest {

    private static final String DEFAULT_XID = "1234567890";
    private static final String TX_NAME = "test";
    private static final int TIME_OUT = 30000;

    /**
     * Init.
     */
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
                return GlobalStatus.Committed;
            }

            @Override
            public GlobalStatus rollback(String xid) throws TransactionException {
                return GlobalStatus.Rollbacked;
            }

            @Override
            public GlobalStatus getStatus(String xid) throws TransactionException {
                return GlobalStatus.Begin;
            }

            @Override
            public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
                return globalStatus;
            }
        });
    }

    /**
     * Clean root context.
     */
    @AfterEach
    public void cleanRootContext() {
        RootContext.unbind();
    }

    /**
     * Test current.
     *
     * @throws Exception the exception
     */
    @Test
    public void testCurrent() throws Exception {
        RootContext.bind(DEFAULT_XID);
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        Assertions.assertEquals(tx.getXid(), DEFAULT_XID);
        Assertions.assertEquals(tx.getStatus(), GlobalStatus.Begin);

    }

    /**
     * Test new tx.
     *
     * @throws Exception the exception
     */
    @Test
    public void testNewTx() throws Exception {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        Assertions.assertEquals(tx.getStatus(), GlobalStatus.UnKnown);
        Assertions.assertNull(tx.getXid());
    }

    /**
     * Test begin.
     *
     * @throws Exception the exception
     */
    @Test
    public void testBegin() throws Exception {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        tx.begin();
        Assertions.assertEquals(tx.getStatus(), GlobalStatus.Begin);
        Assertions.assertNotNull(tx.getXid());

    }

    /**
     * Test nested commit.
     *
     * @throws Exception the exception
     */
    @Test
    public void testNestedCommit() throws Throwable {
        TransactionalTemplate.execute(new AbstractTransactionalExecutor() {
            @Override
            public Object execute() throws Throwable {

                TransactionalTemplate.execute(new AbstractTransactionalExecutor() {
                    @Override
                    public Object execute() throws Throwable {

                        TransactionalTemplate.execute(new AbstractTransactionalExecutor() {
                            @Override
                            public Object execute() throws Throwable {
                                return null;
                            }
                        });
                        return null;
                    }
                });
                return null;
            }
        });
    }

    /**
     * Test nested rollback.
     *
     * @throws Exception the exception
     */
    @Test
    public void testNestedRollback() throws Throwable {

        final String oexMsg = "xxx";

        try {
            TransactionalTemplate.execute(new AbstractTransactionalExecutor() {
                @Override
                public Object execute() throws Throwable {

                    try {
                        TransactionalTemplate.execute(new AbstractTransactionalExecutor() {
                            @Override
                            public Object execute() throws Throwable {

                                try {
                                    TransactionalTemplate.execute(new AbstractTransactionalExecutor() {
                                        @Override
                                        public Object execute() throws Throwable {
                                            throw new RuntimeException(oexMsg);
                                        }
                                    });
                                } catch (TransactionalExecutor.ExecutionException ex) {
                                    throw ex.getOriginalException();
                                }
                                return null;
                            }
                        });
                    } catch (TransactionalExecutor.ExecutionException ex) {
                        throw ex.getOriginalException();
                    }
                    return null;
                }
            });
        } catch (TransactionalExecutor.ExecutionException ex) {
            Throwable oex = ex.getOriginalException();
            Assertions.assertEquals(oex.getMessage(), oexMsg);
        }
    }

    @Test
    public void testGlobalReport() throws Exception {
        RootContext.bind(DEFAULT_XID);
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        tx.globalReport(tx.getStatus());

        Assertions.assertThrows(IllegalStateException.class, () ->  tx.globalReport(null));
        Assertions.assertThrows(IllegalStateException.class, () -> {
            RootContext.unbind();
            GlobalTransaction tx2 = GlobalTransactionContext.getCurrentOrCreate();
            tx2.globalReport(tx2.getStatus());
        });

        Assertions.assertEquals(tx.getStatus(), GlobalStatus.Begin);
        Assertions.assertNotNull(tx.getXid());

    }


    private static abstract class AbstractTransactionalExecutor implements TransactionalExecutor {

        @Override
        public TransactionInfo getTransactionInfo() {
            TransactionInfo txInfo = new TransactionInfo();
            txInfo.setTimeOut(TIME_OUT);
            txInfo.setName(TX_NAME);
            return txInfo;
        }
    }
}
