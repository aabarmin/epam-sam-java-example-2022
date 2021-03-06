package dev.abarmin.lambda.ingest.alerter.model;

import java.util.Collection;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author Aleksandr Barmin
 */
@Data
@RequiredArgsConstructor
public class Response {
  private final Collection<ResponseLine> items;
}
