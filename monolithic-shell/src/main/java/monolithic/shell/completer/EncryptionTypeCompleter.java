package monolithic.shell.completer;

import jline.console.completer.EnumCompleter;
import monolithic.crypto.EncryptionType;

/**
 * Responsible for performing tab-completions for the {@link EncryptionType} enumeration values.
 */
public class EncryptionTypeCompleter extends EnumCompleter {
    /**
     * Default constructor.
     */
    public EncryptionTypeCompleter() {
        super(EncryptionType.class);
    }
}
