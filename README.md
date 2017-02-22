# Introduction

This repository provides support for encrypting data that is written to Kafka. You specify this as a 
serializer/deserializer which will wrap any existing serializer/deserializer that you already use.

# Configuration

## CryptoSerializer

| Name                    | Description                                                                                                                                                                                                                     | Type     | Default  | Valid Values | Importance |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|----------|--------------|------------|
| serializer.class        | The standard kafka serializer that will be used to write the unencrypted payload. Any serializer setting can be passed by prefixing `serializer.` to the settings passed to this serializer.                                    | class    |          |              | high       |
| vault.address           | The url to connect to vault with. This will use the `VAULT_ADDR` environment variable if not set. See [Initializing a Driver Instance](https://github.com/BetterCloud/vault-java-driver#initializing-a-driver-instance)         | string   |          |              | high       |
| kafka.path              | The path under the storage engine to store kafka specific secrets.                                                                                                                                                              | string   | kafka    |              | high       |
| vault.backend           | The Vault [secret backend](https://www.vaultproject.io/docs/secrets/) to store the secrets in.                                                                                                                                  | string   | secret   |              | high       |
| vault.token             | The token to authenticate to vault with. This will use the `VAULT_TOKEN` environment variable if not set. See [Initializing a Driver Instance](https://github.com/BetterCloud/vault-java-driver#initializing-a-driver-instance) | password | [hidden] |              | high       |
| vault.cache.interval.ms | The number of milliseconds to cache the encryption ciphers.                                                                                                                                                                     | long     | 300000   |              | low        |
| vault.retries           | The number of retries if there is an issue connecting to vault.                                                                                                                                                                 | int      | 3        |              | low        |
| vault.retry.interval.ms | The amount of time to delay between errors when interacting with vault.                                                                                                                                                         | int      | 1000     |              | low        |

## CryptoDeserializer

| Name                    | Description                                                                                                                                                                                                                     | Type     | Default  | Valid Values | Importance |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|----------|--------------|------------|
| deserializer.class      | The standard kafka deserializer that will be used to read the unencrypted payload. Any deserializer setting can be passed by prefixing `deserializer.` to the settings passed to this deserializer.                             | class    |          |              | high       |
| vault.address           | The url to connect to vault with. This will use the `VAULT_ADDR` environment variable if not set. See [Initializing a Driver Instance](https://github.com/BetterCloud/vault-java-driver#initializing-a-driver-instance)         | string   |          |              | high       |
| kafka.path              | The path under the storage engine to store kafka specific secrets.                                                                                                                                                              | string   | kafka    |              | high       |
| vault.backend           | The Vault [secret backend](https://www.vaultproject.io/docs/secrets/) to store the secrets in.                                                                                                                                  | string   | secret   |              | high       |
| vault.token             | The token to authenticate to vault with. This will use the `VAULT_TOKEN` environment variable if not set. See [Initializing a Driver Instance](https://github.com/BetterCloud/vault-java-driver#initializing-a-driver-instance) | password | [hidden] |              | high       |
| vault.cache.interval.ms | The number of milliseconds to cache the encryption ciphers.                                                                                                                                                                     | long     | 300000   |              | low        |
| vault.retries           | The number of retries if there is an issue connecting to vault.                                                                                                                                                                 | int      | 3        |              | low        |
| vault.retry.interval.ms | The amount of time to delay between errors when interacting with vault.                                                                                                                                                         | int      | 1000     |              | low        |

# Troubleshooting

## Missing JCE Policy files

```bash
java.security.NoSuchAlgorithmException: Cannot find any provider supporting AES/CBC/PKCS5PADDING
```

Download and install the [JCE Policy](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html) 
files.

