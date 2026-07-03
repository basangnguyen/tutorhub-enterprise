// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/service/QuizHubScoringService.java
package com.mycompany.tutorhub_enterprise.client.quizhub.service;

/**
 * Canonical scoring formula for QuizHub Classic Game.
 *
 * Academic accuracy must stay separate from this gamified session score.
 * Power-ups in later phases should affect sessionScore only, never accuracy.
 *
 * Current Phase 0 formula:
 * - wrong or timed out: 0
 * - correct: round((600 + 400 * timeLeftMs / timeLimitMs) * comboMultiplier * powerMultiplier) + streakBonus
 * - combo starts at streak 3, grows by 0.05, and caps at 1.25
 */
public final class QuizHubScoringService {

    public static final int BASE_POINTS = 600;
    public static final int MAX_SPEED_BONUS = 400;
    public static final int STREAK_BONUS_STEP = 30;
    public static final int STREAK_BONUS_CAP = 150;
    public static final int FIRE_AT = 3;
    public static final double COMBO_STEP = 0.05d;
    public static final double COMBO_CAP = 1.25d;

    public GameScoreResult scoreAnswer(boolean correct,
                                       boolean timedOut,
                                       long timeLeftMs,
                                       long timeLimitMs,
                                       int streakAfterAnswer) {
        return scoreAnswer(correct, timedOut, timeLeftMs, timeLimitMs, streakAfterAnswer, 1.0d);
    }

    public GameScoreResult scoreAnswer(boolean correct,
                                       boolean timedOut,
                                       long timeLeftMs,
                                       long timeLimitMs,
                                       int streakAfterAnswer,
                                       double powerMultiplier) {
        if (!correct || timedOut) {
            return new GameScoreResult(0, 0, 0, 1.0d, 1.0d, 0, 0);
        }

        long safeLimit = Math.max(1L, timeLimitMs);
        long safeLeft = Math.max(0L, Math.min(timeLeftMs, safeLimit));
        int speedBonus = (int) Math.round(MAX_SPEED_BONUS * (safeLeft / (double) safeLimit));
        double comboMultiplier = comboMultiplier(streakAfterAnswer);
        double safePowerMultiplier = Math.max(1.0d, powerMultiplier);
        int scoreBeforeFlatBonus = (int) Math.round((BASE_POINTS + speedBonus) * comboMultiplier * safePowerMultiplier);
        int streakBonus = streakBonus(streakAfterAnswer);
        int sessionScore = scoreBeforeFlatBonus + streakBonus;

        return new GameScoreResult(BASE_POINTS, speedBonus, scoreBeforeFlatBonus, comboMultiplier, safePowerMultiplier, streakBonus, sessionScore);
    }

    public static double comboMultiplier(int streakAfterAnswer) {
        if (streakAfterAnswer < FIRE_AT) {
            return 1.0d;
        }
        return Math.min(1.0d + (streakAfterAnswer - FIRE_AT + 1) * COMBO_STEP, COMBO_CAP);
    }

    public static int streakBonus(int streakAfterAnswer) {
        if (streakAfterAnswer < FIRE_AT) {
            return 0;
        }
        return Math.min((streakAfterAnswer - FIRE_AT + 1) * STREAK_BONUS_STEP, STREAK_BONUS_CAP);
    }

    public static final class GameScoreResult {
        private final int baseScore;
        private final int speedBonus;
        private final int scoreBeforeFlatBonus;
        private final double comboMultiplier;
        private final double powerMultiplier;
        private final int streakBonus;
        private final int sessionScore;

        private GameScoreResult(int baseScore,
                                int speedBonus,
                                int scoreBeforeFlatBonus,
                                double comboMultiplier,
                                double powerMultiplier,
                                int streakBonus,
                                int sessionScore) {
            this.baseScore = baseScore;
            this.speedBonus = speedBonus;
            this.scoreBeforeFlatBonus = scoreBeforeFlatBonus;
            this.comboMultiplier = comboMultiplier;
            this.powerMultiplier = powerMultiplier;
            this.streakBonus = streakBonus;
            this.sessionScore = sessionScore;
        }

        public int getBaseScore() {
            return baseScore;
        }

        public int getSpeedBonus() {
            return speedBonus;
        }

        public int getScoreBeforeFlatBonus() {
            return scoreBeforeFlatBonus;
        }

        public double getComboMultiplier() {
            return comboMultiplier;
        }

        public double getPowerMultiplier() {
            return powerMultiplier;
        }

        public int getStreakBonus() {
            return streakBonus;
        }

        public int getSessionScore() {
            return sessionScore;
        }
    }
}
