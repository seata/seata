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
package io.seata.common.util;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import io.seata.common.testmodel.NotExistsNoArgsConstructorTestClass;
import io.seata.common.testmodel.TestClass;
import io.seata.common.testmodel.TestEmptyClass;
import io.seata.common.testmodel.TestInterface;
import io.seata.common.testmodel.TestSuperClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReflectionUtilTest {

    //Prevent jvm from optimizing final
    public static final String testValue = (null != null ? "hello" : "hello");

    @Test
    public void testGetClassByName() throws ClassNotFoundException {
        Assertions.assertEquals(String.class,
                ReflectionUtil.getClassByName("java.lang.String"));
    }

    @Test
    public void testGetFieldValue() throws
            NoSuchFieldException, IllegalAccessException {
        Assertions.assertEquals("d",
                ReflectionUtil.getFieldValue(new DurationUtil(), "DAY_UNIT"));

        Assertions.assertThrows(NoSuchFieldException.class,
                () -> ReflectionUtil.getFieldValue(new Object(), "A1B2C3"));
    }

    @Test
    public void testInvokeMethod() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Assertions.assertEquals(0, ReflectionUtil.invokeMethod("", "length"));
        Assertions.assertEquals(3,
                ReflectionUtil.invokeMethod("foo", "length"));

        Assertions.assertThrows(NoSuchMethodException.class,
                () -> ReflectionUtil.invokeMethod("", "size"));
    }

    @Test
    public void testInvokeMethod2() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Assertions.assertEquals(0, ReflectionUtil
                .invokeMethod("", "length", null, null));
        Assertions.assertEquals(3, ReflectionUtil
                .invokeMethod("foo", "length", null, null));

        Assertions.assertThrows(NoSuchMethodException.class, () -> ReflectionUtil
                .invokeMethod("", "size", null, null));
    }

    @Test
    public void testInvokeMethod3() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Assertions.assertEquals("0", ReflectionUtil.invokeStaticMethod(
                String.class, "valueOf",
                new Class<?>[]{int.class}, new Object[]{0}));
        Assertions.assertEquals("123", ReflectionUtil.invokeStaticMethod(
                String.class, "valueOf",
                new Class<?>[]{int.class}, new Object[]{123}));

        Assertions.assertThrows(NoSuchMethodException.class, () -> ReflectionUtil
                .invokeStaticMethod(String.class, "size", null, null));
    }

    @Test
    public void testGetInterfaces() {
        Assertions.assertArrayEquals(new Object[]{Serializable.class},
                ReflectionUtil.getInterfaces(Serializable.class).toArray());

        Assertions.assertArrayEquals(new Object[]{
                        Serializable.class, Comparable.class, CharSequence.class},
                ReflectionUtil.getInterfaces(String.class).toArray());
    }

    @Test
    public void testModifyStaticFinalField() throws NoSuchFieldException, IllegalAccessException {
        Assertions.assertEquals("hello", testValue);
        ReflectionUtil.modifyStaticFinalField(ReflectionUtilTest.class, "testValue", "hello world");
        Assertions.assertEquals("hello world", testValue);
    }


    //region test the method 'getAllFields'

    @Test
    public void testGetAllFields() {
        // TestClass
        this.testGetAllFieldsInternal(TestClass.class, "f1", "f2");
        // TestSuperClass
        this.testGetAllFieldsInternal(TestSuperClass.class, "f2");
        // TestEmptyClass
        this.testGetAllFieldsInternal(TestEmptyClass.class);
        // TestInterface
        this.testGetAllFieldsInternal(TestInterface.class);
        // Object
        this.testGetAllFieldsInternal(Object.class);

        // case: The fields of TestEmptyClass is `EMPTY_FIELD_ARRAY`
        Assertions.assertTrue(ReflectionUtil.getAllFields(TestEmptyClass.class) == ReflectionUtil.EMPTY_FIELD_ARRAY);
        // case: The fields of TestInterface is `EMPTY_FIELD_ARRAY`
        Assertions.assertTrue(ReflectionUtil.getAllFields(TestInterface.class) == ReflectionUtil.EMPTY_FIELD_ARRAY);
        // case: The fields of Object is `EMPTY_FIELD_ARRAY`
        Assertions.assertTrue(ReflectionUtil.getAllFields(Object.class) == ReflectionUtil.EMPTY_FIELD_ARRAY);
    }

    @Test
    public void testGetSingleton() {
        // case: normal Class
        TestClass testClass = ReflectionUtil.getSingleton(TestClass.class);
        Assertions.assertNotNull(testClass);

        // case: null
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ReflectionUtil.getSingleton(null);
        });

        // case: Interface
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ReflectionUtil.getSingleton(TestInterface.class);
        });

        // case: not Exists No-Args Constructor Class
        try {
            ReflectionUtil.getSingleton(NotExistsNoArgsConstructorTestClass.class);
        } catch (RuntimeException e) {
            Assertions.assertEquals(IllegalArgumentException.class, e.getClass());
            Assertions.assertEquals(InstantiationException.class, e.getCause().getClass());
            Assertions.assertEquals(NoSuchMethodException.class, e.getCause().getCause().getClass());
        }

        // case: Inner class
        try {
            ReflectionUtil.getSingleton(TestClass.TestInnerClass.class);
        } catch (RuntimeException e) {
            Assertions.assertEquals(IllegalArgumentException.class, e.getClass());
            Assertions.assertEquals(InstantiationException.class, e.getCause().getClass());
            Assertions.assertEquals(NoSuchMethodException.class, e.getCause().getCause().getClass());
        }
    }

    private void testGetAllFieldsInternal(Class<?> clazz, String... fieldNames) {
        Field[] fields = ReflectionUtil.getAllFields(clazz);
        Assertions.assertEquals(fieldNames.length, fields.length);
        Field[] fields2 = ReflectionUtil.getAllFields(clazz);
        // same instance, use the `==`
        Assertions.assertTrue(fields == fields2);

        if (fieldNames.length == 0) {
            return;
        }

        List<String> fieldNameList = Arrays.asList(fieldNames);
        for (Field field : fields) {
            Assertions.assertTrue(fieldNameList.contains(field.getName()));
        }
    }

    //endregion
}
