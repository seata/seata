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

public aspect RunnableTransactionPropagationAspect {

    public interface RunnableTransactionPropagation {}
    private String RunnableTransactionPropagation.xid = null;

    public void RunnableTransactionPropagation.init() {
        this.xid = RootContext.getXID();
    }
    public String RunnableTransactionPropagation.xid() {
        return this.xid;
    }

    declare parents : java.lang.Runnable+ implements RunnableTransactionPropagation;

    pointcut init() : execution(java.lang.Runnable+.new(..));
    pointcut cut(): (execution(* *..run()));

    after(RunnableTransactionPropagation m): init() && this(m) {
        m.init();
    }

    before(RunnableTransactionPropagation m): cut() && this(m) {
        String xid = m.xid();
        if (NonPropagateRunnable.class.isAssignableFrom(m.getClass()))  {
            RootContext.unbind();
        } else if (ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.CLIENT_THREAD_PROPAGATE, false) && xid != null){
            RootContext.unbind();
            RootContext.bind(xid);
        } else {
            RootContext.unbind();
        }
    }
}
