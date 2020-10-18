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
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.GlobalCondition;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionManager;
import io.seata.server.store.TransactionStoreManager;
import io.seata.server.storage.file.store.FileTransactionStoreManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * The interface Session storable.
 *
 * @author lizhao
 */
public class WriteStoreMultithreadTest {
    private static String vgroup = "vgroupMock";
    private static String appname = "appnameMock";
    private static String instname = "seataMock";
    private static int per_thread_trx_num = 65535;
    private static int threadNum = 16;
    private static CountDownLatch countDownLatch = new CountDownLatch(threadNum);
    public static void main(String[] args) throws Exception {
        TransactionStoreManager transactionStoreManager = new FileTransactionStoreManager(
                "data",
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
                    public List<GlobalSession> findGlobalSessions(GlobalCondition condition) {
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
                });
        long beginWriteMills = System.currentTimeMillis();
        for (int i = 0; i < threadNum; i++) {
            final int threadNo = i;
            Thread thread = new Thread(() -> {write(transactionStoreManager, threadNo);});
            thread.start();
        }
        countDownLatch.await();
        long endWriteMills = System.currentTimeMillis();
        System.out.println("thread nums:" + threadNum + ", per_thread_trx_num:" + per_thread_trx_num +" ,cost" + (endWriteMills-beginWriteMills));
    }

    private static void write(TransactionStoreManager transactionStoreManager, int threadNo) {
        int trx_begin = threadNo * per_thread_trx_num;
        for (int i = trx_begin; i < trx_begin + per_thread_trx_num; i++) {
            GlobalSession globalSession = new GlobalSession(appname, vgroup, instname, 60000);
            transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, globalSession);

            BranchSession branchSession1 = new BranchSession();
            branchSession1.setTransactionId(globalSession.getTransactionId());
            branchSession1.setBranchId(trx_begin + per_thread_trx_num + (i - trx_begin) * 2);
            branchSession1.setResourceId("mockDbkeY1");
            transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, branchSession1);
            transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_UPDATE, branchSession1);
            transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_REMOVE, branchSession1);

            BranchSession branchSession2 = new BranchSession();
            branchSession2.setTransactionId(globalSession.getTransactionId());
            branchSession2.setBranchId(trx_begin + (i - trx_begin) + i * 2 + 1);
            branchSession2.setResourceId("mockDbkeY2");
            transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, branchSession2);
            transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_UPDATE, branchSession2);
            transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_REMOVE, branchSession2);

            transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_UPDATE, globalSession);
            transactionStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_REMOVE, globalSession);
        }
        countDownLatch.countDown();
    }
}
