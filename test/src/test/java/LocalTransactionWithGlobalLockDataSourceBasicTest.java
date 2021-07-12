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
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.CommitType;
import io.seata.core.model.Resource;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.RMClient;
import io.seata.rm.datasource.DataSourceManager;
import io.seata.tm.TMClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;

/**
 * The type Data source basic test.
 *
 * @author services
 */
@Disabled
public class LocalTransactionWithGlobalLockDataSourceBasicTest {

    private static ClassPathXmlApplicationContext context;
    private static JdbcTemplate jdbcTemplate;
    private static JdbcTemplate directJdbcTemplate;
    private static final String APPLICATION_ID = "my_test_app";
    private static final String TX_SERVICE_GROUP = "my_test_tx_group";

    /**
     * Before.
     */
    @BeforeAll
    public static void before() {
        // Mock DataSourceManager
        initClient();
        DefaultResourceManager.mockResourceManager(BranchType.AT, new MockDataSourceManager());
        context = new ClassPathXmlApplicationContext(
                "basic-test-context.xml");
        jdbcTemplate = (JdbcTemplate) context
                .getBean("jdbcTemplate");
        directJdbcTemplate = (JdbcTemplate) context
                .getBean("directJdbcTemplate");

        directJdbcTemplate.execute("delete from user0");
        directJdbcTemplate.execute("delete from user1");

    }

    /**
     * Test insert.
     */
    @Test
    public void testInsert() {
        RootContext.bindGlobalLockFlag();
        jdbcTemplate.update("insert into user0 (id, name, gmt) values (?, ?, ?)",
                new Object[]{2, "xxx", new Date()});
    }

    /**
     * Test insert.
     */
    @Test
    public void testInsertWithLock() {
        RootContext.bindGlobalLockFlag();
        new AbstractLockConflictExecuteTemplate() {
            @Override
            public void doExecute() {
                jdbcTemplate.update("insert into user0 (id, name, gmt) values (?, ?, ?)",
                        new Object[]{3, "xxx", new Date()});
            }
        }.execute();
    }

    /**
     * Test update.
     */
    @Test
    public void testUpdate() {
        RootContext.bindGlobalLockFlag();
        jdbcTemplate.update("update user0 a set a.name = 'yyyy' where a.id = ?", new Object[]{1});
    }

    @Test
    public void testUpdateWithLock() {
        RootContext.bindGlobalLockFlag();
        RootContext.bindGlobalLockFlag();
        new AbstractLockConflictExecuteTemplate() {
            @Override
            public void doExecute() {
                jdbcTemplate.update("update user0 a set a.name = 'yyyy' where a.id = ?", new Object[]{1});

            }
        }.execute();
    }

    /**
     * Test update with alias 1.
     */
    @Test
    public void testUpdateWithAlias1() {

        directJdbcTemplate.update("delete from User1 where Id = ?",
                new Object[]{1});
        directJdbcTemplate.update("insert into User1 (Id, Name, gMt) values (?, ?, ?)",
                new Object[]{1, "xxx", new Date()});

        RootContext.bindGlobalLockFlag();
        jdbcTemplate.update("update User1 a set a.Name = 'yyy' where a.Name = ?", new Object[]{"xxx"});
    }

    @Test
    public void testUpdateWithAlias1WithLockConflict() {

        directJdbcTemplate.update("delete from User1 where Id = ?",
                new Object[]{1});
        directJdbcTemplate.update("insert into User1 (Id, Name, gMt) values (?, ?, ?)",
                new Object[]{1, "xxx", new Date()});

        RootContext.bindGlobalLockFlag();
        new AbstractLockConflictExecuteTemplate() {

            @Override
            public void doExecute() {
                jdbcTemplate.update("update User1 a set a.Name = 'yyy' where a.Name = ?", new Object[]{"xxx"});
            }
        };

    }

    /**
     * Test delete.
     */
    @Test
    public void testDelete() {
        RootContext.bindGlobalLockFlag();
        jdbcTemplate.update("delete from user0 where id = ?", new Object[]{2});
    }

    /**
     * Test delete.
     */
    @Test
    public void testDeleteForLockConflict() {
        RootContext.bindGlobalLockFlag();
        new AbstractLockConflictExecuteTemplate() {

            @Override
            public void doExecute() {
                jdbcTemplate.update("delete from user0 where id = ?", new Object[]{2});
            }
        };
    }

    /**
     * Test select for update.
     */
    @Test
    public void testSelectForUpdate() {
        RootContext.bindGlobalLockFlag();
        jdbcTemplate.queryForRowSet("select a.name from user0 a where a.id = ? for update", new Object[]{1});
    }

    /**
     * Test select for update.
     */
    @Test
    public void testSelectForUpdateWithLockConflict() {
        RootContext.bindGlobalLockFlag();
        new AbstractLockConflictExecuteTemplate() {

            @Override
            public void doExecute() {
                jdbcTemplate.queryForRowSet("select a.name from user0 a where a.id = ? for update", new Object[]{1});
            }
        };
    }

    public static class MockDataSourceManager extends DataSourceManager {

        @Override
        public Long branchRegister(BranchType branchType, CommitType commitType, String resourceId, String clientId, String xid, String applicationData, String lockKeys)
                throws TransactionException {
            throw new RuntimeException("this method should not be called!");
        }

        @Override
        public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
            throw new RuntimeException("this method should not be called!");
        }

        @Override
        public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
                throws TransactionException {
            return true;
        }

        @Override
        public void registerResource(Resource resource) {

        }

        @Override
        public void unregisterResource(Resource resource) {

        }

        @Override
        public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
            throw new RuntimeException("this method should not be called!");
        }

        @Override
        public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId, String applicationData)
                throws TransactionException {
            throw new RuntimeException("this method should not be called!");
        }
    }

    private static void initClient() {
        TMClient.init(APPLICATION_ID, TX_SERVICE_GROUP);
        RMClient.init(APPLICATION_ID, TX_SERVICE_GROUP);
    }

    private abstract static class AbstractLockConflictExecuteTemplate {
        public void execute() {
            synchronized (LocalTransactionWithGlobalLockDataSourceBasicTest.class) {
                DefaultResourceManager.mockResourceManager(BranchType.AT, new MockDataSourceManager() {
                    @Override
                    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
                            throws TransactionException {
                        return false;
                    }
                });

                boolean exceptionOccour = false;
                try {
                    doExecute();
                } catch (UncategorizedSQLException e) {
                    exceptionOccour = true;
                    Assertions.assertTrue(e.getMessage().contains("LockConflict"), "not lock Conflict exception");
                } finally {
                    DefaultResourceManager.mockResourceManager(BranchType.AT, new MockDataSourceManager());
                }

                Assertions.assertTrue(exceptionOccour, "Lock Exception not occur!");
            }
        }

        public abstract void doExecute();

    }

    /**
     * After.
     */
    @AfterAll
    public static void after() {
        if (context != null) {
            context.close();
        }
    }
}
