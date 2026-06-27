import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

public class SchemaChecker {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseManager.getConnection();
            DatabaseMetaData meta = conn.getMetaData();
            
            System.out.println("=== Schema of exam_answers ===");
            ResultSet rs1 = meta.getColumns(null, null, "exam_answers", null);
            while (rs1.next()) {
                System.out.println(rs1.getString("COLUMN_NAME") + " - " + rs1.getString("TYPE_NAME"));
            }
            
            System.out.println("=== Schema of question_banks ===");
            ResultSet rs2 = meta.getColumns(null, null, "question_banks", null);
            while (rs2.next()) {
                System.out.println(rs2.getString("COLUMN_NAME") + " - " + rs2.getString("TYPE_NAME"));
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
