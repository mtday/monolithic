package monolithic.shell.runner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import monolithic.common.config.ConfigKeys;
import monolithic.crypto.CryptoFactory;
import monolithic.discovery.DiscoveryManager;
import monolithic.shell.CapturingConsoleReader;
import monolithic.shell.ConsoleManager;
import monolithic.shell.RegistrationManager;
import monolithic.shell.model.ShellEnvironment;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Perform testing on the {@link Runner} class.
 */
public class RunnerTest {
    @Test
    public void testRun() throws Exception {
        // Don't want to see stack traces in the output when attempting to create invalid commands during testing.
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RegistrationManager.class)).setLevel(Level.OFF);

        try (final TestingServer testServer = new TestingServer(true)) {
            final Map<String, ConfigValue> map = new HashMap<>();
            map.put(ConfigKeys.ZOOKEEPER_HOSTS.getKey(),
                    ConfigValueFactory.fromAnyRef(testServer.getConnectString()));
            final Config config = ConfigFactory.parseMap(map).withFallback(ConfigFactory.load());

            final ConsoleManager consoleManager = Mockito.mock(ConsoleManager.class);
            final Runner runner = new Runner(config);
            runner.setConsoleManager(consoleManager);
            runner.run();
            runner.shutdown();
        }
    }

    @Test
    public void testProcessCommandLineNoOptions() throws Exception {
        final Runner runner = Mockito.mock(Runner.class);
        Runner.processCommandLine(runner, new String[] {"shell"});
        Mockito.verify(runner).run();
        Mockito.verify(runner).shutdown();
    }

    @Test
    public void testProcessCommandLineWithNonExistentFile() throws Exception {
        final Runner runner = Mockito.mock(Runner.class);
        Runner.processCommandLine(runner, new String[] {"shell", "-f", "non-existent-file.txt"});
        Mockito.verify(runner, Mockito.times(0)).run(Mockito.any(File.class));
        Mockito.verify(runner).shutdown();
    }

    @Test
    public void testProcessCommandLineWithInvalidArgs() throws Exception {
        final Runner runner = Mockito.mock(Runner.class);
        Runner.processCommandLine(runner, new String[] {"shell", "-a"});
        Mockito.verify(runner, Mockito.times(0)).run(Mockito.any(File.class));
        Mockito.verify(runner).shutdown();
    }

    @Test
    public void testProcessCommandLineWithFile() throws Exception {
        final TemporaryFolder tmp = new TemporaryFolder();
        tmp.create();
        final File file = tmp.newFile();

        try {
            final Runner runner = Mockito.mock(Runner.class);
            Runner.processCommandLine(runner, new String[] {"shell", "-f", file.getAbsolutePath()});
            Mockito.verify(runner).run(Mockito.any(File.class));
            Mockito.verify(runner).shutdown();
        } finally {
            assertTrue(file.delete());
            tmp.delete();
        }
    }

    @Test
    public void testProcessCommandLineWithCommand() throws Exception {
        final ConsoleManager consoleManager = Mockito.mock(ConsoleManager.class);
        final Runner runner = Mockito.mock(Runner.class);
        Mockito.when(runner.getConsoleManager()).thenReturn(consoleManager);
        Mockito.doCallRealMethod().when(runner).run(Mockito.anyString());
        Runner.processCommandLine(runner, new String[] {"shell", "-c", "help"});
        Mockito.verify(runner).run(Mockito.anyString());
        Mockito.verify(runner).shutdown();
    }

    @Test
    public void testRunWithFile() throws Exception {
        final TemporaryFolder tmp = new TemporaryFolder();
        tmp.create();
        final File file = tmp.newFile();
        try {
            try (final FileWriter fw = new FileWriter(file);
                 final PrintWriter pw = new PrintWriter(fw, true)) {
                pw.println("help");
                pw.println("service list");
                pw.println("quit");
            }

            // Don't want to see stack traces in the output when attempting to create invalid commands during testing.
            ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RegistrationManager.class)).setLevel(Level.OFF);

            try (final TestingServer testServer = new TestingServer(true)) {
                final Map<String, ConfigValue> map = new HashMap<>();
                map.put(
                        ConfigKeys.ZOOKEEPER_HOSTS.getKey(),
                        ConfigValueFactory.fromAnyRef(testServer.getConnectString()));
                final Config config = ConfigFactory.parseMap(map).withFallback(ConfigFactory.load());
                final ExecutorService executor = Executors.newFixedThreadPool(3);
                final CuratorFramework curator =
                        CuratorFrameworkFactory.builder().connectString(testServer.getConnectString()).namespace("test")
                                .retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
                curator.start();
                final DiscoveryManager discovery = new DiscoveryManager(config, curator);
                final RegistrationManager registrationManager = new RegistrationManager();
                final OkHttpClient httpClient = new OkHttpClient.Builder().build();
                final CryptoFactory cryptoFactory = new CryptoFactory(config);
                final ShellEnvironment shellEnvironment =
                        new ShellEnvironment(config, executor, discovery, curator, registrationManager, httpClient,
                                cryptoFactory);
                registrationManager.loadCommands(shellEnvironment);

                final CapturingConsoleReader consoleReader = new CapturingConsoleReader();
                final ConsoleManager consoleManager = new ConsoleManager(config, shellEnvironment, consoleReader);

                final Runner runner = new Runner(config);
                runner.setConsoleManager(consoleManager);
                Runner.processCommandLine(runner, new String[] {"shell", "-f", file.getAbsolutePath()});

                final List<String> lines = consoleReader.getOutputLines();
                assertEquals(18, lines.size());

                int line = 0;
                assertEquals("# help", lines.get(line++));
                assertEquals("  config list              display system configuration information", lines.get(line++));
                assertEquals("  crypto decrypt           decrypt the provided input data", lines.get(line++));
                assertEquals("  crypto encrypt           encrypt the provided input data", lines.get(line++));
                assertEquals("  crypto sign              sign the provided input data", lines.get(line++));
                assertEquals("  crypto verify            verify the provided input data", lines.get(line++));
                assertEquals("  exit                     exit the shell", lines.get(line++));
                assertEquals("  help                     display usage information for available shell commands",
                        lines.get(line++));
                assertEquals("  quit                     exit the shell", lines.get(line++));
                assertEquals(
                        "  service control restart  request the restart of one or more services", lines.get(line++));
                assertEquals("  service control stop     request the stop of one or more services", lines.get(line++));
                assertEquals(
                        "  service list             provides information about the available services",
                        lines.get(line++));
                assertEquals("  service memory           display memory usage information for one or more services",
                        lines.get(line++));
                assertEquals("# service list", lines.get(line++));
                assertEquals("No services are running", lines.get(line++));
                assertEquals("# quit", lines.get(line++));
                assertEquals("Terminating", lines.get(line++));
                assertEquals("\n", lines.get(line));
            }
        } finally {
            assertTrue(file.delete());
            tmp.delete();
        }
    }

    @Test
    public void testMainWithInvalidArgs() throws Exception {
        Runner.main("shell", "-a");
    }
}
