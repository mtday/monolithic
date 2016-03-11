package monolithic.shell.command.service;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;

import monolithic.discovery.model.Service;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Responsible for performing filtering operations on {@link Service} objects based on user-provided command-line
 * parameters from the shell.
 */
class ServiceFilter {
    @Nonnull
    private final Optional<String> host;
    @Nonnull
    private final Optional<Integer> port;
    @Nonnull
    private final Optional<String> version;

    public ServiceFilter(@Nonnull final Optional<CommandLine> commandLine) {
        Objects.requireNonNull(commandLine);
        if (commandLine.isPresent()) {
            if (commandLine.get().hasOption('h')) {
                this.host = Optional.of(commandLine.get().getOptionValue('h'));
            } else {
                this.host = Optional.empty();
            }
            if (commandLine.get().hasOption('p') && StringUtils.isNumeric(commandLine.get().getOptionValue('p'))) {
                this.port = Optional.of(Integer.parseInt(commandLine.get().getOptionValue('p'), 10));
            } else {
                this.port = Optional.empty();
            }
            if (commandLine.get().hasOption('v')) {
                this.version = Optional.of(commandLine.get().getOptionValue('v'));
            } else {
                this.version = Optional.empty();
            }
        } else {
            this.host = Optional.empty();
            this.port = Optional.empty();
            this.version = Optional.empty();
        }
    }

    public boolean matches(@Nonnull final Service service) {
        Objects.requireNonNull(service);
        boolean matches = true;
        if (this.host.isPresent() && !StringUtils.equalsIgnoreCase(this.host.get(), service.getHost())) {
            matches = false;
        }
        if (this.port.isPresent() && this.port.get() != service.getPort()) {
            matches = false;
        }
        if (this.version.isPresent() && !StringUtils.equalsIgnoreCase(this.version.get(), service.getSystemVersion())) {
            matches = false;
        }
        return matches;
    }
}
