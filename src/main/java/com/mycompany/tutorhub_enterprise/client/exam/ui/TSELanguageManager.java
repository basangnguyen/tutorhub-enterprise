package com.mycompany.tutorhub_enterprise.client.exam.ui;

/**
 * Keeps Secure Exam chrome text in one place.
 * TSE chrome is intentionally kept in Vietnamese. The footer VIE/ENG control is
 * an input mode switch, not a UI translation switch.
 */
public class TSELanguageManager {

    public String getLanguageCode() {
        return "vi";
    }

    public String getFooterLabel() {
        return "VIE";
    }

    /**
     * Kept as a no-op for compatibility with older call sites.
     * Input mode is handled by TSEInputModeManager.
     */
    public void toggle() {
    }

    public String text(String key) {
        return vietnameseText(key);
    }

    private String vietnameseText(String key) {
        switch (key) {
            case "submit.button":
                return "Nộp Bài";
            case "timer.remaining":
                return "Thời gian còn lại";
            case "submit.confirm.title":
                return "Xác nhận nộp bài";
            case "submit.confirm.message":
                return "Bạn có chắc chắn muốn nộp bài? Sau khi nộp, bạn không thể chỉnh sửa câu trả lời.";
            case "submit.cancel":
                return "Hủy";
            case "submit.confirm":
                return "Nộp bài";
            case "submit.processing.title":
                return "Đang nộp bài...";
            case "submit.processing.message":
                return "Vui lòng chờ trong giây lát...";
            case "about.title":
                return "Thông tin phần mềm";
            case "about.product":
                return "TutorHub Secure Exam";
            case "about.description":
                return "Môi trường làm bài bảo mật của TutorHub";
            case "about.build":
                return "Phiên bản";
            case "about.examId":
                return "Exam ID";
            case "about.sessionId":
                return "Session ID";
            case "about.userId":
                return "User ID";
            case "about.serverStatus":
                return "Trạng thái máy chủ";
            case "exit.blocked.title":
                return "Không thể thoát khi đang thi";
            case "exit.blocked.message":
                return "Bạn không thể thoát khi bài thi đang diễn ra. Vui lòng nộp bài hoặc liên hệ giám thị.";
            case "close":
                return "Đóng";
            case "refresh.tooltip":
                return "Làm mới";
            case "about.tooltip":
                return "Thông tin phần mềm";
            case "language.tooltip":
                return "Chế độ gõ: VIE bật Telex, ENG tắt";
            case "power.tooltip":
                return "Thoát";
            case "error.submit.title":
                return "Lỗi nộp bài";
            case "error.submit.script":
                return "Không thể thu thập bài làm do lỗi kịch bản web. Vui lòng thử lại.";
            case "error.submit.save":
                return "Lỗi khi lưu file nộp bài";
            case "unknown":
                return "Không khả dụng";
            default:
                return key;
        }
    }
}
