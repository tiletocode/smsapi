package com.hanwha.smsapi;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

@Slf4j
public class SendJson {

    private Config config = Config.getConfig();
    private final String token = config.getString("admin.token", "2DHTY8Z7FRDFNC8JVKEJ");
    private final String collector = config.getString("server.collector.address", "http://10.253.248.90:8080");

    private final OkHttpClient okHttpClient;
    private ConnectionPool cPool = new ConnectionPool(50, 1L, java.util.concurrent.TimeUnit.MINUTES);
    private String loggingLevel = config.getString("okhttp.logging.level", "NONE");
    private HttpLoggingInterceptor.Level logLevel = HttpLoggingInterceptor.Level.valueOf(loggingLevel);

    public SendJson() {

        // 생성자에서 OkHttpClient를 초기화
        this.okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(logLevel))
                .connectionPool(cPool)
                .build();
    }

    private List<String> getReceiversByGroupId(String groupId) throws IOException{
        List<String> result = new ArrayList<String>();
        String groupListUrl = collector + "/open/api/json/group/" + groupId + "/members";
        Request httpGetRequest = new Request.Builder()
                .url(groupListUrl)
                .addHeader("x-whatap-token", token)
                .build();
        Response okHttpResponse = okHttpClient.newCall(httpGetRequest).execute();
        String whatapResponse = okHttpResponse.body().string();
        ObjectMapper smsObjectMapper = new ObjectMapper();
        JsonNode smsJsonNode = smsObjectMapper.readTree(whatapResponse);
        result.addAll(extractSms(smsJsonNode));
        return result;
    }

    private List<String> getReceiversByPcode(int pcode) throws IOException {
        List<String> result = new ArrayList<String>();
        String memberListUrl = collector + "/open/api/json/project/" + pcode + "/members";
        Request httpGetRequest = new Request.Builder()
                .url(memberListUrl)
                .addHeader("x-whatap-token", token)
                .build();
        Response okHttpResponse = okHttpClient.newCall(httpGetRequest).execute();
        String whatapResponse = okHttpResponse.body().string();
        ObjectMapper smsObjectMapper = new ObjectMapper();
        JsonNode smsJsonNode = smsObjectMapper.readTree(whatapResponse);
        result.addAll(extractSms(smsJsonNode));
        return result;
    }

    public void send(WebhookDto dto, String groupId) throws IOException {

        List<String> receivers = new ArrayList<String>();
        String apiUrl = config.getString("webhook.endpoint.url", "http://inf.hanwhalife.com/esb:80");

        int pcode = dto.getPcode();

        // /webhook -> pcode로 프로젝트 멤버목록, /webhook/{groupId} -> groupId로 그룹 멤버목록
        if (groupId != null) {
            receivers.addAll(getReceiversByGroupId(groupId));
        } else {
            receivers.addAll(getReceiversByPcode(pcode));
        }

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

        // Payload 정의
        String sendCont = "1";
        String ntfcKindCode = config.getString("payload.ntfckindcode", "ZAC9001");
        String prefixCntn = config.getString("payload.prefixcntn", "[Whatap-모니터링 알림]\\n");
        String jobMsgeCntn = prefixCntn + dto.getMessage();
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

        Response finalResponse = null;

        try {
            ObjectMapper dataObjectMapper = new ObjectMapper();
            ObjectNode dataJsonNode = dataObjectMapper.createObjectNode();

            // 로깅 index 출력용

            // Timestamp, RandomNum, digits는 매번 받아와야함
            for (int index = 0; index < receivers.size(); index++) {
                try {
                    String sms = receivers.get(index);

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

                    // ObjectNode dataJsonNode = dataObjectMapper.createObjectNode();
                    dataJsonNode.set("header", headerNode);
                    dataJsonNode.set("payload", payloadNode);

                    String finalOutput = dataJsonNode.toString();

                    // OkHttp를 사용한 POST 요청
                    RequestBody requestBody = RequestBody.create(finalOutput, MediaType.get("application/json"));
                    Request request = new Request.Builder()
                            .url(apiUrl)
                            .post(requestBody)
                            .build();

                    finalResponse = okHttpClient.newCall(request).execute();
                    log.info("HTTP STATUS [" + (index + 1) + "] : " + finalResponse.code());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 메모리 누수방지: Payload 전송
                    if (finalResponse != null && finalResponse.body() != null) {
                        finalResponse.body().close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 받아온 JSON에서 전화번호 추출
    private List<String> extractSms(JsonNode smsJsonNode) {
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