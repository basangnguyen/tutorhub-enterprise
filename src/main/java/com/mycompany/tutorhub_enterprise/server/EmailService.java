package com.mycompany.tutorhub_enterprise.server;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import java.net.URLEncoder;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import java.io.File;

public class EmailService {
    
    // 1. Email gửi đi
    private static final String SENDER_EMAIL = ServerConfig.get("TUTORHUB_SMTP_USER", "tutorhub.smtp.user", "");
    
    // 2. Mật khẩu ứng dụng 16 ký tự
    private static final String APP_PASSWORD = ServerConfig.get("TUTORHUB_SMTP_PASSWORD", "tutorhub.smtp.password", "");
    private static final String SMTP_HOST = ServerConfig.get("TUTORHUB_SMTP_HOST", "tutorhub.smtp.host", "smtp.gmail.com");
    private static final String SMTP_PORT = ServerConfig.get("TUTORHUB_SMTP_PORT", "tutorhub.smtp.port", "587");
    private static final String MAIL_FROM_NAME = ServerConfig.get("TUTORHUB_SMTP_FROM_NAME", "tutorhub.smtp.fromName", "TutorHub Enterprise");
    
    // 3. ĐƯỜNG LINK LOGO TRỰC TUYẾN
    private static final String LOGO_URL = "https://i.postimg.cc/xjtYgsH9/Tutor-Hub-(40-x-40-px)-(512-x-512-px)-(1).png";

    // ==============================================================
    // 📧 1. MẪU 2: EMAIL THƯ MỜI SỰ KIỆN LỊCH TRÌNH (EVENT)
    // ==============================================================
    public static boolean sendCalendarInvite(String recipientEmail, String subject, String eventTitle, String eventTime, String location, String meetLink, String attachmentPath, String description) {
        return sendEmailCore(recipientEmail, subject, eventTitle, eventTime, location, meetLink, attachmentPath, description, null, false);
    }

    // ==============================================================
    // 📧 2. MẪU 1: EMAIL YÊU CẦU KHẢO SÁT LỊCH HỌC (POLL)
    // ==============================================================
    public static boolean sendPollInvite(String recipientEmail, String subject, String pollTitle, String timeWindow, String location, String meetLink, String attachmentPath, String description, String pollLink) {
        return sendEmailCore(recipientEmail, subject, pollTitle, timeWindow, location, meetLink, attachmentPath, description, pollLink, true);
    }

    // ==============================================================
    // ⚙️ HÀM CỐT LÕI (XÂY DỰNG HTML BẰNG KỸ THUẬT TABLE LỒNG TABLE)
    // ==============================================================
    private static boolean sendEmailCore(String recipientEmail, String subject, String title, String timeStr, String location, String meetLink, String attachmentPath, String description, String pollLink, boolean isPoll) {
        Session session = createMailSession();
        if (session == null) {
            return false;
        }

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, MAIL_FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            
            // 👇 CHỈNH SỬA TẠI ĐÂY: Đổi icon biểu đồ thành icon ghim đỏ ở ngoài hộp thư đến 👇
            message.setSubject(subject.replace("📊", "📌"));
            
            // Cấu hình nội dung khác nhau cho Poll và Event
            String headerText = isPoll ? "✦ Bạn nhận được một yêu cầu khảo sát lịch học ✦" : "✦ Bạn có một lời mời tham gia lịch trình mới ✦";
            
            // Luôn dùng icon ghim màu đỏ cho tiêu đề bên trong thư
            String titleIcon = "https://i.postimg.cc/7hsFDZrs/gim.png";

            StringBuilder html = new StringBuilder();
            
            // Khai báo HTML cơ bản để tối ưu hiển thị trên di động
            html.append("<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>");
            html.append("<body style='margin:0; padding:0; background-color:#F3F4F6; font-family: Arial, sans-serif;'>");
            
            // --- 1. NỀN NGOÀI CÙNG ---
            html.append("<table width='100%' bgcolor='#F3F4F6' cellpadding='0' cellspacing='0' border='0'><tr><td align='center' style='padding: 40px 10px;'>");
            
            // Bọc bằng thẻ của Outlook để căn giữa
            html.append("");
            
            // --- 2. THẺ CARD TRẮNG CHÍNH (Chống vỡ giao diện) ---
            html.append("<table width='100%' style='max-width: 600px; background-color: #FFFFFF; border-radius: 16px; overflow: hidden; border: 1px solid #E5E7EB; box-shadow: 0 4px 15px rgba(0,0,0,0.05);' cellpadding='0' cellspacing='0' border='0'>");
            
            // --- 3. HEADER VỚI LOGO & GRADIENT (NẰM TRONG CARD) ---
            html.append("<tr><td align='center' style='background-color: #F8FAFC; background: linear-gradient(135deg, #F5F3FF, #E0E7FF); padding: 35px 20px 25px 20px; border-bottom: 1px solid #F1F5F9;'>");
            html.append("<img src='").append(LOGO_URL).append("' width='72' height='72' style='display: block; margin-bottom: 15px;'>");
            html.append("<h1 style='color: #1E1B4B; margin: 0; font-size: 26px;'>TutorHub Enterprise</h1>");
            html.append("<p style='color: #8B5CF6; font-size: 14px; margin: 8px 0 0 0;'>").append(headerText).append("</p>");
            html.append("</td></tr>");
            
            // --- 4. BODY CHÍNH CỦA EMAIL ---
            html.append("<tr><td style='padding: 30px 25px;'>");
            
            // 4.1. Tiêu đề sự kiện 
            html.append("<table width='100%' cellpadding='0' cellspacing='0' border='0'><tr>");
            html.append("<td width='45' valign='middle'><img src='").append(titleIcon).append("' width='36'></td>");
            html.append("<td valign='middle' style='padding-left: 10px;'><h2 style='margin: 0; color: #0F172A; font-size: 22px;'>").append(title).append("</h2></td>");
            html.append("</tr></table>");
            
            html.append("<hr style='border: 0; border-top: 1px solid #F1F5F9; margin: 25px 0 25px 0;'>");
            
            // 4.2. CẤU TRÚC GRID: THỜI GIAN & ĐỊA ĐIỂM (Bảng chia 2 cột bo góc)
            html.append("<table width='100%' cellpadding='0' cellspacing='0' border='0' style='margin-bottom: 20px;'><tr>");
            
            // Cột 1: Thời gian (background xám bo góc)
            html.append("<td width='48%' valign='top' bgcolor='#F8FAFC' style='padding: 15px; border-radius: 12px; border: 1px solid #F1F5F9;'>");
            html.append("<table width='100%' cellpadding='0' cellspacing='0' border='0'><tr>");
            html.append("<td width='36' valign='top'><img src='https://img.icons8.com/ios-filled/50/6D28D9/clock--v1.png' width='18' style='background-color:#EDE9FE; border-radius:50%; padding: 8px;'></td>");
            html.append("<td style='padding-left: 12px;'><p style='margin: 0; font-size: 13px; color: #1E293B; font-weight: bold;'>Khung giờ đề xuất:</p>");
            html.append("<p style='margin: 4px 0 0 0; font-size: 14px; color: #475569;'>").append(timeStr).append("</p></td>");
            html.append("</tr></table></td>");
            
            html.append("<td width='4%'></td>"); // Khoảng trống giữa 2 cột (Spacer)
            
            // Cột 2: Địa điểm (background xám bo góc)
            html.append("<td width='48%' valign='top' bgcolor='#F8FAFC' style='padding: 15px; border-radius: 12px; border: 1px solid #F1F5F9;'>");
            if (location != null && !location.isEmpty()) {
                html.append("<table width='100%' cellpadding='0' cellspacing='0' border='0'><tr>");
                html.append("<td width='36' valign='top'><img src='https://img.icons8.com/ios-filled/50/6D28D9/marker.png' width='18' style='background-color:#EDE9FE; border-radius:50%; padding: 8px;'></td>");
                html.append("<td style='padding-left: 12px;'><p style='margin: 0; font-size: 13px; color: #1E293B; font-weight: bold;'>Địa điểm dự kiến:</p>");
                html.append("<p style='margin: 4px 0 0 0; font-size: 14px; color: #475569;'>").append(location).append("</p>");
                try {
                    String mapLink = "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(location, "UTF-8");
                    html.append("<p style='margin: 6px 0 0 0;'><a href='").append(mapLink).append("' style='color: #10B981; text-decoration: none; font-size: 12px; font-weight: bold;'>🗺️ Xem trên bản đồ (Google Maps)</a></p>");
                } catch (Exception ignored) {}
                html.append("</td></tr></table>");
            } else {
                 html.append("<p style='margin: 0; color: #94A3B8; font-size: 13px; text-align: center; padding-top: 10px;'>Chưa cập nhật địa điểm</p>");
            }
            html.append("</td></tr></table>");
            
            // 4.3. KHỐI MÔ TẢ (Nền tím nhạt bo góc)
            if (description != null && !description.isEmpty()) {
                html.append("<table width='100%' bgcolor='#F8FAFC' cellpadding='0' cellspacing='0' border='0' style='border-radius: 12px; border: 1px solid #F1F5F9; margin-bottom: 20px;'><tr><td style='padding: 18px 20px;'>");
                html.append("<table width='100%' cellpadding='0' cellspacing='0' border='0'><tr>");
                html.append("<td width='36' valign='top'><img src='https://img.icons8.com/ios-filled/50/FFFFFF/document--v1.png' width='18' style='background-color:#8B5CF6; border-radius:50%; padding: 8px;'></td>");
                html.append("<td style='padding-left: 12px;'><p style='margin: 0 0 5px 0; font-size: 14px; color: #1E293B; font-weight: bold;'>Mô tả:</p>");
                html.append("<p style='margin: 0; font-size: 14px; color: #475569; line-height: 1.5;'>").append(description.replace("\n", "<br>")).append("</p></td>");
                html.append("</tr></table></td></tr></table>");
            }
            
            // 4.4. KHỐI VIDEO CALL 
            if (meetLink != null && !meetLink.isEmpty()) {
                if (isPoll) {
                    // Mẫu 1 (Poll): Video call màu tím nhạt giống thiết kế
                    html.append("<table width='100%' bgcolor='#F5F3FF' cellpadding='0' cellspacing='0' border='0' style='border-radius: 12px; border: 1px solid #EDE9FE; margin-bottom: 25px;'><tr><td style='padding: 18px 20px;'>");
                    html.append("<table width='100%' cellpadding='0' cellspacing='0' border='0'><tr>");
                    html.append("<td width='36' valign='top'><img src='https://img.icons8.com/ios-filled/50/FFFFFF/video-call.png' width='18' style='background-color:#8B5CF6; border-radius:50%; padding: 8px;'></td>");
                    html.append("<td style='padding-left: 12px;'><p style='margin: 0 0 5px 0; font-size: 14px; color: #1E293B; font-weight: bold;'>Link Video Call (Dự kiến):</p>");
                    html.append("<p style='margin: 0;'><a href='").append(meetLink).append("' style='color: #6D28D9; font-size: 14px; text-decoration: underline; word-break: break-all;'>").append(meetLink).append("</a></p></td>");
                    html.append("</tr></table></td></tr></table>");
                } else {
                    // Mẫu 2 (Event): Khối Video Call to
                    html.append("<table width='100%' bgcolor='#F5F3FF' cellpadding='0' cellspacing='0' border='0' style='border-radius: 12px; border: 1px solid #EDE9FE; margin-bottom: 20px;'><tr><td align='center' style='padding: 25px 20px;'>");
                    html.append("<p style='margin: 0 0 15px 0; font-size: 15px; color: #6D28D9; font-weight: bold;'>🎥 Đây là cuộc họp trực tuyến:</p>");
                    html.append("<a href='").append(meetLink).append("' style='background-color: #7C3AED; color: #FFFFFF; padding: 14px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block; font-size: 15px;'>THAM GIA PHÒNG HỌC (VIDEO CALL)</a>");
                    html.append("<p style='margin: 15px 0 0 0; font-size: 12px; color: #64748B;'>Hoặc copy link truy cập:<br><a href='").append(meetLink).append("' style='color: #7C3AED;'>").append(meetLink).append("</a></p>");
                    html.append("</td></tr></table>");
                }
            }

            // --- 5. NÚT ĐIỀN KHẢO SÁT CHÍNH (Chỉ dành cho Mẫu 1 - POLL) ---
            if (isPoll && pollLink != null) {
                html.append("<table width='100%' bgcolor='#FFFFFF' cellpadding='0' cellspacing='0' border='0' style='border-radius: 12px; border: 2px dashed #A78BFA; margin-top: 10px; margin-bottom: 10px;'><tr><td align='center' style='padding: 30px 20px;'>");
                html.append("<img src='https://img.icons8.com/ios-filled/50/FFFFFF/calendar--v1.png' width='22' style='background-color:#8B5CF6; border-radius:8px; padding: 10px; margin-bottom: 15px;'>");
                html.append("<p style='margin: 0 0 20px 0; font-size: 16px; color: #0F172A; font-weight: bold;'>Vui lòng chọn thời gian bạn rảnh rỗi:</p>");
                html.append("<a href='").append(pollLink).append("' style='background-color: #7C3AED; color: #FFFFFF; padding: 16px 40px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block; font-size: 16px;'>📝 ĐIỀN KHẢO SÁT NGAY</a>");
                html.append("<p style='margin: 15px 0 0 0; font-size: 12px; color: #64748B;'>Hoặc copy link:<br><a href='").append(pollLink).append("' style='color: #7C3AED;'>").append(pollLink).append("</a></p>");
                html.append("</td></tr></table>");
            }

            html.append("</td></tr>"); // Hết Body
            
            // --- 6. FOOTER ---
            html.append("<tr><td align='center' style='background-color: #F8FAFC; padding: 20px; border-top: 1px solid #F1F5F9;'>");
            html.append("<p style='margin: 0; font-size: 12px; color: #64748B;'><img src='https://img.icons8.com/fluency/48/approval.png' width='16' style='vertical-align: middle; margin-right: 5px;'> Email này được gửi tự động từ hệ thống quản lý TutorHub.</p>");
            html.append("<p style='margin: 5px 0 0 0; font-size: 12px; color: #64748B;'>Vui lòng không trả lời email này.</p>");
            html.append("</td></tr>");
            
            html.append("</table>"); // Kết thúc Card chính
            
            // Đóng thẻ căn giữa của Outlook
            html.append("");
            html.append("</td></tr></table></body></html>");
            
            // Đóng gói nội dung và đính kèm (nếu có)
            Multipart multipart = new MimeMultipart();
            
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html.toString(), "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);
            
            if (attachmentPath != null && !attachmentPath.isEmpty()) {
                File attachFile = new File(attachmentPath);
                if (attachFile.exists()) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    DataSource source = new FileDataSource(attachFile);
                    attachmentPart.setDataHandler(new DataHandler(source));
                    attachmentPart.setFileName(attachFile.getName()); 
                    multipart.addBodyPart(attachmentPart);
                }
            }

            message.setContent(multipart);
            Transport.send(message); 
            System.out.println("✅ Đã gửi Email thành công tới: " + recipientEmail);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    static boolean sendOTP(String email, String otpCode) {
        if (ServerConfig.isAuthDevMode()) {
            System.out.println("[AUTH DEV] Email OTP for " + email + ": " + otpCode);
            return true;
        }

        String html = "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f6f7fb;font-family:Arial,sans-serif;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f6f7fb;padding:32px 0;'><tr><td align='center'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='max-width:520px;background:#ffffff;border:1px solid #e5e7eb;border-radius:16px;overflow:hidden;'>"
                + "<tr><td style='padding:28px 32px;text-align:center;background:#f8fafc;'>"
                + "<h1 style='margin:0;color:#111827;font-size:24px;'>TutorHub Verification</h1>"
                + "<p style='margin:8px 0 0;color:#64748b;font-size:14px;'>Use this one-time code to continue.</p>"
                + "</td></tr>"
                + "<tr><td style='padding:32px;text-align:center;'>"
                + "<div style='display:inline-block;letter-spacing:10px;font-size:34px;font-weight:700;color:#2563eb;background:#eff6ff;border-radius:14px;padding:18px 22px;'>"
                + otpCode
                + "</div>"
                + "<p style='margin:24px 0 0;color:#475569;font-size:14px;line-height:1.6;'>This code expires in 10 minutes. If you did not request it, you can ignore this email.</p>"
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";

        return sendSimpleHtmlEmail(email, "[TutorHub] Verification code", html);
    }

    private static Session createMailSession() {
        if (ServerConfig.isBlank(SENDER_EMAIL) || ServerConfig.isBlank(APP_PASSWORD)) {
            System.err.println("[EMAIL] Missing SMTP credentials. Set TUTORHUB_SMTP_USER and TUTORHUB_SMTP_PASSWORD.");
            return null;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        return Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });
    }

    private static boolean sendSimpleHtmlEmail(String recipientEmail, String subject, String html) {
        Session session = createMailSession();
        if (session == null) {
            return false;
        }

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, MAIL_FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setContent(html, "text/html; charset=utf-8");
            Transport.send(message);
            return true;
        } catch (Exception e) {
            System.err.println("[EMAIL] Failed to send OTP email: " + e.getMessage());
            return false;
        }
    }
}
