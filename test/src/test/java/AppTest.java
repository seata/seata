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
import io.seata.common.ApplicationKeeper;
import io.seata.rm.RMClient;
import io.seata.tm.TMClient;
import io.seata.tm.api.TransactionalExecutor;
import io.seata.tm.api.TransactionalTemplate;
import io.seata.tm.api.transaction.TransactionInfo;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The type App test.
 *
 * @author sharajava
 */
public class AppTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppTest.class);

    private static final String APPLICATION_ID = "my_test_app";
    private static final String TX_SERVICE_GROUP = "my_test_tx_group";
    private static final String TX_NAME = "my_tx_instance";
    private static final int TX_TIME_OUT = 60000;

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) throws Throwable {
        TMClient.init(APPLICATION_ID, TX_SERVICE_GROUP);
        RMClient.init(APPLICATION_ID, TX_SERVICE_GROUP);

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "basic-test-context.xml");

        final JdbcTemplate jdbcTemplate = (JdbcTemplate) context
                .getBean("jdbcTemplate");

        jdbcTemplate.update("delete from undo_log");
        jdbcTemplate.update("delete from user0");
        jdbcTemplate.update("insert into user0 (id, name, gmt) values (1, 'user0', '2019-01-01')");
        jdbcTemplate.update("delete from user1");
        final MyBusinessException bizException = new MyBusinessException("mock bizException");
        try {
            TransactionalTemplate.execute(new TransactionalExecutor() {
                @Override
                public Object execute() throws Throwable {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Exception Rollback Business Begin ...");
                    }
                    jdbcTemplate.update("update user0 set name = 'xxx' where id = ?", new Object[]{1});
                    jdbcTemplate.update("insert into user1 (id, name, gmt) values (1, 'user1', '2019-01-01')");
                    throw bizException;
                }

                @Override
                public TransactionInfo getTransactionInfo() {
                    TransactionInfo txInfo = new TransactionInfo();
                    txInfo.setTimeOut(TX_TIME_OUT);
                    txInfo.setName(TX_NAME);
                    return txInfo;
                }

            });
        } catch (TransactionalExecutor.ExecutionException e) {
            TransactionalExecutor.Code code = e.getCode();
            if (code == TransactionalExecutor.Code.RollbackDone) {
                Throwable businessEx = e.getOriginalException();
                if (businessEx instanceof MyBusinessException) {
                    Assertions.assertEquals(((MyBusinessException) businessEx).getBusinessErrorCode(),
                            bizException.businessErrorCode);
                }
            } else {
                Assertions.assertFalse(false, "Not expected," + e.getMessage());

            }
        }
        new ApplicationKeeper(context).keep();

    }

    private static class MyBusinessException extends Exception {

        private String businessErrorCode;

        /**
         * Gets business error code.
         *
         * @return the business error code
         */
        public String getBusinessErrorCode() {
            return businessErrorCode;
        }

        /**
         * Sets business error code.
         *
         * @param businessErrorCode the business error code
         */
        public void setBusinessErrorCode(String businessErrorCode) {
            this.businessErrorCode = businessErrorCode;
        }

        /**
         * Instantiates a new My business exception.
         *
         * @param businessErrorCode the business error code
         */
        public MyBusinessException(String businessErrorCode) {
            this.businessErrorCode = businessErrorCode;
        }
    }
}
