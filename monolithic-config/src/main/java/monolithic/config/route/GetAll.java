package monolithic.config.route;

import com.google.common.net.MediaType;
import com.typesafe.config.Config;

import monolithic.config.service.ConfigService;
import spark.Request;
import spark.Response;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

/**
 * Retrieve all of the dynamic system configuration properties managed in this system.
 */
public class GetAll extends BaseConfigRoute {
    /**
     * @param config the system configuration properties
     * @param configService the {@link ConfigService} used to manage the dynamic system configuration properties
     */
    public GetAll(@Nonnull final Config config, @Nonnull final ConfigService configService) {
        super(config, configService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Object handle(@Nonnull final Request request, @Nonnull final Response response)
            throws ExecutionException, InterruptedException, TimeoutException {
        response.status(HttpServletResponse.SC_OK);
        response.type(MediaType.JSON_UTF_8.type());

        return getConfigService().getAll().get(10, TimeUnit.SECONDS).toJson();
    }
}
