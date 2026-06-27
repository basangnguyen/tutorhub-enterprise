package com.mycompany.tutorhub_enterprise.server;

import org.mindrot.jbcrypt.BCrypt;

public class HashTest {
    public static void main(String[] args) {
        String password = "123456";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        System.out.println("GENERATED_HASH=" + hash);
        boolean match = BCrypt.checkpw(password, hash);
        System.out.println("MATCH_RESULT=" + match);
        
        // Let's connect to Neon DB and update it directly
        try {
            java.sql.Connection conn = DatabaseManager.getConnection();
            java.sql.PreparedStatement pst = conn.prepareStatement(
                "UPDATE users SET password_hash = ?, role = 'STUDENT', status = 'ACTIVE' WHERE email = 'student.test.safe@tutorhub.local'"
            );
            pst.setString(1, hash);
            int rows = pst.executeUpdate();
            System.out.println("ROWS_UPDATED=" + rows);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
