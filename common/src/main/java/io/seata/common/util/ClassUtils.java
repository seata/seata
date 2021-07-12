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

import static io.seata.common.util.StringFormatUtils.DOT;

/**
 * @author wang.liang
 */
public class ClassUtils {

    private ClassUtils() {
    }

    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;

        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ignored) {
        }

        if (cl == null) {
            cl = ClassUtils.class.getClassLoader();
            if (cl == null) {
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ignored) {
                }
            }
        }

        return cl;
    }

    public static Class<?> forName(String name, ClassLoader classLoader) throws ClassNotFoundException {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }

        if (classLoader == null) {
            classLoader = getDefaultClassLoader();
        }

        try {
            return simpleForName(name, classLoader);
        } catch (ClassNotFoundException e) {
            int lastDotIndex = name.lastIndexOf(DOT);
            if (lastDotIndex != -1) {
                // The last dot '.' replace to '$'
                String innerClassName = name.substring(0, lastDotIndex) + '$' + name.substring(lastDotIndex + 1);

                try {
                    return simpleForName(innerClassName, classLoader);
                } catch (ClassNotFoundException ignored) {
                }
            }

            throw e;
        }
    }

    public static Class<?> forName(String name) throws ClassNotFoundException {
        return forName(name, null);
    }

    static Class<?> simpleForName(String name, ClassLoader classLoader) throws ClassNotFoundException {
        return classLoader != null ? classLoader.loadClass(name) : Class.forName(name);
    }

    public static boolean isPresent(String className, ClassLoader classLoader) {
        try {
            forName(className, classLoader);
            return true;
        } catch (Throwable ex) {
            // Class or one of its dependencies is not present...
            return false;
        }
    }

    public static boolean isPresent(String className) {
        return isPresent(className, null);
    }
}
