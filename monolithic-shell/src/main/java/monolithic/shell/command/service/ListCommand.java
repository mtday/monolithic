package monolithic.shell.command.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * This command implements the {@code service list} command in the shell.
 */
public class ListCommand extends BaseServiceCommand {
    /**
     * @param shellEnvironment the shell command execution environment
     */
    public ListCommand(@Nonnull final ShellEnvironment shellEnvironment) {
        super(shellEnvironment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public List<Registration> getRegistrations() {
        final Option host = getHostOption("the host to list");
        final Option port = getPortOption("the port to list");
        final Option version = getVersionOption("the version to list");
        final Optional<Options> listOptions = Optional.of(new Options(host, port, version));

        final Optional<String> description = Optional.of("provides information about the available services");
        final CommandPath commandPath = new CommandPath("service", "list");
        return Collections.singletonList(new Registration(commandPath, listOptions, description));
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

            final Stringer stringer = new Stringer(filtered);
            final List<String> output = filtered.stream().map(stringer::toString).collect(Collectors.toList());

            writer.println(new ServiceSummary(services.size(), output.size()));
            output.forEach(writer::println);
        } catch (final DiscoveryException exception) {
            writer.println("Failed to retrieve available services: " + ExceptionUtils.getMessage(exception));
        }

        return CommandStatus.SUCCESS;
    }

    protected static class Stringer {
        private final OptionalInt longestHost;
        private final OptionalInt longestPort;

        public Stringer(@Nonnull final List<Service> services) {
            this.longestHost = services.stream().mapToInt(s -> s.getHost().length()).max();
            this.longestPort = services.stream().mapToInt(s -> String.valueOf(s.getPort()).length()).max();
        }

        @Nonnull
        public String toString(@Nonnull final Service service) {
            final String host = StringUtils.rightPad(service.getHost(), this.longestHost.getAsInt());
            final String port = StringUtils.rightPad(String.valueOf(service.getPort()), this.longestPort.getAsInt());
            final String secure = service.isSecure() ? "secure  " : "insecure";
            final String version = service.getSystemVersion();
            return String.format("    %s  %s  %s  %s", host, port, secure, version);
        }
    }
}
