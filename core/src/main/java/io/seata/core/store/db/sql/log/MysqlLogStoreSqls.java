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

import io.seata.common.loader.LoadLevel;
import io.seata.core.constants.ServerTableColumnsName;

/**
 * Database log store mysql sql
 * @author will
 */
@LoadLevel(name = "mysql")
public class MysqlLogStoreSqls extends AbstractLogStoreSqls {

    /**
     * The constant INSERT_GLOBAL_TRANSACTION_MYSQL.
     */
    public static final String INSERT_GLOBAL_TRANSACTION_MYSQL = "insert into " + GLOBAL_TABLE_PLACEHOLD
            + "(" + ALL_GLOBAL_COLUMNS + ")"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())";

    /**
     * The constant UPDATE_GLOBAL_TRANSACTION_MYSQL.
     */
    public static final String UPDATE_GLOBAL_TRANSACTION_MYSQL = "update " + GLOBAL_TABLE_PLACEHOLD
            + "   set " + SETS_PLACEHOLD
            + "       " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED + " = now()"
            + " where " + ServerTableColumnsName.GLOBAL_TABLE_XID + " = ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_BY_STATUS.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_BY_STATUS_MYSQL = "select " + ALL_GLOBAL_COLUMNS
            + "  from " + GLOBAL_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.GLOBAL_TABLE_STATUS + " in (" + PRAMETER_PLACEHOLD + ")"
            + " order by " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED
            + " limit ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_MYSQL.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_MYSQL = "select " + ALL_GLOBAL_COLUMNS
            + "  from " + GLOBAL_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.GLOBAL_TABLE_STATUS + " in (0, 2, 3, 4, 5, 6, 7, 8, 10 ,12, 14)"
            + " order by " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED
            + " limit ?";

    /**
     * The constant INSERT_BRANCH_TRANSACTION_MYSQL.
     */
    public static final String INSERT_BRANCH_TRANSACTION_MYSQL = "insert into " + BRANCH_TABLE_PLACEHOLD
            + "(" + ALL_BRANCH_COLUMNS + ")"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(6), now(6))";

    /**
     * The constant UPDATE_BRANCH_TRANSACTION_MYSQL.
     */
    public static final String UPDATE_BRANCH_TRANSACTION_MYSQL = "update " + BRANCH_TABLE_PLACEHOLD
            + "   set " + SETS_PLACEHOLD
            + "       " + ServerTableColumnsName.BRANCH_TABLE_GMT_MODIFIED + " = now(6)"
            + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ?"
            + "   and " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " = ?";

    public static final String UPDATE_BRANCH_STATUS_APPLICATION_DATA_MYSQL =  "update " + BRANCH_TABLE_PLACEHOLD
            + "   set " + ServerTableColumnsName.BRANCH_TABLE_STATUS + " = ?,"
            + "       " + ServerTableColumnsName.BRANCH_TABLE_APPLICATION_DATA + " = ?,"
            + "       " + ServerTableColumnsName.BRANCH_TABLE_GMT_MODIFIED + " = now(6)"
            + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ?"
            + "   and " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " = ?";

    @Override
    public String getInsertGlobalTransactionSQL(String globalTable) {
        return INSERT_GLOBAL_TRANSACTION_MYSQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    @Override
    public String getUpdateGlobalTransactionSQL(String globalTable, String setsPlaceHolder) {
        return UPDATE_GLOBAL_TRANSACTION_MYSQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable)
            .replace(SETS_PLACEHOLD, setsPlaceHolder);
    }

    @Override
    public String getQueryGlobalTransactionSQLByStatus(String globalTable, String paramsPlaceHolder) {
        return QUERY_GLOBAL_TRANSACTION_BY_STATUS_MYSQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable)
            .replace(PRAMETER_PLACEHOLD, paramsPlaceHolder);
    }

    @Override
    public String getQueryGlobalTransactionForRecoverySQL(String globalTable) {
        return QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_MYSQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    @Override
    public String getInsertBranchTransactionSQL(String branchTable) {
        return INSERT_BRANCH_TRANSACTION_MYSQL.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }

    @Override
    public String getUpdateBranchTransactionSQL(String branchTable, String setsPlaceHolder) {
        return UPDATE_BRANCH_TRANSACTION_MYSQL.replace(BRANCH_TABLE_PLACEHOLD, branchTable)
            .replace(SETS_PLACEHOLD, setsPlaceHolder);
    }

    @Override
    public String getUpdateBranchTransactionStatusAppDataSQL(String branchTable) {
        return UPDATE_BRANCH_STATUS_APPLICATION_DATA_MYSQL.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }
}
