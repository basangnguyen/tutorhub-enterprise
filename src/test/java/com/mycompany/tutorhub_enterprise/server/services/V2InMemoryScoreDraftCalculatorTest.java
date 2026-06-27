package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class V2InMemoryScoreDraftCalculatorTest {

    @Test
    public void testCalculateInternal_AllCorrect() {
        Map<Long, Long> answers = new HashMap<>();
        answers.put(1L, 101L);
        answers.put(2L, 201L);

        Map<Long, Long> answerKey = new HashMap<>();
        answerKey.put(1L, 101L);
        answerKey.put(2L, 201L);

        V2InMemoryScoreDraftCalculator.InternalAggregateResult result =
                V2InMemoryScoreDraftCalculator.calculateInternal(answers, answerKey, 2);

        assertEquals(2, result.correctCount);
        assertEquals(0, result.incorrectCount);
        assertEquals(10.0f, result.rawScore, 0.01f);
        assertEquals(100.0f, result.percentage, 0.01f);
    }

    @Test
    public void testCalculateInternal_PartialCorrect() {
        Map<Long, Long> answers = new HashMap<>();
        answers.put(1L, 101L); // Correct
        answers.put(2L, 202L); // Incorrect

        Map<Long, Long> answerKey = new HashMap<>();
        answerKey.put(1L, 101L);
        answerKey.put(2L, 201L);
        answerKey.put(3L, 301L);

        V2InMemoryScoreDraftCalculator.InternalAggregateResult result =
                V2InMemoryScoreDraftCalculator.calculateInternal(answers, answerKey, 3);

        assertEquals(1, result.correctCount);
        assertEquals(1, result.incorrectCount);
        assertEquals(3.33f, result.rawScore, 0.01f);
        assertEquals(33.33f, result.percentage, 0.01f);
    }

    @Test
    public void testCalculateInternal_EmptyAnswers() {
        Map<Long, Long> answers = new HashMap<>();

        Map<Long, Long> answerKey = new HashMap<>();
        answerKey.put(1L, 101L);

        V2InMemoryScoreDraftCalculator.InternalAggregateResult result =
                V2InMemoryScoreDraftCalculator.calculateInternal(answers, answerKey, 1);

        assertEquals(0, result.correctCount);
        assertEquals(0, result.incorrectCount);
        assertEquals(0.0f, result.rawScore, 0.01f);
        assertEquals(0.0f, result.percentage, 0.01f);
    }
}
