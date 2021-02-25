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
package io.seata.spring.boot.autoconfigure.properties.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.METRICS_PREFIX;

/**
 * @author spilledyear@outlook.com
 */
@Component
@ConfigurationProperties(prefix = METRICS_PREFIX)
public class MetricsProperties {
    Boolean enabled = false;
    String registryType = "compact";
    String exporterList = "prometheus";
    Integer exporterPrometheusPort = 9898;


    public Boolean getEnabled() {
        return enabled;
    }

    public MetricsProperties setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getRegistryType() {
        return registryType;
    }

    public MetricsProperties setRegistryType(String registryType) {
        this.registryType = registryType;
        return this;
    }

    public String getExporterList() {
        return exporterList;
    }

    public MetricsProperties setExporterList(String exporterList) {
        this.exporterList = exporterList;
        return this;
    }

    public Integer getExporterPrometheusPort() {
        return exporterPrometheusPort;
    }

    public MetricsProperties setExporterPrometheusPort(Integer exporterPrometheusPort) {
        this.exporterPrometheusPort = exporterPrometheusPort;
        return this;
    }
}