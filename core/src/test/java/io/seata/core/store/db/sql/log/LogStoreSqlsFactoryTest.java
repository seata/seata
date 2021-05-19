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
package io.seata.core.store.db.sql.log;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author: will
 */
public class LogStoreSqlsFactoryTest {

    private static LogStoreSqls mysqlLog = LogStoreSqlsFactory.getLogStoreSqls("mysql");

    private static LogStoreSqls oracleLog = LogStoreSqlsFactory.getLogStoreSqls("oracle");

    private static LogStoreSqls pgLog = LogStoreSqlsFactory.getLogStoreSqls("postgresql");

    private static LogStoreSqls h2Log = LogStoreSqlsFactory.getLogStoreSqls("h2");

    private static LogStoreSqls oceanbase = LogStoreSqlsFactory.getLogStoreSqls("oceanbase");

    private static String globalTable = "global_table";

    private static String branchTable = "branch_table";

    @Test
    public void mysqlLogTest() {

        String sql = mysqlLog.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getUpdateGlobalTransactionSQL(globalTable, "status = ?,");
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getUpdateBranchTransactionSQL(branchTable, "status = ?,");
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = mysqlLog.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }

    @Test
    public void oracleLogTest() {

        String sql = oracleLog.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getUpdateGlobalTransactionSQL(globalTable, "status = ?,");
        Assertions.assertNotNull(sql);
        sql = oracleLog.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getUpdateBranchTransactionSQL(branchTable, "status = ?,");
        Assertions.assertNotNull(sql);
        sql = oracleLog.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = oracleLog.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }

    @Test
    public void pgLogTest() {

        String sql = pgLog.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getUpdateGlobalTransactionSQL(globalTable, "status = ?,");
        Assertions.assertNotNull(sql);
        sql = pgLog.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getUpdateBranchTransactionSQL(branchTable, "status = ?,");
        Assertions.assertNotNull(sql);
        sql = pgLog.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = pgLog.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }

    @Test
    public void h2LogTest() {

        String sql = h2Log.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getUpdateGlobalTransactionSQL(globalTable, "status = ?,");
        Assertions.assertNotNull(sql);
        sql = h2Log.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getUpdateBranchTransactionSQL(branchTable, "status = ?,");
        Assertions.assertNotNull(sql);
        sql = h2Log.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = h2Log.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }

    @Test
    public void oceanbaseLogTest() {

        String sql = oceanbase.getInsertGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getUpdateGlobalTransactionSQL(globalTable, "status = ?,");
        Assertions.assertNotNull(sql);
        sql = oceanbase.getDeleteGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryGlobalTransactionSQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryGlobalTransactionSQLByTransactionId(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryGlobalTransactionSQLByStatus(globalTable, "1");
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryGlobalTransactionForRecoverySQL(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getInsertBranchTransactionSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getUpdateBranchTransactionSQL(branchTable, "status = ?,");
        Assertions.assertNotNull(sql);
        sql = oceanbase.getDeleteBranchTransactionByBranchIdSQL(branchTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getDeleteBranchTransactionByXId(branchTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryBranchTransaction(branchTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryBranchTransaction(branchTable, "1");
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryGlobalMax(globalTable);
        Assertions.assertNotNull(sql);
        sql = oceanbase.getQueryBranchMax(branchTable);
        Assertions.assertNotNull(sql);
    }
}
