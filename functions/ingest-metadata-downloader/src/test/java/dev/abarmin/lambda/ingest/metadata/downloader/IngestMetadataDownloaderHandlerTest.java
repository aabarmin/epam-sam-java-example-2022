package dev.abarmin.lambda.ingest.metadata.downloader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Aleksandr Barmin
 */
class IngestMetadataDownloaderHandlerTest {
  private static final String DOWNLOAD_URL_TEMPLATE =
      "https://eur-lex.europa.eu/download-notice.html?legalContentId=cellar:${cellar_id}&noticeType=branch&callingUrl=&lng=EN";

  private final HttpClient httpClient = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.ALWAYS)
      .build();

  @Test
  void checkNoticeLoads() throws Exception {
    final String downloadUrl = DOWNLOAD_URL_TEMPLATE.replace(
        "${cellar_id}", "4ed0b222-129c-11eb-9a54-01aa75ed71a1");

    final HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(downloadUrl))
        .header("Accept", "*/*")
        .GET()
        .build();

    final HttpResponse<Path> loaded = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(
        Files.createTempFile("notice_", ".xml")
    ));

    assertThat(loaded).isNotNull();
  }
}