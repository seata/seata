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

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.IndexMeta;
import io.seata.rm.datasource.sql.struct.IndexType;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.sqlparser.util.JdbcConstants;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The type Table meta cache.
 *
 * @author chengxiaoxiao
 */
@LoadLevel(name = JdbcConstants.DM)
public class DmTableMetaCache extends OracleTableMetaCache {
    public static class TableNameMeta {
        private String schema;
        private String tableName;

        public TableNameMeta(String schema, String tableName) {
            this.schema = schema;
            this.tableName = tableName;
        }

        public String getSchema() {
            return schema;
        }

        public String getTableName() {
            return tableName;
        }
    }

    @Override
    protected TableMeta fetchSchema(Connection connection, String tableName) throws SQLException {
        try {
            return resultSetMetaToSchema(connection.getMetaData(), tableName);
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } catch (Exception e) {
            throw new SQLException(String.format("Failed to fetch schema of %s", tableName), e);
        }
    }

    protected TableMeta resultSetMetaToSchema(DatabaseMetaData dbmd, String tableName) throws SQLException {
        TableMeta result = new TableMeta();
        result.setTableName(tableName);

        TableNameMeta tableNameMeta = toTableNameMeta(tableName, dbmd.getUserName());
        try (ResultSet rsColumns = dbmd.getColumns("", tableNameMeta.getSchema(), tableNameMeta.getTableName(), "%");
             ResultSet rsIndex = dbmd.getIndexInfo(null, tableNameMeta.getSchema(), tableNameMeta.getTableName(), false, true);
             ResultSet rsPrimary = dbmd.getPrimaryKeys(null, tableNameMeta.getSchema(), tableNameMeta.getTableName())) {
            processColumns(result, rsColumns);

            processIndexs(result, rsIndex);

            processPrimaries(result, rsPrimary);

            if (result.getAllIndexes().isEmpty()) {
                throw new ShouldNeverHappenException(String.format("Could not found any index in the table: %s", tableName));
            }
        }

        return result;
    }

    protected TableNameMeta toTableNameMeta(String tableName, String username) {
        String[] schemaTable = tableName.split("\\.");

        String schema = schemaTable.length > 1 ? schemaTable[0] : username;
        if (schema != null) {
            schema = schema.contains("\"") ? schema.replace("\"", "") : schema.toUpperCase();
        }

        tableName = schemaTable.length > 1 ? schemaTable[1] : tableName;
        tableName = tableName.contains("\"") ? tableName.replace("\"", "") : tableName.toUpperCase();

        return new TableNameMeta(schema, tableName);
    }

    protected void processColumns(TableMeta tableMeta, ResultSet rs) throws SQLException {
        while (rs.next()) {
            ColumnMeta col = toColumnMeta(rs);
            tableMeta.getAllColumns().put(col.getColumnName(), col);
        }
    }

    protected void processIndexs(TableMeta tableMeta, ResultSet rs) throws SQLException {
        while (rs.next()) {
            String indexName = rs.getString("INDEX_NAME");
            if (StringUtils.isNullOrEmpty(indexName)) {
                continue;
            }

            String colName = rs.getString("COLUMN_NAME");
            ColumnMeta col = tableMeta.getAllColumns().get(colName);
            if (tableMeta.getAllIndexes().containsKey(indexName)) {
                IndexMeta index = tableMeta.getAllIndexes().get(indexName);
                index.getValues().add(col);
                continue;
            }

            tableMeta.getAllIndexes().put(indexName, toIndexMeta(rs, indexName, col));
        }
    }

    protected void processPrimaries(TableMeta tableMeta, ResultSet rs) throws SQLException {
        while (rs.next()) {
            String colName = rs.getString("COLUMN_NAME");
            for (IndexMeta i : tableMeta.getAllIndexes().values()) {
                i.getValues().stream()
                        .filter(c -> colName.equals(c.getColumnName()))
                        .forEach(c -> i.setIndextype(IndexType.PRIMARY));
            }
        }
    }

    protected ColumnMeta toColumnMeta(ResultSet rs) throws SQLException {
        ColumnMeta result = new ColumnMeta();
        result.setTableCat(rs.getString("TABLE_CAT"));
        result.setTableSchemaName(rs.getString("TABLE_SCHEM"));
        result.setTableName(rs.getString("TABLE_NAME"));
        result.setColumnName(rs.getString("COLUMN_NAME"));
        result.setDataType(rs.getInt("DATA_TYPE"));
        result.setDataTypeName(rs.getString("TYPE_NAME"));
        result.setColumnSize(rs.getInt("COLUMN_SIZE"));
        result.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
        result.setNumPrecRadix(rs.getInt("NUM_PREC_RADIX"));
        result.setNullAble(rs.getInt("NULLABLE"));
        result.setRemarks(rs.getString("REMARKS"));
        result.setColumnDef(rs.getString("COLUMN_DEF"));
        result.setSqlDataType(rs.getInt("SQL_DATA_TYPE"));
        result.setSqlDatetimeSub(rs.getInt("SQL_DATETIME_SUB"));
        result.setCharOctetLength(rs.getInt("CHAR_OCTET_LENGTH"));
        result.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
        result.setIsNullAble(rs.getString("IS_NULLABLE"));
        return result;
    }

    protected IndexMeta toIndexMeta(ResultSet rs, String indexName, ColumnMeta columnMeta) throws SQLException {
        IndexMeta result = new IndexMeta();
        result.setIndexName(indexName);
        result.setNonUnique(rs.getBoolean("NON_UNIQUE"));
        result.setIndexQualifier(rs.getString("INDEX_QUALIFIER"));
        result.setType(rs.getShort("TYPE"));
        result.setOrdinalPosition(rs.getShort("ORDINAL_POSITION"));
        result.setAscOrDesc(rs.getString("ASC_OR_DESC"));
        result.setCardinality(rs.getInt("CARDINALITY"));
        result.getValues().add(columnMeta);
        if (!result.isNonUnique()) {
            result.setIndextype(IndexType.UNIQUE);
        } else {
            result.setIndextype(IndexType.NORMAL);
        }
        return result;
    }
}