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
     * The constant UPDATE_GLOBAL_TRANSACTION_POSTGRESQL.
     */
    public static final String UPDATE_GLOBAL_TRANSACTION_POSTGRESQL = "update " + GLOBAL_TABLE_PLACEHOLD
            + " set " + SETS_PLACEHOLD + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED + " = now()"
            + " where " + ServerTableColumnsName.GLOBAL_TABLE_XID + " = ?";

    /**
     * This constant QUERY_GLOBAL_TRANSACTION_BY_STATUS_POSTGRESQL.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_POSTGRESQL = "select " + ALL_GLOBAL_COLUMNS
            + " from " + GLOBAL_TABLE_PLACEHOLD
            + WHERE_PLACEHOLD
            + SORT_PLACEHOLD
            + " limit ?";

    /**
     * The constant INSERT_BRANCH_TRANSACTION_POSTGRESQL.
     */
    public static final String INSERT_BRANCH_TRANSACTION_POSTGRESQL = "insert into " + BRANCH_TABLE_PLACEHOLD
            + "(" + ALL_BRANCH_COLUMNS + ")"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())";

    /**
     * The constant UPDATE_BRANCH_TRANSACTION_POSTGRESQL.
     */
    public static final String UPDATE_BRANCH_TRANSACTION_POSTGRESQL = "update " + BRANCH_TABLE_PLACEHOLD
            + " set " + SETS_PLACEHOLD + ServerTableColumnsName.BRANCH_TABLE_GMT_MODIFIED + " = now()"
            + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ?"
            + " and " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " = ?";

    @Override
    public String getInsertGlobalTransactionSQL(String globalTable) {
        return INSERT_GLOBAL_TRANSACTION_POSTGRESQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    @Override
    public String getUpdateGlobalTransactionSQL(String globalTable, String setsPlaceHolder) {
        return UPDATE_GLOBAL_TRANSACTION_POSTGRESQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable)
            .replace(SETS_PLACEHOLD, setsPlaceHolder);
    }

    @Override
    public String getQueryGlobalTransactionSQL(String globalTable, String wherePlaceHolder, String sortPlaceHolder) {
        return QUERY_GLOBAL_TRANSACTION_POSTGRESQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable)
                .replace(WHERE_PLACEHOLD, wherePlaceHolder).replace(SORT_PLACEHOLD, sortPlaceHolder);
    }

    @Override
    public String getInsertBranchTransactionSQL(String branchTable) {
        return INSERT_BRANCH_TRANSACTION_POSTGRESQL.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }

    @Override
    public String getUpdateBranchTransactionSQL(String branchTable, String setsPlaceHolder) {
        return UPDATE_BRANCH_TRANSACTION_POSTGRESQL.replace(BRANCH_TABLE_PLACEHOLD, branchTable)
            .replace(SETS_PLACEHOLD, setsPlaceHolder);
    }
}
