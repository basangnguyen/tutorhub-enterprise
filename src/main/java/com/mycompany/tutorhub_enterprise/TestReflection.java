import org.cef.CefClient;
import java.lang.reflect.Method;

public class TestReflection {
    public static void main(String[] args) {
        for (Method m : CefClient.class.getMethods()) {
            if (m.getName().toLowerCase().contains("permission")) {
                System.out.println(m.getName());
            }
        }
    }
}
