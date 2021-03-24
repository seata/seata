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
package io.seata.rm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.core.model.BranchType;
import io.seata.core.model.ResourceManager;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;
import io.seata.rm.datasource.DataSourceManager;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.undo.UndoLogManager;
import io.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Rm handler at.
 *
 * @author sharajava
 */
public class RMHandlerAT extends AbstractRMHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RMHandlerAT.class);

    private static final int LIMIT_ROWS = 3000;

    private final Map<String, Boolean> undoLogTableExistRecord = new ConcurrentHashMap<>();

    @Override
    public void handle(UndoLogDeleteRequest request) {
        String resourceId = request.getResourceId();
        DataSourceManager dataSourceManager = (DataSourceManager)getResourceManager();
        DataSourceProxy dataSourceProxy = dataSourceManager.get(resourceId);
        if (dataSourceProxy == null) {
            LOGGER.warn("Failed to get dataSourceProxy for delete undolog on {}", resourceId);
            return;
        }

        boolean hasUndoLogTable = undoLogTableExistRecord.computeIfAbsent(resourceId, id -> checkUndoLogTableExist(dataSourceProxy));
        if (!hasUndoLogTable) {
            LOGGER.debug("resource({}) has no undo_log table, UndoLogDeleteRequest will be ignored", resourceId);
            return;
        }

        Connection conn = getConnection(dataSourceProxy);
        if (conn == null) {
            LOGGER.warn("Failed to get connection to delete expired undo_log for {}", resourceId);
            return;
        }

        Date division = getLogCreated(request.getSaveDays());

        UndoLogManager manager = getUndoLogManager(dataSourceProxy);

        int deleteRows;
        do {
            deleteRows = deleteUndoLog(manager, conn, division);
        } while (deleteRows == LIMIT_ROWS);

        try {
            conn.close();
        } catch (SQLException closeEx) {
            LOGGER.warn("Failed to close JDBC resource after deleting undo_log", closeEx);
        }
    }

    boolean checkUndoLogTableExist(DataSourceProxy dataSourceProxy) {
        Connection connection = getConnection(dataSourceProxy);
        if (connection == null) {
            return false;
        }
        UndoLogManager manager = getUndoLogManager(dataSourceProxy);
        boolean isExisted = manager.hasUndoLogTable(connection);
        try {
            connection.close();
        } catch (SQLException e) {
            String resourceId = dataSourceProxy.getResourceId();
            LOGGER.warn("Failed to close JDBC resource for {}", resourceId, e);
        }
        return isExisted;
    }

    Connection getConnection(DataSourceProxy dataSourceProxy) {
        try {
            return dataSourceProxy.getPlainConnection();
        } catch (SQLException e) {
            String resourceId = dataSourceProxy.getResourceId();
            LOGGER.error("Failed to get connection for {}", resourceId, e);
            return null;
        }
    }

    UndoLogManager getUndoLogManager(DataSourceProxy dataSourceProxy) {
        return UndoLogManagerFactory.getUndoLogManager(dataSourceProxy.getDbType());
    }

    int deleteUndoLog(UndoLogManager manager, Connection conn, Date division) {
        try {
            int deleteRows = manager.deleteUndoLogByLogCreated(division, LIMIT_ROWS, conn);
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
            return deleteRows;
        } catch (SQLException e) {
            LOGGER.error("Failed to delete expired undo_log", e);
            try {
                if (!conn.getAutoCommit()) {
                    conn.rollback();
                }
            } catch (SQLException re) {
                LOGGER.error("Failed to rollback undolog", re);
            }
            return 0;
        }
    }

    private Date getLogCreated(int saveDays) {
        if (saveDays <= 0) {
            saveDays = UndoLogDeleteRequest.DEFAULT_SAVE_DAYS;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -saveDays);
        return calendar.getTime();
    }

    /**
     * get AT resource manager
     */
    @Override
    protected ResourceManager getResourceManager() {
        return DefaultResourceManager.get().getResourceManager(BranchType.AT);
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.AT;
    }
}
