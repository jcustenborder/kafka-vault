package com.github.jcustenborder.kafka.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import com.google.common.base.MoreObjects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import org.apache.kafka.common.utils.SystemTime;
import org.apache.kafka.common.utils.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


class CipherManager {
  private static final Logger log = LoggerFactory.getLogger(CipherManager.class);
  final KafkaVaultConfig config;
  final Vault vault;
  final Cache<CacheKey, CipherState> stateCache;
  final Time time;

  CipherManager(KafkaVaultConfig config) {
    this(config, new SystemTime());
  }

  CipherManager(KafkaVaultConfig config, Time time) {
    this.config = config;
    this.time = time;
    this.vault = new Vault(this.config.vaultConfig());

    this.stateCache = CacheBuilder.newBuilder()
        .expireAfterWrite(this.config.vaultCacheInterval, TimeUnit.MILLISECONDS)
        .build();
  }

  public void setRandomKey(String topic) {
    Long keyVersion = this.time.milliseconds();
    Path versionedPath = secretPath(topic, keyVersion);
    Path currentPath = secretCurrentPath(topic);



    byte[] key;
    try {
      KeyGenerator kgen = KeyGenerator.getInstance("AES");
      kgen.init(256);
      key = kgen.generateKey().getEncoded();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }

    byte[] iv = new byte[16];
    SecureRandom random = new SecureRandom();
    random.nextBytes(iv);

    Map<String, String> settings = ImmutableMap.of(
        SecretConfig.CIPHER_CONF, SecretConfig.CIPHER_DEFAULT,
        SecretConfig.KEY_CONF, BaseEncoding.base64().encode(key),
        SecretConfig.IV_CONF, BaseEncoding.base64().encode(iv)
    );

    try {
      this.vault.withRetries(this.config.vaultMaxRetries, this.config.vaultMaxRetries)
          .logical().write(versionedPath.toString(), settings);
    } catch (VaultException e) {
      throw new IllegalStateException(e);
    }

    try {
      this.vault.withRetries(this.config.vaultMaxRetries, this.config.vaultMaxRetries)
          .logical().write(
          currentPath.toString(),
          ImmutableMap.of(SecretConfig.VERSION_KEY, keyVersion.toString())
      );
    } catch (VaultException e) {
      throw new IllegalStateException(e);
    }
  }


  private CipherState getInternal(final CacheKey key) {
    log.trace("getInternal({})", key);

    try {
      return this.stateCache.get(key, new Callable<CipherState>() {
        @Override
        public CipherState call() throws Exception {
          log.trace("load('{}') - Retrieving secret from vault '{}'.", key, key.path);
          LogicalResponse response = vault
              .withRetries(config.vaultMaxRetries, config.vaultRetryInterval)
              .logical().read(key.path.toString());

          long actualVersion;
          if (response.getData().containsKey(SecretConfig.VERSION_KEY)) {
            String version = response.getData().get(SecretConfig.VERSION_KEY);
            CacheKey versionCacheKey = key(key.topic, Long.parseLong(version));
            log.trace("load('{}') - Data contains '{}' key, retrieving '{}' instead.", key, SecretConfig.VERSION_KEY, versionCacheKey.path);
            response = vault
                .withRetries(config.vaultMaxRetries, config.vaultRetryInterval)
                .logical().read(versionCacheKey.path.toString());
            actualVersion = Long.parseLong(versionCacheKey.version);
          } else {
            actualVersion = Long.parseLong(key.version);
          }

          Cipher encryptCipher = createCipher(response.getData(), Cipher.ENCRYPT_MODE);
          Cipher decryptCipher = createCipher(response.getData(), Cipher.DECRYPT_MODE);

          return new CipherState(key.topic, encryptCipher, decryptCipher, actualVersion);
        }
      });
    } catch (ExecutionException e) {
      throw new IllegalStateException("Exception thrown while configuring ciphers for " + key, e);
    }
  }

  private Path secretPath(String topic, Long version) {
    return secretPath(topic, version.toString());
  }

  private Path secretPath(String topic, String version) {
    return Paths.get(this.config.vaultBackend, this.config.kafkaPath, topic, version);
  }

  private Path secretCurrentPath(String topic) {
    return secretPath(topic, "current");
  }

  public CipherState get(String topic) {
    log.trace("get('{}')", topic);
    CacheKey key = key(topic, null);
    return getInternal(key);
  }

  public CipherState get(String topic, Long version) {
    log.trace("get('{}', {})", topic, version);
    CacheKey key = key(topic, version);
    return getInternal(key);
  }

  Cipher createCipher(Map<String, String> data, int mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
    SecretConfig secretConfig = new SecretConfig(data);
    Cipher cipher = Cipher.getInstance(secretConfig.cipher);
    final String algorithm = cipher.getParameters().getAlgorithm();
    log.trace("createCipher() - creating SecretKeySpec for algorithm='{}' mode={}", algorithm, mode);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(secretConfig.iv);
    SecretKeySpec keySpec = new SecretKeySpec(secretConfig.key, algorithm);
    cipher.init(mode, keySpec, ivParameterSpec);
    return cipher;
  }

  CacheKey key(String topic, Long version) {
    Path path = secretPath(topic, null == version ? "current" : version.toString());
    return new CacheKey(topic, null == version ? "current" : version.toString(), path);
  }

  static class CacheKey implements Comparable<CacheKey> {
    public final String topic;
    public final String version;
    public final Path path;


    private CacheKey(String topic, String version, Path path) {
      this.topic = topic;
      this.version = version;
      this.path = path;
    }

    @Override
    public int compareTo(CacheKey that) {
      return ComparisonChain.start()
          .compare(this.topic, that.topic)
          .compare(this.version, that.version)
          .compare(this.path, that.path)
          .result();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof CacheKey) {
        CacheKey that = (CacheKey) obj;
        return 0 == compareTo(that);
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(CacheKey.class)
          .add("topic", this.topic)
          .add("version", this.version)
          .add("path", this.path)
          .toString();
    }
  }

  public static class CipherState {
    public final String topic;
    public final Cipher encryptCipher;
    public final Cipher decryptCipher;
    public final long version;


    public CipherState(String topic, Cipher encryptCipher, Cipher decryptCipher, long version) {
      this.topic = topic;
      this.encryptCipher = encryptCipher;
      this.decryptCipher = decryptCipher;
      this.version = version;
    }
  }
}
