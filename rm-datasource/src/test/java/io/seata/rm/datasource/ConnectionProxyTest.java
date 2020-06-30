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
package io.seata.rm.datasource;

import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchType;
import io.seata.core.model.ResourceManager;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.exec.LockConflictException;
import io.seata.rm.datasource.exec.LockWaitTimeoutException;
import io.seata.rm.datasource.sql.struct.LockKey;
import io.seata.rm.datasource.undo.SQLUndoLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * ConnectionProxy test
 *
 * @author ggndnn
 */
public class ConnectionProxyTest {
    private DataSourceProxy dataSourceProxy;

    private final static String TEST_RESOURCE_ID = "testResourceId";

    private final static String TEST_XID = "testXid";

    private final static List<LockKey> lockKeys = new ArrayList<>();

    private Field branchRollbackFlagField;

    @BeforeEach
    public void initBeforeEach() throws Exception {
        branchRollbackFlagField = ConnectionProxy.LockRetryPolicy.class.getDeclaredField("LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT");
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(branchRollbackFlagField, branchRollbackFlagField.getModifiers() & ~Modifier.FINAL);
        branchRollbackFlagField.setAccessible(true);
        boolean branchRollbackFlag = (boolean) branchRollbackFlagField.get(null);
        Assertions.assertTrue(branchRollbackFlag);

        dataSourceProxy = Mockito.mock(DataSourceProxy.class);
        Mockito.when(dataSourceProxy.getResourceId())
                .thenReturn(TEST_RESOURCE_ID);
        ResourceManager rm = Mockito.mock(ResourceManager.class);
        Mockito.when(rm.branchRegister(BranchType.AT, dataSourceProxy.getResourceId(), null, TEST_XID, null, "order:123"))
                .thenThrow(new TransactionException(TransactionExceptionCode.LockKeyConflict));
        DefaultResourceManager defaultResourceManager = DefaultResourceManager.get();
        Assertions.assertNotNull(defaultResourceManager);
        DefaultResourceManager.mockResourceManager(BranchType.AT, rm);
    }

    @Test
    public void testLockRetryPolicyRollbackOnConflict() throws Exception {
        boolean oldBranchRollbackFlag = (boolean) branchRollbackFlagField.get(null);
        branchRollbackFlagField.set(null, true);
        ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, null);
        connectionProxy.bind(TEST_XID);
        connectionProxy.appendUndoLog(new SQLUndoLog());
        LockKey lockKey = new LockKey();
        lockKey.setTableName("order");
        lockKey.setPk("123");
        lockKeys.add(lockKey);
        connectionProxy.appendLockKey(lockKeys);
        Assertions.assertThrows(LockConflictException.class, connectionProxy::commit);
        branchRollbackFlagField.set(null, oldBranchRollbackFlag);
    }

    @Test
    public void testLockRetryPolicyNotRollbackOnConflict() throws Exception {
        boolean oldBranchRollbackFlag = (boolean) branchRollbackFlagField.get(null);
        branchRollbackFlagField.set(null, false);
        ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, null);
        connectionProxy.bind(TEST_XID);
        connectionProxy.appendUndoLog(new SQLUndoLog());
        LockKey lockKey = new LockKey();
        lockKey.setTableName("order");
        lockKey.setPk("123");
        lockKeys.add(lockKey);
        connectionProxy.appendLockKey(lockKeys);
        Assertions.assertThrows(LockWaitTimeoutException.class, connectionProxy::commit);
        branchRollbackFlagField.set(null, oldBranchRollbackFlag);
    }
}
