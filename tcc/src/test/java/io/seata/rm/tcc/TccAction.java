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
package io.seata.rm.tcc;

import java.util.List;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.parameterfetcher.MockBooleanParameterFetcher;
import io.seata.rm.tcc.parameterfetcher.MockObjectParameterFetcher;

/**
 * The interface Tcc action.
 *
 * @author zhangsen
 */
@LocalTCC
public interface TccAction {

    /**
     * Prepare boolean.
     *
     * @param actionContext the action context
     * @param a             the int a
     * @param b             the list b
     * @param c             the array c
     * @param d             the object d
     * @param e             the object e
     * @param f             the object f
     * @param g             the boolean g
     * @param h             the boolean h
     * @return the boolean
     */
    @TwoPhaseBusinessAction(name = "tccActionForTest", commitMethod = "commit", rollbackMethod = "rollback")
    boolean prepare(BusinessActionContext actionContext,
                    @BusinessActionContextParameter("a") int a,
                    @BusinessActionContextParameter(paramName = "b", index = 0) List b,
                    @BusinessActionContextParameter(value = "c", index = 1) long[] c,
                    @BusinessActionContextParameter(isParamInProperty = true) TccParam d,
                    @BusinessActionContextParameter(paramName = "e", isParamInProperty = true) TccParam e,
                    @BusinessActionContextParameter(paramName = "f", isParamInProperty = true, fetcher = MockObjectParameterFetcher.class) TccParam f,
                    @BusinessActionContextParameter(paramName = "g", isParamInProperty = true, fetcher = MockBooleanParameterFetcher.class) boolean g,
                    @BusinessActionContextParameter(paramName = "h", isParamInProperty = true, fetcher = MockBooleanParameterFetcher.class) boolean h);

    /**
     * Commit boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    boolean commit(BusinessActionContext actionContext);

    /**
     * Rollback boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    boolean rollback(BusinessActionContext actionContext);
}
