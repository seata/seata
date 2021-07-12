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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TCC annotation.
 * Define a TCC interface, which added on the try method.
 * Must be used with `@LocalTCC`.
 *
 * @author zhangsen
 * @see io.seata.rm.tcc.api.LocalTCC // TCC annotation, which added on the TCC interface. It can't be left out.
 * @see io.seata.spring.annotation.GlobalTransactionScanner#wrapIfNecessary(Object, String, Object) // the scanner for TM, GlobalLock, and TCC mode
 * @see io.seata.spring.tcc.TccActionInterceptor // the interceptor of TCC mode
 * @see BusinessActionContext
 * @see BusinessActionContextUtil
 * @see BusinessActionContextParameter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface TwoPhaseBusinessAction {

    /**
     * TCC bean name, must be unique
     *
     * @return the string
     */
    String name();

    /**
     * commit type
     *
     * @return the commit type
     */
    TCCCommitType commitType() default TCCCommitType.SyncCommit;

    /**
     * commit method name
     *
     * @return the string
     */
    String commitMethod() default "commit";

    /**
     * rollback method name
     *
     * @return the string
     */
    String rollbackMethod() default "rollback";

    /**
     * delay branch report while sharing params to tcc phase 2 to enhance performance
     *
     * @return isDelayReport
     */
    boolean isDelayReport() default false;

    /**
     * whether use TCC fence (idempotent,non_rollback,suspend)
     *
     * @return the boolean
     */
    boolean useTCCFence() default false;
}
