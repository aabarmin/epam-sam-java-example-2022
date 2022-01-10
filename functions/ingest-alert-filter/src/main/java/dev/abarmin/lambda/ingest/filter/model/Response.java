package dev.abarmin.lambda.ingest.filter.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author Aleksandr Barmin
 */
@Data
@RequiredArgsConstructor
public class Response {
  private final String cellarId;
  private final boolean exists;
}
