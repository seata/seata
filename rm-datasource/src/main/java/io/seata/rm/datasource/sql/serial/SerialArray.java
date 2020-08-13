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
package io.seata.rm.datasource.sql.serial;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialDatalink;
import javax.sql.rowset.serial.SerialException;
import javax.sql.rowset.serial.SerialJavaObject;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

/**
 * used for jdbc type is JDBCType.ARRAY serialize.
 *
 * @author jsbxyyx
 */
public class SerialArray implements java.sql.Array, java.io.Serializable {

    static final long serialVersionUID = 1L;

    private Object[] elements;
    private int baseType;
    private String baseTypeName;
    private int len;

    public SerialArray() {
    }

    public SerialArray(java.sql.Array array) throws SerialException, SQLException {
        if (array == null) {
            throw new SQLException("Cannot instantiate a SerialArray " +
                    "object with a null Array object");
        }

        if ((elements = (Object[]) array.getArray()) == null) {
            throw new SQLException("Invalid Array object. Calls to Array.getArray() " +
                    "return null value which cannot be serialized");
        }

        baseType = array.getBaseType();
        baseTypeName = array.getBaseTypeName();
        len = elements.length;

        switch (baseType) {
            case java.sql.Types.BLOB:
                for (int i = 0; i < len; i++) {
                    elements[i] = new SerialBlob((Blob) elements[i]);
                }
                break;
            case java.sql.Types.CLOB:
                for (int i = 0; i < len; i++) {
                    elements[i] = new SerialClob((Clob) elements[i]);
                }
                break;
            case java.sql.Types.DATALINK:
                for (int i = 0; i < len; i++) {
                    elements[i] = new SerialDatalink((URL) elements[i]);
                }
                break;
            case java.sql.Types.JAVA_OBJECT:
                for (int i = 0; i < len; i++) {
                    elements[i] = new SerialJavaObject(elements[i]);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public String getBaseTypeName() throws SQLException {
        return baseTypeName;
    }

    public void setBaseTypeName(String baseTypeName) {
        this.baseTypeName = baseTypeName;
    }

    @Override
    public int getBaseType() throws SQLException {
        return baseType;
    }

    public void setBaseType(int baseType) {
        this.baseType = baseType;
    }

    @Override
    public Object getArray() throws SQLException {
        return elements;
    }

    @Override
    public Object getArray(Map<String, Class<?>> map) throws SQLException {
        return elements;
    }

    @Override
    public Object getArray(long index, int count) throws SQLException {
        return elements;
    }

    @Override
    public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
        return elements;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        // don't throws exception.
        return null;
    }

    @Override
    public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
        // don't throws exception.
        return null;
    }

    @Override
    public ResultSet getResultSet(long index, int count) throws SQLException {
        // don't throws exception.
        return null;
    }

    @Override
    public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {
        // don't throws exception.
        return null;
    }

    @Override
    public void free() throws SQLException {
        if (elements != null) {
            elements = null;
            baseTypeName = null;
        }
    }

    public Object[] getElements() {
        return elements;
    }

    public void setElements(Object[] elements) {
        this.elements = elements;
        this.len = elements != null ? elements.length : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof SerialArray) {
            SerialArray sa = (SerialArray) obj;
            return baseType == sa.baseType &&
                    baseTypeName.equals(sa.baseTypeName) &&
                    Arrays.equals(elements, sa.elements);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (((31 + Arrays.hashCode(elements)) * 31 + len) * 31 +
                baseType) * 31 + baseTypeName.hashCode();
    }

}
