package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * RAM-only debug selection state for V2 child package rendering.
 * This class intentionally stores only questionId -> optionId.
 */
public class TSEV2AnswerSelectionState {

    private final int totalQuestionCount;
    private final Map<Integer, Integer> selectedOptions = new LinkedHashMap<>();

    public TSEV2AnswerSelectionState() {
        this(0);
    }

    public TSEV2AnswerSelectionState(int totalQuestionCount) {
        this.totalQuestionCount = Math.max(0, totalQuestionCount);
    }

    public void selectOption(int questionId, int optionId) {
        selectedOptions.put(questionId, optionId);
    }

    public Optional<Integer> getSelectedOption(int questionId) {
        return Optional.ofNullable(selectedOptions.get(questionId));
    }

    public void clearSelection(int questionId) {
        selectedOptions.remove(questionId);
    }

    public void clearAllSelections() {
        selectedOptions.clear();
    }

    public int getAnsweredCount() {
        return selectedOptions.size();
    }

    public int getTotalQuestionCount() {
        return totalQuestionCount;
    }

    public Map<Integer, Integer> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(selectedOptions));
    }
}
