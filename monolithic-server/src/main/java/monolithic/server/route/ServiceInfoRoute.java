package monolithic.server.route;

import com.google.common.net.MediaType;
import com.typesafe.config.Config;

import monolithic.common.config.ConfigKeys;
import monolithic.common.model.service.ServiceInfo;
import monolithic.common.route.BaseRoute;
import spark.Request;
import spark.Response;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

/**
 * A base route that provides some information about the service like the name and version.
 */
public class ServiceInfoRoute extends BaseRoute {
    /**
     * @param config the static system configuration information
     */
    public ServiceInfoRoute(@Nonnull final Config config) {
        super(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Object handle(@Nonnull final Request request, @Nonnull final Response response) {
        response.status(HttpServletResponse.SC_OK);
        response.type(MediaType.JSON_UTF_8.type());

        final String systemName = getConfig().getString(ConfigKeys.SYSTEM_NAME.getKey());
        final String systemVersion = getConfig().getString(ConfigKeys.SYSTEM_VERSION.getKey());

        return new ServiceInfo(systemName, systemVersion).toJson();
    }
}
