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

import io.seata.common.util.CollectionUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Resource manager transaction hook manager
 *
 * @author wang.liang
 */
public final class RMTransactionHookManager {

    private RMTransactionHookManager() {
    }

    //global hooks
    private static final List<RMTransactionHook> GLOBAL_HOOKS = new ArrayList<>();
    //branch hooks
    private static final ConcurrentMap<Long, List<RMTransactionHook>> BRANCH_HOOKS = new ConcurrentHashMap<>();
    //local branchId, hooks
    private static final ThreadLocal<Long> LOCAL_BRANCH_ID = new ThreadLocal<>();
    private static final ThreadLocal<List<RMTransactionHook>> LOCAL_HOOKS = new ThreadLocal<>();

    /**
     * get global hooks
     */
    public static List<RMTransactionHook> getGlobalHooks() {
        return GLOBAL_HOOKS;
    }

    /**
     * get branch hooks
     *
     * @param branchId the branch id
     */
    public static List<RMTransactionHook> getBranchHooks(long branchId) {
        return BRANCH_HOOKS.get(branchId);
    }

    /**
     * get local hooks
     */
    public static List<RMTransactionHook> getLocalHooks() {
        return LOCAL_HOOKS.get();
    }

    /**
     * remove branch hooks
     *
     * @param branchId the branch id
     */
    public static void removeBranchHooks(long branchId) {
        BRANCH_HOOKS.remove(branchId);
    }

    /**
     * get local branch id
     */
    public static Long getLocalBranchId() {
        return LOCAL_BRANCH_ID.get();
    }

    /**
     * set local branch id
     *
     * @param branchId the branch id
     */
    public static void setLocalBranchId(long branchId) {
        LOCAL_BRANCH_ID.set(branchId);
        localHooksToBranchHooks(branchId);
    }

    /**
     * local hooks to branch hooks
     *
     * @param branchId the branch id
     */
    public static void localHooksToBranchHooks(long branchId) {
        List<RMTransactionHook> localHooks = LOCAL_HOOKS.get();
        if (localHooks != null && !localHooks.isEmpty()) {
            BRANCH_HOOKS.put(branchId, localHooks);
            LOCAL_HOOKS.remove();
        }
    }

    /**
     * get the hooks
     *
     * @param branchId the branch id
     */
    private static List<RMTransactionHook> getHooks(long branchId) {
        List<RMTransactionHook> hooks = getBranchHooks(branchId);
        if (hooks == null || hooks.isEmpty()) {
            hooks = GLOBAL_HOOKS;
        } else {
            hooks = new ArrayList<>(hooks);
            hooks.addAll(0, GLOBAL_HOOKS);
        }
        return Collections.unmodifiableList(hooks);
    }

    /**
     * add new global hook
     */
    public static void registerGlobalHook(RMTransactionHook rmTransactionHook) {
        if (rmTransactionHook == null) {
            throw new NullPointerException("RM transactionHook must be not null");
        }
        GLOBAL_HOOKS.add(rmTransactionHook);
    }

    /**
     * add new local hook
     */
    public static void registerLocalHook(RMTransactionHook rmTransactionHook) {
        if (rmTransactionHook == null) {
            throw new NullPointerException("RM transactionHook must be not null");
        }

        // get list
        Long branchId = getLocalBranchId();
        List<RMTransactionHook> list;
        if (branchId != null) {
            list = getBranchHooks(branchId);
            if (list == null) {
                list = new ArrayList<>();
                BRANCH_HOOKS.put(branchId, list);
            }
        } else {
            list = LOCAL_HOOKS.get();
            if (list == null) {
                list = new ArrayList<>();
                LOCAL_HOOKS.set(list);
            }
        }

        // add to list
        list.add(rmTransactionHook);
    }

    /**
     * trigger hooks
     *
     * @param logger   the logger in the trigger
     * @param branchId the branch id
     * @param consumer the hook consumer
     */
    public static void triggerHooks(Logger logger, long branchId, Consumer<RMTransactionHook> consumer) {
        List<RMTransactionHook> hooks = getHooks(branchId);
        if (CollectionUtils.isNotEmpty(hooks)) {
            for (RMTransactionHook hook : hooks) {
                try {
                    consumer.accept(hook);
                } catch (Exception e) {
                    if (logger != null) {
                        logger.error("execute rm transaction hook failed: branchId={}", branchId, e);
                    }
                }
            }
        }
    }

    /**
     * clear local hooks
     */
    public static void clear() {
        LOCAL_HOOKS.remove();
        LOCAL_BRANCH_ID.remove();
    }
}
