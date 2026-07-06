import java.net.URI;

public class TestURI {
    public static void main(String[] args) {
        try {
            String url = "https://hocba299-3-tutorhub-sync.hf.space/livekit/token?room=LESSON_1&username=Ba%20Sang";
            URI uri = URI.create(url);
            System.out.println("URI parsed correctly: " + uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
