package dev.abarmin.lambda.ingest.alerter;

import dev.abarmin.lambda.ingest.alerter.model.Request;
import dev.abarmin.lambda.ingest.alerter.model.Response;
import dev.abarmin.lambda.ingest.alerter.model.ResponseLine;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @author Aleksandr Barmin
 */
@RequiredArgsConstructor
public class IngestAlerter {
  private static final String URI_TEMPLATE =
      "https://publications.europa.eu/webapi/notification/ingestion?" +
      "startDate=${startDate}&" +
      "endDate=${endDate}&" +
      "type=CREATE&" +
      "wemiClasses=work";

  private final HttpClient httpClient;
  private final DocumentBuilder documentBuilder;
  private final XPathExpression extractExpression;

  @SneakyThrows
  public Response process(final Request input) {
    final HttpRequest request = HttpRequest.newBuilder(buildUri(input))
        .header("Accept-Language", "en_EN")
        .header("Accept", "*/*")
        .GET()
        .build();

    final HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    final byte[] responseBody = response.body();

    return processResponse(responseBody);
  }

  private Response processResponse(final byte[] response) throws Exception {
    final Document document = documentBuilder.parse(new InputSource(new ByteArrayInputStream(response)));
    final NodeList cellarNodes = (NodeList) extractExpression.evaluate(document, XPathConstants.NODESET);

    final List<ResponseLine> lines = IntStream.range(0, cellarNodes.getLength())
        .mapToObj(index -> cellarNodes.item(index))
        .map(Node::getTextContent)
        .map(content -> StringUtils.substringAfter(content, "cellar:"))
        .map(ResponseLine::new)
        .collect(Collectors.toList());

    return new Response(lines);
  }

  @SneakyThrows
  private URI buildUri(Request input) {
    final String uri = URI_TEMPLATE
        .replace("${startDate}", input.getStartDate())
        .replace("${endDate}", input.getEndDate());

    return new URI(uri);
  }
}
