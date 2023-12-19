package com.hanwha.smsapi;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Config {
    private Properties props = new Properties();
    private static Config instance = new Config();

    private Config() {
        String propsPath = System.getProperty("config.path", "config/config.properties");
        try {
            // Specify UTF-8 encoding
            InputStreamReader reader = new InputStreamReader(new FileInputStream(propsPath), StandardCharsets.UTF_8);
            props.load(reader);
        } catch (Exception e) {
            log.error("Failed to read the config file.", e);
        }
    }

    public static Config getConfig() {
        return instance;
    }

    public String getString(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return Integer.parseInt(props.getProperty(key, Integer.toString(defaultValue)));
    }

    @Override
    public String toString() {
        return props.toString();
    }
}
