package com.mycompany.tutorhub_enterprise.server;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SmsService {

    private static final String SPEED_SMS_TOKEN = ServerConfig.get(
            "TUTORHUB_SPEEDSMS_TOKEN",
            "tutorhub.speedsms.token",
            ""
    );

    public static boolean sendOtpSms(String targetPhone) {
        return sendOtpSms(targetPhone, OtpService.PURPOSE_SMS_LOGIN);
    }

    public static boolean sendOtpSms(String targetPhone, String purpose) {
        String normalizedPhone = DatabaseManager.normalizePhone(targetPhone);
        OtpService.IssueResult issue = OtpService.issue(normalizedPhone, purpose);
        if (!issue.isIssued()) {
            System.err.println("[SMS] OTP send is rate limited. Retry after " + issue.getRetryAfterSeconds() + "s.");
            return false;
        }

        String otp = issue.getCode();
        if (ServerConfig.isAuthDevMode()) {
            System.out.println("[AUTH DEV] SMS OTP for " + normalizedPhone + ": " + otp);
            return true;
        }

        if (ServerConfig.isBlank(SPEED_SMS_TOKEN)) {
            System.err.println("[SMS] Missing SpeedSMS token. Set TUTORHUB_SPEEDSMS_TOKEN.");
            OtpService.clear(normalizedPhone, purpose);
            return false;
        }

        try {
            URL url = new URL("https://api.speedsms.vn/index.php/sms/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            String auth = SPEED_SMS_TOKEN + ":x";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            conn.setDoOutput(true);

            String jsonInputString = "{\"to\":\"" + normalizedPhone
                    + "\",\"content\":\"[TutorHub] Ma xac thuc OTP cua ban la: " + otp
                    + "\",\"sms_type\":2,\"sender\":\"Notify\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("[SMS] OTP sent successfully via SpeedSMS.");
                return true;
            }

            System.err.println("[SMS] SpeedSMS rejected the request. Code: " + responseCode);
        } catch (Exception apiEx) {
            System.err.println("[SMS] Failed to connect to SpeedSMS: " + apiEx.getMessage());
        }

        OtpService.clear(normalizedPhone, purpose);
        return false;
    }

    public static boolean verifyOtp(String targetPhone, String inputOtp) {
        return verifyOtp(targetPhone, OtpService.PURPOSE_SMS_LOGIN, inputOtp);
    }

    public static boolean verifyOtp(String targetPhone, String purpose, String inputOtp) {
        return OtpService
                .verify(DatabaseManager.normalizePhone(targetPhone), purpose, inputOtp)
                .isSuccess();
    }
}
