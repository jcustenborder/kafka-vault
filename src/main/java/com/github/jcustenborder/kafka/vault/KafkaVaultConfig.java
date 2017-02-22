package com.github.jcustenborder.kafka.vault;

import com.bettercloud.vault.VaultConfig;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

public abstract class KafkaVaultConfig extends AbstractConfig {
  static final byte MAGIC_BYTE = 0x069;
  public final String vaultAddress;
  public final String vaultToken;
  public final String vaultBackend;
  public final String kafkaPath;
  public final int vaultMaxRetries;
  public final int vaultRetryInterval;
  public final long vaultCacheInterval;

  public KafkaVaultConfig(ConfigDef config, Map<String, ?> parsedConfig) {
    super(config, parsedConfig);
    this.vaultAddress = this.getString(VAULT_ADDRESS_CONF);
    this.vaultToken = this.getPassword(VAULT_TOKEN_CONF).value();
    this.vaultBackend = this.getString(VAULT_BACKEND_CONF);
    this.kafkaPath = this.getString(KAFKA_SECRET_PATH_CONF);
    this.vaultMaxRetries = this.getInt(VAULT_RETRIES_CONF);
    this.vaultRetryInterval = this.getInt(VAULT_RETRY_INTERVAL_CONF);
    this.vaultCacheInterval = this.getLong(VAULT_CACHE_INTERVAL_MS_CONF);
  }

  public static final String VAULT_ADDRESS_CONF = "vault.address";
  static final String VAULT_ADDRESS_DOC = "The url to connect to vault with. This will use the `VAULT_ADDR` environment " +
      "variable if not set. See [Initializing a Driver Instance](https://github.com/BetterCloud/vault-java-driver#initializing-a-driver-instance)";

  public static final String VAULT_TOKEN_CONF = "vault.token";
  static final String VAULT_TOKEN_DOC = "The token to authenticate to vault with. This will use the `VAULT_TOKEN` " +
      "environment variable if not set. See [Initializing a Driver Instance](https://github.com/BetterCloud/vault-java-driver#initializing-a-driver-instance)";

  public static final String VAULT_BACKEND_CONF = "vault.backend";
  static final String VAULT_BACKEND_DOC = "The Vault [secret backend](https://www.vaultproject.io/docs/secrets/) to store the secrets in.";
  static final String VAULT_BACKEND_DEFAULT = "secret";

  public static final String VAULT_RETRIES_CONF = "vault.retries";
  static final String VAULT_RETRIES_DOC = "The number of retries if there is an issue connecting to vault.";
  static final int VAULT_RETRIES_DEFAULT = 3;

  public static final String VAULT_RETRY_INTERVAL_CONF = "vault.retry.interval.ms";
  static final String VAULT_RETRY_INTERVAL_DOC = "The amount of time to delay between errors when interacting with vault.";
  static final int VAULT_RETRY_INTERVAL_DEFAULT = 1000;

  public static final String KAFKA_SECRET_PATH_CONF = "kafka.path";
  static final String KAFKA_SECRET_PATH_DOC = "The path under the storage engine to store kafka specific secrets.";
  static final String KAFKA_SECRET_PATH_DEFAULT = "kafka";

  public static final String VAULT_CACHE_INTERVAL_MS_CONF = "vault.cache.interval.ms";
  static final String VAULT_CACHE_INTERVAL_MS_DOC = "The number of milliseconds to cache the encryption ciphers.";
  static final long VAULT_CACHE_INTERVAL_MS_DEFAULT = 60 * 1000 * 5;





  public static ConfigDef config() {
    return new ConfigDef()
        .define(VAULT_TOKEN_CONF, ConfigDef.Type.PASSWORD, "", ConfigDef.Importance.HIGH, VAULT_TOKEN_DOC)
        .define(VAULT_ADDRESS_CONF, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, VAULT_ADDRESS_DOC)
        .define(VAULT_BACKEND_CONF, ConfigDef.Type.STRING, VAULT_BACKEND_DEFAULT, ConfigDef.Importance.HIGH, VAULT_BACKEND_DOC)
        .define(KAFKA_SECRET_PATH_CONF, ConfigDef.Type.STRING, KAFKA_SECRET_PATH_DEFAULT, ConfigDef.Importance.HIGH, KAFKA_SECRET_PATH_DOC)
        .define(VAULT_RETRIES_CONF, ConfigDef.Type.INT, VAULT_RETRIES_DEFAULT, ConfigDef.Importance.LOW, VAULT_RETRIES_DOC)
        .define(VAULT_RETRY_INTERVAL_CONF, ConfigDef.Type.INT, VAULT_RETRY_INTERVAL_DEFAULT, ConfigDef.Importance.LOW, VAULT_RETRY_INTERVAL_DOC)
        .define(VAULT_CACHE_INTERVAL_MS_CONF, ConfigDef.Type.LONG, VAULT_CACHE_INTERVAL_MS_DEFAULT, ConfigDef.Importance.LOW, VAULT_CACHE_INTERVAL_MS_DOC);
  }

  public VaultConfig vaultConfig() {
    VaultConfig config = new VaultConfig();

    if (!isNullOrEmpty(this.vaultAddress)) {
      config.address(this.vaultAddress);
    }

    if (!isNullOrEmpty(this.vaultToken)) {
      config.token(this.vaultToken);
    }

    return config;
  }


  private static boolean isNullOrEmpty(String s) {
    return null == s || s.isEmpty();
  }
}
