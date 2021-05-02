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
package io.seata.server.storage.file;

import java.nio.ByteBuffer;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager.LogOperation;

/**
 * The type Transaction write store.
 *
 * @author slievrly
 */
public class TransactionWriteStore implements SessionStorable {
    private SessionStorable sessionRequest;
    private LogOperation operate;

    /**
     * Instantiates a new Transaction write store.
     *
     * @param sessionRequest the session request
     * @param operate        the operate
     */
    public TransactionWriteStore(SessionStorable sessionRequest, LogOperation operate) {
        this.sessionRequest = sessionRequest;
        this.operate = operate;
    }

    /**
     * Instantiates a new Transaction write store.
     */
    public TransactionWriteStore() {}

    /**
     * Gets session request.
     *
     * @return the session request
     */
    public SessionStorable getSessionRequest() {
        return sessionRequest;
    }

    /**
     * Sets session request.
     *
     * @param sessionRequest the session request
     */
    public void setSessionRequest(SessionStorable sessionRequest) {
        this.sessionRequest = sessionRequest;
    }

    /**
     * Gets operate.
     *
     * @return the operate
     */
    public LogOperation getOperate() {
        return operate;
    }

    /**
     * Sets operate.
     *
     * @param operate the operate
     */
    public void setOperate(LogOperation operate) {
        this.operate = operate;
    }

    @Override
    public byte[] encode() {
        byte[] bySessionRequest = this.sessionRequest.encode();
        byte byOpCode = this.getOperate().getCode();
        int len = bySessionRequest.length + 1;
        byte[] byResult = new byte[len];
        ByteBuffer byteBuffer = ByteBuffer.wrap(byResult);
        byteBuffer.put(bySessionRequest);
        byteBuffer.put(byOpCode);
        return byResult;
    }

    @Override
    public void decode(byte[] src) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(src);
        byte[] bySessionRequest = new byte[src.length - 1];
        byteBuffer.get(bySessionRequest);
        byte byOpCode = byteBuffer.get();
        this.operate = LogOperation.getLogOperationByCode(byOpCode);
        SessionStorable tmpSessionStorable = getSessionInstanceByOperation(this.operate);
        tmpSessionStorable.decode(bySessionRequest);
        this.sessionRequest = tmpSessionStorable;
    }

    private SessionStorable getSessionInstanceByOperation(LogOperation logOperation) {
        SessionStorable sessionStorable = null;
        switch (logOperation) {
            case GLOBAL_ADD:
            case GLOBAL_UPDATE:
            case GLOBAL_REMOVE:
            case GLOBAL_CLEAN:
                sessionStorable = new GlobalSession();
                break;
            case BRANCH_ADD:
            case BRANCH_UPDATE:
            case BRANCH_REMOVE:
            case BRANCH_CLEAN:
                sessionStorable = new BranchSession();
                break;
            default:
                throw new ShouldNeverHappenException("incorrect logOperation");
        }
        return sessionStorable;
    }
}
