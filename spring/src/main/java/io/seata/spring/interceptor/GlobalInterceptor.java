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
package io.seata.spring.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import io.seata.spring.annotation.HandleGlobalTransaction;
import io.seata.tm.api.DefaultFailureHandlerImpl;
import io.seata.tm.api.FailureHandler;
import io.seata.tm.api.TransactionalTemplate;

/**
 * @author funkye
 */
public class GlobalInterceptor {

    protected static final FailureHandler DEFAULT_FAIL_HANDLER = new DefaultFailureHandlerImpl();
    protected final HandleGlobalTransaction handleGlobalTransaction = new HandleGlobalTransaction();
    protected final TransactionalTemplate transactionalTemplate = new TransactionalTemplate();

    public <T extends Annotation> T getAnnotation(Method method, Class<?> targetClass, Class<T> annotationClass) {
        return Optional.ofNullable(method).map(m -> m.getAnnotation(annotationClass))
            .orElse(Optional.ofNullable(targetClass).map(t -> t.getAnnotation(annotationClass)).orElse(null));
    }
}
