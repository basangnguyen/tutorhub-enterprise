import java.sql.*;

public class CheckUserDB {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_2zR6SambqLdQ";
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT id, email, role FROM users WHERE email = 'basangthaonhi@gmail.com'");
            while (rs.next()) {
                System.out.println("User ID: " + rs.getInt("id") + ", Email: " + rs.getString("email") + ", Role: " + rs.getString("role"));
            }
            System.out.println("Done.");
        }
    }
}
