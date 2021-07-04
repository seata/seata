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
package io.seata.xa;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.xa.DruidXADataSource;
import com.alibaba.druid.util.JdbcUtils;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.xa.AbstractDataSourceProxyXA;
import io.seata.rm.datasource.xa.DataSourceProxyXA;
import io.seata.rm.datasource.xa.DataSourceProxyXANative;
import io.seata.rm.datasource.xa.ResourceManagerXA;
import io.seata.rm.datasource.xa.XAXid;
import io.seata.rm.datasource.xa.XAXidBuilder;
import io.seata.spring.annotation.GlobalTransactionScanner;
import io.seata.sqlparser.util.JdbcConstants;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.postgresql.xa.PGXADataSource;

public class XAModeTest2 {

    private static final int testRecordId = 888;
    private static final String testRecordName = "xxx";
    private static final long testTid = 1582688600006L;
    private static final String mockXid = "127.0.0.1:8091:" + testTid;
    private static final long mockBranchId = testTid + 1;

    private static final String pg_jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/postgres";
    private static final String pg_username = "postgres";
    private static final String pg_password = "postgres";
    private static final String pg_driverClassName = JdbcUtils.POSTGRESQL_DRIVER;

    private static final String mysql_jdbcUrl = "jdbc:mysql://127.0.0.1:3306/demo";
    private static final String mysql_username = "demo";
    private static final String mysql_password = "demo";
    private static final String mysql_driverClassName = JdbcUtils.MYSQL_DRIVER;

    private static final String mysql8_jdbcUrl = "jdbc:mysql://0.0.0.0:3306/demo?useUnicode=true&characterEncoding=utf-8&useSSL=false";
    private static final String mysql8_username = "demo";
    private static final String mysql8_password = "demo";
    private static final String mysql8_driverClassName = JdbcUtils.MYSQL_DRIVER_6;

    private static final String oracle_jdbcUrl = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String oracle_username = "demo";
    private static final String oracle_password = "demo";
    private static final String oracle_driverClassName = JdbcUtils.ORACLE_DRIVER;

    // Test on different DB, including: MySQL(5.7, 8.0), PostgreSQL(11), Oracle(11)
    private static final String dbType = JdbcConstants.MYSQL;

    private static final boolean nativeXA = false;

    private static final boolean mySQL8 = false;

    private DruidDataSource createNewDruidDataSource() throws Throwable {
        DruidDataSource druidDataSource = new DruidDataSource();
        initDruidDataSource(druidDataSource);
        return druidDataSource;

    }

    private DruidXADataSource createNewDruidXADataSource() throws Throwable {
        DruidXADataSource druidDataSource = new DruidXADataSource();
        initDruidDataSource(druidDataSource);
        return druidDataSource;

    }

    private XADataSource createNewNativeXADataSource() throws Throwable {
        if (dbType.equalsIgnoreCase(JdbcConstants.POSTGRESQL)) {
            PGXADataSource pgxaDataSource = new PGXADataSource();
            pgxaDataSource.setUrl(pg_jdbcUrl);
            pgxaDataSource.setUser(pg_username);
            pgxaDataSource.setPassword(pg_password);
            return pgxaDataSource;

        } else if (dbType.equalsIgnoreCase(JdbcConstants.MYSQL)) {
            MysqlXADataSource mysqlXADataSource = new MysqlXADataSource();
            if (mySQL8) {
                mysqlXADataSource.setURL(mysql8_jdbcUrl);
                mysqlXADataSource.setUser(mysql8_username);
                mysqlXADataSource.setPassword(mysql8_username);

            } else {
                mysqlXADataSource.setURL(mysql_jdbcUrl);
                mysqlXADataSource.setUser(mysql_username);
                mysqlXADataSource.setPassword(mysql_username);
            }
            return mysqlXADataSource;

        } else if (dbType.equalsIgnoreCase(JdbcConstants.ORACLE)) {
            return createOracleXADataSource();

        } else {
            throw new IllegalAccessError("Unknown dbType: " + dbType);
        }
    }

    private XADataSource createOracleXADataSource() {
        try {
            Class oracleXADataSourceClass = Class.forName("oracle.jdbc.xa.client.OracleXADataSource");
            XADataSource xaDataSource = (XADataSource)oracleXADataSourceClass.newInstance();

            Method setURLMethod = oracleXADataSourceClass.getMethod("setURL", String.class);
            setURLMethod.invoke(xaDataSource, oracle_jdbcUrl);

            Method setUserMethod = oracleXADataSourceClass.getMethod("setUser", String.class);
            setUserMethod.invoke(xaDataSource, oracle_username);

            Method setPasswordMethod = oracleXADataSourceClass.getMethod("setPassword", String.class);
            setPasswordMethod.invoke(xaDataSource, oracle_password);

            Method setDriverTypeMethod = oracleXADataSourceClass.getMethod("setDriverType", String.class);
            setDriverTypeMethod.invoke(xaDataSource, oracle_driverClassName);

            return xaDataSource;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    private void initDruidDataSource(DruidDataSource druidDataSource) throws Throwable {
        druidDataSource.setDbType(dbType);
        if (dbType.equalsIgnoreCase(JdbcConstants.POSTGRESQL)) {
            druidDataSource.setUrl(pg_jdbcUrl);
            druidDataSource.setUsername(pg_username);
            druidDataSource.setPassword(pg_password);
            druidDataSource.setDriverClassName(pg_driverClassName);

        } else if (dbType.equalsIgnoreCase(JdbcConstants.MYSQL)) {
            if (mySQL8) {
                druidDataSource.setUrl(mysql8_jdbcUrl);
                druidDataSource.setUsername(mysql8_username);
                druidDataSource.setPassword(mysql8_password);
                druidDataSource.setDriverClassName(mysql8_driverClassName);

            } else {
                druidDataSource.setUrl(mysql_jdbcUrl);
                druidDataSource.setUsername(mysql_username);
                druidDataSource.setPassword(mysql_password);
                druidDataSource.setDriverClassName(mysql_driverClassName);
            }

        } else if (dbType.equalsIgnoreCase(JdbcConstants.ORACLE)) {
            druidDataSource.setUrl(oracle_jdbcUrl);
            druidDataSource.setUsername(oracle_username);
            druidDataSource.setPassword(oracle_password);
            druidDataSource.setDriverClassName(oracle_driverClassName);

        } else {
            throw new IllegalAccessError("Unknown dbType: " + dbType);
        }
        druidDataSource.init();
    }

    private void initRM() throws Throwable {
        // init RM
        DefaultResourceManager.get();
        // mock the RM of XA
        DefaultResourceManager.mockResourceManager(BranchType.XA, new ResourceManagerXA() {
            @Override
            public void registerResource(Resource resource) {
                dataSourceCache.put(resource.getResourceId(), resource);
            }

            @Override
            public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid,
                                       String applicationData, String lockKeys) throws TransactionException {
                return mockBranchId;
            }

            @Override
            public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status,
                                     String applicationData) throws TransactionException {

            }
        });

    }

    @Test
    @Disabled
    public void testAllInOne() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();
        doTestXAModeNormalCaseAllInOne(mockXid, mockBranchId);
    }

    @Test
    @Disabled
    public void testGlobalCommitOnDifferentDataSource() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();
        doTestXAModeNormalCasePhase1(mockXid, mockBranchId);
        // Use new DataSource in phase 2
        doTestXAModeNormalCasePhase2(true, mockXid, mockBranchId);
    }

    @Test
    @Disabled
    public void testGlobalRollbackOnDifferentDataSource() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();
        doTestXAModeNormalCasePhase1(mockXid, mockBranchId);
        // Use new DataSource in phase 2
        doTestXAModeNormalCasePhase2(false, mockXid, mockBranchId);
    }

    @Test
    @Disabled
    public void testOnlyPhase1() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();
        doTestXAModeNormalCasePhase1(mockXid, mockBranchId);
    }

    @Test
    @Disabled
    public void testOnlyPhase2Commit() throws Throwable {
        doTestXAModeNormalCasePhase2(true, mockXid, mockBranchId);
    }

    @Test
    @Disabled
    public void testOnlyPhase2Rollback() throws Throwable {
        doTestXAModeNormalCasePhase2(false, mockXid, mockBranchId);
    }

    private void doTestXAModeNormalPrepareData() throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        // prepare data for test: make sure no test record there
        Connection helperConn = helperDS.getConnection();
        Statement helperStat = helperConn.createStatement();
        ResultSet helperRes = null;
        helperStat.execute("delete from test where id = " + testRecordId);
        helperStat.close();
        helperConn.close();

    }

    private void doTestXAModeNormalCasePhase2(boolean globalCommit, String mockXid, Long mockBranchId) throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet helperRes = null;

        // init RM
        initRM();

        AbstractDataSourceProxyXA dataSourceProxyXA = null;
        if (nativeXA) {
            // init XADataSource runnerXA
            XADataSource runnerXADS = createNewNativeXADataSource();
            dataSourceProxyXA = new DataSourceProxyXANative(runnerXADS);
        } else {
            // init DataSource: runner
            DruidDataSource runnerDS = createNewDruidDataSource();
            dataSourceProxyXA = new DataSourceProxyXA(runnerDS);
        }

        // Global Tx Phase 2:
        if (globalCommit) {
            DefaultResourceManager.get().branchCommit(dataSourceProxyXA.getBranchType(), mockXid, mockBranchId,
                dataSourceProxyXA.getResourceId(), null);

            // have a check
            helperConn = helperDS.getConnection();
            helperStat = helperConn.createStatement();
            helperRes = helperStat.executeQuery("select * from test where id = " + testRecordId);
            // should see the test record now
            Assertions.assertTrue(helperRes.next());
            Assertions.assertEquals(helperRes.getInt(1), testRecordId);
            Assertions.assertEquals(helperRes.getString(2), testRecordName);
            helperRes.close();
            helperStat.close();
            helperConn.close();

        } else {
            DefaultResourceManager.get().branchRollback(dataSourceProxyXA.getBranchType(), mockXid, mockBranchId,
                dataSourceProxyXA.getResourceId(), null);

            // have a check
            helperConn = helperDS.getConnection();
            helperStat = helperConn.createStatement();
            helperRes = helperStat.executeQuery("select * from test where id = " + testRecordId);
            // should NOT see the test record now
            Assertions.assertFalse(helperRes.next());
            helperRes.close();
            helperStat.close();
            helperConn.close();

        }
        System.out.println("Phase2 looks good!");
    }

    private void doTestXAModeNormalCasePhase1(String mockXid, Long mockBranchId) throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet helperRes = null;

        // init RM
        initRM();

        AbstractDataSourceProxyXA dataSourceProxyXA = null;
        if (nativeXA) {
            // init XADataSource runnerXA
            XADataSource runnerXADS = createNewNativeXADataSource();
            dataSourceProxyXA = new DataSourceProxyXANative(runnerXADS);
        } else {
            // init DataSource: runner
            DruidDataSource runnerDS = createNewDruidDataSource();
            dataSourceProxyXA = new DataSourceProxyXA(runnerDS);
        }

        // Global Tx Phase 1:
        RootContext.bind(mockXid);
        Connection testConn = dataSourceProxyXA.getConnection();
        Statement testStat = testConn.createStatement();
        // >>> insert the test record with XA mode
        testStat.execute("insert into test(id, name) values(" + testRecordId + ", '" + testRecordName + "')");
        // >>> close the statement and connection
        testStat.close();
        testConn.close();
        RootContext.unbind();

        // have a check
        helperConn = helperDS.getConnection();
        helperStat = helperConn.createStatement();
        helperRes = helperStat.executeQuery("select * from test where id = " + testRecordId);
        // should NOT see the record(id=888) now
        Assertions.assertFalse(helperRes.next());
        helperRes.close();
        helperStat.close();
        helperConn.close();

        if (JdbcConstants.MYSQL.equalsIgnoreCase(dbType)) {
            XAXid xaXid = XAXidBuilder.build(mockXid, mockBranchId);
            dataSourceProxyXA.forceClosePhysicalConnection(xaXid);
        }

        System.out.println("Phase1 looks good!");
    }

    private void doTestXAModeNormalCaseAllInOne(String mockXid, Long mockBranchId) throws Throwable {
        // init DataSource: helper
        DruidDataSource helperDS = createNewDruidDataSource();

        Connection helperConn = null;
        Statement helperStat = null;
        ResultSet helperRes = null;

        // init RM
        initRM();

        AbstractDataSourceProxyXA dataSourceProxyXA = null;
        if (nativeXA) {
            // init XADataSource runnerXA
            XADataSource runnerXADS = createNewNativeXADataSource();
            dataSourceProxyXA = new DataSourceProxyXANative(runnerXADS);
        } else {
            // init DataSource: runner
            DruidDataSource runnerDS = createNewDruidDataSource();
            dataSourceProxyXA = new DataSourceProxyXA(runnerDS);
        }

        // Global Tx Phase 1:
        RootContext.bind(mockXid);
        Connection testConn = dataSourceProxyXA.getConnection();
        Statement testStat = testConn.createStatement();
        // >>> insert the test record with XA mode
        testStat.execute("insert into test(id, name) values(" + testRecordId + ", '" + testRecordName + "')");
        // >>> close the statement and connection
        testStat.close();
        testConn.close();
        RootContext.unbind();

        // have a check
        helperConn = helperDS.getConnection();
        helperStat = helperConn.createStatement();
        helperRes = helperStat.executeQuery("select * from test where id = " + testRecordId);
        // should NOT see the record(id=888) now
        Assertions.assertFalse(helperRes.next());
        helperRes.close();
        helperStat.close();
        helperConn.close();

        // Global Tx Phase 2: run phase 2 with the same runner DS
        DefaultResourceManager.get().branchCommit(dataSourceProxyXA.getBranchType(), mockXid, mockBranchId,
            dataSourceProxyXA.getResourceId(), null);

        // have a check
        helperConn = helperDS.getConnection();
        helperStat = helperConn.createStatement();
        helperRes = helperStat.executeQuery("select * from test where id = " + testRecordId);
        // should see the test record now
        Assertions.assertTrue(helperRes.next());
        Assertions.assertEquals(helperRes.getInt(1), testRecordId);
        Assertions.assertEquals(helperRes.getString(2), testRecordName);
        helperRes.close();
        helperStat.close();
        helperConn.close();

        System.out.println("All in one looks good!");
    }

    @Test
    @Disabled
    public void testXid() throws Throwable {
        XAXid xaXid = XAXidBuilder.build(mockXid, mockBranchId);

        XAXid retrievedXAXid = XAXidBuilder.build(xaXid.getGlobalTransactionId(), xaXid.getBranchQualifier());
        String retrievedXid = retrievedXAXid.getGlobalXid();
        long retrievedBranchId = retrievedXAXid.getBranchId();

        Assertions.assertEquals(mockXid, retrievedXid);
        Assertions.assertEquals(mockBranchId, retrievedBranchId);

    }

    @Test
    @Disabled
    public void testCleanXARecover() throws Throwable {
        XADataSource xaDataSource = createNewNativeXADataSource();

        XAConnection xaConnection = xaDataSource.getXAConnection();
        XAResource xaResource = xaConnection.getXAResource();

        Xid[] xids = xaResource.recover(XAResource.TMSTARTRSCAN|XAResource.TMENDRSCAN);
        for (Xid xid : xids) {
            try {
                xaResource.rollback(xid);
            } catch (XAException xae) {
                xae.printStackTrace();
            }
        }
        System.out.println("Unfinished XA branches are ALL cleaned!");

    }

    @Test
    @Disabled
    public void testXADataSourceNative() throws Throwable {
        XADataSource nativeXADataSource = createOracleXADataSource();

        XAConnection xaConnection = nativeXADataSource.getXAConnection();
        XAResource xaResource = xaConnection.getXAResource();
        Xid xid = XAXidBuilder.build("127.0.0.1:8091:1234", 1235L);
        xaResource.start(xid, XAResource.TMNOFLAGS);
    }

    @Test
    @Disabled
    public void testXADataSourceNormal() throws Throwable {
        DruidXADataSource druidDataSource = new DruidXADataSource();
        druidDataSource.setUrl(oracle_jdbcUrl);
        druidDataSource.setUsername(oracle_username);
        druidDataSource.setPassword(oracle_password);
        druidDataSource.setDriverClassName(oracle_driverClassName);

        XAConnection xaConnection = druidDataSource.getXAConnection();
        XAResource xaResource = xaConnection.getXAResource();
        Xid xid = XAXidBuilder.build("127.0.0.1:8091:1234", 1235L);
        // Since issue of Druid(https://github.com/alibaba/druid/issues/3707), XA start will fail.
        xaResource.start(xid, XAResource.TMNOFLAGS);
    }

    @Test
    @Disabled
    // Should RUN with local Seata Server
    public void testStandardAppGlobalCommit() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();

        // Create a standard proxy according to non-XA data source
        DataSource ds = createDataSourceProxyXA();
        // Create a global tx
        GlobalTransaction gtx = createGlobalTransaction();

        gtx.begin();
        runInGlobalTx(ds);
        gtx.commit();

        Thread.sleep(5000);
    }

    @Test
    @Disabled
    // Should RUN with local Seata Server
    public void testXANativeAppGlobalCommit() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();

        // Create a native proxy according to XA data source
        DataSource ds = createDataSourceProxyXANative();
        // Create a global tx
        GlobalTransaction gtx = createGlobalTransaction();

        gtx.begin();
        runInGlobalTx(ds);
        gtx.commit();

        Thread.sleep(5000);
    }

    @Test
    @Disabled
    // Should RUN with local Seata Server
    public void testStandardAppGlobalRollback() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();

        // Create a standard proxy according to non-XA data source
        DataSource ds = createDataSourceProxyXA();
        // Create a global tx
        GlobalTransaction gtx = createGlobalTransaction();

        gtx.begin();
        runInGlobalTx(ds);
        gtx.rollback();

        Thread.sleep(5000);
    }

    @Test
    @Disabled
    // Should RUN with local Seata Server
    public void testXANativeAppGlobalRollback() throws Throwable {
        testCleanXARecover();
        doTestXAModeNormalPrepareData();

        // Create a native proxy according to XA data source
        DataSource ds = createDataSourceProxyXANative();
        // Create a global tx
        GlobalTransaction gtx = createGlobalTransaction();

        gtx.begin();
        runInGlobalTx(ds);
        gtx.rollback();

        Thread.sleep(5000);
    }

    private void runInGlobalTx(DataSource ds) throws SQLException {
        System.out.println(RootContext.getXID());

        Connection testConn = ds.getConnection();
        Statement testStat = testConn.createStatement();
        // >>> insert the test record with XA mode
        testStat.execute("insert into test(id, name) values(" + testRecordId + ", '" + testRecordName + "')");
        // >>> close the statement and connection
        testStat.close();
        testConn.close();

    }

    private DataSourceProxyXANative createDataSourceProxyXANative() throws Throwable {
        XADataSource originalDS = createNewDruidXADataSource();
        DataSourceProxyXANative dataSourceProxyXA = new DataSourceProxyXANative(originalDS);
        return dataSourceProxyXA;
    }

    private DataSourceProxyXA createDataSourceProxyXA() throws Throwable {
        DataSource originalDS = createNewDruidDataSource();
        DataSourceProxyXA dataSourceProxyXA = new DataSourceProxyXA(originalDS);
        return dataSourceProxyXA;
    }

    private GlobalTransaction createGlobalTransaction() {
        String vgroup = "my_test_tx_group";
        GlobalTransactionScanner scanner = new GlobalTransactionScanner(vgroup);
        scanner.afterPropertiesSet();

        GlobalTransaction gtx = GlobalTransactionContext.getCurrentOrCreate();
        return gtx;
    }

}
