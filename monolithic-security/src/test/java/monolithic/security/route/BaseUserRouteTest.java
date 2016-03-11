package monolithic.security.route;

import static org.junit.Assert.assertEquals;

import com.typesafe.config.Config;

import org.junit.Test;
import org.mockito.Mockito;

import monolithic.security.service.UserService;
import spark.Request;
import spark.Response;

/**
 * Perform testing on the {@link BaseUserRoute} class.
 */
public class BaseUserRouteTest {
    @Test
    public void test() {
        final Config config = Mockito.mock(Config.class);
        final UserService userService = Mockito.mock(UserService.class);

        final monolithic.security.route.BaseUserRoute route = new monolithic.security.route.BaseUserRoute(config, userService) {
            @Override
            public Object handle(final Request request, final Response response) throws Exception {
                return null;
            }
        };

        assertEquals(config, route.getConfig());
        assertEquals(userService, route.getUserService());
    }
}
