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
package io.seata.server.session;

import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalOperation;
import io.seata.core.model.GlobalStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * The type Global session test.
 *
 * @author tianming.xm @gmail.com
 * @since 2019 /1/23
 */
public class GlobalSessionTest {

    /**
     * Can be committed async test.
     *
     * @param globalSession the global session
     */
    @ParameterizedTest
    @MethodSource("branchSessionTCCProvider")
    public void canBeCommittedAsyncTest(GlobalSession globalSession) {
        Assertions.assertFalse(globalSession.canBeCommittedAsync());
    }

    /**
     * Begin test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void beginTest(GlobalSession globalSession) throws Exception {
        globalSession.begin();
    }

    /**
     * Change status test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void changeStatusTest(GlobalSession globalSession) throws Exception {
        globalSession.changeStatus(GlobalOperation.COMMIT, GlobalStatus.Committing);
    }

    /**
     * Change branch status test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void changeBranchStatusTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        globalSession.changeBranchStatus(branchSession, BranchStatus.PhaseTwo_Committed);
    }

    /**
     * Close test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void closeTest(GlobalSession globalSession) throws Exception {
        globalSession.close();
    }

    /**
     * End test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void endTest(GlobalSession globalSession) throws Exception {
        globalSession.end();
    }

    /**
     * Add branch test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void addBranchTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        globalSession.addBranch(branchSession);
    }

    /**
     * Remove branch test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void removeBranchTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        globalSession.addBranch(branchSession);
        globalSession.removeBranch(branchSession);
    }

    /**
     * Codec test.
     *
     * @param globalSession the global session
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void codecTest(GlobalSession globalSession) {
        byte[] result = globalSession.encode();
        Assertions.assertNotNull(result);
        GlobalSession expected = new GlobalSession();
        expected.decode(result);
        Assertions.assertEquals(expected.getTransactionId(), globalSession.getTransactionId());
        Assertions.assertEquals(expected.getTimeout(), globalSession.getTimeout());
        Assertions.assertEquals(expected.getApplicationId(), globalSession.getApplicationId());
        Assertions.assertEquals(expected.getTransactionServiceGroup(), globalSession.getTransactionServiceGroup());
        Assertions.assertEquals(expected.getTransactionName(), globalSession.getTransactionName());
    }

    /**
     * Global session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> globalSessionProvider() {
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);
        globalSession.setActive(true);
        globalSession.addSessionLifecycleListener(new DefaultSessionManager("default"));
        return Stream.of(
                Arguments.of(
                        globalSession)
        );
    }

    /**
     * Branch session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> branchSessionProvider() {
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        globalSession.add(branchSession);
        return Stream.of(
                Arguments.of(
                        globalSession, branchSession)
        );
    }

    /**
     * Branch session mt provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */

    static Stream<Arguments> branchSessionTCCProvider() {
        GlobalSession globalSession = new GlobalSession("demo-app", "my_test_tx_group", "test", 6000);
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.TCC);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        globalSession.add(branchSession);
        return Stream.of(
                Arguments.of(globalSession)
        );
    }
}
