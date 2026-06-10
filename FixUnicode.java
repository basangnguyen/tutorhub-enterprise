import java.nio.file.*;
import java.util.regex.*;

public class FixUnicode {
    public static void main(String[] args) throws Exception {
        String path = "D:/TutorHub_Maven/src/main/java/com/mycompany/tutorhub_enterprise/client/ScheduleTab.java";
        String content = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
        Matcher m = Pattern.compile("\\\\\\\\u([0-9a-fA-F]{4})").matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int code = Integer.parseInt(m.group(1), 16);
            m.appendReplacement(sb, Matcher.quoteReplacement(Character.toString((char) code)));
        }
        m.appendTail(sb);
        Files.write(Paths.get(path), sb.toString().getBytes("UTF-8"));
        System.out.println("Done!");
    }
}
