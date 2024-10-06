package com.ticketbooking.service;

import com.ticketbooking.config.payment.VNPayConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@RequiredArgsConstructor
@Service
public class VNPayService {

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.vnpUrl}")
    private String vnpUrl;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    public String createPaymentUrl(Long amount, String orderId) {
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_OrderInfo = "Thanh toan don hang: " + orderId.replaceAll("\"", "");  // Loại bỏ dấu ngoặc kép nếu có
            String vnp_OrderType = "other";
            String vnp_Amount = String.valueOf(amount * 100);  // Chuyển từ VND sang "xu"

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", tmnCode);
            vnp_Params.put("vnp_Amount", vnp_Amount);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", vnp_OrderType);
            vnp_Params.put("vnp_ReturnUrl", returnUrl);
            vnp_Params.put("vnp_TxnRef", orderId);
            vnp_Params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    if (query.length() > 0) {
                        query.append('&');
                        hashData.append('&');
                    }
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(fieldValue);
                }
            }

            String vnp_SecureHash = VNPayConfig.hmacSHA512(hashSecret, hashData.toString());
            query.append("&vnp_SecureHash=").append(vnp_SecureHash);

            System.out.println("VNPay Params: " + vnp_Params);
            System.out.println("Query: " + query.toString());

            return vnpUrl + "?" + query.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

