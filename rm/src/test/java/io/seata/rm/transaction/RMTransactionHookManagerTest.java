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
package io.seata.rm.transaction;

import io.seata.core.model.BranchType;
import io.seata.rm.transaction.mock.MockRMTransactionHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author wang.liang
 */
public class RMTransactionHookManagerTest {

    private static final String DEFAULT_XID = "1234567890";
    private static final long DEFAULT_BRANCH_ID = 1234567890L;

    @AfterEach
    public void clearAfterEach() {
        RMTransactionHookManager.getGlobalHooks().clear();
        RMTransactionHookManager.clear();
    }

    @Test
    public void test_getGlobalHooks_registerGlobalHooks() {
        assertThat(RMTransactionHookManager.getGlobalHooks()).isNotNull();
        assertThat(RMTransactionHookManager.getGlobalHooks()).isEmpty();
        RMTransactionHookManager.registerGlobalHook(new MockRMTransactionHook());
        assertThat(RMTransactionHookManager.getGlobalHooks()).isNotEmpty();
    }

    @Test
    public void test_getLocalBranchId_setLocalBranchId() {
        assertThat(RMTransactionHookManager.getLocalBranchId()).isNull();
        RMTransactionHookManager.setLocalBranchId(DEFAULT_BRANCH_ID);
        assertThat(RMTransactionHookManager.getLocalBranchId()).isEqualTo(DEFAULT_BRANCH_ID);
        RMTransactionHookManager.clear();
        assertThat(RMTransactionHookManager.getLocalBranchId()).isNull();
    }

    @Test
    public void test_getLocalHooks_registerLocalHook_getBranchHooks_removeBranchHooks_clear() {
        assertThat(RMTransactionHookManager.getBranchHooks(DEFAULT_BRANCH_ID)).isNull();

        // before set local branch id
        assertThat(RMTransactionHookManager.getLocalHooks()).isNull();
        RMTransactionHookManager.registerLocalHook(new MockRMTransactionHook()); // register to local hooks
        assertThat(RMTransactionHookManager.getLocalHooks()).isNotEmpty();
        assertThat(RMTransactionHookManager.getBranchHooks(DEFAULT_BRANCH_ID)).isNull();

        // after set local branch id
        RMTransactionHookManager.setLocalBranchId(DEFAULT_BRANCH_ID);
        assertThat(RMTransactionHookManager.getLocalHooks()).isNull(); // after setLocalBranchId, the localHooks moved to branchHooks
        assertThat(RMTransactionHookManager.getBranchHooks(DEFAULT_BRANCH_ID)).isNotEmpty();
        RMTransactionHookManager.registerLocalHook(new MockRMTransactionHook()); // register to branch hooks
        assertThat(RMTransactionHookManager.getLocalHooks()).isNull();

        //remove
        RMTransactionHookManager.removeBranchHooks(DEFAULT_BRANCH_ID);
        assertThat(RMTransactionHookManager.getBranchHooks(DEFAULT_BRANCH_ID)).isNull();

        //clear
        assertThat(RMTransactionHookManager.getLocalBranchId()).isEqualTo(DEFAULT_BRANCH_ID);
        RMTransactionHookManager.clear();
        assertThat(RMTransactionHookManager.getLocalBranchId()).isNull();
    }

    @Test
    public void test_getHooks_triggerHooks() {
        RMTransactionHookManager.registerGlobalHook(new RMTransactionHook() {
            @Override
            public void beforeBranchCommit(BranchType branchType, String xid, long branchId) {
                assertThat(branchType).isEqualTo(BranchType.SAGA);
                assertThat(xid).isEqualTo(DEFAULT_XID);
                assertThat(branchId).isEqualTo(DEFAULT_BRANCH_ID);
                throw new RuntimeException();
            }
        });

        StringBuilder sb = new StringBuilder();

        //no throw
        Assertions.assertDoesNotThrow(() -> {
            RMTransactionHookManager.triggerHooks(null, DEFAULT_BRANCH_ID, (hook) -> {
                sb.append("1");
                hook.beforeBranchCommit(BranchType.SAGA, DEFAULT_XID, DEFAULT_BRANCH_ID);
            });
        });
        assertThat(sb.toString()).isEqualTo("1");
    }

    @Test
    public void test_clear() {
        assertThat(RMTransactionHookManager.getLocalBranchId()).isNull();
        assertThat(RMTransactionHookManager.getLocalHooks()).isNull();

        RMTransactionHookManager.registerLocalHook(new MockRMTransactionHook());
        assertThat(RMTransactionHookManager.getLocalHooks()).isNotEmpty();
        RMTransactionHookManager.clear();
        assertThat(RMTransactionHookManager.getLocalHooks()).isNull();

        //clear
        RMTransactionHookManager.setLocalBranchId(DEFAULT_BRANCH_ID);
        RMTransactionHookManager.registerLocalHook(new MockRMTransactionHook());
        assertThat(RMTransactionHookManager.getLocalHooks()).isNull();
        assertThat(RMTransactionHookManager.getLocalBranchId()).isEqualTo(DEFAULT_BRANCH_ID);
        RMTransactionHookManager.clear();
        assertThat(RMTransactionHookManager.getLocalBranchId()).isNull();
    }

    @Test
    public void testNPE() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            RMTransactionHookManager.registerGlobalHook(null);
        });
        Assertions.assertThrows(NullPointerException.class, () -> {
            RMTransactionHookManager.registerLocalHook(null);
        });
    }
}
