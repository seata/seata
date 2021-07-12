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

import com.google.common.base.Objects;

/**
 * Field
 *
 * @author sharajava
 */
public class Field implements java.io.Serializable {

    private static final long serialVersionUID = -3489407607572041783L;

    /**
     * The Name.
     */
    private String name;

    private KeyType keyType = KeyType.NULL;

    /**
     * The Type.
     */
    private int type;

    /**
     * The Value.
     */
    private Object value;

    /**
     * Instantiates a new Field.
     */
    public Field() {
    }

    /**
     * Instantiates a new Field.
     *
     * @param name  the name
     * @param type  the type
     * @param value the value
     */
    public Field(String name, int type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param attrName the attr name
     */
    public void setName(String attrName) {
        this.name = attrName;
    }

    /**
     * Gets key type.
     *
     * @return the key type
     */
    public KeyType getKeyType() {
        return keyType;
    }

    /**
     * Sets key type.
     *
     * @param keyType the key type
     */
    public void setKeyType(KeyType keyType) {
        this.keyType = keyType;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * Sets type.
     *
     * @param attrType the attr type
     */
    public void setType(int attrType) {
        this.type = attrType;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets value.
     *
     * @param value the value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Is key boolean.
     *
     * @param pkname the pkname
     * @return the boolean
     */
    public boolean isKey(String pkname) {
        return name.equalsIgnoreCase(pkname);
    }

    @Override
    public String toString() {
        return String.format("[%s,%s]", name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Field field = (Field) o;
        return type == field.type && Objects.equal(name, field.name) && keyType == field.keyType
                && Objects.equal(value, field.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, keyType, type, value);
    }
}
