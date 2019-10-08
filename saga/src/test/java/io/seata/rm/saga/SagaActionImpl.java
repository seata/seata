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
package io.seata.rm.saga;

import io.seata.rm.saga.api.BusinessActionContext;

import java.util.List;

/**
 * The type Tcc action.
 *
 * @author zhangsen
 */
public class SagaActionImpl implements SagaAction {

    @Override
    public boolean prepare(BusinessActionContext actionContext,
                           int a,
                           List b,
                           SagaParam TccParam  ) {
        return true;
    }

    @Override
    public boolean rollback(BusinessActionContext actionContext) {
        return true;
    }
}
