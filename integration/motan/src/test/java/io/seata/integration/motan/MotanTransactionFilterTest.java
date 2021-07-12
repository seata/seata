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
package io.seata.integration.motan;

import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.ServiceConfig;
import io.seata.core.context.RootContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author slievrly
 */
class MotanTransactionFilterTest {

    private static final String SERVICE_GROUP = "motan";
    private static final String SERVICE_VERSION = "1.0.0";
    private static final int SERVICE_PORT = 8004;
    private static final String PROTOCOL_ID = "motan";
    private static final String PROTOCOL_NAME = "motan";
    private static final String XID = "127.0.0.1:8091:87654321";
    private static final int REQUEST_TIMEOUT = 1000;

    @Test
    void testGetProviderXID() {
        RootContext.bind(XID);
        providerStart();
        consumerStart();
        RootContext.unbind();
    }

    public void providerStart() {
        ServiceConfig<XIDService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(XIDService.class);
        serviceConfig.setRef(new XIDServiceImpl());
        serviceConfig.setGroup(SERVICE_GROUP);
        serviceConfig.setVersion(SERVICE_VERSION);
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setRegProtocol("local");
        registryConfig.setCheck(false);
        serviceConfig.setRegistry(registryConfig);
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId(PROTOCOL_ID);
        protocol.setName(PROTOCOL_NAME);
        serviceConfig.setProtocol(protocol);
        serviceConfig.setExport("motan:" + SERVICE_PORT);
        serviceConfig.export();
    }

    private void consumerStart() {
        RefererConfig<XIDService> refererConfig = new RefererConfig<>();
        refererConfig.setInterface(XIDService.class);
        refererConfig.setGroup(SERVICE_GROUP);
        refererConfig.setVersion(SERVICE_VERSION);
        refererConfig.setRequestTimeout(REQUEST_TIMEOUT);
        RegistryConfig registry = new RegistryConfig();
        refererConfig.setRegistry(registry);
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId(PROTOCOL_ID);
        protocol.setName(PROTOCOL_NAME);
        refererConfig.setProtocol(protocol);
        refererConfig.setDirectUrl("127.0.0.1:" + SERVICE_PORT);
        XIDService service = refererConfig.getRef();
        Assertions.assertEquals(service.getXid(), XID);
    }
}
