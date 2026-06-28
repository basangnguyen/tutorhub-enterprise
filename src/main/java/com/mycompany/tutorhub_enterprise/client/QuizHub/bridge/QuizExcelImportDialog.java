// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/bridge/QuizExcelImportDialog.java
package com.mycompany.tutorhub_enterprise.client.quizhub.bridge;

import com.mycompany.tutorhub_enterprise.client.JcefManager;
import com.mycompany.tutorhub_enterprise.client.quizhub.service.QuizHubAttemptService;
import com.mycompany.tutorhub_enterprise.client.quizhub.service.QuizHubDeckService;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefMessageRouter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * JDialog chứa JCEF + Luckysheet để nhập/sửa đề QuizHub — kiến trúc giống ExcelEditorDialog
 */
public class QuizExcelImportDialog extends JDialog {

    private final CefBrowser browser;
    private final CefMessageRouter msgRouter;
    private static File extractAssetsDir = null;

    public QuizExcelImportDialog(Window owner) {
        super(owner, "Nhập đề QuizHub từ Excel", ModalityType.APPLICATION_MODAL);
        setSize(1200, 800);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        QuizHubDeckService deckService = new QuizHubDeckService();
        QuizHubAttemptService attemptService = new QuizHubAttemptService();
        QuizHubBridge bridge = new QuizHubBridge(deckService, attemptService);

        File assetsDir = getExtractedAssetsDir();
        File htmlFile = new File(assetsDir, "quiz_excel.html");
        String htmlUrl = htmlFile.toURI().toString();

        System.out.println("[QUIZ_EXCEL_EDITOR] Loading UI from: " + htmlUrl);
        browser = JcefManager.getClient().createBrowser(htmlUrl, false, false);

        msgRouter = CefMessageRouter.create();
        msgRouter.addHandler(new QuizHubCefRouterHandler(bridge), true);

        JcefManager.getClient().addMessageRouter(msgRouter);
        add(browser.getUIComponent(), BorderLayout.CENTER);
    }

    private static synchronized File getExtractedAssetsDir() {
        if (extractAssetsDir != null && extractAssetsDir.exists()) {
            return extractAssetsDir;
        }
        
        File destDir = new File(System.getProperty("user.home"), ".tutorhub_excel_assets");
        
        try {
            URL url = QuizExcelImportDialog.class.getResource("/quiz_excel.html");
            if (url != null && "jar".equals(url.getProtocol())) {
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                JarFile jar = connection.getJarFile();
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith("luckysheet/") || name.startsWith("luckyexcel/") || name.equals("quiz_excel.html")) {
                        File destFile = new File(destDir, name);
                        if (entry.isDirectory()) {
                            destFile.mkdirs();
                        } else {
                            destFile.getParentFile().mkdirs();
                            try (InputStream is = jar.getInputStream(entry);
                                 FileOutputStream fos = new FileOutputStream(destFile)) {
                                byte[] buffer = new byte[8192];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                }
                            }
                        }
                    }
                }
                System.out.println("[QUIZ_EXCEL_EDITOR] Extracted assets from JAR to: " + destDir.getAbsolutePath());
            } else {
                if (url != null) {
                    File file = new File(url.toURI());
                    extractAssetsDir = file.getParentFile();
                    return extractAssetsDir;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        extractAssetsDir = destDir;
        return destDir;
    }
}