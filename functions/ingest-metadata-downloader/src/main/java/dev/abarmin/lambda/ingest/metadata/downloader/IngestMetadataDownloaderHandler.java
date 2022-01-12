package dev.abarmin.lambda.ingest.metadata.downloader;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.abarmin.lambda.ingest.metadata.downloader.model.Request;
import dev.abarmin.lambda.ingest.metadata.downloader.model.Response;
import lombok.SneakyThrows;
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
    return System.getProperty("INGEST_DYNAMODB_TABLE_NAME");
  }

  private String getBucketName() {
    return System.getProperty("INGEST_S3_BUCKET_NAME");
  }

  private void uploadNotice(Path downloadPath, Request request) {
    final S3Client s3Client = S3Client.builder()
        .httpClient(ApacheHttpClient.create())
        .build();

    final PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(getBucketName())
        .key(String.format(
            "notice_%s.xml", request.getCellarId()
        ))
        .build();

    final PutObjectResponse putResponse = s3Client.putObject(putRequest, downloadPath);
  }

  @SneakyThrows
  private Path downloadNotice(final Request request) {
    final String downloadUrl = DOWNLOAD_URL_TEMPLATE.replace("${cellar_id}", request.getCellarId());
    final HttpRequest downloadRequest = HttpRequest.newBuilder()
        .uri(new URI(downloadUrl))
        .GET()
        .build();
    final HttpResponse.BodyHandler<Path> bodyHandler = HttpResponse.BodyHandlers.ofFileDownload(getTempDirectory());
    final HttpResponse<Path> downloadedNotice = httpClient.send(downloadRequest, bodyHandler);
    return downloadedNotice.body();
  }

  @SneakyThrows
  private Path getTempDirectory() {
    if (this.temporaryDirectory == null) {
      this.temporaryDirectory = Files.createTempDirectory("downloader");
    }
    return this.temporaryDirectory;
  }
}
