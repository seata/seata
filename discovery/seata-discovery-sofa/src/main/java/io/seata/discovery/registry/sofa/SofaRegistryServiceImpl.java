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
package io.seata.discovery.registry.sofa;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.alipay.sofa.registry.client.api.RegistryClient;
import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.api.SubscriberDataObserver;
import com.alipay.sofa.registry.client.api.model.RegistryType;
import com.alipay.sofa.registry.client.api.registration.PublisherRegistration;
import com.alipay.sofa.registry.client.api.registration.SubscriberRegistration;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClient;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClientConfigBuilder;
import com.alipay.sofa.registry.core.model.ScopeEnum;
import io.seata.common.util.NetUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.RegistryService;
import org.apache.commons.lang.StringUtils;

import static io.seata.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static io.seata.config.ConfigurationKeys.FILE_ROOT_REGISTRY;

/**
 * The type SOFARegistry registry service.
 *
 * @author leizhiyuan
 */
public class SofaRegistryServiceImpl implements RegistryService<SubscriberDataObserver> {

    private static final String SOFA_FILEKEY_PREFIX = "registry.sofa.";

    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String PRO_REGION_KEY = "region";
    private static final String PRO_DATACENTER_KEY = "datacenter";
    private static final String PRO_GROUP_KEY = "group";
    private static final String PRO_APPLICATION_KEY = "application";
    private static final String PRO_CLUSTER_KEY = "cluster";
    private static final String PRO_ADDRESS_WAIT_TIME_KEY = "addressWaitTime";

    private static final String DEFAULT_LOCAL_DATACENTER = "DefaultDataCenter";
    private static final String DEFAULT_LOCAL_REGION = "DEFAULT_ZONE";
    private static final String DEFAULT_GROUP = "SEATA_GROUP";
    private static final String DEFAULT_APPLICATION = "seata-server";
    private static final String DEFAULT_CLUSTER = "default";
    private static final String DEFAULT_ADDRESS_WAIT_TIME = "3000";

    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    private static final String HOST_SEPERATOR = ":";
    private static final String REGISTRY_TYPE = "sofa";

    private static final ConcurrentMap<String, List<SubscriberDataObserver>> LISTENER_SERVICE_MAP
        = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<InetSocketAddress>> CLUSTER_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static Properties registryProps;
    private static volatile RegistryClient registryClient;

    private static volatile SofaRegistryServiceImpl instance;

    private SofaRegistryServiceImpl() {
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static SofaRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (SofaRegistryServiceImpl.class) {
                if (instance == null) {
                    registryProps = getNamingProperties();
                    instance = new SofaRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        String clusterName = registryProps.getProperty(PRO_CLUSTER_KEY);
        PublisherRegistration publisherRegistration = new PublisherRegistration(clusterName);
        publisherRegistration.setGroup(registryProps.getProperty(PRO_GROUP_KEY));
        String serviceData = address.getAddress().getHostAddress() + HOST_SEPERATOR + address.getPort();
        getRegistryInstance().register(publisherRegistration, serviceData);
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        String clusterName = registryProps.getProperty(PRO_CLUSTER_KEY);
        getRegistryInstance().unregister(clusterName, registryProps.getProperty(PRO_GROUP_KEY), RegistryType.PUBLISHER);
    }

    private RegistryClient getRegistryInstance() {
        if (registryClient == null) {
            synchronized (SofaRegistryServiceImpl.class) {
                if (registryClient == null) {
                    String address = registryProps.getProperty(PRO_SERVER_ADDR_KEY);
                    final String portStr = StringUtils.substringAfter(address, HOST_SEPERATOR);

                    RegistryClientConfig config = DefaultRegistryClientConfigBuilder.start()
                        .setAppName(getApplicationName())
                        .setDataCenter(registryProps.getProperty(PRO_DATACENTER_KEY))
                        .setZone(registryProps.getProperty(PRO_REGION_KEY))
                        .setRegistryEndpoint(StringUtils.substringBefore(address, HOST_SEPERATOR))
                        .setRegistryEndpointPort(Integer.parseInt(portStr)).build();

                    DefaultRegistryClient result = new DefaultRegistryClient(config);
                    result.init();
                    registryClient = result;
                }
            }
        }
        return registryClient;
    }

    @Override
    public void subscribe(String cluster, SubscriberDataObserver listener) throws Exception {
        SubscriberRegistration subscriberRegistration = new SubscriberRegistration(cluster, listener);
        subscriberRegistration.setScopeEnum(ScopeEnum.global);
        subscriberRegistration.setGroup(registryProps.getProperty(PRO_GROUP_KEY));

        LISTENER_SERVICE_MAP.computeIfAbsent(cluster, key -> new ArrayList<>())
                .add(listener);
        getRegistryInstance().register(subscriberRegistration);
    }

    @Override
    public void unsubscribe(String cluster, SubscriberDataObserver listener) throws Exception {
        getRegistryInstance().unregister(cluster, registryProps.getProperty(PRO_GROUP_KEY), RegistryType.SUBSCRIBER);
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getServiceGroup(key);
        if (clusterName == null) {
            return null;
        }
        if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
            CountDownLatch respondRegistries = new CountDownLatch(1);
            subscribe(clusterName, (dataId, data) -> {
                Map<String, List<String>> instances = data.getZoneData();
                if (instances == null && CLUSTER_ADDRESS_MAP.get(clusterName) != null) {
                    CLUSTER_ADDRESS_MAP.remove(clusterName);
                } else {
                    List<InetSocketAddress> newAddressList = flatData(instances);
                    CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
                }
                respondRegistries.countDown();
            });

            //wait max for first lookup
            final String property = registryProps.getProperty(PRO_ADDRESS_WAIT_TIME_KEY);
            respondRegistries.await(Integer.parseInt(property), TimeUnit.MILLISECONDS);

        }
        return CLUSTER_ADDRESS_MAP.get(clusterName);
    }

    private List<InetSocketAddress> flatData(Map<String, List<String>> instances) {
        List<InetSocketAddress> result = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : instances.entrySet()) {
            for (String str : entry.getValue()) {
                String ip = StringUtils.substringBefore(str, HOST_SEPERATOR);
                String port = StringUtils.substringAfter(str, HOST_SEPERATOR);
                InetSocketAddress inetSocketAddress = new InetSocketAddress(ip, Integer.parseInt(port));
                result.add(inetSocketAddress);
            }
        }
        return result;
    }

    @Override
    public void close() throws Exception {
    }

    private static Properties getNamingProperties() {
        Properties properties = new Properties();
        if (System.getProperty(SOFA_FILEKEY_PREFIX + PRO_SERVER_ADDR_KEY) != null) {
            properties.setProperty(PRO_SERVER_ADDR_KEY, System.getProperty(SOFA_FILEKEY_PREFIX + PRO_SERVER_ADDR_KEY));
        } else {
            String address = FILE_CONFIG.getConfig(getSofaAddrFileKey());
            if (address != null) {
                properties.setProperty(PRO_SERVER_ADDR_KEY, address);
            }
        }
        if (System.getProperty(SOFA_FILEKEY_PREFIX + PRO_REGION_KEY) != null) {
            properties.setProperty(PRO_REGION_KEY, System.getProperty(SOFA_FILEKEY_PREFIX + PRO_REGION_KEY));
        } else {
            String region = FILE_CONFIG.getConfig(getSofaRegionFileKey());
            if (region == null) {
                region = DEFAULT_LOCAL_REGION;
            }
            properties.setProperty(PRO_REGION_KEY, region);
        }

        if (System.getProperty(SOFA_FILEKEY_PREFIX + PRO_DATACENTER_KEY) != null) {
            properties.setProperty(PRO_DATACENTER_KEY, System.getProperty(SOFA_FILEKEY_PREFIX + PRO_DATACENTER_KEY));
        } else {
            String datacenter = FILE_CONFIG.getConfig(getSofaDataCenterFileKey());
            if (datacenter == null) {
                datacenter = DEFAULT_LOCAL_DATACENTER;
            }
            properties.setProperty(PRO_DATACENTER_KEY, datacenter);
        }

        if (System.getProperty(SOFA_FILEKEY_PREFIX + PRO_GROUP_KEY) != null) {
            properties.setProperty(PRO_GROUP_KEY, System.getProperty(SOFA_FILEKEY_PREFIX + PRO_GROUP_KEY));
        } else {
            String group = FILE_CONFIG.getConfig(getSofaGroupFileKey());
            if (group == null) {
                group = DEFAULT_GROUP;
            }
            properties.setProperty(PRO_GROUP_KEY, group);
        }

        if (System.getProperty(SOFA_FILEKEY_PREFIX + PRO_CLUSTER_KEY) != null) {
            properties.setProperty(PRO_CLUSTER_KEY, System.getProperty(SOFA_FILEKEY_PREFIX + PRO_CLUSTER_KEY));
        } else {
            String cluster = FILE_CONFIG.getConfig(getSofaClusterFileKey());
            if (cluster == null) {
                cluster = DEFAULT_CLUSTER;
            }
            properties.setProperty(PRO_CLUSTER_KEY, cluster);
        }

        if (System.getProperty(SOFA_FILEKEY_PREFIX + PRO_ADDRESS_WAIT_TIME_KEY) != null) {
            properties.setProperty(PRO_ADDRESS_WAIT_TIME_KEY, System.getProperty(SOFA_FILEKEY_PREFIX + PRO_ADDRESS_WAIT_TIME_KEY));
        } else {
            String group = FILE_CONFIG.getConfig(getSofaAddressWaitTimeFileKey());
            if (group == null) {
                group = DEFAULT_ADDRESS_WAIT_TIME;
            }
            properties.setProperty(PRO_ADDRESS_WAIT_TIME_KEY, group);
        }

        return properties;
    }

    private static String getSofaClusterFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_CLUSTER_KEY);
    }

    private static String getSofaAddressWaitTimeFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_ADDRESS_WAIT_TIME_KEY);
    }

    private static String getSofaAddrFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_SERVER_ADDR_KEY);
    }

    private static String getSofaRegionFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_REGION_KEY);
    }

    private static String getSofaDataCenterFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_DATACENTER_KEY);
    }

    private static String getSofaGroupFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_GROUP_KEY);
    }

    private String getApplicationFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_APPLICATION_KEY);
    }

    private String getApplicationName() {
        String application = FILE_CONFIG.getConfig(getApplicationFileKey());
        if (application == null) {
            application = DEFAULT_APPLICATION;
        }
        return application;
    }
}
