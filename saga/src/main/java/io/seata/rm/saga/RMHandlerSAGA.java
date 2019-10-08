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

import io.seata.core.model.BranchType;
import io.seata.core.model.ResourceManager;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;
import io.seata.rm.AbstractRMHandler;
import io.seata.rm.DefaultResourceManager;

/**
 * The type Rm handler saga.
 *
 * @author zhangsen
 */
public class RMHandlerSAGA extends AbstractRMHandler {

    @Override
    public void handle(UndoLogDeleteRequest request) {
        //DO nothing
    }

    /**
     * get TCC resource manager
     *
     * @return
     */
    @Override
    protected ResourceManager getResourceManager() {
        return DefaultResourceManager.get().getResourceManager(BranchType.SAGA);
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.SAGA;
    }

}
