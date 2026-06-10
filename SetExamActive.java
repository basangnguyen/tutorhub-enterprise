import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SetExamActive {
    public static void main(String[] args) {
        String DB_URL = "jdbc:postgresql://ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String DB_USER = "neondb_owner";
        String DB_PASS = "npg_2zR6SambqLdQ";
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            
            // Get the first draft exam
            PreparedStatement getPst = conn.prepareStatement("SELECT id FROM exams WHERE status = 'DRAFT' LIMIT 1");
            ResultSet rs = getPst.executeQuery();
            if (rs.next()) {
                int examId = rs.getInt("id");
                
                String sql = "UPDATE exams SET status = 'ACTIVE' WHERE id = ?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, examId);
                int rows = pst.executeUpdate();
                System.out.println("Set exam " + examId + " to ACTIVE. Updated rows: " + rows);
            } else {
                System.out.println("No DRAFT exams found. Trying to set ANY exam to ACTIVE...");
                PreparedStatement getAny = conn.prepareStatement("SELECT id FROM exams LIMIT 1");
                ResultSet rsAny = getAny.executeQuery();
                if (rsAny.next()) {
                    int examId = rsAny.getInt("id");
                    String sql = "UPDATE exams SET status = 'ACTIVE' WHERE id = ?";
                    PreparedStatement pst = conn.prepareStatement(sql);
                    pst.setInt(1, examId);
                    int rows = pst.executeUpdate();
                    System.out.println("Set exam " + examId + " to ACTIVE. Updated rows: " + rows);
                } else {
                    System.out.println("No exams exist in the database at all.");
                }
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
