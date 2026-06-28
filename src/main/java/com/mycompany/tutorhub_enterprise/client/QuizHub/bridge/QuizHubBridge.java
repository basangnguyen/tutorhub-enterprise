// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/bridge/QuizHubBridge.java
package com.mycompany.tutorhub_enterprise.client.quizhub.bridge;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.client.quizhub.importer.QuizHubImportException;
import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubAttempt;
import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubDeck;
import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubDeckSummary;
import com.mycompany.tutorhub_enterprise.client.quizhub.service.QuizHubAttemptService;
import com.mycompany.tutorhub_enterprise.client.quizhub.service.QuizHubDeckService;

import java.nio.file.Paths;
import java.util.List;

/**
 * Bridge để WebView (quiz.html) gọi sang Java — mọi method nhận/trả String (JSON)
 * để tương thích trực tiếp với JSObject của JavaFX WebView.
 * Việc đăng ký "window.quizBridge = ..." sẽ làm ở batch tích hợp QuizHubTab.java sau,
 * KHÔNG nằm trong file này.
 */
public class QuizHubBridge {

    private static final Gson GSON = new Gson();

    private final QuizHubDeckService deckService;
    private final QuizHubAttemptService attemptService;

    public QuizHubBridge(QuizHubDeckService deckService, QuizHubAttemptService attemptService) {
        this.deckService = deckService;
        this.attemptService = attemptService;
    }

    public String listDecks() {
        try {
            List<QuizHubDeckSummary> decks = deckService.listDecks();
            return ok(decks);
        } catch (Exception e) {
            return err("Không lấy được danh sách đề: " + e.getMessage());
        }
    }

    public String previewExcelRows(String rowsJson) {
    try { return ok(deckService.previewExcelRows(rowsJson)); }
    catch (QuizHubImportException e) { return err(e.getMessage()); }
    catch (Exception e) { return err("Đọc dữ liệu thất bại: " + e.getMessage()); }
}

public String importExcelRows(String rowsJson) {
    try { return ok(deckService.importExcelRows(rowsJson)); }
    catch (QuizHubImportException e) { return err(e.getMessage()); }
    catch (Exception e) { return err("Import thất bại: " + e.getMessage()); }
}

    public String getDeck(String deckId) {
        try {
            QuizHubDeck deck = deckService.getDeck(deckId);
            if (deck == null) return err("Không tìm thấy đề: " + deckId);
            return ok(deck);
        } catch (Exception e) {
            return err("Không lấy được đề: " + e.getMessage());
        }
    }

    public String importExcel(String path) {
        try {
            QuizHubDeck deck = deckService.importExcel(Paths.get(path));
            return ok(deck);
        } catch (QuizHubImportException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("Import Excel thất bại: " + e.getMessage());
        }
    }

    public String saveBestScore(String deckId, String json) {
        try {
            attemptService.saveBestScore(deckId, json);
            return ok(null);
        } catch (Exception e) {
            return err("Không lưu được điểm cao nhất: " + e.getMessage());
        }
    }

    public String getBestScore(String deckId) {
        try {
            String raw = attemptService.getBestScore(deckId);
            return okRaw(raw);
        } catch (Exception e) {
            return err("Không lấy được điểm cao nhất: " + e.getMessage());
        }
    }

    public String saveAttempt(String deckId, String json) {
        try {
            QuizHubAttempt attempt = GSON.fromJson(json, QuizHubAttempt.class);
            if (attempt == null) {
                return err("Dữ liệu attempt không hợp lệ (json rỗng hoặc sai định dạng)");
            }
            if (attempt.getDeckId() == null || attempt.getDeckId().isBlank()) {
                attempt.setDeckId(deckId);
            }
            QuizHubAttempt saved = attemptService.saveAttempt(attempt);
            return ok(saved);
        } catch (Exception e) {
            return err("Không lưu được lượt làm bài: " + e.getMessage());
        }
    }

    public String getAttempts(String deckId) {
        try {
            List<QuizHubAttempt> attempts = attemptService.getAttempts(deckId);
            return ok(attempts);
        } catch (Exception e) {
            return err("Không lấy được lịch sử làm bài: " + e.getMessage());
        }
    }

    // ---------- helper định dạng JSON trả về cho JS ----------

    private static String ok(Object data) {
        return GSON.toJson(new BridgeResponse(true, data, null));
    }

    /**
     * Dùng khi "data" đã LÀ một chuỗi JSON có sẵn (best score lưu thô dạng String).
     * Tránh bị Gson encode 2 lần thành chuỗi-trong-chuỗi.
     * Kết quả: {"ok":true,"data": <object hoặc null>} thay vì {"ok":true,"data":"{...}"}.
     */
    private static String okRaw(String rawJsonOrNull) {
        String dataPart = (rawJsonOrNull == null || rawJsonOrNull.isBlank()) ? "null" : rawJsonOrNull;
        return "{\"ok\":true,\"data\":" + dataPart + "}";
    }

    private static String err(String message) {
        return GSON.toJson(new BridgeResponse(false, null, message));
    }

    private static class BridgeResponse {
        final boolean ok;
        final Object data;
        final String message;

        BridgeResponse(boolean ok, Object data, String message) {
            this.ok = ok;
            this.data = data;
            this.message = message;
        }
    }
}