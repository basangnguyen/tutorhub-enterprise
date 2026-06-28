// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/bridge/QuizHubCefRouterHandler.java
package com.mycompany.tutorhub_enterprise.client.quizhub.bridge;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

/**
 * Chuyển tiếp request từ window.cefQuery() (quiz.html / quiz_excel.html) sang QuizHubBridge.
 * Trả về false cho request không khớp prefix nào của QuizHub, để các handler khác (vd SAVE_DEG:) xử lý tiếp.
 */
public class QuizHubCefRouterHandler extends CefMessageRouterHandlerAdapter {

    private final QuizHubBridge bridge;

    public QuizHubCefRouterHandler(QuizHubBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId,
                            String request, boolean persistent, CefQueryCallback callback) {
        String result;
        try {
            result = route(request);
        } catch (Exception e) {
            callback.failure(-1, "QuizHub lỗi xử lý request: " + e.getMessage());
            return true;
        }
        if (result == null) return false;
        callback.success(result);
        return true;
    }

    private String route(String request) {
        if (request.equals("LIST_DECKS:") || request.equals("LIST_DECKS")) return bridge.listDecks();
        if (request.startsWith("GET_DECK:")) return bridge.getDeck(after(request, "GET_DECK:"));
        if (request.startsWith("PREVIEW_QUIZ_ROWS:")) return bridge.previewExcelRows(after(request, "PREVIEW_QUIZ_ROWS:"));
        if (request.startsWith("IMPORT_QUIZ_ROWS:")) return bridge.importExcelRows(after(request, "IMPORT_QUIZ_ROWS:"));
        if (request.startsWith("SAVE_BEST_SCORE:")) {
            String[] p = splitOnce(after(request, "SAVE_BEST_SCORE:"), '|');
            return bridge.saveBestScore(p[0], p[1]);
        }
        if (request.startsWith("GET_BEST_SCORE:")) return bridge.getBestScore(after(request, "GET_BEST_SCORE:"));
        if (request.startsWith("SAVE_ATTEMPT:")) {
            String[] p = splitOnce(after(request, "SAVE_ATTEMPT:"), '|');
            return bridge.saveAttempt(p[0], p[1]);
        }
        if (request.startsWith("GET_ATTEMPTS:")) return bridge.getAttempts(after(request, "GET_ATTEMPTS:"));
        return null;
    }

    private static String after(String s, String prefix) { return s.substring(prefix.length()); }

    private static String[] splitOnce(String s, char sep) {
        int idx = s.indexOf(sep);
        return idx < 0 ? new String[]{s, ""} : new String[]{s.substring(0, idx), s.substring(idx + 1)};
    }
}