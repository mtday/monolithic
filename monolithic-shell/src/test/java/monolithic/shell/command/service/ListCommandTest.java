package monolithic.shell.command.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import monolithic.discovery.DiscoveryException;
import monolithic.discovery.DiscoveryManager;
import monolithic.discovery.model.Service;
import monolithic.shell.model.CommandPath;
import monolithic.shell.model.CommandStatus;
import monolithic.shell.model.Option;
import monolithic.shell.model.Registration;
import monolithic.shell.model.ShellEnvironment;
import monolithic.shell.model.UserCommand;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;

/**
 * Perform testing of the {@link ListCommand} class.
 */
public class ListCommandTest {
    @Nonnull
    protected ShellEnvironment getShellEnvironment() throws Exception {
        final SortedSet<Service> services = new TreeSet<>();
        services.add(new Service("system", "1.2.3", "host1", 1234, false));
        services.add(new Service("system", "1.2.3", "host1", 1235, false));
        services.add(new Service("system", "1.2.4", "host2", 1236, true));
        services.add(new Service("system", "1.2.4", "host2", 1237, true));
        final DiscoveryManager discoveryManager = Mockito.mock(DiscoveryManager.class);
        Mockito.when(discoveryManager.getAll()).thenReturn(services);
        final ShellEnvironment shellEnvironment = Mockito.mock(ShellEnvironment.class);
        Mockito.when(shellEnvironment.getDiscoveryManager()).thenReturn(discoveryManager);
        return shellEnvironment;
    }

    @Test
    public void testGetRegistrations() {
        final ShellEnvironment shellEnvironment = Mockito.mock(ShellEnvironment.class);
        final ListCommand listCommand = new ListCommand(shellEnvironment);

        final List<Registration> registrations = listCommand.getRegistrations();
        assertEquals(1, registrations.size());

        final Registration list = registrations.get(0);
        assertEquals(new CommandPath("service", "list"), list.getPath());
        assertTrue(list.getDescription().isPresent());
        assertEquals("provides information about the available services", list.getDescription().get());
        assertTrue(list.getOptions().isPresent());
        final SortedSet<Option> listOptions = list.getOptions().get().getOptions();
        assertEquals(3, listOptions.size());
    }

    @Test
    public void testProcessList() throws Exception {
        final ListCommand listCommand = new ListCommand(getShellEnvironment());

        final CommandPath commandPath = new CommandPath("service", "list");
        final Registration reg = new Registration(commandPath, Optional.empty(), Optional.empty());
        final UserCommand userCommand = new UserCommand(commandPath, reg, commandPath.getPath());
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter, true);

        final CommandStatus status = listCommand.process(userCommand, writer);
        assertEquals(CommandStatus.SUCCESS, status);

        final List<String> output = Arrays.asList(stringWriter.getBuffer().toString().split(System.lineSeparator()));
        assertEquals(5, output.size());

        int line = 0;
        assertEquals("Displaying all 4 services:", output.get(line++));
        assertEquals("    host1  1234  insecure  1.2.3", output.get(line++));
        assertEquals("    host1  1235  insecure  1.2.3", output.get(line++));
        assertEquals("    host2  1236  secure    1.2.4", output.get(line++));
        assertEquals("    host2  1237  secure    1.2.4", output.get(line));
    }

    @Test
    public void testProcessListWithHost() throws Exception {
        final ListCommand listCommand = new ListCommand(getShellEnvironment());

        final List<String> input = Arrays.asList("service", "list", "-h", "host1");
        final CommandPath commandPath = new CommandPath("service", "list");
        final Registration reg = listCommand.getRegistrations().get(0);
        final UserCommand userCommand = new UserCommand(commandPath, reg, input);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter, true);

        final CommandStatus status = listCommand.process(userCommand, writer);
        assertEquals(CommandStatus.SUCCESS, status);

        final List<String> output = Arrays.asList(stringWriter.getBuffer().toString().split(System.lineSeparator()));
        assertEquals(3, output.size());

        int line = 0;
        assertEquals("Displaying 2 matching services (of 4 total):", output.get(line++));
        assertEquals("    host1  1234  insecure  1.2.3", output.get(line++));
        assertEquals("    host1  1235  insecure  1.2.3", output.get(line));
    }

    @Test
    public void testProcessListWithPort() throws Exception {
        final ListCommand listCommand = new ListCommand(getShellEnvironment());

        final List<String> input = Arrays.asList("service", "list", "-p", "1234");
        final CommandPath commandPath = new CommandPath("service", "list");
        final Registration reg = listCommand.getRegistrations().get(0);
        final UserCommand userCommand = new UserCommand(commandPath, reg, input);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter, true);

        final CommandStatus status = listCommand.process(userCommand, writer);
        assertEquals(CommandStatus.SUCCESS, status);

        final List<String> output = Arrays.asList(stringWriter.getBuffer().toString().split(System.lineSeparator()));
        assertEquals(2, output.size());

        int line = 0;
        assertEquals("Displaying the matching service (of 4 total):", output.get(line++));
        assertEquals("    host1  1234  insecure  1.2.3", output.get(line));
    }

    @Test
    public void testProcessListWithPortNotNumeric() throws Exception {
        final ListCommand listCommand = new ListCommand(getShellEnvironment());

        final List<String> input = Arrays.asList("service", "list", "-p", "abcd");
        final CommandPath commandPath = new CommandPath("service", "list");
        final Registration reg = listCommand.getRegistrations().get(0);
        final UserCommand userCommand = new UserCommand(commandPath, reg, input);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter, true);

        final CommandStatus status = listCommand.process(userCommand, writer);
        assertEquals(CommandStatus.SUCCESS, status);

        final List<String> output = Arrays.asList(stringWriter.getBuffer().toString().split(System.lineSeparator()));
        assertEquals(5, output.size());

        int line = 0;
        assertEquals("Displaying all 4 services:", output.get(line++));
        assertEquals("    host1  1234  insecure  1.2.3", output.get(line++));
        assertEquals("    host1  1235  insecure  1.2.3", output.get(line++));
        assertEquals("    host2  1236  secure    1.2.4", output.get(line++));
        assertEquals("    host2  1237  secure    1.2.4", output.get(line));
    }

    @Test
    public void testProcessListWithVersion() throws Exception {
        final ListCommand listCommand = new ListCommand(getShellEnvironment());

        final List<String> input = Arrays.asList("service", "list", "-v", "1.2.3");
        final CommandPath commandPath = new CommandPath("service", "list");
        final Registration reg = listCommand.getRegistrations().get(0);
        final UserCommand userCommand = new UserCommand(commandPath, reg, input);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter, true);

        final CommandStatus status = listCommand.process(userCommand, writer);
        assertEquals(CommandStatus.SUCCESS, status);

        final List<String> output = Arrays.asList(stringWriter.getBuffer().toString().split(System.lineSeparator()));
        assertEquals(3, output.size());

        int line = 0;
        assertEquals("Displaying 2 matching services (of 4 total):", output.get(line++));
        assertEquals("    host1  1234  insecure  1.2.3", output.get(line++));
        assertEquals("    host1  1235  insecure  1.2.3", output.get(line));
    }

    @Test
    public void testProcessListNoMatchingServices() throws Exception {
        final ListCommand listCommand = new ListCommand(getShellEnvironment());

        final List<String> input = Arrays.asList("service", "list", "-h", "missing");
        final CommandPath commandPath = new CommandPath("service", "list");
        final Registration reg = listCommand.getRegistrations().get(0);
        final UserCommand userCommand = new UserCommand(commandPath, reg, input);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter, true);

        final CommandStatus status = listCommand.process(userCommand, writer);
        assertEquals(CommandStatus.SUCCESS, status);

        final List<String> output = Arrays.asList(stringWriter.getBuffer().toString().split(System.lineSeparator()));
        assertEquals(1, output.size());
        assertEquals("None of the running services (of which there are 4) match", output.get(0));
    }

    @Test
    public void testProcessListNoServices() throws Exception {
        final DiscoveryManager discoveryManager = Mockito.mock(DiscoveryManager.class);
        Mockito.when(discoveryManager.getAll()).thenReturn(new TreeSet<>());
        final ShellEnvironment shellEnvironment = Mockito.mock(ShellEnvironment.class);
        Mockito.when(shellEnvironment.getDiscoveryManager()).thenReturn(discoveryManager);
        final ListCommand listCommand = new ListCommand(shellEnvironment);

        final List<String> input = Arrays.asList("service", "list");
        final CommandPath commandPath = new CommandPath("service", "list");
        final Registration reg = listCommand.getRegistrations().get(0);
        final UserCommand userCommand = new UserCommand(commandPath, reg, input);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter, true);

        final CommandStatus status = listCommand.process(userCommand, writer);
        assertEquals(CommandStatus.SUCCESS, status);

        final List<String> output = Arrays.asList(stringWriter.getBuffer().toString().split(System.lineSeparator()));
        assertEquals(1, output.size());
        assertEquals("No services are running", output.get(0));
    }

    @Test
    public void testProcessListOneService() throws Exception {
        final SortedSet<Service> services = new TreeSet<>();
        services.add(new Service("system", "1.2.3", "host1", 1234, false));
        final DiscoveryManager discoveryManager = Mockito.mock(DiscoveryManager.class);
        Mockito.when(discoveryManager.getAll()).thenReturn(services);
        final ShellEnvironment shellEnvironment = Mockito.mock(ShellEnvironment.class);
        Mockito.when(shellEnvironment.getDiscoveryManager()).thenReturn(discoveryManager);
        final ListCommand listCommand = new ListCommand(shellEnvironment);

        final List<String> input = Arrays.asList("service", "list");
        final CommandPath commandPath = new CommandPath("service", "list");
        final Registration reg = listCommand.getRegistrations().get(0);
        final UserCommand userCommand = new UserCommand(commandPath, reg, input);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter, true);

        final CommandStatus status = listCommand.process(userCommand, writer);
        assertEquals(CommandStatus.SUCCESS, status);

        final List<String> output = Arrays.asList(stringWriter.getBuffer().toString().split(System.lineSeparator()));
        assertEquals(2, output.size());
        assertEquals("Displaying the single available service:", output.get(0));
        assertEquals("    host1  1234  insecure  1.2.3", output.get(1));
    }

    @Test
    public void testProcessListTwoServices() throws Exception {
        final SortedSet<Service> services = new TreeSet<>();
        services.add(new Service("system", "1.2.3", "host1", 1234, false));
        services.add(new Service("system", "1.2.3", "host1", 1235, false));
        final DiscoveryManager discoveryManager = Mockito.mock(DiscoveryManager.class);
        Mockito.when(discoveryManager.getAll()).thenReturn(services);
        final ShellEnvironment shellEnvironment = Mockito.mock(ShellEnvironment.class);
        Mockito.when(shellEnvironment.getDiscoveryManager()).thenReturn(discoveryManager);
        final ListCommand listCommand = new ListCommand(shellEnvironment);

        final List<String> input = Arrays.asList("service", "list");
        final CommandPath commandPath = new CommandPath("service", "list");
        final Registration reg = listCommand.getRegistrations().get(0);
        final UserCommand userCommand = new UserCommand(commandPath, reg, input);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter, true);

        final CommandStatus status = listCommand.process(userCommand, writer);
        assertEquals(CommandStatus.SUCCESS, status);

        final List<String> output = Arrays.asList(stringWriter.getBuffer().toString().split(System.lineSeparator()));
        assertEquals(3, output.size());
        assertEquals("Displaying both available services:", output.get(0));
        assertEquals("    host1  1234  insecure  1.2.3", output.get(1));
        assertEquals("    host1  1235  insecure  1.2.3", output.get(2));
    }

    @Test
    public void testHandleListException() throws Exception {
        final DiscoveryManager discoveryManager = Mockito.mock(DiscoveryManager.class);
        Mockito.when(discoveryManager.getAll()).thenThrow(new DiscoveryException("Fake"));
        final ShellEnvironment shellEnvironment = Mockito.mock(ShellEnvironment.class);
        Mockito.when(shellEnvironment.getDiscoveryManager()).thenReturn(discoveryManager);

        final ListCommand listCommand = new ListCommand(shellEnvironment);

        final CommandPath commandPath = new CommandPath("service", "list");
        final UserCommand userCommand = Mockito.mock(UserCommand.class);
        Mockito.when(userCommand.getCommandPath()).thenReturn(commandPath);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter, true);

        final CommandStatus status = listCommand.process(userCommand, writer);
        assertEquals(CommandStatus.SUCCESS, status);

        final List<String> output = Arrays.asList(stringWriter.getBuffer().toString().split(System.lineSeparator()));
        assertEquals(1, output.size());
        assertEquals("Failed to retrieve available services: DiscoveryException: Fake", output.get(0));
    }
}
