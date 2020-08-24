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
package io.seata.tc.lock;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.tc.UUIDGenerator;
import io.seata.tc.lock.file.FileLockManagerForTest;
import io.seata.tc.session.BranchSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The type Lock manager test.
 *
 * @author tianming.xm @gmail.com
 * @since 2019 /1/23
 */
public class LockManagerTest {

    /**
     * Acquire lock success.
     *
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void acquireLock_success(BranchSession branchSession) throws Exception {
        LockManager lockManager = new FileLockManagerForTest();
        Assertions.assertTrue(lockManager.acquireLock(branchSession));
    }

    /**
     * Acquire lock failed.
     *
     * @param branchSession1 the branch session 1
     * @param branchSession2 the branch session 2
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionsProvider")
    public void acquireLock_failed(BranchSession branchSession1, BranchSession branchSession2) throws Exception {
        LockManager lockManager = new FileLockManagerForTest();
        Assertions.assertTrue(lockManager.acquireLock(branchSession1));
        Assertions.assertFalse(lockManager.acquireLock(branchSession2));
    }

    /**
     * deadlock test.
     *
     * @param branchSession1 the branch session 1
     * @param branchSession2 the branch session 2
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("deadlockBranchSessionsProvider")
    public void deadlockTest(BranchSession branchSession1, BranchSession branchSession2) throws Exception {
        LockManager lockManager = new FileLockManagerForTest();
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            new Thread(() -> {
                try {
                    lockManager.acquireLock(branchSession1);
                } catch (TransactionException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
            new Thread(() -> {
                try {
                    lockManager.acquireLock(branchSession2);
                } catch (TransactionException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
            // Assume execute more than 5 seconds means deadlock happened.
            Assertions.assertTrue(countDownLatch.await(5, TimeUnit.SECONDS));
        } finally {
            lockManager.releaseLock(branchSession1);
            lockManager.releaseLock(branchSession2);
        }

    }

    /**
     * Make sure two concurrent branchSession register process with different row key list, at least one process could
     * success and only one process could success.
     *
     * @param branchSession1 the branch session 1
     * @param branchSession2 the branch session 2
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("deadlockBranchSessionsProvider")
    public void concurrentUseAbilityTest(BranchSession branchSession1, BranchSession branchSession2)  throws Exception {
        LockManager lockManager = new FileLockManagerForTest();
        try {
            final AtomicBoolean first = new AtomicBoolean();
            final AtomicBoolean second = new AtomicBoolean();
            CountDownLatch countDownLatch = new CountDownLatch(2);
            new Thread(() -> {
                try {
                    first.set(lockManager.acquireLock(branchSession1));
                } catch (TransactionException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
            new Thread(() -> {
                try {
                    second.set(lockManager.acquireLock(branchSession2));
                } catch (TransactionException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
            // Assume execute more than 5 seconds means deadlock happened.
            if (countDownLatch.await(5, TimeUnit.SECONDS)) {
                Assertions.assertTrue(!first.get() || !second.get());
            }
        } finally {
            lockManager.releaseLock(branchSession1);
            lockManager.releaseLock(branchSession2);
        }
    }

    /**
     * Is lockable test.
     *
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void isLockableTest(BranchSession branchSession) throws Exception {
        branchSession.setLockKey("t:4");
        LockManager lockManager = new FileLockManagerForTest();
        Assertions.assertTrue(lockManager
                .isLockable(branchSession.getXid(), branchSession.getResourceId(), branchSession.getLockKey()));
        lockManager.acquireLock(branchSession);
        branchSession.setTransactionId(UUIDGenerator.generateUUID());
        Assertions.assertFalse(lockManager
                .isLockable(branchSession.getXid(), branchSession.getResourceId(), branchSession.getLockKey()));
    }

    @ParameterizedTest
    @MethodSource("duplicatePkBranchSessionsProvider")
    public void duplicatePkBranchSessionHolderTest(BranchSession branchSession1, BranchSession branchSession2) throws Exception {
        LockManager lockManager = new FileLockManagerForTest();
        Assertions.assertTrue(lockManager.acquireLock(branchSession1));
        Assertions.assertEquals(4, branchSession1.getLockHolder().values().stream().map(Set::size).count());
        Assertions.assertTrue(lockManager.releaseLock(branchSession1));
        Assertions.assertEquals(0, branchSession1.getLockHolder().values().stream().map(Set::size).count());
        Assertions.assertTrue(lockManager.acquireLock(branchSession2));
        Assertions.assertEquals(4, branchSession2.getLockHolder().values().stream().map(Set::size).count());
        Assertions.assertTrue(lockManager.releaseLock(branchSession2));
        Assertions.assertEquals(0, branchSession2.getLockHolder().values().stream().map(Set::size).count());
    }

    /**
     * Branch session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> branchSessionProvider() {
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(UUIDGenerator.generateUUID());
        branchSession.setBranchId(0L);
        branchSession.setClientId("c1");
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t:0");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setBranchType(BranchType.AT);
        return Stream.of(
                Arguments.of(branchSession));
    }

    /**
     * Base branch sessions provider object [ ] [ ]. Could assign resource and lock keys.
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> baseBranchSessionsProvider(String resource, String lockKey1, String lockKey2) {
        BranchSession branchSession1 = new BranchSession();
        branchSession1.setTransactionId(UUIDGenerator.generateUUID());
        branchSession1.setBranchId(1L);
        branchSession1.setClientId("c1");
        branchSession1.setResourceGroupId("my_test_tx_group");
        branchSession1.setResourceId(resource);
        branchSession1.setLockKey(lockKey1);
        branchSession1.setBranchType(BranchType.AT);
        branchSession1.setApplicationData("{\"data\":\"test\"}");
        branchSession1.setBranchType(BranchType.AT);

        BranchSession branchSession2 = new BranchSession();
        branchSession2.setTransactionId(UUIDGenerator.generateUUID());
        branchSession2.setBranchId(2L);
        branchSession2.setClientId("c1");
        branchSession2.setResourceGroupId("my_test_tx_group");
        branchSession2.setResourceId(resource);
        branchSession2.setLockKey(lockKey2);
        branchSession2.setBranchType(BranchType.AT);
        branchSession2.setApplicationData("{\"data\":\"test\"}");
        branchSession2.setBranchType(BranchType.AT);
        return Stream.of(
                Arguments.of(branchSession1, branchSession2));
    }

    /**
     * Branch sessions provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> branchSessionsProvider() {
        return baseBranchSessionsProvider("tb_1", "t:1,2", "t:1,2");
    }

    static Stream<Arguments> deadlockBranchSessionsProvider() {
        return baseBranchSessionsProvider("tb_2", "t:1,2,3,4,5", "t:5,4,3,2,1");
    }

    static Stream<Arguments> duplicatePkBranchSessionsProvider() {
        return baseBranchSessionsProvider("tb_2", "t:1,2;t1:1;t2:2", "t:1,2;t1:1;t2:2");
    }
}
