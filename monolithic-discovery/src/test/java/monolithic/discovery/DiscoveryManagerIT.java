package monolithic.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import monolithic.common.config.ConfigKeys;
import monolithic.discovery.model.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Perform testing on the {@link DiscoveryManager} class.
 */
public class DiscoveryManagerIT {
    private static Config config;
    private static TestingServer testingServer;
    private static CuratorFramework curator;

    @BeforeClass
    public static void setup() throws Exception {
        testingServer = new TestingServer();

        final Map<String, ConfigValue> map = new HashMap<>();
        map.put(ConfigKeys.ZOOKEEPER_HOSTS.getKey(), ConfigValueFactory.fromAnyRef(testingServer.getConnectString()));
        config = ConfigFactory.parseMap(map).withFallback(ConfigFactory.load());

        curator =
                CuratorFrameworkFactory.builder().namespace("namespace").connectString(testingServer.getConnectString())
                        .defaultData(new byte[0]).retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
        curator.start();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (curator != null) {
            curator.close();
        }
        if (testingServer != null) {
            testingServer.close();
        }
    }

    @Test
    public void test() throws Exception {
        final DiscoveryManager discovery = new DiscoveryManager(config, curator);
        try {
            assertEquals(config, discovery.getConfig());
            assertNotNull(discovery.getDiscovery());

            // Nothing registered at first.
            assertEquals(0, discovery.getAll().size());
            assertFalse(discovery.getRandom().isPresent());

            final Service service =
                    new Service(config.getString(ConfigKeys.SYSTEM_NAME.getKey()), "1.2.3", "host", 1234, false);
            discovery.register(service);

            // Should now have one registered service.
            assertEquals(1, discovery.getAll().size());
            assertTrue(discovery.getRandom().isPresent());

            discovery.unregister(service);

            // Nothing registered at again.
            assertEquals(0, discovery.getAll().size());
            assertFalse(discovery.getRandom().isPresent());
        } finally {
            discovery.close();
            assertTrue(discovery.isClosed());
        }

        // Now that discovery is closed, nothing happens.
        assertEquals(0, discovery.getAll().size());
        assertFalse(discovery.getRandom().isPresent());

        final Service service =
                new Service(config.getString(ConfigKeys.SYSTEM_NAME.getKey()), "1.2.3", "host", 1234, false);
        discovery.register(service);

        // Still nothing registered.
        assertEquals(0, discovery.getAll().size());
        assertFalse(discovery.getRandom().isPresent());

        discovery.unregister(service);

        // Nothing registered still.
        assertEquals(0, discovery.getAll().size());
        assertFalse(discovery.getRandom().isPresent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCloseWithException() throws Exception {
        final ServiceDiscovery<String> serviceDiscovery = Mockito.mock(ServiceDiscovery.class);
        Mockito.doThrow(new IOException("Fake")).when(serviceDiscovery).close();
        final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
        Mockito.when(discovery.getDiscovery()).thenReturn(serviceDiscovery);
        Mockito.doCallRealMethod().when(discovery).close();
        discovery.close();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = DiscoveryException.class)
    public void testRegisterWithException() throws Exception {
        final ServiceDiscovery<String> serviceDiscovery = Mockito.mock(ServiceDiscovery.class);
        Mockito.doThrow(new Exception("Fake")).when(serviceDiscovery).registerService(Mockito.any());
        final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
        Mockito.when(discovery.getDiscovery()).thenReturn(serviceDiscovery);
        Mockito.doCallRealMethod().when(discovery).register(Mockito.any());
        discovery.register(new Service("system", "1.2.3", "host", 1234, false));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = DiscoveryException.class)
    public void testUnregisterWithException() throws Exception {
        final ServiceDiscovery<String> serviceDiscovery = Mockito.mock(ServiceDiscovery.class);
        Mockito.doThrow(new Exception("Fake")).when(serviceDiscovery).unregisterService(Mockito.any());
        final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
        Mockito.when(discovery.getDiscovery()).thenReturn(serviceDiscovery);
        Mockito.doCallRealMethod().when(discovery).unregister(Mockito.any());
        discovery.unregister(new Service("system", "1.2.3", "host", 1234, false));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = DiscoveryException.class)
    public void testGetAllWithException() throws Exception {
        final ServiceDiscovery<String> serviceDiscovery = Mockito.mock(ServiceDiscovery.class);
        Mockito.doThrow(new Exception("Fake")).when(serviceDiscovery).queryForInstances(Mockito.anyString());
        final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
        Mockito.when(discovery.getDiscovery()).thenReturn(serviceDiscovery);
        Mockito.doCallRealMethod().when(discovery).getAll();
        discovery.getAll();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = DiscoveryException.class)
    public void testGetRandomWithException() throws Exception {
        final ServiceDiscovery<String> serviceDiscovery = Mockito.mock(ServiceDiscovery.class);
        Mockito.doThrow(new Exception("Fake")).when(serviceDiscovery).queryForInstances(Mockito.anyString());
        final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
        Mockito.when(discovery.getDiscovery()).thenReturn(serviceDiscovery);
        Mockito.doCallRealMethod().when(discovery).getRandom();
        discovery.getRandom();
    }
}
