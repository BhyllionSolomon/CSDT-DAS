package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.PaystackLabManualService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/paystack")
public class PaystackWebhookController {
    private final PaystackLabManualService payments;
    public PaystackWebhookController(PaystackLabManualService payments) { this.payments=payments; }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody byte[] rawBody,
            @RequestHeader(value="Paystack-Signature", required=false) String signature,
            @RequestHeader(value="x-paystack-signature", required=false) String legacySignature) {
        if (!payments.validWebhookSignature(rawBody, signature, legacySignature))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (payments.isChargeSuccess(rawBody))
            payments.verifyWebhookReference(payments.extractReference(rawBody));
        return ResponseEntity.ok().build();
    }
}
