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
package io.seata.rm.tcc.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import io.seata.common.Constants;
import io.seata.common.exception.FrameworkException;
import io.seata.common.executor.Callback;
import io.seata.common.util.NetUtil;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.core.model.CommitType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.tcc.TCCFenceHandler;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Handler the TCC Participant Aspect : Setting Context, Creating Branch Record
 *
 * @author zhangsen
 */
public class ActionInterceptorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionInterceptorHandler.class);

    /**
     * Handler the TCC Aspect
     *
     * @param method         the method
     * @param arguments      the arguments
     * @param businessAction the business action
     * @param targetCallback the target callback
     * @return the business result
     * @throws Throwable the throwable
     */
    public Object proceed(Method method, Object[] arguments, String xid, TwoPhaseBusinessAction businessAction,
                                       Callback<Object> targetCallback) throws Throwable {
        //TCC name
        String actionName = businessAction.name();
        BusinessActionContext actionContext = new BusinessActionContext();
        actionContext.setXid(xid);
        //set action name
        actionContext.setActionName(actionName);

        //Creating Branch Record
        String branchId = doTccActionLogStore(method, arguments, businessAction, actionContext);
        actionContext.setBranchId(branchId);
        //MDC put branchId
        MDC.put(RootContext.MDC_KEY_BRANCH_ID, branchId);

        //set the parameter whose type is BusinessActionContext
        Class<?>[] types = method.getParameterTypes();
        int argIndex = 0;
        for (Class<?> cls : types) {
            if (cls.isAssignableFrom(BusinessActionContext.class)) {
                arguments[argIndex] = actionContext;
                break;
            }
            argIndex++;
        }

        // save the previous action context
        BusinessActionContext previousActionContext = BusinessActionContextUtil.getContext();
        try {
            //share actionContext implicitly
            BusinessActionContextUtil.setContext(actionContext);

            if (businessAction.useTCCFence()) {
                try {
                    // Use TCC Fence, and return the business result
                    return TCCFenceHandler.prepareFence(xid, Long.valueOf(branchId), actionName, targetCallback);
                } catch (FrameworkException | UndeclaredThrowableException e) {
                    Throwable originException = e.getCause();
                    if (originException instanceof FrameworkException) {
                        LOGGER.error("[{}] prepare TCC fence error: {}", xid, originException.getMessage());
                    }
                    throw originException;
                }
            } else {
                //Execute business, and return the business result
                return targetCallback.execute();
            }
        } finally {
            try {
                //to report business action context finally if the actionContext.getUpdated() is true
                BusinessActionContextUtil.reportContext(actionContext);
            } finally {
                if (previousActionContext != null) {
                    // recovery the previous action context
                    BusinessActionContextUtil.setContext(previousActionContext);
                } else {
                    // clear the action context
                    BusinessActionContextUtil.clear();
                }
            }
        }
    }

    /**
     * Creating Branch Record
     *
     * @param method         the method
     * @param arguments      the arguments
     * @param businessAction the business action
     * @param actionContext  the action context
     * @return the branchId
     */
    protected String doTccActionLogStore(Method method, Object[] arguments, TwoPhaseBusinessAction businessAction,
                                         BusinessActionContext actionContext) {
        String actionName = actionContext.getActionName();
        String xid = actionContext.getXid();

        Map<String, Object> context = fetchActionRequestContext(method, arguments);
        context.put(Constants.ACTION_START_TIME, System.currentTimeMillis());

        //Init business context
        initBusinessContext(context, method, businessAction);
        //Init running environment context
        initFrameworkContext(context);
        actionContext.setDelayReport(businessAction.isDelayReport());
        actionContext.setActionContext(context);

        //Init applicationData
        Map<String, Object> applicationContext = Collections.singletonMap(Constants.TCC_ACTION_CONTEXT, context);
        String applicationContextStr = JSON.toJSONString(applicationContext);
        try {
            //registry branch record
            CommitType commitType = businessAction.commitType().getCommitType();
            Long branchId = DefaultResourceManager.get().branchRegister(BranchType.TCC,
                    commitType, actionName, null, xid, applicationContextStr, null);
            return String.valueOf(branchId);
        } catch (Throwable t) {
            String msg = String.format("TCC branch Register error, xid: %s", xid);
            LOGGER.error(msg, t);
            throw new FrameworkException(t, msg);
        }
    }

    /**
     * Init running environment context
     *
     * @param context the context
     */
    protected void initFrameworkContext(Map<String, Object> context) {
        try {
            context.put(Constants.HOST_NAME, NetUtil.getLocalIp());
        } catch (Throwable t) {
            LOGGER.warn("getLocalIP error", t);
        }
    }

    /**
     * Init business context
     *
     * @param context        the context
     * @param method         the method
     * @param businessAction the business action
     */
    protected void initBusinessContext(Map<String, Object> context, Method method,
                                       TwoPhaseBusinessAction businessAction) {
        if (method != null) {
            //the phase one method name
            context.put(Constants.PREPARE_METHOD, method.getName());
        }
        if (businessAction != null) {
            //the phase two method name
            context.put(Constants.COMMIT_METHOD, businessAction.commitMethod());
            context.put(Constants.ROLLBACK_METHOD, businessAction.rollbackMethod());
            context.put(Constants.ACTION_NAME, businessAction.name());
            context.put(Constants.USE_TCC_FENCE, businessAction.useTCCFence());
        }
    }

    /**
     * Extracting context data from parameters, add them to the context
     *
     * @param method    the method
     * @param arguments the arguments
     * @return the context
     */
    protected Map<String, Object> fetchActionRequestContext(Method method, Object[] arguments) {
        Map<String, Object> context = new HashMap<>(8);

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                if (parameterAnnotations[i][j] instanceof BusinessActionContextParameter) {
                    // get annotation
                    BusinessActionContextParameter annotation = (BusinessActionContextParameter)parameterAnnotations[i][j];
                    if (arguments[i] == null) {
                        throw new IllegalArgumentException("@BusinessActionContextParameter 's params can not null");
                    }

                    // get param
                    Object paramObject = arguments[i];
                    if (paramObject == null) {
                        continue;
                    }

                    // load param by the config of annotation, and then put to the context
                    ActionContextUtil.loadParamByAnnotationAndPutToContext("param", "", paramObject, annotation, context);
                }
            }
        }
        return context;
    }
}
