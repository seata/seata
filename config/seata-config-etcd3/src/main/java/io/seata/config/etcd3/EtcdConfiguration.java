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
package io.seata.config.etcd3;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.watch.WatchResponse;
import io.netty.util.internal.ConcurrentSet;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.AbstractConfiguration;
import io.seata.config.ConfigFuture;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.netty.util.CharsetUtil.UTF_8;
import static io.seata.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static io.seata.config.ConfigurationKeys.FILE_ROOT_CONFIG;

/**
 * The type Etcd configuration.
 *
 * @author xingfudeshi @gmail.com
 */
public class EtcdConfiguration extends AbstractConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdConfiguration.class);
    private static volatile EtcdConfiguration instance;
    private static volatile Client client;

    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String ETCD_CONFIG_KEY = "key";
    private static final String CONFIG_TYPE = "etcd3";
    private static final String DEFAULT_ETCD_CONFIG_KEY_VALUE = "seata.properties";
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR + CONFIG_TYPE
            + FILE_CONFIG_SPLIT_CHAR;
    private static final int THREAD_POOL_NUM = 1;
    private static final int MAP_INITIAL_CAPACITY = 8;
    private ExecutorService etcdConfigExecutor;
    private static final ConcurrentMap<String, Set<ConfigurationChangeListener>> CONFIG_LISTENERS_MAP = new ConcurrentHashMap<>(
            MAP_INITIAL_CAPACITY);
    private static volatile Properties seataConfig = new Properties();

    private static final long VERSION_NOT_EXIST = 0;

    private EtcdConfiguration() {
        etcdConfigExecutor = new ThreadPoolExecutor(THREAD_POOL_NUM, THREAD_POOL_NUM, Integer.MAX_VALUE,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("etcd-config-executor", THREAD_POOL_NUM));
        initSeataConfig();
    }

    /**
     * get instance
     *
     * @return instance
     */
    public static EtcdConfiguration getInstance() {
        if (instance == null) {
            synchronized (EtcdConfiguration.class) {
                if (instance == null) {
                    instance = new EtcdConfiguration();
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
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        String value = getConfigFromSysPro(dataId);
        if (value != null) {
            return value;
        }

        value = seataConfig.getProperty(dataId);
        if (value != null) {
            return value;
        }

        ConfigFuture configFuture = new ConfigFuture(dataId, defaultValue, ConfigFuture.ConfigOperation.GET,
                timeoutMills);
        etcdConfigExecutor.execute(() -> complete(getClient().getKVClient().get(ByteSequence.from(dataId, UTF_8)), configFuture));
        return (String) configFuture.get();
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        if (!seataConfig.isEmpty()) {
            seataConfig.setProperty(dataId, content);
            String etcdConfigKey = getEtcdConfigKey();
            String seataConfigStr = getSeataConfigStr();
            ConfigFuture configFuture = new ConfigFuture(etcdConfigKey, seataConfigStr, ConfigFuture.ConfigOperation.PUT, timeoutMills);
            etcdConfigExecutor.execute(() -> complete(
                    getClient().getKVClient().put(ByteSequence.from(etcdConfigKey, UTF_8), ByteSequence.from(seataConfigStr, UTF_8)),
                    configFuture));
            return (Boolean) configFuture.get();
        }

        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUT, timeoutMills);
        etcdConfigExecutor.execute(() -> complete(
                getClient().getKVClient().put(ByteSequence.from(dataId, UTF_8), ByteSequence.from(content, UTF_8)),
                configFuture));
        return (Boolean) configFuture.get();
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        if (!seataConfig.isEmpty()) {
            if (seataConfig.contains(dataId)) {
                return true;
            } else {
                return putConfig(dataId, content, timeoutMills);
            }
        }

        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUTIFABSENT,
                timeoutMills);
        etcdConfigExecutor.execute(() -> {
            //use etcd transaction to ensure the atomic operation
            complete(client.getKVClient().txn()
                    //whether the key exists
                    .If(new Cmp(ByteSequence.from(dataId, UTF_8), Cmp.Op.EQUAL, CmpTarget.version(VERSION_NOT_EXIST)))
                    //not exist,then will create
                    .Then(Op.put(ByteSequence.from(dataId, UTF_8), ByteSequence.from(content, UTF_8), PutOption.DEFAULT))
                    .commit(), configFuture);
        });
        return (Boolean) configFuture.get();
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        if (!seataConfig.isEmpty()) {
            seataConfig.remove(dataId);
            String etcdConfigKey = getEtcdConfigKey();
            String seataConfigStr = getSeataConfigStr();
            ConfigFuture configFuture = new ConfigFuture(etcdConfigKey, seataConfigStr, ConfigFuture.ConfigOperation.PUT, timeoutMills);
            etcdConfigExecutor.execute(() -> complete(
                    getClient().getKVClient().put(ByteSequence.from(etcdConfigKey, UTF_8), ByteSequence.from(seataConfigStr, UTF_8)),
                    configFuture));
            return (Boolean) configFuture.get();
        }

        ConfigFuture configFuture = new ConfigFuture(dataId, null, ConfigFuture.ConfigOperation.REMOVE, timeoutMills);
        etcdConfigExecutor.execute(() -> complete(getClient().getKVClient().delete(ByteSequence.from(dataId, UTF_8)), configFuture));
        return (Boolean) configFuture.get();
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        EtcdListener etcdListener = new EtcdListener(dataId, listener);
        CONFIG_LISTENERS_MAP.computeIfAbsent(dataId, key -> new ConcurrentSet<>())
                .add(etcdListener);
        etcdListener.onProcessEvent(new ConfigurationChangeEvent());
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        Set<ConfigurationChangeListener> configListeners = getConfigListeners(dataId);
        if (CollectionUtils.isNotEmpty(configListeners)) {
            ConfigurationChangeListener target;
            for (ConfigurationChangeListener entry : configListeners) {
                target = ((EtcdListener) entry).getTargetListener();
                if (listener.equals(target)) {
                    entry.onShutDown();
                    configListeners.remove(entry);
                    break;
                }
            }
        }
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return CONFIG_LISTENERS_MAP.get(dataId);
    }

    /**
     * get client
     *
     * @return client
     */
    private static Client getClient() {
        if (client == null) {
            synchronized (EtcdConfiguration.class) {
                if (client == null) {
                    client = Client.builder().endpoints(FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY))
                            .build();
                }
            }
        }
        return client;
    }

    /**
     * complete the future
     *
     * @param completableFuture
     * @param configFuture
     * @param <T>
     */
    private static <T> void complete(CompletableFuture<T> completableFuture, ConfigFuture configFuture) {
        try {
            T response = completableFuture.get();
            if (response instanceof GetResponse) {
                List<KeyValue> keyValues = ((GetResponse) response).getKvs();
                if (CollectionUtils.isNotEmpty(keyValues)) {
                    ByteSequence value = keyValues.get(0).getValue();
                    if (value != null) {
                        configFuture.setResult(value.toString(UTF_8));
                    }
                }
            } else if (response instanceof PutResponse) {
                configFuture.setResult(Boolean.TRUE);
            } else if (response instanceof TxnResponse) {
                boolean result = ((TxnResponse) response).isSucceeded();
                //create key if file does not exist)
                if (result) {
                    configFuture.setResult(Boolean.TRUE);
                }
            } else if (response instanceof DeleteResponse) {
                configFuture.setResult(Boolean.TRUE);
            } else {
                throw new ShouldNeverHappenException("unsupported response type");
            }
        } catch (Exception e) {
            LOGGER.error("error occurred while completing the future{}", e.getMessage(), e);
        }
    }

    private static void initSeataConfig() {
        String etcdConfigKey = getEtcdConfigKey();
        CompletableFuture<GetResponse> future = getClient().getKVClient().get(ByteSequence.from(etcdConfigKey, UTF_8));
        try {
            GetResponse getResponse = future.get();
            List<KeyValue> kvs = getResponse.getKvs();
            if (!kvs.isEmpty()) {
                try (Reader reader = new InputStreamReader(new ByteArrayInputStream(kvs.get(0).getValue().getBytes()), StandardCharsets.UTF_8)) {
                    seataConfig.load(reader);
                }

                EtcdListener etcdListener = new EtcdListener(etcdConfigKey, null);
                CONFIG_LISTENERS_MAP.computeIfAbsent(etcdConfigKey, key -> new ConcurrentSet<>())
                        .add(etcdListener);
                etcdListener.onProcessEvent(new ConfigurationChangeEvent());
            }
        } catch (Exception e) {
            LOGGER.error("init config properties error", e);
        }
    }

    private static String getEtcdConfigKey() {
        return FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + ETCD_CONFIG_KEY, DEFAULT_ETCD_CONFIG_KEY_VALUE);
    }

    private static String getSeataConfigStr() {
        StringBuilder sb = new StringBuilder();

        Enumeration<?> enumeration = seataConfig.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            String property = seataConfig.getProperty(key);
            sb.append(key).append("=").append(property).append("\n");
        }

        return sb.toString();
    }

    /**
     * the type config change notifier
     */
    private static class EtcdListener implements ConfigurationChangeListener {
        private final String dataId;
        private final ConfigurationChangeListener listener;
        private Watch.Watcher watcher;
        private final ExecutorService executor = new ThreadPoolExecutor(CORE_LISTENER_THREAD, MAX_LISTENER_THREAD, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("etcdListener", MAX_LISTENER_THREAD));

        /**
         * Instantiates a new Etcd listener.
         *
         * @param dataId   the data id
         * @param listener the listener
         */
        public EtcdListener(String dataId, ConfigurationChangeListener listener) {
            this.dataId = dataId;
            this.listener = listener;
        }

        /**
         * get the listener
         *
         * @return ConfigurationChangeListener target listener
         */
        public ConfigurationChangeListener getTargetListener() {
            return this.listener;
        }

        @Override
        public void onShutDown() {
            this.watcher.close();
            getExecutorService().shutdownNow();
        }

        @Override
        public void onChangeEvent(ConfigurationChangeEvent event) {
            Watch watchClient = getClient().getWatchClient();
            watcher = watchClient.watch(ByteSequence.from(dataId, UTF_8), new Watch.Listener() {

                @Override
                public void onNext(WatchResponse watchResponse) {
                    if (dataId.equals(getEtcdConfigKey())) {
                        byte[] bytes = watchResponse.getEvents().get(0).getKeyValue().getValue().getBytes();
                        Properties seataConfigNew = new Properties();
                        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
                            seataConfigNew.load(reader);
                        } catch (IOException e) {
                            LOGGER.error("load config properties error", e);
                        }

                        for (Map.Entry<String, Set<ConfigurationChangeListener>> entry : CONFIG_LISTENERS_MAP.entrySet()) {
                            String key = entry.getKey();
                            String valueOld = seataConfig.getProperty(key, "");
                            String valueNew = seataConfigNew.getProperty(key, "");
                            if (!valueOld.equals(valueNew)) {
                                for (ConfigurationChangeListener changeListener : entry.getValue()) {
                                    event.setDataId(key).setNewValue(valueNew);
                                    ConfigurationChangeListener listener = ((EtcdListener) changeListener).getTargetListener();
                                    listener.onProcessEvent(event);
                                }
                            }
                        }
                        seataConfig = seataConfigNew;
                        return;
                    }

                    try {
                        GetResponse getResponse = getClient().getKVClient().get(ByteSequence.from(dataId, UTF_8)).get();
                        List<KeyValue> keyValues = getResponse.getKvs();
                        if (CollectionUtils.isNotEmpty(keyValues)) {
                            event.setDataId(dataId).setNewValue(keyValues.get(0).getValue().toString(UTF_8));
                            listener.onChangeEvent(event);
                        }
                    } catch (Exception e) {
                        LOGGER.error("error occurred while getting value{}", e.getMessage(), e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {

                }
            });
        }

        @Override
        public ExecutorService getExecutorService() {
            return executor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EtcdListener that = (EtcdListener) o;
            return Objects.equals(dataId, that.dataId) &&
                    Objects.equals(listener, that.listener) &&
                    Objects.equals(watcher, that.watcher) &&
                    Objects.equals(executor, that.executor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dataId, listener, watcher, executor);
        }
    }
}
