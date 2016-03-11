package monolithic.config.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import monolithic.config.model.ConfigKeyValue;
import monolithic.config.model.ConfigKeyValueCollection;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Perform testing on the {@link CuratorConfigService} class.
 */
public class CuratorConfigServiceTest {
    private ExecutorService executor;

    @Before
    public void before() throws Exception {
        // Don't want to see too much logging.
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CuratorConfigService.class)).setLevel(Level.OFF);

        this.executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void after() throws Exception {
        this.executor.shutdown();
    }

    @Test
    public void test() throws Exception {
        final TestingServer testingServer = new TestingServer();
        final CuratorFramework curator = CuratorFrameworkFactory.builder().namespace("namespace-test")
                .connectString(testingServer.getConnectString()).defaultData(new byte[0])
                .retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
        curator.start();
        curator.blockUntilConnected(10, TimeUnit.SECONDS);

        final CuratorConfigService svc = new CuratorConfigService(this.executor, curator);

        final ConfigKeyValueCollection coll = svc.getAll().get();
        assertEquals(0, coll.size());

        assertFalse(svc.set(new ConfigKeyValue("key", "value")).get().isPresent());

        // Wait a little to allow the value to be stored.
        TimeUnit.MILLISECONDS.sleep(300);

        final ConfigKeyValueCollection afterSet = svc.getAll().get();
        assertEquals(1, afterSet.size());

        // Overwrite the previous value.
        final Optional<ConfigKeyValue> oldValue = svc.set(new ConfigKeyValue("key", "new-value")).get();
        assertTrue(oldValue.isPresent());
        assertEquals(new ConfigKeyValue("key", "value"), oldValue.get());

        // Wait a little to allow the value to be stored.
        TimeUnit.MILLISECONDS.sleep(300);

        final Optional<ConfigKeyValue> get = svc.get("key").get();
        assertTrue(get.isPresent());
        assertEquals(new ConfigKeyValue("key", "new-value"), get.get());

        final Optional<ConfigKeyValue> unset = svc.unset("key").get();
        assertTrue(unset.isPresent());
        assertEquals(new ConfigKeyValue("key", "new-value"), unset.get());

        final Optional<ConfigKeyValue> getMissing = svc.get("missing").get();
        assertFalse(getMissing.isPresent());

        final Optional<ConfigKeyValue> unsetMissing = svc.unset("missing").get();
        assertFalse(unsetMissing.isPresent());
    }

    @Test
    public void testExistingData() throws Exception {
        final TestingServer testingServer = new TestingServer();
        final CuratorFramework curator = CuratorFrameworkFactory.builder().namespace("namespace-test-existing")
                .connectString(testingServer.getConnectString()).defaultData(new byte[0])
                .retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
        curator.start();
        curator.blockUntilConnected(10, TimeUnit.SECONDS);

        curator.create().forPath("/dynamic-config");
        curator.create().forPath("/dynamic-config/key", "value".getBytes(StandardCharsets.UTF_8));

        final CuratorConfigService svc = new CuratorConfigService(this.executor, curator);

        // Wait a little to allow the value to be stored.
        TimeUnit.MILLISECONDS.sleep(300);

        final Optional<ConfigKeyValue> get = svc.get("key").get();
        assertTrue(get.isPresent());
        assertEquals(new ConfigKeyValue("key", "value"), get.get());
    }
}
