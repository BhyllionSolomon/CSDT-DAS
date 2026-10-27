package com.kdu.csdtdas.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final boolean devLog;

    public MailService(
            @Value("${app.mail.api-key:}") String apiKey,
            @Value("${app.mail.from-email:}") String fromEmail,
            @Value("${app.mail.from-name:CSDT-DAS}") String fromName,
            @Value("${app.verification.devlog:}") String devLogSetting
    ) {
        this.apiKey = apiKey.trim();
        this.fromEmail = fromEmail.trim();
        this.fromName = fromName.isBlank() ? "CSDT-DAS" : fromName.trim();

        // Render always sets a RENDER variable, so its absence means this is your own computer.
        // Codes are printed in the console there, and never on Render.
        this.devLog = devLogSetting.isBlank()
                ? System.getenv("RENDER") == null
                : Boolean.parseBoolean(devLogSetting.trim());
    }

    public void send(String to, String subject, String text) {

        if (!apiKey.isBlank() && !fromEmail.isBlank()) {
            post(to, subject, text);
            return;
        }

        if (devLog) {
            log.warn("LOCAL MODE: no email was sent. To: {} | {} | {}", to, subject, text);
            return;
        }

        throw new IllegalStateException("Email sending is not set up on this server.");
    }

    private void post(String to, String subject, String text) {

        String body = "{\"sender\":{\"name\":" + quote(fromName) + ",\"email\":" + quote(fromEmail) + "},"
                + "\"to\":[{\"email\":" + quote(to) + "}],"
                + "\"subject\":" + quote(subject) + ",\"textContent\":" + quote(text) + "}";

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", apiKey)
                .header("content-type", "application/json")
                .header("accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("The email service refused the message (HTTP {}): {}", response.statusCode(), response.body());
                throw new IllegalStateException("The email could not be sent.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("The email could not be sent.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The email could not be sent.", e);
        }
    }

    private static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : (s == null ? "" : s).toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.append('"').toString();
    }
}