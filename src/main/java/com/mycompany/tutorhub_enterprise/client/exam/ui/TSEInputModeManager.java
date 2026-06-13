package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Manages the internal TSE typing mode.
 *
 * VIE = internal Vietnamese Telex engine inside JCEF.
 * ENG = raw English typing, no conversion.
 */
public class TSEInputModeManager {

    private static final String RESOURCE_PATH = "tse/tse-vietnamese-input-engine.js";
    private static final String TEST_PANEL_ID = "tse-input-test-panel";

    private String mode = "en";
    private final String engineScript;
    
    private static TSEInputModeManager instance;
    
    public static synchronized TSEInputModeManager getInstance() {
        if (instance == null) {
            instance = new TSEInputModeManager();
        }
        return instance;
    }

    public TSEInputModeManager() {
        this.engineScript = loadEngineScript();
    }
    
    public void setMode(String newMode) {
        this.mode = "vi".equals(newMode) ? "vi" : "en";
        notifyListeners();
    }
    
    private final java.util.List<Runnable> listeners = new java.util.ArrayList<>();
    public void addModeChangeListener(Runnable listener) {
        listeners.add(listener);
    }
    private void notifyListeners() {
        for (Runnable r : listeners) {
            r.run();
        }
    }

    public String getMode() {
        return mode;
    }

    public String getFooterLabel() {
        return "vi".equals(mode) ? "VIE" : "ENG";
    }

    public String toggleMode() {
        mode = "vi".equals(mode) ? "en" : "vi";
        notifyListeners();
        return mode;
    }

    public String injectEngineIntoHtml(String html) {
        String source = html == null ? "" : html;
        String scriptTag = "\n<script id=\"tse-vietnamese-input-engine\">\n" + engineScript + "\n</script>\n";
        if (source.contains("tse-vietnamese-input-engine")) {
            return source;
        }
        int bodyIndex = source.toLowerCase().lastIndexOf("</body>");
        if (bodyIndex >= 0) {
            return source.substring(0, bodyIndex) + scriptTag + source.substring(bodyIndex);
        }
        return source + scriptTag;
    }

    public String injectInputTestPanelIfEnabled(String html) {
        return injectInputTestPanelIfEnabled(html, Boolean.getBoolean("tutorhub.tse.inputTest"));
    }

    public String injectInputTestPanelIfEnabled(String html, boolean inputTestEnabled) {
        String source = html == null ? "" : html;
        if (!inputTestEnabled || source.contains(TEST_PANEL_ID)) {
            return source;
        }

        String panel =
                "\n<div id=\"tse-input-test-panel\" style=\"margin:24px auto;padding:18px 20px;max-width:860px;border:1px solid #dbe3f0;border-radius:12px;background:#ffffff;font-family:Segoe UI,Arial,sans-serif;box-shadow:0 8px 24px rgba(15,23,42,0.08);\">" +
                "<h3 style=\"margin:0 0 8px;color:#0f172a;font-size:18px;\">Ki\u1ec3m tra b\u1ed9 g\u00f5 ti\u1ebfng Vi\u1ec7t</h3>" +
                "<p style=\"margin:0 0 12px;color:#475569;line-height:1.5;\">G\u00f5 th\u1eed: tieengs Vieetj, duowngf, truowngf, baif thi</p>" +
                "<textarea id=\"tse-input-test-textarea\" rows=\"5\" style=\"width:100%;box-sizing:border-box;border:1px solid #cbd5e1;border-radius:10px;padding:12px;font-size:15px;line-height:1.5;resize:vertical;font-family:Segoe UI,Arial,sans-serif;\"></textarea>" +
                "</div>\n";

        int bodyIndex = source.toLowerCase().lastIndexOf("</body>");
        if (bodyIndex >= 0) {
            return source.substring(0, bodyIndex) + panel + source.substring(bodyIndex);
        }
        return source + panel;
    }

    public void applyMode(TSEBrowserPanel browserPanel) {
        if (browserPanel == null) {
            return;
        }
        browserPanel.executeJavaScript(
                "window.__TSE_INPUT_MODE='" + mode + "';" +
                "if (window.TSEVietnameseInput) {" +
                " window.TSEVietnameseInput.setMode('" + mode + "');" +
                "}"
        );
    }

    private String loadEngineScript() {
        try (InputStream in = TSEInputModeManager.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                System.err.println("[TSE_INPUT] Missing JS engine resource: " + RESOURCE_PATH);
                return "window.TSEVietnameseInput={__installed:true,setMode:function(){},getMode:function(){return 'en';},isEnabled:function(){return false;}};";
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            System.err.println("[TSE_INPUT] Failed to load JS engine: " + ex.getMessage());
            return "window.TSEVietnameseInput={__installed:true,setMode:function(){},getMode:function(){return 'en';},isEnabled:function(){return false;}};";
        }
    }
}
