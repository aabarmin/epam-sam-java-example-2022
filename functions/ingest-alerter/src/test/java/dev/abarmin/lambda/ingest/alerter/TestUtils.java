package dev.abarmin.lambda.ingest.alerter;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.net.URL;
import java.nio.charset.Charset;

/**
 * @author Aleksandr Barmin
 */
public class TestUtils {
  @SneakyThrows
  public static final String readResource(final String resource) {
    final URL resourceUri = TestUtils.class.getResource(resource);
    return IOUtils.toString(resourceUri, Charset.defaultCharset());
  }
}
