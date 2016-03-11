package monolithic.shell.completer;

import static org.junit.Assert.assertEquals;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.apache.curator.framework.CuratorFramework;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import monolithic.crypto.CryptoFactory;
import monolithic.discovery.DiscoveryManager;
import monolithic.discovery.model.Service;
import monolithic.shell.RegistrationManager;
import monolithic.shell.model.ShellEnvironment;
import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Perform testing on the {@link ShellCompleter} class.
 */
public class ShellCompleterTest {
    private static ShellCompleter shellCompleter;

    @BeforeClass
    public static void setup() throws Exception {
        // Don't want to see stack traces in the output when attempting to create invalid commands during testing.
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RegistrationManager.class)).setLevel(Level.OFF);

        final SortedSet<Service> services = new TreeSet<>();
        services.add(new Service("system", "1.2.3", "host1", 1234, false));
        services.add(new Service("system", "1.2.3", "host1", 1235, false));
        services.add(new Service("system", "1.2.4", "host2", 1236, true));
        services.add(new Service("system", "1.2.4", "host2", 1237, true));
        final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
        Mockito.when(discovery.getAll()).thenReturn(services);

        final Config config = ConfigFactory.load();
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final CuratorFramework curator = Mockito.mock(CuratorFramework.class);
        final RegistrationManager regMgr = new RegistrationManager();
        final OkHttpClient httpClient = new OkHttpClient.Builder().build();
        final CryptoFactory cryptoFactory = new CryptoFactory(config);
        final ShellEnvironment shellEnvironment =
                new ShellEnvironment(config, executor, discovery, curator, regMgr, httpClient, cryptoFactory);
        regMgr.loadCommands(shellEnvironment);

        shellCompleter = new ShellCompleter(regMgr);
    }

    @Test
    public void testCompleteBufferEmpty() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("", 0, candidates);

        assertEquals(6, candidates.size());
        assertEquals(0, position);
        assertEquals("[config, crypto, exit, help, quit, service]", candidates.toString());
    }

    @Test
    public void testCompleteBufferWhiteSpace() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete(" ", 1, candidates);

        assertEquals(6, candidates.size());
        assertEquals(1, position);
        assertEquals("[config, crypto, exit, help, quit, service]", candidates.toString());
    }

    @Test
    public void testCompleteCommandNoMatch() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("xyz", 3, candidates);

        assertEquals(0, candidates.size());
        assertEquals(3, position);
    }

    @Test
    public void testCompleteDoubleCommandNoMatch() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("xyz abc", 7, candidates);

        assertEquals(0, candidates.size());
        assertEquals(7, position);
    }

    @Test
    public void testCompleteCommandSingleAvailable() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("hel", 3, candidates);

        assertEquals(1, candidates.size());
        assertEquals(0, position);
        assertEquals("help", candidates.get(0).toString());
    }

    /* TODO: Need to fix completions from the middle of a word
    @Test
    public void testCompleteCommandSingleAvailableMiddle() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("hel", 2, candidates);

        assertEquals(1, candidates.size());
        assertEquals(0, position);
        assertEquals("help", candidates.get(0).toString());
    }
    */

    @Test
    public void testCompleteCommandMultipleAvailable() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("serv", 4, candidates);

        assertEquals(1, candidates.size());
        assertEquals(0, position);
        assertEquals("[service]", candidates.toString());
    }

    @Test
    public void testCompleteCommandCompleteMultipleAvailable() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("service", 7, candidates);

        assertEquals(1, candidates.size());
        assertEquals(0, position);
        assertEquals("[service]", candidates.toString());
    }

    @Test
    public void testCompleteCommandCompleteMultipleAvailableWithSpace() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("service ", 8, candidates);

        assertEquals(3, candidates.size());
        assertEquals(8, position);
        assertEquals("[control, list, memory]", candidates.toString());
    }

    @Test
    public void testCompleteCommandSingleAvailableWithOptionsNoWhiteSpace() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("service list", 12, candidates);

        assertEquals(1, candidates.size());
        assertEquals(8, position);
        assertEquals("[list]", candidates.toString());
    }

    @Test
    public void testCompleteCommandSingleAvailableWithOptionsAndWhiteSpace() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("service list ", 13, candidates);

        assertEquals(6, candidates.size());
        assertEquals(13, position);

        int index = 0;
        assertEquals("--host", candidates.get(index++).toString());
        assertEquals("--port", candidates.get(index++).toString());
        assertEquals("--version", candidates.get(index++).toString());
        assertEquals("-h", candidates.get(index++).toString());
        assertEquals("-p", candidates.get(index++).toString());
        assertEquals("-v", candidates.get(index).toString());
    }

    @Test
    public void testCompleteCommandSingleAvailableWithDash() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("service list -", 14, candidates);

        assertEquals(6, candidates.size());
        assertEquals(13, position);

        int index = 0;
        assertEquals("--host", candidates.get(index++).toString());
        assertEquals("--port", candidates.get(index++).toString());
        assertEquals("--version", candidates.get(index++).toString());
        assertEquals("-h", candidates.get(index++).toString());
        assertEquals("-p", candidates.get(index++).toString());
        assertEquals("-v", candidates.get(index).toString());
    }

    @Test
    public void testCompleteCommandSingleAvailableWithTwoDashes() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("service list --", 15, candidates);

        assertEquals(3, candidates.size());
        assertEquals(13, position);

        int index = 0;
        assertEquals("--host", candidates.get(index++).toString());
        assertEquals("--port", candidates.get(index++).toString());
        assertEquals("--version", candidates.get(index).toString());
    }

    @Test
    public void testCompleteCommandSingleAvailableWithParamPartial() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("service list --ho", 17, candidates);

        assertEquals(1, candidates.size());
        assertEquals(13, position);
        assertEquals("--host", candidates.get(0).toString());
    }

    @Test
    public void testCompleteCommandSingleAvailableWithParamComplete() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("service list --host", 19, candidates);

        assertEquals(1, candidates.size());
        assertEquals(13, position);
        assertEquals("--host", candidates.get(0).toString());
    }

    @Test
    public void testCompleteCommandSingleAvailableWithParamCompleteAndSpace() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("service list --host ", 20, candidates);

        assertEquals(2, candidates.size());
        assertEquals(20, position);
    }

    @Test
    public void testCompleteCommandSingleAvailableWithParamCompleteAndPartialValue() {
        final List<CharSequence> candidates = new ArrayList<>();
        final int position = shellCompleter.complete("service list --host h", 21, candidates);

        assertEquals(2, candidates.size());
        assertEquals(20, position);
        assertEquals("[host1, host2]", candidates.toString());
    }
}
