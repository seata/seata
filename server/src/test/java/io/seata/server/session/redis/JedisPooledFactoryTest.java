package io.seata.server.session.redis;

import io.seata.server.storage.redis.JedisPooledFactory;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.HostAndPort;

/**
 * Jedis pool factory test
 * @author wangzhongxiang
 */
public class JedisPooledFactoryTest {

    @Test
    public void testGetHostAndPorts() {
        String nodes = "127.0.0.1:6379;127.0.0.1:6380;127.0.0.1:6381;127.0.0.1:6382;127.0.0.1:6383;127.0.0.1:6384";
        JedisPooledFactory factory = new JedisPooledFactory();
        List<HostAndPort> hostAndPorts = factory.getHostAndPorts(nodes);
        Assertions.assertEquals(6,hostAndPorts.size());
        Assertions.assertEquals("127.0.0.1",hostAndPorts.get(0).getHost());
        Assertions.assertEquals(6379,hostAndPorts.get(0).getPort());
        Assertions.assertEquals(6380,hostAndPorts.get(1).getPort());
        Assertions.assertEquals(6381,hostAndPorts.get(2).getPort());
        Assertions.assertEquals(6382,hostAndPorts.get(3).getPort());
        Assertions.assertEquals(6383,hostAndPorts.get(4).getPort());
        Assertions.assertEquals(6384,hostAndPorts.get(5).getPort());
        nodes = null;
        List<HostAndPort> hostAndPorts1 = factory.getHostAndPorts(nodes);
        Assertions.assertEquals(null,hostAndPorts1);
    }

}
