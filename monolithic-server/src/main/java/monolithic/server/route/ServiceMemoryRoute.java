package monolithic.server.route;

import com.google.common.net.MediaType;
import com.typesafe.config.Config;

import monolithic.common.model.service.ServiceMemory;
import monolithic.common.route.BaseRoute;
import spark.Request;
import spark.Response;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

/**
 * A base route that provides some information about the memory usage and availability in this service.
 */
public class ServiceMemoryRoute extends BaseRoute {
    /**
     * @param config the static system configuration information
     */
    public ServiceMemoryRoute(@Nonnull final Config config) {
        super(config);
    }

    @Nonnull
    protected MemoryUsage getHeapMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }

    @Nonnull
    protected MemoryUsage getNonHeapMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Object handle(@Nonnull final Request request, @Nonnull final Response response) {
        response.status(HttpServletResponse.SC_OK);
        response.type(MediaType.JSON_UTF_8.type());

        return new ServiceMemory(getHeapMemoryUsage(), getNonHeapMemoryUsage()).toJson();
    }
}
