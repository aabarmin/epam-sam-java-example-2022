package dev.abarmin.lambda.ingest.alerter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.abarmin.lambda.ingest.alerter.model.Request;
import dev.abarmin.lambda.ingest.alerter.model.Response;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @author Aleksandr Barmin
 */
public class IngestAlerterHandler implements RequestHandler<Request, Collection<Response>> {
  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
  private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
  private static final DocumentBuilderFactory BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

  @Override
  @SneakyThrows
  public Collection<Response> handleRequest(Request input, Context context) {
    final IngestAlerter alerter = new IngestAlerter(
        HTTP_CLIENT,
        BUILDER_FACTORY.newDocumentBuilder(),
        XPATH_FACTORY.newXPath().compile("//*[local-name(.)=\"cellarId\"]/text()")
    );

    return alerter.process(input);
  }
}
