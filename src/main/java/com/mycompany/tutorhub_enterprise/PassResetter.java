package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

public class PassResetter {
    public static void main(String[] args) {
        boolean success = DatabaseManager.resetPassword("basangthaonhi@gmail.com", "123456");
        System.out.println("Password reset: " + success);
        System.exit(0);
    }
}
