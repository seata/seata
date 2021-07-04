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
 * Database log store postgresql sql
 * @author will
 */
@LoadLevel(name = "postgresql")
public class PostgresqlLogStoreSqls extends AbstractLogStoreSqls {

    /**
     * The constant INSERT_GLOBAL_TRANSACTION_POSTGRESQL.
     */
    public static final String INSERT_GLOBAL_TRANSACTION_POSTGRESQL = "insert into " + GLOBAL_TABLE_PLACEHOLD
        + "(" + ALL_GLOBAL_COLUMNS + ")"
        + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())";

    /**
     * The constant UPDATE_GLOBAL_TRANSACTION_STATUS_POSTGRESQL.
     */
    public static final String UPDATE_GLOBAL_TRANSACTION_STATUS_POSTGRESQL = "update " + GLOBAL_TABLE_PLACEHOLD
        + "   set " + ServerTableColumnsName.GLOBAL_TABLE_STATUS + " = ?,"
        + "       " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED + " = now()"
        + " where " + ServerTableColumnsName.GLOBAL_TABLE_XID + " = ?";

    /**
     * This constant QUERY_GLOBAL_TRANSACTION_BY_STATUS_POSTGRESQL.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_BY_STATUS_POSTGRESQL = "select " + ALL_GLOBAL_COLUMNS
        + "  from " + GLOBAL_TABLE_PLACEHOLD
        + " where " + ServerTableColumnsName.GLOBAL_TABLE_STATUS + " in (" + PRAMETER_PLACEHOLD + ")"
        + " order by " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED
        + " limit ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_POSTGRESQL.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_POSTGRESQL = "select " + ALL_GLOBAL_COLUMNS
        + "  from " + GLOBAL_TABLE_PLACEHOLD
        + " where " + ServerTableColumnsName.GLOBAL_TABLE_STATUS + " in (0, 2, 3, 4, 5, 6, 7, 8, 10 ,12, 14)"
        + " order by " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED
        + " limit ?";

    /**
     * The constant INSERT_BRANCH_TRANSACTION_POSTGRESQL.
     */
    public static final String INSERT_BRANCH_TRANSACTION_POSTGRESQL = "insert into " + BRANCH_TABLE_PLACEHOLD
        + "(" + ALL_BRANCH_COLUMNS + ")"
        + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())";

    /**
     * The constant UPDATE_BRANCH_TRANSACTION_STATUS_POSTGRESQL.
     */
    public static final String UPDATE_BRANCH_TRANSACTION_STATUS_POSTGRESQL = "update " + BRANCH_TABLE_PLACEHOLD
        + "   set " + ServerTableColumnsName.BRANCH_TABLE_STATUS + " = ?,"
        + "       " + ServerTableColumnsName.BRANCH_TABLE_GMT_MODIFIED + " = now()"
        + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ?"
        + "   and " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " = ?";

    /**
     * The constant UPDATE_BRANCH_STATUS_APPLICATION_DATA_POSTGRESQL.
     */
    public static final String UPDATE_BRANCH_STATUS_APPLICATION_DATA_POSTGRESQL = "update " + BRANCH_TABLE_PLACEHOLD
        + "   set " + ServerTableColumnsName.BRANCH_TABLE_STATUS + " = ?,"
        + "       " + ServerTableColumnsName.BRANCH_TABLE_APPLICATION_DATA + " = ?,"
        + "       " + ServerTableColumnsName.BRANCH_TABLE_GMT_MODIFIED + " = now()"
        + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ?"
        + "   and " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " = ?";

    @Override
    public String getInsertGlobalTransactionSQL(String globalTable) {
        return INSERT_GLOBAL_TRANSACTION_POSTGRESQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    @Override
    public String getUpdateGlobalTransactionStatusSQL(String globalTable) {
        return UPDATE_GLOBAL_TRANSACTION_STATUS_POSTGRESQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    @Override
    public String getQueryGlobalTransactionSQLByStatus(String globalTable, String paramsPlaceHolder) {
        return QUERY_GLOBAL_TRANSACTION_BY_STATUS_POSTGRESQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable)
            .replace(PRAMETER_PLACEHOLD, paramsPlaceHolder);
    }

    @Override
    public String getQueryGlobalTransactionForRecoverySQL(String globalTable) {
        return QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_POSTGRESQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    @Override
    public String getInsertBranchTransactionSQL(String branchTable) {
        return INSERT_BRANCH_TRANSACTION_POSTGRESQL.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }

    @Override
    public String getUpdateBranchTransactionStatusSQL(String branchTable) {
        return UPDATE_BRANCH_TRANSACTION_STATUS_POSTGRESQL.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }

    @Override
    public String getUpdateBranchTransactionStatusAppDataSQL(String branchTable) {
        return UPDATE_BRANCH_STATUS_APPLICATION_DATA_POSTGRESQL.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }
}
