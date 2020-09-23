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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.seata.core.store.BaseModel;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.file.TransactionWriteStore;
import io.seata.server.storage.file.store.LogStoreFileDAO;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import io.seata.server.store.TransactionStoreManager.LogOperation;

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
    public static void main(String[] args) throws IOException, NoSuchMethodException, InterruptedException {
        LogStoreFileDAO fileDAO = new LogStoreFileDAO("~/Documents/test/data");

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
        try {
            write(fileDAO);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        long endWriteMills = System.currentTimeMillis();
        Thread.sleep(10 * 1000);
        long beginReadMills = System.currentTimeMillis();
        Map<SessionStorable, LogOperation> resultMap = readAll(fileDAO);
        long endReadMills = System.currentTimeMillis();
        if ((resultMap.size() % (65535)) % 3000 == 0) {
            System.out.print("check success");
        } else {
            System.out.print("check failed");
        }
        System.out.print(
            "write cost:" + (endWriteMills - beginWriteMills) + ",read cost:" + (endReadMills - beginReadMills));

    }

    private static void write(LogStoreFileDAO fileDAO) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method writeSessionMethod = LogStoreFileDAO.class.getDeclaredMethod("writeSession",
            TransactionStoreManager.LogOperation.class, BaseModel.class);
        writeSessionMethod.setAccessible(true);

        for (int i = trx_begin; i < trx_begin + trx_num; i++) {
            GlobalSession globalSession = new GlobalSession(appname, vgroup, instname, 60000);
            writeSessionMethod.invoke(fileDAO, LogOperation.GLOBAL_ADD, globalSession);

            BranchSession branchSession1 = new BranchSession();
            branchSession1.setTransactionId(globalSession.getTransactionId());
            branchSession1.setBranchId(trx_begin + trx_num + (i - trx_begin) * 2);
            branchSession1.setResourceId("mockDbkeY1");
            writeSessionMethod.invoke(fileDAO, LogOperation.BRANCH_ADD, branchSession1);
            writeSessionMethod.invoke(fileDAO, LogOperation.BRANCH_UPDATE, branchSession1);
            writeSessionMethod.invoke(fileDAO, LogOperation.BRANCH_REMOVE, branchSession1);

            BranchSession branchSession2 = new BranchSession();
            branchSession2.setTransactionId(globalSession.getTransactionId());
            branchSession2.setBranchId(trx_begin + (i - trx_begin) + i * 2 + 1);
            branchSession2.setResourceId("mockDbkeY2");
            writeSessionMethod.invoke(fileDAO, LogOperation.BRANCH_ADD, branchSession2);
            writeSessionMethod.invoke(fileDAO, LogOperation.BRANCH_UPDATE, branchSession2);
            writeSessionMethod.invoke(fileDAO, LogOperation.BRANCH_REMOVE, branchSession2);

            writeSessionMethod.invoke(fileDAO, LogOperation.GLOBAL_UPDATE, globalSession);
            writeSessionMethod.invoke(fileDAO, LogOperation.GLOBAL_REMOVE, globalSession);
        }
    }

    private static Map<SessionStorable, LogOperation> readAll(LogStoreFileDAO fileDAO) {
        Map<SessionStorable, LogOperation> resultMap = new HashMap<>(65535 * 5 * 9);
        while (fileDAO.hasRemaining(true)) {
            List<TransactionWriteStore> transactionWriteStores = fileDAO.readWriteStore(2000,
                true);
            if (transactionWriteStores != null) {
                for (TransactionWriteStore transactionWriteStore : transactionWriteStores) {
                    printLog(transactionWriteStore);
                    resultMap.put(transactionWriteStore.getSessionRequest(), transactionWriteStore.getOperate());
                }
            }
        }
        while (fileDAO.hasRemaining(false)) {
            List<TransactionWriteStore> transactionWriteStores = fileDAO.readWriteStore(2000,
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
