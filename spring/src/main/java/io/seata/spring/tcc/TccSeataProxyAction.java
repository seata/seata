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
package io.seata.spring.tcc;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * Tcc Seata Proxy Action
 *
 * @author wang.liang
 * @see TccSeataProxyHandler
 * @see io.seata.spring.proxy.SeataProxyInterceptor
 */
@LocalTCC
public interface TccSeataProxyAction {

    /**
     * Prepare.
     *
     * @param actionContext the action context
     * @return the boolean
     * @throws Exception the Exception
     */
    @TwoPhaseBusinessAction(name = "tccSeataProxyAction", useTCCFence = false, commitMethod = "commit", rollbackMethod = "rollback")
    boolean prepare(BusinessActionContext actionContext) throws Exception;

    /**
     * Commit.
     *
     * @param actionContext the action context
     * @return the boolean
     * @throws Exception the Exception
     */
    boolean commit(BusinessActionContext actionContext) throws Exception;

    /**
     * Rollback.
     *
     * @param actionContext the action context
     * @return the boolean
     * @throws Exception the Exception
     */
    boolean rollback(BusinessActionContext actionContext) throws Exception;
}
