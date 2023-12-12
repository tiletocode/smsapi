package com.hanwha.smsapi;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SendJson {

    public void send(WebhookDto dto) throws IOException {
        
        Config config = Config.getConfig();

        String apiUrl = config.getString("webhook.endpoint.url", "https://webhook.site/");
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        int pcode = dto.getPcode();
        String token = config.getString("admin.token", "2DHTY8Z7FRDFNC8JVKEJ");
        String collector = config.getString("server.collector.address",
                "http://10.253.248.90:8080");

        // 수집서버 API PULL
        HttpClient HttpClient = HttpClients.createDefault();
        String memberListUrl = collector + "/open/api/json/project/" + pcode + "/members";
        HttpGet httpGet = new HttpGet(memberListUrl);
        httpGet.addHeader(new BasicHeader("x-whatap-token", token));

        HttpResponse response = HttpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String whatapResponse = EntityUtils.toString(entity);

        // Header 정의
        String trnmSysCode = config.getString("header.trnmsyscode", "APW");
        String ipAddr = config.getString("header.ipaddr", "010253248090");
        int hsno = 1;
        String prsnInfoIncsYn = config.getString("header.prsninfoincsyn", "N");
        String itfcId = config.getString("header.itfcid", "HLIAPW00001");
        String rcveSrvcId = config.getString("header.rcvesrvcid", "iniCspdDvlmUmsSendMgmtPSI004c");
        String rcveSysCode = config.getString("header.rcvesyscode", "INI");
        String serverType = config.getString("header.servertype", "D");
        String rspnDvsnCode = config.getString("reqorresp.code", "S");

        // Optional Key
        String ctfnTokn = "";
        String ogtsTrnnNo = "";
        String mciNodeNo = "";
        String mciSesnId = "";
        String extlDvsnCode = "";
        String emnb = "";
        String belnOrgnCode = "";
        String custId = "";
        String chnlTypeCode = "";
        String scrnId = "";
        String befoScrnId = "";
        String userTmunIdnfVal = "";
        String rqsrIp = "";
        String rqstDttm = "";
        String baseCrny = "";
        String baseCnty = "";
        String baseLang = "";
        String tscsRqstVal = "";
        String postfixSysCode = "";

        // Payload 정의
        String sendCont = "1";
        String ntfcKindCode = config.getString("payload.ntfckindcode", "ZAC9001");
        String jobMsgeCntn = dto.getMessage();
        String sndeDeptCode = config.getString("payload.sndedeptcode", "00025");
        String ntfcTmplCode = config.getString("payload.ntfctmplcode", "AZAC000015");
        String btchPrcsYn = config.getString("payload.btchprcsyn", "1");
        String msgeTitlNm = config.getString("payload.msgetitlnm", "Whatap Event Alert");
        String sndeTlphArcd = config.getString("payload.sndetlpharcd", "");
        String sndeTlphOfno = config.getString("payload.sndetlphofno", "1588");
        String sndeTlphInno = config.getString("payload.sndetlphinno", "6363");
        String sbsnSendYn = config.getString("payload.sbsnsendyn", "N");
        String onlnBtchDvsnCode = config.getString("onlnbtchdvsncode", "R");
        String dutySendYn = config.getString("dutysendyn", "Y");

        // sms 추출
        try {
            // 본문용
            ObjectMapper dataObjectMapper = new ObjectMapper();
            ObjectNode dataJsonNode = dataObjectMapper.createObjectNode();
            // 휴대폰번호 추출용
            ObjectMapper smsObjectMapper = new ObjectMapper();
            JsonNode smsJsonNode = smsObjectMapper.readTree(whatapResponse);

            List<String> smsList = extractSms(smsJsonNode);
            String[] smsArray = smsList.toArray(new String[smsList.size()]);
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            // 반복호출허용
            connection.setDoOutput(true);

            for (String sms : smsArray) {

                // 매 건 새 값이 필요한 Header
                LocalDateTime currentTime = LocalDateTime.now();
                int randomNum = new Random().nextInt(9999);
                String tlgrCretDttm = currentTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
                String rndmNo = String.format("%04d", randomNum);

                String hpTlphTlcmNo = "";
                String hpTlphOfno = "";
                String hpTlphSbno = "";

                // 하이픈을 기준으로 분리저장
                String[] parts = sms.split("-");
                if (parts.length == 3) {
                    hpTlphTlcmNo = parts[0];
                    hpTlphOfno = parts[1];
                    hpTlphSbno = parts[2];
                } else {
                    // 하이픈이 없으면 3-4-4 자리로 끊어서 저장
                    hpTlphTlcmNo = parts[0].substring(0, 3);
                    hpTlphOfno = parts[0].substring(3, 7);
                    hpTlphSbno = parts[0].substring(7);
                }

                // Header 삽입
                ObjectNode headerNode = dataObjectMapper.createObjectNode();
                headerNode.put("trnmSysCode", trnmSysCode);
                headerNode.put("ipAddr", ipAddr);
                headerNode.put("tlgrCretDttm", tlgrCretDttm);
                headerNode.put("rndmNo", rndmNo);
                headerNode.put("hsno", hsno);
                headerNode.put("prsnInfoIncsYn", prsnInfoIncsYn);
                headerNode.put("itfcId", itfcId);
                headerNode.put("rcveSrvcId", rcveSrvcId);
                headerNode.put("rcveSysCode", rcveSysCode);
                headerNode.put("serverType", serverType);
                headerNode.put("rspnDvsnCode", rspnDvsnCode);

                // Header(Optional)
                headerNode.put("ctfnTokn", ctfnTokn);
                headerNode.put("ogtsTrnnNo", ogtsTrnnNo);
                headerNode.put("mciNodeNo", mciNodeNo);
                headerNode.put("mciSesnId", mciSesnId);
                headerNode.put("extlDvsnCode", extlDvsnCode);
                headerNode.put("emnb", emnb);
                headerNode.put("belnOrgnCode", belnOrgnCode);
                headerNode.put("custId", custId);
                headerNode.put("chnlTypeCode", chnlTypeCode);
                headerNode.put("scrnId", scrnId);
                headerNode.put("befoScrnId", befoScrnId);
                headerNode.put("userTmunIdnfVal", userTmunIdnfVal);
                headerNode.put("rqsrIp", rqsrIp);
                headerNode.put("rqstDttm", rqstDttm);
                headerNode.put("baseCrny", baseCrny);
                headerNode.put("baseCnty", baseCnty);
                headerNode.put("baseLang", baseLang);
                headerNode.put("tscsRqstVal", tscsRqstVal);
                headerNode.put("postfixSysCode", postfixSysCode);

                dataJsonNode.set("header", headerNode);

                // Payload 삽입
                ObjectNode payloadNode = dataObjectMapper.createObjectNode();
                payloadNode.put("sendCont", sendCont);
                payloadNode.put("ntfcKindCode", ntfcKindCode);
                payloadNode.put("jobMsgeCntn", jobMsgeCntn);
                payloadNode.put("sndeDeptCode", sndeDeptCode);
                payloadNode.put("ntfcTmplCode", ntfcTmplCode);
                payloadNode.put("hpTlphTlcmNo", hpTlphTlcmNo);
                payloadNode.put("hpTlphOfno", hpTlphOfno);
                payloadNode.put("hpTlphSbno", hpTlphSbno);
                payloadNode.put("btchPrcsYn", btchPrcsYn);
                payloadNode.put("msgeTitlNm", msgeTitlNm);
                payloadNode.put("sndeTlphArcd", sndeTlphArcd);
                payloadNode.put("sndeTlphOfno", sndeTlphOfno);
                payloadNode.put("sndeTlphInno", sndeTlphInno);
                payloadNode.put("sbsnSendYn", sbsnSendYn);
                payloadNode.put("onlnBtchDvsnCode", onlnBtchDvsnCode);
                payloadNode.put("dutySendYn", dutySendYn);

                dataJsonNode.set("payload", payloadNode);

                String finalOutput = dataJsonNode.toPrettyString();
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = finalOutput.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                int responseCode = connection.getResponseCode();
                log.info("HTTP STATUS: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } 
        connection.disconnect();
    }

    private static List<String> extractSms(JsonNode smsJsonNode) {
        List<String> smsList = new ArrayList<>();

        JsonNode dataArray = smsJsonNode.get("data");
        if (dataArray != null && dataArray.isArray()) {
            for (JsonNode dataNode : dataArray) {
                JsonNode smsNode = dataNode.get("sms");
                if (smsNode != null && smsNode.isTextual()) {
                    String smsString = smsNode.asText();
                    if (!smsString.isEmpty()) {
                        String[] smsArray = smsString.split(", ");
                        for (String sms : smsArray) {
                            smsList.add(sms);
                        }
                    }
                }
            }
        }
        return smsList;
    }
}