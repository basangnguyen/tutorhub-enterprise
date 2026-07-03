package com.mycompany.tutorhub_enterprise.client.quizhub.service;

import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubAttempt;
import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubReportSummary;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuizHubReportServiceTest {

    @Test
    void summarizeDeckSeparatesAcademicAccuracyAndGameScore() {
        QuizHubAttempt first = new QuizHubAttempt();
        first.setMode("game");
        first.setAccuracyPoints(8);
        first.setMaxAccuracyPoints(10);
        first.setSessionScore(4_500);
        first.setBestStreak(4);
        first.setAverageResponseMs(1_200);
        first.setFinishedAt("2026-07-02T08:00:00Z");

        QuizHubAttempt second = new QuizHubAttempt();
        second.setMode("game");
        second.setAccuracyPoints(5);
        second.setMaxAccuracyPoints(10);
        second.setSessionScore(9_000);
        second.setBestStreak(2);
        second.setAverageResponseMs(800);
        second.setFinishedAt("2026-07-02T09:00:00Z");

        QuizHubAttemptService fakeAttemptService = new QuizHubAttemptService() {
            @Override
            public List<QuizHubAttempt> getAttempts(String deckId) {
                return List.of(first, second);
            }
        };

        QuizHubReportSummary summary = new QuizHubReportService(fakeAttemptService).summarizeDeck("deck-a");

        assertEquals(2, summary.getAttemptCount());
        assertEquals(2, summary.getGameAttemptCount());
        assertEquals(9_000, summary.getBestSessionScore());
        assertEquals(80.0, summary.getBestAccuracyPercent());
        assertEquals(65.0, summary.getAverageAccuracyPercent());
        assertEquals(1_000.0, summary.getAverageResponseMs());
        assertEquals(4, summary.getBestStreak());
        assertEquals("2026-07-02T09:00:00Z", summary.getLastAttemptAt());
    }
}
