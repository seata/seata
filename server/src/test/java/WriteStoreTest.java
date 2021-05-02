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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.session.SessionManager;
import io.seata.server.storage.file.ReloadableStore;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import io.seata.server.store.TransactionStoreManager.LogOperation;
import io.seata.server.storage.file.TransactionWriteStore;
import io.seata.server.storage.file.store.FileTransactionStoreManager;


/**
 * The type Write store test.
 *
 * @author slievrly
 * write  cost:87281,read cost:158922   65535*5  1000 per open  init 1024 write cost:86454,read
 * cost:160541   65535*5  2000 per open  init 1024 write cost:82953,read cost:157736   65535*5  2000 per open  init
 * 65535*5*9 write cost:115079,read cost:163664   65535*5  2000 per open  init 65535*5*9  schedule flush 10||2s
 */
public class WriteStoreTest {
    private static String vgroup = "vgroupMock";
    private static String appname = "appnameMock";
    private static String instname = "seataMocK";
    private static int trx_num = 65535 * 5;
    private static int trx_begin = 0;

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws InterruptedException the interrupted exception
     * @throws IOException the io exception
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        TransactionStoreManager transactionStoreManager = new FileTransactionStoreManager(
        "~/Documents/test/data",
            new SessionManager() {
                @Override
                public void destroy() {

                }

                @Override
                public void addGlobalSession(GlobalSession session) throws TransactionException {

                }

                @Override
                public GlobalSession findGlobalSession(String xid)  {
                    return null;
                }

                @Override
                public GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
                    return null;
                }

                @Override
                public void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status)
                    throws TransactionException {

                }

                @Override
                public void removeGlobalSession(GlobalSession session) throws TransactionException {

                }

                @Override
                public void addBranchSession(GlobalSession globalSession, BranchSession session)
                    throws TransactionException {

                }

                @Override
                public void updateBranchSessionStatus(BranchSession session, BranchStatus status)
                    throws TransactionException {

                }

                @Override
                public void removeBranchSession(GlobalSession globalSession, BranchSession session)
                    throws TransactionException {

                }

                @Override
                public Collection<GlobalSession> allSessions() {
                    return null;
                }

                @Override
                public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
                    List<GlobalSession> globalSessions = new ArrayList<>();
                    int begin = 10000;
                    int num = 1000;
                    for (int i = begin; i < begin + num; i++) {
                        BranchSession branchSession1 = new BranchSession();
                        branchSession1.setTransactionId(i);
                        branchSession1.setBranchId(begin + num + (i - begin) * 2);
                        branchSession1.setResourceId("mockDbkeY1");

                        BranchSession branchSession2 = new BranchSession();
                        branchSession2.setTransactionId(i);
                        branchSession2.setBranchId(begin + num + (i - begin) * 2 + 1);
                        branchSession2.setResourceId("mockDbkeY2");

                        GlobalSession globalSession = new GlobalSession(appname, vgroup, instname, 60000);
                        try {
                            globalSession.add(branchSession1);
                            globalSession.add(branchSession2);
                            globalSessions.add(globalSession);
                        } catch (Exception exx) {}
                    }
                    return globalSessions;

                }

                @Override
                public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
                        throws TransactionException {
                    return null;
                }

                @Override
                public void onBegin(GlobalSession globalSession) throws TransactionException {

                }

                @Override
                public void onStatusChange(GlobalSession globalSession, GlobalStatus status)
                    throws TransactionException {

                }

                @Override
                public void onBranchStatusChange(GlobalSession globalSession, BranchSession branchSession,
                                                 BranchStatus status) throws TransactionException {

                }

                @Override
                public void onAddBranch(GlobalSession globalSession, BranchSession branchSession)
                    throws TransactionException {

                }

                @Override
                public void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession)
                    throws TransactionException {

                }

                @Override
                public void onClose(GlobalSession globalSession) throws TransactionException {

                }

                @Override
                public void onEnd(GlobalSession globalSession) throws TransactionException {

                }

                @Override
                public void cleanGlobalSession(List<GlobalSession> sessions) throws TransactionException {
                    
                }

                @Override
                public void cleanBranchSession(List<BranchSession> sessions) throws TransactionException {
                    
                }

            });
        long beginWriteMills = System.currentTimeMillis();
        write(transactionStoreManager);
        long endWriteMills = System.currentTimeMillis();
        Thread.sleep(10 * 1000);
        long beginReadMills = System.currentTimeMillis();
        Map<SessionStorable, LogOperation> resultMap = readAll(transactionStoreManager);
        long endReadMills = System.currentTimeMillis();
        if ((resultMap.size() % (65535)) % 3000 == 0) {
            System.out.print("check success");
        } else {
            System.out.print("check failed");
        }
        System.out.print(
            "write cost:" + (endWriteMills - beginWriteMills) + ",read cost:" + (endReadMills - beginReadMills));

    }

    private static void write(TransactionStoreManager transactionStoreManager) {
        for (int i = trx_begin; i < trx_begin + trx_num; i++) {
            GlobalSession globalSession = new GlobalSession(appname, vgroup, instname, 60000);
            transactionStoreManager.writeSession(LogOperation.GLOBAL_ADD, globalSession);

            BranchSession branchSession1 = new BranchSession();
            branchSession1.setTransactionId(globalSession.getTransactionId());
            branchSession1.setBranchId(trx_begin + trx_num + (i - trx_begin) * 2);
            branchSession1.setResourceId("mockDbkeY1");
            transactionStoreManager.writeSession(LogOperation.BRANCH_ADD, branchSession1);
            transactionStoreManager.writeSession(LogOperation.BRANCH_UPDATE, branchSession1);
            transactionStoreManager.writeSession(LogOperation.BRANCH_REMOVE, branchSession1);

            BranchSession branchSession2 = new BranchSession();
            branchSession2.setTransactionId(globalSession.getTransactionId());
            branchSession2.setBranchId(trx_begin + (i - trx_begin) + i * 2 + 1);
            branchSession2.setResourceId("mockDbkeY2");
            transactionStoreManager.writeSession(LogOperation.BRANCH_ADD, branchSession2);
            transactionStoreManager.writeSession(LogOperation.BRANCH_UPDATE, branchSession2);
            transactionStoreManager.writeSession(LogOperation.BRANCH_REMOVE, branchSession2);

            transactionStoreManager.writeSession(LogOperation.GLOBAL_UPDATE, globalSession);
            transactionStoreManager.writeSession(LogOperation.GLOBAL_REMOVE, globalSession);
        }
    }

    private static Map<SessionStorable, LogOperation> readAll(TransactionStoreManager transactionStoreManager) {
        Map<SessionStorable, LogOperation> resultMap = new HashMap<>(65535 * 5 * 9);
        while (((ReloadableStore)transactionStoreManager).hasRemaining(true)) {
            List<TransactionWriteStore> transactionWriteStores = ((ReloadableStore)transactionStoreManager).readWriteStore(2000,
                true);
            if (transactionWriteStores != null) {
                for (TransactionWriteStore transactionWriteStore : transactionWriteStores) {
                    printLog(transactionWriteStore);
                    resultMap.put(transactionWriteStore.getSessionRequest(), transactionWriteStore.getOperate());
                }
            }
        }
        while (((ReloadableStore)transactionStoreManager).hasRemaining(false)) {
            List<TransactionWriteStore> transactionWriteStores = ((ReloadableStore)transactionStoreManager).readWriteStore(2000,
                false);
            if (transactionWriteStores != null) {
                for (TransactionWriteStore transactionWriteStore : transactionWriteStores) {
                    printLog(transactionWriteStore);
                    resultMap.put(transactionWriteStore.getSessionRequest(), transactionWriteStore.getOperate());
                }
            }
        }
        return resultMap;
    }

    private static void printLog(TransactionWriteStore transactionWriteStore) {
        if (transactionWriteStore.getSessionRequest() instanceof GlobalSession) {
            GlobalSession globalSession = (GlobalSession)transactionWriteStore.getSessionRequest();
            System.out.print(
                "xid:" + globalSession.getTransactionId() + "," + globalSession.getApplicationId() + "," + globalSession
                    .getTransactionServiceGroup() + "," + globalSession.getTransactionName() + "," + globalSession
                    .getTimeout());
        } else {
            BranchSession branchSession = (BranchSession)transactionWriteStore.getSessionRequest();
            System.out.print(
                "xid:" + branchSession.getTransactionId() + ",branchId:" + branchSession.getBranchId() + ","
                    + branchSession.getResourceId());
        }
        System.out.println(",op:" + transactionWriteStore.getOperate().name());
    }
}
