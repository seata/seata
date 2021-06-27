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

/**
 * The type Tcc action.
 *
 * @author zhangsen
 */
public class TccActionImpl implements TccAction {

    @Override
    public boolean prepare(BusinessActionContext actionContext,
                           int a,
                           List b,
                           long[] c,
                           TccParam d,
                           TccParam e,
                           TccParam f,
                           boolean g,
                           boolean h) {
        return true;
    }

    @Override
    public boolean commit(BusinessActionContext actionContext) {
        return true;
    }

    @Override
    public boolean rollback(BusinessActionContext actionContext) {
        return true;
    }
}
