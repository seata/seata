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
package io.seata.server.storage.file.store;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.exception.StoreException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.AbstractLogStore;
import io.seata.core.store.BaseModel;
import io.seata.core.store.GlobalCondition;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.Reloadable;
import io.seata.server.storage.file.FlushDiskMode;
import io.seata.server.storage.file.ReloadableStore;
import io.seata.server.storage.file.TransactionWriteStore;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.StoreConfig;
import io.seata.server.store.TransactionStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Destroyable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static io.seata.common.DefaultValues.FIRST_PAGE_INDEX;

/**
 * The type Log store file dao.
 *
 * @author slievrly
 */
public class LogStoreFileDAO extends AbstractLogStore<GlobalSession, BranchSession>
        implements Reloadable, ReloadableStore, Destroyable {

    //region Constant

    private static final Logger LOGGER = LoggerFactory.getLogger(LogStoreFileDAO.class);

    /**
     * The constant READ_SIZE.
     */
    private static final int READ_SIZE = ConfigurationFactory.getInstance().getInt(
            ConfigurationKeys.SERVICE_SESSION_RELOAD_READ_SIZE, 100);

    private static final int MAX_THREAD_WRITE = 1;

    private static final int MAX_SHUTDOWN_RETRY = 3;

    private static final int SHUTDOWN_CHECK_INTERNAL = 1 * 1000;

    private static final int MAX_WRITE_RETRY = 5;

    private static final String HIS_DATA_FILENAME_POSTFIX = ".1";

    private static final AtomicLong FILE_TRX_NUM = new AtomicLong(0);

    private static final AtomicLong FILE_FLUSH_NUM = new AtomicLong(0);

    private static final int MARK_SIZE = 4;

    private static final int MAX_WAIT_TIME_MILLS = 2 * 1000;

    private static final int MAX_FLUSH_TIME_MILLS = 2 * 1000;

    private static final int MAX_FLUSH_NUM = 10;

    private static final int PER_FILE_BLOCK_SIZE = 65535 * 8;

    private static final long MAX_TRX_TIMEOUT_MILLS = 30 * 60 * 1000;

    private static volatile long trxStartTimeMills = System.currentTimeMillis();

    private static final int MAX_WRITE_BUFFER_SIZE = StoreConfig.getFileWriteBufferCacheSize();

    private static final FlushDiskMode FLUSH_DISK_MODE = StoreConfig.getFlushDiskMode();

    private static final int MAX_WAIT_FOR_FLUSH_TIME_MILLS = 2 * 1000;

    private static final int MAX_WAIT_FOR_CLOSE_TIME_MILLS = 2 * 1000;

    private static final int INT_BYTE_SIZE = 4;

    //endregion

    //region Fields

    //region Log stores

    /**
     * The Session store file.
     */
    private File currDataFile;

    /**
     * The Session map by xid.
     */
    private Map<String, GlobalSession> sessionMapByXid = new ConcurrentHashMap<>();

    /**
     * The Session map by global status.
     */
    private Map<GlobalStatus, Set<GlobalSession>> sessionMapByStatus = new ConcurrentHashMap<>();

    //endregion

    private ExecutorService fileWriteExecutor;

    private volatile boolean stopping = false;

    private RandomAccessFile currRaf;

    private FileChannel currFileChannel;

    private long recoverCurrOffset = 0;

    private long recoverHisOffset = 0;

    private String currFullFileName;

    private String hisFullFileName;

    private WriteDataFileRunnable writeDataFileRunnable;

    private ReentrantLock writeSessionLock = new ReentrantLock();

    private volatile long lastModifiedTime;

    private final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(MAX_WRITE_BUFFER_SIZE);

    //endregion

    //region Constructor

    /**
     * Instantiates a new Log store file dao.
     */
    public LogStoreFileDAO(String fullFileName) throws IOException {
        // init file
        this.initFile(fullFileName);

        // init writeDataFileRunnable
        this.writeDataFileRunnable = new WriteDataFileRunnable();

        // init fileWriteExecutor
        this.fileWriteExecutor = new ThreadPoolExecutor(MAX_THREAD_WRITE, MAX_THREAD_WRITE, Integer.MAX_VALUE,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("fileTransactionStore", MAX_THREAD_WRITE, true));
        this.fileWriteExecutor.submit(this.writeDataFileRunnable);

        // init sessionMapByStatus
        for (GlobalStatus status : GlobalStatus.values()) {
            this.sessionMapByStatus.put(status, Collections.synchronizedSet(new LinkedHashSet<>()));
        }
    }

    //endregion

    //region Init

    private void initFile(String fullFileName) throws IOException {
        this.currFullFileName = fullFileName;
        this.hisFullFileName = fullFileName + HIS_DATA_FILENAME_POSTFIX;
        try {
            currDataFile = new File(currFullFileName);
            if (!currDataFile.exists()) {
                // create parent dir first
                if (currDataFile.getParentFile() != null && !currDataFile.getParentFile().exists()) {
                    currDataFile.getParentFile().mkdirs();
                }
                currDataFile.createNewFile();
                trxStartTimeMills = System.currentTimeMillis();
            } else {
                trxStartTimeMills = currDataFile.lastModified();
            }
            lastModifiedTime = System.currentTimeMillis();
            currRaf = new RandomAccessFile(currDataFile, "rw");
            currRaf.seek(currDataFile.length());
            currFileChannel = currRaf.getChannel();
        } catch (IOException exx) {
            LOGGER.error("init file error,{}", exx.getMessage(), exx);
            throw exx;
        }
    }

    //endregion

    //region Override LogStore

    @Override
    public GlobalSession getGlobalTransactionDO(String xid) {
        return sessionMapByXid.get(xid);
    }

    @Override
    public List<GlobalSession> queryGlobalTransactionDO(GlobalCondition condition) {
        List<GlobalSession> found = new ArrayList<>();

        if (condition.getStatuses() != null && condition.getStatuses().length > 0) {
            // status in (?, ?, ?)
            for (GlobalStatus status : condition.getStatuses()) {
                found.addAll(sessionMapByStatus.get(status));
            }
        } else {
            found.addAll(sessionMapByXid.values());
        }

        // do query
        found = condition.doQuery(found);

        return found;
    }

    @Override
    public int countGlobalTransactionDO(GlobalCondition condition) {
        int pageIndexBak = condition.getPageIndex();
        int pageSizeBak = condition.getPageSize();

        condition.setPageIndex(FIRST_PAGE_INDEX);
        condition.setPageSize(0);

        try {
            return queryGlobalTransactionDO(condition).size();
        } finally {
            condition.setPageIndex(pageIndexBak);
            condition.setPageSize(pageSizeBak);
        }
    }

    @Override
    public boolean insertGlobalTransactionDO(GlobalSession globalTransactionDO) {
        this.insertToSessionMap(globalTransactionDO);
        return this.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, globalTransactionDO);
    }

    @Override
    public boolean updateGlobalTransactionDO(GlobalSession globalTransactionDO) {
        this.updateToSessionMap(globalTransactionDO);
        return this.writeSession(TransactionStoreManager.LogOperation.GLOBAL_UPDATE, globalTransactionDO);
    }

    @Override
    public boolean deleteGlobalTransactionDO(GlobalSession globalTransactionDO) {
        this.removeFromSessionMap(globalTransactionDO);
        return this.writeSession(TransactionStoreManager.LogOperation.GLOBAL_REMOVE, globalTransactionDO);
    }

    @Override
    public List<BranchSession> queryBranchTransactionDO(String xid) {
        throw new StoreException("unsupport for read from file");
    }

    @Override
    public List<BranchSession> queryBranchTransactionDO(List<String> xids) {
        throw new StoreException("unsupport for read from file");
    }

    @Override
    public boolean insertBranchTransactionDO(BranchSession branchTransactionDO) {
        return this.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, branchTransactionDO);
    }

    @Override
    public boolean updateBranchTransactionDO(BranchSession branchTransactionDO) {
        return this.writeSession(TransactionStoreManager.LogOperation.BRANCH_UPDATE, branchTransactionDO);
    }

    @Override
    public boolean deleteBranchTransactionDO(BranchSession branchTransactionDO) {
        return this.writeSession(TransactionStoreManager.LogOperation.BRANCH_REMOVE, branchTransactionDO);
    }

    //endregion

    //region Override Reloadable and ReloadableStore

    @Override
    public void reload() {
        restoreSessions();
        washSessions();
    }

    @Override
    public List<TransactionWriteStore> readWriteStore(int readSize, boolean isHistory) {
        File file;
        long currentOffset;
        if (isHistory) {
            file = new File(hisFullFileName);
            currentOffset = recoverHisOffset;
        } else {
            file = new File(currFullFileName);
            currentOffset = recoverCurrOffset;
        }
        if (file.exists()) {
            return parseDataFile(file, readSize, currentOffset, isHistory);
        }
        return null;
    }

    @Override
    public boolean hasRemaining(boolean isHistory) {
        RandomAccessFile raf = null;
        File file;
        long currentOffset;
        if (isHistory) {
            file = new File(hisFullFileName);
            currentOffset = recoverHisOffset;
        } else {
            file = new File(currFullFileName);
            currentOffset = recoverCurrOffset;
        }
        try {
            raf = new RandomAccessFile(file, "r");
            return currentOffset < raf.length();

        } catch (IOException ignore) {
        } finally {
            this.closeFile(raf);
        }
        return false;
    }

    //endregion

    //region Private

    private synchronized void insertToSessionMap(GlobalSession globalTransactionDO) {
        // add to map by xid
        sessionMapByXid.put(globalTransactionDO.getXid(), globalTransactionDO);
        // add to map by status, and backup status
        globalTransactionDO.copyToStatusInStore();
        sessionMapByStatus.get(globalTransactionDO.getStatus()).add(globalTransactionDO);
    }

    private synchronized void updateToSessionMap(GlobalSession globalTransactionDO) {
        if (globalTransactionDO.getStatus() == globalTransactionDO.getStatusInStore()) {
            return;
        }

        globalTransactionDO.copyToStatusInStore();
        sessionMapByStatus.get(globalTransactionDO.getStatus()).add(globalTransactionDO);

        sessionMapByStatus.get(globalTransactionDO.getStatusInStore()).remove(globalTransactionDO);
    }

    private synchronized GlobalSession removeFromSessionMap(GlobalSession globalTransactionDO) {
        // remove from map by xid
        GlobalSession removedSession = sessionMapByXid.remove(globalTransactionDO.getXid());
        // remove from map by status
        sessionMapByStatus.get(globalTransactionDO.getStatus()).remove(globalTransactionDO);
        if (globalTransactionDO.getStatusInStore() != null
                && globalTransactionDO.getStatusInStore() != globalTransactionDO.getStatus()) {
            sessionMapByStatus.get(globalTransactionDO.getStatusInStore()).remove(globalTransactionDO);
        }
        globalTransactionDO.clearStatusInStore();
        return removedSession;
    }

    private boolean writeSession(TransactionStoreManager.LogOperation logOperation, BaseModel session) {
        if (!(session instanceof SessionStorable)) {
            throw new IllegalArgumentException(
                    "the parameter of session is not available, SessionStorable:" + StringUtils.toString(session));
        }

        writeSessionLock.lock();
        long curFileTrxNum;
        try {
            if (!this.writeDataFile(new TransactionWriteStore((SessionStorable) session, logOperation).encode())) {
                return false;
            }
            lastModifiedTime = System.currentTimeMillis();
            curFileTrxNum = FILE_TRX_NUM.incrementAndGet();
            if (curFileTrxNum % PER_FILE_BLOCK_SIZE == 0
                    && (System.currentTimeMillis() - trxStartTimeMills) > MAX_TRX_TIMEOUT_MILLS) {
                return saveHistory();
            }
        } catch (Exception exx) {
            LOGGER.error("writeSession error, {}", exx.getMessage(), exx);
            return false;
        } finally {
            writeSessionLock.unlock();
        }
        this.flushDisk(curFileTrxNum, currFileChannel);
        return true;
    }

    private void flushDisk(long curFileNum, FileChannel currFileChannel) {
        if (FLUSH_DISK_MODE == FlushDiskMode.SYNC_MODEL) {
            SyncFlushRequest syncFlushRequest = new SyncFlushRequest(curFileNum, currFileChannel);
            writeDataFileRunnable.putRequest(syncFlushRequest);
            syncFlushRequest.waitForFlush(MAX_WAIT_FOR_FLUSH_TIME_MILLS);
        } else {
            writeDataFileRunnable.putRequest(new AsyncFlushRequest(curFileNum, currFileChannel));
        }
    }

    /**
     * get all overTimeSessionStorables
     * merge write file
     *
     * @throws IOException
     */
    private boolean saveHistory() throws IOException {
        boolean result;
        try {
            result = findTimeoutAndSave();
            StoreRequest request = new CloseFileRequest(currFileChannel, currRaf);
            writeDataFileRunnable.putRequest(request);
            ((CloseFileRequest) request).waitForClose(MAX_WAIT_FOR_CLOSE_TIME_MILLS);
            Files.move(currDataFile.toPath(), new File(hisFullFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exx) {
            LOGGER.error("save history data file error, {}", exx.getMessage(), exx);
            result = false;
        } finally {
            initFile(currFullFileName);
        }
        return result;
    }

    private boolean writeDataFrame(byte[] data) {
        if (data == null || data.length <= 0) {
            return true;
        }
        int dataLength = data.length;
        int bufferRemainingSize = writeBuffer.remaining();
        if (bufferRemainingSize <= INT_BYTE_SIZE) {
            if (!flushWriteBuffer(writeBuffer)) {
                return false;
            }
        }
        bufferRemainingSize = writeBuffer.remaining();
        if (bufferRemainingSize <= INT_BYTE_SIZE) {
            throw new IllegalStateException(
                    String.format("Write buffer remaining size %d was too small", bufferRemainingSize));
        }
        writeBuffer.putInt(dataLength);
        bufferRemainingSize = writeBuffer.remaining();
        int dataPos = 0;
        while (dataPos < dataLength) {
            int dataLengthToWrite = dataLength - dataPos;
            dataLengthToWrite = Math.min(dataLengthToWrite, bufferRemainingSize);
            writeBuffer.put(data, dataPos, dataLengthToWrite);
            bufferRemainingSize = writeBuffer.remaining();
            if (bufferRemainingSize == 0) {
                if (!flushWriteBuffer(writeBuffer)) {
                    return false;
                }
                bufferRemainingSize = writeBuffer.remaining();
            }
            dataPos += dataLengthToWrite;
        }
        return true;
    }

    private boolean flushWriteBuffer(ByteBuffer writeBuffer) {
        writeBuffer.flip();
        if (!writeDataFileByBuffer(writeBuffer)) {
            return false;
        }
        writeBuffer.clear();
        return true;
    }

    private boolean findTimeoutAndSave() throws IOException {
        List<GlobalSession> globalSessionsOverMaxTimeout = this.queryGlobalTransactionDO(
                new GlobalCondition(MAX_TRX_TIMEOUT_MILLS));
        if (CollectionUtils.isEmpty(globalSessionsOverMaxTimeout)) {
            return true;
        }
        for (GlobalSession globalSession : globalSessionsOverMaxTimeout) {
            TransactionWriteStore globalWriteStore = new TransactionWriteStore(globalSession, TransactionStoreManager.LogOperation.GLOBAL_ADD);
            byte[] data = globalWriteStore.encode();
            if (!writeDataFrame(data)) {
                return false;
            }
            List<BranchSession> branchSessIonsOverMaXTimeout = globalSession.getSortedBranches();
            if (branchSessIonsOverMaXTimeout != null) {
                for (BranchSession branchSession : branchSessIonsOverMaXTimeout) {
                    TransactionWriteStore branchWriteStore = new TransactionWriteStore(branchSession,
                            TransactionStoreManager.LogOperation.BRANCH_ADD);
                    data = branchWriteStore.encode();
                    if (!writeDataFrame(data)) {
                        return false;
                    }
                }
            }
        }
        if (flushWriteBuffer(writeBuffer)) {
            currFileChannel.force(false);
            return true;
        }
        return false;
    }

    private List<TransactionWriteStore> parseDataFile(File file, int readSize, long currentOffset, boolean isHistory) {
        List<TransactionWriteStore> transactionWriteStores = new ArrayList<>(readSize);
        RandomAccessFile raf = null;
        FileChannel fileChannel = null;
        try {
            raf = new RandomAccessFile(file, "r");
            raf.seek(currentOffset);
            fileChannel = raf.getChannel();
            fileChannel.position(currentOffset);
            long size = raf.length();
            ByteBuffer buffSize = ByteBuffer.allocate(MARK_SIZE);
            while (fileChannel.position() < size) {
                try {
                    buffSize.clear();
                    int avilReadSize = fileChannel.read(buffSize);
                    if (avilReadSize != MARK_SIZE) {
                        break;
                    }
                    buffSize.flip();
                    int bodySize = buffSize.getInt();
                    byte[] byBody = new byte[bodySize];
                    ByteBuffer buffBody = ByteBuffer.wrap(byBody);
                    avilReadSize = fileChannel.read(buffBody);
                    if (avilReadSize != bodySize) {
                        break;
                    }
                    TransactionWriteStore writeStore = new TransactionWriteStore();
                    writeStore.decode(byBody);
                    transactionWriteStores.add(writeStore);
                    if (transactionWriteStores.size() == readSize) {
                        break;
                    }
                } catch (Exception ex) {
                    LOGGER.error("decode data file error:{}", ex.getMessage(), ex);
                    break;
                }
            }
            return transactionWriteStores;
        } catch (IOException exx) {
            LOGGER.error("parse data file error:{},file:{}", exx.getMessage(), file.getName(), exx);
            return null;
        } finally {
            try {
                if (fileChannel != null) {
                    if (isHistory) {
                        recoverHisOffset = fileChannel.position();
                    } else {
                        recoverCurrOffset = fileChannel.position();
                    }
                }
                closeFile(raf);
            } catch (IOException exx) {
                LOGGER.error("file close error{}", exx.getMessage(), exx);
            }
        }
    }

    private void closeFile(RandomAccessFile raf) {
        try {
            if (raf != null) {
                raf.close();
                raf = null;
            }
        } catch (IOException exx) {
            LOGGER.error("file close error,{}", exx.getMessage(), exx);
        }
    }

    private boolean writeDataFile(byte[] bs) {
        if (bs == null || bs.length >= Integer.MAX_VALUE - 3) {
            return false;
        }
        if (!writeDataFrame(bs)) {
            return false;
        }
        return flushWriteBuffer(writeBuffer);
    }

    private boolean writeDataFileByBuffer(ByteBuffer byteBuffer) {
        for (int retry = 0; retry < MAX_WRITE_RETRY; retry++) {
            try {
                while (byteBuffer.hasRemaining()) {
                    currFileChannel.write(byteBuffer);
                }
                return true;
            } catch (Exception exx) {
                LOGGER.error("write data file error:{}", exx.getMessage(), exx);
            }
        }
        LOGGER.error("write dataFile failed,retry more than :{}", MAX_WRITE_RETRY);
        return false;
    }

    private void restoreSessions() {
        Map<Long, BranchSession> unhandledBranchBuffer = new HashMap<>();

        restoreSessions(true, unhandledBranchBuffer);
        restoreSessions(false, unhandledBranchBuffer);

        if (!unhandledBranchBuffer.isEmpty()) {
            unhandledBranchBuffer.values().forEach(branchSession -> {
                String xid = branchSession.getXid();
                long bid = branchSession.getBranchId();
                GlobalSession found = sessionMapByXid.get(xid);
                if (found == null) {
                    // Ignore
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("GlobalSession Does Not Exists For BranchSession [" + bid + "/" + xid + "]");
                    }
                } else {
                    BranchSession existingBranch = found.getBranch(branchSession.getBranchId());
                    if (existingBranch == null) {
                        found.add(branchSession);
                    } else {
                        existingBranch.setStatus(branchSession.getStatus());
                    }
                }

            });
        }
    }

    private void washSessions() {
        if (!sessionMapByXid.isEmpty()) {
            Iterator<Map.Entry<String, GlobalSession>> iterator = sessionMapByXid.entrySet().iterator();
            while (iterator.hasNext()) {
                GlobalSession globalSession = iterator.next().getValue();

                GlobalStatus globalStatus = globalSession.getStatus();
                switch (globalStatus) {
                    case UnKnown: // 0
                    case Committed: // 9
                    case CommitFailed: // 10
                    case Rollbacked: // 11
                    case RollbackFailed: // 12
                    case TimeoutRollbacked: // 13
                    case TimeoutRollbackFailed: // 14
                    case Finished: // 15
                        // Remove all sessions finished
                        iterator.remove();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void restoreSessions(boolean isHistory, Map<Long, BranchSession> unhandledBranchBuffer) {
        while (this.hasRemaining(isHistory)) {
            List<TransactionWriteStore> stores = this.readWriteStore(READ_SIZE, isHistory);
            restore(stores, unhandledBranchBuffer);
        }
    }

    private void restore(List<TransactionWriteStore> stores, Map<Long, BranchSession> unhandledBranchSessions) {
        for (TransactionWriteStore store : stores) {
            TransactionStoreManager.LogOperation logOperation = store.getOperate();
            SessionStorable sessionStorable = store.getSessionRequest();
            switch (logOperation) {
                case GLOBAL_ADD:
                case GLOBAL_UPDATE: {
                    GlobalSession globalSession = (GlobalSession) sessionStorable;
                    if (globalSession.getTransactionId() == 0) {
                        LOGGER.error(
                                "Restore globalSession from file failed, the transactionId is zero , xid:" + globalSession
                                        .getXid());
                        break;
                    }
                    GlobalSession foundGlobalSession = sessionMapByXid.get(globalSession.getXid());
                    if (foundGlobalSession == null) {
                        this.insertToSessionMap(globalSession);
                    } else {
                        foundGlobalSession.setStatus(globalSession.getStatus());
                        this.updateToSessionMap(foundGlobalSession);
                    }
                    break;
                }
                case GLOBAL_REMOVE: {
                    GlobalSession globalSession = (GlobalSession) sessionStorable;
                    if (globalSession.getTransactionId() == 0) {
                        LOGGER.error(
                                "Restore globalSession from file failed, the transactionId is zero , xid:" + globalSession
                                        .getXid());
                        break;
                    }
                    if (this.removeFromSessionMap(globalSession) == null) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("GlobalSession To Be Removed Does Not Exists [" + globalSession.getXid() + "]");
                        }
                    }
                    break;
                }
                case BRANCH_ADD:
                case BRANCH_UPDATE: {
                    BranchSession branchSession = (BranchSession) sessionStorable;
                    if (branchSession.getTransactionId() == 0) {
                        LOGGER.error(
                                "Restore branchSession from file failed, the transactionId is zero , xid:" + branchSession
                                        .getXid());
                        break;
                    }
                    GlobalSession foundGlobalSession = sessionMapByXid.get(branchSession.getXid());
                    if (foundGlobalSession == null) {
                        unhandledBranchSessions.put(branchSession.getBranchId(), branchSession);
                    } else {
                        BranchSession existingBranch = foundGlobalSession.getBranch(branchSession.getBranchId());
                        if (existingBranch == null) {
                            foundGlobalSession.add(branchSession);
                        } else {
                            existingBranch.setStatus(branchSession.getStatus());
                        }
                    }
                    break;
                }
                case BRANCH_REMOVE: {
                    BranchSession branchSession = (BranchSession) sessionStorable;
                    String xid = branchSession.getXid();
                    long bid = branchSession.getBranchId();
                    if (branchSession.getTransactionId() == 0) {
                        LOGGER.error(
                                "Restore branchSession from file failed, the transactionId is zero , xid:" + branchSession
                                        .getXid());
                        break;
                    }
                    GlobalSession found = sessionMapByXid.get(xid);
                    if (found == null) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(
                                    "GlobalSession To Be Updated (Remove Branch) Does Not Exists [" + bid + "/" + xid
                                            + "]");
                        }
                    } else {
                        BranchSession theBranch = found.getBranch(bid);
                        if (theBranch == null) {
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("BranchSession To Be Updated Does Not Exists [" + bid + "/" + xid + "]");
                            }
                        } else {
                            found.remove(theBranch);
                        }
                    }
                    break;
                }

                default:
                    throw new ShouldNeverHappenException("Unknown Operation: " + logOperation);
            }
        }
    }

    //endregion

    //region Shutdown

    public void shutdown() {
        if (fileWriteExecutor != null) {
            fileWriteExecutor.shutdown();
            stopping = true;
            int retry = 0;
            while (!fileWriteExecutor.isTerminated() && retry < MAX_SHUTDOWN_RETRY) {
                ++retry;
                try {
                    Thread.sleep(SHUTDOWN_CHECK_INTERNAL);
                } catch (InterruptedException ignore) {
                }
            }
            if (retry >= MAX_SHUTDOWN_RETRY) {
                fileWriteExecutor.shutdownNow();
            }
        }
        try {
            currFileChannel.force(true);
        } catch (IOException e) {
            LOGGER.error("fileChannel force error{}", e.getMessage(), e);
        }
        closeFile(currRaf);
    }

    //endregion

    //region Interface and Class

    /**
     * The interface Store request.
     */
    interface StoreRequest {

    }

    /**
     * The type Abstract flush request.
     */
    abstract static class AbstractFlushRequest implements StoreRequest {
        private final long curFileTrxNum;

        private final FileChannel curFileChannel;

        protected AbstractFlushRequest(long curFileTrxNum, FileChannel curFileChannel) {
            this.curFileTrxNum = curFileTrxNum;
            this.curFileChannel = curFileChannel;
        }

        public long getCurFileTrxNum() {
            return curFileTrxNum;
        }

        public FileChannel getCurFileChannel() {
            return curFileChannel;
        }
    }

    /**
     * The type Sync flush request.
     */
    class SyncFlushRequest extends AbstractFlushRequest {

        private final CountDownLatch countDownLatch = new CountDownLatch(1);

        public SyncFlushRequest(long curFileTrxNum, FileChannel curFileChannel) {
            super(curFileTrxNum, curFileChannel);
        }

        public void wakeup() {
            this.countDownLatch.countDown();
        }

        public void waitForFlush(long timeout) {
            try {
                this.countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
            }
        }
    }

    /**
     * The type Async flush request.
     */
    class AsyncFlushRequest extends AbstractFlushRequest {

        public AsyncFlushRequest(long curFileTrxNum, FileChannel curFileChannel) {
            super(curFileTrxNum, curFileChannel);
        }

    }

    /**
     * The type Close file request.
     */
    static class CloseFileRequest implements StoreRequest {
        private final CountDownLatch countDownLatch = new CountDownLatch(1);
        private FileChannel fileChannel;

        private RandomAccessFile file;

        public CloseFileRequest(FileChannel fileChannel, RandomAccessFile file) {
            this.fileChannel = fileChannel;
            this.file = file;
        }

        public FileChannel getFileChannel() {
            return fileChannel;
        }

        public RandomAccessFile getFile() {
            return file;
        }

        public void wakeup() {
            this.countDownLatch.countDown();
        }

        public void waitForClose(long timeout) {
            try {
                this.countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
            }
        }
    }

    /**
     * The type Write data file runnable.
     */
    class WriteDataFileRunnable implements Runnable {

        private LinkedBlockingQueue<StoreRequest> storeRequests = new LinkedBlockingQueue<>();

        public void putRequest(final StoreRequest request) {
            storeRequests.add(request);
        }

        @Override
        public void run() {
            while (!stopping) {
                try {
                    StoreRequest storeRequest = storeRequests.poll(MAX_WAIT_TIME_MILLS, TimeUnit.MILLISECONDS);
                    handleStoreRequest(storeRequest);
                } catch (Exception exx) {
                    LOGGER.error("write file error: {}", exx.getMessage(), exx);
                }
            }
            handleRestRequest();
        }

        /**
         * handle the rest requests when stopping is true
         */
        private void handleRestRequest() {
            int remainNums = storeRequests.size();
            for (int i = 0; i < remainNums; i++) {
                handleStoreRequest(storeRequests.poll());
            }
        }

        private void handleStoreRequest(StoreRequest storeRequest) {
            if (storeRequest == null) {
                flushOnCondition(currFileChannel);
            }
            if (storeRequest instanceof SyncFlushRequest) {
                syncFlush((SyncFlushRequest) storeRequest);
            } else if (storeRequest instanceof AsyncFlushRequest) {
                async((AsyncFlushRequest) storeRequest);
            } else if (storeRequest instanceof CloseFileRequest) {
                closeAndFlush((CloseFileRequest) storeRequest);
            }
        }

        private void closeAndFlush(CloseFileRequest req) {
            long diff = FILE_TRX_NUM.get() - FILE_FLUSH_NUM.get();
            flush(req.getFileChannel());
            FILE_FLUSH_NUM.addAndGet(diff);
            closeFile(req.getFile());
            req.wakeup();
        }

        private void async(AsyncFlushRequest req) {
            flushOnCondition(req.getCurFileChannel());
        }

        private void syncFlush(SyncFlushRequest req) {
            if (req.getCurFileTrxNum() > FILE_FLUSH_NUM.get()) {
                long diff = FILE_TRX_NUM.get() - FILE_FLUSH_NUM.get();
                flush(req.getCurFileChannel());
                FILE_FLUSH_NUM.addAndGet(diff);
            }
            // notify
            req.wakeup();
        }

        private void flushOnCondition(FileChannel fileChannel) {
            if (FLUSH_DISK_MODE == FlushDiskMode.SYNC_MODEL) {
                return;
            }
            long diff = FILE_TRX_NUM.get() - FILE_FLUSH_NUM.get();
            if (diff == 0) {
                return;
            }
            if (diff % MAX_FLUSH_NUM == 0 || System.currentTimeMillis() - lastModifiedTime > MAX_FLUSH_TIME_MILLS) {
                flush(fileChannel);
                FILE_FLUSH_NUM.addAndGet(diff);
            }
        }

        private void flush(FileChannel fileChannel) {
            try {
                fileChannel.force(false);
            } catch (IOException exx) {
                LOGGER.error("flush error: {}", exx.getMessage(), exx);
            }
        }
    }

    //endregion
}
