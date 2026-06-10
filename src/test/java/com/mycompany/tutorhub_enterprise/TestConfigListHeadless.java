package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.client.exam.services.NetworkTSEExamService;
import com.mycompany.tutorhub_enterprise.client.exam.models.TSEExamConfig;
import com.mycompany.tutorhub_enterprise.client.exam.models.TSELoginResult;
import java.util.List;

public class TestConfigListHeadless {
    public static void main(String[] args) throws Exception {
        System.out.println("Connecting to network...");
        NetworkManager.getInstance().connect("localhost", 7860);
        
        System.out.println("Testing TSE_GET_CONFIG_LIST with NetworkTSEExamService...");
        NetworkTSEExamService service = new NetworkTSEExamService();
        
        TSELoginResult loginRes = service.login("dev.tse@test.local", "123456").join();
        if (loginRes.success) {
            List<TSEExamConfig> configs = service.getConfigList(0).join();
            System.out.println("Lấy được " + configs.size() + " exams từ DB.");
            for (TSEExamConfig c : configs) {
                System.out.println(" - " + c.examTitle + " (ID=" + c.examId + ")");
            }
        }
        
        NetworkManager.getInstance().disconnect();
        System.exit(0);
    }
}
