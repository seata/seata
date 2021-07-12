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
package io.seata.rm.datasource.lcn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.seata.common.util.CollectionUtils;

/**
 * LCN-Xid builder.
 *
 * @author funkye
 */
public class LcnXidBuilder {

    private static volatile Map<String, List<ConnectionProxyLcn>> concurrentHashMap = new ConcurrentHashMap<>();

    private LcnXidBuilder() {}

    public static synchronized void registerConn(String xid, ConnectionProxyLcn conn) {
        List<ConnectionProxyLcn> connList = concurrentHashMap.get(xid);
        if (CollectionUtils.isEmpty(connList)) {
            connList = new ArrayList<>();
        }
        connList.add(conn);
        concurrentHashMap.put(xid, connList);
    }

    public static List<ConnectionProxyLcn> getConnectionList(String xid) {
        return concurrentHashMap.get(xid);
    }

    public static List<ConnectionProxyLcn> remove(String xid) {
        return concurrentHashMap.remove(xid);
    }

    public Map<String, List<ConnectionProxyLcn>> getConcurrentHashMap() {
        return concurrentHashMap;
    }

    public void setConcurrentHashMap(Map<String, List<ConnectionProxyLcn>> concurrentHashMap) {
        this.concurrentHashMap = concurrentHashMap;
    }
}
