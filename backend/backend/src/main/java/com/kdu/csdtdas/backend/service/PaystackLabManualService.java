package com.kdu.csdtdas.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdu.csdtdas.backend.dto.LabStudentDTOs;
import com.kdu.csdtdas.backend.entity.LabManualOrder;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.LabManualOrderRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class PaystackLabManualService {
    private final LabManualOrderRepository orders;
    private final StudentRepository students;
    private final LabManualService manualService;
    private final RestClient http = RestClient.create();
    private final ObjectMapper mapper;
    private final String secretKey;
    private final String webhookSecret;
    private final String callbackUrl;

    public PaystackLabManualService(LabManualOrderRepository orders, StudentRepository students,
            LabManualService manualService, ObjectMapper mapper,
            @Value("${paystack.secret-key:}") String secretKey,
            @Value("${paystack.webhook-secret:}") String webhookSecret,
            @Value("${paystack.callback-url:}") String callbackUrl) {
        this.orders=orders; this.students=students; this.manualService=manualService; this.mapper=mapper;
        this.secretKey=secretKey; this.webhookSecret=webhookSecret; this.callbackUrl=callbackUrl;
    }

    @Transactional
    public LabStudentDTOs.PaymentInit initialise(String username, Long orderId) {
        requireConfigured();
        Student student = student(username);
        LabManualOrder order = orders.findByIdAndStudentId(orderId, student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Manual purchase request not found."));
        if ("PAID".equals(order.getStatus()) || "ISSUED".equals(order.getStatus()))
            throw new IllegalArgumentException("This lab manual has already been paid for.");
        if (!"AWAITING_PAYMENT".equals(order.getStatus()))
            throw new IllegalArgumentException("This purchase request cannot be paid.");
        User user = student.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank())
            throw new IllegalArgumentException("Your student account must have a valid email address before payment.");

        if (order.getPaystackReference() != null) {
            if (order.getPaystackAuthorizationUrl() != null && !order.getPaystackAuthorizationUrl().isBlank())
                return new LabStudentDTOs.PaymentInit(order.getId(), order.getPaystackReference(), order.getPaystackAuthorizationUrl());
            throw new IllegalStateException("A payment attempt already exists. Please contact the laboratory office before retrying.");
        }

        long kobo = order.getAmount().multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        String reference = "LABMAN-" + UUID.randomUUID().toString().replace("-", "");
        order.setPaystackReference(reference);
        orders.save(order);

        Map<String,Object> body = Map.of(
                "email", user.getEmail(),
                "amount", Long.toString(kobo),
                "currency", "NGN",
                "reference", reference,
                "callback_url", callbackUrl,
                "metadata", Map.of("order_id", order.getId(), "purpose", "CSDT lab manual")
        );
        JsonNode response = http.post().uri("https://api.paystack.co/transaction/initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + secretKey)
                .body(body).retrieve().body(JsonNode.class);
        if (response == null || !response.path("status").asBoolean(false)
                || response.path("data").path("authorization_url").asText("").isBlank())
            throw new IllegalStateException("Paystack could not initialise this payment. Please try again.");
        String authorizationUrl=response.path("data").path("authorization_url").asText();
        order.setPaystackAuthorizationUrl(authorizationUrl);
        orders.save(order);
        return new LabStudentDTOs.PaymentInit(order.getId(), reference, authorizationUrl);
    }

    @Transactional
    public LabStudentDTOs.Receipt verifyForStudent(String username, String reference) {
        Student student=student(username);
        LabManualOrder order=orders.findByPaystackReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Payment reference not found."));
        if (!order.getStudent().getId().equals(student.getId()))
            throw new IllegalArgumentException("This payment does not belong to your student account.");
        verifyAndApply(reference);
        return receipt(order);
    }

    @Transactional
    public void verifyWebhookReference(String reference) {
        if (reference == null || reference.isBlank()) return;
        orders.findByPaystackReference(reference).ifPresent(order -> verifyAndApply(reference));
    }

    private void verifyAndApply(String reference) {
        requireConfigured();
        LabManualOrder order=orders.findByPaystackReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment reference."));
        if ("PAID".equals(order.getStatus()) || "ISSUED".equals(order.getStatus())) return;

        JsonNode response=http.get().uri("https://api.paystack.co/transaction/verify/{reference}", reference)
                .header("Authorization", "Bearer " + secretKey).retrieve().body(JsonNode.class);
        JsonNode data=response == null ? null : response.path("data");
        if (response == null || !response.path("status").asBoolean(false) || data == null
                || !"success".equalsIgnoreCase(data.path("status").asText())
                || !reference.equals(data.path("reference").asText())
                || !"NGN".equalsIgnoreCase(data.path("currency").asText())
                || data.path("amount").asLong(-1) != order.getAmount().multiply(new BigDecimal("100")).longValueExact()) {
            throw new IllegalArgumentException("Payment is not verified or the amount does not match this manual.");
        }

        order.setStatus("PAID");
        order.setGatewayTransactionId(data.path("id").asText());
        order.setPaymentChannel(data.path("channel").asText(""));
        order.setPaidAt(LocalDateTime.now());
        order.setReceiptNumber("CSDT-LM-" + order.getId() + "-" + reference.substring(reference.length()-8));
        order.setUpdatedBy("PAYSTACK_VERIFIED");
        orders.save(order);
    }

    @Transactional(readOnly=true)
    public LabStudentDTOs.Receipt getReceipt(String username, Long orderId) {
        Student student=student(username);
        LabManualOrder order=orders.findByIdAndStudentId(orderId, student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found."));
        if (!"PAID".equals(order.getStatus()) && !"ISSUED".equals(order.getStatus()))
            throw new IllegalArgumentException("A receipt is available only after payment is confirmed.");
        return receipt(order);
    }

    @Transactional
    public LabStudentDTOs.OrderRow issue(Long orderId, String staffUsername) {
        LabManualOrder order=orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase request not found."));
        if (!"PAID".equals(order.getStatus()))
            throw new IllegalArgumentException("Only a verified paid manual can be issued.");
        // Record the physical issue through the existing stock/sales ledger in the same transaction.
        manualService.recordPaidSale(staffUsername, order.getManual().getId(), order.getStudent().getId(), order.getAcademicSession().getId());
        order.setStatus("ISSUED"); order.setIssuedAt(LocalDateTime.now());
        order.setIssuedBy(staffUsername); order.setUpdatedBy(staffUsername);
        orders.save(order);
        return new LabStudentDTOs.OrderRow(order.getId(), order.getManual().getId(),
                order.getManual().getTitle(), order.getManual().getCourse()==null ? "" : order.getManual().getCourse().getCode(),
                order.getAmount(), order.getStatus(), order.getAcademicSession().getName(), order.getCreatedAt());
    }

    public boolean validWebhookSignature(byte[] rawBody, String paystackSignature, String legacySignature) {
        if (rawBody == null) return false;
        try {
            if (paystackSignature != null && !webhookSecret.isBlank()) {
                String timestamp=null;
                for (String part : paystackSignature.split(",")) {
                    String p=part.trim();
                    if (p.startsWith("t=")) timestamp=p.substring(2);
                }
                if (timestamp == null) return false;
                long signedAt=Long.parseLong(timestamp);
                if (Math.abs(Instant.now().getEpochSecond()-signedAt)>300) return false;
                byte[] expected=hmac(webhookSecret, (timestamp + ".").getBytes(StandardCharsets.UTF_8), rawBody);
                for (String part : paystackSignature.split(",")) {
                    String p=part.trim();
                    if (p.startsWith("v1=")) {
                        String value=p.substring(3);
                        int colon=value.indexOf(':');
                        String hex=colon>=0 ? value.substring(colon+1) : value;
                        if (hex.matches("(?i)[0-9a-f]{128}") &&
                                MessageDigest.isEqual(expected, HexFormat.of().parseHex(hex))) return true;
                    }
                }
            }
            if (legacySignature != null && !secretKey.isBlank()
                    && legacySignature.matches("(?i)[0-9a-f]{128}")) {
                byte[] expected=hmac(secretKey, new byte[0], rawBody);
                return MessageDigest.isEqual(expected, HexFormat.of().parseHex(legacySignature));
            }
        } catch (Exception ignored) { return false; }
        return false;
    }

    public boolean isChargeSuccess(byte[] rawBody) {
        try { return "charge.success".equals(mapper.readTree(rawBody).path("event").asText()); }
        catch (Exception e) { return false; }
    }

    public String extractReference(byte[] rawBody) {
        try { return mapper.readTree(rawBody).path("data").path("reference").asText(null); }
        catch (Exception e) { return null; }
    }

    private byte[] hmac(String key, byte[] prefix, byte[] body) throws Exception {
        Mac mac=Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        if (prefix.length>0) mac.update(prefix);
        mac.update(body);
        return mac.doFinal();
    }

    private Student student(String username) {
        return students.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found."));
    }
    private LabStudentDTOs.Receipt receipt(LabManualOrder o) {
        Student s=o.getStudent();
        return new LabStudentDTOs.Receipt(o.getId(), o.getReceiptNumber(), s.getMatricNumber(),
                s.getFullName(), o.getManual().getCourse()==null ? "" : o.getManual().getCourse().getCode(),
                o.getManual().getTitle(), o.getAmount(), "NGN", o.getPaystackReference(),
                o.getPaymentChannel(), o.getPaidAt(), o.getStatus(), o.getIssuedAt(), o.getIssuedBy());
    }
    private void requireConfigured() {
        if (secretKey == null || secretKey.isBlank() || callbackUrl == null || callbackUrl.isBlank())
            throw new IllegalStateException("Online payment is not configured. Contact the department administrator.");
    }
}
