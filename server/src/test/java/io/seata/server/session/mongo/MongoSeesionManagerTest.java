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

package io.seata.server.session.mongo;

import com.github.fakemongo.Fongo;
import io.seata.common.XID;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.server.UUIDGenerator;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionManager;
import io.seata.server.storage.mongo.MongoPooledFactory;
import io.seata.server.storage.mongo.session.MongoSessionManager;
import io.seata.server.storage.mongo.session.MongoTransactionStoreManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author funkye
 */
public class MongoSeesionManagerTest {
    private static SessionManager sessionManager = null;

    @BeforeAll
    public static void start() {
        Fongo fongo = new Fongo("mongo server 1");
        MongoPooledFactory.getMongoPoolInstance(fongo.getMongo());
        MongoTransactionStoreManager transactionStoreManager = MongoTransactionStoreManager.getInstance();
        MongoSessionManager mongoSessionManager = new MongoSessionManager();
        mongoSessionManager.setTransactionStoreManager(transactionStoreManager);
        sessionManager = mongoSessionManager;
    }

    @Test
    public void test_addGlobalSession() throws TransactionException {
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);
        sessionManager.addGlobalSession(session);
    }

    @Test
    public void test_updateGlobalSessionStatus() throws TransactionException {
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);
        sessionManager.addGlobalSession(session);
        session.setStatus(GlobalStatus.Committing);
        sessionManager.updateGlobalSessionStatus(session,GlobalStatus.Committing);
    }

    @Test
    public void test_removeGlobalSession() throws Exception {
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);
        sessionManager.addGlobalSession(session);
        BranchSession branchSession = new BranchSession();
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setXid(xid);
        branchSession.setTransactionId(session.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        sessionManager.addBranchSession(session, branchSession);
        sessionManager.removeBranchSession(session, branchSession);
        sessionManager.removeGlobalSession(session);
    }

    @Test
    public void test_addBranchSession() throws TransactionException {
        GlobalSession globalSession = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(globalSession.getTransactionId());
        globalSession.setXid(xid);
        globalSession.setTransactionId(146757978);
        globalSession.setBeginTime(System.currentTimeMillis());
        globalSession.setApplicationData("abc=878s");
        globalSession.setStatus(GlobalStatus.Begin);
        sessionManager.addGlobalSession(globalSession);

        BranchSession branchSession = new BranchSession();
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setXid(xid);
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        sessionManager.addBranchSession(globalSession,branchSession);
    }

    @Test
    public void test_updateBranchSessionStatus() throws Exception {
        GlobalSession globalSession = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(globalSession.getTransactionId());
        globalSession.setXid(xid);
        globalSession.setTransactionId(146757978);
        globalSession.setBeginTime(System.currentTimeMillis());
        globalSession.setApplicationData("abc=878s");
        globalSession.setStatus(GlobalStatus.Begin);

        BranchSession branchSession = new BranchSession();
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setXid(xid);
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setStatus(BranchStatus.PhaseOne_Done);
        sessionManager.addBranchSession(globalSession, branchSession);
        branchSession.setStatus(BranchStatus.PhaseOne_Timeout);
        sessionManager.updateBranchSessionStatus(branchSession, BranchStatus.PhaseOne_Timeout);
    }

}
