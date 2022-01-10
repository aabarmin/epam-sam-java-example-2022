package dev.abarmin.lambda.ingest.alerter;

import dev.abarmin.lambda.ingest.alerter.model.Request;
import dev.abarmin.lambda.ingest.alerter.model.Response;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Aleksandr Barmin
 */
class IngestAlerterHandlerTest {
  private IngestAlerterHandler uut;

  @BeforeEach
  void setUp() {
    uut = new IngestAlerterHandler();
  }

  @Test
  void handle_checkRequestIsSentAndResponseReceived() {
    final Request request = new Request();
    request.setStartDate("2020-10-20");
    request.setEndDate("2020-10-21");

    final Collection<Response> response = uut.handleRequest(request, null);

    assertThat(response)
        .isNotNull();
  }
}