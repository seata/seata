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
package io.seata.rm.datasource.sql.struct;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.DataSourceProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author jsbxyyx
 */
public class TableMetaCacheAdapter {

    /**
     * adapter table meta add escape
     * @param dbType the db type
     * @param dataSourceProxy the datasource proxy
     * @param tableName the table name
     * @return the table meta
     */
    public static TableMeta getTableMeta(final String dbType, final DataSourceProxy dataSourceProxy, final String tableName) {
        return getTableMeta(dbType, dataSourceProxy, tableName, true);
    }

    /**
     * adapter table meta
     * @param dbType the db type
     * @param dataSourceProxy the datasource proxy
     * @param tableName the table name
     * @param escape true: add escape. false: not add escape.
     * @return the table meta
     */
    public static TableMeta getTableMeta(final String dbType, final DataSourceProxy dataSourceProxy, final String tableName, boolean escape) {
        TableMeta tableMeta;
        if (JdbcConstants.ORACLE.equalsIgnoreCase(dbType)) {
            tableMeta = TableMetaCacheOracle.getTableMeta(dataSourceProxy, tableName);
        } else {
            tableMeta = TableMetaCache.getTableMeta(dataSourceProxy, tableName);
        }

        if (!escape) {
            return tableMeta;
        }

        TableMetaAdapter adapter = new TableMetaAdapter();

        adapter.setDbType(dbType);
        adapter.setTableName(ColumnUtils.addEscape(tableMeta.getTableName(), dbType));

        for (Map.Entry<String, ColumnMeta> entry : tableMeta.getAllColumns().entrySet()) {
            ColumnMeta value = copyColumnMeta(entry.getValue());
            if (value.getColumnName() != null) {
                value.setColumnName(ColumnUtils.addEscape(value.getColumnName(), dbType));
            }
            if (value.getTableName() != null) {
                value.setTableName(ColumnUtils.addEscape(value.getTableName(), dbType));
            }
            adapter.getAllColumns().put(ColumnUtils.addEscape(entry.getKey(), dbType), value);
        }

        for (Map.Entry<String, IndexMeta> entry : tableMeta.getAllIndexes().entrySet()) {
            IndexMeta value = copyIndexMeta(entry.getValue());
            List<ColumnMeta> values = value.getValues();
            if (values != null) {
                for (int i = 0, len = values.size(); i < len; i++) {
                    ColumnMeta columnMeta = values.get(i);
                    if (columnMeta != null) {
                        if (columnMeta.getColumnName() != null) {
                            columnMeta.setColumnName(ColumnUtils.addEscape(columnMeta.getColumnName(), dbType));
                        }
                        if (columnMeta.getTableName() != null) {
                            columnMeta.setTableName(ColumnUtils.addEscape(columnMeta.getTableName(), dbType));
                        }
                        values.set(i, columnMeta);
                    }
                }
            }
            if (value.getIndexName() != null) {
                value.setIndexName(ColumnUtils.addEscape(value.getIndexName(), dbType));
            }
            adapter.getAllIndexes().put(entry.getKey(), value);
        }

        return adapter;
    }

    private static IndexMeta copyIndexMeta(IndexMeta src) {
        IndexMeta dest = new IndexMeta();
        dest.setIndexName(src.getIndexName());
        dest.setIndextype(src.getIndextype());
        for (ColumnMeta columnMeta : src.getValues()) {
            dest.getValues().add(copyColumnMeta(columnMeta));
        }
        dest.setAscOrDesc(src.getAscOrDesc());
        dest.setIndexQualifier(src.getIndexQualifier());
        dest.setNonUnique(src.isNonUnique());
        dest.setOrdinalPosition(src.getOrdinalPosition());
        dest.setType(src.getType());
        dest.setCardinality(src.getCardinality());
        return dest;
    }

    private static ColumnMeta copyColumnMeta(ColumnMeta src) {
        ColumnMeta dest = new ColumnMeta();
        dest.setIsAutoincrement(src.getIsAutoincrement());
        dest.setTableName(src.getTableName());
        dest.setColumnName(src.getColumnName());
        dest.setColumnDef(src.getColumnDef());
        dest.setIsNullAble(src.getIsNullAble());
        dest.setColumnSize(src.getColumnSize());
        dest.setCharOctetLength(src.getCharOctetLength());
        dest.setDataType(src.getDataType());
        dest.setDataTypeName(src.getDataTypeName());
        dest.setDecimalDigits(src.getDecimalDigits());
        dest.setNullAble(src.getNullAble());
        dest.setNumPrecRadix(src.getNumPrecRadix());
        dest.setOrdinalPosition(src.getOrdinalPosition());
        dest.setRemarks(src.getRemarks());
        dest.setSqlDataType(src.getSqlDataType());
        dest.setSqlDatetimeSub(src.getSqlDatetimeSub());
        dest.setTableCat(src.getTableCat());
        dest.setTableSchemaName(src.getTableSchemaName());
        return dest;
    }

}
