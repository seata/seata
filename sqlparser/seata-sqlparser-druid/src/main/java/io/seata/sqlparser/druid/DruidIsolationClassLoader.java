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
package io.seata.sqlparser.druid;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Used for druid isolation.
 *
 * @author ggndnn
 */
class DruidIsolationClassLoader extends URLClassLoader {
    private final static String[] DRUID_CLASS_PREFIX = new String[]{"com.alibaba.druid.", "io.seata.sqlparser.druid."};

    private final static String[] DRUID_RESOURCE_PREFIX = new String[]{"META-INF/seata/io.seata.sqlparser.druid", "META-INF/services/io.seata.sqlparser.druid"};

    private final static DruidIsolationClassLoader INSTANCE = new DruidIsolationClassLoader(DefaultDruidLoader.get());

    DruidIsolationClassLoader(DruidLoader druidLoader) {
        super(getDruidUrls(druidLoader), DruidIsolationClassLoader.class.getClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (String prefix : DRUID_CLASS_PREFIX) {
            if (name.startsWith(prefix)) {
                return loadInternalClass(name, resolve);
            }
        }
        return super.loadClass(name, resolve);
    }

    /**
     * While loading resource files, the resources under the current layer classLoader's path and the parent
     * classLoader's path will be loaded at the same time.{@link ClassLoader#getResources(String)}
     * So we rewrite this method to avoid repeated loading of druid resources which in the
     * {@link #DRUID_RESOURCE_PREFIX}
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        for (String prefix : DRUID_RESOURCE_PREFIX) {
            if (name.startsWith(prefix)) {
                return loadInternalResource(name);
            }
        }
        return super.getResources(name);
    }

    private Enumeration<URL> loadInternalResource(String name) throws IOException {
        Enumeration<URL>[] tmp = (Enumeration<URL>[])new Enumeration<?>[1];
        tmp[0] = findResources(name);
        return new CompoundEnumeration<>(tmp);
    }

    private Class<?> loadInternalClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c;
        synchronized (getClassLoadingLock(name)) {
            c = findLoadedClass(name);
            if (c == null) {
                c = findClass(name);
            }
        }
        if (c == null) {
            throw new ClassNotFoundException(name);
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    private static URL[] getDruidUrls(DruidLoader druidLoader) {
        List<URL> urls = new ArrayList<>();
        urls.add(findClassLocation(DruidIsolationClassLoader.class));
        urls.add(druidLoader.getEmbeddedDruidLocation());
        return urls.toArray(new URL[0]);
    }

    private static URL findClassLocation(Class<?> clazz) {
        CodeSource cs = clazz.getProtectionDomain().getCodeSource();
        if (cs == null) {
            throw new IllegalStateException("Not a normal druid startup environment");
        }
        return cs.getLocation();
    }

    static DruidIsolationClassLoader get() {
        return INSTANCE;
    }

    /**
     * The purpose of this class is to eliminate the difference on {@link #CompoundEnumeration} between jdk 8 and 11
     *
     * @param <E>
     */
    private static class CompoundEnumeration<E> implements Enumeration<E> {
        private Enumeration<E>[] enums;
        private int index = 0;

        public CompoundEnumeration(Enumeration<E>[] enums) {
            this.enums = enums;
        }

        private boolean next() {
            while (this.index < this.enums.length) {
                if (this.enums[this.index] != null && this.enums[this.index].hasMoreElements()) {
                    return true;
                }
                index++;
            }

            return false;
        }

        @Override
        public boolean hasMoreElements() {
            return this.next();
        }

        @Override
        public E nextElement() {
            if (!this.next()) {
                throw new NoSuchElementException();
            } else {
                return this.enums[this.index].nextElement();
            }
        }
    }
}
