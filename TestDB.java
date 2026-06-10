import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestDB {
    public static void main(String[] args) {
        String DB_URL = "jdbc:postgresql://ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String DB_USER = "neondb_owner";
        String DB_PASS = "npg_2zR6SambqLdQ";
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            PreparedStatement pst = conn.prepareStatement("SELECT * FROM users");
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                System.out.println("User: " + rs.getString("email") + " Role: " + rs.getString("role"));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
