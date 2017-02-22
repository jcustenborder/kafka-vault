package com.github.jcustenborder.kafka.vault;

import com.google.common.io.BaseEncoding;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.types.Password;

import java.util.Map;

class SecretConfig extends AbstractConfig {
  public final byte[] key;
  public final byte[] iv;
  public final String cipher;
  public final String keyType;

  private byte[] base64Password(String key) {
    Password password = this.getPassword(key);
    return BaseEncoding.base64().decode(password.value());
  }

  public SecretConfig(Map<String, String> originals) {
    super(config(), originals, false);
    this.key = base64Password(KEY_CONF);
    this.iv = base64Password(IV_CONF);
    this.cipher = this.getString(CIPHER_CONF);
    this.keyType = this.getString(KEY_TYPE_CONF);
  }

  public static String KEY_CONF = "key";
  static String KEY_DOC = "";

  public static String KEY_TYPE_CONF = "key.type";
  static String KEY_TYPE_DOC = "";
  static String KEY_TYPE_DEFAULT = "AES";

  public static String IV_CONF = "iv";
  static String IV_DOC = "";


  public static String CIPHER_CONF = "cipher";
  static String CIPHER_DEFAULT = "AES/CBC/PKCS5PADDING";
  static String CIPHER_DOC = "";

  public static String VERSION_KEY = "version";

  public static ConfigDef config() {
    return new ConfigDef()
        .define(KEY_CONF, ConfigDef.Type.PASSWORD, ConfigDef.Importance.HIGH, KEY_DOC)
        .define(IV_CONF, ConfigDef.Type.PASSWORD, ConfigDef.Importance.HIGH, IV_DOC)
        .define(CIPHER_CONF, ConfigDef.Type.STRING, CIPHER_DEFAULT, ConfigDef.Importance.HIGH, CIPHER_DOC)
        .define(KEY_TYPE_CONF, ConfigDef.Type.STRING, KEY_TYPE_DEFAULT, ConfigDef.Importance.HIGH, KEY_TYPE_DOC);
  }
}
