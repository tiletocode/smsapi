package com.hanwha.smsapi;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SendJson {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void send(WebhookDto dto, String pid, List<String> uids, String topic) throws IOException {
        Config config = Config.getConfig();
        String title = dto.getTitle();
        String message = dto.getMessage();

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("projectId", pid);
        payloadMap.put("userIds", uids);
        payloadMap.put("topic", topic);
        payloadMap.put("title", title);
        payloadMap.put("body", message);
        payloadMap.put("data", ""); 

        // Map -> JSON
        String jsonPayload = objectMapper.writeValueAsString(payloadMap);

        String apiUrl = config.getString("webhook.endpoint.url", "https://whatap.requestcatcher.com/");
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        int responseCode = connection.getResponseCode();
        log.info("HTTP STATUS: " + responseCode);

        connection.disconnect();
    }
}