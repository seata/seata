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

import java.util.Objects;

/**
 * The type Column meta.
 *
 * @author sharajava
 */
public class ColumnMeta {
    private String tableCat;
    private String tableSchemaName;
    private String tableName;
    private String columnName;
    private int dataType;
    private String dataTypeName;
    private int columnSize;
    private int decimalDigits;
    private int numPrecRadix;
    private int nullAble;
    private String remarks;
    private String columnDef;
    private int sqlDataType;
    private int sqlDatetimeSub;
    private Object charOctetLength;
    private int ordinalPosition;
    private String isNullAble;
    private String isAutoincrement;
    private String isOnUpdateTimestamp;

    /**
     * Instantiates a new Column meta.
     */
    public ColumnMeta() {
    }

    @Override
    public String toString() {
        return "ColumnMeta{" +
                "tableCat='" + tableCat + '\'' +
                ", tableSchemaName='" + tableSchemaName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", columnName='" + columnName + '\'' +
                ", dataType=" + dataType +
                ", dataTypeName='" + dataTypeName + '\'' +
                ", columnSize=" + columnSize +
                ", decimalDigits=" + decimalDigits +
                ", numPrecRadix=" + numPrecRadix +
                ", nullAble=" + nullAble +
                ", remarks='" + remarks + '\'' +
                ", columnDef='" + columnDef + '\'' +
                ", sqlDataType=" + sqlDataType +
                ", sqlDatetimeSub=" + sqlDatetimeSub +
                ", charOctetLength=" + charOctetLength +
                ", ordinalPosition=" + ordinalPosition +
                ", isNullAble='" + isNullAble + '\'' +
                ", isAutoincrement='" + isAutoincrement + '\'' +
                ", isOnUpdateTimestamp='" + isOnUpdateTimestamp + '\'' +
                '}';
    }

    /**
     * Is autoincrement boolean.
     *
     * @return the boolean
     */
    public boolean isAutoincrement() {
        return "YES".equalsIgnoreCase(isAutoincrement);
    }

    /**
     * Gets table cat.
     *
     * @return the table cat
     */
    public String getTableCat() {
        return tableCat;
    }

    /**
     * Sets table cat.
     *
     * @param tableCat the table cat
     */
    public void setTableCat(String tableCat) {
        this.tableCat = tableCat;
    }

    /**
     * Sets table schema name.
     *
     * @param tableSchemaName the table schema name
     */
    public void setTableSchemaName(String tableSchemaName) {
        this.tableSchemaName = tableSchemaName;
    }

    /**
     * Gets table schema name
     * @return
     */
    protected String getTableSchemaName() {
        return tableSchemaName;
    }

    /**
     * Sets table name.
     *
     * @param tableName the table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }


    /**
     * Gets table name
     * @return
     */
    protected String getTableName() {
        return tableName;
    }

    /**
     * Gets column name.
     *
     * @return the column name
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Sets column name.
     *
     * @param columnName the column name
     */
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    /**
     * Gets data type.
     *
     * @return the data type
     */
    public int getDataType() {
        return dataType;
    }

    /**
     * Sets data type.
     *
     * @param dataType the data type
     */
    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    /**
     * Gets data type name.
     *
     * @return the data type name
     */
    public String getDataTypeName() {
        return dataTypeName;
    }

    /**
     * Sets data type name.
     *
     * @param dataTypeName the data type name
     */
    public void setDataTypeName(String dataTypeName) {
        this.dataTypeName = dataTypeName;
    }

    /**
     * Gets column size.
     *
     * @return the column size
     */
    public int getColumnSize() {
        return columnSize;
    }

    /**
     * Sets column size.
     *
     * @param columnSize the column size
     */
    public void setColumnSize(int columnSize) {
        this.columnSize = columnSize;
    }

    /**
     * Gets decimal digits.
     *
     * @return the decimal digits
     */
    public int getDecimalDigits() {
        return decimalDigits;
    }

    /**
     * Sets decimal digits.
     *
     * @param decimalDigits the decimal digits
     */
    public void setDecimalDigits(int decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

    /**
     * Gets num prec radix.
     *
     * @return the num prec radix
     */
    public int getNumPrecRadix() {
        return numPrecRadix;
    }

    /**
     * Sets num prec radix.
     *
     * @param numPrecRadix the num prec radix
     */
    public void setNumPrecRadix(int numPrecRadix) {
        this.numPrecRadix = numPrecRadix;
    }

    /**
     * Gets null able.
     *
     * @return the null able
     */
    public int getNullAble() {
        return nullAble;
    }

    /**
     * Sets null able.
     *
     * @param nullAble the null able
     */
    public void setNullAble(int nullAble) {
        this.nullAble = nullAble;
    }

    /**
     * Gets remarks.
     *
     * @return the remarks
     */
    public String getRemarks() {
        return remarks;
    }

    /**
     * Sets remarks.
     *
     * @param remarks the remarks
     */
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    /**
     * Gets column def.
     *
     * @return the column def
     */
    public String getColumnDef() {
        return columnDef;
    }

    /**
     * Sets column def.
     *
     * @param columnDef the column def
     */
    public void setColumnDef(String columnDef) {
        this.columnDef = columnDef;
    }

    /**
     * Gets sql data type.
     *
     * @return the sql data type
     */
    public int getSqlDataType() {
        return sqlDataType;
    }

    /**
     * Sets sql data type.
     *
     * @param sqlDataType the sql data type
     */
    public void setSqlDataType(int sqlDataType) {
        this.sqlDataType = sqlDataType;
    }

    /**
     * Gets sql datetime sub.
     *
     * @return the sql datetime sub
     */
    public int getSqlDatetimeSub() {
        return sqlDatetimeSub;
    }

    /**
     * Sets sql datetime sub.
     *
     * @param sqlDatetimeSub the sql datetime sub
     */
    public void setSqlDatetimeSub(int sqlDatetimeSub) {
        this.sqlDatetimeSub = sqlDatetimeSub;
    }

    /**
     * Gets char octet length.
     *
     * @return the char octet length
     */
    public Object getCharOctetLength() {
        return charOctetLength;
    }

    /**
     * Sets char octet length.
     *
     * @param charOctetLength the char octet length
     */
    public void setCharOctetLength(Object charOctetLength) {
        this.charOctetLength = charOctetLength;
    }

    /**
     * Gets ordinal position.
     *
     * @return the ordinal position
     */
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    /**
     * Sets ordinal position.
     *
     * @param ordinalPosition the ordinal position
     */
    public void setOrdinalPosition(int ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }

    /**
     * Gets is null able.
     *
     * @return the is null able
     */
    public String getIsNullAble() {
        return isNullAble;
    }

    /**
     * Sets is null able.
     *
     * @param isNullAble the is null able
     */
    public void setIsNullAble(String isNullAble) {
        this.isNullAble = isNullAble;
    }

    /**
     * Gets is autoincrement.
     *
     * @return the is autoincrement
     */
    public String getIsAutoincrement() {
        return isAutoincrement;
    }

    /**
     * Sets is autoincrement.
     *
     * @param isAutoincrement the is autoincrement
     */
    public void setIsAutoincrement(String isAutoincrement) {
        this.isAutoincrement = isAutoincrement;
    }


    /**
     * Sets is onUpdateTimestamp.
     *
     * @return  the is onUpdateTimestamp
     */
    public String getIsOnUpdateTimestamp() {
        return isOnUpdateTimestamp;
    }

    /**
     * Sets is autoincrement.
     *
     * @param isOnUpdateTimestamp the is autoincrement
     */
    public void setIsOnUpdateTimestamp(String isOnUpdateTimestamp) {
        this.isOnUpdateTimestamp = isOnUpdateTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ColumnMeta)) {
            return false;
        }
        ColumnMeta columnMeta = (ColumnMeta) o;
        if (!Objects.equals(columnMeta.tableCat, this.tableCat)) {
            return false;
        }
        if (!Objects.equals(columnMeta.tableSchemaName, this.tableSchemaName)) {
            return false;
        }
        if (!Objects.equals(columnMeta.tableName, this.tableName)) {
            return false;
        }
        if (!Objects.equals(columnMeta.columnName, this.columnName)) {
            return false;
        }
        if (!Objects.equals(columnMeta.dataType, this.dataType)) {
            return false;
        }
        if (!Objects.equals(columnMeta.dataTypeName, this.dataTypeName)) {
            return false;
        }
        if (!Objects.equals(columnMeta.columnSize, this.columnSize)) {
            return false;
        }
        if (!Objects.equals(columnMeta.decimalDigits, this.decimalDigits)) {
            return false;
        }
        if (!Objects.equals(columnMeta.numPrecRadix, this.numPrecRadix)) {
            return false;
        }
        if (!Objects.equals(columnMeta.nullAble, this.nullAble)) {
            return false;
        }
        if (!Objects.equals(columnMeta.remarks, this.remarks)) {
            return false;
        }
        if (!Objects.equals(columnMeta.columnDef, this.columnDef)) {
            return false;
        }
        if (!Objects.equals(columnMeta.sqlDataType, this.sqlDataType)) {
            return false;
        }
        if (!Objects.equals(columnMeta.sqlDatetimeSub, this.sqlDatetimeSub)) {
            return false;
        }
        if (!Objects.equals(columnMeta.charOctetLength, this.charOctetLength)) {
            return false;
        }
        if (!Objects.equals(columnMeta.ordinalPosition, this.ordinalPosition)) {
            return false;
        }
        if (!Objects.equals(columnMeta.isNullAble, this.isNullAble)) {
            return false;
        }
        if (!Objects.equals(columnMeta.isAutoincrement, this.isAutoincrement)) {
            return false;
        }
        if (!Objects.equals(columnMeta.isOnUpdateTimestamp, this.isOnUpdateTimestamp)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hashCode(tableCat);
        hash += Objects.hashCode(tableSchemaName);
        hash += Objects.hashCode(tableName);
        hash += Objects.hashCode(columnName);
        hash += Objects.hashCode(dataType);
        hash += Objects.hashCode(dataTypeName);
        hash += Objects.hashCode(columnSize);
        hash += Objects.hashCode(decimalDigits);
        hash += Objects.hashCode(numPrecRadix);
        hash += Objects.hashCode(nullAble);
        hash += Objects.hashCode(remarks);
        hash += Objects.hashCode(columnDef);
        hash += Objects.hashCode(sqlDataType);
        hash += Objects.hashCode(sqlDatetimeSub);
        hash += Objects.hashCode(charOctetLength);
        hash += Objects.hashCode(ordinalPosition);
        hash += Objects.hashCode(isNullAble);
        hash += Objects.hashCode(isAutoincrement);
        hash += Objects.hashCode(isOnUpdateTimestamp);
        return hash;
    }
}
