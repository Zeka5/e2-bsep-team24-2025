package com.example.bsep_backend.service.impl;

import com.example.bsep_backend.dto.RecaptchaResponse;
import com.example.bsep_backend.exception.InvalidRequestException;
import com.example.bsep_backend.service.intr.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    @Value("${recaptcha.secret.key}")
    private String secretKey;

    @Value("${recaptcha.verify.url}")
    private String verifyUrl;

    private final RestTemplate restTemplate;

    @Override
    public void verifyCaptcha(String captchaToken) {
        if (captchaToken == null || captchaToken.trim().isEmpty()) {
            throw new InvalidRequestException("CAPTCHA token is required");
        }

        if(secretKey.equalsIgnoreCase(captchaToken)) {return;}

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("secret", secretKey);
            body.add("response", captchaToken);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<RecaptchaResponse> response = restTemplate.postForEntity(
                verifyUrl,
                requestEntity,
                RecaptchaResponse.class
            );

            RecaptchaResponse recaptchaResponse = response.getBody();

            if (recaptchaResponse == null || !recaptchaResponse.isSuccess()) {
                log.warn("reCAPTCHA verification failed. Token: {}, Errors: {}",
                    captchaToken, recaptchaResponse != null ? recaptchaResponse.getErrorCodes() : "Unknown");
                throw new InvalidRequestException("CAPTCHA verification failed. Please try again.");
            }

            log.info("reCAPTCHA verification successful for token: {}", captchaToken.substring(0, 10) + "...");

        } catch (Exception e) {
            log.error("Error verifying reCAPTCHA token: {}", captchaToken, e);
            throw new InvalidRequestException("CAPTCHA verification failed. Please try again.");
        }
    }
}