package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2SelectionPanelTest {

    @BeforeEach
    public void setUp() {
        System.clearProperty("tse.v2.clientServerNoGradingSubmit.enabled");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.clientServerNoGradingSubmit.enabled");
    }

    @Test
    public void panelRendersQuestionOptionAndInitialProgress() {
        TSEV2ReadOnlyExamPanel panel = new TSEV2ReadOnlyExamPanel(createSafeModel());

        String text = panel.renderSafePanelText().toLowerCase();

        assertTrue(text.contains("safe question one"));
        assertTrue(text.contains("safe option a"));
        assertTrue(text.contains("answered 0 / 2"));
        assertNoForbiddenText(text);
    }

    @Test
    public void radioClickUpdatesAnsweredProgress() {
        TSEV2ReadOnlyExamPanel panel = new TSEV2ReadOnlyExamPanel(createSafeModel());
        JRadioButton firstRadio = findFirstRadio(panel);

        assertNotNull(firstRadio);
        firstRadio.doClick();

        String text = panel.renderSafePanelText().toLowerCase();
        assertTrue(text.contains("answered 1 / 2"));
        assertEquals(1, panel.getSelectionState().getAnsweredCount());
    }

    @Test
    public void radioClickInvokesAutosaveHandlerAndShowsSavedStatus() {
        AtomicInteger autosaveCalls = new AtomicInteger();
        TSEV2ReadOnlyExamPanel panel = new TSEV2ReadOnlyExamPanel(
                createSafeModel(),
                new TSEV2AnswerSelectionState(2),
                (model, state) -> {
                    autosaveCalls.incrementAndGet();
                    assertEquals(1, state.getAnsweredCount());
                    assertEquals(1001, state.getSelectedOption(101).orElseThrow());
                }
        );
        JRadioButton firstRadio = findFirstRadio(panel);

        assertNotNull(firstRadio);
        firstRadio.doClick();

        String text = panel.renderSafePanelText().toLowerCase();
        assertEquals(1, autosaveCalls.get());
        assertTrue(text.contains("encrypted local draft autosave: saved"));
        assertNoForbiddenText(text);
    }

    @Test
    public void panelRestoreSuccessUpdatesRadioProgressAndSafeStatus() {
        TSEV2ReadOnlyExamPanel panel = new TSEV2ReadOnlyExamPanel(
                createSafeModel(),
                new TSEV2AnswerSelectionState(2),
                null,
                (model, state) -> {
                    state.selectOption(101, 1002);
                    return TSEV2ReadOnlyExamPanel.DraftRestoreResult.restored();
                }
        );

        JRadioButton restoredRadio = findRadioContaining(panel, "Safe option B");
        String text = panel.renderSafePanelText().toLowerCase();

        assertNotNull(restoredRadio);
        assertTrue(restoredRadio.isSelected());
        assertTrue(text.contains("answered 1 / 2"));
        assertTrue(text.contains("encrypted draft restored."));
        assertNoForbiddenText(text);
    }

    @Test
    public void autosaveFailureShowsSafeErrorCodeWithoutLeakingDetails() {
        TSEV2ReadOnlyExamPanel panel = new TSEV2ReadOnlyExamPanel(
                createSafeModel(),
                new TSEV2AnswerSelectionState(2),
                (model, state) -> {
                    throw new IllegalArgumentException("sessionToken rawKey plaintext");
                }
        );
        JRadioButton firstRadio = findFirstRadio(panel);

        assertNotNull(firstRadio);
        firstRadio.doClick();

        String text = panel.renderSafePanelText().toLowerCase();
        assertTrue(text.contains("encrypted local draft autosave: failed - validation_error"));
        assertNoForbiddenText(text);
    }

    @Test
    public void panelDoesNotCreateSubmitSaveOrFinishButtons() {
        TSEV2ReadOnlyExamPanel panel = new TSEV2ReadOnlyExamPanel(createSafeModel());

        List<String> buttonTexts = new ArrayList<>();
        collectJButtonTexts(panel, buttonTexts);

        String joined = String.join(" ", buttonTexts).toLowerCase();
        assertFalse(joined.contains("submit"));
        assertFalse(joined.contains("save"));
        assertFalse(joined.contains("finish"));
        assertNoForbiddenText(panel.renderSafePanelText().toLowerCase());
    }

    @Test
    public void panelShowsServerSubmitDryRunButtonOnlyWhenClientFlagEnabled() {
        System.setProperty("tse.v2.clientServerNoGradingSubmit.enabled", "true");
        TSEV2ReadOnlyExamPanel panel = new TSEV2ReadOnlyExamPanel(createSafeModel());

        List<String> buttonTexts = new ArrayList<>();
        collectJButtonTexts(panel, buttonTexts);

        String joined = String.join(" ", buttonTexts);
        assertTrue(joined.contains("Server Submit Dry-run"));
        assertFalse(joined.contains("Final Submit"));
        assertFalse(joined.contains("Submit Now"));
        assertFalse(joined.contains("Finish Exam"));
        assertFalse(joined.contains("Nộp bài thật"));
        assertNoForbiddenText(panel.renderSafePanelText().toLowerCase());
    }

    @Test
    public void sensitiveMarkersBlockSelectionPanelRendering() {
        TSEV2ReadOnlyExamRenderModel model = createSafeModel();
        model.getQuestions().get(0).setContent("safe visible answerKey marker");

        TSEV2ReadOnlyExamPanel panel = new TSEV2ReadOnlyExamPanel(model);
        String text = panel.renderSafePanelText().toLowerCase();

        assertTrue(text.contains("selection prototype unavailable"));
        assertFalse(text.contains("safe visible answerkey marker"));
    }

    private static TSEV2ReadOnlyExamRenderModel createSafeModel() {
        TSEV2ReadOnlyExamRenderModel model = new TSEV2ReadOnlyExamRenderModel();
        model.setExamId(7);
        model.setPaperId(8);
        model.setQuestionCount(2);
        model.setAttemptId("attempt-debug-id");
        model.setDeadlineAt("2026-06-18T10:00:00+07:00");

        TSEV2ReadOnlyQuestionView q1 = new TSEV2ReadOnlyQuestionView();
        q1.setId(101);
        q1.setContent("Safe question one");
        q1.getOptions().add(option(1001, "Safe option A"));
        q1.getOptions().add(option(1002, "Safe option B"));

        TSEV2ReadOnlyQuestionView q2 = new TSEV2ReadOnlyQuestionView();
        q2.setId(102);
        q2.setContent("Safe question two");
        q2.getOptions().add(option(2001, "True"));
        q2.getOptions().add(option(2002, "False"));

        model.getQuestions().add(q1);
        model.getQuestions().add(q2);
        return model;
    }

    private static TSEV2ReadOnlyOptionView option(int id, String content) {
        TSEV2ReadOnlyOptionView option = new TSEV2ReadOnlyOptionView();
        option.setId(id);
        option.setContent(content);
        return option;
    }

    private static JRadioButton findFirstRadio(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JRadioButton) {
                return (JRadioButton) component;
            }
            if (component instanceof Container) {
                JRadioButton found = findFirstRadio((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JRadioButton findRadioContaining(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JRadioButton
                    && ((JRadioButton) component).getText().contains(text)) {
                return (JRadioButton) component;
            }
            if (component instanceof Container) {
                JRadioButton found = findRadioContaining((Container) component, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void collectJButtonTexts(Container container, List<String> texts) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton) {
                String text = ((JButton) component).getText();
                texts.add(text == null ? "" : text);
            }
            if (component instanceof Container) {
                collectJButtonTexts((Container) component, texts);
            }
        }
    }

    private static void assertNoForbiddenText(String text) {
        assertFalse(text.contains("sessiontoken"));
        assertFalse(text.contains("keyb64"));
        assertFalse(text.contains("plaintext"));
        assertFalse(text.contains("score"));
        assertFalse(text.contains("grading"));
        assertFalse(text.contains("iscorrect"));
        assertFalse(text.contains("answerkey"));
        assertFalse(text.contains("correctoption"));
        assertFalse(text.contains("passwordhash"));
        assertFalse(text.contains("password"));
    }
}
