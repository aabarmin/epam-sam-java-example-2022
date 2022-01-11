package dev.abarmin.lambda.ingest.metadata.downloader.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author Aleksandr Barmin
 */
@Data
@Builder
public class Response {
  private final String cellarId;
  private final boolean exists;
  private final boolean downloaded;

  public static Response of(final Request request) {
    return Response.builder()
        .cellarId(request.getCellarId())
        .exists(request.isExists())
        .downloaded(true)
        .build();
  }
}
