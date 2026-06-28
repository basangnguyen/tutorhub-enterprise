// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/bridge/QuizExcelImportDialog.java
package com.mycompany.tutorhub_enterprise.client.quizhub.bridge;

import com.mycompany.tutorhub_enterprise.client.quizhub.service.QuizHubAttemptService;
import com.mycompany.tutorhub_enterprise.client.quizhub.service.QuizHubDeckService;
import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefMessageRouter;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/**
 * JDialog chứa JCEF + Luckysheet để nhập/sửa đề QuizHub — kiến trúc giống ExcelEditorDialog
 * dùng cho tab Bằng cấp, chỉ khác router xử lý prefix "IMPORT_QUIZ_ROWS:"/"PREVIEW_QUIZ_ROWS:" thay vì "SAVE_DEG:".
 */
public class QuizExcelImportDialog extends JDialog {

    public QuizExcelImportDialog(Window owner, Path quizExcelHtmlAssetPath) {
        super(owner, "Nhập đề QuizHub từ Excel", ModalityType.APPLICATION_MODAL);
        setSize(1200, 800);
        setLocationRelativeTo(owner);

        QuizHubDeckService deckService = new QuizHubDeckService();
        QuizHubAttemptService attemptService = new QuizHubAttemptService();
        QuizHubBridge bridge = new QuizHubBridge(deckService, attemptService);

        CefMessageRouter router = CefMessageRouter.create();
        router.addHandler(new QuizHubCefRouterHandler(bridge), true);

        CefBrowser browser = CefApp.getInstance()
                .createClient()
                .createBrowser(quizExcelHtmlAssetPath.toUri().toString(), false, false);
        browser.getClient().addMessageRouter(router);

        add(browser.getUIComponent(), BorderLayout.CENTER);
    }
}