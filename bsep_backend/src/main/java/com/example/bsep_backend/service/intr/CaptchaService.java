package com.example.bsep_backend.service.intr;

public interface CaptchaService {
    void verifyCaptcha(String captchaToken, String ipAddress);
}