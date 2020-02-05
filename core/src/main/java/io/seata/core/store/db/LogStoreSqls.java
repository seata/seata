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
package io.seata.core.store.db;

import io.seata.common.exception.NotSupportYetException;
import io.seata.core.constants.DBType;
import io.seata.core.constants.ServerTableColumnsName;

/**
 * database log store SQLs
 *
 * @author zhangsen
 */
public class LogStoreSqls {

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
        + ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_DATA + ", " + ServerTableColumnsName.GLOBAL_TABLE_GMT_CREATE + ", "
        + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED + ", " + ServerTableColumnsName.GLOBAL_TABLE_VERSION;

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
     * The constant INSERT_GLOBAL_TRANSACTION_MYSQL.
     */
    public static final String INSERT_GLOBAL_TRANSACTION_MYSQL = "insert into " + GLOBAL_TABLE_PLACEHOLD + "("
        + ALL_GLOBAL_COLUMNS + ")" +
        "values(?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), ?) ";

    /**
     * The constant INSERT_GLOBAL_TRANSACTION_ORACLE.
     */
    public static final String INSERT_GLOBAL_TRANSACTION_ORACLE = "insert into " + GLOBAL_TABLE_PLACEHOLD + "("
        + ALL_GLOBAL_COLUMNS + ")" +
        "values(?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate, sysdate, ?) ";

    /**
     * The constant UPDATE_GLOBAL_TRANSACTION_STATUS_MYSQL.
     */
    public static final String UPDATE_GLOBAL_TRANSACTION_STATUS_MYSQL = "update " + GLOBAL_TABLE_PLACEHOLD
        + " set " + ServerTableColumnsName.GLOBAL_TABLE_STATUS + " = ?, " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED + " = now(), " + ServerTableColumnsName.GLOBAL_TABLE_VERSION + " = " + ServerTableColumnsName.GLOBAL_TABLE_VERSION + " + 1 where " + ServerTableColumnsName.GLOBAL_TABLE_XID + " = ? and " + ServerTableColumnsName.GLOBAL_TABLE_VERSION + " = ?";

    /**
     * The constant UPDATE_GLOBAL_TRANSACTION_STATUS_ORACLE.
     */
    public static final String UPDATE_GLOBAL_TRANSACTION_STATUS_ORACLE = "update " + GLOBAL_TABLE_PLACEHOLD
        + " set " + ServerTableColumnsName.GLOBAL_TABLE_STATUS + " = ?, " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED + " = sysdate, " + ServerTableColumnsName.GLOBAL_TABLE_VERSION + " = " + ServerTableColumnsName.GLOBAL_TABLE_VERSION + " + 1 where " + ServerTableColumnsName.GLOBAL_TABLE_XID + " = ? and " + ServerTableColumnsName.GLOBAL_TABLE_VERSION + " = ?";

    /**
     * The constant DELETE_GLOBAL_TRANSACTION.
     */
    public static final String DELETE_GLOBAL_TRANSACTION = "delete from " + GLOBAL_TABLE_PLACEHOLD + " where " + ServerTableColumnsName.GLOBAL_TABLE_XID + " = ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION.
     */
    public static final String QUERY_GLOBAL_TRANSACTION = "select " + ALL_GLOBAL_COLUMNS + " from "
        + GLOBAL_TABLE_PLACEHOLD + " where " + ServerTableColumnsName.GLOBAL_TABLE_XID + " = ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_ID.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_BY_ID = "select " + ALL_GLOBAL_COLUMNS + " from "
        + GLOBAL_TABLE_PLACEHOLD + " where " + ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID + " = ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_BY_STATUS.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_BY_STATUS_MYSQL =
            "select " + ALL_GLOBAL_COLUMNS + " from " + GLOBAL_TABLE_PLACEHOLD
                    + " where " + ServerTableColumnsName.GLOBAL_TABLE_STATUS + " in (" + PRAMETER_PLACEHOLD + ")"
                    + " order by " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED + " limit ?";

    public static final String QUERY_GLOBAL_TRANSACTION_BY_STATUS_ORACLE =
            "select t.* from ("
                    + "  select " + ALL_GLOBAL_COLUMNS + " from " + GLOBAL_TABLE_PLACEHOLD
                    + "  where " + ServerTableColumnsName.GLOBAL_TABLE_STATUS + " in (" + PRAMETER_PLACEHOLD + ")"
                    + "  order by " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED + ") t"
                    + " where ROWNUM <= ?";
    /**
     * The constant QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_MYSQL.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_MYSQL = "select " + ALL_GLOBAL_COLUMNS + " from "
        + GLOBAL_TABLE_PLACEHOLD + " where " + ServerTableColumnsName.GLOBAL_TABLE_STATUS + " in (" +
        "0, 2, 3, 4, 5, 6, 7, 8, 10 ,12, 14) order by " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED + " limit ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_ORACLE.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_ORACLE = "select A.* from ( select "
        + ALL_GLOBAL_COLUMNS + " from " + GLOBAL_TABLE_PLACEHOLD + " where " + ServerTableColumnsName.GLOBAL_TABLE_STATUS + " in (" +
        "0, 2, 3, 4, 5, 6, 7, 8, 10 ,12, 14) order by " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED + " ) A where ROWNUM <= ?";

    /**
     * The constant INSERT_BRANCH_TRANSACTION_MYSQL.
     */
    public static final String INSERT_BRANCH_TRANSACTION_MYSQL = "insert into " + BRANCH_TABLE_PLACEHOLD + "("
        + ALL_BRANCH_COLUMNS + ")" +
        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(6), now(6))";

    /**
     * The constant INSERT_BRANCH_TRANSACTION_ORACLE.
     */
    public static final String INSERT_BRANCH_TRANSACTION_ORACLE = "insert into " + BRANCH_TABLE_PLACEHOLD + "("
        + ALL_BRANCH_COLUMNS + ")" +
        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, systimestamp, systimestamp)";

    /**
     * The constant UPDATE_BRANCH_TRANSACTION_STATUS_MYSQL.
     */
    public static final String UPDATE_BRANCH_TRANSACTION_STATUS_MYSQL = "update " + BRANCH_TABLE_PLACEHOLD
        + " set " + ServerTableColumnsName.BRANCH_TABLE_STATUS + " = ?, " + ServerTableColumnsName.BRANCH_TABLE_GMT_MODIFIED + " = now(6) where "
        + ServerTableColumnsName.BRANCH_TABLE_XID + " = ? and " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " = ?";

    /**
     * The constant UPDATE_BRANCH_TRANSACTION_STATUS_ORACLE.
     */
    public static final String UPDATE_BRANCH_TRANSACTION_STATUS_ORACLE = "update " + BRANCH_TABLE_PLACEHOLD
        + " set " + ServerTableColumnsName.BRANCH_TABLE_STATUS + " = ?, " + ServerTableColumnsName.BRANCH_TABLE_GMT_MODIFIED
        + " = systimestamp where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ? and " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID
        + " = ?";

    /**
     * The constant DELETE_BRANCH_TRANSACTION_BY_BRANCH_ID.
     */
    public static final String DELETE_BRANCH_TRANSACTION_BY_BRANCH_ID = "delete from " + BRANCH_TABLE_PLACEHOLD
        + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ? and " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID
        + " = ?";

    /**
     * The constant DELETE_BRANCH_TRANSACTION_BY_XID.
     */
    public static final String DELETE_BRANCH_TRANSACTION_BY_XID = "delete from " + BRANCH_TABLE_PLACEHOLD
        + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ?";

    /**
     * The constant QUERY_BRANCH_TRANSACTION.
     */
    public static final String QUERY_BRANCH_TRANSACTION = "select " + ALL_BRANCH_COLUMNS + " from "
        + BRANCH_TABLE_PLACEHOLD + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ? order by "
        + ServerTableColumnsName.BRANCH_TABLE_GMT_CREATE + " asc";

    /**
     * The constant QUERY_BRANCH_TRANSACTION_XIDS.
     */
    public static final String QUERY_BRANCH_TRANSACTION_XIDS = "select " + ALL_BRANCH_COLUMNS + " from "
        + BRANCH_TABLE_PLACEHOLD + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " in (" + PRAMETER_PLACEHOLD + ") order by "
        + ServerTableColumnsName.BRANCH_TABLE_GMT_CREATE + " asc";

    /**
     * The constant CHECK_MAX_TRANS_ID.
     */
    public static final String QUERY_MAX_TRANS_ID = "select max(" + ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID
        + ") from " + GLOBAL_TABLE_PLACEHOLD + " where " + ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID
        + " < ? and " + ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID + " > ?";

    /**
     * The constant CHECK_MAX_BTANCH_ID.
     */
    public static final String QUERY_MAX_BTANCH_ID = "select max(" + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID
        + ") from " + BRANCH_TABLE_PLACEHOLD + " where " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " < ? and "
        + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " > ?";

    /**
     * Get insert global transaction sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    public static String getInsertGlobalTransactionSQL(String globalTable, String dbType) {
        if (DBType.MYSQL.name().equalsIgnoreCase(dbType)
            || DBType.OCEANBASE.name().equalsIgnoreCase(dbType)
            || DBType.H2.name().equalsIgnoreCase(dbType)) {
            return INSERT_GLOBAL_TRANSACTION_MYSQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        } else if (DBType.ORACLE.name().equalsIgnoreCase(dbType)) {
            return INSERT_GLOBAL_TRANSACTION_ORACLE.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        } else {
            throw new NotSupportYetException("unknown dbType:" + dbType);
        }
    }

    /**
     * Get update global transaction status sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    public static String getUpdateGlobalTransactionStatusSQL(String globalTable, String dbType) {
        if (DBType.MYSQL.name().equalsIgnoreCase(dbType)
            || DBType.OCEANBASE.name().equalsIgnoreCase(dbType)
            || DBType.H2.name().equalsIgnoreCase(dbType)) {
            return UPDATE_GLOBAL_TRANSACTION_STATUS_MYSQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        } else if (DBType.ORACLE.name().equalsIgnoreCase(dbType)) {
            return UPDATE_GLOBAL_TRANSACTION_STATUS_ORACLE.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        } else {
            throw new NotSupportYetException("unknown dbType:" + dbType);
        }
    }

    /**
     * Get delete global transaction sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    public static String getDeleteGlobalTransactionSQL(String globalTable, String dbType) {
        return DELETE_GLOBAL_TRANSACTION.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    /**
     * Get query global transaction sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    public static String getQueryGlobalTransactionSQL(String globalTable, String dbType) {
        return QUERY_GLOBAL_TRANSACTION.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    /**
     * Get query global transaction sql by transaction id string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    public static String getQueryGlobalTransactionSQLByTransactionId(String globalTable, String dbType) {
        return QUERY_GLOBAL_TRANSACTION_BY_ID.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    /**
     * Get query global transaction sql by status string.
     *
     * @param globalTable       the global table
     * @param dbType            the db type
     * @param paramsPlaceHolder the params place holder
     * @return the string
     */
    public static String getQueryGlobalTransactionSQLByStatus(String globalTable, String dbType,
                                                              String paramsPlaceHolder) {
        if (DBType.MYSQL.name().equalsIgnoreCase(dbType)
            || DBType.OCEANBASE.name().equalsIgnoreCase(dbType)
            || DBType.H2.name().equalsIgnoreCase(dbType)) {
            return QUERY_GLOBAL_TRANSACTION_BY_STATUS_MYSQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable).replace(
                    PRAMETER_PLACEHOLD, paramsPlaceHolder);
        } else if (DBType.ORACLE.name().equalsIgnoreCase(dbType)) {
            return QUERY_GLOBAL_TRANSACTION_BY_STATUS_ORACLE.replace(GLOBAL_TABLE_PLACEHOLD, globalTable).replace(
                    PRAMETER_PLACEHOLD, paramsPlaceHolder);
        } else {
            throw new IllegalArgumentException("unknown database type");
        }
    }

    /**
     * Get query global transaction for recovery sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    public static String getQueryGlobalTransactionForRecoverySQL(String globalTable, String dbType) {
        if (DBType.MYSQL.name().equalsIgnoreCase(dbType)
            || DBType.OCEANBASE.name().equalsIgnoreCase(dbType)
            || DBType.H2.name().equalsIgnoreCase(dbType)) {
            return QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_MYSQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        } else if (DBType.ORACLE.name().equalsIgnoreCase(dbType)) {
            return QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_ORACLE.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        } else {
            throw new NotSupportYetException("unknown dbType:" + dbType);
        }
    }

    /**
     * Get insert branch transaction sql string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the string
     */
    public static String getInsertBranchTransactionSQL(String branchTable, String dbType) {
        if (DBType.MYSQL.name().equalsIgnoreCase(dbType)
            || DBType.OCEANBASE.name().equalsIgnoreCase(dbType)
            || DBType.H2.name().equalsIgnoreCase(dbType)) {
            return INSERT_BRANCH_TRANSACTION_MYSQL.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
        } else if (DBType.ORACLE.name().equalsIgnoreCase(dbType)) {
            return INSERT_BRANCH_TRANSACTION_ORACLE.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
        } else {
            throw new NotSupportYetException("unknown dbType:" + dbType);
        }
    }

    /**
     * Get update branch transaction status sql string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the string
     */
    public static String getUpdateBranchTransactionStatusSQL(String branchTable, String dbType) {
        if (DBType.MYSQL.name().equalsIgnoreCase(dbType)
            || DBType.OCEANBASE.name().equalsIgnoreCase(dbType)
            || DBType.H2.name().equalsIgnoreCase(dbType)) {
            return UPDATE_BRANCH_TRANSACTION_STATUS_MYSQL.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
        } else if (DBType.ORACLE.name().equalsIgnoreCase(dbType)) {
            return UPDATE_BRANCH_TRANSACTION_STATUS_ORACLE.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
        } else {
            throw new NotSupportYetException("unknown dbType:" + dbType);
        }
    }

    /**
     * Get delete branch transaction by branch id sql string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the string
     */
    public static String getDeleteBranchTransactionByBranchIdSQL(String branchTable, String dbType) {
        return DELETE_BRANCH_TRANSACTION_BY_BRANCH_ID.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }

    /**
     * Get delete branch transaction by x id string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the string
     */
    public static String getDeleteBranchTransactionByXId(String branchTable, String dbType) {
        return DELETE_BRANCH_TRANSACTION_BY_XID.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }

    /**
     * Get query branch transaction string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the string
     */
    public static String getQueryBranchTransaction(String branchTable, String dbType) {
        return QUERY_BRANCH_TRANSACTION.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }

    /**
     * Get query branch transaction string.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @param paramsPlaceHolder the params place holder
     * @return the string
     */
    public static String getQueryBranchTransaction(String branchTable, String dbType,
                                                   String paramsPlaceHolder) {
        return QUERY_BRANCH_TRANSACTION_XIDS.replace(BRANCH_TABLE_PLACEHOLD, branchTable)
            .replace(PRAMETER_PLACEHOLD, paramsPlaceHolder);
    }

    /**
     * Gets query global max.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the query global max
     */
    public static String getQueryGlobalMax(String globalTable, String dbType) {
        return QUERY_MAX_TRANS_ID.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    /**
     * Gets query branch max.
     *
     * @param branchTable the branch table
     * @param dbType      the db type
     * @return the query branch max
     */
    public static String getQueryBranchMax(String branchTable, String dbType) {
        return QUERY_MAX_BTANCH_ID.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }
}
