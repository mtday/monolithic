package monolithic.shell.command.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import monolithic.common.model.service.ServiceControlStatus;
import monolithic.discovery.DiscoveryException;
import monolithic.discovery.model.Service;
import monolithic.shell.model.CommandPath;
import monolithic.shell.model.CommandStatus;
import monolithic.shell.model.Option;
import monolithic.shell.model.Options;
import monolithic.shell.model.Registration;
import monolithic.shell.model.ShellEnvironment;
import monolithic.shell.model.UserCommand;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * This command implements the {@code service control} commands in the shell.
 */
public class ControlCommand extends BaseServiceCommand {
    /**
     * @param shellEnvironment the shell command execution environment
     */
    public ControlCommand(@Nonnull final ShellEnvironment shellEnvironment) {
        super(shellEnvironment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public List<Registration> getRegistrations() {
        final Option host = getHostOption("the host of the service to control");
        final Option port = getPortOption("the port of the service to control");
        final Option version = getVersionOption("the version of the service to control");
        final Optional<Options> options = Optional.of(new Options(host, port, version));

        final Optional<String> stopDescription = Optional.of("request the stop of one or more services");
        final CommandPath serviceStopPath = new CommandPath("service", "control", "stop");
        final Registration serviceStop = new Registration(serviceStopPath, options, stopDescription);

        final Optional<String> restartDescription = Optional.of("request the restart of one or more services");
        final CommandPath serviceRestartPath = new CommandPath("service", "control", "restart");
        final Registration serviceRestart = new Registration(serviceRestartPath, options, restartDescription);

        return Arrays.asList(serviceStop, serviceRestart);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public CommandStatus process(@Nonnull final UserCommand userCommand, @Nonnull final PrintWriter writer) {
        try {
            final SortedSet<Service> services = getShellEnvironment().getDiscoveryManager().getAll();
            final ServiceFilter filter = new ServiceFilter(userCommand.getCommandLine());

            final List<Service> filtered =
                    services.stream().filter(filter::matches).sorted().collect(Collectors.toList());

            writer.println(new ServiceSummary(services.size(), filtered.size()));
            if (!filtered.isEmpty()) {
                if (userCommand.getCommandPath().equals(new CommandPath("service", "control", "stop"))) {
                    return handleStop(filtered, writer);
                } else {
                    return handleRestart(filtered, writer);
                }
            }
        } catch (final ExecutionException | InterruptedException | TimeoutException | DiscoveryException exception) {
            writer.println("Failed to retrieve available services: " + ExceptionUtils.getMessage(exception));
        }

        return CommandStatus.SUCCESS;
    }

    @Nonnull
    protected CommandStatus handleStop(@Nonnull final List<Service> services, @Nonnull final PrintWriter writer)
            throws ExecutionException, InterruptedException, TimeoutException {
        return control(getShellEnvironment().getServerClient().stop(services), writer);
    }

    @Nonnull
    protected CommandStatus handleRestart(@Nonnull final List<Service> services, @Nonnull final PrintWriter writer)
            throws ExecutionException, InterruptedException, TimeoutException {
        return control(getShellEnvironment().getServerClient().restart(services), writer);
    }

    @Nonnull
    protected CommandStatus control(
            @Nonnull final Future<Map<Service, ServiceControlStatus>> future, @Nonnull final PrintWriter writer)
            throws ExecutionException, InterruptedException, TimeoutException {
        final Map<Service, ServiceControlStatus> map = future.get(10, TimeUnit.SECONDS);
        final Stringer stringer = new Stringer(map.keySet());
        map.entrySet().stream().map(stringer::toString).forEach(writer::println);
        return CommandStatus.SUCCESS;
    }

    protected static class Stringer {
        private final OptionalInt longestHost;
        private final OptionalInt longestPort;

        public Stringer(@Nonnull final Set<Service> services) {
            this.longestHost = services.stream().mapToInt(s -> s.getHost().length()).max();
            this.longestPort = services.stream().mapToInt(s -> String.valueOf(s.getPort()).length()).max();
        }

        @Nonnull
        public String toString(@Nonnull final Map.Entry<Service, ServiceControlStatus> entry) {
            final Service service = entry.getKey();
            final ServiceControlStatus status = entry.getValue();

            final String host = StringUtils.rightPad(service.getHost(), this.longestHost.getAsInt());
            final String port = StringUtils.rightPad(String.valueOf(service.getPort()), this.longestPort.getAsInt());

            final String action = status.getAction();
            final boolean success = status.isSuccess();

            return String.format("    %s  %s  - %s in progress: %s", host, port, action, success);
        }
    }
}
