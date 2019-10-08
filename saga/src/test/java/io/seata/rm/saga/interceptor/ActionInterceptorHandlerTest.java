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
package io.seata.rm.saga.interceptor;

import io.seata.rm.saga.SagaAction;
import io.seata.rm.saga.SagaParam;
import io.seata.rm.saga.api.BusinessActionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type Action interceptor handler test.
 *
 * @author zhangsen
 */
public class ActionInterceptorHandlerTest {

    /**
     * The Action interceptor handler.
     */
    protected  ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    /**
     * Test business action context.
     *
     * @throws NoSuchMethodException the no such method exception
     */
    @Test
    public void testBusinessActionContext() throws NoSuchMethodException {
        Method prepareMethod = SagaAction.class.getDeclaredMethod("prepare",
                BusinessActionContext.class, int.class, List.class, SagaParam.class);
        List list = new ArrayList();
        list.add("b");
        SagaParam tccParam = new SagaParam(1, "abc@ali.com");

        Map<String, Object>  paramContext = actionInterceptorHandler.fetchActionRequestContext(prepareMethod,
                new Object[]{null, 10, list, tccParam});
        System.out.println(paramContext);

        Assertions.assertEquals(10, paramContext.get("a"));
        Assertions.assertEquals("b", paramContext.get("b"));
        Assertions.assertEquals("abc@ali.com", paramContext.get("email"));
    }

}
