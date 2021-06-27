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
package io.seata.rm.tcc.api;

import java.util.Map;
import javax.annotation.Nonnull;

/**
 * The interface ParameterFetcher.
 * Warn: The implementation class must contain a no-parameter constructor.
 *
 * @author wang.liang
 * @since above 1.4.2
 */
public interface ParameterFetcher<T> {

    /**
     * fetch context from the param or field
     *
     * @param paramType     the type of the param, 'param' or 'field'
     * @param paramName     the name of the param or field
     * @param paramValue    the value of the param or field
     * @param annotation    the annotation on the param or field
     * @param actionContext the action context
     */
    void fetchContext(@Nonnull ParamType paramType, @Nonnull String paramName, @Nonnull T paramValue,
        @Nonnull BusinessActionContextParameter annotation, @Nonnull final Map<String, Object> actionContext);
}
