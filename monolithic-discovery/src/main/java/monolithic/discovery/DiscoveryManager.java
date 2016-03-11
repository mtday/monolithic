package monolithic.discovery;

import com.typesafe.config.Config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;

import monolithic.common.config.ConfigKeys;
import monolithic.discovery.model.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;

/**
 * The class is used to manage the system service discovery activity and is responsible for registering and
 * unregistering services, and retrieving available services.
 */
public class DiscoveryManager {
    @Nonnull
    private final Config config;
    @Nonnull
    private final ServiceDiscovery<String> discovery;

    private boolean isClosed = false;

    /**
     * @param config the static system configuration information
     * @param curator the {@link CuratorFramework} that is managing zookeeper operations
     * @throws DiscoveryException if there is a problem starting the service discovery service
     */
    public DiscoveryManager(@Nonnull final Config config, @Nonnull final CuratorFramework curator)
            throws DiscoveryException {
        this.config = Objects.requireNonNull(config);
        this.discovery = ServiceDiscoveryBuilder.builder(String.class).client(curator).basePath("/discovery").build();
        try {
            this.discovery.start();
        } catch (final Exception exception) {
            throw new DiscoveryException("Failed to start service discovery", exception);
        }
    }

    /**
     * @return the static system configuration information
     */
    @Nonnull
    protected Config getConfig() {
        return this.config;
    }

    /**
     * @return the internal curator {@link ServiceDiscovery} object that manages the interaction with zookeeper
     */
    @Nonnull
    protected ServiceDiscovery<String> getDiscovery() {
        return this.discovery;
    }

    /**
     * @return whether this manager has been closed or not
     */
    public boolean isClosed() {
        return this.isClosed;
    }

    /**
     * Shutdown the resources associated with this service discovery manager.
     */
    public void close() {
        try {
            this.isClosed = true;
            getDiscovery().close();
        } catch (final IOException ignored) {
            // Ignored.
        }
    }

    /**
     * @param service the {@link Service} to register
     * @throws DiscoveryException if there is a problem registering the service
     */
    public void register(@Nonnull final Service service) throws DiscoveryException {
        final ServiceInstance<String> serviceInstance = Objects.requireNonNull(service).asServiceInstance();
        if (!isClosed()) {
            try {
                getDiscovery().registerService(serviceInstance);
            } catch (final Exception exception) {
                throw new DiscoveryException("Failed to register service " + service, exception);
            }
        }
    }

    /**
     * @param service the {@link Service} to unregister
     * @throws DiscoveryException if there is a problem unregistering the service
     */
    public void unregister(@Nonnull final Service service) throws DiscoveryException {
        final ServiceInstance<String> serviceInstance = service.asServiceInstance();
        if (!isClosed()) {
            try {
                getDiscovery().unregisterService(serviceInstance);
            } catch (final Exception exception) {
                throw new DiscoveryException("Failed to unregister service " + service, exception);
            }
        }
    }

    /**
     * @return all of the available {@link Service} objects of the specified type that have registered with service
     * discovery
     * @throws DiscoveryException if there is a problem retrieving the discoverable services of the specified type
     */
    @Nonnull
    public SortedSet<Service> getAll() throws DiscoveryException {
        final SortedSet<Service> services = new TreeSet<>();
        if (!isClosed()) {
            try {
                final String systemName = getConfig().getString(ConfigKeys.SYSTEM_NAME.getKey());
                final List<ServiceInstance<String>> list =
                        new ArrayList<>(getDiscovery().queryForInstances(systemName));
                list.stream().map(Service::new).forEach(services::add);
            } catch (final Exception exception) {
                throw new DiscoveryException("Failed to retrieve services", exception);
            }
        }
        return services;
    }

    /**
     * @return a randomly chosen {@link Service} of the specified type that has registered with service discovery,
     * possibly empty if there are no registered services of the specified type
     * @throws DiscoveryException if there is a problem retrieving a random discoverable service of the specified type
     */
    @Nonnull
    public Optional<Service> getRandom() throws DiscoveryException {
        if (!isClosed()) {
            try {
                final String systemName = getConfig().getString(ConfigKeys.SYSTEM_NAME.getKey());
                final List<ServiceInstance<String>> services =
                        new ArrayList<>(getDiscovery().queryForInstances(Objects.requireNonNull(systemName)));
                if (!services.isEmpty()) {
                    // Return a random service instance from the list.
                    return Optional.of(new Service(services.get(new Random().nextInt(services.size()))));
                }
            } catch (final Exception exception) {
                throw new DiscoveryException("Failed to retrieve random service", exception);
            }
        }
        return Optional.empty();
    }
}
