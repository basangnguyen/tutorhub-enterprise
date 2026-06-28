// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/storage/QuizHubJsonStorage.java
package com.mycompany.tutorhub_enterprise.client.quizhub.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Tiện ích đọc/ghi JSON local dùng chung cho các Service. Lỗi I/O được bọc thành RuntimeException. */
public final class QuizHubJsonStorage {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private QuizHubJsonStorage() {
    }

    public static void writeJson(Path file, Object value) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            String json = GSON.toJson(value);
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Không ghi được file: " + file, e);
        }
    }

    public static <T> T readJson(Path file, Class<T> type) {
        if (!Files.isRegularFile(file)) return null;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return GSON.fromJson(json, type);
        } catch (IOException e) {
            throw new RuntimeException("Không đọc được file: " + file, e);
        }
    }

    public static <T> T readJson(Path file, Type type) {
        if (!Files.isRegularFile(file)) return null;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return GSON.fromJson(json, type);
        } catch (IOException e) {
            throw new RuntimeException("Không đọc được file: " + file, e);
        }
    }
}