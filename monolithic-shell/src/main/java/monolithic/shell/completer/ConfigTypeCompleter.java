package monolithic.shell.completer;

import jline.console.completer.EnumCompleter;
import monolithic.common.config.ConfigType;

/**
 * Responsible for performing tab-completions for the {@link ConfigType} enumeration values.
 */
public class ConfigTypeCompleter extends EnumCompleter {
    /**
     * Default constructor.
     */
    public ConfigTypeCompleter() {
        super(ConfigType.class);
    }
}
