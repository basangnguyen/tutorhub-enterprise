package com.mycompany.tutorhub_enterprise.client.quizhub.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuizHubScoringServiceTest {

    private final QuizHubScoringService service = new QuizHubScoringService();

    @Test
    void correctAnswerAtFullTimeScoresOneThousandBeforeStreak() {
        QuizHubScoringService.GameScoreResult result =
                service.scoreAnswer(true, false, 20_000L, 20_000L, 1);

        assertEquals(600, result.getBaseScore());
        assertEquals(400, result.getSpeedBonus());
        assertEquals(1.0d, result.getComboMultiplier(), 0.0001d);
        assertEquals(0, result.getStreakBonus());
        assertEquals(1000, result.getSessionScore());
    }

    @Test
    void correctAnswerAtZeroTimeScoresBaseOnly() {
        QuizHubScoringService.GameScoreResult result =
                service.scoreAnswer(true, false, 0L, 20_000L, 1);

        assertEquals(0, result.getSpeedBonus());
        assertEquals(600, result.getSessionScore());
    }

    @Test
    void wrongAnswerScoresZero() {
        QuizHubScoringService.GameScoreResult result =
                service.scoreAnswer(false, false, 20_000L, 20_000L, 0);

        assertEquals(0, result.getBaseScore());
        assertEquals(0, result.getSpeedBonus());
        assertEquals(0, result.getStreakBonus());
        assertEquals(0, result.getSessionScore());
    }

    @Test
    void timedOutAnswerScoresZero() {
        QuizHubScoringService.GameScoreResult result =
                service.scoreAnswer(true, true, 0L, 20_000L, 1);

        assertEquals(0, result.getSessionScore());
    }

    @Test
    void thirdCorrectStreakAppliesComboAndFlatBonus() {
        QuizHubScoringService.GameScoreResult result =
                service.scoreAnswer(true, false, 20_000L, 20_000L, 3);

        assertEquals(1.05d, result.getComboMultiplier(), 0.0001d);
        assertEquals(30, result.getStreakBonus());
        assertEquals(1080, result.getSessionScore());
    }

    @Test
    void powerMultiplierAffectsSessionScoreOnlyFormula() {
        QuizHubScoringService.GameScoreResult result =
                service.scoreAnswer(true, false, 20_000L, 20_000L, 1, 2.0d);

        assertEquals(1.0d, result.getComboMultiplier(), 0.0001d);
        assertEquals(2.0d, result.getPowerMultiplier(), 0.0001d);
        assertEquals(2_000, result.getSessionScore());
    }
}
