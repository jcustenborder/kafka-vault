package com.github.jcustenborder.kafka.vault;

import com.github.jcustenborder.kafka.connect.utils.config.MarkdownFormatter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Documentationtest {
  private static final Logger log = LoggerFactory.getLogger(Documentationtest.class);
  @Test
  public void serializerDoc() {
    log.info("\n" +
        MarkdownFormatter.toMarkdown(CryptoSerializer.Config.config())
    );
  }

  @Test
  public void deserializerDoc() {
    log.info("\n" +
        MarkdownFormatter.toMarkdown(CryptoDeserializer.Config.config())
    );
  }
}
