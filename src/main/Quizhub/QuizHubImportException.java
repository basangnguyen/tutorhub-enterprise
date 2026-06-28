// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/importer/QuizHubImportException.java
package com.mycompany.tutorhub_enterprise.client.quizhub.importer;

/**
 * Lỗi CẤU TRÚC file Excel (thiếu sheet, thiếu cột bắt buộc, không có câu hỏi hợp lệ nào...).
 * Khác với QuizHubRowError (lỗi từng dòng, vẫn import được phần còn lại), lỗi này
 * khiến toàn bộ việc import không thể tiếp tục.
 */
public class QuizHubImportException extends RuntimeException {

    public QuizHubImportException(String message) {
        super(message);
    }

    public QuizHubImportException(String message, Throwable cause) {
        super(message, cause);
    }
}