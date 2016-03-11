package monolithic.common.config;

import javax.annotation.Nonnull;

/**
 * Defines the static configuration keys expected to exist in the system configuration.
 */
public enum ConfigKeys {
    SYSTEM_NAME,
    SYSTEM_VERSION,

    SSL_ENABLED,
    SSL_KEYSTORE_FILE,
    SSL_KEYSTORE_TYPE,
    SSL_KEYSTORE_PASSWORD,
    SSL_TRUSTSTORE_FILE,
    SSL_TRUSTSTORE_TYPE,
    SSL_TRUSTSTORE_PASSWORD,

    SERVER_THREADS_MAX,
    SERVER_THREADS_MIN,
    SERVER_TIMEOUT,
    SERVER_HOSTNAME,
    SERVER_PORT_MIN,
    SERVER_PORT_MAX,

    ZOOKEEPER_HOSTS,
    ZOOKEEPER_AUTH_ENABLED,
    ZOOKEEPER_AUTH_USER,
    ZOOKEEPER_AUTH_PASSWORD,

    EXECUTOR_THREADS,

    SHELL_HISTORY_FILE,

    SHARED_SECRET_VARIABLE;

    /**
     * @return the key to use when retrieving the common configuration value from the system configuration file
     */
    @Nonnull
    public String getKey() {
        return name().toLowerCase().replaceAll("_", ".");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public String toString() {
        return getKey();
    }
}
