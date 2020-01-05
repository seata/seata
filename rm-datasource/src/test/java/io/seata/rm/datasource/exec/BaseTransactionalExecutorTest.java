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
package io.seata.rm.datasource.exec;

import io.seata.rm.GlobalLockTemplate;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class BaseTransactionalExecutorTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testExecuteWithGlobalLockSet() throws Exception {

        //initial objects
        ConnectionProxy connectionProxy = new ConnectionProxy(null, null);
        StatementProxy statementProxy = new StatementProxy<>(connectionProxy, null);

        BaseTransactionalExecutor<Object, Statement> baseTransactionalExecutor
                = new BaseTransactionalExecutor<Object, Statement>(statementProxy, null, null) {
            @Override
            protected Object doExecute(Object... args) {
                return null;
            }
        };

        // not in global lock context
        ((Callable<Object>) () -> {
            try {
                baseTransactionalExecutor.execute(new Object());
                Assertions.assertTrue(!connectionProxy.isGlobalLockRequire(), "conectionContext set!");
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return null;
        }).call();

        //in global lock context
        GlobalLockTemplate.execute(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                try {
                    baseTransactionalExecutor.execute(new Object());
                    Assertions.assertTrue(connectionProxy.isGlobalLockRequire(), "conectionContext not set!");
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });

    }

    @Test
    public void testBuildLockKey() {
        //build expect data
        String tableName = "test_name";
        String fieldOne = "field_one";
        String fieldTwo = "field_two";
        String split1 = ":";
        String split2 = ",";
        String buildLockKeyExpect = tableName + split1 + fieldOne + split2 + fieldTwo;
        // mock field
        Field field1 = mock(Field.class);
        when(field1.getValue()).thenReturn(fieldOne);
        Field field2 = mock(Field.class);
        when(field2.getValue()).thenReturn(fieldTwo);
        List<Field> fieldList = new ArrayList<>();
        fieldList.add(field1);
        fieldList.add(field2);
        // mock tableMeta
        TableMeta tableMeta = mock(TableMeta.class);
        when(tableMeta.getTableName()).thenReturn(tableName);
        // mock tableRecords
        TableRecords tableRecords = mock(TableRecords.class);
        when(tableRecords.getTableMeta()).thenReturn(tableMeta);
        when(tableRecords.pkRows()).thenReturn(fieldList);
        when(tableRecords.size()).thenReturn(fieldList.size());
        // mock executor
        BaseTransactionalExecutor executor = mock(BaseTransactionalExecutor.class);
        when(executor.buildLockKey(tableRecords)).thenCallRealMethod();
        assertThat(executor.buildLockKey(tableRecords)).isEqualTo(buildLockKeyExpect);
    }

}
