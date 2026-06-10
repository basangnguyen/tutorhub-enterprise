package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

/**
 * SeedDevAccount - Tiện ích hỗ trợ tạo tài khoản test (Dev Only).
 * Dùng để bypass việc gửi email OTP trong môi trường thử nghiệm nội bộ.
 * Không đụng chạm vào Auth production.
 */
public class SeedDevAccount {
    public static void main(String[] args) {
        System.out.println("--- Bắt đầu kiểm tra và tạo tài khoản test dev ---");
        
        String email = "dev.tse@test.local";
        String pass = "123456";
        String fullName = "TSE Dev Tester";
        String role = "TUTOR";
        
        try {
            boolean exists = DatabaseManager.isEmailExists(email);
            if (exists) {
                System.out.println("[INFO] Account already exists: " + email);
            } else {
                boolean success = DatabaseManager.registerUser(email, pass, fullName, role);
                if (success) {
                    System.out.println("[SUCCESS] Account created successfully: " + email);
                } else {
                    System.out.println("[ERROR] Failed with reason: Database registerUser returned false");
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed with reason: Exception -> " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("--- Kết thúc quá trình ---");
    }
}
