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
package io.seata.core.rpc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type TransportProtocolType test.
 *
 * @author wang.liang
 */
class TransportProtocolTypeTest {

    @Test
    void test_getType() {
        Assertions.assertEquals(TransportProtocolType.getType("tcp"), TransportProtocolType.TCP);
        Assertions.assertEquals(TransportProtocolType.getType("TCP"), TransportProtocolType.TCP);

        Assertions.assertEquals(TransportProtocolType.getType("UNIX_DOMAIN_SOCKET"), TransportProtocolType.UNIX_DOMAIN_SOCKET);
        Assertions.assertEquals(TransportProtocolType.getType("unix_domain_socket"), TransportProtocolType.UNIX_DOMAIN_SOCKET);
        Assertions.assertEquals(TransportProtocolType.getType("UNIX-DOMAIN-SOCKET"), TransportProtocolType.UNIX_DOMAIN_SOCKET);
        Assertions.assertEquals(TransportProtocolType.getType("unix-domain-socket"), TransportProtocolType.UNIX_DOMAIN_SOCKET);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            TransportProtocolType.getType(null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            TransportProtocolType.getType("null");
        });
    }
}
