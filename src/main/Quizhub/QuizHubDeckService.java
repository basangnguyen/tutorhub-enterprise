// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/service/QuizHubDeckService.java
package com.mycompany.tutorhub_enterprise.client.quizhub.service;

import com.mycompany.tutorhub_enterprise.client.quizhub.importer.QuizHubExcelImportService;
import com.mycompany.tutorhub_enterprise.client.quizhub.importer.QuizHubImportResult;
import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubDeck;
import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubDeckSummary;
import com.mycompany.tutorhub_enterprise.client.quizhub.storage.QuizHubDataDir;
import com.mycompany.tutorhub_enterprise.client.quizhub.storage.QuizHubJsonStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class QuizHubDeckService {

    private final QuizHubExcelImportService importService;

    public QuizHubDeckService() {
        this(new QuizHubExcelImportService());
    }

    public QuizHubDeckService(QuizHubExcelImportService importService) {
        this.importService = importService;
    }

    public List<QuizHubDeckSummary> listDecks() {
        List<QuizHubDeckSummary> result = new ArrayList<>();
        Path decksDir = QuizHubDataDir.getDecksDir();
        try (Stream<Path> files = Files.list(decksDir)) {
            List<Path> jsonFiles = files.filter(p -> p.toString().endsWith(".json")).toList();
            for (Path file : jsonFiles) {
                QuizHubDeck deck = QuizHubJsonStorage.readJson(file, QuizHubDeck.class);
                if (deck != null) result.add(QuizHubDeckSummary.from(deck));
            }
        } catch (IOException e) {
            throw new RuntimeException("Không đọc được danh sách đề trong: " + decksDir, e);
        }
        return result;
    }

    public QuizHubImportResult previewExcelRows(String rowsJson) {
    return importService.parseFromRows(rowsJson);
}

public QuizHubDeck importExcelRows(String rowsJson) {
    QuizHubImportResult result = importService.parseFromRows(rowsJson);
    QuizHubDeck deck = importService.buildDeckFromImport(result);
    return saveDeck(deck);
}

    public QuizHubDeck getDeck(String deckId) {
        if (deckId == null || deckId.isBlank()) return null;
        return QuizHubJsonStorage.readJson(deckFile(deckId), QuizHubDeck.class);
    }

    public QuizHubDeck saveDeck(QuizHubDeck deck) {
        if (deck == null) throw new IllegalArgumentException("deck là null");
        String now = Instant.now().toString();
        if (deck.getId() == null || deck.getId().isBlank()) {
            deck.setId("deck-" + System.currentTimeMillis());
        }
        if (deck.getCreatedAt() == null || deck.getCreatedAt().isBlank()) {
            deck.setCreatedAt(now);
        }
        deck.setUpdatedAt(now);
        QuizHubJsonStorage.writeJson(deckFile(deck.getId()), deck);
        return deck;
    }

    public boolean deleteDeck(String deckId) {
        if (deckId == null || deckId.isBlank()) return false;
        try {
            return Files.deleteIfExists(deckFile(deckId));
        } catch (IOException e) {
            throw new RuntimeException("Không xoá được đề: " + deckId, e);
        }
    }

    /** Parse + tạo deck + lưu thật trong 1 bước. Dùng cho luồng import nhanh (không cần preview). */
    public QuizHubDeck importExcel(Path excelFile) {
        QuizHubImportResult result = importService.parse(excelFile);
        QuizHubDeck deck = importService.buildDeckFromImport(result);
        return saveDeck(deck);
    }

    private Path deckFile(String deckId) {
        return QuizHubDataDir.getDecksDir().resolve(deckId + ".json");
    }
}