package monolithic.common.route;

import com.typesafe.config.Config;

import spark.Route;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * The base class for routes in this system.
 */
public abstract class BaseRoute implements Route {
    protected final static Object NO_CONTENT = "";

    @Nonnull
    private final Config config;

    /**
     * @param config the static system configuration information
     */
    public BaseRoute(@Nonnull final Config config) {
        this.config = Objects.requireNonNull(config);
    }

    /**
     * @return the static system configuration information
     */
    @Nonnull
    public Config getConfig() {
        return this.config;
    }

    /**
     * @return the object to be returned when no content is to be sent back to the client
     */
    @Nonnull
    public Object getNoContent() {
        return NO_CONTENT;
    }
}
