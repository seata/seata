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

import io.seata.common.util.StringUtils;

/**
 * The enum Transport protocol type.
 *
 * @author slievrly
 */
public enum TransportProtocolType {
    /**
     * Tcp transport protocol type.
     */
    TCP("tcp"),

    /**
     * Unix domain socket transport protocol type.
     */
    UNIX_DOMAIN_SOCKET("unix-domain-socket");

    /**
     * The Name.
     */
    public final String name;

    TransportProtocolType(String name) {
        this.name = name;
    }

    /**
     * Gets type.
     *
     * @param name the name
     * @return the type
     */
    public static TransportProtocolType getType(String name) {
        if (StringUtils.isNotBlank(name)) {
            name = name.trim().replace('-', '_');
            for (TransportProtocolType b : TransportProtocolType.values()) {
                if (b.name().equalsIgnoreCase(name)) {
                    return b;
                }
            }
        }
        throw new IllegalArgumentException("unknown type:" + name);
    }
}
