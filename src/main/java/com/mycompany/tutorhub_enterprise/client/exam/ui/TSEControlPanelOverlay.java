package com.mycompany.tutorhub_enterprise.client.exam.ui;

/**
 * DOM overlays for active Secure Exam controls.
 * Swing modal dialogs and root-level overlays are intentionally avoided because
 * JCEF is a heavyweight browser surface and exam flow must stay non-blocking.
 */
public final class TSEControlPanelOverlay {

    private static final String CONTROL_OVERLAY_ID = "tse-control-panel-overlay";
    private static final String SUBMIT_OVERLAY_ID = "tse-submit-confirm-overlay";

    private TSEControlPanelOverlay() {
    }

    public static void showAbout(
            TSEBrowserPanel browser,
            TSELanguageManager language,
            String buildVersion,
            String examId,
            String sessionId,
            String userId,
            String serverStatus) {

        String body =
                "<div style=\"font-weight:800;font-size:18px;color:#0f172a;margin-bottom:6px;\">" +
                        escapeHtml(language.text("about.product")) +
                "</div>" +
                "<div style=\"color:#64748b;line-height:1.5;margin-bottom:18px;\">" +
                        escapeHtml(language.text("about.description")) +
                "</div>" +
                aboutRow(language.text("about.build"), buildVersion) +
                aboutRow(language.text("about.examId"), examId) +
                aboutRow(language.text("about.sessionId"), sessionId) +
                aboutRow(language.text("about.userId"), emptyToUnavailable(userId, language)) +
                aboutRow(language.text("about.serverStatus"), emptyToUnavailable(serverStatus, language));

        showPanel(browser, CONTROL_OVERLAY_ID, language.text("about.title"), body, language.text("close"));
    }

    public static void showExitBlocked(TSEBrowserPanel browser, TSELanguageManager language) {
        String body =
                "<div style=\"color:#475569;line-height:1.6;font-size:15px;\">" +
                        escapeHtml(language.text("exit.blocked.message")) +
                "</div>";
        showPanel(browser, CONTROL_OVERLAY_ID, language.text("exit.blocked.title"), body, language.text("close"));
    }

    public static void showError(TSEBrowserPanel browser, String title, String message, String closeText) {
        String body =
                "<div style=\"color:#475569;line-height:1.6;font-size:15px;\">" +
                        escapeHtml(message) +
                "</div>";
        showPanel(browser, CONTROL_OVERLAY_ID, title, body, closeText);
    }

    public static void showSubmitConfirm(TSEBrowserPanel browser, TSELanguageManager language) {
        if (browser == null) {
            return;
        }

        String title = escapeHtml(language.text("submit.confirm.title"));
        String message = escapeHtml(language.text("submit.confirm.message"));
        String cancel = escapeHtml(language.text("submit.cancel"));
        String confirm = escapeHtml(language.text("submit.confirm"));
        String processingTitle = escapeHtml(language.text("submit.processing.title"));
        String processingMessage = escapeHtml(language.text("submit.processing.message"));

        String cardHtml =
                "<div id=\"tse-submit-confirm-card-content\" style=\"background:#ffffff;padding:30px;border-radius:10px;max-width:420px;text-align:center;box-shadow:0 18px 50px rgba(15,23,42,0.22);font-family:Segoe UI,Arial,sans-serif;\">" +
                "<h2 style=\"margin:0;color:#1e293b;font-size:20px;font-weight:800;\">" + title + "</h2>" +
                "<p style=\"color:#475569;margin:16px 0 24px;line-height:1.5;font-size:14px;\">" + message + "</p>" +
                "<div style=\"display:flex;justify-content:center;gap:16px;\">" +
                "<button id=\"tse-cancel-submit\" style=\"padding:10px 20px;border:none;border-radius:6px;background:#e2e8f0;color:#0f172a;font-weight:700;cursor:pointer;\">" + cancel + "</button>" +
                "<button id=\"tse-confirm-submit\" style=\"padding:10px 20px;border:none;border-radius:6px;background:#dc2626;color:white;font-weight:800;cursor:pointer;\">" + confirm + "</button>" +
                "</div></div>";

        String processingHtml =
                "<h2 style=\"margin:0;color:#1e293b;font-size:20px;font-weight:800;\">" + processingTitle + "</h2>" +
                "<p style=\"color:#475569;margin:16px 0 0;\">" + processingMessage + "</p>";

        String script =
                "var existing=document.getElementById('" + SUBMIT_OVERLAY_ID + "');" +
                "if(existing) existing.remove();" +
                "var overlay=document.createElement('div');" +
                "overlay.id='" + SUBMIT_OVERLAY_ID + "';" +
                "overlay.style.position='fixed';" +
                "overlay.style.inset='0';" +
                "overlay.style.zIndex='2147483647';" +
                "overlay.style.background='rgba(15,23,42,0.48)';" +
                "overlay.style.display='flex';" +
                "overlay.style.alignItems='center';" +
                "overlay.style.justifyContent='center';" +
                "overlay.innerHTML='" + escapeJs(cardHtml) + "';" +
                "document.body.appendChild(overlay);" +
                "document.getElementById('tse-cancel-submit').onclick=function(){" +
                " if(overlay.parentNode) overlay.parentNode.removeChild(overlay);" +
                " window.cefQuery&&window.cefQuery({request:'SUBMIT_PAYLOAD:CANCEL_FINAL_SUBMIT'});" +
                "};" +
                "document.getElementById('tse-confirm-submit').onclick=function(){" +
                " document.getElementById('tse-submit-confirm-card-content').innerHTML='" + escapeJs(processingHtml) + "';" +
                " window.cefQuery&&window.cefQuery({request:'SUBMIT_PAYLOAD:CONFIRM_FINAL_SUBMIT'});" +
                "};";

        browser.executeJavaScript(script);
    }

    private static void showPanel(TSEBrowserPanel browser, String id, String title, String bodyHtml, String closeText) {
        if (browser == null) {
            return;
        }

        String cardHtml =
                "<div style=\"background:#ffffff;width:min(480px,calc(100vw - 48px));border-radius:12px;box-shadow:0 18px 50px rgba(15,23,42,0.22);font-family:Segoe UI,Arial,sans-serif;overflow:hidden;\">" +
                "<div style=\"display:flex;align-items:center;justify-content:space-between;padding:18px 22px;border-bottom:1px solid #e5e7eb;\">" +
                "<div style=\"font-size:18px;font-weight:800;color:#0f172a;\">" + escapeHtml(title) + "</div>" +
                "<button id=\"" + id + "-close-x\" style=\"width:32px;height:32px;border:none;border-radius:8px;background:#f1f5f9;color:#334155;font-size:18px;font-weight:700;cursor:pointer;\">x</button>" +
                "</div>" +
                "<div style=\"padding:22px;\">" + bodyHtml + "</div>" +
                "<div style=\"display:flex;justify-content:flex-end;padding:0 22px 20px;\">" +
                "<button id=\"" + id + "-close\" style=\"padding:10px 18px;border:none;border-radius:8px;background:#1e3a7a;color:#ffffff;font-weight:800;cursor:pointer;\">" + escapeHtml(closeText) + "</button>" +
                "</div></div>";

        String script =
                "var existing=document.getElementById('" + id + "');" +
                "if(existing) existing.remove();" +
                "var overlay=document.createElement('div');" +
                "overlay.id='" + id + "';" +
                "overlay.style.position='fixed';" +
                "overlay.style.inset='0';" +
                "overlay.style.zIndex='2147483646';" +
                "overlay.style.background='rgba(15,23,42,0.42)';" +
                "overlay.style.display='flex';" +
                "overlay.style.alignItems='center';" +
                "overlay.style.justifyContent='center';" +
                "overlay.innerHTML='" + escapeJs(cardHtml) + "';" +
                "document.body.appendChild(overlay);" +
                "var close=function(){if(overlay.parentNode) overlay.parentNode.removeChild(overlay);};" +
                "document.getElementById('" + id + "-close').onclick=close;" +
                "document.getElementById('" + id + "-close-x').onclick=close;";

        browser.executeJavaScript(script);
    }

    private static String aboutRow(String label, String value) {
        return "<div style=\"display:flex;gap:16px;align-items:flex-start;padding:9px 0;border-top:1px solid #f1f5f9;\">" +
                "<div style=\"width:140px;color:#64748b;font-size:13px;\">" + escapeHtml(label) + "</div>" +
                "<div style=\"flex:1;color:#0f172a;font-size:13px;font-weight:700;word-break:break-word;\">" + escapeHtml(value) + "</div>" +
                "</div>";
    }

    private static String emptyToUnavailable(String value, TSELanguageManager language) {
        if (value == null || value.trim().isEmpty()) {
            return language.text("unknown");
        }
        return value;
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String escapeJs(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "");
    }
}
