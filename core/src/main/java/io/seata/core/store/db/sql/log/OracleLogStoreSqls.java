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
import io.seata.core.store.Pageable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static io.seata.common.DefaultValues.FIRST_PAGE_INDEX;

/**
 * Database log store oracle sql
 * @author will
 */
@LoadLevel(name = "oracle")
public class OracleLogStoreSqls extends AbstractLogStoreSqls {

    /**
     * The constant INSERT_GLOBAL_TRANSACTION_ORACLE.
     */
    public static final String INSERT_GLOBAL_TRANSACTION_ORACLE = "insert into " + GLOBAL_TABLE_PLACEHOLD
            + "(" + ALL_GLOBAL_COLUMNS + ")"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate, sysdate)";

    /**
     * The constant UPDATE_GLOBAL_TRANSACTION_ORACLE.
     */
    public static final String UPDATE_GLOBAL_TRANSACTION_ORACLE = "update " + GLOBAL_TABLE_PLACEHOLD
            + "   set " + SETS_PLACEHOLD
            + "       " + ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED + " = sysdate"
            + " where " + ServerTableColumnsName.GLOBAL_TABLE_XID + " = ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_BY_CONDITION_ORACLE.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_BY_CONDITION_ORACLE = ""
            + " select " + ALL_GLOBAL_COLUMNS
            + "   from " + GLOBAL_TABLE_PLACEHOLD
            + WHERE_PLACEHOLD
            + ORDERBY_PLACEHOLD;

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_ORACLE_PAGING_1.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_ORACLE_PAGING_1 =
            "select A.* from ( " + SQL_PLACEHOLD + " ) A" + " where ROWNUM <= ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_ORACLE_PAGING_2.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_ORACLE_PAGING_2 = ""
            + "select B.* from ("
            + "  select ROWNUM AS RNUM, A.* from ( " + SQL_PLACEHOLD + " ) A" + " where ROWNUM <= ? "
            + ") B where B.RNUM > ?";

    /**
     * The constant INSERT_BRANCH_TRANSACTION_ORACLE.
     */
    public static final String INSERT_BRANCH_TRANSACTION_ORACLE = "insert into " + BRANCH_TABLE_PLACEHOLD
            + "(" + ALL_BRANCH_COLUMNS + ")"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, systimestamp, systimestamp)";

    /**
     * The constant UPDATE_BRANCH_TRANSACTION_ORACLE.
     */
    public static final String UPDATE_BRANCH_TRANSACTION_ORACLE = "update " + BRANCH_TABLE_PLACEHOLD
            + "   set " + SETS_PLACEHOLD
            + "       " + ServerTableColumnsName.BRANCH_TABLE_GMT_MODIFIED + " = systimestamp"
            + " where " + ServerTableColumnsName.BRANCH_TABLE_XID + " = ?"
            + "   and " + ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID + " = ?";

    @Override
    public String getInsertGlobalTransactionSQL(String globalTable) {
        return INSERT_GLOBAL_TRANSACTION_ORACLE.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    @Override
    public String getUpdateGlobalTransactionSQL(String globalTable, String setsPlaceHolder) {
        return UPDATE_GLOBAL_TRANSACTION_ORACLE.replace(GLOBAL_TABLE_PLACEHOLD, globalTable)
            .replace(SETS_PLACEHOLD, setsPlaceHolder);
    }

    @Override
    public String getQueryGlobalTransactionSQLByCondition(String globalTable, String wherePlaceHolder,
                                                          String orderByPlaceHolder, Pageable pageable) {
        String sql = QUERY_GLOBAL_TRANSACTION_BY_CONDITION_ORACLE.replace(GLOBAL_TABLE_PLACEHOLD, globalTable)
            .replace(WHERE_PLACEHOLD, wherePlaceHolder)
            .replace(ORDERBY_PLACEHOLD, wherePlaceHolder);

        if (pageable != null && pageable.getPageSize() > 0) {
            if (pageable.getPageIndex() > FIRST_PAGE_INDEX) {
                sql = QUERY_GLOBAL_TRANSACTION_ORACLE_PAGING_2.replace(SQL_PLACEHOLD, sql);
            } else {
                sql = QUERY_GLOBAL_TRANSACTION_ORACLE_PAGING_1.replace(SQL_PLACEHOLD, sql);
            }
        }

        return sql;
    }

    @Override
    public void setQueryGlobalTransactionSQLPagingParameters(PreparedStatement ps, Pageable pageable, int currentParamIndex) throws SQLException {
        if (pageable.getPageSize() > 0) {
            if (pageable.getPageIndex() > FIRST_PAGE_INDEX) {
                int fromIndex = (pageable.getPageIndex() - FIRST_PAGE_INDEX) * pageable.getPageSize();
                int toIndex = fromIndex + pageable.getPageSize();
                // Different from other databases, first is toIndex, second is fromIndex
                ps.setInt(++currentParamIndex, toIndex);
                ps.setInt(++currentParamIndex, fromIndex);
            } else {
                ps.setInt(++currentParamIndex, pageable.getPageSize());
            }
        }
    }

    @Override
    public String getInsertBranchTransactionSQL(String branchTable) {
        return INSERT_BRANCH_TRANSACTION_ORACLE.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }

    @Override
    public String getUpdateBranchTransactionSQL(String branchTable, String setsPlaceHolder) {
        return UPDATE_BRANCH_TRANSACTION_ORACLE.replace(BRANCH_TABLE_PLACEHOLD, branchTable)
            .replace(SETS_PLACEHOLD, setsPlaceHolder);
    }
}
