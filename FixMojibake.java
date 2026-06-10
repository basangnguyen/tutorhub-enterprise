import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;

public class FixMojibake {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get("D:/Ban_sao_du_an/src/main/java/com/mycompany/tutorhub_enterprise/client/ClassManagerTab.java");
        byte[] bytes = Files.readAllBytes(p);
        String text = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("Original contains Lá»›p: " + text.contains("Lá»›p"));
        
        byte[] cp1252Bytes = text.getBytes("windows-1252");
        String fixed = new String(cp1252Bytes, StandardCharsets.UTF_8);
        System.out.println("Fixed contains Lớp: " + fixed.contains("Lớp"));
        
        // Write it to a temp file to verify
        Files.write(Paths.get("D:/Ban_sao_du_an/temp_test.java"), fixed.getBytes(StandardCharsets.UTF_8));
    }
}
