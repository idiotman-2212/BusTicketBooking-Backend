package com.ticketbooking.controller;

import com.ticketbooking.config.payment.VNPayConfig;
import com.ticketbooking.service.MomoService;
import com.ticketbooking.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;
    private final MomoService momoService;

    @PostMapping("/vnpay/create-payment")
    public ResponseEntity<String> createVNPayPayment(@RequestParam("amount") Long amount, @RequestParam("orderId") String orderId) {
        String paymentUrl = vnPayService.createPaymentUrl(amount, orderId);
        System.out.println("Payment URL:" + paymentUrl);
        return ResponseEntity.ok(paymentUrl);
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<String> vnPayReturn(HttpServletRequest request) {
        Map<String, String> params = VNPayConfig.getQueryParams(request);
        System.out.println("VNPay Params: " + params); // Log các tham số từ VNPay
        String secureHash = params.get("vnp_SecureHash");
        if (VNPayConfig.validateSignature(secureHash, params)) {
            // Handle successful payment here
            return ResponseEntity.ok("Payment successful");
        } else {
            return ResponseEntity.badRequest().body("Invalid payment");
        }
    }

    @PostMapping("/momo/create-payment")
    public ResponseEntity<String> createMoMoPayment(@RequestParam("amount") Long amount, @RequestParam("orderId") String orderId) {
        String paymentUrl = momoService.createPayment(amount, orderId);
        return ResponseEntity.ok(paymentUrl);
    }

    @GetMapping("/momo/return")
    public ResponseEntity<String> momoReturn(HttpServletRequest request) {
        // Handle MoMo payment return
        return ResponseEntity.ok("MoMo payment successful");
    }
}

