// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/importer/QuizHubRowError.java
package com.mycompany.tutorhub_enterprise.client.quizhub.importer;

/** Lỗi của 1 dòng cụ thể trong sheet Cau_hoi — dòng này bị bỏ qua, các dòng khác vẫn import được. */
public class QuizHubRowError {

    private int rowNumber; // số dòng hiển thị trong Excel (header = dòng 1)
    private String message;

    public QuizHubRowError() {
    }

    public QuizHubRowError(int rowNumber, String message) {
        this.rowNumber = rowNumber;
        this.message = message;
    }

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}