package com.mycompany.tutorhub_enterprise.utils;

public class DriveFormatUtils {

    /**
     * Format timestamp thành thời gian tương đối dễ đọc ("3 phút trước", "Hôm qua", ...)
     */
    public static String formatRelativeTime(java.sql.Timestamp ts) {
        if (ts == null) return "Mới đây";
        long diff = System.currentTimeMillis() - ts.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (hours < 24) return hours + " giờ trước";
        if (days == 1) return "Hôm qua";
        if (days < 7) return days + " ngày trước";
        if (days < 30) return (days / 7) + " tuần trước";
        return new java.text.SimpleDateFormat("dd/MM/yyyy").format(ts);
    }

    /**
     * Format kích thước file thành chuỗi dễ đọc
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
