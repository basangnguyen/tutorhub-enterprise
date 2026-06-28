// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/service/QuizHubAttemptService.java
package com.mycompany.tutorhub_enterprise.client.quizhub.service;

import com.google.gson.reflect.TypeToken;
import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubAttempt;
import com.mycompany.tutorhub_enterprise.client.quizhub.storage.QuizHubDataDir;
import com.mycompany.tutorhub_enterprise.client.quizhub.storage.QuizHubJsonStorage;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuizHubAttemptService {

    public QuizHubAttempt saveAttempt(QuizHubAttempt attempt) {
        if (attempt == null) throw new IllegalArgumentException("attempt là null");
        if (attempt.getDeckId() == null || attempt.getDeckId().isBlank()) {
            throw new IllegalArgumentException("attempt thiếu deckId");
        }
        if (attempt.getId() == null || attempt.getId().isBlank()) {
            attempt.setId(UUID.randomUUID().toString());
        }
        if (attempt.getFinishedAt() == null || attempt.getFinishedAt().isBlank()) {
            attempt.setFinishedAt(Instant.now().toString());
        }

        List<QuizHubAttempt> attempts = getAttempts(attempt.getDeckId());
        attempts.removeIf(a -> a.getId() != null && a.getId().equals(attempt.getId()));
        attempts.add(attempt);

        QuizHubJsonStorage.writeJson(attemptsFile(attempt.getDeckId()), attempts);
        return attempt;
    }

    public List<QuizHubAttempt> getAttempts(String deckId) {
        if (deckId == null || deckId.isBlank()) return new ArrayList<>();
        Type listType = new TypeToken<List<QuizHubAttempt>>() {
        }.getType();
        List<QuizHubAttempt> attempts = QuizHubJsonStorage.readJson(attemptsFile(deckId), listType);
        return attempts != null ? attempts : new ArrayList<>();
    }

    public void saveBestScore(String deckId, String json) {
        if (deckId == null || deckId.isBlank()) throw new IllegalArgumentException("deckId là null/rỗng");
        Map<String, String> all = readBestScores();
        all.put(deckId, json);
        QuizHubJsonStorage.writeJson(QuizHubDataDir.getBestScoresFile(), all);
    }

    public String getBestScore(String deckId) {
        if (deckId == null || deckId.isBlank()) return null;
        return readBestScores().get(deckId);
    }

    private Map<String, String> readBestScores() {
        Type mapType = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> map = QuizHubJsonStorage.readJson(QuizHubDataDir.getBestScoresFile(), mapType);
        return map != null ? map : new HashMap<>();
    }

    private Path attemptsFile(String deckId) {
        return QuizHubDataDir.getAttemptsDir().resolve(deckId + "-attempts.json");
    }
}