import java.sql.*;

public class CheckDBSchema {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_2zR6SambqLdQ";
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "exams", null);
            while (rs.next()) {
                System.out.println("Col: " + rs.getString("COLUMN_NAME") + " - " + rs.getString("TYPE_NAME"));
            }
        }
    }
}
