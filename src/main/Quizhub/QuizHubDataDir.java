// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/storage/QuizHubDataDir.java
package com.mycompany.tutorhub_enterprise.client.quizhub.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Quản lý thư mục dữ liệu local của QuizHub: %APPDATA%/TutorHub/quizhub, fallback ~/.tutorhub/quizhub. */
public final class QuizHubDataDir {

    private QuizHubDataDir() {
    }

    public static Path getBaseDir() {
        Path base = resolveBaseDir();
        ensureDir(base);
        return base;
    }

    public static Path getDecksDir() {
        Path dir = getBaseDir().resolve("decks");
        ensureDir(dir);
        return dir;
    }

    public static Path getAttemptsDir() {
        Path dir = getBaseDir().resolve("attempts");
        ensureDir(dir);
        return dir;
    }

    public static Path getBestScoresFile() {
        return getBaseDir().resolve("best-scores.json");
    }

    private static Path resolveBaseDir() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "TutorHub", "quizhub");
        }
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".tutorhub", "quizhub");
    }

    private static void ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Không tạo được thư mục dữ liệu QuizHub: " + dir, e);
        }
    }
}