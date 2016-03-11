package monolithic.server.route;

import com.google.common.net.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import monolithic.common.model.service.ServiceControlStatus;
import monolithic.common.route.BaseRoute;
import monolithic.server.Server;
import spark.Request;
import spark.Response;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

/**
 * A base route that provides some REST end-points for controlling the service.
 */
public class ServiceControlRoute extends BaseRoute {
    private final static Logger LOG = LoggerFactory.getLogger(ServiceControlRoute.class);

    @Nonnull
    private final Server server;

    /**
     * @param server the {@link Server} providing access to the main server object that will be controlled
     */
    public ServiceControlRoute(@Nonnull final Server server) {
        super(Objects.requireNonNull(server).getConfig());
        this.server = server;
    }

    /**
     * @return the {@link Server} object to be managed
     */
    @Nonnull
    protected Server getServer() {
        return this.server;
    }

    /**
     * Perform the delay, allowing time to return the response back to the caller.
     * @throws InterruptedException if the sleep operation is interrupted
     */
    protected void delayBeforeAction() throws InterruptedException {
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    }

    /**
     * Perform a stop, possibly followed by a start (if the {@code restart} parameter is true).
     *
     * @param restart whether the server should be restarted
     */
    protected void stop(final boolean restart) {
        getServer().getExecutor().submit(() -> {
            try {
                // Wait a little to allow for the response to make it back to the caller.
                LOG.info("Scheduling server {}", restart ? "restart" : "shutdown");
                delayBeforeAction();
                getServer().setShouldRestart(restart);
                getServer().stop();
            } catch (final InterruptedException interrupted) {
                // Ignored.
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Object handle(@Nonnull final Request request, @Nonnull final Response response) {
        final String action = request.params("action");

        if (StringUtils.isEmpty(action)) {
            response.status(HttpServletResponse.SC_BAD_REQUEST);
            return "A control action must be specified";
        } else {
            response.status(HttpServletResponse.SC_OK);
            response.type(MediaType.JSON_UTF_8.type());

            if ("stop".equalsIgnoreCase(action)) {
                stop(false);
            } else if ("restart".equalsIgnoreCase(action)) {
                stop(true);
            } else {
                response.status(HttpServletResponse.SC_BAD_REQUEST);
                return "Unrecognized control action: " + action;
            }

            return new ServiceControlStatus(true, action).toJson();
        }
    }
}
