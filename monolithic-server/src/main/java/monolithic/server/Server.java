package monolithic.server;

import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import monolithic.common.config.ConfigKeys;
import monolithic.common.model.service.Reservation;
import monolithic.crypto.CryptoFactory;
import monolithic.crypto.EncryptionException;
import monolithic.curator.CuratorCreator;
import monolithic.discovery.DiscoveryException;
import monolithic.discovery.DiscoveryManager;
import monolithic.discovery.model.Service;
import monolithic.server.filter.RequestLoggingFilter;
import monolithic.server.filter.RequestSigningFilter;
import monolithic.server.port.PortManager;
import monolithic.server.port.PortReservationException;
import monolithic.server.route.ServiceControlRoute;
import monolithic.server.route.ServiceInfoRoute;
import monolithic.server.route.ServiceMemoryRoute;
import spark.Spark;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * The main class used to run the server.
 */
public class Server {
    private final static Logger LOG = LoggerFactory.getLogger(Server.class);

    @Nonnull
    private final Config config;
    @Nonnull
    private final Optional<CountDownLatch> serverStopLatch;

    @Nonnull
    private final ExecutorService executor;
    @Nonnull
    private final CuratorFramework curator;
    @Nonnull
    private final DiscoveryManager discoveryManager;
    @Nonnull
    private final CryptoFactory cryptoFactory;

    @Nonnull
    private Optional<Service> service;
    private boolean shouldRestart = false;

    /**
     * This constructor initializes the system configuration, then configures the REST end-points and starts the
     * micro service
     *
     * @param config the static system configuration information
     * @param serverStopLatch the {@link CountDownLatch} used to manage the running server process
     * @throws Exception if there is a problem during service initialization
     */
    public Server(@Nonnull final Config config, @Nonnull final CountDownLatch serverStopLatch) throws Exception {
        this.config = Objects.requireNonNull(config);
        this.serverStopLatch = Optional.of(Objects.requireNonNull(serverStopLatch));

        this.cryptoFactory = createCryptoFactory(this.config);
        this.executor = createExecutor(config);
        this.curator = CuratorCreator.create(config, cryptoFactory);
        this.discoveryManager = createDiscoveryManager(this.config, this.curator);

        this.service = Optional.empty();

        start();
    }

    /**
     * This constructor initializes the system configuration, then configures the REST end-points and starts the
     * micro service
     *
     * @param config the static system configuration information
     * @param executor the {@link ExecutorService} used to process asynchronous tasks
     * @param curator the {@link CuratorFramework} used to perform communication with zookeeper
     * @param discoveryManager the {@link DiscoveryManager} used to manage available services
     * @param cryptoFactory the {@link CryptoFactory} used to manage encryption and decryption operations
     * @throws Exception if there is a problem during service initialization
     */
    @VisibleForTesting
    public Server(
            @Nonnull final Config config, @Nonnull final ExecutorService executor,
            @Nonnull final CuratorFramework curator, @Nonnull final DiscoveryManager discoveryManager,
            @Nonnull final CryptoFactory cryptoFactory) throws Exception {
        this.config = Objects.requireNonNull(config);
        this.serverStopLatch = Optional.empty();
        this.executor = Objects.requireNonNull(executor);
        this.curator = Objects.requireNonNull(curator);
        this.discoveryManager = Objects.requireNonNull(discoveryManager);
        this.cryptoFactory = Objects.requireNonNull(cryptoFactory);

        this.service = Optional.empty();

        start();
    }

    /**
     * @return the static system configuration information
     */
    @Nonnull
    public Config getConfig() {
        return this.config;
    }

    /**
     * @return the {@link ExecutorService} used to perform asynchronous task processing
     */
    @Nonnull
    public ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * @return the {@link CuratorFramework} used to perform communication with zookeeper
     */
    @Nonnull
    public CuratorFramework getCurator() {
        return this.curator;
    }

    /**
     * @return the {@link DiscoveryManager} used to manage available services
     */
    @Nonnull
    public DiscoveryManager getDiscoveryManager() {
        return this.discoveryManager;
    }

    /**
     * @return the {@link CryptoFactory} used to manage encryption and decryption operations
     */
    @Nonnull
    public CryptoFactory getCryptoFactory() {
        return this.cryptoFactory;
    }

    /**
     * @return the {@link CountDownLatch} tracking the running server process
     */
    @Nonnull
    public Optional<CountDownLatch> getServerStopLatch() {
        return this.serverStopLatch;
    }

    /**
     * @return the {@link Service} describing this running service, possibly not present if not running
     */
    @Nonnull
    public Optional<Service> getService() {
        return this.service;
    }

    /**
     * @return whether the service should be restarted
     */
    public boolean getShouldRestart() {
        return this.shouldRestart;
    }

    /**
     * @param shouldRestart whether the service should be restarted
     */
    public void setShouldRestart(final boolean shouldRestart) {
        this.shouldRestart = shouldRestart;
    }

    @Nonnull
    protected String getHostName() {
        return getConfig().getString(ConfigKeys.SERVER_HOSTNAME.getKey());
    }

    @Nonnull
    protected DiscoveryManager createDiscoveryManager(
            @Nonnull final Config config, @Nonnull final CuratorFramework curator) throws DiscoveryException {
        return new DiscoveryManager(Objects.requireNonNull(config), Objects.requireNonNull(curator));
    }

    @Nonnull
    protected ExecutorService createExecutor(@Nonnull final Config config) {
        return Executors.newFixedThreadPool(config.getInt(ConfigKeys.EXECUTOR_THREADS.getKey()));
    }

    @Nonnull
    protected CryptoFactory createCryptoFactory(@Nonnull final Config config) throws DiscoveryException {
        return new CryptoFactory(Objects.requireNonNull(config));
    }

    protected void configurePort(@Nonnull final Reservation reservation) {
        Spark.port(reservation.getPort());
    }

    protected void configureThreading() {
        final int maxThreads = getConfig().getInt(ConfigKeys.SERVER_THREADS_MAX.getKey());
        final int minThreads = getConfig().getInt(ConfigKeys.SERVER_THREADS_MIN.getKey());
        final long timeout = getConfig().getDuration(ConfigKeys.SERVER_TIMEOUT.getKey(), TimeUnit.MILLISECONDS);

        Spark.threadPool(maxThreads, minThreads, (int) timeout);
    }

    protected void configureSecurity() throws EncryptionException {
        final boolean ssl = getConfig().getBoolean(ConfigKeys.SSL_ENABLED.getKey());
        if (ssl) {
            final String keystoreFile = getConfig().getString(ConfigKeys.SSL_KEYSTORE_FILE.getKey());
            final String keystorePass =
                    getCryptoFactory().getDecryptedConfig(ConfigKeys.SSL_KEYSTORE_PASSWORD.getKey());
            final String truststoreFile = getConfig().getString(ConfigKeys.SSL_TRUSTSTORE_FILE.getKey());
            final String truststorePass =
                    getCryptoFactory().getDecryptedConfig(ConfigKeys.SSL_TRUSTSTORE_PASSWORD.getKey());
            Spark.secure(keystoreFile, keystorePass, truststoreFile, truststorePass);
        }
    }

    protected void configureRequestLogger() {
        Spark.before(new RequestLoggingFilter());
    }

    protected void configureRequestSigner(@Nonnull final Config config, @Nonnull final CryptoFactory cryptoFactory) {
        Spark.before(new RequestSigningFilter(config, cryptoFactory));
    }

    protected void configureRoutes() {
        Spark.get("/service/info", new ServiceInfoRoute(getConfig()));
        Spark.get("/service/memory", new ServiceMemoryRoute(getConfig()));
        Spark.get("/service/control/:action", new ServiceControlRoute(this));
    }

    protected Reservation getPortReservation() throws PortReservationException {
        try (final PortManager portManager = new PortManager(getConfig(), getCurator())) {
            return portManager.getReservation(getHostName());
        }
    }

    /**
     * Start the service.
     * @throws PortReservationException if there is a problem reserving the port for the service
     * @throws EncryptionException if there is a problem decrypting the SSL key store or trust store passwords
     */
    public void start() throws PortReservationException, EncryptionException {
        // Configure the service.
        final Reservation reservation = getPortReservation();

        configurePort(reservation);
        configureThreading();
        configureSecurity();
        configureRequestLogger();
        configureRequestSigner(getConfig(), getCryptoFactory());
        configureRoutes();

        final boolean ssl = getConfig().getBoolean(ConfigKeys.SSL_ENABLED.getKey());
        final String name = getConfig().getString(ConfigKeys.SYSTEM_NAME.getKey());
        final String version = getConfig().getString(ConfigKeys.SYSTEM_VERSION.getKey());
        this.service = Optional.of(new Service(name, version, reservation.getHost(), reservation.getPort(), ssl));

        // Register with service discovery once the server has started.
        this.executor.submit(() -> {
            Spark.awaitInitialization();
            LOG.info("Service {} started on {}:{}", name, reservation.getHost(), reservation.getPort());

            try {
                if (getService().isPresent()) {
                    getDiscoveryManager().register(getService().get());
                }
            } catch (final DiscoveryException registerFailed) {
                LOG.error("Failed to register with service discovery", registerFailed);
                stop();
            }
        });
    }

    /**
     * Stop the service.
     */
    public void stop() {
        try {
            if (getService().isPresent()) {
                getDiscoveryManager().unregister(getService().get());
                this.service = Optional.empty();
            }
        } catch (final DiscoveryException unregisterFailed) {
            // Not really an issue because the ephemeral registration will disappear automatically soon.
            LOG.warn("Failed to unregister with service discovery", unregisterFailed);
        }

        getDiscoveryManager().close();
        getCurator().close();
        getExecutor().shutdown();
        Spark.stop();

        if (getServerStopLatch().isPresent()) {
            getServerStopLatch().get().countDown();
        }
    }

    /**
     * @param args the command-line parameters
     * @throws Exception if there is a problem during service initialization
     */
    public static void main(@Nonnull final String... args) throws Exception {
        boolean restart;
        do {
            final Config config = ConfigFactory.load().withFallback(ConfigFactory.systemProperties())
                    .withFallback(ConfigFactory.systemEnvironment());
            final CountDownLatch serverStopLatch = new CountDownLatch(1);
            final Server server = new Server(config, serverStopLatch);
            serverStopLatch.await();

            restart = server.getShouldRestart();
        } while (restart);
    }
}
