package monolithic.config.route;

import com.typesafe.config.Config;

import monolithic.common.route.BaseRoute;
import monolithic.config.service.ConfigService;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * The base class for config routes, provides easy access to a {@link ConfigService} object.
 */
public abstract class BaseConfigRoute extends BaseRoute {
    @Nonnull
    private final ConfigService configService;

    /**
     * @param config the system configuration properties
     * @param configService the {@link ConfigService} used to manage the dynamic system configuration properties
     */
    public BaseConfigRoute(@Nonnull final Config config, @Nonnull final ConfigService configService) {
        super(config);
        this.configService = Objects.requireNonNull(configService);
    }

    /**
     * @return the {@link ConfigService} responsible for processing configuration requests
     */
    @Nonnull
    protected ConfigService getConfigService() {
        return this.configService;
    }
}
