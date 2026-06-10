package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.client.exam.services.NetworkTSEExamService;
import com.mycompany.tutorhub_enterprise.client.exam.models.TSELoginResult;

public class TestLoginHeadless {
    public static void main(String[] args) throws Exception {
        System.out.println("Connecting to network...");
        NetworkManager.getInstance().connect("localhost", 7860);
        
        System.out.println("Testing login with NetworkTSEExamService...");
        NetworkTSEExamService service = new NetworkTSEExamService();
        
        TSELoginResult result = service.login("dev.tse@test.local", "123456").join();
        System.out.println("Login success: " + result.success);
        System.out.println("Login message: " + result.message);
        
        if (result.success && result.context != null) {
            System.out.println("Username: " + result.context.username);
            System.out.println("Token: " + result.context.token);
        }
        
        NetworkManager.getInstance().disconnect();
        System.exit(0);
    }
}
