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
package io.seata.discovery.registry.kubernetes;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.seata.common.loader.LoadLevel;
import io.seata.discovery.registry.RegistryProvider;
import io.seata.discovery.registry.RegistryService;

/**
 * @author @hero_zhanghao
 * @date 2019/5/6 17:52
 **/
@LoadLevel(name = "Kubernetes", order = 1)
public class KubernetesRegistryProvider implements RegistryProvider {
    @Override
    public RegistryService provide() {
        return KubernetesRegistryServiceImpl.getInstance(new DefaultKubernetesClient());
    }
}
