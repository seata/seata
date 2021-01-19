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

import com.alibaba.fastjson.JSON;
import io.seata.common.Constants;
import io.seata.common.exception.FrameworkException;
import io.seata.common.executor.Callback;
import io.seata.common.util.NetUtil;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.transaction.RMTransactionHookManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @return map map
     * @throws Throwable the throwable
     */
    public Map<String, Object> proceed(Method method, Object[] arguments, String xid, TwoPhaseBusinessAction businessAction,
                                       Callback<Object> targetCallback) throws Throwable {
        Map<String, Object> ret = new HashMap<>(4);

        //TCC name
        String actionName = businessAction.name();
        BusinessActionContext actionContext = new BusinessActionContext();
        actionContext.setXid(xid);
        //set action name
        actionContext.setActionName(actionName);

        //Creating Branch Record
        String branchId = doTccActionLogStore(method, arguments, businessAction, actionContext);
        actionContext.setBranchId(branchId);

        //set the parameter whose type is BusinessActionContext
        Class<?>[] types = method.getParameterTypes();
        int argIndex = 0;
        for (Class<?> cls : types) {
            if (cls.getName().equals(BusinessActionContext.class.getName())) {
                arguments[argIndex] = actionContext;
                break;
            }
            argIndex++;
        }
        //the final parameters of the try method
        ret.put(Constants.TCC_METHOD_ARGUMENTS, arguments);
        //the final result
        ret.put(Constants.TCC_METHOD_RESULT, targetCallback.execute());
        return ret;
    }

    /**
     * Creating Branch Record
     *
     * @param method         the method
     * @param arguments      the arguments
     * @param businessAction the business action
     * @param actionContext  the action context
     * @return the string
     */
    protected String doTccActionLogStore(Method method, Object[] arguments, TwoPhaseBusinessAction businessAction,
                                         BusinessActionContext actionContext) {
        String actionName = actionContext.getActionName();
        String xid = actionContext.getXid();
        //
        Map<String, Object> context = fetchActionRequestContext(method, arguments);
        context.put(Constants.ACTION_START_TIME, System.currentTimeMillis());

        //init business context
        initBusinessContext(context, method, businessAction);
        //Init running environment context
        initFrameworkContext(context);
        actionContext.setActionContext(context);

        //init applicationData
        Map<String, Object> applicationContext = new HashMap<>(4);
        applicationContext.put(Constants.TCC_ACTION_CONTEXT, context);
        String applicationContextStr = JSON.toJSONString(applicationContext);
        try {
            //registry branch record
            Long branchId = DefaultResourceManager.get().branchRegister(BranchType.TCC, actionName, null, xid,
                applicationContextStr, null);
            RMTransactionHookManager.setLocalBranchId(branchId);
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
        }
    }

    /**
     * Extracting context data from parameters, add them to the context
     *
     * @param method    the method
     * @param arguments the arguments
     * @return map map
     */
    protected Map<String, Object> fetchActionRequestContext(Method method, Object[] arguments) {
        Map<String, Object> context = new HashMap<>(8);

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                if (parameterAnnotations[i][j] instanceof BusinessActionContextParameter) {
                    BusinessActionContextParameter param = (BusinessActionContextParameter)parameterAnnotations[i][j];
                    if (arguments[i] == null) {
                        throw new IllegalArgumentException("@BusinessActionContextParameter 's params can not null");
                    }
                    Object paramObject = arguments[i];
                    int index = param.index();
                    //List, get by index
                    if (index >= 0) {
                        @SuppressWarnings("unchecked")
                        Object targetParam = ((List<Object>)paramObject).get(index);
                        if (param.isParamInProperty()) {
                            context.putAll(ActionContextUtil.fetchContextFromObject(targetParam));
                        } else {
                            context.put(param.paramName(), targetParam);
                        }
                    } else {
                        if (param.isParamInProperty()) {
                            context.putAll(ActionContextUtil.fetchContextFromObject(paramObject));
                        } else {
                            context.put(param.paramName(), paramObject);
                        }
                    }
                }
            }
        }
        return context;
    }

}
