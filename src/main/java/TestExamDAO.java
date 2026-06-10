import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import com.mycompany.tutorhub_enterprise.server.dao.ExamDAO;
import com.mycompany.tutorhub_enterprise.models.exam.Exam;

public class TestExamDAO {
    public static void main(String[] args) {
        try {
            System.out.println("Starting TestExamDAO...");
            Exam exam = new Exam();
            exam.creatorId = 1; // Assuming 1 exists, but Exam DB has NO foreign key constraint to users right now!
            exam.title = "Test Exam";
            exam.description = "Test Desc";
            exam.durationMins = 60;
            exam.status = "DRAFT";
            
            System.out.println("Inserting exam...");
            int id = ExamDAO.createExam(exam);
            System.out.println("Created exam ID: " + id);
            
            System.out.println("Fetching exams...");
            java.util.List<Exam> exams = ExamDAO.getExamsByCreator(1);
            System.out.println("Exams found: " + exams.size());
            for (Exam e : exams) {
                System.out.println("- " + e.id + ": " + e.title);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
