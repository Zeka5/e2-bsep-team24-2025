package com.example.bsep_backend.service.intr;

public interface EmailService {
    void sendActivationEmail(String to, String username, String activationToken);
}