package monolithic.shell.command.service;

import monolithic.shell.completer.ServiceHostCompleter;
import monolithic.shell.completer.ServicePortCompleter;
import monolithic.shell.completer.ServiceVersionCompleter;
import monolithic.shell.model.Command;
import monolithic.shell.model.Option;
import monolithic.shell.model.ShellEnvironment;

import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * This command provides some of the common functionality between the service commands.
 */
public abstract class BaseServiceCommand extends Command {
    /**
     * @param shellEnvironment the shell command execution environment
     */
    public BaseServiceCommand(@Nonnull final ShellEnvironment shellEnvironment) {
        super(shellEnvironment);
    }

    /**
     * @param description the description to include in the option
     * @return the {@link Option} used to input the service host
     */
    @Nonnull
    protected Option getHostOption(@Nonnull final String description) {
        return new Option(description, "h", Optional.of("host"), Optional.of("host"), 1, false, false,
                Optional.of(new ServiceHostCompleter(getShellEnvironment())));
    }

    /**
     * @param description the description to include in the option
     * @return the {@link Option} used to input the service port
     */
    @Nonnull
    protected Option getPortOption(@Nonnull final String description) {
        return new Option(description, "p", Optional.of("port"), Optional.of("port"), 1, false, false,
                Optional.of(new ServicePortCompleter(getShellEnvironment())));
    }

    /**
     * @param description the description to include in the option
     * @return the {@link Option} used to input the service version
     */
    @Nonnull
    protected Option getVersionOption(@Nonnull final String description) {
        return new Option(description, "v", Optional.of("version"), Optional.of("version"), 1, false, false,
                Optional.of(new ServiceVersionCompleter(getShellEnvironment())));
    }
}
