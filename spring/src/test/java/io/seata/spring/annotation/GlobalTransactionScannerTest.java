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

import java.util.stream.Stream;

import io.seata.spring.schema.SimpleBusinessImpl;
import io.seata.spring.tcc.LocalTccAction;
import io.seata.spring.tcc.LocalTccActionImpl;
import io.seata.spring.tcc.TccAction;
import io.seata.spring.tcc.TccActionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * GlobalTransactionScanner Unit Test
 */
public class GlobalTransactionScannerTest {
    /**
     * The Global transaction scanner.
     */
    protected GlobalTransactionScanner globalTransactionScanner = new GlobalTransactionScanner("global-trans-scanner-test");


    @ParameterizedTest
    @MethodSource("schemaBeanProvider")
    public void testWrapSchemaBean(Object bean, String beanName, Object cacheKey) {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:seata.xml");
        GlobalTransactionScanner globalTransactionScanner = applicationContext.getBean(GlobalTransactionScanner.class);
        Object result = globalTransactionScanner.wrapIfNecessary(bean, beanName, cacheKey);
        Assertions.assertNotSame(result, bean);
    }

    /**
     * Test wrap normal bean.
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @param cacheKey the cache key
     */
    @ParameterizedTest
    @MethodSource("normalBeanProvider")
    public void testWrapNormalBean(Object bean, String beanName, Object cacheKey) {
        Object result = globalTransactionScanner.wrapIfNecessary(bean, beanName, cacheKey);
        Assertions.assertNotSame(result, bean);
    }

    /**
     * wrap nothing
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @param cacheKey the cache key
     */
    @ParameterizedTest
    @MethodSource("normalTccBeanProvider")
    public void testWrapNormalTccBean(Object bean, String beanName, Object cacheKey) {
        Object result = globalTransactionScanner.wrapIfNecessary(bean, beanName, cacheKey);
        Assertions.assertSame(result, bean);
    }

    /**
     * wrapped
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @param cacheKey the cache key
     */
    @ParameterizedTest
    @MethodSource("localTccBeanProvider")
    public void testWrapLocalTccBean(Object bean, String beanName, Object cacheKey) {
        TccAction result = (LocalTccAction) globalTransactionScanner.wrapIfNecessary(bean, beanName, cacheKey);
        Assertions.assertNotSame(result, bean);
    }

    /**
     * Test after properties set.
     */
    @Test
    public void testAfterPropertiesSet() {
        globalTransactionScanner.afterPropertiesSet();
    }

    /**
     * Normal bean provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> normalBeanProvider() {
        Business business = new BusinessImpl();
        String beanName = "business";
        String cacheKey = "business-key";
        return Stream.of(
            Arguments.of(business, beanName, cacheKey)
        );
    }

    /**
     * schema bean provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> schemaBeanProvider() {
        Business business = new SimpleBusinessImpl();
        String beanName = "schemaBean";
        String cacheKey = "schemaBean-key";
        return Stream.of(
            Arguments.of(business, beanName, cacheKey)
        );
    }

    /**
     * Normal tcc bean provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> normalTccBeanProvider() {
        TccAction tccAction = new TccActionImpl();
        String beanName = "tccBean";
        String cacheKey = "tccBean-key";
        return Stream.of(
            Arguments.of(tccAction, beanName, cacheKey)
        );
    }

    /**
     * Local tcc bean provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> localTccBeanProvider() {
        LocalTccAction localTccAction = new LocalTccActionImpl();
        String beanName = "lcoalTccBean";
        String cacheKey = "lcoalTccBean-key";
        return Stream.of(
            Arguments.of(localTccAction, beanName, cacheKey)
        );
    }
}
