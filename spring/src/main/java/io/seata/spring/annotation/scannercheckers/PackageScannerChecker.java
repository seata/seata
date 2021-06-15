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
package io.seata.spring.annotation.scannercheckers;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

import io.seata.common.loader.LoadLevel;
import io.seata.spring.annotation.ScannerChecker;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Package scanner checker.
 *
 * @author wang.liang
 */
@LoadLevel(name = "Packages", order = 100)
public class PackageScannerChecker implements ScannerChecker {

    /**
     * The packages need to scan
     */
    private static final Set<String> SCANNABLE_PACKAGE_SET;

    static {
        SCANNABLE_PACKAGE_SET = new HashSet<>();
        SCANNABLE_PACKAGE_SET.add("io.seata.spring.tcc");
    }

    /**
     * Add more packages.
     *
     * @param packages the packages
     */
    public static void addScannablePackages(String... packages) {
        if (ArrayUtils.isNotEmpty(packages)) {
            synchronized (SCANNABLE_PACKAGE_SET) {
                for (String pkg : packages) {
                    if (StringUtils.isNotBlank(pkg)) {
                        SCANNABLE_PACKAGE_SET.add(pkg.trim().toLowerCase());
                    }
                }
            }
        }
    }

    @Override
    public boolean check(Object bean, String beanName, @Nullable ConfigurableListableBeanFactory beanFactory) throws Exception {
        if (SCANNABLE_PACKAGE_SET.isEmpty()) {
            // if empty, pass this checker
            return true;
        }

        String className = bean.getClass().getName();
        for (String pkg : SCANNABLE_PACKAGE_SET) {
            if (className.startsWith(pkg)) {
                // need scan
                return true;
            }
        }

        // not in the scannable packages, do not scan this bean
        return false;
    }
}
