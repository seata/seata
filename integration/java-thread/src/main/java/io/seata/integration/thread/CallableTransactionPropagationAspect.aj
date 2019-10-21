/**
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
package io.seata.integration.thread;

import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.RootContext;

public aspect CallableTransactionPropagationAspect {

    public interface CallableTransactionPropagation {}
    private String CallableTransactionPropagation.xid = null;

    public void CallableTransactionPropagation.init() {
        this.xid = RootContext.getXID();
    }
    public String CallableTransactionPropagation.xid() {
        return this.xid;
    }

    declare parents : java.util.concurrent.Callable+ implements CallableTransactionPropagation;

    pointcut init() : execution(java.util.concurrent.Callable+.new(..));
    pointcut cut(): (execution(* *..call()));

    after(CallableTransactionPropagation m): init() && this(m) {
        m.init();
    }

    before(CallableTransactionPropagation m): cut() && this(m) {
        String xid = m.xid();
        if (NonPropagateCallable.class.isAssignableFrom(m.getClass())) {
            RootContext.unbind();
        } else if (ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.CLIENT_THREAD_PROPAGATE, false) && xid != null){
            RootContext.unbind();
            RootContext.bind(xid);
        } else {
            RootContext.unbind();
        }
    }
}
