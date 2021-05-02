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
package io.seata.sqlparser.druid;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author chd
 */
public class DruidSQLRecognizerFactoryImplTest {
    String sqlForUpdate = "select * from tb where id=1 for update";
    String sqlCommon = "select * from tb where id=1";

    @Test
    public void testIsForUpdate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        DruidSQLRecognizerFactoryImpl factory = new DruidSQLRecognizerFactoryImpl();
        Method method = DruidSQLRecognizerFactoryImpl.class.getDeclaredMethod("isForUpdate", SQLStatement.class, String.class);
        method.setAccessible(true);
        // mysql for update
        List<SQLStatement> asts = SQLUtils.parseStatements(sqlForUpdate, JdbcConstants.MYSQL);
        Assertions.assertTrue((boolean)method.invoke(factory, asts.get(0), JdbcConstants.MYSQL));

        // mysql not for update
        asts = SQLUtils.parseStatements(sqlCommon, JdbcConstants.MYSQL);
        Assertions.assertFalse((boolean)method.invoke(factory, asts.get(0), JdbcConstants.MYSQL));

        // oracle for update
        asts = SQLUtils.parseStatements(sqlForUpdate, JdbcConstants.ORACLE);
        Assertions.assertTrue((boolean)method.invoke(factory, asts.get(0), JdbcConstants.ORACLE));

        // oracle not for update
        asts = SQLUtils.parseStatements(sqlCommon, JdbcConstants.ORACLE);
        Assertions.assertFalse((boolean)method.invoke(factory, asts.get(0), JdbcConstants.ORACLE));

        // pg for update
        asts = SQLUtils.parseStatements(sqlForUpdate, JdbcConstants.POSTGRESQL);
        Assertions.assertTrue((boolean)method.invoke(factory, asts.get(0), JdbcConstants.POSTGRESQL));

        // pg not for update
        asts = SQLUtils.parseStatements(sqlCommon, JdbcConstants.POSTGRESQL);
        Assertions.assertFalse((boolean)method.invoke(factory, asts.get(0), JdbcConstants.POSTGRESQL));
    }
}