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
package io.seata.server.storage.file.session;

import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.server.session.AbstractSessionManager;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.Reloadable;
import io.seata.server.storage.file.store.FileTransactionStoreManager;

import java.io.File;
import java.io.IOException;

/**
 * The type File based session manager.
 *
 * @author slievrly
 */
@LoadLevel(name = "file", scope = Scope.PROTOTYPE)
public class FileSessionManager extends AbstractSessionManager
        implements Reloadable {

    //region Constant

    /**
     * The default DEFAULT_SESSION_STORE_FILE_DIR.
     */
    public static final String DEFAULT_SESSION_STORE_FILE_DIR = "sessionStore";

    /**
     * The constant SESSION_STORE_PATH.
     */
    private static final String SESSION_STORE_PATH = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_FILE_DIR,
            DEFAULT_SESSION_STORE_FILE_DIR);

    /**
     * The constant FILE_NAME.
     */
    public static final String FILE_NAME = "root.data";

    //endregion

    //region Constructor

    /**
     * Instantiates a new File based session manager.
     *
     * @throws IOException the io exception
     */
    public FileSessionManager() throws IOException {
        this(FILE_NAME, SESSION_STORE_PATH);
    }

    /**
     * Instantiates a new File based session manager.
     *
     * @param fileName             the file name
     * @param sessionStoreFilePath the session store file path
     * @throws IOException the io exception
     */
    public FileSessionManager(String fileName, String sessionStoreFilePath) throws IOException {
        super.transactionStoreManager = new FileTransactionStoreManager(sessionStoreFilePath + File.separator + fileName);
    }

    //endregion

    //region Override SessionManager, Reloadable, Disposable

    @Override
    public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
            throws TransactionException {
        globalSession.lock();
        try {
            return lockCallable.call();
        } finally {
            globalSession.unlock();
        }
    }

    @Override
    public void reload() {
        ((FileTransactionStoreManager) super.transactionStoreManager).reload();
    }

    @Override
    public void destroy() {
        transactionStoreManager.shutdown();
    }

    //endregion
}
