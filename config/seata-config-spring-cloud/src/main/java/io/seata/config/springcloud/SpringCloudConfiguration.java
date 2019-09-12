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
package io.seata.config.springcloud;

import io.seata.common.util.StringUtils;
import io.seata.config.AbstractConfiguration;
import io.seata.config.ConfigChangeListener;

import java.util.List;


public class SpringCloudConfiguration extends AbstractConfiguration<ConfigChangeListener> {

    private static final String CONFIG_TYPE = "SpringCloudConfig";
    private static SpringCloudConfiguration instance;
    private static final String PREFIX = "seata.";
    public static SpringCloudConfiguration getInstance() {
        if (null == instance) {
            synchronized (SpringCloudConfiguration.class) {
                if (null == instance) {
                    instance = new SpringCloudConfiguration();
                }
            }
        }
        return instance;
    }

    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        if (null == SpringContextProvider.getEnvironment()) {
            return defaultValue;
        }
        String conf = SpringContextProvider.getEnvironment().getProperty(PREFIX + dataId);
        return StringUtils.isNotBlank(conf) ? conf : defaultValue;
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        return false;
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        return false;
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        return false;
    }

    @Override
    public void addConfigListener(String dataId, ConfigChangeListener listener) {

    }

    @Override
    public void removeConfigListener(String dataId, ConfigChangeListener listener) {

    }

    @Override
    public List<ConfigChangeListener> getConfigListeners(String dataId) {
        return null;
    }
}
