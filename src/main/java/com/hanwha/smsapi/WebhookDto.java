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
    if (dto.getMetricName() == null) {
      dto.setMetricName(nullReplace);
    }
    if (dto.getLevel() == null) {
      dto.setLevel(nullReplace);
    }
    if (dto.getMetricValue() == null) {
      dto.setMetricValue(nullReplace);
    }
    if (dto.getTitle() == null) {
      dto.setTitle(nullReplace);
    }
    if (dto.getMessage() == null) {
      dto.setMessage(nullReplace);
    }
    if (dto.getMetricThreshold() == null) {
      dto.setMetricThreshold(nullReplace);
    }
    if (dto.getOname() == null) {
      dto.setOname(nullReplace);
    }
    if (dto.getProjectName() == null) {
      dto.setProjectName(nullReplace);
    }
    if (dto.getStatus() == null) {
      dto.setStatus(nullReplace);
    }
  }

  public void nodata(WebhookDto dto) {
    if (dto.metricName.equals("nodata")) {
      String originMessage = dto.getMessage();
      String hostName = "[" + dto.getOname() + "] ";
      dto.setMessage(hostName + originMessage);
    }
  }

  public void restart(WebhookDto dto) {
    if (dto.getMetricName().equals("restart")) {
      String originMessage = dto.getMessage();
      String hostName = "[" + dto.getOname() + "] ";
      dto.setMessage(hostName + originMessage);
    }
  }
}