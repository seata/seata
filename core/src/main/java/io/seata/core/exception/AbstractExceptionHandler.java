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
package io.seata.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.AbstractTransactionRequest;
import io.seata.core.protocol.transaction.AbstractTransactionResponse;
import io.seata.core.raft.RaftServerFactory;

/**
 * The type Abstract exception handler.
 *
 * @author sharajava
 */
public abstract class AbstractExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExceptionHandler.class);

    /**
     * The constant RAFT_SERVER_FACTORY.
     */
    private static final RaftServerFactory RAFT_SERVER_FACTORY = RaftServerFactory.getInstance();

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * Exception handle template.
     *
     * @param <T>      the type parameter
     * @param <S>      the type parameter
     * @param callback the callback
     * @param request  the request
     * @param response the response
     */
    public <T extends AbstractTransactionRequest, S extends AbstractTransactionResponse> void exceptionHandleTemplate(Callback<T, S> callback, T request, S response) {
        try {
            if (RAFT_SERVER_FACTORY.isNotRaftModeLeader()) {
                throw new TransactionException(TransactionExceptionCode.NotRaftLeader,
                    " The current TC is not a leader node, interrupt processing !");
            }
            callback.execute(request, response);
            callback.onSuccess(request, response);
        } catch (TransactionException tex) {
            LOGGER.error("Catch TransactionException while do RPC, request: {}", request, tex);
            callback.onTransactionException(request, response, tex);
        } catch (RuntimeException rex) {
            LOGGER.error("Catch RuntimeException while do RPC, request: {}", request, rex);
            callback.onException(request, response, rex);
        }
    }

    /**
     * The interface Callback.
     *
     * @param <T> the type parameter
     * @param <S> the type parameter
     */
    public interface Callback<T extends AbstractTransactionRequest, S extends AbstractTransactionResponse> {
        /**
         * Execute.
         *
         * @param request  the request
         * @param response the response
         * @throws TransactionException the transaction exception
         */
        void execute(T request, S response) throws TransactionException;

        /**
         * On success.
         *
         * @param request  the request
         * @param response the response
         */
        void onSuccess(T request, S response);

        /**
         * onTransactionException
         *
         * @param request   the request
         * @param response  the response
         * @param exception the exception
         */
        void onTransactionException(T request, S response, TransactionException exception);

        /**
         * on other exception
         *
         * @param request   the request
         * @param response  the response
         * @param exception the exception
         */
        void onException(T request, S response, Exception exception);

    }

    /**
     * The type Abstract callback.
     *
     * @param <T> the type parameter
     * @param <S> the type parameter
     */
    public abstract static class AbstractCallback<T extends AbstractTransactionRequest, S extends AbstractTransactionResponse>
        implements Callback<T, S> {

        @Override
        public void onSuccess(T request, S response) {
            response.setResultCode(ResultCode.Success);
        }

        @Override
        public void onTransactionException(T request, S response,
            TransactionException tex) {
            response.setTransactionExceptionCode(tex.getCode());
            response.setResultCode(ResultCode.Failed);
            response.setMsg("TransactionException[" + tex.getMessage() + "]");
        }

        @Override
        public void onException(T request, S response, Exception rex) {
            response.setResultCode(ResultCode.Failed);
            response.setMsg("RuntimeException[" + rex.getMessage() + "]");
        }
    }

}
