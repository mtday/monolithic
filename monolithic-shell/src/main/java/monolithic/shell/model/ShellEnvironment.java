package monolithic.shell.model;

import com.typesafe.config.Config;

import org.apache.curator.framework.CuratorFramework;

import monolithic.config.client.ConfigClient;
import monolithic.crypto.CryptoFactory;
import monolithic.discovery.DiscoveryManager;
import monolithic.server.client.ServerClient;
import monolithic.shell.RegistrationManager;
import okhttp3.OkHttpClient;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

/**
 * Provides shell environmental configuration and utilities for use within commands.
 */
public class ShellEnvironment {
    @Nonnull
    private final Config config;
    @Nonnull
    private final ExecutorService executor;
    @Nonnull
    private final DiscoveryManager discoveryManager;
    @Nonnull
    private final CuratorFramework curatorFramework;
    @Nonnull
    private final RegistrationManager registrationManager;
    @Nonnull
    private final OkHttpClient httpClient;
    @Nonnull
    private final CryptoFactory cryptoFactory;

    /**
     * @param config the static system configuration information
     * @param executor the {@link ExecutorService} used to perform asynchronous task processing
     * @param discoveryManager the service discovery manager used to find and manage available micro services
     * @param curatorFramework the curator framework used to manage interactions with zookeeper
     * @param registrationManager the manager used to track the available command registrations
     * @param httpClient the {@link OkHttpClient} used to make REST calls to other services
     * @param cryptoFactory the {@link CryptoFactory} used to perform encryption operations
     */
    public ShellEnvironment(
            @Nonnull final Config config, @Nonnull final ExecutorService executor,
            @Nonnull final DiscoveryManager discoveryManager, @Nonnull final CuratorFramework curatorFramework,
            @Nonnull final RegistrationManager registrationManager, @Nonnull final OkHttpClient httpClient,
            @Nonnull final CryptoFactory cryptoFactory) {
        this.config = Objects.requireNonNull(config);
        this.executor = Objects.requireNonNull(executor);
        this.discoveryManager = Objects.requireNonNull(discoveryManager);
        this.curatorFramework = Objects.requireNonNull(curatorFramework);
        this.registrationManager = Objects.requireNonNull(registrationManager);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.cryptoFactory = Objects.requireNonNull(cryptoFactory);
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
     * @return the service discovery manager used to find and manage available micro services
     */
    @Nonnull
    public DiscoveryManager getDiscoveryManager() {
        return this.discoveryManager;
    }

    /**
     * @return the curator framework used to manage interactions with zookeeper
     */
    @Nonnull
    public CuratorFramework getCuratorFramework() {
        return this.curatorFramework;
    }

    /**
     * @return the manager used to track the available command registrations
     */
    @Nonnull
    public RegistrationManager getRegistrationManager() {
        return this.registrationManager;
    }

    /**
     * @return the {@link OkHttpClient} used to make REST calls to other services
     */
    @Nonnull
    public OkHttpClient getHttpClient() {
        return this.httpClient;
    }

    /**
     * @return the {@link CryptoFactory} used to perform encryption operations
     */
    @Nonnull
    public CryptoFactory getCryptoFactory() {
        return this.cryptoFactory;
    }

    /**
     * @return a {@link ServerClient} used to make remote service calls to other services
     */
    @Nonnull
    public ServerClient getServerClient() {
        return new ServerClient(getConfig(), getExecutor(), getHttpClient(), getCryptoFactory());
    }

    /**
     * @return a {@link ConfigClient} used to make remote calls to the dynamic configuration services
     */
    @Nonnull
    public ConfigClient getConfigClient() {
        return new ConfigClient(getConfig(), getExecutor(), getDiscoveryManager(), getHttpClient(), getCryptoFactory());
    }

    /**
     * Close down the resources associated with this environment.
     */
    public void close() {
        getExecutor().shutdown();
        getCuratorFramework().close();
        getDiscoveryManager().close();
    }
}
