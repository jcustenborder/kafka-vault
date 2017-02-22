package com.github.jcustenborder.kafka.vault;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class CryptoDeserializer<T> implements Deserializer<T> {
  private static final Logger log = LoggerFactory.getLogger(CryptoDeserializer.class);
  Config config;
  CipherManager cipherManager;
  Deserializer<T> deserializer;

  @Override
  public void configure(Map<String, ?> settings, boolean isKey) {
    this.config = new Config(settings);
    this.cipherManager = new CipherManager(this.config);
    try {
      this.deserializer = (Deserializer<T>) this.config.serializerClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }

    this.deserializer.configure(this.config.serializerSettings, isKey);
  }

  @Override
  public T deserialize(String topic, byte[] bytes) {
    log.trace("deserialize('{}') - {} bytes.",topic, bytes.length);

    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    byte magicByte = buffer.get();
    if(KafkaVaultConfig.MAGIC_BYTE != magicByte) {
      throw new IllegalStateException("Message does not start with magic byte.");
    }
    long version = buffer.getLong();
    int length = buffer.getInt();
    log.trace("deserialize('{}') - version = {}, length = {}.",topic, version, length);
    CipherManager.CipherState state = this.cipherManager.get(topic, version);

    byte[] unencrypted;
    try {
      byte[] encrypted = new byte[length];
      buffer.get(encrypted);
      unencrypted = state.decryptCipher.doFinal(encrypted);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new IllegalStateException(e);
    }

    return this.deserializer.deserialize(topic, unencrypted);
  }

  @Override
  public void close() {

  }

  public static class Config extends KafkaVaultConfig {

    public final Map<String, Object> serializerSettings;
    public final Class<?> serializerClass;

    public Config(Map<String, ?> parsedConfig) {
      super(config(), parsedConfig);

      Map<String, Object> prefixedOriginals = this.originalsWithPrefix("deserializer.");
      Map<String, Object> serializerSettings = new HashMap<>();
      serializerSettings.putAll(prefixedOriginals);
      serializerSettings.remove(DESERIALIZER_CLASS_CONF);
      this.serializerSettings = serializerSettings;
      this.serializerClass = this.getClass(DESERIALIZER_CLASS_CONF);
    }

    public static final String DESERIALIZER_CLASS_CONF = "deserializer.class";
    static final String DESERIALIZER_CLASS_DOC = "The standard kafka deserializer that will be used to read the unencrypted " +
        "payload. Any deserializer setting can be passed by prefixing `deserializer.` to the settings passed to this deserializer.";

    public static ConfigDef config() {
      return KafkaVaultConfig.config()
          .define(DESERIALIZER_CLASS_CONF, ConfigDef.Type.CLASS, ConfigDef.Importance.HIGH, DESERIALIZER_CLASS_DOC);
    }
  }
}
