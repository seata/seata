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
package io.seata.server.lock.db;

import io.seata.common.util.IOUtil;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.server.storage.db.lock.LockStoreDataBaseDAO;
import io.seata.server.storage.file.lock.FileLockManager;
import io.seata.server.lock.LockManager;
import io.seata.server.storage.db.lock.DataBaseLocker;
import io.seata.server.session.BranchSession;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;



/**
 * @author zhangsen
 */
public class DataBaseLockManagerImplTest {

    static LockManager lockManager = null;

    static BasicDataSource dataSource = null;

    static LockStoreDataBaseDAO dataBaseLockStoreDAO  = null;

    @BeforeAll
    public static void start(){
        dataSource =  new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:./db_store/db_lock");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        dataBaseLockStoreDAO = new LockStoreDataBaseDAO(dataSource);
        dataBaseLockStoreDAO.setDbType("h2");
        dataBaseLockStoreDAO.setLockTable("lock_table");

        lockManager = new DBLockManagerForTest(dataBaseLockStoreDAO);

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
            s.execute("CREATE TABLE lock_table (" +
                    "  xid            varchar(96)," +
                    "  transaction_id long," +
                    "  branch_id      long," +
                    "  resource_id    varchar(32)," +
                    "  table_name     varchar(32)," +
                    "  pk             varchar(32)," +
                    "  row_key        varchar(128) primary key not null," +
                    "  gmt_create     timestamp(6)," +
                    "  gmt_modified   timestamp(6)" +
                    ")");
            System.out.println("create table lock_table success.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(conn);
        }
    }

    @Test
    public void acquireLock() throws TransactionException, SQLException {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:786756");
        branchSession.setTransactionId(123543465);
        branchSession.setBranchId(5756678);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:13,14;t2:11,12");

        Assertions.assertTrue(lockManager.acquireLock(branchSession));

        String sql = "select * from lock_table where xid = 'abc-123:786756'"  ;
        String sql2 = "select count(*) from lock_table where xid = 'abc-123:786756' " +
                "and row_key in ('abcss^^^t1^^^13', 'abcss^^^t1^^^14', 'abcss^^^t2^^^11', 'abcss^^^t2^^^12')"  ;
        String delSql = "delete from lock_table where xid = 'abc-123:786756'"  ;
        Connection conn =  null;
        try {
            conn = dataSource.getConnection();

            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
            }else {
                Assertions.assertTrue(false);
            }
            rs.close();

            rs = conn.createStatement().executeQuery(sql2);
            if(rs.next()){
                Assertions.assertTrue(true);
                Assertions.assertEquals(4, rs.getInt(1));
            }else {
                Assertions.assertTrue(false);
            }
            rs.close();

            conn.createStatement().execute(delSql);
        } finally {
            IOUtil.close(conn);
        }
    }

    @Test
    public void re_acquireLock() throws TransactionException, SQLException {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:65867978");
        branchSession.setTransactionId(123543465);
        branchSession.setBranchId(5756678);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:53,54;t2:21,32");

        Assertions.assertTrue(lockManager.acquireLock(branchSession));

        BranchSession branchSession2 = new BranchSession();
        branchSession2.setXid("abc-123:65867978");
        branchSession2.setTransactionId(123543465);
        branchSession2.setBranchId(575667854);
        branchSession2.setResourceId("abcss");
        branchSession2.setLockKey("t1:13,14;t2:21,45");

        Assertions.assertTrue(lockManager.acquireLock(branchSession2));

        BranchSession branchSession3 = new BranchSession();
        branchSession3.setXid("abc-123:5678789");
        branchSession3.setTransactionId(334123);
        branchSession3.setBranchId(5657);
        branchSession3.setResourceId("abcss");
        branchSession3.setLockKey("t1:53,14;t2:21,45");

        Assertions.assertTrue(!lockManager.acquireLock(branchSession3));

        String delSql = "delete from lock_table where xid in( 'abc-123:65867978' , 'abc-123:65867978' , 'abc-123:5678789'  )"  ;
        Connection conn =  null;
        try {
            conn = dataSource.getConnection();

            conn.createStatement().execute(delSql);
        } finally {
            IOUtil.close(conn);
        }
    }

    @Test
    public void unLock() throws TransactionException, SQLException {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:56867");
        branchSession.setTransactionId(1236765);
        branchSession.setBranchId(204565);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:3,4;t2:4,5");

        Assertions.assertTrue(lockManager.acquireLock(branchSession));

        String sql = "select * from lock_table where xid = 'abc-123:56867'"  ;
        String sql2 = "select count(*) from lock_table where xid = 'abc-123:56867' " +
                "and row_key in ('abcss^^^t1^^^3', 'abcss^^^t1^^^4', 'abcss^^^t2^^^4', 'abcss^^^t2^^^5')"  ;
        Connection conn =  null;
        try {
            conn = dataSource.getConnection();

            ResultSet rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(true);
            }else {
                Assertions.assertTrue(false);
            }
            rs.close();

            rs = conn.createStatement().executeQuery(sql2);
            if(rs.next()){
                Assertions.assertTrue(true);
                Assertions.assertEquals(4, rs.getInt(1));
            }else {
                Assertions.assertTrue(false);
            }
            rs.close();

            //un lock
            Assertions.assertTrue(lockManager.releaseLock(branchSession));

            rs = conn.createStatement().executeQuery(sql);
            if(rs.next()){
                Assertions.assertTrue(false);
            }else {
                Assertions.assertTrue(true);
            }
            rs.close();

        } finally {
            IOUtil.close(conn);
        }



    }

    @Test
    public void isLockable() throws TransactionException, SQLException {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("abc-123:56877898");
        branchSession.setTransactionId(245686786);
        branchSession.setBranchId(467568);
        branchSession.setResourceId("abcss");
        branchSession.setLockKey("t1:8,7;t2:1,2");

        Assertions.assertTrue(lockManager.acquireLock(branchSession));

        BranchSession branchSession2 = new BranchSession();
        branchSession2.setXid("abc-123:56877898");
        branchSession2.setTransactionId(245686786);
        branchSession2.setBranchId(1242354576);
        branchSession2.setResourceId("abcss");
        branchSession2.setLockKey("t1:8");

        Assertions.assertTrue(lockManager.isLockable(branchSession2.getXid(), branchSession2.getResourceId(), branchSession2.getLockKey()));

        BranchSession branchSession3 = new BranchSession();
        branchSession3.setXid("abc-123:4575614354");
        branchSession3.setTransactionId(65867867);
        branchSession3.setBranchId(123123);
        branchSession3.setResourceId("abcss");
        branchSession3.setLockKey("t2:1,12");

        Assertions.assertTrue(!lockManager.isLockable(branchSession3.getXid(), branchSession3.getResourceId(), branchSession3.getLockKey()));

        String delSql = "delete from lock_table where xid in( 'abc-123:56877898' , 'abc-123:56877898' , 'abc-123:4575614354'  )"  ;
        Connection conn =  null;
        try {
            conn = dataSource.getConnection();

            conn.createStatement().execute(delSql);
        } finally {
            IOUtil.close(conn);
        }
    }

    public static class DBLockManagerForTest extends FileLockManager {

        protected LockStoreDataBaseDAO lockStore;

        public DBLockManagerForTest(LockStoreDataBaseDAO db){
            lockStore = db;
        }

        @Override
        public Locker getLocker(BranchSession branchSession) {
            DataBaseLocker locker =  new DataBaseLocker();
            locker.setLockStore(lockStore);
            return locker;
        }
    }
}
