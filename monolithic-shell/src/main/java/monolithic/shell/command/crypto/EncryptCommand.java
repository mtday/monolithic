package monolithic.shell.command.crypto;

import org.apache.commons.cli.CommandLine;

import monolithic.crypto.CryptoFactory;
import monolithic.crypto.EncryptionType;
import monolithic.crypto.PasswordBasedEncryption;
import monolithic.crypto.SymmetricKeyEncryption;
import monolithic.shell.model.CommandPath;
import monolithic.shell.model.CommandStatus;
import monolithic.shell.model.Option;
import monolithic.shell.model.Options;
import monolithic.shell.model.Registration;
import monolithic.shell.model.ShellEnvironment;
import monolithic.shell.model.UserCommand;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * This command implements the {@code crypto encrypt} command in the shell.
 */
public class EncryptCommand extends BaseCryptoCommand {
    /**
     * @param shellEnvironment the shell command execution environment
     */
    public EncryptCommand(@Nonnull final ShellEnvironment shellEnvironment) {
        super(shellEnvironment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public List<Registration> getRegistrations() {
        final Option type = getTypeOption("the encryption type to use when encrypting the data");
        final Option input = getInputOption("the user data to encrypt");
        final Optional<Options> encryptOptions = Optional.of(new Options(type, input));

        final Optional<String> description = Optional.of("encrypt the provided input data");
        final CommandPath commandPath = new CommandPath("crypto", "encrypt");
        return Collections.singletonList(new Registration(commandPath, encryptOptions, description));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public CommandStatus process(@Nonnull final UserCommand userCommand, @Nonnull final PrintWriter writer) {
        final CryptoFactory cryptoFactory = getShellEnvironment().getCryptoFactory();

        final CommandLine commandLine = userCommand.getCommandLine().get();
        final EncryptionType encryptionType = EncryptionType.valueOf(commandLine.getOptionValue("t").toUpperCase());
        final String input = commandLine.getOptionValue("i");

        try {
            if (encryptionType == EncryptionType.PASSWORD_BASED) {
                final PasswordBasedEncryption pbe = cryptoFactory.getPasswordBasedEncryption();
                writer.println(pbe.encryptString(input, StandardCharsets.UTF_8));
            } else {
                final SymmetricKeyEncryption ske = cryptoFactory.getSymmetricKeyEncryption();
                writer.println(ske.encryptString(input, StandardCharsets.UTF_8));
            }
        } catch (final Exception exception) {
            writer.println("Failed to encrypt input: " + exception.getMessage());
        }

        return CommandStatus.SUCCESS;
    }
}
