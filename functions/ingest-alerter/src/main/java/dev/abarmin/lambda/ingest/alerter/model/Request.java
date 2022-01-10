package dev.abarmin.lambda.ingest.alerter.model;

import lombok.Data;

/**
 * @author Aleksandr Barmin
 */
@Data
public class Request {
  private String startDate;
  private String endDate;
}
