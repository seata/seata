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
package io.seata.spring.proxy.desc;

import javax.annotation.Nullable;

import io.seata.common.util.StringUtils;
import io.seata.spring.proxy.SeataProxy;
import io.seata.spring.proxy.SeataProxyHandler;
import io.seata.spring.proxy.SeataProxyResultHandler;
import io.seata.spring.proxy.SeataProxyValidator;
import io.seata.spring.proxy.util.SeataProxyInterceptorUtil;

/**
 * The Seata Proxy Interface Implemention Desc
 *
 * @author wang.liang
 * @see SeataProxyParser
 * @see SeataProxyBeanDesc
 * @see SeataProxyMethodDesc
 */
public class SeataProxyImplementationDesc {

    // the classes for getting the implementations
    private Class<? extends SeataProxyValidator> validatorClass;
    private Class<? extends SeataProxyHandler> handlerClass;
    private Class<? extends SeataProxyResultHandler> resultHandlerClass;

    // the bean names for getting the implementations
    private String validatorBeanName;
    private String handlerBeanName;
    private String resultHandlerBeanName;

    // the implementations
    private SeataProxyValidator validator;
    private SeataProxyHandler handler;
    private SeataProxyResultHandler resultHandler;

    // is exists for the implementations
    private Boolean existingProxyValidator;
    private Boolean existingProxyHandler;
    private Boolean existingProxyResultHandler;

    public SeataProxyImplementationDesc(Class<? extends SeataProxyValidator> validatorClass,
                                        Class<? extends SeataProxyHandler> handlerClass, Class<? extends SeataProxyResultHandler> resultHandlerClass,
                                        String validatorBeanName, String handlerBeanName, String resultHandlerBeanName) {
        if (validatorClass != SeataProxyValidator.class) {
            this.validatorClass = validatorClass;
        }
        if (handlerClass != SeataProxyHandler.class) {
            this.handlerClass = handlerClass;
        }
        if (resultHandlerClass != SeataProxyResultHandler.class) {
            this.resultHandlerClass = resultHandlerClass;
        }
        if (StringUtils.isNotBlank(validatorBeanName)) {
            this.validatorBeanName = validatorBeanName;
        }
        if (StringUtils.isNotBlank(handlerBeanName)) {
            this.handlerBeanName = handlerBeanName;
        }
        if (StringUtils.isNotBlank(resultHandlerBeanName)) {
            this.resultHandlerBeanName = resultHandlerBeanName;
        }
    }

    public SeataProxyImplementationDesc(SeataProxy annotation) {
        this(annotation.validatorClass(), annotation.handlerClass(), annotation.resultHandlerClass(),
                annotation.validatorBeanName(), annotation.handlerBeanName(), annotation.resultHandlerBeanName());
    }

    @Nullable
    public SeataProxyValidator getValidator() {
        if (this.validator == null) {
            if (Boolean.FALSE.equals(existingProxyValidator)) {
                return null;
            }
            synchronized (this) {
                if (this.validator == null && !Boolean.FALSE.equals(existingProxyValidator)) {
                    this.validator = SeataProxyInterceptorUtil.tryGetBean(validatorBeanName, validatorClass, true);
                    existingProxyValidator = this.validator != null;
                }
            }
        }

        return this.validator;
    }

    @Nullable
    public SeataProxyHandler getHandler() {
        if (this.handler == null) {
            if (Boolean.FALSE.equals(existingProxyHandler)) {
                return null;
            }
            synchronized (this) {
                if (this.handler == null && !Boolean.FALSE.equals(existingProxyHandler)) {
                    this.handler = SeataProxyInterceptorUtil.tryGetBean(handlerBeanName, handlerClass, true);
                    existingProxyHandler = this.handler != null;
                }
            }
        }

        return this.handler;
    }

    @Nullable
    public SeataProxyResultHandler getResultHandler() {
        if (this.resultHandler == null) {
            if (Boolean.FALSE.equals(existingProxyResultHandler)) {
                return null;
            }
            synchronized (this) {
                if (this.resultHandler == null && !Boolean.FALSE.equals(existingProxyResultHandler)) {
                    this.resultHandler = SeataProxyInterceptorUtil.tryGetBean(resultHandlerBeanName, resultHandlerClass, true);
                    existingProxyResultHandler = this.resultHandler != null;
                }
            }
        }

        return this.resultHandler;
    }
}
