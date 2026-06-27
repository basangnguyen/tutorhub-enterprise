package com.mycompany.tutorhub_enterprise.server.services;

import java.util.Map;

/**
 * Pure utility calculating raw aggregate stats internally based on parsed answers and answer keys.
 * Does NOT expose per-question results or save drafts to DB.
 */
public class V2InMemoryScoreDraftCalculator {

    public static class InternalAggregateResult {
        public int correctCount;
        public int incorrectCount;
        public int totalQuestions;
        public float rawScore;
        public float percentage;
    }

    public static InternalAggregateResult calculateInternal(
            Map<Long, Long> answers,
            Map<Long, Long> answerKey,
            int paperTotalQuestions) {

        InternalAggregateResult result = new InternalAggregateResult();
        result.totalQuestions = paperTotalQuestions;
        
        if (answerKey == null || answerKey.isEmpty() || paperTotalQuestions <= 0) {
            return result;
        }

        int correct = 0;
        int incorrect = 0;

        for (Map.Entry<Long, Long> entry : answers.entrySet()) {
            Long questionId = entry.getKey();
            Long selectedOptionId = entry.getValue();

            if (answerKey.containsKey(questionId)) {
                Long correctOptionId = answerKey.get(questionId);
                if (correctOptionId.equals(selectedOptionId)) {
                    correct++;
                } else {
                    incorrect++;
                }
            }
        }

        result.correctCount = correct;
        result.incorrectCount = incorrect;
        // Basic proportional score 10.0 scale
        result.rawScore = ((float) correct / paperTotalQuestions) * 10.0f;
        result.percentage = ((float) correct / paperTotalQuestions) * 100.0f;

        return result;
    }
}
