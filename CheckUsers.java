import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckUsers {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_2zR6SambqLdQ";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT email, password_hash, role FROM users LIMIT 10")) {
            System.out.println("--- USERS IN DATABASE ---");
            while (rs.next()) {
                System.out.println("Email: " + rs.getString("email") + " | Role: " + rs.getString("role"));
            }
        }
    }
}
