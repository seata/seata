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
package io.seata.server.session.db;

import io.seata.common.XID;
import io.seata.common.exception.StoreException;
import io.seata.common.util.IOUtil;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.db.LogStoreDataBaseDAO;
import io.seata.server.UUIDGenerator;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.session.SessionManager;
import io.seata.server.store.db.DatabaseTransactionStoreManager;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The type Data base session manager test.
 *
 * @author zhangsen
 */
public class DataBaseSessionManagerTest {

    static SessionManager sessionManager = null;

    static LogStoreDataBaseDAO logStoreDataBaseDAO  = null;

    static BasicDataSource dataSource = null;

    @BeforeAll
    public static void start() throws Exception {
        DataBaseSessionManager tempSessionManager = new DataBaseSessionManager();
        DatabaseTransactionStoreManager transactionStoreManager = new DatabaseTransactionStoreManager();

        dataSource =  new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:./db_store/db_session");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        logStoreDataBaseDAO = new LogStoreDataBaseDAO(dataSource);
        logStoreDataBaseDAO.setDbType("h2");
        logStoreDataBaseDAO.setGlobalTable("global_table");
        logStoreDataBaseDAO.setBrachTable("branch_table");

        transactionStoreManager.setLogQueryLimit(100);
        transactionStoreManager.setLogStore(logStoreDataBaseDAO);

        tempSessionManager.setTransactionStoreManager(transactionStoreManager);
        sessionManager = tempSessionManager;

        prepareTable(dataSource);

        logStoreDataBaseDAO.initTransactionNameSize();
    }

    private static void prepareTable(BasicDataSource dataSource) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            Statement s = conn.createStatement();
            try {
                s.execute("drop table global_table");
            } catch (Exception e) {
            }
            s.execute("CREATE TABLE global_table ( xid varchar(96),  transaction_id long , STATUS int,  application_id varchar(32), transaction_service_group varchar(32) ,transaction_name varchar(128) ,timeout int,  begin_time long, application_data varchar(500), gmt_create TIMESTAMP(6) ,gmt_modified TIMESTAMP(6), version int ) ");

            System.out.println("create table global_table success.");

            try {
                s.execute("drop table branch_table");
            } catch (Exception e) {
            }
            s.execute("CREATE TABLE branch_table ( xid varchar(96),  transaction_id long , branch_id long, resource_group_id varchar(32), resource_id varchar(32) ,lock_key varchar(64) ,branch_type varchar(32) ,  status int , client_id varchar(128),  application_data varchar(500),  gmt_create TIMESTAMP(6) ,gmt_modified TIMESTAMP(6) ) ");
            System.out.println("create table branch_table success.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(conn);
        }
    }


    @Test
    public void test_addGlobalSession() throws TransactionException, SQLException {
        GlobalSession session = GlobalSession.createGlobalSession("test",
                "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);

        sessionManager.addGlobalSession(session);

        String sql = "select * from global_table where xid= '"+xid+"'";
        String delSql = "delete from global_table where xid= '"+xid+"'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
            }else{
                Assertions.assertTrue(false);
            }

            conn.createStatement().execute(delSql);
        }finally {
            if(conn != null){
                conn.close();
            }
        }
    }


    @Test
    public void test_updateGlobalSessionStatus() throws TransactionException, SQLException {
        GlobalSession session = GlobalSession.createGlobalSession("test",
                "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);

        sessionManager.addGlobalSession(session);

        session.setStatus(GlobalStatus.Committing);
        sessionManager.updateGlobalSessionStatus(session, GlobalStatus.Committing);

        String sql = "select * from global_table where xid= '"+xid+"'";
        String delSql = "delete from global_table where xid= '"+xid+"'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
                Assertions.assertEquals(rs.getInt("status"), GlobalStatus.Committing.getCode());
            }else{
                Assertions.assertTrue(false);
            }

            conn.createStatement().execute(delSql);
        }finally {
            if(conn != null){
                conn.close();
            }
        }
    }

    @Test
    public void test_removeGlobalSession() throws Exception {
        GlobalSession session = GlobalSession.createGlobalSession("test",
                "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);

        sessionManager.addGlobalSession(session);

        String sql = "select * from global_table where xid= '"+xid+"'";
        String delSql = "delete from global_table where xid= '"+xid+"'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
            }else{
                Assertions.assertTrue(false);
            }
            rs.close();

            //delete
            sessionManager.removeGlobalSession(session);

            rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(false);
            }else{
                Assertions.assertTrue(true);
            }
            rs.close();

            conn.createStatement().execute(delSql);
        }finally {
            if(conn != null){
                conn.close();
            }
        }
    }

    @Test
    public void test_findGlobalSession() throws Exception {
        GlobalSession session = GlobalSession.createGlobalSession("test",
                "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);

        sessionManager.addGlobalSession(session);

        GlobalSession globalSession_db = sessionManager.findGlobalSession(session.getXid());
        Assertions.assertNotNull(globalSession_db);

        Assertions.assertEquals(globalSession_db.getTransactionId(), session.getTransactionId());
        Assertions.assertEquals(globalSession_db.getXid(), session.getXid());
        Assertions.assertEquals(globalSession_db.getApplicationData(), session.getApplicationData());
        Assertions.assertEquals(globalSession_db.getApplicationId(), session.getApplicationId());
        Assertions.assertEquals(globalSession_db.getTransactionName(), session.getTransactionName());
        Assertions.assertEquals(globalSession_db.getTransactionServiceGroup(), session.getTransactionServiceGroup());
        Assertions.assertEquals(globalSession_db.getBeginTime(), session.getBeginTime());
        Assertions.assertEquals(globalSession_db.getTimeout(), session.getTimeout());
        Assertions.assertEquals(globalSession_db.getStatus(), session.getStatus());

        String delSql = "delete from global_table where xid= '"+xid+"'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            conn.createStatement().execute(delSql);
        }finally {
            if(conn != null){
                conn.close();
            }
        }
    }


    @Test
    public void test_addBranchSession() throws Exception {
        GlobalSession globalSession = GlobalSession.createGlobalSession("test",
                "test", "test123", 100);
        String xid = XID.generateXID(globalSession.getTransactionId());
        globalSession.setXid(xid);
        globalSession.setTransactionId(146757978);
        globalSession.setBeginTime(System.currentTimeMillis());
        globalSession.setApplicationData("abc=878s");
        globalSession.setStatus(GlobalStatus.Begin);

        BranchSession branchSession = new BranchSession();
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setXid(xid);
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");

        sessionManager.addBranchSession(globalSession, branchSession);

        String sql = "select * from branch_table where xid= '"+xid+"'";
        String delSql = "delete from branch_table where xid= '"+xid+"'" + ";" + "delete from global_table where xid= '"+xid+"'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
            }else{
                Assertions.assertTrue(false);
            }

            conn.createStatement().execute(delSql);
        }finally {
            if(conn != null){
                conn.close();
            }
        }
    }


    @Test
    public void test_updateBranchSessionStatus() throws Exception {
        GlobalSession globalSession = GlobalSession.createGlobalSession("test",
                "test", "test123", 100);
        String xid = XID.generateXID(globalSession.getTransactionId());
        globalSession.setXid(xid);
        globalSession.setTransactionId(146757978);
        globalSession.setBeginTime(System.currentTimeMillis());
        globalSession.setApplicationData("abc=878s");
        globalSession.setStatus(GlobalStatus.Begin);

        BranchSession branchSession = new BranchSession();
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setXid(xid);
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setStatus(BranchStatus.PhaseOne_Done);

        sessionManager.addBranchSession(globalSession, branchSession);

        branchSession.setStatus(BranchStatus.PhaseOne_Timeout);
        sessionManager.updateBranchSessionStatus(branchSession, BranchStatus.PhaseOne_Timeout);

        String sql = "select * from branch_table where xid= '"+xid+"'";
        String delSql = "delete from branch_table where xid= '"+xid+"'" + ";" + "delete from global_table where xid= '"+xid+"'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
                Assertions.assertEquals(rs.getInt("status"), BranchStatus.PhaseOne_Timeout.getCode());
            }else{
                Assertions.assertTrue(false);
            }

            conn.createStatement().execute(delSql);
        }finally {
            if(conn != null){
                conn.close();
            }
        }
    }

    @Test
    public void test_removeBranchSession() throws Exception {
        GlobalSession globalSession = GlobalSession.createGlobalSession("test",
                "test", "test123", 100);
        String xid = XID.generateXID(globalSession.getTransactionId());
        globalSession.setXid(xid);
        globalSession.setTransactionId(146757978);
        globalSession.setBeginTime(System.currentTimeMillis());
        globalSession.setApplicationData("abc=878s");
        globalSession.setStatus(GlobalStatus.Begin);

        BranchSession branchSession = new BranchSession();
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setXid(xid);
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setStatus(BranchStatus.PhaseOne_Done);

        sessionManager.addBranchSession(globalSession, branchSession);

        sessionManager.removeBranchSession(globalSession, branchSession);

        String sql = "select * from branch_table where xid= '"+xid+"'";
        String delSql = "delete from branch_table where xid= '"+xid+"'" + ";" + "delete from global_table where xid= '"+xid+"'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(false);
            }else{
                Assertions.assertTrue(true);
            }

            conn.createStatement().execute(delSql);
        }finally {
            if(conn != null){
                conn.close();
            }
        }
    }


    @Test
    public void test_allSessions() throws Exception {
        GlobalSession globalSession = GlobalSession.createGlobalSession("test",
                "test", "test123", 100);
        String xid = XID.generateXID(globalSession.getTransactionId());
        globalSession.setXid(xid);
        globalSession.setTransactionId(146757978);
        globalSession.setBeginTime(System.currentTimeMillis());
        globalSession.setApplicationData("abc=878s");
        globalSession.setStatus(GlobalStatus.Begin);

        sessionManager.addGlobalSession(globalSession);

        BranchSession branchSession = new BranchSession();
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setXid(xid);
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setClientId("abc-123");
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setStatus(BranchStatus.PhaseOne_Done);

        sessionManager.addBranchSession(globalSession, branchSession);


        BranchSession branchSession2 = new BranchSession();
        branchSession2.setBranchId(UUIDGenerator.generateUUID());
        branchSession2.setXid(xid);
        branchSession2.setTransactionId(globalSession.getTransactionId());
        branchSession2.setBranchId(2L);
        branchSession2.setResourceGroupId("my_test_tx_group");
        branchSession2.setResourceId("tb_1");
        branchSession2.setLockKey("t_1");
        branchSession2.setBranchType(BranchType.TCC);
        branchSession2.setClientId("abc-123");
        branchSession2.setApplicationData("{\"data\":\"test\"}");
        branchSession2.setStatus(BranchStatus.PhaseOne_Done);

        sessionManager.addBranchSession(globalSession, branchSession2);

        Collection<GlobalSession> rets = sessionManager.allSessions();
        Assertions.assertNotNull(rets);
        Assertions.assertEquals(1, rets.size());

        GlobalSession globalSession_db = (io.seata.server.session.GlobalSession) new ArrayList(rets).get(0);

        Assertions.assertNotNull(globalSession_db.getReverseSortedBranches());
        Assertions.assertEquals(2, globalSession_db.getReverseSortedBranches().size());

        Assertions.assertNotNull(globalSession_db.getBranch(1L));
        Assertions.assertNotNull(globalSession_db.getBranch(2L));

        String delSql = "delete from branch_table where xid= '"+xid+"'" + ";" + "delete from global_table where xid= '"+xid+"'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            conn.createStatement().execute(delSql);
        }finally {
            if(conn != null){
                conn.close();
            }
        }
    }

    @Test
    public void test_findGlobalSessions() throws TransactionException, SQLException {
        String xid = null;
        {
            GlobalSession globalSession = GlobalSession.createGlobalSession("test",
                    "test", "test123", 100);
            xid = XID.generateXID(globalSession.getTransactionId());
            globalSession.setXid(xid);
            globalSession.setTransactionId(146757978);
            globalSession.setBeginTime(System.currentTimeMillis());
            globalSession.setApplicationData("abc=878s");
            globalSession.setStatus(GlobalStatus.Begin);

            sessionManager.addGlobalSession(globalSession);

            BranchSession branchSession = new BranchSession();
            branchSession.setBranchId(UUIDGenerator.generateUUID());
            branchSession.setXid(xid);
            branchSession.setTransactionId(globalSession.getTransactionId());
            branchSession.setBranchId(1L);
            branchSession.setResourceGroupId("my_test_tx_group");
            branchSession.setResourceId("tb_1");
            branchSession.setLockKey("t_1");
            branchSession.setBranchType(BranchType.AT);
            branchSession.setClientId("abc-123");
            branchSession.setApplicationData("{\"data\":\"test\"}");
            branchSession.setStatus(BranchStatus.PhaseOne_Done);
            sessionManager.addBranchSession(globalSession, branchSession);
        }
        String xid2 = null;
        {
            GlobalSession globalSession = GlobalSession.createGlobalSession("test",
                    "test", "test123", 100);
            xid2 = XID.generateXID(globalSession.getTransactionId());
            globalSession.setXid(xid);
            globalSession.setTransactionId(146757978);
            globalSession.setBeginTime(System.currentTimeMillis());
            globalSession.setApplicationData("abc=878s");
            globalSession.setStatus(GlobalStatus.CommitRetrying);

            sessionManager.addGlobalSession(globalSession);

            BranchSession branchSession = new BranchSession();
            branchSession.setBranchId(UUIDGenerator.generateUUID());
            branchSession.setXid(xid2);
            branchSession.setTransactionId(globalSession.getTransactionId());
            branchSession.setBranchId(1L);
            branchSession.setResourceGroupId("my_test_tx_group");
            branchSession.setResourceId("tb_1");
            branchSession.setLockKey("t_1");
            branchSession.setBranchType(BranchType.AT);
            branchSession.setClientId("abc-123");
            branchSession.setApplicationData("{\"data\":\"test\"}");
            branchSession.setStatus(BranchStatus.PhaseOne_Done);
            sessionManager.addBranchSession(globalSession, branchSession);
        }


        Collection<GlobalSession> rets = sessionManager.findGlobalSessions(new SessionCondition( GlobalStatus.Begin));
        Assertions.assertNotNull(rets);
        Assertions.assertEquals(1, rets.size());

        GlobalSession globalSession_db = (io.seata.server.session.GlobalSession) new ArrayList(rets).get(0);

        Assertions.assertNotNull(globalSession_db.getReverseSortedBranches());
        Assertions.assertEquals(1, globalSession_db.getReverseSortedBranches().size());

        Assertions.assertNotNull(globalSession_db.getBranch(1L));

        String delSql = "delete from branch_table where xid= '"+xid+"'" + ";" + "delete from global_table where xid= '"+xid+"'";
        String delSql2 = "delete from branch_table where xid= '"+xid2+"'" + ";" + "delete from global_table where xid= '"+xid2+"'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            conn.createStatement().execute(delSql);
            conn.createStatement().execute(delSql2);
        }finally {
            if(conn != null){
                conn.close();
            }
        }
    }

    @Test
    public void test_transactionNameGreaterDbSize() throws Exception {

        int transactionNameColumnSize = logStoreDataBaseDAO.getTransactionNameColumnSize();
        StringBuilder sb = new StringBuilder("test");
        for (int i = 4; i < transactionNameColumnSize; i++) {
            sb.append("0");
        }
        final String finalTxName = sb.toString();
        sb.append("1321465454545436");

        GlobalSession session = GlobalSession.createGlobalSession("test",
                "test", sb.toString(), 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);

        sessionManager.addGlobalSession(session);

        GlobalSession globalSession_db = sessionManager.findGlobalSession(session.getXid());
        Assertions.assertNotNull(globalSession_db);

        Assertions.assertEquals(globalSession_db.getTransactionName(), finalTxName);

        String delSql = "delete from global_table where xid= '"+xid+"'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            conn.createStatement().execute(delSql);
        }finally {
            if(conn != null){
                conn.close();
            }
        }
    }

    @Test
    public void test_updateGlobalSessionStatusWithOptimisticLocking() throws TransactionException, SQLException {
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);

        sessionManager.addGlobalSession(session);

        GlobalSession sessionShadow = sessionManager.findGlobalSession(xid);
        Assertions.assertNotNull(sessionShadow);

        sessionManager.updateGlobalSessionStatus(session, GlobalStatus.Committing);
        Assertions.assertEquals(GlobalStatus.Committing, session.getStatus());
        Assertions.assertEquals(1, session.getVersion());

        boolean shadowCommittedFailed = false;
        try {
            sessionManager.updateGlobalSessionStatus(sessionShadow, GlobalStatus.Committed);
        } catch(StoreException e) {
            Assertions.assertEquals("GlobalSession was updated or deleted by another transaction", e.getMessage());
            shadowCommittedFailed = true;
        }
        Assertions.assertTrue(shadowCommittedFailed);

        sessionManager.updateGlobalSessionStatus(session, GlobalStatus.Committed);
        Assertions.assertEquals(2, session.getVersion());

        session = sessionManager.findGlobalSession(xid);
        Assertions.assertNotNull(session);
        Assertions.assertEquals(2, session.getVersion());

        String sql = "select * from global_table where xid= '"+xid+"'";
        String delSql = "delete from global_table where xid= '"+xid+"'";
        try(Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
                Assertions.assertEquals(rs.getInt("status"), GlobalStatus.Committed.getCode());
                Assertions.assertEquals(rs.getInt("version"), 2);
            }else{
                Assertions.assertTrue(false);
            }
            conn.createStatement().execute(delSql);
        }
    }

}
