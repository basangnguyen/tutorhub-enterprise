import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.mindrot.jbcrypt.BCrypt; 

public class ResetPass {
    public static void main(String[] args) {
        String DB_URL = "jdbc:postgresql://ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String DB_USER = "neondb_owner";
        String DB_PASS = "npg_2zR6SambqLdQ";
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            String sql = "UPDATE users SET password_hash = ? WHERE email = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            String hashedPass = BCrypt.hashpw("123456", BCrypt.gensalt(12));
            pst.setString(1, hashedPass);
            pst.setString(2, "basangthaonhi@gmail.com");
            int rows = pst.executeUpdate();
            System.out.println("Updated rows: " + rows);
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
