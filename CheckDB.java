import java.sql.*;

public class CheckDB {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_2zR6SambqLdQ";
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM exams");
            while (rs.next()) {
                System.out.println("Exam ID: " + rs.getInt("id") + ", Title: " + rs.getString("title") + ", Creator: " + rs.getInt("creator_id"));
            }
            System.out.println("Done.");
        }
    }
}
