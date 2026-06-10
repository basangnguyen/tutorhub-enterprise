import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.mindrot.jbcrypt.BCrypt;

public class ResetPassword {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_2zR6SambqLdQ";
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            String hashedPass = BCrypt.hashpw("Hocbatrolai293$", BCrypt.gensalt(12));
            try (PreparedStatement pst = c.prepareStatement("UPDATE users SET password_hash = ? WHERE email = ?")) {
                pst.setString(1, hashedPass);
                pst.setString(2, "basangnguyen12@gmail.com");
                int rows = pst.executeUpdate();
                System.out.println("Updated " + rows + " rows.");
            }
        }
    }
}
