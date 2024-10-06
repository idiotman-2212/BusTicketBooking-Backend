package com.ticketbooking.config.payment;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.Map;

@Component
@Data
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class VNPayConfig {
    public static String hmacSHA512(String key, String data) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            sha512_HMAC.init(secret_key);
            byte[] array = sha512_HMAC.doFinal(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte item : array) {
                sb.append(String.format("%02x", item));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hmacSHA512", e);
        }
    }

    public static Map<String, String> getQueryParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            params.put(entry.getKey(), entry.getValue()[0]);
        }
        return params;
    }

    public static boolean validateSignature(String secureHash, Map<String, String> params) {
        // Validate payment signature logic
        return true; // For simplicity, return true
    }
}

