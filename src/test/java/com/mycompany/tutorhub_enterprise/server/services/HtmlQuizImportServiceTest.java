package com.mycompany.tutorhub_enterprise.server.services;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.models.exam.ParsedQuizQuestion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HtmlQuizImportServiceTest {

    @Test
    public void testPayloadValidation() {
        HtmlQuizImportService service = new HtmlQuizImportService();
        
        // Empty payload
        HtmlQuizImportService.ImportResult r1 = service.processImport("", 1);
        assertFalse(r1.success);
        assertEquals("Payload trống.", r1.errorMessage);

        // Invalid JSON
        HtmlQuizImportService.ImportResult r2 = service.processImport("{invalid json", 1);
        assertFalse(r2.success);
        assertTrue(r2.errorMessage.contains("Dữ liệu JSON không hợp lệ"));

        // Missing bankName
        HtmlQuizImportService.ImportRequest req = new HtmlQuizImportService.ImportRequest();
        req.bankName = "";
        HtmlQuizImportService.ImportResult r3 = service.processImport(new Gson().toJson(req), 1);
        assertFalse(r3.success);
        assertEquals("Tên ngân hàng câu hỏi không được để trống.", r3.errorMessage);

        // Empty questions
        req.bankName = "Test Bank";
        req.questions = new ArrayList<>();
        HtmlQuizImportService.ImportResult r4 = service.processImport(new Gson().toJson(req), 1);
        assertFalse(r4.success);
        assertEquals("Không có câu hỏi nào để import.", r4.errorMessage);

        // Max questions limit
        for (int i = 0; i < 501; i++) {
            req.questions.add(new ParsedQuizQuestion());
        }
        HtmlQuizImportService.ImportResult r5 = service.processImport(new Gson().toJson(req), 1);
        assertFalse(r5.success);
        assertTrue(r5.errorMessage.contains("Vượt quá giới hạn"));

        // Question validation failure
        req.questions.clear();
        ParsedQuizQuestion invalidQ = new ParsedQuizQuestion();
        invalidQ.setQuestion(""); // Empty question
        req.questions.add(invalidQ);
        HtmlQuizImportService.ImportResult r6 = service.processImport(new Gson().toJson(req), 1);
        assertFalse(r6.success);
        assertTrue(r6.errorMessage.contains("bị lỗi"));
    }
}
