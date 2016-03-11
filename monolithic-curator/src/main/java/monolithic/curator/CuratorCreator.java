package monolithic.curator;

import com.typesafe.config.Config;

import org.apache.curator.framework.AuthInfo;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import monolithic.common.config.ConfigKeys;
import monolithic.crypto.CryptoFactory;
import monolithic.crypto.EncryptionException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

/**
 * Launch the shell.
 */
public class CuratorCreator {
    @Nonnull
    public static CuratorFramework create(
            @Nonnull final Config config, @Nonnull final CryptoFactory cryptoFactory)
            throws TimeoutException, InterruptedException, EncryptionException {
        Objects.requireNonNull(config);
        Objects.requireNonNull(cryptoFactory);

        final String zookeepers = config.getString(ConfigKeys.ZOOKEEPER_HOSTS.getKey());
        final boolean secure = config.getBoolean(ConfigKeys.ZOOKEEPER_AUTH_ENABLED.getKey());
        final String namespace = config.getString(ConfigKeys.SYSTEM_NAME.getKey());
        final CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
        builder.connectString(zookeepers);
        builder.namespace(namespace);
        builder.retryPolicy(new ExponentialBackoffRetry(1000, 3));
        builder.defaultData(new byte[0]);
        if (secure) {
            final String user = config.getString(ConfigKeys.ZOOKEEPER_AUTH_USER.getKey());
            final String pass = cryptoFactory.getDecryptedConfig(ConfigKeys.ZOOKEEPER_AUTH_PASSWORD.getKey());
            final byte[] authData = String.format("%s:%s", user, pass).getBytes(StandardCharsets.UTF_8);
            final AuthInfo authInfo = new AuthInfo("digest", authData);
            builder.authorization(Collections.singletonList(authInfo));
            builder.aclProvider(new CuratorACLProvider());
        }
        final CuratorFramework curator = builder.build();
        curator.start();
        if (!curator.blockUntilConnected(2, TimeUnit.SECONDS)) {
            throw new TimeoutException("Failed to connect to zookeeper");
        }
        return curator;
    }
}
