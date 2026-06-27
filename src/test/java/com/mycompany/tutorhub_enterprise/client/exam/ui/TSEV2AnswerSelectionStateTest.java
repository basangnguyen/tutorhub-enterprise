package com.mycompany.tutorhub_enterprise.client.exam.ui;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2AnswerSelectionStateTest {

    @Test
    public void initialAnsweredCountIsZero() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(3);

        assertEquals(0, state.getAnsweredCount());
        assertEquals(3, state.getTotalQuestionCount());
        assertTrue(state.snapshot().isEmpty());
    }

    @Test
    public void selectOptionIncreasesAnsweredCount() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);

        state.selectOption(10, 100);

        assertEquals(1, state.getAnsweredCount());
        assertEquals(100, state.getSelectedOption(10).orElseThrow());
    }

    @Test
    public void changingOptionForSameQuestionDoesNotIncreaseAnsweredCount() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);

        state.selectOption(10, 100);
        state.selectOption(10, 101);

        assertEquals(1, state.getAnsweredCount());
        assertEquals(101, state.getSelectedOption(10).orElseThrow());
    }

    @Test
    public void clearSelectionDecreasesAnsweredCount() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);
        state.selectOption(10, 100);
        state.selectOption(11, 200);

        state.clearSelection(10);

        assertEquals(1, state.getAnsweredCount());
        assertTrue(state.getSelectedOption(10).isEmpty());
        assertEquals(200, state.getSelectedOption(11).orElseThrow());
    }

    @Test
    public void snapshotContainsOnlyQuestionToOptionIds() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(1);
        state.selectOption(10, 100);

        Map<Integer, Integer> snapshot = state.snapshot();
        String text = snapshot.toString().toLowerCase();

        assertEquals(Map.of(10, 100), snapshot);
        assertFalse(text.contains("secret"));
        assertFalse(text.contains("sessiontoken"));
        assertFalse(text.contains("keyb64"));
        assertFalse(text.contains("iscorrect"));
        assertFalse(text.contains("answerkey"));
        assertFalse(text.contains("correctoption"));
    }
}
