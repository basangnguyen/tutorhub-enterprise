public class TestHttp {
    public static void main(String[] args) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("https://hocba299-3-tutorhub-sync.hf.space/livekit/token?room=LESSON_1&username=Guest"))
            .header("Authorization", "Bearer TUTORHUB_SECRET_2026")
            .GET()
            .build();
        java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        System.out.println("STATUS: " + res.statusCode());
        System.out.println("BODY: " + res.body());
    }
}
