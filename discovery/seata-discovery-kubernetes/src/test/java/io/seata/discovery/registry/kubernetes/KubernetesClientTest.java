package io.seata.discovery.registry.kubernetes;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zhanghao
 * @date 2019/5/19 17:05
 **/
public class KubernetesClientTest {

    @ClassRule
    public static KubernetesServer mockServer = new KubernetesServer(true,true);

    private static KubernetesClient mockClient;


    private static final String SERVICE_NAME = "default";



    @Before
    public void setup() {
        mockClient = new DefaultKubernetesClient();
//        mockClient = mockServer.getClient();
        // Configure the kubernetes master url to point to the mock server
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY,
                mockClient.getConfiguration().getMasterUrl());
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
        System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY,
                "false");
    }

    @Test
    public void testClient() throws Exception {
        KubernetesRegistryServiceImpl kubernetesRegistryServic =  KubernetesRegistryServiceImpl.getInstance(mockClient);

        final List<InetSocketAddress> inetSocketAddressList = kubernetesRegistryServic.lookup("my_test_tx_group");

        assertThat(inetSocketAddressList).isNotEmpty();
    }
}
