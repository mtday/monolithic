package monolithic.shell.command.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import monolithic.common.model.service.ServiceMemory;
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
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * This command implements the {@code service memory} command in the shell.
 */
public class MemoryCommand extends BaseServiceCommand {
    /**
     * @param shellEnvironment the shell command execution environment
     */
    public MemoryCommand(@Nonnull final ShellEnvironment shellEnvironment) {
        super(shellEnvironment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public List<Registration> getRegistrations() {
        final Option host = getHostOption("the host of the service to show memory information");
        final Option port = getPortOption("the port of the service to show memory information");
        final Option version = getVersionOption("the version of the service to show memory information");
        final Optional<Options> options = Optional.of(new Options(host, port, version));

        final Optional<String> desc = Optional.of("display memory usage information for one or more services");
        final CommandPath commandPath = new CommandPath("service", "memory");
        return Collections.singletonList(new Registration(commandPath, options, desc));
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
                final Future<Map<Service, ServiceMemory>> future =
                        getShellEnvironment().getServerClient().getMemory(filtered);
                final Map<Service, ServiceMemory> memoryMap = future.get(10, TimeUnit.SECONDS);

                final Stringer stringer = new Stringer(filtered);
                memoryMap.entrySet().stream().map(stringer::toString).forEach(writer::println);
            }
        } catch (final ExecutionException | InterruptedException | TimeoutException | DiscoveryException exception) {
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
        public String toString(@Nonnull final Map.Entry<Service, ServiceMemory> entry) {
            final Service service = entry.getKey();
            final ServiceMemory memory = entry.getValue();

            final String host = StringUtils.rightPad(service.getHost(), this.longestHost.getAsInt());
            final String port = StringUtils.rightPad(String.valueOf(service.getPort()), this.longestPort.getAsInt());

            final String heapUsed = readable(memory.getHeapUsed());
            final String heapAvailable = readable(memory.getHeapAvailable());
            final double heapPct = memory.getHeapUsedPercent();

            final String nonheapUsed = readable(memory.getNonHeapUsed());
            final String nonheapAvailable = readable(memory.getNonHeapAvailable());
            final double nonheapPct = memory.getNonHeapUsedPercent();

            return String
                    .format("    %s  %s  Heap: %s of %s (%.2f%%), Non-Heap: %s of %s (%.2f%%)", host, port, heapUsed,
                            heapAvailable, heapPct, nonheapUsed, nonheapAvailable, nonheapPct);
        }

        @Nonnull
        public String readable(final long bytes) {
            if (bytes <= 0) {
                return "unknown";
            } else if (bytes < 1024) {
                // Bytes
                return String.format("%db", bytes);
            } else if (bytes < 1024 * 1024) {
                // Kilobytes
                return String.format("%.02fk", (double) bytes / 1024);
            } else if (bytes < 1024 * 1024 * 1024) {
                // Megabytes
                return String.format("%.02fM", (double) bytes / 1024 / 1024);
            } else {
                // Gigabytes
                return String.format("%.02fG", (double) bytes / 1024 / 1024 / 1024);
            }
        }
    }
}
