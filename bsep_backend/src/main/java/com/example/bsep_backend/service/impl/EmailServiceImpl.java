package com.example.bsep_backend.service.impl;

import com.example.bsep_backend.service.intr.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendActivationEmail(String to, String username, String activationToken) {
        try {
            System.out.println("Sending activation email to " + to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Aktivacija naloga - MA App");

            String activationLink = baseUrl + "/auth/activate/" + activationToken;
            String htmlContent = createActivationEmailContent(username, activationLink);

            helper.setText(htmlContent, true);
            System.out.println("Almost there...");

            mailSender.send(message);
            log.info("Activation email sent successfully to: {}", to);

        } catch (MessagingException e) {
            System.out.println("EMAIL COULDNT BE SENT: "+e.getMessage());
            log.error("Failed to send activation email to: {}", to, e);
            throw new RuntimeException("Failed to send activation email", e);
        }
    }

    private String createActivationEmailContent(String username, String activationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Aktivacija naloga</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Dobrodošli u MA App!</h2>

                    <p>Pozdrav <strong>%s</strong>,</p>

                    <p>Hvala Vam što ste se registrovali na našoj aplikaciji. Da biste aktivirali svoj nalog, molimo Vas da kliknite na dugme ispod:</p>

                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #3498db; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block;">
                            Aktiviraj nalog
                        </a>
                    </div>

                    <p>Ili kopirajte i zalepite sledeći link u Vaš browser:</p>
                    <p style="word-break: break-all; background-color: #f8f9fa; padding: 10px; border-radius: 3px;">
                        %s
                    </p>

                    <p><strong>Važno:</strong> Ovaj link je validan 24 sata od momenta registracije. Ako ne aktivirate nalog u tom periodu, moraćete da ponovite postupak registracije.</p>

                    <p>Ako niste Vi poslali ovaj zahtev, molimo Vas da ignorišite ovaj email.</p>

                    <hr style="margin: 20px 0; border: none; border-top: 1px solid #eee;">
                    <p style="font-size: 12px; color: #666;">
                        MA App Team<br>
                        Ovaj email je automatski generisan, molimo Vas ne odgovarajte na njega.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(username, activationLink, activationLink);
    }
}