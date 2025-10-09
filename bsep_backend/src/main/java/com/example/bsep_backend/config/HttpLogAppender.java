package com.example.bsep_backend.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class HttpLogAppender extends AppenderBase<ILoggingEvent> {

    private String url = "http://localhost:3001/api/logs";
    private RestTemplate restTemplate = new RestTemplate();

    @Override
    protected void append(ILoggingEvent event) {
        try {
            Map<String, String> logData = new HashMap<>();
            logData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            logData.put("level", event.getLevel().toString());
            logData.put("logger", event.getLoggerName());
            logData.put("message", event.getFormattedMessage());
            logData.put("thread", event.getThreadName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(logData, headers);
            restTemplate.postForEntity(url, request, String.class);

        } catch (Exception e) {
            // Ne logujemo grešku da ne napravimo beskonačnu petlju
            System.err.println("Failed to send log to server: " + e.getMessage());
        }
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
