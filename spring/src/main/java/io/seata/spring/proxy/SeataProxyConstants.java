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
package io.seata.spring.proxy;

import io.seata.spring.proxy.resulthandler.impl.DefaultSeataProxyResultHandlerImpl;
import io.seata.spring.tcc.TccSeataProxyHandler;

/**
 * @author wang.liang
 */
public interface SeataProxyConstants {

    String DEFAULT_VALIDATOR_BEAN_NAME = "defaultProxyValidator";
    Class<? extends SeataProxyValidator> DEFAULT_VALIDATOR_CLASS = null;

    String DEFAULT_HANDLER_BEAN_NAME = "defaultProxyHandler";
    Class<? extends SeataProxyHandler> DEFAULT_HANDLER_CLASS = TccSeataProxyHandler.class;

    String DEFAULT_RESULT_HANDLER_BEAN_NAME = "defaultResultProxyHandler";
    Class<? extends SeataProxyResultHandler> DEFAULT_RESULT_HANDLER_CLASS = DefaultSeataProxyResultHandlerImpl.class;
}
