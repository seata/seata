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
import io.seata.core.store.BaseModel;
import io.seata.core.store.LogStore;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.file.store.LogStoreFileDAO;
import io.seata.server.store.TransactionStoreManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        LogStore fileDAO = new LogStoreFileDAO("data");

        // add test data
        int begin = 10000;
        int num = 1000;
        Method insertToSessionMapMethod = LogStoreFileDAO.class.getDeclaredMethod("insertToSessionMap");
        insertToSessionMapMethod.setAccessible(true);
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
                insertToSessionMapMethod.invoke(fileDAO, globalSession);
            } catch (Exception exx) {
            }
        }

        long beginWriteMills = System.currentTimeMillis();
        for (int i = 0; i < threadNum; i++) {
            final int threadNo = i;
            Thread thread = new Thread(() -> {
                try {
                    write(fileDAO, threadNo);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }
        countDownLatch.await();
        long endWriteMills = System.currentTimeMillis();
        System.out.println("thread nums:" + threadNum + ", per_thread_trx_num:" + per_thread_trx_num + " ,cost" + (endWriteMills - beginWriteMills));
    }

    private static void write(LogStore logStore, int threadNo) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method writeSessionMethod = LogStoreFileDAO.class.getDeclaredMethod("writeSession",
                TransactionStoreManager.LogOperation.class, BaseModel.class);
        writeSessionMethod.setAccessible(true);

        int trx_begin = threadNo * per_thread_trx_num;
        for (int i = trx_begin; i < trx_begin + per_thread_trx_num; i++) {
            GlobalSession globalSession = new GlobalSession(appname, vgroup, instname, 60000);
            writeSessionMethod.invoke(logStore, TransactionStoreManager.LogOperation.GLOBAL_ADD, globalSession);

            BranchSession branchSession1 = new BranchSession();
            branchSession1.setTransactionId(globalSession.getTransactionId());
            branchSession1.setBranchId(trx_begin + per_thread_trx_num + (i - trx_begin) * 2);
            branchSession1.setResourceId("mockDbkeY1");
            writeSessionMethod.invoke(logStore, TransactionStoreManager.LogOperation.BRANCH_ADD, branchSession1);
            writeSessionMethod.invoke(logStore, TransactionStoreManager.LogOperation.BRANCH_UPDATE, branchSession1);
            writeSessionMethod.invoke(logStore, TransactionStoreManager.LogOperation.BRANCH_REMOVE, branchSession1);

            BranchSession branchSession2 = new BranchSession();
            branchSession2.setTransactionId(globalSession.getTransactionId());
            branchSession2.setBranchId(trx_begin + (i - trx_begin) + i * 2 + 1);
            branchSession2.setResourceId("mockDbkeY2");
            writeSessionMethod.invoke(logStore, TransactionStoreManager.LogOperation.BRANCH_ADD, branchSession2);
            writeSessionMethod.invoke(logStore, TransactionStoreManager.LogOperation.BRANCH_UPDATE, branchSession2);
            writeSessionMethod.invoke(logStore, TransactionStoreManager.LogOperation.BRANCH_REMOVE, branchSession2);

            writeSessionMethod.invoke(logStore, TransactionStoreManager.LogOperation.GLOBAL_UPDATE, globalSession);
            writeSessionMethod.invoke(logStore, TransactionStoreManager.LogOperation.GLOBAL_REMOVE, globalSession);
        }
        countDownLatch.countDown();
    }
}
