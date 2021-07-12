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
package io.seata.core.store.db.sql.distribute.lock;

import io.seata.core.constants.ServerTableColumnsName;

/**
 * @author chd
 */
public class BaseDistributeLockSql implements DistributeLockSql {
    protected static final String DISTRIBUTE_LOCK_TABLE_PLACE_HOLD = " #distribute_lock_table# ";

    protected static final String ALL_COLUMNS = ServerTableColumnsName.DISTRIBUTE_LOCK_KEY + "," +
            ServerTableColumnsName.DISTRIBUTE_LOCK_VALUE + "," + ServerTableColumnsName.DISTRIBUTE_LOCK_EXPIRE;

    protected static final String SELECT_FOR_UPDATE_SQL = "SELECT " + ALL_COLUMNS + " FROM " + DISTRIBUTE_LOCK_TABLE_PLACE_HOLD
            + " WHERE " + ServerTableColumnsName.DISTRIBUTE_LOCK_KEY + " = ? FOR UPDATE";

    protected static final String INSERT_DISTRIBUTE_LOCK_SQL = "INSERT INTO " + DISTRIBUTE_LOCK_TABLE_PLACE_HOLD + "("
            + ALL_COLUMNS + ") VALUE (?, ?, ?)";

    protected static final String UPDATE_DISTRIBUTE_LOCK_SQL = "UPDATE " + DISTRIBUTE_LOCK_TABLE_PLACE_HOLD + " SET "
            + ServerTableColumnsName.DISTRIBUTE_LOCK_VALUE + "=?, " + ServerTableColumnsName.DISTRIBUTE_LOCK_EXPIRE + "=?"
            + " WHERE " + ServerTableColumnsName.DISTRIBUTE_LOCK_KEY + "=?";

    @Override
    public String getSelectDistributeForUpdateSql(String distributeLockTable) {
        return SELECT_FOR_UPDATE_SQL.replace(DISTRIBUTE_LOCK_TABLE_PLACE_HOLD, distributeLockTable);
    }

    @Override
    public String getInsertSql(String distributeLockTable) {
        return INSERT_DISTRIBUTE_LOCK_SQL.replace(DISTRIBUTE_LOCK_TABLE_PLACE_HOLD, distributeLockTable);
    }

    @Override
    public String getUpdateSql(String distributeLockTable) {
        return UPDATE_DISTRIBUTE_LOCK_SQL.replace(DISTRIBUTE_LOCK_TABLE_PLACE_HOLD, distributeLockTable);
    }
}
