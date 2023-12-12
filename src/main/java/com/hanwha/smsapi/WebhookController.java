package com.hanwha.smsapi;

import java.io.IOException;

import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<String> webhook(@RequestBody WebhookDto dto) throws IOException {
        dto.nullReplace(dto);
        SendJson sj = new SendJson();
        sj.send(dto);
        return new ResponseEntity<>("Receive Complete.", HttpStatus.OK);
    }
}
