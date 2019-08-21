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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.sql.SQLInsertRecognizer;
import io.seata.rm.datasource.sql.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author zjinlei
 * @date 2019/07/16
 */
public class BatchInsertExecutorTest2 {

    private static final String ID_COLUMN = "id";
    private static final String USER_ID_COLUMN = "user_id";
    private static final String USER_NAME_COLUMN = "user_name";
    private static final List<Integer> PK_VALUES = Arrays.asList(100000001, 100000002, 100000003, 100000004, 100000005);

    private PreparedStatementProxy statementProxy;

    private SQLInsertRecognizer sqlInsertRecognizer;

    private StatementCallback statementCallback;

    private TableMeta tableMeta;

    private InsertExecutor insertExecutor;

    @BeforeEach
    public void init() {
        statementProxy = mock(PreparedStatementProxy.class);
        statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta = mock(TableMeta.class);
        insertExecutor = Mockito.spy(new InsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));
    }

    @Test
    public void testGetPkValuesByColumnOfJDBC() throws SQLException {
        mockInsertColumns();
        mockParameters();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        List<Integer> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesByColumn, pkValues);
    }

    @Test
    public void testGetPkValuesByColumnAndAllRefOfJDBC() throws SQLException {
        mockInsertColumns();
        mockParametersWithAllRefOfJDBC();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        List<Integer> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesByColumn, pkValues);
    }

    @Test
    public void testGetPkValuesByColumnAndPkRefOfJDBC() throws SQLException {
        mockInsertColumns();
        mockParametersWithPkRefOfJDBC();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        List<Integer> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesByColumn, pkValues);
    }

    @Test
    public void testGetPkValuesByColumnAndPkUnRefOfJDBC() throws SQLException {
        mockInsertColumns();
        int pkId = PK_VALUES.get(0);
        mockParametersWithPkUnRefOfJDBC(pkId);
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.add(pkId);
        List<Integer> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesByColumn, pkValues);
    }

    //----------------mysql batch values (),(),()------------------------

    @Test
    public void testGetPkValuesByColumnAndAllRefOfMysql() throws SQLException {
        mockInsertColumns();
        mockParametersAllRefOfMysql();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        List<Integer> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesByColumn, pkValues);
    }

    @Test
    public void testGetPkValuesByColumnAndPkRefOfMysql() throws SQLException {
        mockInsertColumns();
        mockParametersWithPkRefOfMysql();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        List<Integer> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesByColumn, pkValues);
    }

    @Test
    public void testGetPkValuesByColumnAndPkUnRefOfMysql() throws SQLException {
        mockInsertColumns();
        mockParametersWithPkUnRefOfMysql();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        List<Integer> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesByColumn, pkValues);
    }


    private List<String> mockInsertColumns() {
        List<String> columns = new ArrayList<>();
        columns.add(USER_ID_COLUMN);
        columns.add(ID_COLUMN);
        columns.add(USER_NAME_COLUMN);
        when(sqlInsertRecognizer.getInsertColumns()).thenReturn(columns);
        return columns;
    }

    private void mockParameters() {
        int PK_INDEX = 1;
        ArrayList<Object>[] paramters = new ArrayList[3];
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add("userId1");
        arrayList0.add("userId2");
        arrayList0.add("userId3");
        arrayList0.add("userId4");
        arrayList0.add("userId5");
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add(PK_VALUES.get(0));
        arrayList1.add(PK_VALUES.get(1));
        arrayList1.add(PK_VALUES.get(2));
        arrayList1.add(PK_VALUES.get(3));
        arrayList1.add(PK_VALUES.get(4));
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add("userName1");
        arrayList2.add("userName2");
        arrayList2.add("userName3");
        arrayList2.add("userName4");
        arrayList2.add("userName5");

        paramters[0] = arrayList0;
        paramters[1] = arrayList1;
        paramters[2] = arrayList2;

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?"));

        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(insertRows);
        when(statementProxy.getParamsByIndex(PK_INDEX)).thenReturn(paramters[PK_INDEX]);
    }

    private void mockParametersAllRefOfMysql() {

        ArrayList<Object>[] paramters = new ArrayList[15];
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add("userId1");
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add(100000001);
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userName1");
        ArrayList arrayList4 = new ArrayList<>();
        arrayList4.add("userId2");
        ArrayList arrayList5 = new ArrayList<>();
        arrayList5.add(100000002);
        ArrayList arrayList6 = new ArrayList<>();
        arrayList6.add("userName2");
        ArrayList arrayList7 = new ArrayList<>();
        arrayList7.add("userId3");
        ArrayList arrayList8 = new ArrayList<>();
        arrayList8.add(100000003);
        ArrayList arrayList9 = new ArrayList<>();
        arrayList9.add("userName3");
        ArrayList arrayList10 = new ArrayList<>();
        arrayList10.add("userId4");
        ArrayList arrayList11 = new ArrayList<>();
        arrayList11.add(100000004);
        ArrayList arrayList12 = new ArrayList<>();
        arrayList12.add("userName4");
        ArrayList arrayList13 = new ArrayList<>();
        arrayList13.add("userId5");
        ArrayList arrayList14 = new ArrayList<>();
        arrayList14.add(100000005);
        ArrayList arrayList15 = new ArrayList<>();
        arrayList15.add("userName5");
        paramters[0] = arrayList1;
        paramters[1] = arrayList2;
        paramters[2] = arrayList3;
        paramters[3] = arrayList4;
        paramters[4] = arrayList5;
        paramters[5] = arrayList6;
        paramters[6] = arrayList7;
        paramters[7] = arrayList8;
        paramters[8] = arrayList9;
        paramters[9] = arrayList10;
        paramters[10] = arrayList11;
        paramters[11] = arrayList12;
        paramters[12] = arrayList13;
        paramters[13] = arrayList14;
        paramters[14] = arrayList15;
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?"));
        insertRows.add(Arrays.asList("?", "?", "?"));
        insertRows.add(Arrays.asList("?", "?", "?"));
        insertRows.add(Arrays.asList("?", "?", "?"));
        insertRows.add(Arrays.asList("?", "?", "?"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(insertRows);
        when(statementProxy.getParameters()).thenReturn(paramters);
        int pkIndex = 1;
        int rowSize = insertRows.size();
        int placeholderNum = paramters.length / rowSize;
        for (int i = 0; i < rowSize; i++) {
            int index = placeholderNum * i + pkIndex;
            when(statementProxy.getParamsByIndex(index)).thenReturn(paramters[index]);
        }
    }

    private void mockParametersWithPkRefOfMysql() {

        ArrayList<Object>[] paramters = new ArrayList[10];
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add("userId1");
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add(100000001);
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userId2");
        ArrayList arrayList4 = new ArrayList<>();
        arrayList4.add(100000002);
        ArrayList arrayList5 = new ArrayList<>();
        arrayList5.add("userId3");
        ArrayList arrayList6 = new ArrayList<>();
        arrayList6.add(100000003);
        ArrayList arrayList7 = new ArrayList<>();
        arrayList7.add("userId4");
        ArrayList arrayList8 = new ArrayList<>();
        arrayList8.add(100000004);
        ArrayList arrayList9 = new ArrayList<>();
        arrayList9.add("userId5");
        ArrayList arrayList10 = new ArrayList<>();
        arrayList10.add(100000005);
        paramters[0] = arrayList1;
        paramters[1] = arrayList2;
        paramters[2] = arrayList3;
        paramters[3] = arrayList4;
        paramters[4] = arrayList5;
        paramters[5] = arrayList6;
        paramters[6] = arrayList7;
        paramters[7] = arrayList8;
        paramters[8] = arrayList9;
        paramters[9] = arrayList10;
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "1"));
        insertRows.add(Arrays.asList("?", "?", "2"));
        insertRows.add(Arrays.asList("?", "?", "3"));
        insertRows.add(Arrays.asList("?", "?", "4"));
        insertRows.add(Arrays.asList("?", "?", "5"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(insertRows);
        when(statementProxy.getParameters()).thenReturn(paramters);
        int pkIndex = 1;
        int rowSize = insertRows.size();
        int placeholderNum = paramters.length / rowSize;
        for (int i = 0; i < rowSize; i++) {
            int index = placeholderNum * i + pkIndex;
            when(statementProxy.getParamsByIndex(index)).thenReturn(paramters[index]);
        }
    }

    private void mockParametersWithPkUnRefOfMysql() {

        ArrayList<Object>[] paramters = new ArrayList[10];
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add("userId1");
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add(100000001);
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userId2");
        ArrayList arrayList4 = new ArrayList<>();
        arrayList4.add(100000002);
        ArrayList arrayList5 = new ArrayList<>();
        arrayList5.add("userId3");
        ArrayList arrayList6 = new ArrayList<>();
        arrayList6.add(100000003);
        ArrayList arrayList7 = new ArrayList<>();
        arrayList7.add("userId4");
        ArrayList arrayList8 = new ArrayList<>();
        arrayList8.add(100000004);
        ArrayList arrayList9 = new ArrayList<>();
        arrayList9.add("userId5");
        ArrayList arrayList10 = new ArrayList<>();
        arrayList10.add(100000005);
        paramters[0] = arrayList1;
        paramters[1] = arrayList2;
        paramters[2] = arrayList3;
        paramters[3] = arrayList4;
        paramters[4] = arrayList5;
        paramters[5] = arrayList6;
        paramters[6] = arrayList7;
        paramters[7] = arrayList8;
        paramters[8] = arrayList9;
        paramters[9] = arrayList10;
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", 100000001, "?"));
        insertRows.add(Arrays.asList("?", 100000002, "?"));
        insertRows.add(Arrays.asList("?", 100000003, "?"));
        insertRows.add(Arrays.asList("?", 100000004, "?"));
        insertRows.add(Arrays.asList("?", 100000005, "?"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(insertRows);
        when(statementProxy.getParameters()).thenReturn(paramters);
        int pkIndex = 1;
        int rowSize = insertRows.size();
        int placeholderNum = paramters.length / rowSize;
        for (int i = 0; i < rowSize; i++) {
            int index = placeholderNum * i + pkIndex;
            when(statementProxy.getParamsByIndex(index)).thenReturn(paramters[index]);
        }
    }


    private void mockParametersWithAllRefOfJDBC() {
        int PK_INDEX = 1;
        ArrayList<Object>[] paramters = new ArrayList[3];
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add("userId1");
        arrayList0.add("userId2");
        arrayList0.add("userId3");
        arrayList0.add("userId4");
        arrayList0.add("userId5");
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add(PK_VALUES.get(0));
        arrayList1.add(PK_VALUES.get(1));
        arrayList1.add(PK_VALUES.get(2));
        arrayList1.add(PK_VALUES.get(3));
        arrayList1.add(PK_VALUES.get(4));
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add("userName1");
        arrayList2.add("userName2");
        arrayList2.add("userName3");
        arrayList2.add("userName4");
        arrayList2.add("userName5");
        paramters[0] = arrayList0;
        paramters[1] = arrayList1;
        paramters[2] = arrayList2;

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(insertRows);
        when(statementProxy.getParamsByIndex(PK_INDEX)).thenReturn(paramters[PK_INDEX]);
    }

    private void mockParametersWithPkRefOfJDBC() {
        int PK_INDEX = 1;
        ArrayList<Object>[] paramters = new ArrayList[2];
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add("userId1");
        arrayList0.add("userId2");
        arrayList0.add("userId3");
        arrayList0.add("userId4");
        arrayList0.add("userId5");
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add(PK_VALUES.get(0));
        arrayList1.add(PK_VALUES.get(1));
        arrayList1.add(PK_VALUES.get(2));
        arrayList1.add(PK_VALUES.get(3));
        arrayList1.add(PK_VALUES.get(4));
        paramters[0] = arrayList0;
        paramters[1] = arrayList1;

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "userName1"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(insertRows);
        when(statementProxy.getParamsByIndex(PK_INDEX)).thenReturn(paramters[PK_INDEX]);
    }


    private void mockParametersWithPkUnRefOfJDBC(int pkId) {
        ArrayList<Object>[] paramters = new ArrayList[2];
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add("userId1");
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add("userName1");
        paramters[0] = arrayList0;
        paramters[1] = arrayList1;

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", pkId, "?"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(insertRows);
    }

}