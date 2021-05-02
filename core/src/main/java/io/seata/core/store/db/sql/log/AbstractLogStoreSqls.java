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

import io.seata.core.constants.ServerTableColumnsName;


/**
 * The type Abstract log store sqls
 * @author will
 */
public abstract class AbstractLogStoreSqls implements LogStoreSqls {

    /**
     * The constant GLOBAL_TABLE_PLACEHOLD.
     */
    public static final String GLOBAL_TABLE_PLACEHOLD = " #global_table# ";

    /**
     * The constant BRANCH_TABLE_PLACEHOLD.
     */
    public static final String BRANCH_TABLE_PLACEHOLD = " #branch_table# ";

    /**
     * The constant PRAMETER_PLACEHOLD.
     * format: ?, ?, ?
     */
    public static final String PRAMETER_PLACEHOLD = " #PRAMETER_PLACEHOLD# ";

    /**
     * The constant ALL_GLOBAL_COLUMNS.
     * xid, transaction_id, status, application_id, transaction_service_group, transaction_name, timeout, begin_time, application_data, gmt_create, gmt_modified
     */
    public static final String ALL_GLOBAL_COLUMNS
            = ServerTableColumnsName.GLOBAL_TABLE_XID + ", " + ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID + ", "
            + ServerTableColumnsName.GLOBAL_TABLE_STATUS + ", " + ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_ID + ", "
            + ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_SERVICE_GROUP + ", " + ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_NAME + ", "
            + ServerTableColumnsName.GLOBAL_TABLE_TIMEOUT + ", " + ServerTableColumnsName.GLOBAL_TABLE_BEGIN_TIME + ", "
            + ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_DATA + ", "
            + ServerTableColumnsName.GLOBAL_TABLE_GMT_CREATE + ", " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED;

    /**
     * The constant ALL_BRANCH_COLUMNS.
     * xid, transaction_id, branch_id, resource_group_id, resource_id, lock_key, branch_type, status, client_id, application_data, gmt_create, gmt_modified
     */
    protected static final String ALL_BRANCH_COLUMNS
            = ServerTableColumnsName.BRANCH_TABLE_XID + ", " + ServerTableColumnsName.BRANCH_TABLE_TRANSACTION_ID + ", "
            + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + ", " + ServerTableColumnsName.BRANCH_TABLE_RESOURCE_GROUP_ID + ", "
            + ServerTableColumnsName.BRANCH_TABLE_RESOURCE_ID + ", "
            + ServerTableColumnsName.BRANCH_TABLE_BRANCH_TYPE + ", " + ServerTableColumnsName.BRANCH_TABLE_STATUS + ", "
            + ServerTableColumnsName.BRANCH_TABLE_CLIENT_ID + ", " + ServerTableColumnsName.BRANCH_TABLE_APPLICATION_DATA + ", "
            + ServerTableColumnsName.BRANCH_TABLE_GMT_CREATE + ", " + ServerTableColumnsName.BRANCH_TABLE_GMT_MODIFIED;

    /**
     * The constant DELETE_GLOBAL_TRANSACTION.
     */
    public static final String DELETE_GLOBAL_TRANSACTION = "delete from " + GLOBAL_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.GLOBAL_TABLE_XID + " = ?";
    
    /**
     * The constant BATCH_DELETE_GLOBAL_TRANSACTION.
     */
    public static final String BATCH_DELETE_GLOBAL_TRANSACTION = "delete from " + GLOBAL_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.GLOBAL_TABLE_XID + " in (" + PRAMETER_PLACEHOLD + ")";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION.
     */
    public static final String QUERY_GLOBAL_TRANSACTION = "select " + ALL_GLOBAL_COLUMNS
            + "  from " + GLOBAL_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.GLOBAL_TABLE_XID + " = ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_ID.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_BY_ID = "select " + ALL_GLOBAL_COLUMNS
            + "  from " + GLOBAL_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID + " = ?";

    /**
     * The constant DELETE_BRANCH_TRANSACTION_BY_BRANCH_ID.
     */
    public static final String DELETE_BRANCH_TRANSACTION_BY_BRANCH_ID = "delete from " + BRANCH_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ?"
            + "   and " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " = ?";
    
    /**
     * The constant BATCH_BRANCH_TRANSACTION_BY_BRANCH_ID.
     */
    public static final String BATCH_DELETE_BRANCH_TRANSACTION_BY_BRANCH_ID = "delete from " + BRANCH_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " in (" + PRAMETER_PLACEHOLD + ")";

    /**
     * The constant DELETE_BRANCH_TRANSACTION_BY_XID.
     */
    public static final String DELETE_BRANCH_TRANSACTION_BY_XID = "delete from " + BRANCH_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ?";


    /**
     * The constant QUERY_BRANCH_TRANSACTION.
     */
    public static final String QUERY_BRANCH_TRANSACTION = "select " + ALL_BRANCH_COLUMNS
            + "  from " + BRANCH_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ?"
            + " order by " + ServerTableColumnsName.BRANCH_TABLE_GMT_CREATE + " asc";

    /**
     * The constant QUERY_BRANCH_TRANSACTION_XIDS.
     */
    public static final String QUERY_BRANCH_TRANSACTION_XIDS = "select " + ALL_BRANCH_COLUMNS
            + "  from " + BRANCH_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " in (" + PRAMETER_PLACEHOLD + ")"
            + " order by " + ServerTableColumnsName.BRANCH_TABLE_GMT_CREATE + " asc";

    /**
     * The constant CHECK_MAX_TRANS_ID.
     */
    public static final String QUERY_MAX_TRANS_ID = "select max(" + ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID + ")"
            + "  from " + GLOBAL_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID + " < ?"
            + "   and " + ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID + " > ?";

    /**
     * The constant CHECK_MAX_BTANCH_ID.
     */
    public static final String QUERY_MAX_BTANCH_ID = "select max(" + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + ")"
            + "  from " + BRANCH_TABLE_PLACEHOLD
            + " where " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " < ?"
            + "   and " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " > ?";

    @Override
    public abstract String getInsertGlobalTransactionSQL(String globalTable);

    @Override
    public abstract String getUpdateGlobalTransactionStatusSQL(String globalTable);

    @Override
    public String getDeleteGlobalTransactionSQL(String globalTable) {
        return DELETE_GLOBAL_TRANSACTION.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }
    
    @Override
    public String getBatchDeleteGlobalTransactionSQL(String globalTable, String paramsPlaceHolder) {
        return BATCH_DELETE_GLOBAL_TRANSACTION.replace(GLOBAL_TABLE_PLACEHOLD, globalTable)
                .replace(PRAMETER_PLACEHOLD, paramsPlaceHolder);
    }


    @Override
    public String getQueryGlobalTransactionSQL(String globalTable) {
        return QUERY_GLOBAL_TRANSACTION.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    @Override
    public String getQueryGlobalTransactionSQLByTransactionId(String globalTable) {
        return QUERY_GLOBAL_TRANSACTION_BY_ID.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    @Override
    public abstract String getQueryGlobalTransactionSQLByStatus(String globalTable, String paramsPlaceHolder);

    @Override
    public abstract String getQueryGlobalTransactionForRecoverySQL(String globalTable);

    @Override
    public abstract String getQueryRemovedGlobalTransactionSQL(String globalTable);

    @Override
    public abstract String getInsertBranchTransactionSQL(String branchTable);

    @Override
    public abstract String getUpdateBranchTransactionStatusSQL(String branchTable);

    @Override
    public String getDeleteBranchTransactionByBranchIdSQL(String branchTable) {
        return DELETE_BRANCH_TRANSACTION_BY_BRANCH_ID.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }

    @Override
    public String getBatchDeleteBranchTransactionByBranchIdSQL(String branchTable, String paramsPlaceHolder) {
        return BATCH_DELETE_BRANCH_TRANSACTION_BY_BRANCH_ID.replace(BRANCH_TABLE_PLACEHOLD, branchTable)
                .replace(PRAMETER_PLACEHOLD, paramsPlaceHolder);
    }
    
    @Override
    public String getDeleteBranchTransactionByXId(String branchTable) {
        return DELETE_BRANCH_TRANSACTION_BY_XID.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }

    @Override
    public String getQueryBranchTransaction(String branchTable) {
        return QUERY_BRANCH_TRANSACTION.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }

    @Override
    public String getQueryBranchTransaction(String branchTable, String paramsPlaceHolder) {
        return QUERY_BRANCH_TRANSACTION_XIDS.replace(BRANCH_TABLE_PLACEHOLD, branchTable)
                .replace(PRAMETER_PLACEHOLD, paramsPlaceHolder);
    }


    @Override
    public String getQueryGlobalMax(String globalTable) {
        return QUERY_MAX_TRANS_ID.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    @Override
    public String getQueryBranchMax(String branchTable) {
        return QUERY_MAX_BTANCH_ID.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }
}
