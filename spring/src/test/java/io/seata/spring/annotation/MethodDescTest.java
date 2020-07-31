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
package io.seata.spring.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import io.seata.common.exception.FrameworkException;
import io.seata.core.context.RootContext;
import io.seata.spring.interceptor.GlobalTransactionalInterceptor;
import io.seata.tm.api.transaction.TransactionInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.framework.ProxyFactory;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Wu
 */
public class MethodDescTest {

    private static final GlobalTransactionScanner GLOBAL_TRANSACTION_SCANNER = new GlobalTransactionScanner(
        "global-trans-scanner-test");
    private static Method method = null;
    private static Class<?> targetClass = null;
    private static GlobalTransactional transactional = null;

    public MethodDescTest() throws NoSuchMethodException {
        method = MockBusiness.class.getDeclaredMethod("doBiz", String.class);
        transactional = method.getAnnotation(GlobalTransactional.class);
    }

    @Test
    public void testGetAnnotation() throws NoSuchMethodException {
        GlobalTransactionalInterceptor globalTransactionalInterceptor = new GlobalTransactionalInterceptor(null);
        Method method = MockBusiness.class.getDeclaredMethod("doBiz", String.class);
        targetClass = Mockito.mock(MockBusiness.class).getClass();
        transactional = globalTransactionalInterceptor.getAnnotation(method, targetClass, GlobalTransactional.class);
        Assertions.assertEquals(transactional.timeoutMills(), 300000);
        method = null;
        transactional = globalTransactionalInterceptor.getAnnotation(method, targetClass, GlobalTransactional.class);
        Assertions.assertEquals(transactional.timeoutMills(), TransactionInfo.DEFAULT_TIME_OUT * 2);
        targetClass = null;
        transactional = globalTransactionalInterceptor.getAnnotation(method, targetClass, GlobalTransactional.class);
        Assertions.assertNull(transactional);
        // only class has Annotation, method is not null
        targetClass = Mockito.mock(MockMethodAnnotation.class).getClass();
        method = MockMethodAnnotation.class.getDeclaredMethod("doBiz", String.class);
        transactional = globalTransactionalInterceptor.getAnnotation(method, targetClass, GlobalTransactional.class);
        Assertions.assertEquals(transactional.name(), "doBiz");
        // only method has Annotation, class is not null
        targetClass = Mockito.mock(MockClassAnnotation.class).getClass();
        method = MockClassAnnotation.class.getDeclaredMethod("doBiz", String.class);
        transactional = globalTransactionalInterceptor.getAnnotation(method, targetClass, GlobalTransactional.class);
        Assertions.assertEquals(transactional.name(), "MockClassAnnotation");
    }

    @Test
    public void testGlobalTransactional() throws NoSuchMethodException {
        MockClassAnnotation mockClassAnnotation = new MockClassAnnotation();
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(mockClassAnnotation);
        proxyFactory.addAdvice(new GlobalTransactionalInterceptor(null));
        Object proxy = proxyFactory.getProxy();
        mockClassAnnotation = (MockClassAnnotation)proxy;
        mockClassAnnotation.toString();
        Assertions.assertNull(RootContext.getXID());
        mockClassAnnotation.hashCode();
        Assertions.assertNull(RootContext.getXID());
        mockClassAnnotation.equals("test");
        Assertions.assertNull(RootContext.getXID());
        try {
            mockClassAnnotation.doBiz("test");
        } catch (FrameworkException e) {
            Assertions.assertEquals("No available service", e.getMessage());
        }
    }

    @Test
    public void testGetTransactionAnnotation()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc = getMethodDesc();
        assertThat(methodDesc.getTransactionAnnotation()).isEqualTo(transactional);

    }

    @Test
    public void testGetMethod() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc = getMethodDesc();
        assertThat(methodDesc.getMethod()).isEqualTo(method);
    }

    @Test
    public void testSetTransactionAnnotation()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc = getMethodDesc();
        assertThat(methodDesc.getTransactionAnnotation()).isNotNull();
        methodDesc.setTransactionAnnotation(null);
        assertThat(methodDesc.getTransactionAnnotation()).isNull();
    }

    @Test
    public void testSetMethod() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodDesc methodDesc = getMethodDesc();
        assertThat(methodDesc.getMethod()).isNotNull();
        methodDesc.setMethod(null);
        assertThat(methodDesc.getMethod()).isNull();
    }

    private MethodDesc getMethodDesc() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //call the private method
        Method m = GlobalTransactionScanner.class.getDeclaredMethod("makeMethodDesc", GlobalTransactional.class,
            Method.class);
        m.setAccessible(true);
        return (MethodDesc)m.invoke(GLOBAL_TRANSACTION_SCANNER, transactional, method);

    }

    /**
     * the type mock business
     */
    @GlobalTransactional(timeoutMills = TransactionInfo.DEFAULT_TIME_OUT * 2)
    private static class MockBusiness {
        @GlobalTransactional(timeoutMills = 300000, name = "busi-doBiz")
        public String doBiz(String msg) {
            return "hello " + msg;
        }
    }

    /**
     * the type mock class annotation
     */
    @GlobalTransactional(name = "MockClassAnnotation")
    private static class MockClassAnnotation {
        public String doBiz(String msg) {
            return "hello " + msg;
        }
    }

    /**
     * the type mock method annotation
     */
    private static class MockMethodAnnotation {
        @GlobalTransactional(name = "doBiz")
        public String doBiz(String msg) {
            return "hello " + msg;
        }
    }

}
