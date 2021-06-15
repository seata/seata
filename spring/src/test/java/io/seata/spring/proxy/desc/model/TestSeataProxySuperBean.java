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
package io.seata.spring.proxy.desc.model;

import io.seata.spring.proxy.SeataProxy;

public class TestSeataProxySuperBean {

    public static void testStaticSuper() {
    }


    public void testHasNoAnnotation() {
    }

    @SeataProxy
    public void testHasAnnotation() {
    }

    @SeataProxy(skip = true)
    public void testSkipMethod() {
    }


    @SeataProxy(skip = true)
    public void testOverrideAnnotation1() {
    }


    @SeataProxy(skip = false)
    public void testOverrideAnnotation2() {
    }

    private void testPrivateMethod() {
    }
}
