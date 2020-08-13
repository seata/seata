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
package io.seata.tc.lock.db;

import io.seata.common.util.IOUtil;
import io.seata.core.store.LockDO;
import io.seata.tc.storage.db.lock.LockStoreDataBaseDAO;
import org.apache.commons.dbcp2.BasicDataSource;
import org.h2.store.fs.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhangsen
 */
public class DataBaseLockStoreDAOTest {

    static LockStoreDataBaseDAO dataBaseLockStoreDAO  = null;

    static BasicDataSource dataSource = null;

    @BeforeAll
    public static void start(){
        dataSource =  new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:./db_store/lock");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        dataBaseLockStoreDAO = new LockStoreDataBaseDAO(dataSource);
        dataBaseLockStoreDAO.setDbType("h2");
        dataBaseLockStoreDAO.setLockTable("lock_table");

        prepareTable(dataSource);
    }

    private static void prepareTable(BasicDataSource dataSource) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            Statement s = conn.createStatement();
            try {
                s.execute("drop table lock_table");
            } catch (Exception e) {
            }
            s.execute("CREATE TABLE lock_table ( xid varchar(96) ,  transaction_id long , branch_id long, resource_id varchar(32) ,table_name varchar(32) ,pk varchar(32) ,  row_key  varchar(128) primary key not null, gmt_create TIMESTAMP(6) ,gmt_modified TIMESTAMP(6) ) ");
            System.out.println("create table lock_table success.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(conn);
        }
    }

    @Test
    public void test_acquireLocks() throws SQLException {
        List<LockDO> lockDOs = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = dataBaseLockStoreDAO.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        String sql = "select * from lock_table where xid = 'abc-123:123' and table_name  = 't' and row_key in ('abc-0','abc-1','abc-2')"  ;
        Connection conn =  null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
            }else {
                Assertions.assertTrue(false);
            }
        } finally {
            IOUtil.close(conn);
        }

        Assertions.assertTrue(dataBaseLockStoreDAO.unLock(lockDOs));

    }


    @Test
    public void test_re_acquireLocks() throws SQLException {
        List<LockDO> lockDOs = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = dataBaseLockStoreDAO.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        String sql = "select * from lock_table where xid = 'abc-123:123' and table_name  = 't' and row_key in ('abc-0','abc-1','abc-2')"  ;
        Connection conn =  null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
            }else {
                Assertions.assertTrue(false);
            }
        } finally {
            IOUtil.close(conn);
        }

        //lock again
        Assertions.assertTrue(dataBaseLockStoreDAO.acquireLock(lockDOs));

        Assertions.assertTrue(dataBaseLockStoreDAO.unLock(lockDOs));

    }

    @Test
    public void tes_unLocks() throws SQLException {
        List<LockDO> lockDOs = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-456:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = dataBaseLockStoreDAO.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        String sql = "select * from lock_table where xid = 'abc-456:123' and table_name  = 't' and row_key in ('abc-0','abc-1','abc-2')"  ;
        Connection conn =  null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
            }else {
                Assertions.assertTrue(false);
            }
            rs.close();

            //unlock
            Assertions.assertTrue(dataBaseLockStoreDAO.unLock(lockDOs));

            rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(false);
            }else {
                Assertions.assertTrue(true);
            }

        } finally {
            IOUtil.close(conn);
        }


    }


    @Test
    public void test_isLockable_can(){
        List<LockDO> lockDOs = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-678:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = dataBaseLockStoreDAO.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        //unlock
        Assertions.assertTrue(dataBaseLockStoreDAO.unLock(lockDOs));
    }

    @Test
    public void test_isLockable_cannot() throws SQLException {
        List<LockDO> lockDOs = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:222");
            lock.setTransactionId(222L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = dataBaseLockStoreDAO.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        String sql = "select * from lock_table where xid = 'abc-123:222' and table_name  = 't' and row_key in ('abc-0','abc-1','abc-2')"  ;
        Connection conn =  null;
        try{
            conn = dataSource.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
            }else {
                Assertions.assertTrue(false);
            }
        } finally {
            IOUtil.close(conn);
        }

        List<LockDO> lockDOs_2 = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:333");
            lock.setTransactionId(333L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs_2.add(lock);
        }

        boolean ret2 = dataBaseLockStoreDAO.acquireLock(lockDOs_2);
        Assertions.assertTrue(!ret2);

    }

    @AfterAll
    public static void clearStoreDB(){
        FileUtils.deleteRecursive("db_store", true);
    }

}
