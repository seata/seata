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
package io.seata.server.coordinator;

import io.netty.channel.Channel;
import io.seata.common.XID;
import io.seata.common.util.DurationUtil;
import io.seata.common.util.NetUtil;
import io.seata.common.util.ReflectionUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.processor.RemotingProcessor;
import io.seata.core.store.StoreMode;
import io.seata.tc.coordinator.DefaultCore;
import io.seata.tc.session.GlobalSession;
import io.seata.tc.session.SessionHolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.seata.tc.session.SessionHolder.DEFAULT_SESSION_STORE_FILE_DIR;

/**
 * The type DefaultCoordinator test.
 *
 * @author leizhiyuan
 */
public class DefaultCoordinatorTest {
    private static RemotingServer remotingServer;
    private static DefaultCoordinator defaultCoordinator;

    private static final String applicationId = "demo-child-app";

    private static final String txServiceGroup = "my_test_tx_group";

    private static final String txName = "tx-1";

    private static final int timeout = 3000;

    private static final String resourceId = "tb_1";

    private static final String clientId = "c_1";

    private static final String lockKeys_1 = "tb_1:11";

    private static final String lockKeys_2 = "tb_1:12";

    private static final String applicationData = "{\"data\":\"test\"}";

    private static DefaultCore core;

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static String sessionStorePath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR,
        DEFAULT_SESSION_STORE_FILE_DIR);

    @BeforeAll
    public static void beforeClass() throws Exception {
        XID.setIpAddress(NetUtil.getLocalIp());
        RemotingServer remotingServer = new MockServerMessageSender();
        defaultCoordinator = new DefaultCoordinator(remotingServer);
        core = new DefaultCore(remotingServer);
    }

    @BeforeEach
    public void tearUp() throws IOException {
        deleteAndCreateDataFile();
    }

    @Test
    public void branchCommit() throws TransactionException {
        BranchStatus result = null;
        String xid = null;
        GlobalSession globalSession = null;
        try {
            xid = core.begin(applicationId, txServiceGroup, txName, timeout);
            Long branchId = core.branchRegister(BranchType.AT, resourceId, clientId, xid, applicationData, lockKeys_1);
            globalSession = SessionHolder.findGlobalSession(xid);
            result = core.branchCommit(globalSession, globalSession.getBranch(branchId));
        } catch (TransactionException e) {
            Assertions.fail(e.getMessage());
        }
        Assertions.assertEquals(result, BranchStatus.PhaseTwo_Committed);
        globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        globalSession.end();
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("xidAndBranchIdProviderForRollback")
    public void branchRollback(String xid, Long branchId) {
        BranchStatus result = null;
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        try {
            result = core.branchRollback(globalSession, globalSession.getBranch(branchId));
        } catch (TransactionException e) {
            Assertions.fail(e.getMessage());
        }
        Assertions.assertEquals(result, BranchStatus.PhaseTwo_Rollbacked);
    }


    @Test
    public void test_handleRetryRollbacking() throws TransactionException, InterruptedException {

        String xid = core.begin(applicationId, txServiceGroup, txName, 10);
        Long branchId = core.branchRegister(BranchType.AT, "abcd", clientId, xid, applicationData, lockKeys_2);

        Assertions.assertNotNull(branchId);

        Thread.sleep(100);
        defaultCoordinator.timeoutCheck();
        defaultCoordinator.handleRetryRollbacking();

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNull(globalSession);

    }

    @Test
    public void test_handleRetryRollbackingTimeOut() throws TransactionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        defaultCoordinator = new DefaultCoordinator(remotingServer);
        String xid = core.begin(applicationId, txServiceGroup, txName, 10);
        Long branchId = core.branchRegister(BranchType.AT, "abcd", clientId, xid, applicationData, lockKeys_2);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        Assertions.assertNotNull(globalSession.getBranchSessions());
        Assertions.assertNotNull(branchId);

        ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "MAX_ROLLBACK_RETRY_TIMEOUT", Duration.ofMillis(10));
        ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE", false);
        TimeUnit.MILLISECONDS.sleep(100);
        defaultCoordinator.timeoutCheck();
        defaultCoordinator.handleRetryRollbacking();
        int lockSize = globalSession.getBranchSessions().get(0).getLockHolder().size();
        try {
            Assertions.assertTrue(lockSize > 0);
        } finally {
            globalSession.closeAndClean();
            ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "MAX_ROLLBACK_RETRY_TIMEOUT",
                ConfigurationFactory.getInstance().getDuration(ConfigurationKeys.MAX_ROLLBACK_RETRY_TIMEOUT, DurationUtil.DEFAULT_DURATION, 100));
        }
    }

    @Test
    public void test_handleRetryRollbackingTimeOut_unlock() throws TransactionException, InterruptedException,
        NoSuchFieldException, IllegalAccessException {
        defaultCoordinator = new DefaultCoordinator(remotingServer);
        String xid = core.begin(applicationId, txServiceGroup, txName, 10);
        Long branchId = core.branchRegister(BranchType.AT, "abcd", clientId, xid, applicationData, lockKeys_2);

        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        Assertions.assertNotNull(globalSession);
        Assertions.assertNotNull(globalSession.getBranchSessions());
        Assertions.assertNotNull(branchId);

        ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "MAX_ROLLBACK_RETRY_TIMEOUT", Duration.ofMillis(10));
        ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE", true);
        TimeUnit.MILLISECONDS.sleep(100);

        defaultCoordinator.timeoutCheck();
        defaultCoordinator.handleRetryRollbacking();

        int lockSize = globalSession.getBranchSessions().get(0).getLockHolder().size();
        try {
            Assertions.assertTrue(lockSize == 0);
        } finally {
            globalSession.closeAndClean();
            ReflectionUtil.modifyStaticFinalField(defaultCoordinator.getClass(), "MAX_ROLLBACK_RETRY_TIMEOUT",
                ConfigurationFactory.getInstance().getDuration(ConfigurationKeys.MAX_ROLLBACK_RETRY_TIMEOUT, DurationUtil.DEFAULT_DURATION, 100));
        }
    }

    @AfterAll
    public static void afterClass() throws Exception {

        Collection<GlobalSession> globalSessions = SessionHolder.getRootSessionManager().allSessions();
        Collection<GlobalSession> asyncGlobalSessions = SessionHolder.getAsyncCommittingSessionManager().allSessions();
        for (GlobalSession asyncGlobalSession : asyncGlobalSessions) {
            asyncGlobalSession.closeAndClean();
        }
        for (GlobalSession globalSession : globalSessions) {
            globalSession.closeAndClean();
        }
    }

    @AfterEach
    public void tearDown() throws IOException {
        SessionHolder.destroy();
        deleteDataFile();
    }

    private static void deleteDataFile() throws IOException {
        File directory = new File(sessionStorePath);
        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                Files.delete(Paths.get(file.getPath()));
            }
        }
    }

    private static void deleteAndCreateDataFile() throws IOException {
        SessionHolder.destroy();
        deleteDataFile();
        SessionHolder.init(StoreMode.FILE.name());
    }

    static Stream<Arguments> xidAndBranchIdProviderForRollback() throws Exception {
        String xid = core.begin(applicationId, txServiceGroup, txName, timeout);
        Long branchId = core.branchRegister(BranchType.AT, resourceId, clientId, xid, applicationData, lockKeys_2);
        return Stream.of(
            Arguments.of(xid, branchId)
        );
    }


    public static class MockServerMessageSender implements RemotingServer {

        @Override
        public Object sendSyncRequest(String resourceId, String clientId, Object message) throws TimeoutException {
            if (message instanceof BranchCommitRequest) {
                final BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
                branchCommitResponse.setBranchStatus(BranchStatus.PhaseTwo_Committed);
                return branchCommitResponse;
            } else if (message instanceof BranchRollbackRequest) {
                final BranchRollbackResponse branchRollbackResponse = new BranchRollbackResponse();
                branchRollbackResponse.setBranchStatus(BranchStatus.PhaseTwo_Rollbacked);
                return branchRollbackResponse;
            } else {
                return null;
            }
        }

        @Override
        public Object sendSyncRequest(Channel clientChannel, Object message) throws TimeoutException {
            return null;
        }

        @Override
        public void sendAsyncRequest(Channel channel, Object msg) {

        }

        @Override
        public void sendAsyncResponse(RpcMessage request, Channel channel, Object msg) {

        }

        @Override
        public void registerProcessor(int messageType, RemotingProcessor processor, ExecutorService executor) {

        }
    }
}