package monolithic.server.port;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.Test;
import org.mockito.Mockito;

import monolithic.common.config.ConfigKeys;
import monolithic.common.model.service.Reservation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Perform testing on the {@link PortManager} class.
 */
public class PortManagerIT {
    @Test
    public void test() throws Exception {
        final TestingServer testingServer = new TestingServer();

        final Map<String, ConfigValue> map = new HashMap<>();
        map.put(ConfigKeys.ZOOKEEPER_HOSTS.getKey(), ConfigValueFactory.fromAnyRef(testingServer.getConnectString()));
        map.put(ConfigKeys.SERVER_PORT_MIN.getKey(), ConfigValueFactory.fromAnyRef(5000));
        map.put(ConfigKeys.SERVER_PORT_MAX.getKey(), ConfigValueFactory.fromAnyRef(5002));
        final Config config = ConfigFactory.parseMap(map).withFallback(ConfigFactory.load());

        final CuratorFramework curator =
                CuratorFrameworkFactory.builder().namespace("port-test").connectString(testingServer.getConnectString())
                        .defaultData(new byte[0]).retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
        curator.start();

        final PortManager primary = new PortManager(config, curator);
        final PortManager secondary = new PortManager(config, curator);
        try {
            assertEquals(config, primary.getConfig());
            assertEquals(config, secondary.getConfig());

            final Reservation r1 = primary.getReservation("localhost");
            assertNotNull(r1);
            assertEquals("localhost", r1.getHost());
            assertTrue(5000 <= r1.getPort() && r1.getPort() <= 5002);

            final Reservation r2 = secondary.getReservation("localhost");
            assertNotNull(r2);
            assertEquals("localhost", r2.getHost());
            assertTrue(5000 <= r2.getPort() && r2.getPort() <= 5002);

            final Reservation r3 = primary.getReservation("localhost");
            assertNotNull(r3);
            assertEquals("localhost", r3.getHost());
            assertTrue(5000 <= r3.getPort() && r3.getPort() <= 5002);

            final Reservation r4 = secondary.getReservation("localhost");
            assertNotNull(r4);
            assertEquals("localhost", r4.getHost());
            assertTrue(5000 <= r4.getPort() && r4.getPort() <= 5002);
        } finally {
            primary.close();
            secondary.close();
            curator.close();
            testingServer.close();
        }
    }

    @Test(expected = Exception.class)
    public void testNoneAvailable() throws Exception {
        final TestingServer testingServer = new TestingServer();

        final Map<String, ConfigValue> map = new HashMap<>();
        map.put(ConfigKeys.ZOOKEEPER_HOSTS.getKey(), ConfigValueFactory.fromAnyRef(testingServer.getConnectString()));
        map.put(ConfigKeys.SERVER_PORT_MIN.getKey(), ConfigValueFactory.fromAnyRef(5000));
        map.put(ConfigKeys.SERVER_PORT_MAX.getKey(), ConfigValueFactory.fromAnyRef(5002));
        final Config config = ConfigFactory.parseMap(map).withFallback(ConfigFactory.load());

        final CuratorFramework curator =
                CuratorFrameworkFactory.builder().namespace("namespace").connectString(testingServer.getConnectString())
                        .defaultData(new byte[0]).retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
        curator.start();

        final PortManager portManager = new PortManager(config, curator, (host, port) -> false);
        try {
            portManager.getReservation("localhost");
        } finally {
            portManager.close();
            curator.close();
            testingServer.close();
        }
    }

    @Test
    public void testCloseException() throws Exception {
        final PortManager portManager = Mockito.mock(PortManager.class);
        final SharedCount mockedSharedCount = Mockito.mock(SharedCount.class);
        Mockito.doThrow(new IOException("Fake")).when(mockedSharedCount).close();
        Mockito.doCallRealMethod().when(portManager).close();
        Mockito.when(portManager.getPortReservation()).thenReturn(mockedSharedCount);

        portManager.close();
    }

    @Test(expected = PortReservationException.class)
    public void testGetReservationException() throws Exception {
        final SharedCount mockedSharedCount = Mockito.mock(SharedCount.class);
        Mockito.when(mockedSharedCount.trySetCount(Mockito.any(), Mockito.anyInt())).thenThrow(new Exception("Fake"));
        final PortManager portManager = Mockito.mock(PortManager.class);
        Mockito.doCallRealMethod().when(portManager).getReservation(Mockito.any());
        Mockito.when(portManager.getPortReservation()).thenReturn(mockedSharedCount);
        portManager.getReservation("localhost");
    }
}
