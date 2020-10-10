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
package io.seata.rm.datasource.sql.struct.cache;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Objects;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.IndexMeta;
import io.seata.rm.datasource.sql.struct.IndexType;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Table meta cache.
 *
 * @author sharajava
 */
@LoadLevel(name = JdbcConstants.MYSQL)
public class MysqlTableMetaCache extends AbstractTableMetaCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlTableMetaCache.class);

    @Override
    protected String getCacheKey(Connection connection, String tableName, String resourceId) {
        StringBuilder cacheKey = new StringBuilder(resourceId);
        cacheKey.append(".");
        //remove single quote and separate it to catalogName and tableName
        String[] tableNameWithCatalog = tableName.replace("`", "").split("\\.");
        String defaultTableName = tableNameWithCatalog.length > 1 ? tableNameWithCatalog[1] : tableNameWithCatalog[0];

        DatabaseMetaData databaseMetaData = null;
        try {
            databaseMetaData = connection.getMetaData();
        } catch (SQLException e) {
            LOGGER.error("Could not get connection, use default cache key {}", e.getMessage(), e);
            return cacheKey.append(defaultTableName).toString();
        }

        try {
            //prevent duplicated cache key
            if (databaseMetaData.supportsMixedCaseIdentifiers()) {
                cacheKey.append(defaultTableName);
            } else {
                cacheKey.append(defaultTableName.toLowerCase());
            }
        } catch (SQLException e) {
            LOGGER.error("Could not get supportsMixedCaseIdentifiers in connection metadata, use default cache key {}", e.getMessage(), e);
            return cacheKey.append(defaultTableName).toString();
        }

        return cacheKey.toString();
    }

    @Override
    protected TableMeta fetchSchema(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT * FROM " + ColumnUtils.addEscape(tableName, JdbcConstants.MYSQL) + " LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return resultSetMetaToSchema(rs.getMetaData(), connection.getMetaData());
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } catch (Exception e) {
            throw new SQLException(String.format("Failed to fetch schema of %s", tableName), e);
        }
    }

    private TableMeta resultSetMetaToSchema(ResultSetMetaData rsmd, DatabaseMetaData dbmd)
            throws SQLException {
        //always "" for mysql
        String schemaName = rsmd.getSchemaName(1);
        String catalogName = rsmd.getCatalogName(1);
        /*
         * use ResultSetMetaData to get the pure table name
         * can avoid the problem below
         *
         * select * from account_tbl
         * select * from account_TBL
         * select * from `account_tbl`
         * select * from account.account_tbl
         */
        String tableName = rsmd.getTableName(1);

        TableMeta tm = new TableMeta();
        tm.setTableName(tableName);

        /*
         * here has two different type to get the data
         * make sure the table name was right
         * 1. show full columns from xxx from xxx(normal)
         * 2. select xxx from xxx where catalog_name like ? and table_name like ?(informationSchema=true)
         */

        try (ResultSet rsColumns = dbmd.getColumns(catalogName, schemaName, tableName, "%");
             ResultSet rsIndex = dbmd.getIndexInfo(catalogName, schemaName, tableName, false, true);
             ResultSet vsColumns = dbmd.getVersionColumns(catalogName, schemaName, tableName)) {
            while (rsColumns.next()) {
                ColumnMeta col = new ColumnMeta();
                col.setTableCat(rsColumns.getString("TABLE_CAT"));
                col.setTableSchemaName(rsColumns.getString("TABLE_SCHEM"));
                col.setTableName(rsColumns.getString("TABLE_NAME"));
                col.setColumnName(rsColumns.getString("COLUMN_NAME"));
                col.setDataType(rsColumns.getInt("DATA_TYPE"));
                col.setDataTypeName(rsColumns.getString("TYPE_NAME"));
                col.setColumnSize(rsColumns.getInt("COLUMN_SIZE"));
                col.setDecimalDigits(rsColumns.getInt("DECIMAL_DIGITS"));
                col.setNumPrecRadix(rsColumns.getInt("NUM_PREC_RADIX"));
                col.setNullAble(rsColumns.getInt("NULLABLE"));
                col.setRemarks(rsColumns.getString("REMARKS"));
                col.setColumnDef(rsColumns.getString("COLUMN_DEF"));
                col.setSqlDataType(rsColumns.getInt("SQL_DATA_TYPE"));
                col.setSqlDatetimeSub(rsColumns.getInt("SQL_DATETIME_SUB"));
                col.setCharOctetLength(rsColumns.getInt("CHAR_OCTET_LENGTH"));
                col.setOrdinalPosition(rsColumns.getInt("ORDINAL_POSITION"));
                col.setIsNullAble(rsColumns.getString("IS_NULLABLE"));
                col.setIsAutoincrement(rsColumns.getString("IS_AUTOINCREMENT"));

                tm.getAllColumns().put(col.getColumnName(), col);
            }
            while (Objects.nonNull(vsColumns) && vsColumns.next()) {
                String columnName = vsColumns.getString("COLUMN_NAME");
                int dataType = vsColumns.getInt("DATA_TYPE");
                ColumnMeta col = tm.getAllColumns().get(columnName);
                if (col != null && dataType == Types.TIMESTAMP) {
                    col.setIsOnUpdateTimestamp("Yes");
                }
            }

            while (rsIndex.next()) {
                String indexName = rsIndex.getString("INDEX_NAME");
                String colName = rsIndex.getString("COLUMN_NAME");
                ColumnMeta col = tm.getAllColumns().get(colName);

                if (tm.getAllIndexes().containsKey(indexName)) {
                    IndexMeta index = tm.getAllIndexes().get(indexName);
                    index.getValues().add(col);
                } else {
                    IndexMeta index = new IndexMeta();
                    index.setIndexName(indexName);
                    index.setNonUnique(rsIndex.getBoolean("NON_UNIQUE"));
                    index.setIndexQualifier(rsIndex.getString("INDEX_QUALIFIER"));
                    index.setIndexName(rsIndex.getString("INDEX_NAME"));
                    index.setType(rsIndex.getShort("TYPE"));
                    index.setOrdinalPosition(rsIndex.getShort("ORDINAL_POSITION"));
                    index.setAscOrDesc(rsIndex.getString("ASC_OR_DESC"));
                    index.setCardinality(rsIndex.getInt("CARDINALITY"));
                    index.getValues().add(col);
                    if ("PRIMARY".equalsIgnoreCase(indexName)) {
                        index.setIndextype(IndexType.PRIMARY);
                    } else if (!index.isNonUnique()) {
                        index.setIndextype(IndexType.UNIQUE);
                    } else {
                        index.setIndextype(IndexType.NORMAL);
                    }
                    tm.getAllIndexes().put(indexName, index);

                }
            }
            if (tm.getAllIndexes().isEmpty()) {
                throw new ShouldNeverHappenException("Could not found any index in the table: " + tableName);
            }
        }
        return tm;
    }
}
