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
package io.seata.tm.api.transaction;

import io.seata.core.context.RootContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author guoyao
 */
public class TransactionHookManagerTest {

    private final String DEFAULT_XID = "default_xid";

    @AfterEach
    public void clear() {
        TransactionHookManager.clear();
        TransactionHookManager.getGlobalHooks().clear();
    }

    @Test
    public void testRegisterHook() {
        TransactionHook transactionHook = new TransactionHook() {};
        TransactionHook transactionHook2 = new TransactionHook() {};
        TransactionHookManager.registerLocalHook(transactionHook);
        TransactionHookManager.registerGlobalHook(transactionHook2);
        List<TransactionHook> hooks = TransactionHookManager.getHooks();
        assertThat(hooks).isNotEmpty();
        assertThat(hooks.get(0)).isEqualTo(transactionHook2);
        assertThat(hooks.get(1)).isEqualTo(transactionHook);
    }

    @Test
    public void test_registerLocalHook_getGlobalHooks() {
        assertThat(TransactionHookManager.getGlobalHooks()).isEmpty();
        TransactionHookManager.registerGlobalHook(new TransactionHook() {});
        assertThat(TransactionHookManager.getGlobalHooks()).isNotEmpty();
    }

    @Test
    public void testGetHooks() {
        assertThat(TransactionHookManager.getHooks()).isEmpty();
        TransactionHookManager.registerLocalHook(new TransactionHook() {});
        assertThat(TransactionHookManager.getHooks()).isNotEmpty();
    }

    @Test
    public void testTriggerHooks() {
        TransactionHook transactionHook = new TransactionHook() {};
        TransactionHookManager.registerLocalHook(transactionHook);

        RootContext.bind(DEFAULT_XID);
        TransactionHookManager.triggerHooks(DEFAULT_XID, (hooks) -> {
            assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
            assertThat(hooks.get(0)).isEqualTo(transactionHook);
        });
        assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);

        RootContext.unbind();
        TransactionHookManager.triggerHooks(DEFAULT_XID, (hooks) -> {
            assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
            assertThat(hooks.get(0)).isEqualTo(transactionHook);
        });
        assertThat(RootContext.getXID()).isNull();
    }

    @Test
    public void testClear() {
        assertThat(TransactionHookManager.getHooks()).isEmpty();
        TransactionHookManager.registerLocalHook(new TransactionHook() {});
        assertThat(TransactionHookManager.getHooks()).isNotEmpty();
        TransactionHookManager.clear();
        assertThat(TransactionHookManager.getHooks()).isEmpty();
    }

    @Test
    public void testNPE() {
        Assertions.assertThrows(NullPointerException.class, () -> TransactionHookManager.registerLocalHook(null));
    }
}
