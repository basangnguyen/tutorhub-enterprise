package com.mycompany.tutorhub_enterprise.utils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class AutoUpdater {

    private static final String VERSION_URL = "https://hocba299-3-tutorhub-sync.hf.space/version.json";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 120_000;
    private static final int MAX_DOWNLOAD_ATTEMPTS = 3;
    private static final long MIN_UPDATE_JAR_BYTES = 1024 * 1024;

    public static void checkUpdate(String currentVersion, Component parent) {
        new Thread(() -> {
            try {
                // Thêm timestamp để tránh cache
                URL url = new URL(VERSION_URL + "?t=" + System.currentTimeMillis());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.setRequestProperty("User-Agent", "TutorHub-Updater");

                if (conn.getResponseCode() == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    String json = scanner.hasNext() ? scanner.next() : "";
                    scanner.close();

                    // Parse JSON thủ công đơn giản (vì không muốn thêm thư viện JSON nặng ở utils)
                    String latestVersion = extractJsonValue(json, "version");
                    String downloadUrl = extractJsonValue(json, "url");
                    String notes = extractJsonValue(json, "notes");

                    if (latestVersion != null && isNewerVersion(currentVersion, latestVersion)) {
                        SwingUtilities.invokeLater(() -> promptUpdate(parent, latestVersion, downloadUrl, notes));
                    }
                }
            } catch (Exception e) {
                System.out.println("AutoUpdate check failed: " + e.getMessage());
            }
        }).start();
    }

    private static void promptUpdate(Component parent, String newVersion, String downloadUrl, String notes) {
        String msg = "Có phiên bản mới: v" + newVersion + "\n\nTính năng mới:\n" + notes + "\n\nBạn có muốn cập nhật ngay không?";
        int choice = JOptionPane.showConfirmDialog(parent, msg, "Cập nhật phần mềm", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            downloadAndUpdate(parent, downloadUrl);
        }
    }

    private static void downloadAndUpdate(Component parent, String downloadUrl) {
        JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Đang tải bản cập nhật...", true);
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressDialog.setLayout(new BorderLayout(10, 10));
        progressDialog.add(new JLabel(" Vui lòng không đóng phần mềm trong lúc tải..."), BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setSize(350, 100);
        progressDialog.setLocationRelativeTo(parent);

        new Thread(() -> {
            try {
                File tempFile = downloadWithRetries(downloadUrl, progressBar);

                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    applyUpdate(tempFile, parent);
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(parent, "Lỗi tải bản cập nhật: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();

        progressDialog.setVisible(true);
    }

    private static File downloadWithRetries(String downloadUrl, JProgressBar progressBar) throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_DOWNLOAD_ATTEMPTS; attempt++) {
            try {
                final int currentAttempt = attempt;
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(0);
                    progressBar.setString("Dang tai... lan " + currentAttempt + "/" + MAX_DOWNLOAD_ATTEMPTS);
                });
                return downloadOnce(downloadUrl, progressBar, attempt);
            } catch (Exception e) {
                lastError = e;
                if (attempt < MAX_DOWNLOAD_ATTEMPTS) {
                    Thread.sleep(1200L * attempt);
                }
            }
        }
        throw new IOException("Tai ban cap nhat that bai sau " + MAX_DOWNLOAD_ATTEMPTS + " lan: "
                + (lastError == null ? "unknown error" : lastError.getMessage()), lastError);
    }

    private static File downloadOnce(String downloadUrl, JProgressBar progressBar, int attempt) throws Exception {
        String separator = downloadUrl.contains("?") ? "&" : "?";
        URL url = new URL(downloadUrl + separator + "t=" + System.currentTimeMillis() + "&attempt=" + attempt);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("User-Agent", "TutorHub-Updater");

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " khi tai update.jar");
        }

        long fileSize = conn.getContentLengthLong();
        File partFile = File.createTempFile("update_tutorhub_", ".jar.part");
        long totalRead = 0;

        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream out = new BufferedOutputStream(new FileOutputStream(partFile))) {
            byte[] buffer = new byte[64 * 1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                if (fileSize > 0) {
                    int percent = (int) Math.min(100, (totalRead * 100L) / fileSize);
                    SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
                }
            }
        } catch (IOException e) {
            Files.deleteIfExists(partFile.toPath());
            throw e;
        }

        if (fileSize > 0 && totalRead != fileSize) {
            Files.deleteIfExists(partFile.toPath());
            throw new EOFException("Tai thieu du lieu: " + totalRead + "/" + fileSize + " bytes");
        }
        if (totalRead < MIN_UPDATE_JAR_BYTES || !looksLikeJar(partFile)) {
            Files.deleteIfExists(partFile.toPath());
            throw new IOException("File update.jar tai ve khong hop le.");
        }

        File tempFile = new File(System.getProperty("java.io.tmpdir"), "update_tutorhub_" + System.currentTimeMillis() + ".jar");
        Files.move(partFile.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    private static boolean looksLikeJar(File file) {
        byte[] signature = new byte[4];
        try (InputStream in = new FileInputStream(file)) {
            return in.read(signature) == signature.length
                    && signature[0] == 'P'
                    && signature[1] == 'K'
                    && signature[2] == 3
                    && signature[3] == 4;
        } catch (IOException e) {
            return false;
        }
    }

    private static void applyUpdate(File tempJarFile, Component parent) {
        try {
            File currentJarFile = new File(AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!currentJarFile.getAbsolutePath().endsWith(".jar")) {
                JOptionPane.showMessageDialog(parent, "Tính năng tự động cập nhật chỉ hoạt động trên bản Build .EXE hoặc .JAR!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Tìm file TutorHub.exe (Thường nằm ở ../TutorHub.exe so với thư mục app/)
            File exeFile = new File(currentJarFile.getParentFile().getParentFile(), "TutorHub.exe");
            String restartCommand = exeFile.exists() ? "\"" + exeFile.getAbsolutePath() + "\"" : "";

            // Sinh file updater.bat
            File batFile = new File(System.getProperty("java.io.tmpdir"), "tutorhub_updater.bat");
            try (PrintWriter writer = new PrintWriter(batFile, "UTF-8")) {
                writer.println("@echo off");
                writer.println("echo Dang cap nhat phan mem. Vui long doi vai giay...");
                writer.println("timeout /t 3 /nobreak > NUL");
                writer.println("copy /Y \"" + tempJarFile.getAbsolutePath() + "\" \"" + currentJarFile.getAbsolutePath() + "\"");
                if (!restartCommand.isEmpty()) {
                    writer.println("start \"\" " + restartCommand);
                }
                writer.println("del \"" + tempJarFile.getAbsolutePath() + "\"");
                writer.println("del \"%~f0\"");
            }

            // Chạy file .bat
            Runtime.getRuntime().exec("cmd /c start \"\" /MIN \"" + batFile.getAbsolutePath() + "\"");

            // Thoát ứng dụng ngay lập tức
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Lỗi khi áp dụng bản cập nhật: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int index = json.indexOf(searchKey);
        if (index == -1) return null;
        int start = json.indexOf("\"", index + searchKey.length()) + 1;
        int end = json.indexOf("\"", start);
        if (start > 0 && end > start) {
            return json.substring(start, end).replace("\\\"", "\"");
        }
        return null;
    }

    private static boolean isNewerVersion(String current, String remote) {
        String[] currParts = current.split("\\.");
        String[] remoteParts = remote.split("\\.");
        int length = Math.max(currParts.length, remoteParts.length);
        for (int i = 0; i < length; i++) {
            int c = i < currParts.length ? Integer.parseInt(currParts[i]) : 0;
            int r = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;
            if (r > c) return true;
            if (r < c) return false;
        }
        return false;
    }
}
