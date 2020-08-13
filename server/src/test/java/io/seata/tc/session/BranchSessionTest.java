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
package io.seata.tc.session;

import io.seata.core.model.BranchType;
import io.seata.tc.UUIDGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * The type Branch session test.
 *
 * @author tianming.xm @gmail.com
 * @since 2019 /1/23
 */
public class BranchSessionTest {

    /**
     * Codec test.
     *
     * @param branchSession the branch session
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void codecTest(BranchSession branchSession) {
        byte[] result = branchSession.encode();
        Assertions.assertNotNull(result);
        BranchSession expected = new BranchSession();
        expected.decode(result);
        Assertions.assertEquals(branchSession.getTransactionId(), expected.getTransactionId());
        Assertions.assertEquals(branchSession.getBranchId(), expected.getBranchId());
        Assertions.assertEquals(branchSession.getResourceId(), expected.getResourceId());
        Assertions.assertEquals(branchSession.getLockKey(), expected.getLockKey());
        Assertions.assertEquals(branchSession.getClientId(), expected.getClientId());
        Assertions.assertEquals(branchSession.getApplicationData(), expected.getApplicationData());

    }

    /**
     * Branch session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> branchSessionProvider() {
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(UUIDGenerator.generateUUID());
        branchSession.setBranchId(1L);
        branchSession.setClientId("c1");
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setBranchType(BranchType.AT);
        return Stream.of(
                Arguments.of(branchSession)
        );
    }
}
