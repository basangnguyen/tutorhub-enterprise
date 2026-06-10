package com.mycompany.tutorhub_enterprise.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegUtils {

    public static void compressVideo(File inputFile, File outputFile, Consumer<Integer> progressCallback, Runnable onComplete, Consumer<Exception> onError) {
        new Thread(() -> {
            try {
                String ffmpegPath = "tools/ffmpeg.exe";
                File ffmpegFile = new File(ffmpegPath);
                if (!ffmpegFile.exists()) {
                    throw new RuntimeException("Không tìm thấy công cụ FFmpeg tại " + ffmpegFile.getAbsolutePath());
                }

                // Compress command: giữ nguyên độ phân giải, giảm bitrate bằng CRF 28, tốc độ nén ultrafast
                ProcessBuilder pb = new ProcessBuilder(
                        ffmpegFile.getAbsolutePath(),
                        "-y",
                        "-i", inputFile.getAbsolutePath(),
                        "-c:v", "libx264",
                        "-crf", "28",
                        "-preset", "ultrafast",
                        "-c:a", "aac",
                        "-b:a", "128k",
                        outputFile.getAbsolutePath()
                );

                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                
                long totalDurationSec = 0;
                
                Pattern durationPattern = Pattern.compile("Duration: (\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2})");
                Pattern timePattern = Pattern.compile("time=(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2})");

                while ((line = reader.readLine()) != null) {
                    if (totalDurationSec == 0) {
                        Matcher m = durationPattern.matcher(line);
                        if (m.find()) {
                            int hours = Integer.parseInt(m.group(1));
                            int minutes = Integer.parseInt(m.group(2));
                            int seconds = Integer.parseInt(m.group(3));
                            totalDurationSec = hours * 3600 + minutes * 60 + seconds;
                        }
                    }

                    Matcher m = timePattern.matcher(line);
                    if (m.find() && totalDurationSec > 0) {
                        int hours = Integer.parseInt(m.group(1));
                        int minutes = Integer.parseInt(m.group(2));
                        int seconds = Integer.parseInt(m.group(3));
                        long currentTimeSec = hours * 3600 + minutes * 60 + seconds;
                        
                        int progress = (int) ((currentTimeSec * 100) / totalDurationSec);
                        if (progress > 100) progress = 100;
                        if (progressCallback != null) {
                            progressCallback.accept(progress);
                        }
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    if (progressCallback != null) progressCallback.accept(100);
                    if (onComplete != null) onComplete.run();
                } else {
                    if (onError != null) onError.accept(new RuntimeException("FFmpeg báo lỗi (Mã lỗi: " + exitCode + ")"));
                }

            } catch (Exception e) {
                if (onError != null) onError.accept(e);
            }
        }).start();
    }
}
