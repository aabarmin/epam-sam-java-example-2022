package dev.abarmin.lambda.ingest.metadata.downloader.model;

import lombok.Data;

/**
 * @author Aleksandr Barmin
 */
@Data
public class Request {
  private String cellarId;
  private boolean exists;
}
