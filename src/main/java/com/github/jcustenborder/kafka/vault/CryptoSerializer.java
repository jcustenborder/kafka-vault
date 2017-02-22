package com.github.jcustenborder.kafka.vault;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class CryptoSerializer<T> implements Serializer<T> {
  private static final Logger log = LoggerFactory.getLogger(CryptoSerializer.class);
  Config config;
  Serializer<T> serializer;
  CipherManager cipherManager;

  @Override
  public void configure(Map<String, ?> settings, boolean isKey) {
    this.config = new Config(settings);
    this.cipherManager = new CipherManager(this.config);

    try {
      this.serializer = (Serializer<T>) this.config.serializerClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }

    this.serializer.configure(this.config.serializerSettings, isKey);
  }

  @Override
  public byte[] serialize(String topic, T value) {
    final CipherManager.CipherState state = this.cipherManager.get(topic);
    byte[] unencrypted = this.serializer.serialize(topic, value);
    byte[] encrypted;

    try {
      encrypted = state.encryptCipher.doFinal(unencrypted);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new IllegalStateException("Exception thrown while encrypting payload.", e);
    }

    ByteBuffer buffer = ByteBuffer.allocate(encrypted.length + 13);
    buffer.put(KafkaVaultConfig.MAGIC_BYTE);
    buffer.putLong(state.version);
    buffer.putInt(encrypted.length);
    buffer.put(encrypted);
    byte[] result = buffer.array();
    log.trace("serialize() - returning {} bytes.", result.length);
    return result;
  }

  @Override
  public void close() {

  }

  public static class Config extends KafkaVaultConfig {

    public final Map<String, Object> serializerSettings;
    public final Class<?> serializerClass;

    public Config(Map<String, ?> parsedConfig) {
      super(config(), parsedConfig);

      Map<String, Object> prefixedOriginals = this.originalsWithPrefix("serializer.");
      Map<String, Object> serializerSettings = new HashMap<>();
      serializerSettings.putAll(prefixedOriginals);
      serializerSettings.remove(SERIALIZER_CLASS_CONF);
      this.serializerSettings = serializerSettings;
      this.serializerClass = this.getClass(SERIALIZER_CLASS_CONF);
    }

    public static final String SERIALIZER_CLASS_CONF = "serializer.class";
    static final String SERIALIZER_CLASS_DOC = "The standard kafka serializer that will be used to write the unencrypted " +
        "payload. Any serializer setting can be passed by prefixing `serializer.` to the settings passed to this serializer.";

    public static ConfigDef config() {
      return KafkaVaultConfig.config()
          .define(SERIALIZER_CLASS_CONF, ConfigDef.Type.CLASS, ConfigDef.Importance.HIGH, SERIALIZER_CLASS_DOC);
    }
  }
}
