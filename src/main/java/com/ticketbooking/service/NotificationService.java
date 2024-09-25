package com.ticketbooking.service;

import com.ticketbooking.dto.EmailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final SmsService smsService;
    private final MailService mailService;
    private final Environment env;

    public void sendSmsConfirmation(String phoneNumber, String source, String destination, String busInfo, String departureTime, String seatNumbers, BigDecimal totalPayment) {
        String message = String.format(
                "THÔNG TIN VÉ ĐẶT\n" +
                        "Tuyến: %s => %s\n" +
                        "Xe: %s\n" +
                        "Ngày đi: %s\n" +
                        "Ghế: %s\n" +
                        "Giá vé: %s",
                source, destination, busInfo, departureTime, seatNumbers,
                NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(totalPayment)
        );

        // Chuyển đổi số điện thoại
        if (phoneNumber != null && phoneNumber.startsWith("0")) {
            phoneNumber = "+84" + phoneNumber.substring(1);
        }

        // Gửi SMS tới số điện thoại khách hàng
        smsService.sendSms(phoneNumber, message);
    }

    public void sendEmailConfirmation(String email, String source, String destination, String busInfo, String departureTime, String seatNumbers, BigDecimal totalPayment) {
        String emailContent = String.format(
                "Kính chào quý khách,\n\n" +
                        "Cảm ơn bạn đã đặt vé với chúng tôi. Dưới đây là thông tin chi tiết vé của bạn:\n\n" +
                        "Tuyến: %s => %s\n" +
                        "Xe: %s\n" +
                        "Ngày đi: %s\n" +
                        "Ghế: %s\n" +
                        "Giá vé: %s\n\n" +
                        "Chúng tôi hy vọng bạn sẽ có một chuyến đi vui vẻ.\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ hỗ trợ khách hàng.",
                source, destination, busInfo, departureTime, seatNumbers,
                NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(totalPayment)
        );

        // Tạo đối tượng EmailMessage
        EmailMessage emailMessage = EmailMessage.builder()
                .from(env.getProperty("spring.mail.username"))
                .to(email)
                .subject("Xác nhận đặt vé thành công")
                .text(emailContent)
                .build();

        // Gửi email qua MailService
        mailService.send(emailMessage);
    }
}
