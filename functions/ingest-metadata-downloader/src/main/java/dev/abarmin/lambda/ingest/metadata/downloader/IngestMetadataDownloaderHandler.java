package dev.abarmin.lambda.ingest.metadata.downloader;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.abarmin.lambda.ingest.metadata.downloader.model.Request;
import dev.abarmin.lambda.ingest.metadata.downloader.model.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * @author Aleksandr Barmin
 */
@Slf4j
public class IngestMetadataDownloaderHandler implements RequestHandler<Request, Response> {
  private static final String DOWNLOAD_URL_TEMPLATE =
      "https://eur-lex.europa.eu/download-notice.html?legalContentId=cellar:${cellar_id}&noticeType=branch&callingUrl=&lng=EN";

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private Path temporaryDirectory;

  @Override
  public Response handleRequest(Request request, Context context) {
    final Path downloadPath = downloadNotice(request);
    uploadNotice(downloadPath, request);
    createRecord(request);

    return Response.of(request);
  }

  private void createRecord(Request request) {
    final DynamoDbClient dbClient = DynamoDbClient.builder()
        .httpClient(ApacheHttpClient.create())
        .build();

    final UpdateItemRequest updateRequest = UpdateItemRequest.builder()
        .tableName(getTableName())
        .key(Map.of(
            "cellarId",
            AttributeValue.builder().s(request.getCellarId()).build()
        ))
        .attributeUpdates(Map.of(
            "created",
            AttributeValueUpdate.builder()
                .value(AttributeValue.builder()
                    .s(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .build())
                .build()
        ))
        .build();

    final UpdateItemResponse updateResponse = dbClient.updateItem(updateRequest);
  }

  private String getTableName() {
    return System.getenv("INGEST_DYNAMODB_TABLE_NAME");
  }

  private String getBucketName() {
    return System.getenv("INGEST_S3_BUCKET_NAME");
  }

  private void uploadNotice(Path downloadPath, Request request) {
    final S3Client s3Client = S3Client.builder()
        .httpClient(ApacheHttpClient.create())
        .build();

    final String objectKey = String.format(
        "notice_%s.xml", request.getCellarId()
    );
    final PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(getBucketName())
        .key(objectKey)
        .build();

    log.info("Uploading to bucket {}", getBucketName());
    log.info("Object key is {}", objectKey);

    final PutObjectResponse putResponse = s3Client.putObject(putRequest, downloadPath);
  }

  @SneakyThrows
  private Path downloadNotice(final Request request) {
    final String downloadUrl = DOWNLOAD_URL_TEMPLATE.replace("${cellar_id}", request.getCellarId());
    final HttpRequest downloadRequest = HttpRequest.newBuilder()
        .uri(new URI(downloadUrl))
        .header("Accept", "*/*")
        .GET()
        .build();
    final HttpResponse.BodyHandler<Path> bodyHandler =
        HttpResponse.BodyHandlers.ofFile(getFileName(request));
    final HttpResponse<Path> downloadResponse = httpClient.send(downloadRequest, bodyHandler);
    if (downloadResponse.statusCode() == 200) {
      final Path downloadedNoticePath = downloadResponse.body();

      log.info("Notice downloaded to {}", downloadedNoticePath);
      log.info("Notice size is {}", downloadedNoticePath.toFile().length());

      return downloadedNoticePath;
    }
    throw new RuntimeException(String.format(
        "Can't download notice due to the error, %s",
        Files.readAllBytes(downloadResponse.body())
    ));
  }

  private Path getFileName(Request request) {
    return getTempDirectory().resolve(String.format(
        "%s.xml",
        request.getCellarId()
    ));
  }

  @SneakyThrows
  private Path getTempDirectory() {
    if (this.temporaryDirectory == null) {
      this.temporaryDirectory = Files.createTempDirectory("downloader");
    }
    return this.temporaryDirectory;
  }
}
