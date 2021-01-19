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
package io.seata.rm.datasource.exec.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.BaseInsertExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jsbxyyx
 */
@LoadLevel(name = JdbcConstants.MYSQL, scope = Scope.PROTOTYPE)
public class MySQLInsertExecutor extends BaseInsertExecutor implements Defaultable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLInsertExecutor.class);

    /**
     * the modify for test
     */
    public static final String ERR_SQL_STATE = "S1009";

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public MySQLInsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
                               SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    public Map<String,List<Object>> getPkValues() throws SQLException {
        Map<String,List<Object>> pkValuesMap = null;
        List<String> pkColumnNameList = getTableMeta().getPrimaryKeyOnlyName();
        Boolean isContainsPk = containsPK();
        //when there is only one pk in the table
        if (getTableMeta().getPrimaryKeyOnlyName().size() == 1) {
            if (isContainsPk) {
                pkValuesMap = getPkValuesByColumn();
            }
            else if (containsColumns()) {
                pkValuesMap = getPkValuesByAuto();
            }
            else {
                pkValuesMap = getPkValuesByColumn();
            }
        } else {
            //when there is multiple pk in the table
            //1,all pk columns are filled value.
            //2,the auto increment pk column value is null, and other pk value are not null.
            pkValuesMap = getPkValuesByColumn();
            for (String columnName:pkColumnNameList) {
                if (!pkValuesMap.containsKey(columnName)) {
                    ColumnMeta pkColumnMeta = getTableMeta().getColumnMeta(columnName);
                    if (Objects.nonNull(pkColumnMeta) && pkColumnMeta.isAutoincrement()) {
                        //3,the auto increment pk column is not exits in sql, and other pk are exits also the value is not null.
                        pkValuesMap.putAll(getPkValuesByAuto());
                    }
                }
            }
        }
        return pkValuesMap;
    }

    /**
     * the modify for test
     */
    public Map<String, List<Object>> getPkValuesByAuto() throws SQLException {
        // PK is just auto generated
        Map<String, List<Object>> pkValuesMap = new HashMap<>(8);
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        String autoColumnName = null;
        for (Map.Entry<String, ColumnMeta> entry : pkMetaMap.entrySet()) {
            if (entry.getValue().isAutoincrement()) {
                autoColumnName = entry.getKey();
                break;
            }
        }
        if (StringUtils.isBlank(autoColumnName)) {
            throw new ShouldNeverHappenException();
        }

        ResultSet genKeys;
        try {
            genKeys = statementProxy.getGeneratedKeys();
        } catch (SQLException e) {
            // java.sql.SQLException: Generated keys not requested. You need to
            // specify Statement.RETURN_GENERATED_KEYS to
            // Statement.executeUpdate() or Connection.prepareStatement().
            if (ERR_SQL_STATE.equalsIgnoreCase(e.getSQLState())) {
                LOGGER.warn("Fail to get auto-generated keys, use 'SELECT LAST_INSERT_ID()' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.");
                genKeys = statementProxy.getTargetStatement().executeQuery("SELECT LAST_INSERT_ID()");
            } else {
                throw e;
            }
        }
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        try {
            genKeys.beforeFirst();
        } catch (SQLException e) {
            LOGGER.warn("Fail to reset ResultSet cursor. can not get primary key value");
        }
        pkValuesMap.put(autoColumnName,pkValues);
        return pkValuesMap;
    }

    @Override
    public Map<String,List<Object>> getPkValuesByColumn() throws SQLException {
        Map<String,List<Object>> pkValuesMap  = parsePkValuesFromStatement();
        Set<String> keySet = new HashSet<>(pkValuesMap.keySet());
        //auto increment
        for (String pkKey:keySet) {
            List<Object> pkValues = pkValuesMap.get(pkKey);
            // pk auto generated while single insert primary key is expression
            if (pkValues.size() == 1 && (pkValues.get(0) instanceof SqlMethodExpr)) {
                pkValuesMap.putAll(getPkValuesByAuto());
            }
            // pk auto generated while column exists and value is null
            else if (!pkValues.isEmpty() && pkValues.get(0) instanceof Null) {
                pkValuesMap.putAll(getPkValuesByAuto());
            }
        }
        return pkValuesMap;
    }

    @Override
    public List<Object> getPkValuesByDefault() throws SQLException {
        // mysql default keyword the logic not support. (sample: insert into test(id, name) values(default, 'xx'))
        throw new NotSupportYetException();
    }
}
