package com.hanwha.smsapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookDto {
  private String metricName;
  private int pcode;
  private String level;
  private String metricValue;
  private long oid;
  private String title;
  private String message;
  private String uuid;
  private String metricThreshold;
  private String oname;
  private String projectName;
  private String status;
  private long time;

  public void nullReplace(WebhookDto dto) {
    Config config = Config.getConfig();
    String nullReplace = config.getString("webhook.message.nullreplace", "empty_value");
    if (dto.getTitle() == null) {
      dto.setTitle(nullReplace);
    }
    if (dto.getMessage() == null) {
      dto.setMessage(nullReplace);
    }
  }

  public void nodata(WebhookDto dto) {
    if (dto.getMetricName() == "nodata") {
      String originMessage = dto.getMessage();
      String hostName = dto.getOname() + ": ";
      dto.setMessage(hostName + originMessage);
    }
  }

  public void restart(WebhookDto dto) {
    if (dto.getMetricName() == "restart") {
      String originMessage = dto.getMessage();
      String hostName = dto.getOname() + ": ";
      dto.setMessage(hostName + originMessage);
    }
  }
}