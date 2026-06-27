package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;

public class TSEV2ReadOnlyExamPanelTest {

    @Test
    public void testReadOnlyPanel_NoLeakageAndValidContent() {
        TSEV2ReadOnlyExamRenderModel model = new TSEV2ReadOnlyExamRenderModel();
        model.setExamId(2);
        model.setPaperId(2);
        model.setQuestionCount(1);
        
        TSEV2ReadOnlyQuestionView q = new TSEV2ReadOnlyQuestionView();
        q.setId(1);
        q.setContent("SAFE_QUESTION_CONTENT");
        
        TSEV2ReadOnlyOptionView opt = new TSEV2ReadOnlyOptionView();
        opt.setId(1);
        opt.setContent("SAFE_OPTION_CONTENT");
        q.getOptions().add(opt);
        
        model.getQuestions().add(q);

        TSEV2ReadOnlyExamPanel panel = new TSEV2ReadOnlyExamPanel(model);
        String panelText = extractText(panel).toLowerCase();

        // Check if valid content exists
        assertTrue(panelText.contains("2"));
        assertTrue(panelText.contains("2"));
        assertTrue(panelText.contains("safe_question_content"));
        assertTrue(panelText.contains("safe_option_content"));

        // Check for leaks
        assertFalse(panelText.contains("sessiontoken"));
        assertFalse(panelText.contains("keyb64"));
        assertFalse(panelText.contains("plaintext"));
        assertFalse(panelText.contains("iscorrect"));
        assertFalse(panelText.contains("answerkey"));
        assertFalse(panelText.contains("correctoption"));
        assertFalse(panelText.contains("passwordhash"));
        assertFalse(panelText.contains("password"));
    }

    private String extractText(Container container) {
        StringBuilder sb = new StringBuilder();
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                sb.append(((JLabel) comp).getText()).append(" ");
            } else if (comp instanceof JRadioButton) {
                sb.append(((JRadioButton) comp).getText()).append(" ");
            } else if (comp instanceof Container) {
                sb.append(extractText((Container) comp)).append(" ");
            }
        }
        return sb.toString();
    }
}
