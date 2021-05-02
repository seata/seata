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
package io.seata.rm;

import io.seata.core.context.GlobalLockConfigHolder;
import io.seata.core.context.RootContext;
import io.seata.core.model.GlobalLockConfig;

/**
 * executor template for local transaction which need global lock
 * @author selfishlover
 */
public class GlobalLockTemplate {

    public Object execute(GlobalLockExecutor executor) throws Throwable {
        boolean alreadyInGlobalLock = RootContext.requireGlobalLock();
        if (!alreadyInGlobalLock) {
            RootContext.bindGlobalLockFlag();
        }

        // set my config to config holder so that it can be access in further execution
        // for example, LockRetryController can access it with config holder
        GlobalLockConfig myConfig = null;
        GlobalLockConfig previousConfig = null;

        try {
            myConfig = executor.getGlobalLockConfig();
            previousConfig = GlobalLockConfigHolder.setAndReturnPrevious(myConfig);
            return executor.execute();
        } finally {
            // only unbind when this is the root caller.
            // otherwise, the outer caller would lose global lock flag
            if (!alreadyInGlobalLock) {
                RootContext.unbindGlobalLockFlag();
            }

            // if previous config is not null, we need to set it back
            // so that the outer logic can still use their config
            if (previousConfig != null) {
                GlobalLockConfigHolder.setAndReturnPrevious(previousConfig);
            } else {
                GlobalLockConfigHolder.remove();
            }
        }
    }
}
