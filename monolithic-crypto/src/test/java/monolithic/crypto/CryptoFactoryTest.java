package monolithic.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static monolithic.common.config.ConfigKeys.SHARED_SECRET_VARIABLE;
import static monolithic.common.config.ConfigKeys.SSL_ENABLED;
import static monolithic.common.config.ConfigKeys.SSL_KEYSTORE_FILE;
import static monolithic.common.config.ConfigKeys.SSL_KEYSTORE_PASSWORD;
import static monolithic.common.config.ConfigKeys.SSL_KEYSTORE_TYPE;
import static monolithic.common.config.ConfigKeys.SSL_TRUSTSTORE_FILE;
import static monolithic.common.config.ConfigKeys.SSL_TRUSTSTORE_PASSWORD;
import static monolithic.common.config.ConfigKeys.SSL_TRUSTSTORE_TYPE;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

import org.junit.Test;
import org.mockito.Mockito;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Perform testing on the {@link CryptoFactory} class.
 */
public class CryptoFactoryTest {
    @Test
    public void testPBEFromSystemEnvironment() throws EncryptionException {
        final Map<String, ConfigValue> map = new HashMap<>();
        map.put(SHARED_SECRET_VARIABLE.getKey(), ConfigValueFactory.fromAnyRef("USER"));
        final Config config = ConfigFactory.parseMap(map).withFallback(ConfigFactory.systemProperties())
                .withFallback(ConfigFactory.systemEnvironment());
        final CryptoFactory crypto = new CryptoFactory(config);

        final PasswordBasedEncryption pbe = crypto.getPasswordBasedEncryption();
        assertNotNull(pbe);

        final String original = "original data";
        final String encrypted = pbe.encryptString(original, StandardCharsets.UTF_8);
        final String decrypted = pbe.decryptString(encrypted, StandardCharsets.UTF_8);
        assertEquals(original, decrypted);
    }

    @Test
    public void testPBEFromSystemProperties() throws EncryptionException {
        System.setProperty("SHARED_SECRET", "secret");
        final Map<String, ConfigValue> map = new HashMap<>();
        map.put(SHARED_SECRET_VARIABLE.getKey(), ConfigValueFactory.fromAnyRef("SHARED_SECRET"));
        final Config config = ConfigFactory.parseMap(map).withFallback(ConfigFactory.systemProperties())
                .withFallback(ConfigFactory.systemEnvironment());
        final CryptoFactory crypto = new CryptoFactory(config);

        final PasswordBasedEncryption pbe = crypto.getPasswordBasedEncryption();
        assertNotNull(pbe);

        final String original = "original data";
        final String encrypted = pbe.encryptString(original, StandardCharsets.UTF_8);
        final String decrypted = pbe.decryptString(encrypted, StandardCharsets.UTF_8);
        assertEquals(original, decrypted);
    }

    @Test(expected = EncryptionException.class)
    public void testPBENoSharedSecret() throws EncryptionException {
        final Map<String, ConfigValue> map = new HashMap<>();
        map.put(SHARED_SECRET_VARIABLE.getKey(), ConfigValueFactory.fromAnyRef("DOES_NOT_EXIST"));
        final Config config = ConfigFactory.parseMap(map);
        final CryptoFactory crypto = new CryptoFactory(config);

        crypto.getPasswordBasedEncryption();
    }

    @Test(expected = EncryptionException.class)
    public void testSKEWithSslDisabled() throws EncryptionException {
        final Map<String, ConfigValue> map = new HashMap<>();
        map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("false"));
        final Config config = ConfigFactory.parseMap(map);
        final CryptoFactory crypto = new CryptoFactory(config);

        crypto.getSymmetricKeyEncryption();
    }

    @Test
    public void testSKEValidParams() throws EncryptionException {
        final Optional<URL> url = Optional.ofNullable(getClass().getClassLoader().getResource("keystore.jks"));
        if (url.isPresent()) {
            final Map<String, ConfigValue> map = new HashMap<>();
            map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("true"));
            map.put(SSL_KEYSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef(url.get().getFile()));
            map.put(SSL_KEYSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("JKS"));
            map.put(SSL_KEYSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
            final Config config = ConfigFactory.parseMap(map);
            final CryptoFactory crypto = new CryptoFactory(config);

            final SymmetricKeyEncryption ske = crypto.getSymmetricKeyEncryption();
            assertNotNull(ske);
        }
    }

    @Test(expected = EncryptionException.class)
    public void testSKEWrongType() throws EncryptionException {
        final Optional<URL> url = Optional.ofNullable(getClass().getClassLoader().getResource("keystore.jks"));
        if (url.isPresent()) {
            final Map<String, ConfigValue> map = new HashMap<>();
            map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("true"));
            map.put(SSL_KEYSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef(url.get().getFile()));
            map.put(SSL_KEYSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("WRONG"));
            map.put(SSL_KEYSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
            final Config config = ConfigFactory.parseMap(map);
            final CryptoFactory crypto = new CryptoFactory(config);

            final SymmetricKeyEncryption ske = crypto.getSymmetricKeyEncryption();
            assertNotNull(ske);
        }
    }

    @Test(expected = EncryptionException.class)
    public void testSKEMissingKeyStoreFile() throws EncryptionException {
        final Map<String, ConfigValue> map = new HashMap<>();
        map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("true"));
        map.put(SSL_KEYSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef("/tmp/missing-file.jks"));
        map.put(SSL_KEYSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("JKS"));
        map.put(SSL_KEYSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
        final Config config = ConfigFactory.parseMap(map);
        final CryptoFactory crypto = new CryptoFactory(config);

        final SymmetricKeyEncryption ske = crypto.getSymmetricKeyEncryption();
        assertNotNull(ske);
    }

    @Test(expected = EncryptionException.class)
    public void testSKEInvalidKeyStoreFile() throws EncryptionException {
        final Optional<URL> url = Optional.ofNullable(getClass().getClassLoader().getResource("localhost.crt"));
        if (url.isPresent()) {
            final Map<String, ConfigValue> map = new HashMap<>();
            map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("true"));
            map.put(SSL_KEYSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef(url.get().getFile()));
            map.put(SSL_KEYSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("JKS"));
            map.put(SSL_KEYSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
            final Config config = ConfigFactory.parseMap(map);
            final CryptoFactory crypto = new CryptoFactory(config);

            final SymmetricKeyEncryption ske = crypto.getSymmetricKeyEncryption();
            assertNotNull(ske);
        }
    }

    @Test(expected = EncryptionException.class)
    public void testSKEMultipleKeyStoreFile() throws EncryptionException {
        final Optional<URL> url = Optional.ofNullable(getClass().getClassLoader().getResource("multiple.jks"));
        if (url.isPresent()) {
            final Map<String, ConfigValue> map = new HashMap<>();
            map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("true"));
            map.put(SSL_KEYSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef(url.get().getFile()));
            map.put(SSL_KEYSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("JKS"));
            map.put(SSL_KEYSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
            final Config config = ConfigFactory.parseMap(map);
            final CryptoFactory crypto = new CryptoFactory(config);

            final SymmetricKeyEncryption ske = crypto.getSymmetricKeyEncryption();
            assertNotNull(ske);
        }
    }

    @Test(expected = EncryptionException.class)
    public void testSKEImportKeyStoreFile() throws EncryptionException {
        final Optional<URL> url = Optional.ofNullable(getClass().getClassLoader().getResource("truststore.jks"));
        if (url.isPresent()) {
            final Map<String, ConfigValue> map = new HashMap<>();
            map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("true"));
            map.put(SSL_KEYSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef(url.get().getFile()));
            map.put(SSL_KEYSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("JKS"));
            map.put(SSL_KEYSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
            final Config config = ConfigFactory.parseMap(map);
            final CryptoFactory crypto = new CryptoFactory(config);

            final SymmetricKeyEncryption ske = crypto.getSymmetricKeyEncryption();
            assertNotNull(ske);
        }
    }

    @Test
    public void testGetTrustStore() throws EncryptionException {
        final Optional<URL> url = Optional.ofNullable(getClass().getClassLoader().getResource("keystore.jks"));
        if (url.isPresent()) {
            final Map<String, ConfigValue> map = new HashMap<>();
            map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("true"));
            map.put(SSL_TRUSTSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef(url.get().getFile()));
            map.put(SSL_TRUSTSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("JKS"));
            map.put(SSL_TRUSTSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
            final Config config = ConfigFactory.parseMap(map);
            final CryptoFactory crypto = new CryptoFactory(config);

            assertNotNull(crypto.getTrustStore());
        }
    }

    @Test
    public void testGetSSLContext() throws EncryptionException {
        final Optional<URL> url = Optional.ofNullable(getClass().getClassLoader().getResource("keystore.jks"));
        if (url.isPresent()) {
            final Map<String, ConfigValue> map = new HashMap<>();
            map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("true"));
            map.put(SSL_KEYSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef(url.get().getFile()));
            map.put(SSL_KEYSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("JKS"));
            map.put(SSL_KEYSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
            map.put(SSL_TRUSTSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef(url.get().getFile()));
            map.put(SSL_TRUSTSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("JKS"));
            map.put(SSL_TRUSTSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
            final Config config = ConfigFactory.parseMap(map);
            final CryptoFactory crypto = new CryptoFactory(config);

            assertNotNull(crypto.getSSLContext());
        }
    }

    @Test
    public void testGetSSLContextSSLDisabled() throws EncryptionException {
        final Map<String, ConfigValue> map = new HashMap<>();
        map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("false"));
        final Config config = ConfigFactory.parseMap(map);
        final CryptoFactory crypto = new CryptoFactory(config);

        assertNotNull(crypto.getSSLContext());
    }

    @Test(expected = EncryptionException.class)
    public void testGetSSLContextWrongType() throws EncryptionException {
        final Optional<URL> url = Optional.ofNullable(getClass().getClassLoader().getResource("keystore.jks"));
        if (url.isPresent()) {
            final Map<String, ConfigValue> map = new HashMap<>();
            map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("true"));
            map.put(SSL_KEYSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef(url.get().getFile()));
            map.put(SSL_KEYSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("WRONG"));
            map.put(SSL_KEYSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
            map.put(SSL_TRUSTSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef(url.get().getFile()));
            map.put(SSL_TRUSTSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("WRONG"));
            map.put(SSL_TRUSTSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
            final Config config = ConfigFactory.parseMap(map);
            final CryptoFactory crypto = new CryptoFactory(config);

            crypto.getSSLContext();
        }
    }

    @Test(expected = EncryptionException.class)
    public void testGetSSLContextException() throws EncryptionException {
        final CryptoFactory crypto = Mockito.mock(CryptoFactory.class);
        Mockito.when(crypto.getSSLContext()).thenCallRealMethod();
        Mockito.when(crypto.getConfig()).thenThrow(new RuntimeException("Fake"));

        crypto.getSSLContext();
    }

    @Test
    public void testGetDecryptedConfigPBE() throws EncryptionException {
        final Map<String, ConfigValue> map = new HashMap<>();
        map.put("SHARED_SECRET", ConfigValueFactory.fromAnyRef("secret"));
        map.put(SHARED_SECRET_VARIABLE.getKey(), ConfigValueFactory.fromAnyRef("SHARED_SECRET"));
        final String encrypted = "PBE{" + new CryptoFactory(ConfigFactory.parseMap(map)).getPasswordBasedEncryption()
                .encryptString("hello", StandardCharsets.UTF_8) + "}";
        map.put("key", ConfigValueFactory.fromAnyRef(encrypted));

        final Config config = ConfigFactory.parseMap(map).withFallback(ConfigFactory.systemProperties())
                .withFallback(ConfigFactory.systemEnvironment());
        final CryptoFactory crypto = new CryptoFactory(config);
        assertEquals("hello", crypto.getDecryptedConfig("key"));
    }

    @Test
    public void testGetDecryptedConfigSKE() throws EncryptionException {
        final Optional<URL> url = Optional.ofNullable(getClass().getClassLoader().getResource("keystore.jks"));
        if (url.isPresent()) {
            final Map<String, ConfigValue> map = new HashMap<>();
            map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("true"));
            map.put(SSL_KEYSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef(url.get().getFile()));
            map.put(SSL_KEYSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("JKS"));
            map.put(SSL_KEYSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
            final String encrypted = "SKE{" + new CryptoFactory(ConfigFactory.parseMap(map)).getSymmetricKeyEncryption()
                    .encryptString("hello", StandardCharsets.UTF_8) + "}";
            map.put("key", ConfigValueFactory.fromAnyRef(encrypted));

            final CryptoFactory crypto = new CryptoFactory(ConfigFactory.parseMap(map));
            assertEquals("hello", crypto.getDecryptedConfig("key"));
        }
    }

    @Test
    public void testGetDecryptedConfigNotEncrypted() throws EncryptionException {
        final Map<String, ConfigValue> map = new HashMap<>();
        map.put("key", ConfigValueFactory.fromAnyRef("hello"));
        final CryptoFactory crypto = new CryptoFactory(ConfigFactory.parseMap(map));
        assertEquals("hello", crypto.getDecryptedConfig("key"));
    }

    @Test(expected = EncryptionException.class)
    public void testGetDecryptedConfigException() throws EncryptionException {
        final Map<String, ConfigValue> map = new HashMap<>();
        map.put(SSL_ENABLED.getKey(), ConfigValueFactory.fromAnyRef("true"));
        map.put(SSL_KEYSTORE_FILE.getKey(), ConfigValueFactory.fromAnyRef("/tmp/does-not-exist.jks"));
        map.put(SSL_KEYSTORE_TYPE.getKey(), ConfigValueFactory.fromAnyRef("JKS"));
        map.put(SSL_KEYSTORE_PASSWORD.getKey(), ConfigValueFactory.fromAnyRef("changeit"));
        map.put("key", ConfigValueFactory.fromAnyRef("SKE{012}"));

        final CryptoFactory crypto = new CryptoFactory(ConfigFactory.parseMap(map));
        assertEquals("hello", crypto.getDecryptedConfig("key"));
    }
}
