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

/**
 * the database lock store sql interface
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
public interface LockStoreSql {

    /**
     * Get insert lock sql string.
     *
     * @param lockTable the lock table
     * @return the string
     */
    String getInsertLockSQL(String lockTable);

    /**
     * Get delete lock sql string.
     *
     * @param lockTable the lock table
     * @return the string
     */
    String getDeleteLockSql(String lockTable);

    /**
     * Get batch delete lock sql string.
     *
     * @param lockTable      the lock table
     * @param paramPlaceHold the param place hold
     * @return the string
     */
    String getBatchDeleteLockSql(String lockTable, String paramPlaceHold);

    /**
     * Get batch delete lock sql string.
     *
     * @param lockTable the lock table
     * @return the string
     */
    String getBatchDeleteLockSqlByBranch(String lockTable);

    /**
     * Get batch delete lock sql string.
     *
     * @param lockTable      the lock table
     * @param paramPlaceHold the param place hold
     * @return the string
     */
    String getBatchDeleteLockSqlByBranchs(String lockTable, String paramPlaceHold);

    /**
     * Get query lock sql string.
     *
     * @param lockTable the lock table
     * @return the string
     */
    String getQueryLockSql(String lockTable);

    /**
     * Get check lock sql string.
     *
     * @param lockTable      the lock table
     * @param paramPlaceHold the param place hold
     * @return the string
     */
    String getCheckLockableSql(String lockTable, String paramPlaceHold);

    /**
     * get batch update status lock by global sql
     *
     * @param lockTable      the lock table
     * @return the string
     */
    String getBatchUpdateStatusLockByGlobalSql(String lockTable) ;

}
