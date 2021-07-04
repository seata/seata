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
package io.seata.core.lock;

import java.util.List;
import io.seata.core.model.LockStatus;

/**
 * The type Local db locker.
 *
 * @author zhangsen
 */
public class LocalDBLocker extends AbstractLocker {

    @Override
    public boolean acquireLock(List<RowLock> rowLock) {
        return false;
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLock, boolean autoCommit) {
        return false;
    }

    @Override
    public boolean releaseLock(List<RowLock> rowLock) {
        return false;
    }

    @Override
    public boolean isLockable(List<RowLock> rowLock) {
        return false;
    }

    @Override
    public boolean updateLockStatus(String xid, LockStatus lockStatus) {
        return false;
    }

}
