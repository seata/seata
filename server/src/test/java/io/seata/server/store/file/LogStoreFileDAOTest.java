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
package io.seata.server.store.file;

import io.seata.core.store.BaseModel;
import io.seata.server.UUIDGenerator;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.file.TransactionWriteStore;
import io.seata.server.storage.file.session.FileSessionManager;
import io.seata.server.storage.file.store.LogStoreFileDAO;
import io.seata.server.store.StoreConfig;
import io.seata.server.store.TransactionStoreManager;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author ggndnn
 */
public class LogStoreFileDAOTest {
    @Test
    public void testBigDataWrite() throws Exception {
        File seataFile = Files.newTemporaryFile();
        LogStoreFileDAO fileDAO = null;
        try {
            Method writeSessionMethod = LogStoreFileDAO.class.getDeclaredMethod("writeSession",
                    TransactionStoreManager.LogOperation.class, BaseModel.class);
            writeSessionMethod.setAccessible(true);

            fileDAO = new LogStoreFileDAO(seataFile.getAbsolutePath());
            BranchSession branchSessionA = Mockito.mock(BranchSession.class);
            GlobalSession global = new GlobalSession();
            Mockito.when(branchSessionA.encode())
                    .thenReturn(createBigBranchSessionData(global, (byte) 'A'));
            Mockito.when(branchSessionA.getApplicationData())
                    .thenReturn(new String(createBigApplicationData((byte) 'A')));
            BranchSession branchSessionB = Mockito.mock(BranchSession.class);
            Mockito.when(branchSessionB.encode())
                    .thenReturn(createBigBranchSessionData(global, (byte) 'B'));
            Mockito.when(branchSessionB.getApplicationData())
                    .thenReturn(new String(createBigApplicationData((byte) 'B')));
            Assertions.assertTrue((boolean) writeSessionMethod.invoke(fileDAO, TransactionStoreManager.LogOperation.BRANCH_ADD, branchSessionA));
            Assertions.assertTrue((boolean) writeSessionMethod.invoke(fileDAO, TransactionStoreManager.LogOperation.BRANCH_ADD, branchSessionB));
            List<TransactionWriteStore> list = fileDAO.readWriteStore(2000, false);
            Assertions.assertNotNull(list);
            Assertions.assertEquals(2, list.size());
            BranchSession loadedBranchSessionA = (BranchSession) list.get(0).getSessionRequest();
            Assertions.assertEquals(branchSessionA.getApplicationData(), loadedBranchSessionA.getApplicationData());
            BranchSession loadedBranchSessionB = (BranchSession) list.get(1).getSessionRequest();
            Assertions.assertEquals(branchSessionB.getApplicationData(), loadedBranchSessionB.getApplicationData());
        } finally {
            if (fileDAO != null) {
                fileDAO.shutdown();
            }
            Assertions.assertTrue(seataFile.delete());
        }
    }

    @Test
    public void testFindTimeoutAndSave() throws Exception {
        File seataFile = Files.newTemporaryFile();
        Method findTimeoutAndSaveMethod = LogStoreFileDAO.class.getDeclaredMethod("findTimeoutAndSave");
        findTimeoutAndSaveMethod.setAccessible(true);
        FileSessionManager sessionManager = null;
        LogStoreFileDAO fileDAO = null;
        try {
            List<GlobalSession> timeoutSessions = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                GlobalSession globalSession = new GlobalSession("", "", "", 60000);
                BranchSession branchSessionA = Mockito.mock(BranchSession.class);
                Mockito.when(branchSessionA.encode())
                        .thenReturn(createBigBranchSessionData(globalSession, (byte) 'A'));
                Mockito.when(branchSessionA.getApplicationData())
                        .thenReturn(new String(createBigApplicationData((byte) 'A')));
                globalSession.addBranch(branchSessionA);
                BranchSession branchSessionB = Mockito.mock(BranchSession.class);
                Mockito.when(branchSessionB.encode())
                        .thenReturn(createBigBranchSessionData(globalSession, (byte) 'B'));
                Mockito.when(branchSessionB.getApplicationData())
                        .thenReturn(new String(createBigApplicationData((byte) 'B')));
                globalSession.addBranch(branchSessionB);
                timeoutSessions.add(globalSession);
            }
            fileDAO = new LogStoreFileDAO(seataFile.getAbsolutePath());
            Assertions.assertTrue((boolean) findTimeoutAndSaveMethod.invoke(fileDAO));

            sessionManager = new FileSessionManager(seataFile.getName(), seataFile.getParent());
            sessionManager.reload();
            Collection<GlobalSession> globalSessions = sessionManager.allSessions();
            Assertions.assertNotNull(globalSessions);
            globalSessions.forEach(g -> {
                Assertions.assertNotNull(g);
                List<BranchSession> branches = g.getBranchSessions();
                Assertions.assertEquals(2, branches.size());
                Assertions.assertEquals(new String(createBigApplicationData((byte) 'A')), branches.get(0).getApplicationData());
                Assertions.assertEquals(new String(createBigApplicationData((byte) 'B')), branches.get(1).getApplicationData());
            });
        } finally {
            findTimeoutAndSaveMethod.setAccessible(false);
            if (fileDAO != null) {
                fileDAO.shutdown();
            }
            if (sessionManager != null) {
                sessionManager.destroy();
            }
            Assertions.assertTrue(seataFile.delete());
        }
    }

    private byte[] createBigBranchSessionData(GlobalSession global, byte c) {
        int bufferSize = StoreConfig.getFileWriteBufferCacheSize()
                + 8 // trascationId
                + 8 // branchId
                + 2 // resourceIdBytes.length
                + 2 // lockKeyBytes.length
                + 2 // clientIdBytes.length
                + 4 // applicationDataBytes.length
                + 2 // xidBytes.length
                + 1 // statusCode
                + 1;// branchType
        String xid = global.getXid();
        byte[] xidBytes = null;
        if (xid != null) {
            xidBytes = xid.getBytes();
            bufferSize += xidBytes.length;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
        byteBuffer.putLong(global.getTransactionId()); // trascationId 8
        byteBuffer.putLong(UUIDGenerator.generateUUID()); // branchId 8
        byteBuffer.putShort((short) 0); // resourceIdBytes.length 2
        byteBuffer.putShort((short) 0); // lockKeyBytes.length 2
        byteBuffer.putShort((short) 0); // clientIdBytes.length 2
        byte[] applicationDataBytes = createBigApplicationData(c);
        byteBuffer.putInt(applicationDataBytes.length); // applicationDataBytes.length 4
        byteBuffer.put(applicationDataBytes); // applicationDataBytes
        if (xidBytes != null) {
            byteBuffer.putShort((short) xidBytes.length); // xidBytes.length 2
            byteBuffer.put(xidBytes); // xidBytes
        } else {
            byteBuffer.putShort((short) 0); // xidBytes.length 2
        }
        byteBuffer.put((byte) 0); // statusCode 1
        byteBuffer.put((byte) 0); // branchType 1
        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.limit()];
        byteBuffer.get(bytes);
        return bytes;
    }

    private byte[] createBigApplicationData(byte c) {
        int applicationDataSize = StoreConfig.getFileWriteBufferCacheSize();
        byte[] applicationDataBytes = new byte[applicationDataSize];
        for (int i = 0; i < applicationDataSize; i++) {
            applicationDataBytes[i] = c;
        }
        return applicationDataBytes;
    }
}