package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.client.exam.services.NetworkTSEExamService;
import com.mycompany.tutorhub_enterprise.client.exam.models.TSEExamConfig;
import com.mycompany.tutorhub_enterprise.client.exam.models.TSELoginResult;
import com.mycompany.tutorhub_enterprise.client.exam.models.TSEStartExamResult;
import java.util.List;

public class TestStartExamHeadless {
    public static void main(String[] args) throws Exception {
        System.out.println("Connecting to network...");
        NetworkManager.getInstance().connect("localhost", 7860);
        
        System.out.println("Testing EXAM_START_REQUEST with NetworkTSEExamService...");
        NetworkTSEExamService service = new NetworkTSEExamService();
        
        TSELoginResult loginRes = service.login("dev.tse@test.local", "123456").join();
        if (loginRes.success) {
            List<TSEExamConfig> configs = service.getConfigList(loginRes.context.userId).join();
            if (!configs.isEmpty()) {
                TSEExamConfig config = configs.get(0);
                System.out.println("Lấy được exam ID: " + config.examId + " - " + config.examTitle);
                
                System.out.println("Gửi request start_exam với password sai (nếu cần)...");
                TSEStartExamResult failRes = service.verifyPasswordAndStart(loginRes.context.userId, config.examId, "wrongpass").join();
                System.out.println("Result failRes: " + failRes.success + " - " + failRes.message);
                
                System.out.println("Gửi request start_exam với password đúng...");
                String correctPass = config.requiresPassword ? "123456" : ""; // Assuming 123456 is correct if requiresPassword
                TSEStartExamResult startRes = service.verifyPasswordAndStart(loginRes.context.userId, config.examId, correctPass).join();
                System.out.println("Result startRes: " + startRes.success + " - " + startRes.message);
                if (startRes.success) {
                    System.out.println("Session ID: " + startRes.sessionId);
                    System.out.println("HTML Content length: " + (startRes.htmlContent != null ? startRes.htmlContent.length() : 0));
                }
            } else {
                System.out.println("Không có exam nào trong DB.");
            }
        } else {
            System.out.println("Đăng nhập thất bại: " + loginRes.message);
        }
        
        NetworkManager.getInstance().disconnect();
        System.exit(0);
    }
}
