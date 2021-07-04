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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import io.seata.common.exception.StoreException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.session.SessionManager;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.storage.file.FlushDiskMode;
import io.seata.server.storage.file.ReloadableStore;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.StoreConfig;
import io.seata.server.store.TransactionStoreManager;
import io.seata.server.storage.file.TransactionWriteStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static io.seata.core.context.RootContext.MDC_KEY_BRANCH_ID;

/**
 * The type File transaction store manager.
 *
 * @author slievrly
 */
public class FileTransactionStoreManager extends AbstractTransactionStoreManager
    implements TransactionStoreManager, ReloadableStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileTransactionStoreManager.class);

    private static final int MAX_THREAD_WRITE = 1;

    private ExecutorService fileWriteExecutor;

    private volatile boolean stopping = false;

    private static final int MAX_SHUTDOWN_RETRY = 3;

    private static final int SHUTDOWN_CHECK_INTERVAL = 1 * 1000;

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

    private File currDataFile;

    private RandomAccessFile currRaf;

    private FileChannel currFileChannel;

    private long recoverCurrOffset = 0;

    private long recoverHisOffset = 0;

    private SessionManager sessionManager;

    private String currFullFileName;

    private String hisFullFileName;

    private WriteDataFileRunnable writeDataFileRunnable;

    private ReentrantLock writeSessionLock = new ReentrantLock();

    private volatile long lastModifiedTime;

    private static final int MAX_WRITE_BUFFER_SIZE = StoreConfig.getFileWriteBufferCacheSize();

    private final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(MAX_WRITE_BUFFER_SIZE);

    private static final FlushDiskMode FLUSH_DISK_MODE = StoreConfig.getFlushDiskMode();

    private static final int MAX_WAIT_FOR_FLUSH_TIME_MILLS = 2 * 1000;

    private static final int MAX_WAIT_FOR_CLOSE_TIME_MILLS = 2 * 1000;

    private static final int INT_BYTE_SIZE = 4;

    /**
     * Instantiates a new File transaction store manager.
     *
     * @param fullFileName   the dir path
     * @param sessionManager the session manager
     * @throws IOException the io exception
     */
    public FileTransactionStoreManager(String fullFileName, SessionManager sessionManager) throws IOException {
        initFile(fullFileName);
        fileWriteExecutor = new ThreadPoolExecutor(MAX_THREAD_WRITE, MAX_THREAD_WRITE, Integer.MAX_VALUE,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
            new NamedThreadFactory("fileTransactionStore", MAX_THREAD_WRITE, true));
        writeDataFileRunnable = new WriteDataFileRunnable();
        fileWriteExecutor.submit(writeDataFileRunnable);
        this.sessionManager = sessionManager;
    }

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

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        long curFileTrxNum;
        writeSessionLock.lock();
        try {
            if (!writeDataFile(new TransactionWriteStore(session, logOperation).encode())) {
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
        flushDisk(curFileTrxNum, currFileChannel);
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
            CloseFileRequest request = new CloseFileRequest(currFileChannel, currRaf);
            writeDataFileRunnable.putRequest(request);
            request.waitForClose(MAX_WAIT_FOR_CLOSE_TIME_MILLS);
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
        List<GlobalSession> globalSessionsOverMaxTimeout = sessionManager.findGlobalSessions(
            new SessionCondition(MAX_TRX_TIMEOUT_MILLS));
        if (CollectionUtils.isEmpty(globalSessionsOverMaxTimeout)) {
            return true;
        }
        for (GlobalSession globalSession : globalSessionsOverMaxTimeout) {
            TransactionWriteStore globalWriteStore = new TransactionWriteStore(globalSession, LogOperation.GLOBAL_ADD);
            byte[] data = globalWriteStore.encode();
            if (!writeDataFrame(data)) {
                return false;
            }
            List<BranchSession> branchSessIonsOverMaXTimeout = globalSession.getSortedBranches();
            if (branchSessIonsOverMaXTimeout != null) {
                for (BranchSession branchSession : branchSessIonsOverMaXTimeout) {
                    try {
                        MDC.put(MDC_KEY_BRANCH_ID, String.valueOf(branchSession.getBranchId()));
                        TransactionWriteStore branchWriteStore = new TransactionWriteStore(branchSession,
                            LogOperation.BRANCH_ADD);
                        data = branchWriteStore.encode();
                        if (!writeDataFrame(data)) {
                            return false;
                        }
                    } finally {
                        MDC.remove(MDC_KEY_BRANCH_ID);
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

    @Override
    public GlobalSession readSession(String xid) {
        throw new StoreException("unsupport for read from file, xid:" + xid);
    }

    @Override
    public List<GlobalSession> readSession(SessionCondition sessionCondition) {
        throw new StoreException("unsupport for read from file");
    }

    @Override
    public void shutdown() {
        if (fileWriteExecutor != null) {
            fileWriteExecutor.shutdown();
            stopping = true;
            int retry = 0;
            while (!fileWriteExecutor.isTerminated() && retry < MAX_SHUTDOWN_RETRY) {
                ++retry;
                try {
                    Thread.sleep(SHUTDOWN_CHECK_INTERVAL);
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

    @Override
    public List<TransactionWriteStore> readWriteStore(int readSize, boolean isHistory) {
        File file = null;
        long currentOffset = 0;
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
        File file;
        RandomAccessFile raf = null;
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
            closeFile(raf);
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

    interface StoreRequest {

    }

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

    class AsyncFlushRequest extends AbstractFlushRequest {

        public AsyncFlushRequest(long curFileTrxNum, FileChannel curFileChannel) {
            super(curFileTrxNum, curFileChannel);
        }

    }

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
                syncFlush((SyncFlushRequest)storeRequest);
            } else if (storeRequest instanceof AsyncFlushRequest) {
                async((AsyncFlushRequest)storeRequest);
            } else if (storeRequest instanceof CloseFileRequest) {
                closeAndFlush((CloseFileRequest)storeRequest);
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
}
