package monolithic.security.route;

import com.google.common.net.MediaType;
import com.typesafe.config.Config;

import org.apache.commons.lang3.StringUtils;

import monolithic.security.model.User;
import monolithic.security.service.UserService;
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
 * Retrieves the {@link User} for the provided user id.
 */
public class GetById extends BaseUserRoute {
    /**
     * @param config the static system configuration properties
     * @param userService the {@link UserService} used to manage the system user accounts
     */
    public GetById(@Nonnull final Config config, @Nonnull final UserService userService) {
        super(config, userService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Object handle(@Nonnull final Request request, @Nonnull final Response response)
            throws ExecutionException, InterruptedException, TimeoutException {
        final String id = request.params("id");

        if (StringUtils.isEmpty(id)) {
            response.status(HttpServletResponse.SC_BAD_REQUEST);
            return "Invalid request, a user id must be provided";
        } else {
            final Future<Optional<User>> future = getUserService().getById(id);
            final Optional<User> value = future.get(10, TimeUnit.SECONDS);

            if (value.isPresent()) {
                response.status(HttpServletResponse.SC_OK);
                response.type(MediaType.JSON_UTF_8.type());
                return value.get().toJson();
            } else {
                response.status(HttpServletResponse.SC_NOT_FOUND);
                return String.format("A user with the specified id was not found: %s", id);
            }
        }
    }
}
