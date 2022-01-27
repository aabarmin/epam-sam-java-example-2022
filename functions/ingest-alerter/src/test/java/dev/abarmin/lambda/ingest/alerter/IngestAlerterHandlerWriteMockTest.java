package dev.abarmin.lambda.ingest.alerter;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import dev.abarmin.lambda.ingest.alerter.model.Request;
import dev.abarmin.lambda.ingest.alerter.model.Response;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Aleksandr Barmin
 */
@WireMockTest
public class IngestAlerterHandlerWriteMockTest {
  private IngestAlerterHandler uut;

  @BeforeEach
  void setUp() {
    uut = new IngestAlerterHandler();
  }

  @AfterAll
  static void afterAll() {
    System.clearProperty("EURLEX_ENDPOINT");
  }

  @Test
  @DisplayName("Respond using real data and check if parsing is correct")
  void handle_respondWithRealDataFromMockCheckParsingIsCorrect(WireMockRuntimeInfo wireMock) {
    stubFor(get(urlPathEqualTo("/webapi/notification/ingestion"))
        .withQueryParam("startDate", matching("\\d{4}-\\d{2}-\\d{2}"))
        .withQueryParam("endDate", matching("\\d{4}-\\d{2}-\\d{2}"))
        .withQueryParam("type", equalTo("CREATE"))
        .withQueryParam("wemiClasses", equalTo("work"))
        .withHeader("Accept", equalTo("*/*"))
        .withHeader("Accept-Language", equalTo("en_EN"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/xml;charset=UTF-8")
            .withBody(TestUtils.readResource("/eurlex_alerts.xml"))
        )
    );

    final String eurlexMockEndpoint = wireMock.getHttpBaseUrl();
    System.setProperty("EURLEX_ENDPOINT", eurlexMockEndpoint);

    final Response response = uut.handleRequest(Request.builder()
        .startDate("2020-10-20")
        .endDate("2020-10-21")
        .build(), null);

    assertThat(response).isNotNull();
    assertThat(response.getItems()).hasSize(91);
  }
}
