package com.github.jcustenborder.kafka.vault;

import com.bettercloud.vault.VaultException;
import com.github.jcustenborder.kafka.connect.utils.config.MarkdownFormatter;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CryptoSerializerTest {
  private static final Logger log = LoggerFactory.getLogger(KafkaVaultConfig.class);
  @ClassRule
  public static DockerComposeRule docker = DockerComposeRule.builder()
      .file("docker-compose.yml")
      .saveLogsTo("target/docker")
      .waitingForService("vault", HealthChecks.toHaveAllPortsOpen())
      .build();

  String vaultUrl;
  final String TOKEN="myroot";

  Map<String, String> serializerSettings;
  Map<String, String> deserializerSettings;

  CryptoSerializer<String> serializer;
  CryptoDeserializer<String> deserializer;

  CipherManager manager;
  final String TOPIC = "testing.topic";

  @Before
  public void before() {
    DockerPort dockerPort = docker.containers().container("vault").port(8200);
    vaultUrl = dockerPort.inFormat("http://$HOST:$EXTERNAL_PORT");
    this.serializerSettings = new LinkedHashMap<>();
    this.serializerSettings.put(CryptoSerializer.Config.VAULT_ADDRESS_CONF, this.vaultUrl);
    this.serializerSettings.put(CryptoSerializer.Config.VAULT_TOKEN_CONF, TOKEN);
    this.serializerSettings.put(CryptoSerializer.Config.SERIALIZER_CLASS_CONF, StringSerializer.class.getName());

    this.serializer = new CryptoSerializer<>();
    this.serializer.configure(this.serializerSettings, true);

    this.deserializerSettings = new LinkedHashMap<>();
    this.deserializerSettings.put(CryptoDeserializer.Config.VAULT_ADDRESS_CONF, this.vaultUrl);
    this.deserializerSettings.put(CryptoDeserializer.Config.VAULT_TOKEN_CONF, TOKEN);
    this.deserializerSettings.put(CryptoDeserializer.Config.DESERIALIZER_CLASS_CONF, StringDeserializer.class.getName());

    this.deserializer = new CryptoDeserializer<>();
    this.deserializer.configure(this.deserializerSettings, true);

    CryptoSerializer.Config config = new CryptoSerializer.Config(this.serializerSettings);
    this.manager = new CipherManager(config);
    this.manager.setRandomKey(TOPIC);
  }

  @Test
  public void roundTrip() throws VaultException, NoSuchAlgorithmException {
    final String expected = "This is a testing string";
    byte[] buffer = this.serializer.serialize(TOPIC, expected);
    String actual = this.deserializer.deserialize(TOPIC, buffer);
    assertEquals(expected, actual);
    this.manager.setRandomKey(TOPIC);
    actual = this.deserializer.deserialize(TOPIC, buffer);
    assertEquals(expected, actual);
  }



  @After
  public void after() {
    docker.after();
  }


}