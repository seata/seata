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

import java.lang.reflect.Method;
import javax.annotation.Nullable;

import io.seata.common.util.ReflectionUtil;
import io.seata.spring.proxy.SeataProxy;
import io.seata.spring.proxy.SeataProxyHandler;
import io.seata.spring.proxy.SeataProxyResultHandler;
import io.seata.spring.proxy.SeataProxyValidator;
import io.seata.spring.proxy.util.SeataProxyInterceptorUtil;

/**
 * The SeataProxy method Desc
 *
 * @author wang.liang
 * @see SeataProxyParser
 * @see SeataProxyBeanDesc
 * @see SeataProxyImplementationDesc
 */
public class SeataProxyMethodDesc {

    private Method method;

    private boolean shouldSkip = false;

    private SeataProxyImplementationDesc implDesc;

    public SeataProxyMethodDesc(Method method, boolean shouldSkip, SeataProxyImplementationDesc implDesc) {
        this.method = method;
        this.shouldSkip = shouldSkip;
        this.implDesc = implDesc;
    }

    public SeataProxyMethodDesc(Method method) {
        this.method = method;
        SeataProxy annotation = ReflectionUtil.getAnnotation(method, SeataProxy.class);
        if (annotation != null) {
            this.shouldSkip = SeataProxyInterceptorUtil.isShouldSkip(annotation);
            if (!this.shouldSkip) {
                this.implDesc = new SeataProxyImplementationDesc(annotation);
            }
        }
    }

    public boolean isShouldSkip() {
        return shouldSkip;
    }

    public Method getMethod() {
        return method;
    }

    @Nullable
    public SeataProxyImplementationDesc getImplDesc() {
        return implDesc;
    }

    @Nullable
    public SeataProxyValidator getValidator() {
        return implDesc == null ? null : implDesc.getValidator();
    }

    @Nullable
    public SeataProxyHandler getHandler() {
        return implDesc == null ? null : implDesc.getHandler();
    }

    @Nullable
    public SeataProxyResultHandler getResultHandler() {
        return implDesc == null ? null : implDesc.getResultHandler();
    }
}
