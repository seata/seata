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
package io.seata.discovery.loadbalance;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import io.seata.common.rpc.RpcStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Created by guoyao on 2019/2/14.
 */
public class LoadBalanceTest {

    private static final String XID = "XID";

    /**
     * Test random load balance select.
     *
     * @param addresses the addresses
     */
    @ParameterizedTest
    @MethodSource("addressProvider")
    public void testRandomLoadBalance_select(List<InetSocketAddress> addresses) {
        int runs = 10000;
        Map<InetSocketAddress, AtomicLong> counter = getSelectedCounter(runs, addresses, new RandomLoadBalance());
        for (InetSocketAddress address : counter.keySet()) {
            Long count = counter.get(address).get();
            Assertions.assertTrue(count > 0, "selecte one time at last");
        }
    }

    /**
     * Test round robin load balance select.
     *
     * @param addresses the addresses
     */
    @ParameterizedTest
    @MethodSource("addressProvider")
    public void testRoundRobinLoadBalance_select(List<InetSocketAddress> addresses) {
        int runs = 10000;
        Map<InetSocketAddress, AtomicLong> counter = getSelectedCounter(runs, addresses, new RoundRobinLoadBalance());
        for (InetSocketAddress address : counter.keySet()) {
            Long count = counter.get(address).get();
            Assertions.assertTrue(Math.abs(count - runs / (0f + addresses.size())) < 1f, "abs diff shoud < 1");
        }
    }

    /**
     * Test consistent hash load load balance select.
     *
     * @param addresses the addresses
     */
    @ParameterizedTest
    @MethodSource("addressProvider")
    public void testConsistentHashLoadBalance_select(List<InetSocketAddress> addresses) {
        int runs = 10000;
        int selected = 0;
        Map<InetSocketAddress, AtomicLong> counter = getSelectedCounter(runs, addresses, new ConsistentHashLoadBalance());
        for (InetSocketAddress address : counter.keySet()) {
            if (counter.get(address).get() > 0) {
                selected++;
            }
        }
        Assertions.assertEquals(1, selected, "selected must be equal to 1");
    }

    /**
     * Test least active load balance select.
     *
     * @param addresses the addresses
     */
    @ParameterizedTest
    @MethodSource("addressProvider")
    public void testLeastActiveLoadBalance_select(List<InetSocketAddress> addresses) throws Exception {
        int runs = 10000;
        int size = addresses.size();
        for (int i = 0; i < size - 1; i++) {
            RpcStatus.beginCount(addresses.get(i).toString());
        }
        InetSocketAddress socketAddress = addresses.get(size - 1);
        LoadBalance loadBalance = new LeastActiveLoadBalance();
        for (int i = 0; i < runs; i++) {
            InetSocketAddress selectAddress = loadBalance.select(addresses, XID);
            Assertions.assertEquals(selectAddress, socketAddress);
        }
        RpcStatus.beginCount(socketAddress.toString());
        RpcStatus.beginCount(socketAddress.toString());
        Map<InetSocketAddress, AtomicLong> counter = getSelectedCounter(runs, addresses, loadBalance);
        for (InetSocketAddress address : counter.keySet()) {
            Long count = counter.get(address).get();
            if (address == socketAddress) {
                Assertions.assertEquals(count, 0);
            } else {
                Assertions.assertTrue(count > 0);
            }
        }
    }

    /**
     * Gets selected counter.
     *
     * @param runs        the runs
     * @param addresses   the addresses
     * @param loadBalance the load balance
     * @return the selected counter
     */
    public Map<InetSocketAddress, AtomicLong> getSelectedCounter(int runs, List<InetSocketAddress> addresses,
                                                                 LoadBalance loadBalance) {
        Assertions.assertNotNull(loadBalance);
        Map<InetSocketAddress, AtomicLong> counter = new ConcurrentHashMap<>();
        for (InetSocketAddress address : addresses) {
            counter.put(address, new AtomicLong(0));
        }
        try {
            for (int i = 0; i < runs; i++) {
                InetSocketAddress selectAddress = loadBalance.select(addresses, XID);
                counter.get(selectAddress).incrementAndGet();
            }
        } catch (Exception e) {
            //do nothing
        }
        return counter;
    }

    /**
     * Address provider object [ ] [ ].
     *
     * @return Stream<List < InetSocketAddress>>
     */
    static Stream<List<InetSocketAddress>> addressProvider() {
        return Stream.of(
                Arrays.asList(new InetSocketAddress("127.0.0.1", 8091),
                        new InetSocketAddress("127.0.0.1", 8092),
                        new InetSocketAddress("127.0.0.1", 8093),
                        new InetSocketAddress("127.0.0.1", 8094),
                        new InetSocketAddress("127.0.0.1", 8095))
        );
    }
}
