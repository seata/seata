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
package io.seata.rm.datasource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.sql.struct.LockKey;
import io.seata.rm.datasource.undo.SQLUndoLog;

/**
 * The type Connection context.
 *
 * @author sharajava
 */
public class ConnectionContext {
    private String xid;
    private Long branchId;
    private boolean isGlobalLockRequire;

    /**
     * Table and primary key should not be duplicated.
     */
    private Set<LockKey> lockKeysBuffer = new HashSet<>();
    private List<SQLUndoLog> sqlUndoItemsBuffer = new ArrayList<>();

    /**
     * whether requires global lock in this connection
     *
     * @return
     */
    boolean isGlobalLockRequire() {
        return isGlobalLockRequire;
    }

    /**
     * set whether requires global lock in this connection
     *
     * @param isGlobalLockRequire
     */
    void setGlobalLockRequire(boolean isGlobalLockRequire) {
        this.isGlobalLockRequire = isGlobalLockRequire;
    }

    /**
     * Append lock key.
     *
     * @param lockKey the lock key
     */
    void appendLockKey(List<LockKey> lockKey) {
        lockKeysBuffer.addAll(lockKey);
    }

    /**
     * Append undo item.
     *
     * @param sqlUndoLog the sql undo log
     */
    void appendUndoItem(SQLUndoLog sqlUndoLog) {
        sqlUndoItemsBuffer.add(sqlUndoLog);
    }

    /**
     * In global transaction boolean.
     *
     * @return the boolean
     */
    public boolean inGlobalTransaction() {
        return xid != null;
    }

    /**
     * Is branch registered boolean.
     *
     * @return the boolean
     */
    public boolean isBranchRegistered() {
        return branchId != null;
    }

    /**
     * Bind.
     *
     * @param xid the xid
     */
    void bind(String xid) {
        if (xid == null) {
            throw new IllegalArgumentException("xid should not be null");
        }
        if (!inGlobalTransaction()) {
            setXid(xid);
        } else {
            if (!this.xid.equals(xid)) {
                throw new ShouldNeverHappenException();
            }
        }
    }

    /**
     * Has undo log boolean.
     *
     * @return the boolean
     */
    public boolean hasUndoLog() {
        return !sqlUndoItemsBuffer.isEmpty();
    }

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public String getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    void setXid(String xid) {
        this.xid = xid;
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public Long getBranchId() {
        return branchId;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    void setBranchId(Long branchId) {
        this.branchId = branchId;
    }


    /**
     * Reset.
     */
    public void reset() {
        this.reset(null);
    }

    /**
     * Reset.
     *
     * @param xid the xid
     */
    void reset(String xid) {
        this.xid = xid;
        branchId = null;
        this.isGlobalLockRequire = false;
        lockKeysBuffer.clear();
        sqlUndoItemsBuffer.clear();
    }

    /**
     * Build lock keys string.
     *
     * @return the string
     */
    public String buildLockKeys() {
        if (lockKeysBuffer.isEmpty()) {
            return null;
        }
        StringBuilder appender = new StringBuilder();
        Iterator<LockKey> iterable = lockKeysBuffer.iterator();
        while (iterable.hasNext()) {
            LockKey lockKey = iterable.next();
            appender.append(lockKey.getTableName());
            appender.append(":");
            appender.append(lockKey.getPk());
            if (iterable.hasNext()) {
                appender.append(";");
            }
        }
        return appender.toString();
    }

    /**
     * Gets undo items.
     *
     * @return the undo items
     */
    public List<SQLUndoLog> getUndoItems() {
        return sqlUndoItemsBuffer;
    }

    /**
     * Gets lock keys buffer.
     *
     * @return the lock keys buffer
     */
    public Set<LockKey> getLockKeysBuffer() {
        return lockKeysBuffer;
    }

    @Override
    public String toString() {
        return "ConnectionContext [xid=" + xid + ", branchId=" + branchId + ", lockKeysBuffer=" + lockKeysBuffer
            + ", sqlUndoItemsBuffer=" + sqlUndoItemsBuffer + "]";
    }

}
