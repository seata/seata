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
package io.seata.discovery.registry.consul;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.NetUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.RegistryService;

/**
 * @author xingfudeshi@gmail.com
 */
public class ConsulRegistryServiceImpl implements RegistryService<ConsulListener> {

    private static volatile ConsulRegistryServiceImpl instance;
    private static volatile ConsulClient client;

    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_TYPE = "consul";
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String REGISTRY_CLUSTER = "cluster";
    private static final String DEFAULT_CLUSTER_NAME = "default";
    private static final String SERVICE_TAG = "services";
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR;

    private ConcurrentMap<String, List<InetSocketAddress>> clusterAddressMap;
    private ConcurrentMap<String, Set<ConsulListener>> listenerMap;
    private ExecutorService notifierExecutor;
    private ConcurrentMap<String, ConsulNotifier> notifiers;

    private static final int THREAD_POOL_NUM = 1;
    private static final int MAP_INITIAL_CAPACITY = 8;

    /**
     * default tcp check interval
     */
    private static final String DEFAULT_CHECK_INTERVAL = "10s";
    /**
     * default tcp check timeout
     */
    private static final String DEFAULT_CHECK_TIMEOUT = "1s";
    /**
     * default deregister critical server after
     */
    private static final String DEFAULT_DEREGISTER_TIME = "20s";
    /**
     * default watch timeout in second
     */
    private static final int DEFAULT_WATCH_TIMEOUT = 60;


    private ConsulRegistryServiceImpl() {
        //initial the capacity with 8
        clusterAddressMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
        listenerMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
        notifiers = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
        notifierExecutor = new ThreadPoolExecutor(THREAD_POOL_NUM, THREAD_POOL_NUM,
            Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new NamedThreadFactory("services-consul-notifier", THREAD_POOL_NUM));
    }

    /**
     * get instance of ConsulRegistryServiceImpl
     *
     * @return instance
     */
    static ConsulRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (ConsulRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new ConsulRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        getConsulClient().agentServiceRegister(createService(address));
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        getConsulClient().agentServiceDeregister(createServiceId(address));
    }

    @Override
    public void subscribe(String cluster, ConsulListener listener) throws Exception {
        //1.add listener to subscribe list
        listenerMap.computeIfAbsent(cluster, key -> new HashSet<>())
                .add(listener);
        //2.get healthy services
        Response<List<HealthService>> response = getHealthyServices(cluster, -1, DEFAULT_WATCH_TIMEOUT);
        //3.get current consul index.
        Long index = response.getConsulIndex();
        ConsulNotifier notifier = notifiers.computeIfAbsent(cluster, key -> new ConsulNotifier(cluster, index));
        //4.run notifier
        notifierExecutor.submit(notifier);
    }

    @Override
    public void unsubscribe(String cluster, ConsulListener listener) throws Exception {
        //1.remove notifier for the cluster
        ConsulNotifier notifier = notifiers.remove(cluster);
        //2.stop the notifier
        notifier.stop();
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        final String cluster = getServiceGroup(key);
        if (cluster == null) {
            return null;
        }
        if (!listenerMap.containsKey(cluster)) {
            //1.refresh cluster
            refreshCluster(cluster);
            //2. subscribe
            subscribe(cluster, services -> refreshCluster(cluster, services));
        }
        return clusterAddressMap.get(cluster);
    }

    /**
     * get consul client
     *
     * @return client
     */
    private ConsulClient getConsulClient() {
        if (client == null) {
            synchronized (ConsulRegistryServiceImpl.class) {
                if (client == null) {
                    String serverAddr = FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY);
                    InetSocketAddress inetSocketAddress = NetUtil.toInetSocketAddress(serverAddr);
                    client = new ConsulClient(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
                }
            }
        }
        return client;
    }

    /**
     * get cluster name
     *
     * @return
     */
    private String getClusterName() {
        String clusterConfigName = String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, REGISTRY_CLUSTER);
        return FILE_CONFIG.getConfig(clusterConfigName, DEFAULT_CLUSTER_NAME);
    }

    /**
     * create serviceId
     *
     * @param address
     * @return serviceId
     */
    private String createServiceId(InetSocketAddress address) {
        return getClusterName() + "-" + NetUtil.toStringAddress(address);
    }

    /**
     * create a new service
     *
     * @param address
     * @return newService
     */
    private NewService createService(InetSocketAddress address) {
        NewService newService = new NewService();
        newService.setId(createServiceId(address));
        newService.setName(getClusterName());
        newService.setTags(Collections.singletonList(SERVICE_TAG));
        newService.setPort(address.getPort());
        newService.setAddress(NetUtil.toIpAddress(address));
        newService.setCheck(createCheck(address));
        return newService;
    }

    /**
     * create service check based on TCP
     *
     * @param address
     * @return
     */
    private NewService.Check createCheck(InetSocketAddress address) {
        NewService.Check check = new NewService.Check();
        check.setTcp(NetUtil.toStringAddress(address));
        check.setInterval(DEFAULT_CHECK_INTERVAL);
        check.setTimeout(DEFAULT_CHECK_TIMEOUT);
        check.setDeregisterCriticalServiceAfter(DEFAULT_DEREGISTER_TIME);
        return check;
    }

    /**
     * get healthy services
     *
     * @param service
     * @return
     */
    private Response<List<HealthService>> getHealthyServices(String service, long index, long watchTimeout) {
        return getConsulClient().getHealthServices(service, HealthServicesRequest.newBuilder()
            .setTag(SERVICE_TAG)
            .setQueryParams(new QueryParams(watchTimeout, index))
            .setPassing(true)
            .build());
    }

    /**
     * refresh cluster
     *
     * @param cluster
     */
    private void refreshCluster(String cluster) {
        if (cluster == null) {
            return;
        }
        Response<List<HealthService>> response = getHealthyServices(getClusterName(), -1, -1);
        if (response == null) {
            return;
        }
        refreshCluster(cluster, response.getValue());
    }

    /**
     * refresh cluster
     *
     * @param cluster
     * @param services
     */
    private void refreshCluster(String cluster, List<HealthService> services) {
        if (cluster == null || services == null) {
            return;
        }
        clusterAddressMap.put(cluster, services.stream()
            .map(HealthService::getService)
            .map(service -> new InetSocketAddress(service.getAddress(), service.getPort()))
            .collect(Collectors.toList()));
    }

    /**
     * consul notifier
     */
    private class ConsulNotifier implements Runnable {
        private String cluster;
        private long consulIndex;
        private boolean running;

        ConsulNotifier(String cluster, long consulIndex) {
            this.cluster = cluster;
            this.consulIndex = consulIndex;
            this.running = true;
        }

        @Override
        public void run() {
            while (this.running) {
                processService();
            }
        }

        private void processService() {
            Response<List<HealthService>> response = getHealthyServices(cluster, consulIndex, DEFAULT_WATCH_TIMEOUT);
            Long currentIndex = response.getConsulIndex();
            if (currentIndex != null && currentIndex > consulIndex) {
                List<HealthService> services = response.getValue();
                consulIndex = currentIndex;
                for (ConsulListener listener : listenerMap.get(cluster)) {
                    listener.onEvent(services);
                }
            }
        }

        void stop() {
            this.running = false;
        }
    }

    @Override
    public void close() throws Exception {
        client = null;
    }

}
