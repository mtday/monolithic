package monolithic.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import monolithic.common.config.ConfigKeys;
import monolithic.crypto.CryptoFactory;
import monolithic.discovery.DiscoveryManager;
import monolithic.shell.model.ShellEnvironment;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Perform testing on the {@link ConsoleManager} class
 */
public class ConsoleManagerTest {
    @Test
    public void testStandardConstructor() throws IOException {
        final Config config = ConfigFactory.load();
        final ShellEnvironment shellEnvironment = Mockito.mock(ShellEnvironment.class);
        Mockito.when(shellEnvironment.getRegistrationManager()).thenReturn(new RegistrationManager());
        final ConsoleManager cm = new ConsoleManager(config, shellEnvironment);
        cm.stop();

        assertEquals(config, cm.getConfig());
        assertEquals(shellEnvironment, cm.getShellEnvironment());
        assertNotNull(cm.getConsoleReader());
    }

    @Test
    public void testRun() throws IOException {
        // Don't want to see stack traces in the output when attempting to create invalid commands during testing.
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RegistrationManager.class)).setLevel(Level.OFF);

        final Config config =
                ConfigFactory.parseString(String.format("%s = 1.2.3", ConfigKeys.SYSTEM_VERSION.getKey()))
                        .withFallback(ConfigFactory.load());
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
        final CuratorFramework curator = Mockito.mock(CuratorFramework.class);
        final RegistrationManager registrationManager = new RegistrationManager();
        final OkHttpClient httpClient = new OkHttpClient.Builder().build();
        final CryptoFactory cryptoFactory = new CryptoFactory(config);
        final ShellEnvironment shellEnvironment =
                new ShellEnvironment(config, executor, discovery, curator, registrationManager, httpClient,
                        cryptoFactory);
        registrationManager.loadCommands(shellEnvironment);

        final CapturingConsoleReader consoleReader =
                new CapturingConsoleReader("unrecognized", "", "  #comment", "help", "s", "service li -h", "'invalid");

        final ConsoleManager cm = new ConsoleManager(config, shellEnvironment, consoleReader);
        cm.run();

        assertTrue(consoleReader.isShutdown());

        final List<String> lines = consoleReader.getOutputLines();
        assertEquals(28, lines.size());

        int line = 0;
        // Startup
        assertEquals("\n", lines.get(line++));
        assertEquals("monolithic 1.2.3", lines.get(line++));
        assertEquals("\n", lines.get(line++));
        assertEquals("Type 'help' to list the available commands", lines.get(line++));

        // unrecognized
        assertEquals("Unrecognized command: unrecognized", lines.get(line++));
        assertEquals("Use 'help' to see all the available commands.", lines.get(line++));

        // blank and #comment

        // help
        assertEquals("  config list              display system configuration information", lines.get(line++));
        assertEquals("  crypto decrypt           decrypt the provided input data", lines.get(line++));
        assertEquals("  crypto encrypt           encrypt the provided input data", lines.get(line++));
        assertEquals("  crypto sign              sign the provided input data", lines.get(line++));
        assertEquals("  crypto verify            verify the provided input data", lines.get(line++));
        assertEquals("  exit                     exit the shell", lines.get(line++));
        assertEquals("  help                     display usage information for available shell commands",
                lines.get(line++));
        assertEquals("  quit                     exit the shell", lines.get(line++));
        assertEquals("  service control restart  request the restart of one or more services", lines.get(line++));
        assertEquals("  service control stop     request the stop of one or more services", lines.get(line++));
        assertEquals("  service list             provides information about the available services", lines.get(line++));
        assertEquals("  service memory           display memory usage information for one or more services",
                lines.get(line++));

        // s
        assertEquals("Showing help for commands that begin with: s", lines.get(line++));
        assertEquals("  service control restart  request the restart of one or more services", lines.get(line++));
        assertEquals("  service control stop     request the stop of one or more services", lines.get(line++));
        assertEquals("  service list             provides information about the available services", lines.get(line++));
        assertEquals("  service memory           display memory usage information for one or more services",
                lines.get(line++));

        // service li -h
        assertEquals("Assuming you mean: service list", lines.get(line++));
        assertEquals("Missing argument for option: h", lines.get(line++));

        // 'invalid
        assertEquals("--------------^", lines.get(line++));
        assertEquals("Missing terminating quote", lines.get(line++));

        // no more input
        assertEquals("\n", lines.get(line));
    }

    @Test
    public void testRunWithInterrupt() throws IOException {
        // Don't want to see stack traces in the output when attempting to create invalid commands during testing.
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RegistrationManager.class)).setLevel(Level.OFF);

        final Config config =
                ConfigFactory.parseString(String.format("%s = 1.2.3", ConfigKeys.SYSTEM_VERSION.getKey()))
                        .withFallback(ConfigFactory.load());
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
        final CuratorFramework curator = Mockito.mock(CuratorFramework.class);
        final RegistrationManager registrationManager = new RegistrationManager();
        final OkHttpClient httpClient = new OkHttpClient.Builder().build();
        final CryptoFactory cryptoFactory = new CryptoFactory(config);
        final ShellEnvironment shellEnvironment =
                new ShellEnvironment(config, executor, discovery, curator, registrationManager, httpClient,
                        cryptoFactory);
        registrationManager.loadCommands(shellEnvironment);

        final CapturingConsoleReader consoleReader = new CapturingConsoleReader("help");
        consoleReader.setInterrupt("");

        final ConsoleManager cm = new ConsoleManager(config, shellEnvironment, consoleReader);
        cm.run();
        assertTrue(consoleReader.isShutdown());

        final List<String> lines = consoleReader.getOutputLines();
        assertEquals(5, lines.size());

        int line = 0;
        // Startup
        assertEquals("\n", lines.get(line++));
        assertEquals("monolithic 1.2.3", lines.get(line++));
        assertEquals("\n", lines.get(line++));
        assertEquals("Type 'help' to list the available commands", lines.get(line++));

        // no more input
        assertEquals("\n", lines.get(line));
    }

    @Test
    public void testRunWithInterruptPartial() throws IOException {
        // Don't want to see stack traces in the output when attempting to create invalid commands during testing.
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RegistrationManager.class)).setLevel(Level.OFF);

        final Config config =
                ConfigFactory.parseString(String.format("%s = 1.2.3", ConfigKeys.SYSTEM_VERSION.getKey()))
                        .withFallback(ConfigFactory.load());
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
        final CuratorFramework curator = Mockito.mock(CuratorFramework.class);
        final RegistrationManager registrationManager = new RegistrationManager();
        final OkHttpClient httpClient = new OkHttpClient.Builder().build();
        final CryptoFactory cryptoFactory = new CryptoFactory(config);
        final ShellEnvironment shellEnvironment =
                new ShellEnvironment(config, executor, discovery, curator, registrationManager, httpClient,
                        cryptoFactory);
        registrationManager.loadCommands(shellEnvironment);

        final CapturingConsoleReader consoleReader = new CapturingConsoleReader("help");
        consoleReader.setInterrupt("partial");

        final ConsoleManager cm = new ConsoleManager(config, shellEnvironment, consoleReader);
        cm.run();
        assertTrue(consoleReader.isShutdown());

        final List<String> lines = consoleReader.getOutputLines();
        assertEquals(17, lines.size());

        int line = 0;
        // Startup
        assertEquals("\n", lines.get(line++));
        assertEquals("monolithic 1.2.3", lines.get(line++));
        assertEquals("\n", lines.get(line++));
        assertEquals("Type 'help' to list the available commands", lines.get(line++));

        // help
        assertEquals("  config list              display system configuration information", lines.get(line++));
        assertEquals("  crypto decrypt           decrypt the provided input data", lines.get(line++));
        assertEquals("  crypto encrypt           encrypt the provided input data", lines.get(line++));
        assertEquals("  crypto sign              sign the provided input data", lines.get(line++));
        assertEquals("  crypto verify            verify the provided input data", lines.get(line++));
        assertEquals("  exit                     exit the shell", lines.get(line++));
        assertEquals("  help                     display usage information for available shell commands",
                lines.get(line++));
        assertEquals("  quit                     exit the shell", lines.get(line++));
        assertEquals("  service control restart  request the restart of one or more services", lines.get(line++));
        assertEquals("  service control stop     request the stop of one or more services", lines.get(line++));
        assertEquals("  service list             provides information about the available services", lines.get(line++));
        assertEquals("  service memory           display memory usage information for one or more services",
                lines.get(line++));

        // no more input
        assertEquals("\n", lines.get(line));
    }

    @Test
    public void testRunWithFileDoesNotExist() throws IOException {
        // Don't want to see stack traces in the output when attempting to create invalid commands during testing.
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RegistrationManager.class)).setLevel(Level.OFF);

        final TemporaryFolder tmp = new TemporaryFolder();
        tmp.create();
        final File file = new File(tmp.getRoot(), "tmpfile.txt");

        try {
            final Config config =
                    ConfigFactory.parseString(String.format("%s = 1.2.3", ConfigKeys.SYSTEM_VERSION.getKey()))
                            .withFallback(ConfigFactory.load());
            final ExecutorService executor = Executors.newFixedThreadPool(3);
            final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
            final CuratorFramework curator = Mockito.mock(CuratorFramework.class);
            final RegistrationManager registrationManager = new RegistrationManager();
            final OkHttpClient httpClient = new OkHttpClient.Builder().build();
            final CryptoFactory cryptoFactory = new CryptoFactory(config);
            final ShellEnvironment shellEnvironment =
                    new ShellEnvironment(config, executor, discovery, curator, registrationManager, httpClient,
                            cryptoFactory);
            registrationManager.loadCommands(shellEnvironment);

            final CapturingConsoleReader consoleReader = new CapturingConsoleReader();
            final ConsoleManager cm = new ConsoleManager(config, shellEnvironment, consoleReader);
            cm.run(file);
            assertTrue(consoleReader.isShutdown());

            final List<String> lines = consoleReader.getOutputLines();
            assertEquals(1, lines.size());
            assertEquals("\n", lines.get(0));
        } finally {
            tmp.delete();
        }
    }

    @Test
    public void testRunWithFile() throws IOException {
        // Don't want to see stack traces in the output when attempting to create invalid commands during testing.
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RegistrationManager.class)).setLevel(Level.OFF);

        final TemporaryFolder tmp = new TemporaryFolder();
        tmp.create();
        final File file = tmp.newFile();
        try (final FileWriter fw = new FileWriter(file);
             final PrintWriter pw = new PrintWriter(fw, true)) {
            pw.println("help");
            pw.println("service list -h host1");
        }

        try {
            final Config config =
                    ConfigFactory.parseString(String.format("%s = 1.2.3", ConfigKeys.SYSTEM_VERSION.getKey()))
                            .withFallback(ConfigFactory.load());
            final ExecutorService executor = Executors.newFixedThreadPool(3);
            final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
            final CuratorFramework curator = Mockito.mock(CuratorFramework.class);
            final RegistrationManager registrationManager = new RegistrationManager();
            final OkHttpClient httpClient = new OkHttpClient.Builder().build();
            final CryptoFactory cryptoFactory = new CryptoFactory(config);
            final ShellEnvironment shellEnvironment =
                    new ShellEnvironment(config, executor, discovery, curator, registrationManager, httpClient,
                            cryptoFactory);
            registrationManager.loadCommands(shellEnvironment);

            final CapturingConsoleReader consoleReader = new CapturingConsoleReader();
            final ConsoleManager cm = new ConsoleManager(config, shellEnvironment, consoleReader);
            cm.run(file);
            assertTrue(consoleReader.isShutdown());

            final List<String> lines = consoleReader.getOutputLines();
            assertEquals(16, lines.size());

            int line = 0;
            // help
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
            assertEquals("  service control restart  request the restart of one or more services", lines.get(line++));
            assertEquals("  service control stop     request the stop of one or more services", lines.get(line++));
            assertEquals("  service list             provides information about the available services",
                    lines.get(line++));
            assertEquals("  service memory           display memory usage information for one or more services",
                    lines.get(line++));
            assertEquals("# service list -h host1", lines.get(line++));
            assertEquals("No services are running", lines.get(line++));

            // no more input
            assertEquals("\n", lines.get(line));
        } finally {
            tmp.delete();
        }
    }

    @Test
    public void testRunWithCommand() throws IOException {
        // Don't want to see stack traces in the output when attempting to create invalid commands during testing.
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RegistrationManager.class)).setLevel(Level.OFF);

        final Config config =
                ConfigFactory.parseString(String.format("%s = 1.2.3", ConfigKeys.SYSTEM_VERSION.getKey()))
                        .withFallback(ConfigFactory.load());
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
        final CuratorFramework curator = Mockito.mock(CuratorFramework.class);
        final RegistrationManager registrationManager = new RegistrationManager();
        final OkHttpClient httpClient = new OkHttpClient.Builder().build();
        final CryptoFactory cryptoFactory = new CryptoFactory(config);
        final ShellEnvironment shellEnvironment =
                new ShellEnvironment(config, executor, discovery, curator, registrationManager, httpClient,
                        cryptoFactory);
        registrationManager.loadCommands(shellEnvironment);

        final CapturingConsoleReader consoleReader = new CapturingConsoleReader();

        final ConsoleManager cm = new ConsoleManager(config, shellEnvironment, consoleReader);
        cm.run("help");
        assertTrue(consoleReader.isShutdown());

        final List<String> lines = consoleReader.getOutputLines();
        assertEquals(14, lines.size());

        int line = 0;
        // help
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
        assertEquals("  service control restart  request the restart of one or more services", lines.get(line++));
        assertEquals("  service control stop     request the stop of one or more services", lines.get(line++));
        assertEquals("  service list             provides information about the available services", lines.get(line++));
        assertEquals("  service memory           display memory usage information for one or more services",
                lines.get(line++));

        // no more input
        assertEquals("\n", lines.get(line));
    }

    @Test
    public void testCreateHistory() throws IOException {
        // Don't want to see stack traces in the output when attempting to create invalid commands during testing.
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RegistrationManager.class)).setLevel(Level.OFF);

        final String tmp = System.getProperty("java.io.tmpdir");
        final Map<String, ConfigValue> map = new HashMap<>();
        map.put("user.home", ConfigValueFactory.fromAnyRef(tmp));
        map.put(ConfigKeys.SHELL_HISTORY_FILE.getKey(), ConfigValueFactory.fromAnyRef("history.txt"));
        final Config config = ConfigFactory.parseMap(map).withFallback(ConfigFactory.load());
        final String systemName = config.getString(ConfigKeys.SYSTEM_NAME.getKey());

        // Make the directory where the history file is stored a file instead.
        final File file = new File(String.format("%s/.%s", tmp, systemName));
        if (file.exists()) {
            assertTrue("Failed to delete existing dir: " + file.getAbsolutePath(), file.delete());
        }

        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final DiscoveryManager discovery = Mockito.mock(DiscoveryManager.class);
        final CuratorFramework curator = Mockito.mock(CuratorFramework.class);
        final RegistrationManager registrationManager = new RegistrationManager();
        final OkHttpClient httpClient = new OkHttpClient.Builder().build();
        final CryptoFactory cryptoFactory = new CryptoFactory(config);
        final ShellEnvironment shellEnvironment =
                new ShellEnvironment(config, executor, discovery, curator, registrationManager, httpClient,
                        cryptoFactory);
        registrationManager.loadCommands(shellEnvironment);

        final CapturingConsoleReader consoleReader = new CapturingConsoleReader();
        final ConsoleManager cm = new ConsoleManager(config, shellEnvironment, consoleReader);

        assertNotNull(cm.createHistory(config));
    }
}
