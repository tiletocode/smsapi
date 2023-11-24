package com.hanwha.smsapi;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@PropertySource("file:config/config.properties")
public class WebhookController {

    Config config = Config.getConfig();

    @GetMapping("/**")
    public ResponseEntity<String> blockGETRequest() {
        log.warn("There is no service for GET method.");
        return new ResponseEntity<>("There is no service for GET method.", HttpStatus.METHOD_NOT_ALLOWED);
    }

    @PostMapping("${webhook.server.uri:/webhook}")
    public ResponseEntity<String> webhook(@RequestBody WebhookDto dto,
            @RequestParam(name = "projectid", required = true) String pid,
            @RequestParam(name = "userids", required = false) List<String> uids,
            @RequestParam(name = "topic", required = false) String topic) throws IOException {
        dto.nullReplace(dto);

        //userids와 topic이 둘 다 null이면 예외 발생
        if (uids == null && topic == null) {
            log.info("Both userids and topic cannot be null.");
            return new ResponseEntity<>("Both userids and topic cannot be null.", HttpStatus.BAD_REQUEST);
        }
        SendJson sj = new SendJson();
        sj.send(dto, pid, uids, topic);
        return new ResponseEntity<>("Receive Complete.", HttpStatus.OK);
    }
}
