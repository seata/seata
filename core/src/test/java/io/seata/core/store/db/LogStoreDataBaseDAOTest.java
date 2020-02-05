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
package io.seata.core.store.db;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import org.apache.commons.dbcp.BasicDataSource;

import org.h2.store.fs.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;


/**
 * @author zhangsen
 */
public class LogStoreDataBaseDAOTest {

    static LogStoreDataBaseDAO logStoreDataBaseDAO  = null;

    static BasicDataSource dataSource = null;

    @BeforeAll
    public static void start(){
        dataSource =  new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:./db_store/log");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        logStoreDataBaseDAO = new LogStoreDataBaseDAO(dataSource);
        logStoreDataBaseDAO.setDbType("h2");
        logStoreDataBaseDAO.setGlobalTable("global_table");
        logStoreDataBaseDAO.setBrachTable("branch_table");

        prepareTable(dataSource);
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
//            xid, transaction_id, status, application_id, transaction_service_group, transaction_name, timeout, begin_time, application_data, gmt_create, gmt_modified
            s.execute("CREATE TABLE global_table ( xid varchar(96) primary key,  transaction_id long , STATUS int,  application_id varchar(32), transaction_service_group varchar(32) ,transaction_name varchar(32) ,timeout int,  begin_time long, application_data varchar(500), gmt_create TIMESTAMP(6) ,gmt_modified TIMESTAMP(6), version int ) ");
            System.out.println("create table global_table success.");

            try {
                s.execute("drop table branch_table");
            } catch (Exception e) {
            }
//            xid, transaction_id, branch_id, resource_group_id, resource_id, lock_key, branch_type, status, client_id, application_data, gmt_create, gmt_modified
            s.execute("CREATE TABLE branch_table ( xid varchar(96),  transaction_id long , branch_id long primary key, resource_group_id varchar(32), resource_id varchar(32) ,lock_key varchar(64) ,branch_type varchar(32) ,  status int , client_id varchar(128),  application_data varchar(500),  gmt_create TIMESTAMP(6) ,gmt_modified TIMESTAMP(6) ) ");
            System.out.println("create table branch_table success.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(conn);
        }
    }

    @Test
    public void queryGlobalTransactionDO_by_xid() throws SQLException {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid("abc-123:978786");
        globalTransactionDO.setApplicationData("abc=87867978");
        globalTransactionDO.setTransactionServiceGroup("abc");
        globalTransactionDO.setTransactionName("test");
        globalTransactionDO.setTransactionId(143546567);
        globalTransactionDO.setTimeout(20);
        globalTransactionDO.setBeginTime(System.currentTimeMillis());
        globalTransactionDO.setApplicationId("test");
        globalTransactionDO.setStatus(1);

        boolean ret = logStoreDataBaseDAO.insertGlobalTransactionDO(globalTransactionDO);
        Assertions.assertTrue(ret);


        GlobalTransactionDO globalTransactionDO_db = logStoreDataBaseDAO.queryGlobalTransactionDO("abc-123:978786");
        Assertions.assertNotNull(globalTransactionDO_db);

        Assertions.assertEquals(globalTransactionDO_db.getBeginTime(), globalTransactionDO_db.getBeginTime());
        Assertions.assertEquals(globalTransactionDO_db.getTransactionName(), globalTransactionDO_db.getTransactionName());
        Assertions.assertEquals(globalTransactionDO_db.getTransactionId(), globalTransactionDO_db.getTransactionId());
        Assertions.assertEquals(globalTransactionDO_db.getStatus(), globalTransactionDO_db.getStatus());
        Assertions.assertEquals(globalTransactionDO_db.getTimeout(), globalTransactionDO_db.getTimeout());
        Assertions.assertEquals(globalTransactionDO_db.getTransactionServiceGroup(), globalTransactionDO_db.getTransactionServiceGroup());
        Assertions.assertEquals(globalTransactionDO_db.getApplicationId(), globalTransactionDO_db.getApplicationId());
        Assertions.assertNotNull(globalTransactionDO_db.getGmtCreate());
        Assertions.assertNotNull(globalTransactionDO_db.getGmtModified());


        String delSql = "delete from global_table where xid= 'abc-123:978786'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            //delete
            conn.createStatement().execute(delSql);
        }finally {
            IOUtil.close(conn);
        }

    }

    @Test
    public void queryGlobalTransactionDO_by_transaction_id() throws SQLException {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid("abc-123:676787978");
        globalTransactionDO.setApplicationData("abc=234356");
        globalTransactionDO.setTransactionServiceGroup("abc");
        globalTransactionDO.setTransactionName("test");
        globalTransactionDO.setTransactionId(867978970);
        globalTransactionDO.setTimeout(20);
        globalTransactionDO.setBeginTime(System.currentTimeMillis());
        globalTransactionDO.setApplicationId("test");
        globalTransactionDO.setStatus(1);

        boolean ret = logStoreDataBaseDAO.insertGlobalTransactionDO(globalTransactionDO);
        Assertions.assertTrue(ret);

        GlobalTransactionDO globalTransactionDO_db = logStoreDataBaseDAO.queryGlobalTransactionDO(867978970L);
        Assertions.assertNotNull(globalTransactionDO_db);

        Assertions.assertEquals(globalTransactionDO_db.getXid(), globalTransactionDO_db.getXid());
        Assertions.assertEquals(globalTransactionDO_db.getBeginTime(), globalTransactionDO_db.getBeginTime());
        Assertions.assertEquals(globalTransactionDO_db.getTransactionName(), globalTransactionDO_db.getTransactionName());
        Assertions.assertEquals(globalTransactionDO_db.getTransactionId(), globalTransactionDO_db.getTransactionId());
        Assertions.assertEquals(globalTransactionDO_db.getStatus(), globalTransactionDO_db.getStatus());
        Assertions.assertEquals(globalTransactionDO_db.getTimeout(), globalTransactionDO_db.getTimeout());
        Assertions.assertEquals(globalTransactionDO_db.getTransactionServiceGroup(), globalTransactionDO_db.getTransactionServiceGroup());
        Assertions.assertEquals(globalTransactionDO_db.getApplicationId(), globalTransactionDO_db.getApplicationId());
        Assertions.assertNotNull(globalTransactionDO_db.getGmtCreate());
        Assertions.assertNotNull(globalTransactionDO_db.getGmtModified());

        String delSql = "delete from global_table where xid= 'abc-123:978786'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            //delete
            conn.createStatement().execute(delSql);
        }finally {
            IOUtil.close(conn);
        }

    }

    @Test
    public void queryGlobalTransactionDO_by_statuses() throws SQLException {
        {
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
            globalTransactionDO.setXid("abc-123:1267");
            globalTransactionDO.setApplicationData("abc=234356");
            globalTransactionDO.setTransactionServiceGroup("abc");
            globalTransactionDO.setTransactionName("test");
            globalTransactionDO.setTransactionId(867978970);
            globalTransactionDO.setTimeout(20);
            globalTransactionDO.setBeginTime(System.currentTimeMillis());
            globalTransactionDO.setApplicationId("test");
            globalTransactionDO.setStatus(1);

            Assertions.assertTrue(logStoreDataBaseDAO.insertGlobalTransactionDO(globalTransactionDO));
        }
        {
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
            globalTransactionDO.setXid("abc-123:6978");
            globalTransactionDO.setApplicationData("abc=87867978");
            globalTransactionDO.setTransactionServiceGroup("abc");
            globalTransactionDO.setTransactionName("test");
            globalTransactionDO.setTransactionId(143546567);
            globalTransactionDO.setTimeout(20);
            globalTransactionDO.setBeginTime(System.currentTimeMillis());
            globalTransactionDO.setApplicationId("test");
            globalTransactionDO.setStatus(2);

            boolean ret = logStoreDataBaseDAO.insertGlobalTransactionDO(globalTransactionDO);
            Assertions.assertTrue(ret);
        }
        {
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
            globalTransactionDO.setXid("abc-123:5657");
            globalTransactionDO.setApplicationData("abc=5454");
            globalTransactionDO.setTransactionServiceGroup("abc");
            globalTransactionDO.setTransactionName("test");
            globalTransactionDO.setTransactionId(12345);
            globalTransactionDO.setTimeout(20);
            globalTransactionDO.setBeginTime(System.currentTimeMillis());
            globalTransactionDO.setApplicationId("test");
            globalTransactionDO.setStatus(1);

            boolean ret = logStoreDataBaseDAO.insertGlobalTransactionDO(globalTransactionDO);
            Assertions.assertTrue(ret);
        }

        List<GlobalTransactionDO> globalTransactionDOs = logStoreDataBaseDAO.queryGlobalTransactionDO(new int[]{1}, 10);
        Assertions.assertNotNull(globalTransactionDOs);
        Assertions.assertEquals(2, globalTransactionDOs.size());

        if("abc-123:5657".equals(globalTransactionDOs.get(0).getXid()) && "abc-123:1267".equals(globalTransactionDOs.get(1).getXid())){
            Assertions.assertTrue(true);
        }else if("abc-123:5657".equals(globalTransactionDOs.get(1).getXid()) && "abc-123:1267".equals(globalTransactionDOs.get(0).getXid())){
            Assertions.assertTrue(true);
        }else {
            Assertions.assertTrue(false);
        }

        String delSql = "delete from global_table where xid in ('abc-123:1267', 'abc-123:6978', 'abc-123:5657')";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            conn.createStatement().execute(delSql);
        }finally {
            IOUtil.close(conn);
        }
    }

    @Test
    public void queryGlobalTransactionDO_by_statuses_limit() throws SQLException {
        {
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
            globalTransactionDO.setXid("abc-123:1267");
            globalTransactionDO.setApplicationData("abc=234356");
            globalTransactionDO.setTransactionServiceGroup("abc");
            globalTransactionDO.setTransactionName("test");
            globalTransactionDO.setTransactionId(867978970);
            globalTransactionDO.setTimeout(20);
            globalTransactionDO.setBeginTime(System.currentTimeMillis());
            globalTransactionDO.setApplicationId("test");
            globalTransactionDO.setStatus(1);

            Assertions.assertTrue(logStoreDataBaseDAO.insertGlobalTransactionDO(globalTransactionDO));
        }
        {
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
            globalTransactionDO.setXid("abc-123:6978");
            globalTransactionDO.setApplicationData("abc=87867978");
            globalTransactionDO.setTransactionServiceGroup("abc");
            globalTransactionDO.setTransactionName("test");
            globalTransactionDO.setTransactionId(143546567);
            globalTransactionDO.setTimeout(20);
            globalTransactionDO.setBeginTime(System.currentTimeMillis());
            globalTransactionDO.setApplicationId("test");
            globalTransactionDO.setStatus(2);

            boolean ret = logStoreDataBaseDAO.insertGlobalTransactionDO(globalTransactionDO);
            Assertions.assertTrue(ret);
        }
        {
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
            globalTransactionDO.setXid("abc-123:5657");
            globalTransactionDO.setApplicationData("abc=5454");
            globalTransactionDO.setTransactionServiceGroup("abc");
            globalTransactionDO.setTransactionName("test");
            globalTransactionDO.setTransactionId(12345);
            globalTransactionDO.setTimeout(20);
            globalTransactionDO.setBeginTime(System.currentTimeMillis());
            globalTransactionDO.setApplicationId("test");
            globalTransactionDO.setStatus(1);

            boolean ret = logStoreDataBaseDAO.insertGlobalTransactionDO(globalTransactionDO);
            Assertions.assertTrue(ret);
        }

        List<GlobalTransactionDO> globalTransactionDOs = logStoreDataBaseDAO.queryGlobalTransactionDO(new int[]{1}, 1);
        Assertions.assertNotNull(globalTransactionDOs);
        Assertions.assertEquals(1, globalTransactionDOs.size());

        if("abc-123:1267".equals(globalTransactionDOs.get(0).getXid())){
            Assertions.assertTrue(true);
        }else {
            Assertions.assertTrue(false);
        }

        String delSql = "delete from global_table where xid in ('abc-123:1267', 'abc-123:6978', 'abc-123:5657')";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            conn.createStatement().execute(delSql);
        }finally {
            IOUtil.close(conn);
        }

    }

    @Test
    public void insertGlobalTransactionDO() throws SQLException {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid("abc-123:333");
        globalTransactionDO.setApplicationData("abc=5454");
        globalTransactionDO.setTransactionServiceGroup("abc");
        globalTransactionDO.setTransactionName("test");
        globalTransactionDO.setTransactionId(12345);
        globalTransactionDO.setTimeout(20);
        globalTransactionDO.setBeginTime(System.currentTimeMillis());
        globalTransactionDO.setApplicationId("test");
        globalTransactionDO.setStatus(1);

        boolean ret = logStoreDataBaseDAO.insertGlobalTransactionDO(globalTransactionDO);
        Assertions.assertTrue(ret);

        String sql = "select * from global_table where xid= 'abc-123:333'";
        String delSql = "delete from global_table where xid= 'abc-123:333'";
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
            IOUtil.close(conn);
        }
    }

    @Test
    public void updateGlobalTransactionDO() throws SQLException {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid("abc-123:222");
        globalTransactionDO.setApplicationData("abc=5454");
        globalTransactionDO.setTransactionServiceGroup("abc");
        globalTransactionDO.setTransactionName("test");
        globalTransactionDO.setTransactionId(12345);
        globalTransactionDO.setTimeout(20);
        globalTransactionDO.setBeginTime(System.currentTimeMillis());
        globalTransactionDO.setApplicationId("test");
        globalTransactionDO.setStatus(1);

        boolean ret = logStoreDataBaseDAO.insertGlobalTransactionDO(globalTransactionDO);
        Assertions.assertTrue(ret);

        String sql = "select * from global_table where xid= 'abc-123:222'";
        String delSql = "delete from global_table where xid= 'abc-123:222'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
                Assertions.assertEquals(1, rs.getInt("status"));
            }else{
                Assertions.assertTrue(false);
            }
            rs.close();

            //update
            globalTransactionDO.setStatus(2);
            Assertions.assertTrue(logStoreDataBaseDAO.updateGlobalTransactionDO(globalTransactionDO));

            rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
                Assertions.assertEquals(2, rs.getInt("status"));
            }else{
                Assertions.assertTrue(false);
            }
            rs.close();

            //delete
            conn.createStatement().execute(delSql);

        }finally {
            IOUtil.close(conn);
        }

    }

    @Test
    public void deleteGlobalTransactionDO() throws SQLException {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid("abc-123:555");
        globalTransactionDO.setApplicationData("abc=5454");
        globalTransactionDO.setTransactionServiceGroup("abc");
        globalTransactionDO.setTransactionName("test");
        globalTransactionDO.setTransactionId(12345);
        globalTransactionDO.setTimeout(20);
        globalTransactionDO.setBeginTime(System.currentTimeMillis());
        globalTransactionDO.setApplicationId("test");
        globalTransactionDO.setStatus(1);

        boolean ret = logStoreDataBaseDAO.insertGlobalTransactionDO(globalTransactionDO);
        Assertions.assertTrue(ret);

        //delete
        Assertions.assertTrue(logStoreDataBaseDAO.deleteGlobalTransactionDO(globalTransactionDO));

        //check

        String sql = "select * from global_table where xid= 'abc-123:555'";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(false);
            }else{
                Assertions.assertTrue(true);
            }
            rs.close();
        }finally {
            IOUtil.close(conn);
        }
    }

    @Test
    public void queryBranchTransactionDO() throws SQLException {
        {
            //creata data for test
            BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
            branchTransactionDO.setResourceId("qqqq");
            branchTransactionDO.setXid("abc-123:6789");
            branchTransactionDO.setTransactionId(24234235);
            branchTransactionDO.setBranchId(345465676);
            branchTransactionDO.setBranchType("TCC");
            branchTransactionDO.setResourceGroupId("abc");
            branchTransactionDO.setResourceGroupId("a");
            branchTransactionDO.setClientId("1.1.1.1");
            branchTransactionDO.setStatus(1);
            branchTransactionDO.setApplicationData("abc=123");
            branchTransactionDO.setResourceGroupId("test");

            boolean ret = logStoreDataBaseDAO.insertBranchTransactionDO(branchTransactionDO);
            Assertions.assertTrue(ret);
        }
        {
            //creata data for test
            BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
            branchTransactionDO.setResourceId("qqqq");
            branchTransactionDO.setXid("abc-123:6789");
            branchTransactionDO.setTransactionId(24234235);
            branchTransactionDO.setBranchId(78563453);
            branchTransactionDO.setBranchType("TCC");
            branchTransactionDO.setResourceGroupId("abc");
            branchTransactionDO.setResourceGroupId("a");
            branchTransactionDO.setClientId("1.1.1.1");
            branchTransactionDO.setStatus(1);
            branchTransactionDO.setApplicationData("abc=123");
            branchTransactionDO.setResourceGroupId("test");

            boolean ret = logStoreDataBaseDAO.insertBranchTransactionDO(branchTransactionDO);
            Assertions.assertTrue(ret);
        }

        List<BranchTransactionDO> rets = logStoreDataBaseDAO.queryBranchTransactionDO("abc-123:6789");
        Assertions.assertTrue(CollectionUtils.isNotEmpty(rets));
        Assertions.assertEquals(2, rets.size());

        if(78563453 == rets.get(0).getBranchId() && 345465676 == rets.get(1).getBranchId()){
            Assertions.assertTrue(true);
        }else if(78563453 == rets.get(1).getBranchId() && 345465676 == rets.get(0).getBranchId()){
            Assertions.assertTrue(true);
        }else {
            Assertions.assertTrue(false);
        }

        String delSql = "delete from branch_table where xid= 'abc-123:6789' ";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();

            conn.createStatement().execute(delSql);

        }finally {
            IOUtil.close(conn);
        }
    }

    @Test
    public void insertBranchTransactionDO() throws SQLException {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setResourceId("qqqq");
        branchTransactionDO.setXid("abc-123:7777");
        branchTransactionDO.setTransactionId(1285343);
        branchTransactionDO.setBranchId(1234508);
        branchTransactionDO.setBranchType("TCC");
        branchTransactionDO.setResourceGroupId("abc");
        branchTransactionDO.setResourceGroupId("a");
        branchTransactionDO.setClientId("1.1.1.1");
        branchTransactionDO.setStatus(1);
        branchTransactionDO.setApplicationData("abc=123");
        branchTransactionDO.setResourceGroupId("test");

        boolean ret = logStoreDataBaseDAO.insertBranchTransactionDO(branchTransactionDO);
        Assertions.assertTrue(ret);


        String sql = "select * from branch_table where xid= 'abc-123:7777' and branch_id = 1234508";
        String delSql = "delete from branch_table where xid= 'abc-123:7777' and branch_id = 1234508";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();

            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
            }else {
                Assertions.assertTrue(false);
            }

            conn.createStatement().execute(delSql);
        }finally {
            IOUtil.close(conn);
        }

    }

    @Test
    public void updateBranchTransactionDO() throws SQLException {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setResourceId("qqqq");
        branchTransactionDO.setXid("abc-123:8888");
        branchTransactionDO.setTransactionId(1285343);
        branchTransactionDO.setBranchId(343434318);
        branchTransactionDO.setBranchType("TCC");
        branchTransactionDO.setResourceGroupId("abc");
        branchTransactionDO.setResourceGroupId("a");
        branchTransactionDO.setClientId("1.1.1.1");
        branchTransactionDO.setStatus(1);
        branchTransactionDO.setApplicationData("abc=123");
        branchTransactionDO.setResourceGroupId("test");

        boolean ret = logStoreDataBaseDAO.insertBranchTransactionDO(branchTransactionDO);
        Assertions.assertTrue(ret);

        branchTransactionDO.setStatus(3);
        Assertions.assertTrue(logStoreDataBaseDAO.updateBranchTransactionDO(branchTransactionDO));

        String sql = "select * from branch_table where xid= 'abc-123:8888' and branch_id = 343434318";
        String delSql = "delete from branch_table where xid= 'abc-123:8888' and branch_id = 343434318";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();

            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
                Assertions.assertEquals(3, rs.getInt("status"));
            }else {
                Assertions.assertTrue(false);
            }

            conn.createStatement().execute(delSql);
        }finally {
            IOUtil.close(conn);
        }
    }

    @Test
    public void deleteBranchTransactionDO() throws SQLException {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setResourceId("qqqq");
        branchTransactionDO.setXid("abc-123:9999");
        branchTransactionDO.setTransactionId(1285343);
        branchTransactionDO.setBranchId(34567798);
        branchTransactionDO.setBranchType("TCC");
        branchTransactionDO.setResourceGroupId("abc");
        branchTransactionDO.setResourceGroupId("a");
        branchTransactionDO.setClientId("1.1.1.1");
        branchTransactionDO.setStatus(1);
        branchTransactionDO.setApplicationData("abc=123");
        branchTransactionDO.setResourceGroupId("test");

        boolean ret = logStoreDataBaseDAO.insertBranchTransactionDO(branchTransactionDO);
        Assertions.assertTrue(ret);


        String sql = "select * from branch_table where xid= 'abc-123:9999' and branch_id = 34567798";
        String delSql = "delete from branch_table where xid= 'abc-123:9999' and branch_id = 34567798";
        Connection conn = null;
        try{
            conn = dataSource.getConnection();

            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
            }else {
                Assertions.assertTrue(false);
            }
            rs.close();

            //delete
            logStoreDataBaseDAO.deleteBranchTransactionDO(branchTransactionDO);

            rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(false);
            }else {
                Assertions.assertTrue(true);
            }
            rs.close();

        }finally {
            IOUtil.close(conn);
        }
    }

    @AfterAll
    public static void clearStoreDB(){
        FileUtils.deleteRecursive("db_store", true);
    }

}
