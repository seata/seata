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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation SeataProxy
 *
 * @author wang.liang
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface SeataProxy {

    /**
     * do not proxy when the skip() is true.
     * Synonym for {@link #skip()}.
     *
     * @return the boolean
     */
    boolean value() default false;

    /**
     * do not proxy when the skip() is true.
     * Synonym for {@link #value()}.
     *
     * @return the boolean
     */
    boolean skip() default false;


    /**
     * scan the methods which annotated by {@link SeataProxy}
     *
     * @return the boolean
     */
    boolean onlyScanAnnotatedMethods() default false;


    //region Implementation class related properties

    /**
     * the validator class
     *
     * @return the class
     */
    Class<? extends SeataProxyValidator> validatorClass() default SeataProxyValidator.class;

    /**
     * the validator bean name
     *
     * @return the bean name
     */
    String validatorBeanName() default "";


    /**
     * the handler class
     *
     * @return the class
     */
    Class<? extends SeataProxyHandler> handlerClass() default SeataProxyHandler.class;

    /**
     * the handler bean name
     *
     * @return the bean name
     */
    String handlerBeanName() default "";

    /**
     * the result handler class
     *
     * @return the class
     */
    Class<? extends SeataProxyResultHandler> resultHandlerClass() default SeataProxyResultHandler.class;

    /**
     * the result handler bean name
     *
     * @return the bean name
     */
    String resultHandlerBeanName() default "";

    //endregion
}
