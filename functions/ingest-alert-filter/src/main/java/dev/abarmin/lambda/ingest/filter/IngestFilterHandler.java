package dev.abarmin.lambda.ingest.filter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.abarmin.lambda.ingest.filter.model.Request;
import dev.abarmin.lambda.ingest.filter.model.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

/**
 * @author Aleksandr Barmin
 */
public class IngestFilterHandler implements RequestHandler<Request, Response> {
  @Override
  @SneakyThrows
  public Response handleRequest(Request input, Context context) {
    final DynamoDbClient dynamoDbClient = createDynamoClient();

    final GetItemRequest request = GetItemRequest.builder()
        .tableName(getTableName())
        .key(Map.of(
            "cellarId",
            getStringValue(input)
        ))
        .build();

    final GetItemResponse response = dynamoDbClient.getItem(request);

    return new Response(
        input.getCellarId(),
        response.hasItem()
    );
  }

  private DynamoDbClient createDynamoClient() throws URISyntaxException {
    if (StringUtils.isNoneEmpty(System.getProperty("ENDPOINT_OVERRIDE"))) {
      final String endpoint = System.getProperty("ENDPOINT_OVERRIDE");
      return DynamoDbClient.builder()
          .region(Region.AWS_GLOBAL)
          .endpointOverride(new URI(endpoint))
          .build();
    }

    return DynamoDbClient.create();
  }

  private AttributeValue getStringValue(Request input) {
    return AttributeValue.builder().s(input.getCellarId()).build();
  }

  private String getTableName() {
    return System.getProperty("INGEST_DYNAMODB_TABLE_NAME", "eurlex_documents");
  }
}
