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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import io.seata.common.util.CollectionUtils;

/**
 * @author guoyao
 */
public final class TransactionHookManager {

    private TransactionHookManager() {

    }

    private static final ThreadLocal<List<TransactionHook>> LOCAL_HOOKS = new ThreadLocal<>();

    /**
     * get the current hooks
     *
     * @return
     * @throws IllegalStateException
     */
    public static List<TransactionHook> getHooks() throws IllegalStateException {
        List<TransactionHook> hooks = LOCAL_HOOKS.get();

        if (hooks == null || hooks.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(hooks);
    }

    /**
     * add new hook
     *
     * @param transactionHook
     */
    public static void registerHook(TransactionHook transactionHook) {
        if (transactionHook == null) {
            throw new NullPointerException("transactionHook must not be null");
        }
        List<TransactionHook> transactionHooks = LOCAL_HOOKS.get();
        if (transactionHooks == null) {
            LOCAL_HOOKS.set(new ArrayList<>());
        }
        LOCAL_HOOKS.get().add(transactionHook);
    }

    /**
     * add new hooks
     *
     * @param transactionHook
     */
    public static void registerHooks(Set<TransactionHook> transactionHook) {
        if (CollectionUtils.isNotEmpty(transactionHook)) {
            List<TransactionHook> transactionHooks = LOCAL_HOOKS.get();
            if (transactionHooks == null) {
                LOCAL_HOOKS.set(new ArrayList<>());
            }
            LOCAL_HOOKS.get().addAll(transactionHook);
        }
    }

    /**
     * clear hooks
     */
    public static void clear() {
        LOCAL_HOOKS.remove();
    }

}
