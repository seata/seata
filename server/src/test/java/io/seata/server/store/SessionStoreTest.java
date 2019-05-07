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
package io.seata.server.store;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalOperation;
import io.seata.core.model.GlobalStatus;
import io.seata.server.lock.LockManager;
import io.seata.server.lock.LockManagerFactory;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHelper;
import io.seata.server.session.SessionHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * The type Session store test.
 */
public class SessionStoreTest {

    /**
     * The constant RESOURCE_ID.
     */
    public static final String RESOURCE_ID = "mysql:xxx";


    private static Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * Clean.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void clean() throws Exception {
        String sessionStorePath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR);
        File rootDataFile = new File(sessionStorePath + File.separator + SessionHolder.ROOT_SESSION_MANAGER_NAME);
        File rootDataFileHis = new File(sessionStorePath + File.separator + SessionHolder.ROOT_SESSION_MANAGER_NAME + ".1");

        if (rootDataFile.exists()) {
            rootDataFile.delete();
        }
        if (rootDataFileHis.exists()) {
            rootDataFileHis.delete();
        }
        LockManagerFactory.get().cleanAllLocks();
    }

    /**
     * Test restored from file.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRestoredFromFile() throws Exception {
        SessionHolder.init("file");
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        BranchSession branchSession1 = SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, RESOURCE_ID,
                "ta:1,2;tb:3", "xxx");
        branchSession1.lock();
        globalSession.addBranch(branchSession1);

        LockManager lockManager = LockManagerFactory.get();

        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));
        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:2"));
        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "tb:3"));

        Assertions.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:4"));
        Assertions.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "tb:5"));

        lockManager.cleanAllLocks();

        Assertions.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));
        Assertions.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:2"));
        Assertions.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "tb:3"));

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init("file");

        long tid = globalSession.getTransactionId();
        GlobalSession reloadSession = SessionHolder.findGlobalSession(tid);
        Assertions.assertNotNull(reloadSession);
        Assertions.assertFalse(globalSession == reloadSession);
        Assertions.assertEquals(globalSession.getApplicationId(), reloadSession.getApplicationId());

        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));
        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:2"));
        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "tb:3"));
        Assertions.assertTrue(lockManager.isLockable(globalSession.getTransactionId(), RESOURCE_ID, "tb:3"));

        //clear
        reloadSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        reloadSession.end();
    }

    /**
     * Test restored from file 2.
     *
     * @throws Exception the exception
     */
    //@Test
    public void testRestoredFromFile2() throws Exception {
        SessionHolder.init("file");
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init("file");
    }

    /**
     * Test restored from file async committing.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRestoredFromFileAsyncCommitting() throws Exception {
        SessionHolder.init("file");
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        BranchSession branchSession1 = SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, RESOURCE_ID,
                "ta:1", "xxx");
        Assertions.assertTrue(branchSession1.lock());
        globalSession.addBranch(branchSession1);

        LockManager lockManager = LockManagerFactory.get();

        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        globalSession.changeStatus(GlobalOperation.COMMIT,GlobalStatus.Committing);
        globalSession.changeStatus(GlobalOperation.ASYNC_COMMIT,GlobalStatus.AsyncCommitting);

        lockManager.cleanAllLocks();

        Assertions.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init("file");

        long tid = globalSession.getTransactionId();
        GlobalSession reloadSession = SessionHolder.findGlobalSession(tid);
        Assertions.assertEquals(reloadSession.getStatus(), GlobalStatus.AsyncCommitting);

        GlobalSession sessionInAsyncCommittingQueue = SessionHolder.getAsyncCommittingSessionManager()
                .findGlobalSession(tid);
        Assertions.assertTrue(reloadSession == sessionInAsyncCommittingQueue);

        // No locking for session in AsyncCommitting status
        Assertions.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        //clear
        reloadSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        reloadSession.end();
    }

    /**
     * Test restored from file commit retry.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRestoredFromFileCommitRetry() throws Exception {
        SessionHolder.init("file");
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        BranchSession branchSession1 = SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, RESOURCE_ID,
                "ta:1", "xxx");
        branchSession1.lock();
        globalSession.addBranch(branchSession1);

        LockManager lockManager = LockManagerFactory.get();

        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        globalSession.changeStatus(GlobalOperation.COMMIT,GlobalStatus.Committing);
        globalSession.changeBranchStatus(branchSession1, BranchStatus.PhaseTwo_CommitFailed_Retryable);
        globalSession.changeStatus(GlobalOperation.RETRY_COMMIT,GlobalStatus.CommitRetrying);

        lockManager.cleanAllLocks();

        Assertions.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init("file");

        long tid = globalSession.getTransactionId();
        GlobalSession reloadSession = SessionHolder.findGlobalSession(tid);
        Assertions.assertEquals(reloadSession.getStatus(), GlobalStatus.CommitRetrying);

        GlobalSession sessionInRetryCommittingQueue = SessionHolder.getRetryCommittingSessionManager()
                .findGlobalSession(tid);
        Assertions.assertTrue(reloadSession == sessionInRetryCommittingQueue);
        BranchSession reloadBranchSession = reloadSession.getBranch(branchSession1.getBranchId());
        Assertions.assertEquals(reloadBranchSession.getStatus(), BranchStatus.PhaseTwo_CommitFailed_Retryable);

        // Lock is held by session in CommitRetrying status
        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        //clear
        reloadSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        reloadSession.end();
    }

    /**
     * Test restored from file rollback retry.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRestoredFromFileRollbackRetry() throws Exception {
        SessionHolder.init("file");

        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        BranchSession branchSession1 = SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, RESOURCE_ID,
                "ta:1", "xxx");
        branchSession1.lock();
        globalSession.addBranch(branchSession1);

        LockManager lockManager = LockManagerFactory.get();

        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        globalSession.changeStatus(GlobalOperation.ROLLBACK,GlobalStatus.Rollbacking);
        globalSession.changeBranchStatus(branchSession1, BranchStatus.PhaseTwo_RollbackFailed_Retryable);
        globalSession.changeStatus(GlobalOperation.RETRY_ROLLBACK,GlobalStatus.RollbackRetrying);

        lockManager.cleanAllLocks();

        Assertions.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init("file");

        long tid = globalSession.getTransactionId();
        GlobalSession reloadSession = SessionHolder.findGlobalSession(tid);
        Assertions.assertEquals(reloadSession.getStatus(), GlobalStatus.RollbackRetrying);

        GlobalSession sessionInRetryRollbackingQueue = SessionHolder.getRetryRollbackingSessionManager()
                .findGlobalSession(tid);
        Assertions.assertTrue(reloadSession == sessionInRetryRollbackingQueue);
        BranchSession reloadBranchSession = reloadSession.getBranch(branchSession1.getBranchId());
        Assertions.assertEquals(reloadBranchSession.getStatus(), BranchStatus.PhaseTwo_RollbackFailed_Retryable);

        // Lock is held by session in RollbackRetrying status
        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        //clear
        reloadSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        reloadSession.end();
    }

    /**
     * Test restored from file rollback failed.
     *
     * @throws Exception the exception
     */
    @Test
    public void testRestoredFromFileRollbackFailed() throws Exception {
        SessionHolder.init("file");

        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);

        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.begin();

        BranchSession branchSession1 = SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, RESOURCE_ID,
                "ta:1", "xxx");
        branchSession1.lock();
        globalSession.addBranch(branchSession1);

        LockManager lockManager = LockManagerFactory.get();

        Assertions.assertFalse(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        globalSession.changeStatus(GlobalOperation.ROLLBACK,GlobalStatus.Rollbacking);
        globalSession.changeBranchStatus(branchSession1, BranchStatus.PhaseTwo_CommitFailed_Unretryable);
        SessionHelper.endRollbackFailed(globalSession);

        // Lock is released.
        Assertions.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        lockManager.cleanAllLocks();

        Assertions.assertTrue(lockManager.isLockable(0L, RESOURCE_ID, "ta:1"));

        // Re-init SessionHolder: restore sessions from file
        SessionHolder.init("file");

        long tid = globalSession.getTransactionId();
        GlobalSession reloadSession = SessionHolder.findGlobalSession(tid);
        Assertions.assertNull(reloadSession);
    }
}
