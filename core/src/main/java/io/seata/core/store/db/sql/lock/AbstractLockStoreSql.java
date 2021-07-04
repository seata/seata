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
package io.seata.core.store.db.sql.lock;

import io.seata.common.exception.NotSupportYetException;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.ServerTableColumnsName;


/**
 * the database abstract lock store sql interface
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
public class AbstractLockStoreSql implements LockStoreSql {

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The constant LOCK_TABLE_PLACE_HOLD.
     */
    protected static final String LOCK_TABLE_PLACE_HOLD = " #lock_table# ";

    /**
     * The constant IN_PARAMS_PLACE_HOLD.
     */
    protected static final String IN_PARAMS_PLACE_HOLD = " #in_params# ";

    /**
     * The constant ALL_COLUMNS.
     * xid, transaction_id, branch_id, resource_id, table_name, pk, row_key, gmt_create, gmt_modified
     */
    protected static final String ALL_COLUMNS =
        ServerTableColumnsName.LOCK_TABLE_XID + ", " + ServerTableColumnsName.LOCK_TABLE_TRANSACTION_ID + ", "
            + ServerTableColumnsName.LOCK_TABLE_BRANCH_ID + ", " + ServerTableColumnsName.LOCK_TABLE_RESOURCE_ID + ", "
            + ServerTableColumnsName.LOCK_TABLE_TABLE_NAME + ", " + ServerTableColumnsName.LOCK_TABLE_PK + ", "
            + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + ", " + ServerTableColumnsName.LOCK_TABLE_GMT_CREATE + ", "
            + ServerTableColumnsName.LOCK_TABLE_GMT_MODIFIED + "," + ServerTableColumnsName.LOCK_TABLE_STATUS;

    /**
     * The constant DELETE_LOCK_SQL.
     */
    private static final String DELETE_LOCK_SQL = "delete from " + LOCK_TABLE_PLACE_HOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + " = ? and " + ServerTableColumnsName.LOCK_TABLE_XID + " = ?";

    /**
     * The constant BATCH_DELETE_LOCK_SQL.
     */
    private static final String BATCH_DELETE_LOCK_SQL = "delete from " + LOCK_TABLE_PLACE_HOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_XID + " = ? and " + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + " in (" + IN_PARAMS_PLACE_HOLD + ") ";

    /**
     * The constant BATCH_DELETE_LOCK_BY_BRANCH_SQL.
     */
    private static final String BATCH_DELETE_LOCK_BY_BRANCH_SQL = "delete from " + LOCK_TABLE_PLACE_HOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_XID + " = ? and " + ServerTableColumnsName.LOCK_TABLE_BRANCH_ID + " = ? ";

    /**
     * The constant BATCH_UPDATE_STATUS_LOCK_BY_GLOBAL_SQL.
     */
    private static final String BATCH_UPDATE_STATUS_LOCK_BY_GLOBAL_SQL = "update " + LOCK_TABLE_PLACE_HOLD + " set "
        + ServerTableColumnsName.LOCK_TABLE_STATUS + " = ? where " + ServerTableColumnsName.LOCK_TABLE_XID + " = ? ";

    /**
     * The constant BATCH_DELETE_LOCK_BY_BRANCHS_SQL.
     */
    private static final String BATCH_DELETE_LOCK_BY_BRANCHS_SQL = "delete from " + LOCK_TABLE_PLACE_HOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_XID + " = ? and " + ServerTableColumnsName.LOCK_TABLE_BRANCH_ID + " in (" + IN_PARAMS_PLACE_HOLD + ") ";


    /**
     * The constant QUERY_LOCK_SQL.
     */
    private static final String QUERY_LOCK_SQL = "select " + ALL_COLUMNS + " from " + LOCK_TABLE_PLACE_HOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + " = ? ";

    /**
     * The constant CHECK_LOCK_SQL.
     */
    private static final String CHECK_LOCK_SQL = "select " + ALL_COLUMNS + " from " + LOCK_TABLE_PLACE_HOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + " in (" + IN_PARAMS_PLACE_HOLD + ")"
        + " order by status desc ";

    @Override
    public String getInsertLockSQL(String lockTable) {
        throw new NotSupportYetException("unknown dbType:" + CONFIG.getConfig(ConfigurationKeys.STORE_DB_TYPE));
    }

    @Override
    public String getDeleteLockSql(String lockTable) {
        return DELETE_LOCK_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

    @Override
    public String getBatchDeleteLockSql(String lockTable, String paramPlaceHold) {
        return BATCH_DELETE_LOCK_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable).replace(IN_PARAMS_PLACE_HOLD,
            paramPlaceHold);
    }

    @Override
    public String getBatchDeleteLockSqlByBranch(String lockTable) {
        return BATCH_DELETE_LOCK_BY_BRANCH_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

    @Override
    public String getBatchDeleteLockSqlByBranchs(String lockTable, String paramPlaceHold) {
        return BATCH_DELETE_LOCK_BY_BRANCHS_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable).replace(IN_PARAMS_PLACE_HOLD,
            paramPlaceHold);
    }

    @Override
    public String getQueryLockSql(String lockTable) {
        return QUERY_LOCK_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

    @Override
    public String getCheckLockableSql(String lockTable, String paramPlaceHold) {
        return CHECK_LOCK_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable).replace(IN_PARAMS_PLACE_HOLD, paramPlaceHold);
    }

    @Override
    public String getBatchUpdateStatusLockByGlobalSql(String lockTable) {
        return BATCH_UPDATE_STATUS_LOCK_BY_GLOBAL_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

}
