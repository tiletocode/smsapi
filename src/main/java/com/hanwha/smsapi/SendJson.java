package com.hanwha.smsapi;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SendJson {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void send(WebhookDto dto) throws IOException {
        Config config = Config.getConfig();

        int pcode = dto.getPcode();
        String token = config.getString("admin.token", "NDQ6LLNY4WDXVI6120WE");
        String collector = config.getString("server.collector.address", "http://158.247.198.153:8080");

        // 수집서버 API PULL
        HttpClient HttpClient = HttpClients.createDefault();
        String memberListUrl = collector + "/open/api/json/project/" + pcode + "/members";
        HttpGet httpGet = new HttpGet(memberListUrl);
        httpGet.addHeader(new BasicHeader("x-whatap-token", token));

        HttpResponse response = HttpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String jsonResponse = EntityUtils.toString(entity);

        // Header 정의
        String trnmSysCode = config.getString("trnm.sys.code", "APW");
        String ipAddr = config.getString("server.ipaddr", "010253248090");
        String hsno = "1";
        String prsnInfoIncsYn = "Y";
        String itfcId = "";
        String rcveSrvId = "";
        String rcveSysCode = "";
        String serverType = config.getString("server.type", "D");
        String rspnDvsnCode = config.getString("reqorresp.code", "S");

        // Payload 정의
        int sendCont = 1;
        String ntfcKindCode = config.getString("ntfc.kind.code", "CTC00001");
        String jobMsgeCntn = dto.getMessage();
        String sndeDeptCode = config.getString("dept.code", "210505");
        String ntfcTmplCode = config.getString("ntfc.template.code", "ACP00061");

        // 사번, digits 추출
        ObjectMapper digitsObjectMapper = new ObjectMapper();
        JsonNode jsonNode = digitsObjectMapper.readTree(jsonResponse);
        JsonNode dataArray = jsonNode.get("data");
        String[] digitsArray = new String[dataArray.size()];
        String[] addInfoArray = new String[dataArray.size()];

        String apiUrl = config.getString("webhook.endpoint.url", "https://webhook.site/");
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        for (int i = 0; i < dataArray.size(); i++) {
            JsonNode dataItem = dataArray.get(i);
            String digitsValue = dataItem.get("digits").asText();
            String addInfoValue = dataItem.get("addInfo").asText();
            digitsArray[i] = digitsValue;
            addInfoArray[i] = addInfoValue;

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            //매번 새 값이 필요한 Header
            LocalDateTime currentTime = LocalDateTime.now();
            String tlgrCertDttm = currentTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            int randomNum = new Random().nextInt(9999);
            String rndmNo = String.format("%04d", randomNum);

            //매번 새 값이 필요한 Payload
            

            // Header 추가
            connection.setRequestProperty("trnmSysCode", trnmSysCode);
            connection.setRequestProperty("ipAddr", ipAddr);
            connection.setRequestProperty("tlgrCertDttm", tlgrCertDttm);
            connection.setRequestProperty("rndmNo", rndmNo);
            connection.setRequestProperty("hsno", hsno);
            connection.setRequestProperty("prsnInfoIncsYn", prsnInfoIncsYn);
            connection.setRequestProperty("itfcId", itfcId);
            connection.setRequestProperty("rcveSrvId", rcveSrvId);
            connection.setRequestProperty("rcveSysCode", rcveSysCode);
            connection.setRequestProperty("serverType", serverType);
            connection.setRequestProperty("rspnDvsnCode", rspnDvsnCode);

            connection.setDoOutput(true);

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("sendCont", sendCont);
            payloadMap.put("ntfcKindCode", ntfcKindCode);
            payloadMap.put("jobMsgeCntn", jobMsgeCntn);
            payloadMap.put("sndeDeptCode", sndeDeptCode);
            payloadMap.put("ntfcTmplCode", ntfcTmplCode);

            // Map -> JSON
            String jsonPayload = objectMapper.writeValueAsString(payloadMap);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int responseCode = connection.getResponseCode();
            log.info("data" + dataArray.size() + " - " + "HTTP STATUS: " + responseCode);

            connection.disconnect();
        }
    }
}