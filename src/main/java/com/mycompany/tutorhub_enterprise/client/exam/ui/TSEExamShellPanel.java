package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.*;
import java.awt.*;

import com.mycompany.tutorhub_enterprise.client.exam.models.*;
import com.mycompany.tutorhub_enterprise.client.exam.services.*;

/**
 * TSEExamShellPanel – The main container for the exam taking screen.
 * Contains the Header (Timer/Submit), Center (JCEF / Placeholder), and Footer (Status/Exit).
 */
public class TSEExamShellPanel extends JPanel {

    private final ExamHeaderBar headerBar;
    private final ExamFooterStatusBar footerBar;

    private TSEBrowserPlaceholderPanel placeholderPanel;
    private TSEBrowserPanel browserPanel;
    private final boolean useRealBrowser;

    public TSEExamShellPanel(TSEExamService examService, TSEStartExamResult session, Runnable onSubmit, Runnable onExit) {
        super(new BorderLayout(0, 0));
        setBackground(Color.WHITE);
        this.useRealBrowser = true;

        String title = "Bài thi - Đang diễn ra";
        if (session != null && session.sessionId != null) {
            title = "Bài thi (Session: " + session.sessionId + ")";
        }

        headerBar = new ExamHeaderBar(title, onSubmit);
        footerBar = new ExamFooterStatusBar(onExit);

        add(headerBar, BorderLayout.NORTH);
        add(footerBar, BorderLayout.SOUTH);

        browserPanel = new TSEBrowserPanel();
        add(browserPanel, BorderLayout.CENTER);
        
        if (session != null) {
            if (session.htmlContent != null) {
                browserPanel.loadHtml(session.htmlContent);
            } else if (session.examUrl != null) {
                browserPanel.loadUrl(session.examUrl);
            }
        }
    }

    public TSEBrowserPanel getBrowserPanel() {
        return browserPanel;
    }

    public void cleanup() {
        if (browserPanel != null) {
            browserPanel.cleanup();
        }
    }
}
