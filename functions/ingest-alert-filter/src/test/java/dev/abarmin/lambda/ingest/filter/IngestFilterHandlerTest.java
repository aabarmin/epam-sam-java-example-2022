package dev.abarmin.lambda.ingest.filter;

import dev.abarmin.lambda.ingest.filter.model.Request;
import dev.abarmin.lambda.ingest.filter.model.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Aleksandr Barmin
 */
class IngestFilterHandlerTest {
  private static final String EURLEX_DOCUMENTS = "eurlex_documents";
  private static final String DYNAMODB_ENDPOINT = getDynamodbEndpoint();

  private IngestFilterHandler uut;
  private DynamoDbClient testClient;

  private static String getDynamodbEndpoint() {
    final String template = "http://%s:%s";
    return String.format(template,
        System.getProperty("DYNAMODB_HOST", "localhost"),
        System.getProperty("DYNAMODB_PORT", "8000")
    );
  }

  @BeforeAll
  static void beforeAll() {
    System.setProperty("ENDPOINT_OVERRIDE", DYNAMODB_ENDPOINT);
  }

  @BeforeEach
  void setUp() throws Exception {
    testClient = DynamoDbClient.builder()
        .endpointOverride(new URI(DYNAMODB_ENDPOINT))
        .build();

    final Optional<String> tableExists = testClient.listTables().tableNames()
        .stream()
        .filter(name -> StringUtils.equalsIgnoreCase(name, EURLEX_DOCUMENTS))
        .findFirst();
    if (tableExists.isEmpty()) {
      final CreateTableResponse response = testClient.createTable(CreateTableRequest.builder()
          .tableName(EURLEX_DOCUMENTS)
          .keySchema(KeySchemaElement.builder()
              .attributeName("cellarId")
              .keyType(KeyType.HASH)
              .build())
          .attributeDefinitions(
              AttributeDefinition.builder()
                  .attributeName("cellarId")
                  .attributeType(ScalarAttributeType.S)
                  .build())
          .provisionedThroughput(ProvisionedThroughput.builder()
              .readCapacityUnits(5L)
              .writeCapacityUnits(5L)
              .build())
          .build());
    }

    uut = new IngestFilterHandler();
  }

  @Test
  void handle_whenItemIsNotInDynamoDb() {
    final String cellarId = "valid-cellar-id";
    deleteItem(cellarId);

    final Request request = new Request();
    request.setCellarId(cellarId);

    final Response response = uut.handleRequest(request, null);

    assertThat(response)
        .isNotNull()
        .extracting("cellarId", "exists")
        .containsExactly(cellarId, false);
  }

  @Test
  void handle_whenItemIsItDynamoDB() {
    final String cellarId = "valid-cellar-id";
    deleteItem(cellarId);
    createItem(Map.of(
        "cellarId",
        cellarId
    ));

    final Request request = new Request();
    request.setCellarId(cellarId);

    final Response response = uut.handleRequest(request, null);

    assertThat(response)
        .isNotNull()
        .extracting("cellarId", "exists")
        .containsExactly(cellarId, true);
  }

  private void deleteItem(final String key) {
    testClient.deleteItem(DeleteItemRequest.builder()
        .tableName(EURLEX_DOCUMENTS)
        .key(Map.of(
            "cellarId",
            AttributeValue.builder().s(key).build()
        ))
        .build());
  }

  private void createItem(final Map<String, String> item) {
    final Map<String, AttributeValue> record = new HashMap<>();
    for (Map.Entry<String, String> entry : item.entrySet()) {
      record.put(
          entry.getKey(),
          AttributeValue.builder().s(entry.getValue()).build()
      );
    }

    testClient.putItem(PutItemRequest.builder()
        .tableName(EURLEX_DOCUMENTS)
        .item(record)
        .build());
  }
}