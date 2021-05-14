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
package io.seata.rm.tcc.api;

import com.alibaba.fastjson.JSON;
import io.seata.common.exception.FrameworkException;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * the api of sharing business action context to tcc phase 2
 *
 * @author tanzj
 * @date 2021/4/16
 */
public final class BusinessActionContextUtil {

    private BusinessActionContextUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessActionContextUtil.class);

    private static final ThreadLocal<BusinessActionContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * add business action context and share it to tcc phase2
     *
     * @param key   the key of new context
     * @param value new context
     */
    public static void addContext(String key, Object value) {
        Map<String, Object> newContext = new HashMap<>(1, 1);
        newContext.put(key, value);
        addContext(newContext);
    }

    /**
     * batch share new context to tcc phase 2
     *
     * @param context the new context
     */
    @SuppressWarnings("deprecation")
    public static void addContext(Map<String, Object> context) {
        if (context != null) {
            BusinessActionContext actionContext = getContext();
            context.forEach((key, value) -> {
                if (!Objects.isNull(value)) {
                    actionContext.setUpdated(true);
                    actionContext.addActionContext(key, handleValue(value));
                }
            });
            //if delay report, params will be finally reported after phase 1 execution
            if (Boolean.TRUE.equals(actionContext.getDelayReport())) {
                return;
            }
            reportContext(actionContext);
        }
    }

    /**
     * Handle the value.
     * It is convenient to convert type in phase 2.
     *
     * @param value the value
     * @return the value or json string
     * @see BusinessActionContext#getActionContext(String, Class)
     */
    private static Object handleValue(@Nonnull Object value) {
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return value;
        } else {
            return JSON.toJSONString(value);
        }
    }

    /**
     * to do branch report sharing actionContext
     *
     * @param actionContext the context
     */
    public static void reportContext(BusinessActionContext actionContext) {
        try {
            DefaultResourceManager.get().branchReport(
                    BranchType.TCC,
                    actionContext.getXid(),
                    actionContext.getBranchId(),
                    BranchStatus.Registered,
                    JSON.toJSONString(actionContext)
            );
        } catch (TransactionException e) {
            String msg = String.format("TCC branch update error, xid: %s", actionContext.getXid());
            LOGGER.error("{}, error: {}", msg, e.getMessage());
            throw new FrameworkException(e, msg);
        }
    }

    public static BusinessActionContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    public static void setContext(BusinessActionContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
