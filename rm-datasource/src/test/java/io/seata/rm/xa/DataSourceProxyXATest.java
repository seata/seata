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
package io.seata.rm.xa;

import com.alibaba.druid.pool.DruidDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerXADataSource;
import com.mysql.jdbc.JDBC4MySQLConnection;
import com.mysql.jdbc.jdbc2.optional.JDBC4ConnectionWrapper;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.mock.MockDataSource;
import io.seata.rm.datasource.xa.ConnectionProxyXA;
import io.seata.rm.datasource.xa.DataSourceProxyXA;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for DataSourceProxyXA
 *
 * @author sharajava
 */
public class DataSourceProxyXATest {

    @Test
    public void test_constructor() {
        DataSource dataSource = new MockDataSource();

        DataSourceProxyXA dataSourceProxy = new DataSourceProxyXA(dataSource);
        Assertions.assertEquals(dataSourceProxy.getTargetDataSource(), dataSource);

        DataSourceProxyXA dataSourceProxy2 = new DataSourceProxyXA(dataSourceProxy);
        Assertions.assertEquals(dataSourceProxy2.getTargetDataSource(), dataSource);
    }

    @Test
    public void testGetConnection() throws SQLException {
        // Mock
        Driver driver = Mockito.mock(Driver.class);
        JDBC4MySQLConnection connection = Mockito.mock(JDBC4MySQLConnection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(metaData.getURL()).thenReturn("jdbc:mysql:xxx");
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(driver.connect(any(), any())).thenReturn(connection);

        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriver(driver);
        DataSourceProxyXA dataSourceProxyXA = new DataSourceProxyXA(druidDataSource);
        Connection connFromDataSourceProxyXA = dataSourceProxyXA.getConnection();
        Assertions.assertFalse(connFromDataSourceProxyXA instanceof ConnectionProxyXA);
        RootContext.bind("test");
        connFromDataSourceProxyXA = dataSourceProxyXA.getConnection();

        Assertions.assertTrue(connFromDataSourceProxyXA instanceof ConnectionProxyXA);
        ConnectionProxyXA connectionProxyXA = (ConnectionProxyXA)dataSourceProxyXA.getConnection();

        Connection wrappedConnection = connectionProxyXA.getWrappedConnection();
        Assertions.assertTrue(wrappedConnection instanceof PooledConnection);

        Connection wrappedPhysicalConn = ((PooledConnection)wrappedConnection).getConnection();
        Assertions.assertTrue(wrappedPhysicalConn == connection);

        XAConnection xaConnection = connectionProxyXA.getWrappedXAConnection();
        Connection connectionInXA = xaConnection.getConnection();
        Assertions.assertTrue(connectionInXA instanceof JDBC4ConnectionWrapper);
    }

    @Test
    public void testGetSqlServerXADataSource() throws SQLException {
        // Mock
        Driver driver = Mockito.mock(Driver.class);
        JDBC4MySQLConnection connection = Mockito.mock(JDBC4MySQLConnection.class);
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(metaData.getURL()).thenReturn("jdbc:sqlserver://127.0.0.1:1433;DatabaseName=test");
        Mockito.when(metaData.getUserName()).thenReturn("sqlserver");
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(driver.connect(any(), any())).thenReturn(connection);

        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriver(driver);
        druidDataSource.setPassword("123456");
        DataSourceProxyXA dataSourceProxyXA = new DataSourceProxyXA(druidDataSource);

        Assertions.assertTrue(dataSourceProxyXA.getTargetDataSource() instanceof SQLServerXADataSource);
    }

    @AfterAll
    public static void tearDown(){
        RootContext.unbind();
    }
}
