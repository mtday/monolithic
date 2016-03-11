package monolithic.config.route;

import com.google.common.net.MediaType;
import com.typesafe.config.Config;

import org.apache.commons.lang3.StringUtils;

import monolithic.config.model.ConfigKeyValue;
import monolithic.config.service.ConfigService;
import spark.Request;
import spark.Response;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

/**
 * Retrieves the configuration value for the provided key.
 */
public class Get extends BaseConfigRoute {
    /**
     * @param config the system configuration properties
     * @param configService the {@link ConfigService} used to manage the dynamic system configuration properties
     */
    public Get(@Nonnull final Config config, @Nonnull final ConfigService configService) {
        super(config, configService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Object handle(@Nonnull final Request request, @Nonnull final Response response)
            throws ExecutionException, InterruptedException, TimeoutException {
        final String key = request.params("key");

        if (StringUtils.isEmpty(key)) {
            response.status(HttpServletResponse.SC_BAD_REQUEST);
            return "Invalid configuration key";
        } else {
            final Future<Optional<ConfigKeyValue>> future = getConfigService().get(key);
            final Optional<ConfigKeyValue> value = future.get(10, TimeUnit.SECONDS);

            if (value.isPresent()) {
                response.status(HttpServletResponse.SC_OK);
                response.type(MediaType.JSON_UTF_8.type());
                return value.get().toJson();
            } else {
                response.status(HttpServletResponse.SC_NOT_FOUND);
                return String.format("Configuration key not found: %s", key);
            }
        }
    }
}
