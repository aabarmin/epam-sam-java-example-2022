package dev.abarmin.lambda.ingest.alerter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.abarmin.lambda.ingest.alerter.model.Request;
import dev.abarmin.lambda.ingest.alerter.model.Response;
import java.net.http.HttpClient;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import lombok.SneakyThrows;

/**
 * @author Aleksandr Barmin
 */
public class IngestAlerterHandler implements RequestHandler<Request, Response> {
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final XPathFactory xPathFactory = XPathFactory.newInstance();
  private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

  @Override
  @SneakyThrows
  public Response handleRequest(Request input, Context context) {
    final IngestAlerter alerter = new IngestAlerter(
        httpClient,
        documentBuilderFactory.newDocumentBuilder(),
        xPathFactory.newXPath().compile("//*[local-name(.)=\"cellarId\"]/text()")
    );

    return alerter.process(input);
  }
}
