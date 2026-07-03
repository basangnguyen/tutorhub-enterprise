package com.mycompany.tutorhub_enterprise.client.quizhub.service;

import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubAttempt;
import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubReportSummary;

import java.util.Comparator;
import java.util.List;

public class QuizHubReportService {

    private final QuizHubAttemptService attemptService;

    public QuizHubReportService() {
        this(new QuizHubAttemptService());
    }

    public QuizHubReportService(QuizHubAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    public QuizHubReportSummary summarizeDeck(String deckId) {
        List<QuizHubAttempt> attempts = attemptService.getAttempts(deckId);
        QuizHubReportSummary summary = new QuizHubReportSummary();
        summary.setDeckId(deckId);
        summary.setAttemptCount(attempts.size());

        if (attempts.isEmpty()) {
            return summary;
        }

        int gameCount = 0;
        int bestSessionScore = 0;
        int bestStreak = 0;
        double accuracySum = 0.0d;
        double bestAccuracy = 0.0d;
        double responseSum = 0.0d;
        int responseCount = 0;

        for (QuizHubAttempt attempt : attempts) {
            double accuracy = accuracyPercent(attempt);
            accuracySum += accuracy;
            bestAccuracy = Math.max(bestAccuracy, accuracy);
            bestSessionScore = Math.max(bestSessionScore, attempt.getSessionScore());
            bestStreak = Math.max(bestStreak, attempt.getBestStreak());
            if ("game".equalsIgnoreCase(attempt.getMode())) {
                gameCount++;
            }
            if (attempt.getAverageResponseMs() > 0) {
                responseSum += attempt.getAverageResponseMs();
                responseCount++;
            }
        }

        attempts.stream()
                .max(Comparator.comparing(a -> a.getFinishedAt() == null ? "" : a.getFinishedAt()))
                .ifPresent(last -> summary.setLastAttemptAt(last.getFinishedAt()));

        summary.setGameAttemptCount(gameCount);
        summary.setBestSessionScore(bestSessionScore);
        summary.setBestAccuracyPercent(round1(bestAccuracy));
        summary.setAverageAccuracyPercent(round1(accuracySum / attempts.size()));
        summary.setAverageResponseMs(responseCount == 0 ? 0.0d : Math.round(responseSum / responseCount));
        summary.setBestStreak(bestStreak);
        return summary;
    }

    private static double accuracyPercent(QuizHubAttempt attempt) {
        if (attempt.getMaxAccuracyPoints() > 0) {
            return attempt.getAccuracyPoints() * 100.0d / attempt.getMaxAccuracyPoints();
        }
        if (attempt.getTotalCount() > 0) {
            return attempt.getCorrectCount() * 100.0d / attempt.getTotalCount();
        }
        return 0.0d;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0d) / 10.0d;
    }
}
