package com.mycompany.tutorhub_enterprise.client.exam.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.swing.*;
import java.awt.*;

public class TSEV2ChildDebugSummaryPanelTest {

    // Helper to extract text recursively from a container
    private String extractText(Container container) {
        StringBuilder sb = new StringBuilder();
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel) {
                sb.append(((JLabel) c).getText()).append("\n");
            } else if (c instanceof JTextArea) {
                sb.append(((JTextArea) c).getText()).append("\n");
            } else if (c instanceof Container) {
                sb.append(extractText((Container) c));
            }
        }
        return sb.toString();
    }

    @Test
    public void testRenderSuccessResultNoCrash() {
        TSEV2ChildDebugLoadResult result = new TSEV2ChildDebugLoadResult();
        result.setSuccess(true);
        result.setMetaLoaded(true);
        result.setKeyFetched(true);
        result.setHashVerified(true);
        result.setDecrypted(true);
        result.setParsed(true);
        result.setSecurityValidated(true);
        result.setExamId(101);
        result.setPaperId(202);
        result.setAttemptId("303");
        result.setQuestionCount(50);
        result.setTotalScore(100.0f);
        result.setDeadlineAt(String.valueOf(System.currentTimeMillis() + 3600000));
        result.setPackageHash("testHash1234");

        TSEV2ChildDebugSummaryPanel panel = new TSEV2ChildDebugSummaryPanel(result);
        assertNotNull(panel);
        
        String text = extractText(panel);
        assertTrue(text.contains("SUCCESS"));
        assertTrue(text.contains("101"));
        assertTrue(text.contains("202"));
        assertTrue(text.contains("303"));
        assertTrue(text.contains("50"));
        assertTrue(text.contains("testHash1234"));
    }

    @Test
    public void testRenderFailResultNoCrash() {
        TSEV2ChildDebugLoadResult result = new TSEV2ChildDebugLoadResult();
        result.setSuccess(false);
        result.setErrorCode("ERROR_HASH_MISMATCH");
        result.setErrorMessage("SHA-256 hash does not match.");

        TSEV2ChildDebugSummaryPanel panel = new TSEV2ChildDebugSummaryPanel(result);
        assertNotNull(panel);

        String text = extractText(panel);
        assertTrue(text.contains("FAIL"));
        assertTrue(text.contains("ERROR_HASH_MISMATCH"));
        assertTrue(text.contains("SHA-256 hash does not match"));
    }

    @Test
    public void testSecurityBlockerInText() {
        TSEV2ChildDebugLoadResult result = new TSEV2ChildDebugLoadResult();
        result.setSuccess(true);

        TSEV2ChildDebugSummaryPanel panel = new TSEV2ChildDebugSummaryPanel(result);
        String text = extractText(panel).toLowerCase();

        // Ensure no sensitive keywords are hardcoded or accidentally leaked by the result obj
        assertFalse(text.contains("keyb64"));
        assertFalse(text.contains("secretkey"));
        assertFalse(text.contains("sessiontoken"));
        assertFalse(text.contains("plaintext"));
        assertFalse(text.contains("iscorrect"));
        assertFalse(text.contains("answerkey"));
        assertFalse(text.contains("correctoption"));
        assertFalse(text.contains("password"));
    }
}
