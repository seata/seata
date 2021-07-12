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
package io.seata.spring.schema;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.spring.annotation.GlobalLock;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.PatternMatchUtils;

/**
 * The type seata target scanner
 *
 * @author xingfudeshi@gmail.com
 */
public class SeataTargetScanner implements ResourceLoaderAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataTargetScanner.class);
    private static final String PATTERN_CLASS = "@CLASS";
    private static final String PATTERN_METHOD = "@METHOD";
    private final Set<GlobalTransactionalConfig> globalTransactionalConfigs;
    private final Set<GlobalLockConfig> globalLockConfigs;

    public SeataTargetScanner(Set<GlobalTransactionalConfig> globalTransactionalConfigs, Set<GlobalLockConfig> globalLockConfigs) {
        this.globalTransactionalConfigs = globalTransactionalConfigs;
        this.globalLockConfigs = globalLockConfigs;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        try {
            ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
            MetadataReaderFactory metaReader = new CachingMetadataReaderFactory(resourceLoader);
            processGlobalTransactionalConfigs(resolver, metaReader);
            processGlobalLockConfigs(resolver, metaReader);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * process global transactional configs
     *
     * @param resolver
     * @param metaReader
     * @return void
     * @author xingfudeshi@gmail.com
     */
    private void processGlobalTransactionalConfigs(ResourcePatternResolver resolver, MetadataReaderFactory metaReader) throws IOException, ClassNotFoundException {
        for (GlobalTransactionalConfig globalTransactionalConfig : this.globalTransactionalConfigs) {
            Resource[] resources = resolver.getResources("classpath*:" + formatPackageName(globalTransactionalConfig.getScanPackage()) + "/*.class");
            for (Resource r : resources) {
                MetadataReader reader = metaReader.getMetadataReader(r);
                String className = reader.getClassMetadata().getClassName();

                String pattern = globalTransactionalConfig.getPattern();
                if (StringUtils.startsWith(pattern, PATTERN_CLASS)) {
                    String regex = StringUtils.split(pattern, ":")[1];
                    if (PatternMatchUtils.simpleMatch(regex, className)) {
                        SeataTarget seataTarget = new SeataTarget(SeataTargetType.CLASS, className, GlobalTransactional.class, globalTransactionalConfig);
                        SeataTargetHolder.INSTANCE.add(seataTarget);
                    }
                } else if (StringUtils.startsWith(pattern, PATTERN_METHOD)) {
                    String regex = StringUtils.split(pattern, ":")[1];
                    Class<?> targetClass = Class.forName(className);
                    Method[] methods = targetClass.getMethods();
                    for (Method method : methods) {
                        String name = method.getName();
                        if (PatternMatchUtils.simpleMatch(regex, name)) {
                            SeataTarget seataTarget = new SeataTarget(SeataTargetType.METHOD, name, GlobalTransactional.class, globalTransactionalConfig);
                            SeataTargetHolder.INSTANCE.add(seataTarget);
                        }
                    }


                } else {
                    throw new ShouldNeverHappenException("pattern [" + pattern + "] is not supported yet");
                }
            }
        }
    }

    /**
     * process global lock configs
     *
     * @param resolver
     * @param metaReader
     * @return void
     * @author xingfudeshi@gmail.com
     */
    private void processGlobalLockConfigs(ResourcePatternResolver resolver, MetadataReaderFactory metaReader) throws IOException, ClassNotFoundException {
        for (GlobalLockConfig globalLockConfig : this.globalLockConfigs) {
            Resource[] resources = resolver.getResources("classpath*:" + formatPackageName(globalLockConfig.getScanPackage()) + "/*.class");
            for (Resource r : resources) {
                MetadataReader reader = metaReader.getMetadataReader(r);
                String className = reader.getClassMetadata().getClassName();
                String pattern = globalLockConfig.getPattern();
                Class<?> targetClass = Class.forName(className);
                Method[] methods = targetClass.getMethods();
                for (Method method : methods) {
                    String name = method.getName();
                    if (PatternMatchUtils.simpleMatch(pattern, name)) {
                        SeataTarget seataTarget = new SeataTarget(SeataTargetType.METHOD, name, GlobalLock.class, globalLockConfig);
                        SeataTargetHolder.INSTANCE.add(seataTarget);
                    }
                }
            }
        }
    }


    /**
     * format package name
     *
     * @param packageName
     * @return java.lang.String
     * @author xingfudeshi@gmail.com
     */
    private static String formatPackageName(String packageName) {
        if (StringUtils.isBlank(packageName)) {
            return StringUtils.EMPTY;
        }
        return packageName.replace(".", "/");
    }

}
