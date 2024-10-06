package com.ticketbooking.service;

import com.ticketbooking.config.payment.MomoConfig;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MomoService {

    @Value("${momo.partnerCode}")
    private String partnerCode;

    @Value("${momo.accessKey}")
    private String accessKey;

    @Value("${momo.secretKey}")
    private String secretKey;

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.returnUrl}")
    private String returnUrl;

    public String createPayment(Long amount, String orderId) {
        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("partnerCode", partnerCode);
            jsonRequest.put("accessKey", accessKey);
            jsonRequest.put("requestId", orderId);
            jsonRequest.put("amount", String.valueOf(amount));
            jsonRequest.put("orderId", orderId);
            jsonRequest.put("orderInfo", "Payment for order " + orderId);
            jsonRequest.put("returnUrl", returnUrl);

            String data = "partnerCode=" + partnerCode + "&accessKey=" + accessKey + "&requestId=" + orderId + "&amount=" + amount;
            String signature = MomoConfig.signHmacSHA256(secretKey, data);
            jsonRequest.put("signature", signature);

            return MomoConfig.sendPostRequest(endpoint, jsonRequest.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
